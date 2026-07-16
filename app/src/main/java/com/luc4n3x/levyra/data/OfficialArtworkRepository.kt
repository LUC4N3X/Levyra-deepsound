package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.abs

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
    private val cache = ConcurrentHashMap<String, OfficialArtwork>()
    private val misses = ConcurrentHashMap<String, Long>()
    private val keyLocks = Array(32) { Mutex() }
    private val searchSlots = Semaphore(4)

    suspend fun find(track: Track, country: String): OfficialArtwork? {
        if (track.title.isBlank() || track.artist.isBlank()) return null
        val key = cacheKey(track, country)
        cache[key]?.let { return it }
        val now = System.currentTimeMillis()
        misses[key]?.let { if (now - it < MISS_TTL_MS) return null }
        val keyLock = keyLocks[(key.hashCode() and Int.MAX_VALUE) % keyLocks.size]
        return keyLock.withLock {
            cache[key]?.let { return@withLock it }
            misses[key]?.let { if (System.currentTimeMillis() - it < MISS_TTL_MS) return@withLock null }
            val outcome = searchSlots.withPermit { search(track, country) }
            if (outcome.artwork != null) {
                cache[key] = outcome.artwork
                misses.remove(key)
            } else if (outcome.completedRequest) {
                misses[key] = System.currentTimeMillis()
            }
            outcome.artwork
        }
    }

    private suspend fun search(track: Track, country: String): SearchOutcome = withContext(Dispatchers.IO) {
        val normalizedCountry = country.trim().uppercase(Locale.ROOT).takeIf { it.length == 2 } ?: "IT"
        coroutineScope {
            val apple = async { fetchApple(track, primaryQuery(track), normalizedCountry) }
            val deezer = async { fetchDeezer(track) }
            val qobuz = async { fetchQobuz(track, normalizedCountry) }
            val responses = listOf(apple.await(), deezer.await(), qobuz.await())
            val best = responses
                .flatMap { it.items }
                .sortedWith(compareByDescending<OfficialArtwork> { it.score }.thenByDescending { metadataCompleteness(it) })
                .firstOrNull()
                ?.takeIf { it.score >= MIN_ACCEPTED_SCORE }
            SearchOutcome(
                artwork = best,
                completedRequest = responses.any { it.completed }
            )
        }
    }

    private fun fetchApple(track: Track, query: String, country: String): ProviderResponse {
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

    private fun fetchDeezer(track: Track): ProviderResponse {
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
        val albumDetails = HashMap<Long, JSONObject?>()
        preliminary.sortedByDescending { it.second }.take(4).forEach { (item, _) ->
            val albumId = item.optJSONObject("album")?.optLong("id", 0L) ?: 0L
            if (albumId > 0L && !albumDetails.containsKey(albumId)) {
                albumDetails[albumId] = requestJson("$DEEZER_ALBUM_URL/$albumId")
            }
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

    private fun fetchQobuz(track: Track, country: String): ProviderResponse {
        val locale = qobuzLocale(country)
        val encoded = URLEncoder.encode(primaryQuery(track), StandardCharsets.UTF_8.name()).replace("+", "%20")
        val searchUrl = "https://www.qobuz.com/$locale/search/albums/$encoded"
        val searchHtml = requestText(searchUrl, HTML_ACCEPT) ?: return ProviderResponse(emptyList(), false)
        val albumLinks = QOBUZ_ALBUM_LINK.findAll(searchHtml)
            .map { decodeHtml(it.groupValues[1]) }
            .filter { it.contains("/album/") }
            .distinct()
            .take(8)
            .toList()
        val items = albumLinks.mapNotNull { href ->
            val absolute = if (href.startsWith("http")) href else "https://www.qobuz.com$href"
            val id = extractQobuzAlbumId(absolute) ?: return@mapNotNull null
            val html = requestText("https://www.qobuz.com/us-en/album/-/$id", HTML_ACCEPT, "en-US,en;q=0.9") ?: return@mapNotNull null
            parseQobuzAlbum(html, id, track)
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
            year = releaseDate.take(4).takeIf { it.all(Char::isDigit) }.orEmpty()
        )
    }

    private fun requestJson(url: String): JSONObject? {
        val body = requestText(url, JSON_ACCEPT) ?: return null
        return runCatching { JSONObject(body) }.getOrNull()
    }

    private fun requestText(url: String, accept: String, acceptLanguage: String = DEFAULT_ACCEPT_LANGUAGE): String? {
        val request = Request.Builder()
            .url(url)
            .header("Accept", accept)
            .header("Accept-Language", acceptLanguage)
            .header("User-Agent", USER_AGENT)
            .get()
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                response.body?.string()?.takeIf { it.isNotBlank() }
            }
        }.getOrNull()
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
        var score = 0
        score += when {
            candidateTitle == targetTitle -> 170
            candidateTitle.contains(targetTitle) || targetTitle.contains(candidateTitle) -> 120
            tokenCoverage(targetTitle, candidateTitle) >= 0.85 -> 95
            tokenCoverage(targetTitle, candidateTitle) >= 0.65 -> 70
            else -> 0
        }
        score += when {
            candidateArtist == targetArtist -> 125
            candidateArtist.contains(targetArtist) || targetArtist.contains(candidateArtist) -> 88
            tokenCoverage(targetArtist, candidateArtist) >= 0.75 -> 68
            tokenCoverage(targetArtist, candidateArtist) >= 0.5 -> 45
            else -> 0
        }
        if (track.isrc.isNotBlank() && isrc.isNotBlank()) score += if (track.isrc.equals(isrc, true)) 220 else -100
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
        var score = when {
            candidateArtist == targetArtist -> 150
            candidateArtist.contains(targetArtist) || targetArtist.contains(candidateArtist) -> 105
            tokenCoverage(targetArtist, candidateArtist) >= 0.75 -> 80
            tokenCoverage(targetArtist, candidateArtist) >= 0.5 -> 50
            else -> 0
        }
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
        const val USER_AGENT = "Levyra/2.3.10 Android"
        const val MIN_ACCEPTED_SCORE = 200
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
        val GENERIC_ALBUMS = setOf("album", "single", "unknown album", "music", "youtube music", "youtube")
    }
}
