package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.PlaylistImportFailureKind
import com.luc4n3x.levyra.domain.Track
import java.io.IOException
import java.net.InetAddress
import java.net.URI
import java.net.URLDecoder
import java.net.UnknownHostException
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

sealed class PlaylistImportResult {
    data class Success(
        val playlist: Playlist,
        val importedCount: Int,
        val requestedCount: Int = importedCount
    ) : PlaylistImportResult()

    data class Failure(
        val kind: PlaylistImportFailureKind,
        val limit: Int? = null
    ) : PlaylistImportResult()
}

internal data class SpotifyPlaylistPage(
    val title: String,
    val trackUrls: List<String>,
    val declaredTrackCount: Int? = null
)

internal data class SpotifyTrackMetadata(
    val title: String,
    val artist: String,
    val durationMs: Long,
    val artworkUrl: String
)

internal const val MAX_JSON_IMPORT_TRACKS = 500
private const val MAX_SPOTIFY_IMPORT_TRACKS = 100
private const val MAX_IMPORT_INPUT_CHARS = 2_000_000
private const val IMPORT_RESOLUTION_CONCURRENCY = 4
private const val IMPORT_CANDIDATE_LIMIT = 5
private const val MIN_IMPORT_CANDIDATE_SCORE = 170
private const val MAX_SPOTIFY_REDIRECTS = 4
private const val MAX_SPOTIFY_HTML_BYTES = 768L * 1024L
private val YOUTUBE_VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")
private val META_TAG_PATTERN = Regex("""<meta\b[^>]*>""", RegexOption.IGNORE_CASE)
private val META_ATTRIBUTE_PATTERN = Regex(
    """([A-Za-z_:][A-Za-z0-9_.:-]*)\s*=\s*(["'])(.*?)\2""",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
)
private val DECIMAL_HTML_ENTITY = Regex("""&#(\d+);""")
private val HEX_HTML_ENTITY = Regex("&#x([0-9A-Fa-f]+);")
private val IMPORT_VARIANT_MARKERS = listOf(
    "live",
    "remix",
    "cover",
    "karaoke",
    "instrumental",
    "sped up",
    "slowed",
    "nightcore",
    "acoustic",
    "remaster",
    "radio edit"
)

internal fun parseSpotifyPlaylistPage(html: String): SpotifyPlaylistPage {
    val title = spotifyMetaValues(html, "og:title")
        .firstOrNull()
        .orEmpty()
        .substringBefore(" | Spotify")
        .trim()
        .ifBlank { "Spotify Playlist" }
    val trackUrls = spotifyMetaValues(html, "music:song")
        .mapNotNull(::normalizeSpotifyTrackUrl)
        .distinct()
    val declaredTrackCount = spotifyMetaValues(html, "music:song_count")
        .firstOrNull()
        ?.trim()
        ?.toIntOrNull()
        ?.takeIf { it >= 0 }
    return SpotifyPlaylistPage(title, trackUrls, declaredTrackCount)
}

internal fun parseSpotifyTrackPage(html: String): SpotifyTrackMetadata? {
    val title = spotifyMetaValues(html, "og:title").firstOrNull().orEmpty().trim()
    if (title.isBlank()) return null

    val explicitArtist = spotifyMetaValues(html, "music:musician_description")
        .firstOrNull()
        .orEmpty()
        .trim()
    val twitterDescription = spotifyMetaValues(html, "twitter:description").firstOrNull().orEmpty()
    val genericDescription = spotifyMetaValues(html, "description").firstOrNull().orEmpty()
    val artist = explicitArtist.ifBlank {
        spotifyArtistFromDescription(twitterDescription, genericDescription, title)
    }
    if (artist.isBlank()) return null

    val durationMs = spotifyMetaValues(html, "music:duration")
        .firstOrNull()
        ?.toLongOrNull()
        ?.coerceAtLeast(0L)
        ?.times(1000L)
        ?: 0L
    val artworkUrl = spotifyMetaValues(html, "og:image").firstOrNull().orEmpty().trim()
    return SpotifyTrackMetadata(title, artist, durationMs, artworkUrl)
}

private fun spotifyArtistFromDescription(twitter: String, generic: String, title: String): String {
    val twitterArtist = twitter.split(" · ", limit = 2).firstOrNull().orEmpty().trim()
    if (twitterArtist.isNotBlank() && !twitterArtist.equals(title, ignoreCase = true)) return twitterArtist

    val genericParts = generic.split(" · ").map(String::trim).filter(String::isNotBlank)
    if (genericParts.isEmpty()) return ""
    if (generic.contains(" on Spotify.", ignoreCase = true) || generic.startsWith("Listen to ", ignoreCase = true)) {
        return genericParts.getOrNull(1).orEmpty().trim()
    }
    return genericParts.firstOrNull().orEmpty().trim().takeUnless { it.equals(title, ignoreCase = true) }.orEmpty()
}

internal fun importedDurationMs(item: JSONObject): Long {
    val explicitMs = item.optLong("durationMs", 0L)
    if (explicitMs > 0L) return explicitMs
    val duration = item.optLong("duration", 0L)
    if (duration <= 0L) return 0L
    return if (duration > 86_400L) duration else duration * 1000L
}

internal fun jsonImportTrackCountAccepted(count: Int): Boolean = count in 0..MAX_JSON_IMPORT_TRACKS

internal fun validateSpotifyImportUrl(value: String): HttpUrl? {
    val url = value.trim().toHttpUrlOrNull() ?: return null
    if (!url.isHttps || url.port != 443) return null
    if (url.username.isNotEmpty() || url.password.isNotEmpty()) return null
    if (!isAllowedSpotifyHost(url.host.lowercase(Locale.ROOT))) return null
    return url
}

internal fun spotifyHtmlContentTypeAccepted(value: String?): Boolean {
    val mime = value
        ?.substringBefore(';')
        ?.trim()
        ?.lowercase(Locale.ROOT)
        .orEmpty()
    return mime == "text/html" || mime == "application/xhtml+xml"
}

private fun spotifyMetaValues(html: String, key: String): List<String> {
    val expected = key.lowercase(Locale.ROOT)
    return META_TAG_PATTERN.findAll(html).mapNotNull { match ->
        val attributes = META_ATTRIBUTE_PATTERN.findAll(match.value)
            .associate { attr ->
                attr.groupValues[1].lowercase(Locale.ROOT) to decodeHtmlEntities(attr.groupValues[3])
            }
        val selector = attributes["property"] ?: attributes["name"] ?: return@mapNotNull null
        if (selector.lowercase(Locale.ROOT) != expected) return@mapNotNull null
        attributes["content"]?.takeIf(String::isNotBlank)
    }.toList()
}

private fun normalizeSpotifyTrackUrl(value: String): String? {
    val clean = value.trim()
    return when {
        clean.startsWith("spotify:track:", ignoreCase = true) -> {
            val id = clean.substringAfterLast(':').trim()
            id.takeIf(String::isNotBlank)?.let { "https://open.spotify.com/track/$it" }
        }
        else -> {
            val url = validateSpotifyImportUrl(clean) ?: return null
            val path = url.encodedPath
            if (url.host.equals("open.spotify.com", ignoreCase = true) && path.startsWith("/track/")) {
                "https://open.spotify.com/track/${path.substringAfter("/track/").substringBefore('/')}"
            } else {
                null
            }
        }
    }
}

private fun decodeHtmlEntities(value: String): String {
    var decoded = value
        .replace("&amp;", "&", ignoreCase = true)
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&lt;", "<", ignoreCase = true)
        .replace("&gt;", ">", ignoreCase = true)
    decoded = DECIMAL_HTML_ENTITY.replace(decoded) { match ->
        match.groupValues[1].toIntOrNull()?.let(::codePointToString) ?: match.value
    }
    decoded = HEX_HTML_ENTITY.replace(decoded) { match ->
        match.groupValues[1].toIntOrNull(16)?.let(::codePointToString) ?: match.value
    }
    return decoded
}

private fun codePointToString(codePoint: Int): String =
    runCatching { String(Character.toChars(codePoint)) }.getOrDefault("")

internal fun playlistImportCandidateScore(
    sourceTitle: String,
    sourceArtist: String,
    sourceDurationMs: Long,
    candidate: Track
): Int {
    val titleSimilarity = importTextSimilarity(sourceTitle, candidate.title)
    if (titleSimilarity < 52) return Int.MIN_VALUE

    val hasArtistSignal = sourceArtist.isNotBlank()
    val artistSimilarity = if (hasArtistSignal) importTextSimilarity(sourceArtist, candidate.artist) else 60
    if (hasArtistSignal && artistSimilarity < 35) return Int.MIN_VALUE

    var score = titleSimilarity * 2 + artistSimilarity
    if (sourceDurationMs > 0L && candidate.durationMs > 0L) {
        score += when (abs(sourceDurationMs - candidate.durationMs)) {
            in 0L..3_000L -> 30
            in 3_001L..8_000L -> 20
            in 8_001L..15_000L -> 10
            in 30_001L..Long.MAX_VALUE -> -25
            else -> 0
        }
    }

    val sourceVariants = importVariantFlags(sourceTitle)
    val candidateVariants = importVariantFlags(candidate.title)
    score -= (sourceVariants union candidateVariants).count { marker ->
        (marker in sourceVariants) != (marker in candidateVariants)
    } * 35
    return score
}

internal fun bestPlaylistImportCandidate(
    sourceTitle: String,
    sourceArtist: String,
    sourceDurationMs: Long,
    candidates: List<Track>
): Track? = candidates
    .map { candidate -> candidate to playlistImportCandidateScore(sourceTitle, sourceArtist, sourceDurationMs, candidate) }
    .maxByOrNull { it.second }
    ?.takeIf { it.second >= MIN_IMPORT_CANDIDATE_SCORE }
    ?.first

private fun importTextSimilarity(left: String, right: String): Int {
    val a = normalizeImportText(left)
    val b = normalizeImportText(right)
    if (a.isBlank() || b.isBlank()) return 0
    if (a == b) return 100
    val aTokens = a.split(' ').filter(String::isNotBlank).toSet()
    val bTokens = b.split(' ').filter(String::isNotBlank).toSet()
    if (aTokens.isEmpty() || bTokens.isEmpty()) return 0
    val intersection = aTokens.intersect(bTokens).size.toDouble()
    val f1 = (2.0 * intersection / (aTokens.size + bTokens.size).toDouble() * 100.0).toInt()
    val containment = if (a.contains(b) || b.contains(a)) 72 else 0
    return maxOf(f1, containment).coerceIn(0, 100)
}

private fun normalizeImportText(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
    .replace(Regex("""\p{M}+"""), "")
    .lowercase(Locale.ROOT)
    .replace(Regex("""[^\p{L}\p{N}]+"""), " ")
    .replace(Regex("""\s+"""), " ")
    .trim()

private fun importVariantFlags(value: String): Set<String> {
    val normalized = normalizeImportText(value)
    return IMPORT_VARIANT_MARKERS.filterTo(linkedSetOf()) { marker -> normalized.contains(marker) }
}

private class PlaylistImportException(
    val kind: PlaylistImportFailureKind,
    message: String,
    cause: Throwable? = null
) : IOException(message, cause)

class UniversalPlaylistImporter(
    context: Context,
    private val playlistStore: PlaylistStore = PlaylistStore(context),
    private val youtubeRepository: YoutubeMusicRepository = YoutubeMusicRepository(context),
    httpClient: OkHttpClient = LevyraHttpClientFactory.media(context.applicationContext)
) {
    private val baseSpotifyDns = httpClient.dns
    private val spotifyHttpClient = httpClient.newBuilder()
        .dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                val host = hostname.lowercase(Locale.ROOT)
                if (!isAllowedSpotifyHost(host)) throw UnknownHostException("Spotify host not allowed")
                val addresses = baseSpotifyDns.lookup(hostname)
                if (addresses.isEmpty() || addresses.any { !isPublicNetworkAddress(it) }) {
                    throw UnknownHostException("Spotify destination is not public")
                }
                return addresses
            }
        })
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun importFromUrlOrJson(
        input: String,
        customName: String? = null,
        languageCode: String = "en"
    ): PlaylistImportResult = withContext(Dispatchers.IO) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return@withContext PlaylistImportResult.Failure(PlaylistImportFailureKind.INVALID_INPUT)
        if (trimmed.length > MAX_IMPORT_INPUT_CHARS) {
            return@withContext PlaylistImportResult.Failure(PlaylistImportFailureKind.TOO_LARGE)
        }

        try {
            when {
                trimmed.startsWith("{") || trimmed.startsWith("[") -> importFromJson(trimmed, customName, languageCode)
                isYoutubeUrl(trimmed) -> importFromYoutubeUrl(trimmed, customName, languageCode)
                isSpotifyUrl(trimmed) -> importFromSpotifyUrl(trimmed, customName, languageCode)
                else -> PlaylistImportResult.Failure(PlaylistImportFailureKind.INVALID_INPUT)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: PlaylistImportException) {
            Timber.w(error, "Playlist import provider failure")
            PlaylistImportResult.Failure(error.kind)
        } catch (error: JSONException) {
            Timber.d(error, "Invalid playlist JSON")
            PlaylistImportResult.Failure(PlaylistImportFailureKind.INVALID_INPUT)
        } catch (error: IOException) {
            Timber.w(error, "Playlist import network failure")
            PlaylistImportResult.Failure(PlaylistImportFailureKind.NETWORK)
        } catch (error: Throwable) {
            Timber.w(error, "Playlist import failed")
            PlaylistImportResult.Failure(PlaylistImportFailureKind.PROVIDER_CHANGED)
        }
    }

    private suspend fun importFromYoutubeUrl(
        url: String,
        customName: String?,
        languageCode: String
    ): PlaylistImportResult {
        val listId = extractQueryParam(url, "list")
        if (listId.isBlank()) return PlaylistImportResult.Failure(PlaylistImportFailureKind.INVALID_INPUT)

        val fetchedPlaylist = youtubeRepository.playlist(listId, languageCode, 300)
            ?: return PlaylistImportResult.Failure(PlaylistImportFailureKind.NOT_AVAILABLE)
        if (fetchedPlaylist.tracks.isEmpty()) {
            return PlaylistImportResult.Failure(PlaylistImportFailureKind.NOT_AVAILABLE)
        }

        val name = customName?.ifBlank { null } ?: fetchedPlaylist.title.ifBlank { "YouTube Playlist" }
        return persistPlaylist(name, fetchedPlaylist.tracks, fetchedPlaylist.tracks.size)
    }

    private suspend fun importFromSpotifyUrl(
        url: String,
        customName: String?,
        languageCode: String
    ): PlaylistImportResult {
        val page = parseSpotifyPlaylistPage(fetchSpotifyText(url))
        if (page.trackUrls.isEmpty()) {
            return PlaylistImportResult.Failure(PlaylistImportFailureKind.NOT_AVAILABLE)
        }

        val requestedCount = maxOf(page.declaredTrackCount ?: 0, page.trackUrls.size)
        val limiter = Semaphore(IMPORT_RESOLUTION_CONCURRENCY)
        val resolvedTracks = coroutineScope {
            page.trackUrls.take(MAX_SPOTIFY_IMPORT_TRACKS).map { trackUrl ->
                async {
                    limiter.withPermit {
                        try {
                            val metadata = parseSpotifyTrackPage(fetchSpotifyText(trackUrl)) ?: return@withPermit null
                            val resolved = resolveBestTrack(
                                metadata.title,
                                metadata.artist,
                                metadata.durationMs,
                                languageCode
                            ) ?: return@withPermit null
                            resolved.copy(
                                thumbnailUrl = resolved.thumbnailUrl.ifBlank { metadata.artworkUrl },
                                largeThumbnailUrl = resolved.largeThumbnailUrl.ifBlank {
                                    resolved.thumbnailUrl.ifBlank { metadata.artworkUrl }
                                },
                                durationMs = resolved.durationMs.takeIf { it > 0L } ?: metadata.durationMs,
                                source = "Spotify import"
                            )
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Throwable) {
                            Timber.d(error, "Skipping unresolved Spotify track %s", trackUrl)
                            null
                        }
                    }
                }
            }.awaitAll().filterNotNull()
        }

        if (resolvedTracks.isEmpty()) {
            return PlaylistImportResult.Failure(PlaylistImportFailureKind.NO_MATCHES)
        }
        val name = customName?.ifBlank { null } ?: page.title
        return persistPlaylist(name, resolvedTracks, requestedCount.coerceAtLeast(resolvedTracks.size))
    }

    private suspend fun importFromJson(
        jsonText: String,
        customName: String?,
        languageCode: String
    ): PlaylistImportResult {
        val rawTracks = mutableListOf<Track>()
        var extractedName: String? = null
        val requestedCount: Int

        if (jsonText.startsWith("[")) {
            val array = JSONArray(jsonText)
            requestedCount = array.length()
            if (!jsonImportTrackCountAccepted(requestedCount)) {
                return PlaylistImportResult.Failure(PlaylistImportFailureKind.TOO_LARGE, MAX_JSON_IMPORT_TRACKS)
            }
            parseJsonArrayToTracks(array, rawTracks)
        } else {
            val obj = JSONObject(jsonText)
            extractedName = obj.optString("name").ifBlank { obj.optString("title").ifBlank { null } }
            val tracksArray = obj.optJSONArray("tracks")
                ?: obj.optJSONArray("songs")
                ?: obj.optJSONArray("queue")
                ?: return PlaylistImportResult.Failure(PlaylistImportFailureKind.INVALID_INPUT)
            requestedCount = tracksArray.length()
            if (!jsonImportTrackCountAccepted(requestedCount)) {
                return PlaylistImportResult.Failure(PlaylistImportFailureKind.TOO_LARGE, MAX_JSON_IMPORT_TRACKS)
            }
            parseJsonArrayToTracks(tracksArray, rawTracks)
        }

        if (rawTracks.isEmpty()) return PlaylistImportResult.Failure(PlaylistImportFailureKind.NO_MATCHES)
        val playableTracks = resolveImportedTracks(rawTracks, languageCode)
        if (playableTracks.isEmpty()) return PlaylistImportResult.Failure(PlaylistImportFailureKind.NO_MATCHES)
        val name = customName?.ifBlank { null } ?: extractedName ?: "Imported Playlist"
        return persistPlaylist(name, playableTracks, requestedCount)
    }

    private suspend fun resolveImportedTracks(tracks: List<Track>, languageCode: String): List<Track> {
        val limiter = Semaphore(IMPORT_RESOLUTION_CONCURRENCY)
        return coroutineScope {
            tracks.take(MAX_JSON_IMPORT_TRACKS).map { track ->
                async {
                    if (YOUTUBE_VIDEO_ID.matches(track.id)) return@async track
                    limiter.withPermit {
                        try {
                            val resolved = resolveBestTrack(
                                track.title,
                                track.artist,
                                track.durationMs,
                                languageCode
                            ) ?: return@withPermit null
                            resolved.copy(
                                thumbnailUrl = track.thumbnailUrl.ifBlank { resolved.thumbnailUrl },
                                largeThumbnailUrl = track.largeThumbnailUrl.ifBlank {
                                    track.thumbnailUrl.ifBlank { resolved.largeThumbnailUrl.ifBlank { resolved.thumbnailUrl } }
                                },
                                durationMs = resolved.durationMs.takeIf { it > 0L } ?: track.durationMs,
                                source = "Imported playlist"
                            )
                        } catch (error: CancellationException) {
                            throw error
                        } catch (_: Throwable) {
                            null
                        }
                    }
                }
            }.awaitAll().filterNotNull()
        }
    }

    private suspend fun resolveBestTrack(
        title: String,
        artist: String,
        durationMs: Long,
        languageCode: String
    ): Track? {
        val query = listOf(title, artist).filter(String::isNotBlank).joinToString(" ")
        if (query.isBlank()) return null
        val candidates = youtubeRepository.search(query, IMPORT_CANDIDATE_LIMIT, languageCode)
        return bestPlaylistImportCandidate(title, artist, durationMs, candidates)
    }

    private fun parseJsonArrayToTracks(array: JSONArray, outTracks: MutableList<Track>) {
        val boundedLength = minOf(array.length(), MAX_JSON_IMPORT_TRACKS)
        for (index in 0 until boundedLength) {
            val item = array.optJSONObject(index) ?: continue
            val id = item.optString("videoId").ifBlank { item.optString("id") }.trim()
            val title = item.optString("title").ifBlank { item.optString("name") }.trim()
            if (id.isBlank() || title.isBlank()) continue

            val artist = importedArtist(item)
            val album = item.optString("album").ifBlank { item.optString("albumName") }.trim()
            val thumb = item.optString("thumbnailUrl")
                .ifBlank { item.optString("artworkUrl") }
                .ifBlank { item.optString("thumbnail") }
                .trim()
            outTracks += Track(
                id = id,
                title = title,
                artist = artist,
                album = album,
                durationMs = importedDurationMs(item),
                streamUrl = "",
                videoUrl = if (YOUTUBE_VIDEO_ID.matches(id)) "https://www.youtube.com/watch?v=$id" else "",
                thumbnailUrl = thumb,
                largeThumbnailUrl = thumb,
                source = "Imported playlist",
                moodTags = setOf("music", "imported"),
                energy = 50,
                vocal = 50,
                replayScore = 50,
                cacheScore = 50,
                accentStart = 0,
                accentEnd = 0
            )
        }
    }

    private fun importedArtist(item: JSONObject): String {
        val direct = item.optString("artist").ifBlank { item.optString("artistName") }.trim()
        if (direct.isNotBlank()) return direct
        val artists = item.optJSONArray("artists") ?: return ""
        return buildList {
            for (index in 0 until artists.length()) {
                val value = artists.opt(index)
                val name = when (value) {
                    is JSONObject -> value.optString("name")
                    is String -> value
                    else -> ""
                }.trim()
                if (name.isNotBlank()) add(name)
            }
        }.distinct().joinToString(", ")
    }

    private suspend fun persistPlaylist(
        name: String,
        tracks: List<Track>,
        requestedCount: Int
    ): PlaylistImportResult {
        val cleanTracks = tracks
            .filter { it.id.isNotBlank() && it.title.isNotBlank() }
            .distinctBy { it.id }
        if (cleanTracks.isEmpty()) return PlaylistImportResult.Failure(PlaylistImportFailureKind.NO_MATCHES)

        return try {
            val created = playlistStore.createWithTracks(name, cleanTracks)
            PlaylistImportResult.Success(created, cleanTracks.size, requestedCount.coerceAtLeast(cleanTracks.size))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.w(error, "Unable to persist imported playlist")
            PlaylistImportResult.Failure(PlaylistImportFailureKind.STORAGE)
        }
    }

    private suspend fun fetchSpotifyText(url: String): String {
        var currentUrl = validateSpotifyImportUrl(url)
            ?: throw PlaylistImportException(PlaylistImportFailureKind.INVALID_INPUT, "Invalid Spotify URL")
        var redirectCount = 0

        while (true) {
            val request = Request.Builder()
                .url(currentUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/126 Mobile Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml")
                .get()
                .build()
            val response = executeSpotifyRequest(request)
            try {
                if (response.code in SPOTIFY_REDIRECT_CODES) {
                    if (redirectCount >= MAX_SPOTIFY_REDIRECTS) {
                        throw PlaylistImportException(PlaylistImportFailureKind.PROVIDER_CHANGED, "Too many Spotify redirects")
                    }
                    val location = response.header("Location")
                        ?: throw PlaylistImportException(PlaylistImportFailureKind.PROVIDER_CHANGED, "Missing Spotify redirect location")
                    val next = currentUrl.resolve(location)
                        ?: throw PlaylistImportException(PlaylistImportFailureKind.PROVIDER_CHANGED, "Invalid Spotify redirect")
                    currentUrl = validateSpotifyImportUrl(next.toString())
                        ?: throw PlaylistImportException(PlaylistImportFailureKind.PROVIDER_CHANGED, "Spotify redirect not allowed")
                    redirectCount += 1
                    continue
                }

                if (response.code == 403 || response.code == 404) {
                    throw PlaylistImportException(PlaylistImportFailureKind.NOT_AVAILABLE, "Spotify playlist unavailable")
                }
                if (response.code == 429 || response.code >= 500) {
                    throw PlaylistImportException(PlaylistImportFailureKind.NETWORK, "Spotify service temporarily unavailable")
                }
                if (!response.isSuccessful) {
                    throw PlaylistImportException(PlaylistImportFailureKind.PROVIDER_CHANGED, "Unexpected Spotify response ${response.code}")
                }

                val body = response.body
                if (!spotifyHtmlContentTypeAccepted(body.contentType()?.toString())) {
                    throw PlaylistImportException(PlaylistImportFailureKind.PROVIDER_CHANGED, "Spotify response is not HTML")
                }
                val declaredLength = body.contentLength()
                if (declaredLength > MAX_SPOTIFY_HTML_BYTES) {
                    throw PlaylistImportException(PlaylistImportFailureKind.PROVIDER_CHANGED, "Spotify response is too large")
                }
                val html = body.byteStream().use { input ->
                    readUtf8Bounded(input, MAX_SPOTIFY_HTML_BYTES)
                        ?: throw PlaylistImportException(PlaylistImportFailureKind.PROVIDER_CHANGED, "Spotify response is too large")
                }
                return spotifyHeadOnly(html)
            } finally {
                response.close()
            }
        }
    }

    private suspend fun executeSpotifyRequest(request: Request): Response = suspendCancellableCoroutine { continuation ->
        val call = spotifyHttpClient.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, error: IOException) {
                if (continuation.isActive) {
                    continuation.resumeWithException(error)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!continuation.isActive) {
                    response.close()
                    return
                }
                continuation.resume(response)
            }
        })
    }

    private fun isYoutubeUrl(value: String): Boolean {
        val uri = runCatching { URI(value) }.getOrNull() ?: return false
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        val host = uri.host.orEmpty().lowercase(Locale.ROOT)
        return host == "youtube.com" || host.endsWith(".youtube.com") || host == "youtu.be"
    }

    private fun isSpotifyUrl(value: String): Boolean = validateSpotifyImportUrl(value) != null

    private fun extractQueryParam(url: String, key: String): String {
        val rawQuery = runCatching { URI(url).rawQuery.orEmpty() }.getOrDefault("")
        if (rawQuery.isBlank()) return ""
        return rawQuery.split('&')
            .mapNotNull { part ->
                val separator = part.indexOf('=')
                if (separator < 0) null else part.substring(0, separator) to part.substring(separator + 1)
            }
            .firstOrNull { (name, _) -> name == key }
            ?.second
            ?.let { encoded -> runCatching { URLDecoder.decode(encoded, "UTF-8") }.getOrDefault(encoded) }
            .orEmpty()
    }
}

internal fun spotifyHeadOnly(html: String): String {
    val end = html.indexOf("</head>", ignoreCase = true)
    return if (end >= 0) html.substring(0, end + "</head>".length) else html
}

private val SPOTIFY_REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
private val SPOTIFY_ALLOWED_HOSTS = setOf("open.spotify.com", "spotify.com", "www.spotify.com", "spotify.link")

private fun isAllowedSpotifyHost(host: String): Boolean = host in SPOTIFY_ALLOWED_HOSTS

internal fun isPublicNetworkAddress(address: InetAddress): Boolean {
    if (
        address.isAnyLocalAddress ||
        address.isLoopbackAddress ||
        address.isLinkLocalAddress ||
        address.isSiteLocalAddress ||
        address.isMulticastAddress
    ) return false

    val bytes = address.address
    val ipv4Bytes = when {
        bytes.size == 4 -> bytes
        bytes.size == 16 &&
            bytes.take(10).all { it == 0.toByte() } &&
            bytes[10] == 0xFF.toByte() &&
            bytes[11] == 0xFF.toByte() -> bytes.copyOfRange(12, 16)
        else -> null
    }
    if (ipv4Bytes != null) return isPublicIpv4Address(ipv4Bytes)
    if (bytes.size != 16) return false

    val first = bytes[0].toInt() and 0xFF
    val second = bytes[1].toInt() and 0xFF
    if ((first and 0xFE) == 0xFC) return false
    if (first == 0x01 && second == 0x00 && bytes.drop(2).take(6).all { it == 0.toByte() }) return false
    if (first == 0x20 && second == 0x01) {
        val third = bytes[2].toInt() and 0xFF
        val fourth = bytes[3].toInt() and 0xFF
        if (third <= 0x01) return false
        if (third == 0x0D && fourth == 0xB8) return false
    }
    if (first == 0x20 && second == 0x02) return false
    if (first == 0x3F && (second and 0xF0) == 0xF0) return false
    return true
}

private fun isPublicIpv4Address(bytes: ByteArray): Boolean {
    if (bytes.size != 4) return false
    val first = bytes[0].toInt() and 0xFF
    val second = bytes[1].toInt() and 0xFF
    val third = bytes[2].toInt() and 0xFF
    return when {
        first == 0 -> false
        first == 10 -> false
        first == 100 && second in 64..127 -> false
        first == 127 -> false
        first == 169 && second == 254 -> false
        first == 172 && second in 16..31 -> false
        first == 192 && second == 0 && third == 0 -> false
        first == 192 && second == 0 && third == 2 -> false
        first == 192 && second == 31 && third == 196 -> false
        first == 192 && second == 52 && third == 193 -> false
        first == 192 && second == 88 && third == 99 -> false
        first == 192 && second == 168 -> false
        first == 192 && second == 175 && third == 48 -> false
        first == 198 && second in 18..19 -> false
        first == 198 && second == 51 && third == 100 -> false
        first == 203 && second == 0 && third == 113 -> false
        first >= 224 -> false
        else -> true
    }
}
