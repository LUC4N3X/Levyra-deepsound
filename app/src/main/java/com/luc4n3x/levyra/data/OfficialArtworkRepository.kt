package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.LinkedHashMap
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.coroutines.resume

class OfficialArtworkRepository(context: Context) {
    private val client = LevyraHttpClientFactory.general(context.applicationContext)
        .newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
    private val cacheGuard = Any()
    private val cache = object : LinkedHashMap<String, OfficialArtwork>(CACHE_MAX_ENTRIES + 1, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, OfficialArtwork>): Boolean =
            size > CACHE_MAX_ENTRIES
    }
    private val misses = object : LinkedHashMap<String, Long>(MISS_CACHE_MAX_ENTRIES + 1, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>): Boolean =
            size > MISS_CACHE_MAX_ENTRIES
    }
    private var missSweepCounter = 0
    private val keyLocks = Array(32) { Mutex() }
    private val searchSlots = Semaphore(4)

    suspend fun find(track: Track, country: String): OfficialArtwork? {
        if (track.title.isBlank() || track.artist.isBlank()) return null
        val key = cacheKey(track, country)
        readCache(key)?.let { return it }
        val now = System.currentTimeMillis()
        if (hasFreshMiss(key, now)) return null
        val keyLock = keyLocks[(key.hashCode() and Int.MAX_VALUE) % keyLocks.size]
        return keyLock.withLock {
            readCache(key)?.let { return@withLock it }
            if (hasFreshMiss(key, System.currentTimeMillis())) return@withLock null
            val outcome = searchSlots.withPermit { search(track, country) }
            writeOutcome(key, outcome)
            outcome.artwork
        }
    }

    private suspend fun search(track: Track, country: String): SearchOutcome = withContext(Dispatchers.IO) {
        val normalizedCountry = country.trim().uppercase(Locale.ROOT).takeIf { it.length == 2 } ?: "IT"
        coroutineScope {
            val apple = async {
                fetchProvider(APPLE_TIMEOUT_MS) { fetchApple(track, primaryQuery(track), normalizedCountry) }
            }
            val deezer = async {
                fetchProvider(DEEZER_TIMEOUT_MS) { fetchDeezer(track) }
            }
            val first = select<Pair<ProviderResponse, kotlinx.coroutines.Deferred<ProviderResponse>>> {
                apple.onAwait { it to deezer }
                deezer.onAwait { it to apple }
            }
            val firstBest = bestAccepted(first.first.items)
            if (firstBest != null && isDecisiveMatch(track, firstBest)) {
                first.second.cancel()
                return@coroutineScope SearchOutcome(firstBest, first.first.completed)
            }
            val secondResponse = first.second.await()
            val directResponses = listOf(first.first, secondResponse)
            val directBest = bestAccepted(directResponses.flatMap { it.items })
            if (directBest != null && isDecisiveMatch(track, directBest)) {
                return@coroutineScope SearchOutcome(directBest, directResponses.any { it.completed })
            }
            if (!needsQobuzSupplement(directBest)) {
                return@coroutineScope SearchOutcome(directBest, directResponses.any { it.completed })
            }
            val qobuzResponse = fetchProvider(QOBUZ_TIMEOUT_MS) {
                fetchQobuz(track, normalizedCountry)
            }
            val qobuzBest = bestAccepted(qobuzResponse.items)
            SearchOutcome(
                artwork = mergeArtwork(directBest, qobuzBest),
                completedRequest = directResponses.any { it.completed } || qobuzResponse.completed
            )
        }
    }

    private suspend fun fetchApple(track: Track, query: String, country: String): ProviderResponse {
        val url = APPLE_SEARCH_URL.toHttpUrl().newBuilder()
            .addQueryParameter("term", query)
            .addQueryParameter("media", "music")
            .addQueryParameter("entity", "song")
            .addQueryParameter("limit", "30")
            .addQueryParameter("country", country)
            .addQueryParameter("explicit", "Yes")
            .build()
        val root = requestJson(url.toString()) ?: return ProviderResponse(emptyList(), false)
        val results = root.optJSONArray("results") ?: return ProviderResponse(emptyList(), true)
        val items = ArrayList<OfficialArtwork>(results.length())
        for (index in 0 until results.length()) {
            val item = results.optJSONObject(index) ?: continue
            val artwork = item.optString("artworkUrl100").trim()
            val title = item.optString("trackName").trim()
            val artist = item.optString("artistName").trim()
            if (artwork.isBlank() || title.isBlank() || artist.isBlank()) continue
            val album = item.optString("collectionName").trim()
            val durationMs = item.optLong("trackTimeMillis", 0L)
            val releaseDate = item.optString("releaseDate").trim()
            val score = matchScore(track, title, artist, album, durationMs, item.optString("isrc"))
            items += OfficialArtwork(
                thumbnailUrl = resizeAppleArtwork(artwork, 600),
                largeThumbnailUrl = resizeAppleArtwork(artwork, 1400),
                album = album,
                score = score,
                provider = "Apple Music",
                canonicalAlbumUrl = item.optString("collectionViewUrl"),
                releaseDate = releaseDate,
                year = releaseDate.take(4).takeIf { it.all(Char::isDigit) }.orEmpty(),
                trackNumber = item.optInt("trackNumber", 0),
                discNumber = item.optInt("discNumber", 0),
                explicit = item.optString("trackExplicitness").equals("explicit", ignoreCase = true),
                isrc = item.optString("isrc")
            )
        }
        return ProviderResponse(items, true)
    }

    private suspend fun fetchDeezer(track: Track): ProviderResponse {
        val preciseQuery = "track:\"${queryText(removeVersionText(track.title))}\" artist:\"${queryText(track.artist)}\""
        val url = DEEZER_SEARCH_URL.toHttpUrl().newBuilder()
            .addQueryParameter("q", preciseQuery)
            .addQueryParameter("limit", "25")
            .build()
        val root = requestJson(url.toString()) ?: return ProviderResponse(emptyList(), false)
        val results = root.optJSONArray("data") ?: return ProviderResponse(emptyList(), true)
        val preliminary = ArrayList<Pair<JSONObject, Int>>(results.length())
        for (index in 0 until results.length()) {
            val item = results.optJSONObject(index) ?: continue
            val title = item.optString("title").trim()
            val artist = item.optJSONObject("artist")?.optString("name").orEmpty().trim()
            val album = item.optJSONObject("album")?.optString("title").orEmpty().trim()
            if (title.isBlank() || artist.isBlank()) continue
            val durationMs = item.optLong("duration", 0L).coerceAtLeast(0L) * 1000L
            val score = matchScore(track, title, artist, album, durationMs, item.optString("isrc"))
            preliminary += item to score
        }
        val albumIds = preliminary
            .sortedByDescending { it.second }
            .mapNotNull { (item, _) -> item.optJSONObject("album")?.optLong("id", 0L)?.takeIf { it > 0L } }
            .distinct()
            .take(DEEZER_MAX_ALBUM_DETAILS)
        val albumDetails = coroutineScope {
            albumIds.map { albumId ->
                async { albumId to requestJson("$DEEZER_ALBUM_URL/$albumId") }
            }.awaitAll().toMap()
        }
        val items = ArrayList<OfficialArtwork>(preliminary.size)
        preliminary.forEach { (item, score) ->
            val albumObject = item.optJSONObject("album")
            val albumId = albumObject?.optLong("id", 0L) ?: 0L
            val details = albumDetails[albumId]
            val thumbnail = firstNonBlank(
                details?.optString("cover_big").orEmpty(),
                albumObject?.optString("cover_big").orEmpty(),
                albumObject?.optString("cover_medium").orEmpty(),
                albumObject?.optString("cover").orEmpty()
            )
            val large = firstNonBlank(
                details?.optString("cover_xl").orEmpty(),
                albumObject?.optString("cover_xl").orEmpty(),
                details?.optString("cover_big").orEmpty(),
                thumbnail
            )
            if (thumbnail.isBlank()) return@forEach
            val releaseDate = details?.optString("release_date").orEmpty()
            items += OfficialArtwork(
                thumbnailUrl = thumbnail,
                largeThumbnailUrl = large,
                album = details?.optString("title").orEmpty().ifBlank { albumObject?.optString("title").orEmpty() },
                score = score,
                provider = "Deezer",
                canonicalAlbumUrl = details?.optString("link").orEmpty().ifBlank { albumObject?.optString("tracklist").orEmpty() },
                releaseDate = releaseDate,
                year = releaseDate.take(4).takeIf { it.all(Char::isDigit) }.orEmpty(),
                trackNumber = item.optInt("track_position", 0),
                discNumber = item.optInt("disk_number", 0),
                explicit = item.optBoolean("explicit_lyrics"),
                isrc = item.optString("isrc"),
                upc = details?.optString("upc").orEmpty()
            )
        }
        return ProviderResponse(items, true)
    }

    private suspend fun fetchQobuz(track: Track, country: String): ProviderResponse {
        val locale = qobuzLocale(country)
        val encoded = URLEncoder.encode(primaryQuery(track), StandardCharsets.UTF_8.name()).replace("+", "%20")
        val searchUrl = "https://www.qobuz.com/$locale/search/albums/$encoded"
        val searchHtml = requestText(searchUrl, HTML_ACCEPT) ?: return ProviderResponse(emptyList(), false)
        val albumLinks = QOBUZ_ALBUM_LINK.findAll(searchHtml)
            .map { decodeHtml(it.groupValues[1]) }
            .filter { it.contains("/album/") }
            .distinct()
            .take(QOBUZ_MAX_ALBUMS)
            .toList()
        val items = coroutineScope {
            albumLinks.map { href ->
                async {
                    val absolute = if (href.startsWith("http")) href else "https://www.qobuz.com$href"
                    val id = extractQobuzAlbumId(absolute) ?: return@async null
                    val html = requestText(
                        "https://www.qobuz.com/us-en/album/-/$id",
                        HTML_ACCEPT,
                        "en-US,en;q=0.9"
                    ) ?: return@async null
                    parseQobuzAlbum(html, id, track)
                }
            }.awaitAll().filterNotNull()
        }
        return ProviderResponse(items, true)
    }


    private fun extractQobuzAlbumId(url: String): String? {
        val segments = url.substringBefore('?').substringBefore('#').split('/').filter(String::isNotBlank)
        val albumIndex = segments.indexOf("album")
        if (albumIndex < 0) return null
        for (index in segments.lastIndex downTo albumIndex + 1) {
            val candidate = segments[index]
            if (QOBUZ_ID.matches(candidate)) return candidate
        }
        return null
    }

    private fun parseQobuzAlbum(html: String, id: String, track: Track): OfficialArtwork? {
        val types = allMeta(html, "og:type")
        if (types.none { it.equals("music.album", ignoreCase = true) }) return null
        val ogTitle = firstMeta(html, "og:title") ?: return null
        val ogImage = firstMeta(html, "og:image") ?: return null
        val description = firstMeta(html, "og:description").orEmpty()
        val parsed = parseQobuzTitleArtist(ogTitle, description) ?: return null
        val score = albumMatchScore(track, parsed.first, parsed.second)
        val releaseDate = firstNonBlank(
            firstMeta(html, "music:release_date").orEmpty(),
            firstMeta(html, "release_date").orEmpty(),
            QOBUZ_DATE_PUBLISHED.find(html)?.groupValues?.getOrNull(1).orEmpty()
        )
        return OfficialArtwork(
            thumbnailUrl = ogImage,
            largeThumbnailUrl = upgradeQobuzArtwork(ogImage),
            album = parsed.first,
            score = score,
            provider = "Qobuz",
            canonicalAlbumUrl = "https://play.qobuz.com/album/$id",
            releaseDate = releaseDate,
            year = releaseDate.take(4).takeIf { it.all(Char::isDigit) }.orEmpty(),
            upc = extractQobuzUpc(html)
        )
    }

    private suspend fun requestJson(url: String): JSONObject? {
        val body = requestText(url, JSON_ACCEPT) ?: return null
        return runCatching { JSONObject(body) }.getOrNull()
    }

    private suspend fun requestText(url: String, accept: String, acceptLanguage: String = DEFAULT_ACCEPT_LANGUAGE): String? {
        val request = Request.Builder()
            .url(url)
            .header("Accept", accept)
            .header("Accept-Language", acceptLanguage)
            .header("User-Agent", USER_AGENT)
            .get()
            .build()
        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resume(null)
                }

                override fun onResponse(call: Call, response: Response) {
                    val value = runCatching {
                        response.use {
                            if (!it.isSuccessful) null else it.body.string().takeIf(String::isNotBlank)
                        }
                    }.getOrNull()
                    continuation.resume(value)
                }
            })
        }
    }

    private fun parseQobuzTitleArtist(ogTitle: String, description: String): Pair<String, String>? {
        QOBUZ_DESCRIPTION.find(description)?.let { match ->
            val title = decodeHtml(match.groupValues[1]).trim()
            val artist = decodeHtml(match.groupValues[2]).trim()
            if (title.isNotBlank() && artist.isNotBlank()) return title to artist
        }
        val normalized = decodeHtml(ogTitle).replace(QOBUZ_TITLE_SUFFIX, "").trim()
        val split = normalized.lastIndexOf(',')
        if (split <= 0 || split >= normalized.lastIndex) return null
        val title = normalized.substring(0, split).trim()
        val artist = normalized.substring(split + 1).trim()
        return (title to artist).takeIf { title.isNotBlank() && artist.isNotBlank() }
    }

    private fun firstMeta(html: String, property: String): String? = allMeta(html, property).firstOrNull()

    private fun allMeta(html: String, property: String): List<String> {
        val escaped = Regex.escape(property)
        val patterns = listOf(
            Regex("<meta[^>]+(?:property|name)=[\\\"']$escaped[\\\"'][^>]+content=[\\\"']([^\\\"']*)[\\\"'][^>]*>", RegexOption.IGNORE_CASE),
            Regex("<meta[^>]+content=[\\\"']([^\\\"']*)[\\\"'][^>]+(?:property|name)=[\\\"']$escaped[\\\"'][^>]*>", RegexOption.IGNORE_CASE)
        )
        return patterns.flatMap { regex -> regex.findAll(html).map { decodeHtml(it.groupValues[1]) }.toList() }.distinct()
    }

    private fun primaryQuery(track: Track): String {
        val title = queryText(removeVersionText(track.title))
        val artist = queryText(track.artist)
        return "$title $artist".trim()
    }

    private fun matchScore(track: Track, title: String, artist: String, album: String, durationMs: Long, isrc: String): Int {
        val targetTitle = normalize(track.title)
        val targetArtist = normalize(track.artist)
        val targetAlbum = normalize(track.album)
        val candidateTitle = normalize(title)
        val candidateArtist = normalize(artist)
        val candidateAlbum = normalize(album)
        val exactIsrc = track.isrc.isNotBlank() && isrc.isNotBlank() && track.isrc.equals(isrc, true)
        val artistScore = when {
            candidateArtist == targetArtist -> 125
            candidateArtist.contains(targetArtist) || targetArtist.contains(candidateArtist) -> 88
            tokenCoverage(targetArtist, candidateArtist) >= 0.75 -> 68
            tokenCoverage(targetArtist, candidateArtist) >= 0.5 -> 45
            else -> 0
        }
        if (!exactIsrc && artistScore < MIN_ARTIST_MATCH_SCORE) return REJECTED_SCORE
        var score = 0
        score += when {
            candidateTitle == targetTitle -> 170
            candidateTitle.contains(targetTitle) || targetTitle.contains(candidateTitle) -> 120
            tokenCoverage(targetTitle, candidateTitle) >= 0.85 -> 95
            tokenCoverage(targetTitle, candidateTitle) >= 0.65 -> 70
            else -> 0
        }
        score += artistScore
        if (track.isrc.isNotBlank() && isrc.isNotBlank()) score += if (exactIsrc) 220 else -100
        if (targetAlbum.isNotBlank() && candidateAlbum.isNotBlank()) {
            score += when {
                candidateAlbum == targetAlbum -> 35
                candidateAlbum.contains(targetAlbum) || targetAlbum.contains(candidateAlbum) -> 20
                tokenCoverage(targetAlbum, candidateAlbum) >= 0.7 -> 12
                else -> 0
            }
        }
        if (track.durationMs > 0L && durationMs > 0L) {
            val delta = abs(track.durationMs - durationMs)
            score += when {
                delta <= 4_000L -> 30
                delta <= 10_000L -> 18
                delta <= 20_000L -> 8
                delta > 45_000L -> -30
                else -> 0
            }
        }
        val targetBlob = "$targetTitle $targetAlbum"
        val candidateBlob = "$candidateTitle $candidateAlbum"
        VERSION_TERMS.forEach { term -> if (candidateBlob.contains(term) && !targetBlob.contains(term)) score -= 38 }
        if (candidateTitle.isBlank() || candidateArtist.isBlank()) score -= 200
        return score
    }

    private fun albumMatchScore(track: Track, album: String, artist: String): Int {
        val targetArtist = normalize(track.artist)
        val candidateArtist = normalize(artist)
        val targetAlbum = normalize(track.album).takeUnless(::isGenericAlbum)
        val candidateAlbum = normalize(album)
        val targetTitle = normalize(track.title)
        val artistScore = when {
            candidateArtist == targetArtist -> 150
            candidateArtist.contains(targetArtist) || targetArtist.contains(candidateArtist) -> 105
            tokenCoverage(targetArtist, candidateArtist) >= 0.75 -> 80
            tokenCoverage(targetArtist, candidateArtist) >= 0.5 -> 50
            else -> 0
        }
        if (artistScore < MIN_ALBUM_ARTIST_MATCH_SCORE) return REJECTED_SCORE
        var score = artistScore
        score += if (!targetAlbum.isNullOrBlank()) {
            when {
                candidateAlbum == targetAlbum -> 190
                candidateAlbum.contains(targetAlbum) || targetAlbum.contains(candidateAlbum) -> 135
                tokenCoverage(targetAlbum, candidateAlbum) >= 0.8 -> 105
                tokenCoverage(targetAlbum, candidateAlbum) >= 0.6 -> 70
                else -> -90
            }
        } else {
            when {
                candidateAlbum == targetTitle -> 150
                candidateAlbum.contains(targetTitle) || targetTitle.contains(candidateAlbum) -> 95
                tokenCoverage(targetTitle, candidateAlbum) >= 0.8 -> 75
                else -> -70
            }
        }
        val targetBlob = "$targetTitle ${targetAlbum.orEmpty()}"
        VERSION_TERMS.forEach { term -> if (candidateAlbum.contains(term) && !targetBlob.contains(term)) score -= 45 }
        return score
    }

    private suspend fun fetchProvider(
        timeoutMs: Long,
        block: suspend () -> ProviderResponse
    ): ProviderResponse {
        return try {
            withTimeout(timeoutMs) { block() }
        } catch (_: TimeoutCancellationException) {
            ProviderResponse(emptyList(), false)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            ProviderResponse(emptyList(), false)
        }
    }

    private fun bestAccepted(items: List<OfficialArtwork>): OfficialArtwork? = items
        .asSequence()
        .filter { it.score >= MIN_ACCEPTED_SCORE }
        .sortedWith(compareByDescending<OfficialArtwork> { it.score }.thenByDescending { metadataCompleteness(it) })
        .firstOrNull()

    private fun isDecisiveMatch(track: Track, artwork: OfficialArtwork): Boolean {
        val exactIsrc = track.isrc.isNotBlank() && artwork.isrc.isNotBlank() && track.isrc.equals(artwork.isrc, true)
        return exactIsrc || artwork.score >= DECISIVE_MATCH_SCORE
    }

    private fun needsQobuzSupplement(artwork: OfficialArtwork?): Boolean {
        return artwork == null ||
            artwork.thumbnailUrl.isBlank() ||
            artwork.largeThumbnailUrl.isBlank() ||
            artwork.upc.isBlank() ||
            artwork.releaseDate.isBlank() ||
            artwork.canonicalAlbumUrl.isBlank()
    }

    private fun mergeArtwork(primary: OfficialArtwork?, supplement: OfficialArtwork?): OfficialArtwork? {
        if (primary == null) return supplement
        if (supplement == null) return primary
        return primary.copy(
            thumbnailUrl = primary.thumbnailUrl.ifBlank { supplement.thumbnailUrl },
            largeThumbnailUrl = primary.largeThumbnailUrl.ifBlank { supplement.largeThumbnailUrl },
            album = primary.album.ifBlank { supplement.album },
            score = maxOf(primary.score, supplement.score),
            canonicalAlbumUrl = primary.canonicalAlbumUrl.ifBlank { supplement.canonicalAlbumUrl },
            releaseDate = primary.releaseDate.ifBlank { supplement.releaseDate },
            year = primary.year.ifBlank { supplement.year },
            explicit = primary.explicit || supplement.explicit,
            upc = primary.upc.ifBlank { supplement.upc }
        )
    }

    private fun extractQobuzUpc(html: String): String {
        return QOBUZ_UPC_PATTERNS.firstNotNullOfOrNull { pattern ->
            pattern.find(html)?.groupValues?.getOrNull(1)?.filter(Char::isDigit)?.takeIf { it.length in 8..14 }
        }.orEmpty()
    }

    private fun readCache(key: String): OfficialArtwork? = synchronized(cacheGuard) { cache[key] }

    private fun hasFreshMiss(key: String, now: Long): Boolean = synchronized(cacheGuard) {
        missSweepCounter += 1
        if (missSweepCounter >= MISS_SWEEP_INTERVAL) {
            misses.entries.removeAll { now - it.value >= MISS_TTL_MS }
            missSweepCounter = 0
        }
        val timestamp = misses[key] ?: return@synchronized false
        if (now - timestamp < MISS_TTL_MS) true else {
            misses.remove(key)
            false
        }
    }

    private fun writeOutcome(key: String, outcome: SearchOutcome) {
        synchronized(cacheGuard) {
            if (outcome.artwork != null) {
                cache[key] = outcome.artwork
                misses.remove(key)
            } else if (outcome.completedRequest) {
                misses[key] = System.currentTimeMillis()
            }
            Unit
        }
    }

    private fun isGenericAlbum(value: String): Boolean {
        if (value.isBlank()) return true
        return value in GENERIC_ALBUMS || value.startsWith("youtube music") || value.startsWith("youtube")
    }

    private fun metadataCompleteness(value: OfficialArtwork): Int = listOf(
        value.largeThumbnailUrl,
        value.album,
        value.canonicalAlbumUrl,
        value.releaseDate,
        value.isrc,
        value.upc
    ).count { it.isNotBlank() } + if (value.trackNumber > 0) 1 else 0

    private fun tokenCoverage(target: String, candidate: String): Double {
        val targetTokens = target.split(' ').filter { it.length >= 2 }.toSet()
        if (targetTokens.isEmpty()) return 0.0
        val candidateTokens = candidate.split(' ').filter { it.length >= 2 }.toSet()
        if (candidateTokens.isEmpty()) return 0.0
        return targetTokens.count { it in candidateTokens }.toDouble() / targetTokens.size.toDouble()
    }

    private fun normalize(value: String): String = removeVersionText(value)
        .lowercase(Locale.ROOT)
        .replace(Regex("feat\\.?|featuring|ft\\.?"), " ")
        .replace(Regex("[^a-z0-9àèéìòóùçñäöüß\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun removeVersionText(value: String): String = value
        .replace(Regex("\\([^)]*(official\\s*(video|audio)|lyrics?|visualizer|music\\s*video)[^)]*\\)", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("\\[[^]]*(official\\s*(video|audio)|lyrics?|visualizer|music\\s*video)[^]]*]", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("\\s+-\\s+(official\\s*(video|audio)|lyrics?|visualizer|music\\s*video).*$", RegexOption.IGNORE_CASE), " ")
        .trim()

    private fun queryText(value: String): String = value
        .replace(Regex("[\\r\\n\\t]+"), " ")
        .replace('"', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun resizeAppleArtwork(url: String, size: Int): String = url.replace(APPLE_ARTWORK_SIZE, "${size}x${size}bb")

    private fun upgradeQobuzArtwork(url: String): String = url
        .replace(Regex("_\\d+\\.(jpg|jpeg|png)$", RegexOption.IGNORE_CASE), "_max.$1")
        .replace("_600.jpg", "_max.jpg")

    private fun qobuzLocale(country: String): String = when (country.uppercase(Locale.ROOT)) {
        "IT" -> "it-it"
        "FR" -> "fr-fr"
        "DE" -> "de-de"
        "ES" -> "es-es"
        "GB" -> "gb-en"
        else -> "us-en"
    }

    private fun decodeHtml(value: String): String = value
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace(Regex("&#(\\d+);")) { match -> match.groupValues[1].toIntOrNull()?.toChar()?.toString().orEmpty() }

    private fun cacheKey(track: Track, country: String): String = listOf(
        normalize(track.title),
        normalize(track.artist),
        normalize(track.album),
        track.isrc.lowercase(Locale.ROOT),
        (track.durationMs / 5_000L).toString(),
        country.uppercase(Locale.ROOT)
    ).joinToString("|")

    private fun firstNonBlank(vararg values: String): String = values.firstOrNull { it.isNotBlank() }.orEmpty()

    data class OfficialArtwork(
        val thumbnailUrl: String,
        val largeThumbnailUrl: String,
        val album: String,
        val score: Int,
        val provider: String = "",
        val canonicalAlbumUrl: String = "",
        val releaseDate: String = "",
        val year: String = "",
        val trackNumber: Int = 0,
        val discNumber: Int = 0,
        val explicit: Boolean = false,
        val isrc: String = "",
        val upc: String = ""
    )

    private data class ProviderResponse(
        val items: List<OfficialArtwork>,
        val completed: Boolean
    )

    private data class SearchOutcome(
        val artwork: OfficialArtwork?,
        val completedRequest: Boolean
    )

    private companion object {
        const val APPLE_SEARCH_URL = "https://itunes.apple.com/search"
        const val DEEZER_SEARCH_URL = "https://api.deezer.com/search/track"
        const val DEEZER_ALBUM_URL = "https://api.deezer.com/album"
        const val USER_AGENT = "Levyra/2.3.11 Android"
        const val MIN_ACCEPTED_SCORE = 200
        const val DECISIVE_MATCH_SCORE = 350
        const val MIN_ARTIST_MATCH_SCORE = 45
        const val MIN_ALBUM_ARTIST_MATCH_SCORE = 50
        const val REJECTED_SCORE = -1_000
        const val APPLE_TIMEOUT_MS = 10_000L
        const val DEEZER_TIMEOUT_MS = 12_000L
        const val QOBUZ_TIMEOUT_MS = 8_000L
        const val QOBUZ_MAX_ALBUMS = 3
        const val DEEZER_MAX_ALBUM_DETAILS = 3
        const val CACHE_MAX_ENTRIES = 384
        const val MISS_CACHE_MAX_ENTRIES = 512
        const val MISS_SWEEP_INTERVAL = 64
        const val MISS_TTL_MS = 10 * 60 * 1000L
        const val JSON_ACCEPT = "application/json"
        const val HTML_ACCEPT = "text/html,application/xhtml+xml"
        const val DEFAULT_ACCEPT_LANGUAGE = "it-IT,it;q=0.9,en;q=0.7"
        val APPLE_ARTWORK_SIZE = Regex("\\d+x\\d+bb")
        val VERSION_TERMS = listOf("live", "remix", "karaoke", "instrumental", "sped up", "slowed", "nightcore", "acoustic")
        val QOBUZ_ALBUM_LINK = Regex("href=[\\\"']([^\\\"']*/album/[^\\\"']+)[\\\"']", RegexOption.IGNORE_CASE)
        val QOBUZ_ID = Regex("[A-Za-z0-9]{8,}")
        val QOBUZ_DESCRIPTION = Regex("download (.+) by (.+) in Hi-Res quality on Qobuz", RegexOption.IGNORE_CASE)
        val QOBUZ_TITLE_SUFFIX = Regex("\\s*-\\s*Qobuz\\s*$", RegexOption.IGNORE_CASE)
        val QOBUZ_DATE_PUBLISHED = Regex("\"datePublished\"\\s*:\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE)
        val QOBUZ_UPC_PATTERNS = listOf(
            Regex("\"(?:gtin13|gtin|upc|barcode)\"\\s*:\\s*\"?(\\d{8,14})", RegexOption.IGNORE_CASE),
            Regex("(?:UPC|EAN|Barcode)\\s*[:\\-]\\s*(\\d{8,14})", RegexOption.IGNORE_CASE)
        )
        val GENERIC_ALBUMS = setOf("album", "single", "unknown album", "music", "youtube music", "youtube")
    }
}
