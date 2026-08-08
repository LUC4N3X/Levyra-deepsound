from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def write(path: str, content: str) -> None:
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content.rstrip() + "\n", encoding="utf-8")


def replace_exact(path: str, old: str, new: str, count: int = 1) -> None:
    target = ROOT / path
    source = target.read_text(encoding="utf-8")
    found = source.count(old)
    if found != count:
        raise SystemExit(f"{path}: expected {count} occurrence(s), found {found}: {old[:120]!r}")
    target.write_text(source.replace(old, new, count), encoding="utf-8")


write(
    "app/src/main/java/com/luc4n3x/levyra/domain/PlaylistImportFailureKind.kt",
    '''package com.luc4n3x.levyra.domain

enum class PlaylistImportFailureKind {
    INVALID_INPUT,
    NOT_AVAILABLE,
    TOO_LARGE,
    NO_MATCHES,
    NETWORK,
    PROVIDER_CHANGED,
    STORAGE
}
'''
)

write(
    "app/src/main/java/com/luc4n3x/levyra/data/SponsorBlockRepository.kt",
    '''package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.SponsorSegment
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * SponsorBlock community segments (sponsor.ajay.app) so LEVYRA can auto-skip the
 * non-music parts of YouTube videos — sponsor reads, intros, outros, etc.
 */
class SponsorBlockRepository internal constructor(
    private val fetcher: SponsorBlockHttpFetcher,
    private val clockMs: () -> Long
) {
    constructor() : this(UrlConnectionSponsorBlockHttpFetcher(), System::currentTimeMillis)

    private val categories = listOf("sponsor", "selfpromo", "intro", "outro", "interaction", "music_offtopic", "preview")

    private val cache = object : java.util.LinkedHashMap<String, SponsorBlockCacheEntry>(128, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, SponsorBlockCacheEntry>?): Boolean {
            return size > SPONSORBLOCK_CACHE_LIMIT
        }
    }

    suspend fun segments(videoId: String): List<SponsorSegment> = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) return@withContext emptyList()
        cachedSponsorBlockResult(cache, videoId, clockMs())?.let { return@withContext it }

        val catsJson = categories.joinToString(",", prefix = "[", postfix = "]") { "\"$it\"" }
        val cats = URLEncoder.encode(catsJson, "UTF-8")
        val url = "https://sponsor.ajay.app/api/skipSegments?videoID=$videoId&categories=$cats"

        val response = runCatching { fetcher.fetch(url) }.getOrNull() ?: return@withContext emptyList()
        response.use {
            when {
                response.code == HttpURLConnection.HTTP_NOT_FOUND -> {
                    return@withContext publishSponsorBlockCacheResult(cache, videoId, emptyList(), clockMs())
                }
                response.code !in 200..299 -> return@withContext emptyList()
                response.declaredLength > SPONSORBLOCK_MAX_RESPONSE_BYTES -> return@withContext emptyList()
            }

            val input = response.body ?: return@withContext emptyList()
            val body = readUtf8Bounded(input, SPONSORBLOCK_MAX_RESPONSE_BYTES)
                ?: return@withContext emptyList()
            val array = runCatching { JSONArray(body) }.getOrNull() ?: return@withContext emptyList()
            val parsed = buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val range = item.optJSONArray("segment") ?: continue
                    if (range.length() < 2) continue
                    val startMs = (range.optDouble(0, 0.0) * 1000).toLong()
                    val endMs = (range.optDouble(1, 0.0) * 1000).toLong()
                    if (endMs > startMs) {
                        add(SponsorSegment(startMs, endMs, item.optString("category", "sponsor")))
                    }
                }
            }.sortedBy { it.startMs }
            publishSponsorBlockCacheResult(cache, videoId, parsed, clockMs())
        }
    }
}

internal const val SPONSORBLOCK_MAX_RESPONSE_BYTES = 512L * 1024L
internal const val SPONSORBLOCK_CACHE_LIMIT = 200
internal const val SPONSORBLOCK_NEGATIVE_TTL_MS = 2L * 60L * 1000L
internal const val SPONSORBLOCK_POSITIVE_TTL_MS = 30L * 60L * 1000L

internal data class SponsorBlockCacheEntry(
    val segments: List<SponsorSegment>,
    val expiresAtMs: Long
)

internal fun cachedSponsorBlockResult(
    cache: MutableMap<String, SponsorBlockCacheEntry>,
    videoId: String,
    nowMs: Long
): List<SponsorSegment>? = synchronized(cache) {
    val entry = cache[videoId] ?: return@synchronized null
    if (entry.expiresAtMs <= nowMs) {
        cache.remove(videoId)
        null
    } else {
        entry.segments
    }
}

internal fun publishSponsorBlockCacheResult(
    cache: MutableMap<String, SponsorBlockCacheEntry>,
    videoId: String,
    result: List<SponsorSegment>,
    nowMs: Long
): List<SponsorSegment> = synchronized(cache) {
    val existing = cache[videoId]?.takeIf { it.expiresAtMs > nowMs }
    when {
        existing == null -> {
            cache[videoId] = SponsorBlockCacheEntry(result, nowMs + sponsorBlockTtl(result))
            result
        }
        existing.segments.isNotEmpty() -> existing.segments
        result.isNotEmpty() -> {
            cache[videoId] = SponsorBlockCacheEntry(result, nowMs + SPONSORBLOCK_POSITIVE_TTL_MS)
            result
        }
        else -> existing.segments
    }
}

private fun sponsorBlockTtl(segments: List<SponsorSegment>): Long =
    if (segments.isEmpty()) SPONSORBLOCK_NEGATIVE_TTL_MS else SPONSORBLOCK_POSITIVE_TTL_MS

internal fun interface SponsorBlockHttpFetcher {
    fun fetch(url: String): SponsorBlockHttpResponse
}

internal class SponsorBlockHttpResponse(
    val code: Int,
    val declaredLength: Long,
    val body: InputStream?,
    private val closeAction: () -> Unit = {}
) : Closeable {
    override fun close() {
        runCatching { body?.close() }
        closeAction()
    }
}

private class UrlConnectionSponsorBlockHttpFetcher : SponsorBlockHttpFetcher {
    override fun fetch(url: String): SponsorBlockHttpResponse {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 9000
            readTimeout = 11000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "LEVYRA Music Player (Android)")
        }
        return try {
            val code = connection.responseCode
            val body = if (code in 200..299) connection.inputStream else null
            SponsorBlockHttpResponse(
                code = code,
                declaredLength = connection.contentLengthLong,
                body = body,
                closeAction = connection::disconnect
            )
        } catch (error: Throwable) {
            connection.disconnect()
            throw error
        }
    }
}

internal fun readUtf8Bounded(input: InputStream, maxBytes: Long): String? {
    require(maxBytes > 0L)
    val output = ByteArrayOutputStream(minOf(maxBytes, 16L * 1024L).toInt())
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        total += read
        if (total > maxBytes) return null
        output.write(buffer, 0, read)
    }
    return output.toString(StandardCharsets.UTF_8.name())
}
'''
)

write(
    "app/src/main/java/com/luc4n3x/levyra/data/UniversalPlaylistImporter.kt",
    '''package com.luc4n3x.levyra.data

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
private val META_TAG_PATTERN = Regex("<meta\\b[^>]*>", RegexOption.IGNORE_CASE)
private val META_ATTRIBUTE_PATTERN = Regex(
    """([A-Za-z_:][A-Za-z0-9_.:-]*)\s*=\s*([\"'])(.*?)\2""",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
)
private val DECIMAL_HTML_ENTITY = Regex("&#(\\d+);")
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
    .replace(Regex("\\p{M}+"), "")
    .lowercase(Locale.ROOT)
    .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
    .replace(Regex("\\s+"), " ")
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
                val token = continuation.tryResumeWithException(error) ?: return
                continuation.completeResume(token)
            }

            override fun onResponse(call: Call, response: Response) {
                val token = continuation.tryResume(response)
                if (token == null) {
                    response.close()
                    return
                }
                continuation.completeResume(token)
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
'''
)

write(
    "app/src/main/java/com/luc4n3x/levyra/ui/i18n/PlaylistImportStatusStrings.kt",
    '''package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.PlaylistImportFailureKind

fun playlistImportStartedMessage(code: String): String = when (code) {
    "it" -> "Importazione playlist in corso…"
    "es" -> "Importando la playlist…"
    "fr" -> "Importation de la playlist…"
    "de" -> "Playlist wird importiert…"
    "pt" -> "A importar a playlist…"
    "nl" -> "Playlist wordt geïmporteerd…"
    "pl" -> "Importowanie playlisty…"
    "ro" -> "Se importă playlistul…"
    "el" -> "Γίνεται εισαγωγή της playlist…"
    "sv" -> "Importerar spellista…"
    "da" -> "Importerer playliste…"
    "cs" -> "Importuje se playlist…"
    "uk" -> "Імпорт плейлиста…"
    "ru" -> "Импорт плейлиста…"
    "tr" -> "Çalma listesi içe aktarılıyor…"
    "ar" -> "جارٍ استيراد قائمة التشغيل…"
    "zh" -> "正在导入播放列表…"
    "ja" -> "プレイリストをインポート中…"
    "ko" -> "플레이리스트를 가져오는 중…"
    "hi" -> "प्लेलिस्ट आयात की जा रही है…"
    "id" -> "Mengimpor playlist…"
    "vi" -> "Đang nhập playlist…"
    "th" -> "กำลังนำเข้าเพลย์ลิสต์…"
    "fil" -> "Ini-import ang playlist…"
    "he" -> "מייבא פלייליסט…"
    else -> "Importing playlist…"
}

fun playlistImportAlreadyRunningMessage(code: String): String = when (code) {
    "it" -> "Un’importazione è già in corso."
    "es" -> "Ya hay una importación en curso."
    "fr" -> "Une importation est déjà en cours."
    "de" -> "Es läuft bereits ein Import."
    "pt" -> "Já existe uma importação em curso."
    "nl" -> "Er wordt al een playlist geïmporteerd."
    "pl" -> "Import playlisty już trwa."
    "ro" -> "Un import este deja în curs."
    "el" -> "Μια εισαγωγή βρίσκεται ήδη σε εξέλιξη."
    "sv" -> "En import pågår redan."
    "da" -> "En import er allerede i gang."
    "cs" -> "Import již probíhá."
    "uk" -> "Імпорт уже виконується."
    "ru" -> "Импорт уже выполняется."
    "tr" -> "Zaten devam eden bir içe aktarma var."
    "ar" -> "هناك عملية استيراد قيد التنفيذ بالفعل."
    "zh" -> "已有导入任务正在进行。"
    "ja" -> "すでにインポートを実行中です。"
    "ko" -> "이미 가져오기가 진행 중입니다."
    "hi" -> "एक आयात पहले से चल रहा है।"
    "id" -> "Proses impor sedang berlangsung."
    "vi" -> "Một quá trình nhập đang diễn ra."
    "th" -> "มีการนำเข้าอยู่แล้ว"
    "fil" -> "May kasalukuyan nang pag-import."
    "he" -> "ייבוא כבר מתבצע."
    else -> "An import is already in progress."
}

fun playlistImportSuccessMessage(code: String, count: Int, playlistName: String): String = when (code) {
    "it" -> "Importati $count brani in $playlistName"
    "es" -> "Se importaron $count canciones a $playlistName"
    "fr" -> "$count titres importés dans $playlistName"
    "de" -> "$count Titel in $playlistName importiert"
    "pt" -> "$count faixas importadas para $playlistName"
    "nl" -> "$count nummers geïmporteerd in $playlistName"
    "pl" -> "Zaimportowano $count utworów do $playlistName"
    "ro" -> "Au fost importate $count piese în $playlistName"
    "el" -> "Εισήχθησαν $count κομμάτια στο $playlistName"
    "sv" -> "$count låtar importerades till $playlistName"
    "da" -> "$count numre importeret til $playlistName"
    "cs" -> "Do $playlistName bylo importováno $count skladeb"
    "uk" -> "Імпортовано $count композицій у $playlistName"
    "ru" -> "Импортировано $count треков в $playlistName"
    "tr" -> "$playlistName içine $count parça aktarıldı"
    "ar" -> "تم استيراد $count مقطعًا إلى $playlistName"
    "zh" -> "已将 $count 首歌曲导入 $playlistName"
    "ja" -> "$playlistName に $count 曲をインポートしました"
    "ko" -> "${playlistName}에 ${count}곡을 가져왔습니다"
    "hi" -> "$playlistName में $count गाने आयात किए गए"
    "id" -> "$count lagu diimpor ke $playlistName"
    "vi" -> "Đã nhập $count bài hát vào $playlistName"
    "th" -> "นำเข้า $count เพลงไปยัง $playlistName แล้ว"
    "fil" -> "Na-import ang $count kanta sa $playlistName"
    "he" -> "יובאו $count שירים אל $playlistName"
    else -> "Imported $count tracks into $playlistName"
}

fun playlistImportSuccessMessage(
    code: String,
    importedCount: Int,
    requestedCount: Int,
    playlistName: String
): String {
    if (requestedCount <= importedCount) return playlistImportSuccessMessage(code, importedCount, playlistName)
    return when (code) {
        "it" -> "Importati $importedCount di $requestedCount brani in $playlistName"
        "es" -> "Se importaron $importedCount de $requestedCount canciones a $playlistName"
        "fr" -> "$importedCount titres sur $requestedCount importés dans $playlistName"
        "de" -> "$importedCount von $requestedCount Titeln in $playlistName importiert"
        "pt" -> "$importedCount de $requestedCount faixas importadas para $playlistName"
        "nl" -> "$importedCount van $requestedCount nummers geïmporteerd in $playlistName"
        "pl" -> "Zaimportowano $importedCount z $requestedCount utworów do $playlistName"
        "ro" -> "Au fost importate $importedCount din $requestedCount piese în $playlistName"
        "el" -> "Εισήχθησαν $importedCount από $requestedCount κομμάτια στο $playlistName"
        "sv" -> "$importedCount av $requestedCount låtar importerades till $playlistName"
        "da" -> "$importedCount af $requestedCount numre importeret til $playlistName"
        "cs" -> "Do $playlistName bylo importováno $importedCount z $requestedCount skladeb"
        "uk" -> "Імпортовано $importedCount із $requestedCount композицій у $playlistName"
        "ru" -> "Импортировано $importedCount из $requestedCount треков в $playlistName"
        "tr" -> "$requestedCount parçadan $importedCount tanesi $playlistName içine aktarıldı"
        "ar" -> "تم استيراد $importedCount من أصل $requestedCount مقطعًا إلى $playlistName"
        "zh" -> "已将 $requestedCount 首中的 $importedCount 首导入 $playlistName"
        "ja" -> "$requestedCount 曲中 $importedCount 曲を $playlistName にインポートしました"
        "ko" -> "$requestedCount곡 중 $importedCount곡을 $playlistName에 가져왔습니다"
        "hi" -> "$requestedCount में से $importedCount गाने $playlistName में आयात किए गए"
        "id" -> "$importedCount dari $requestedCount lagu diimpor ke $playlistName"
        "vi" -> "Đã nhập $importedCount trong số $requestedCount bài hát vào $playlistName"
        "th" -> "นำเข้า $importedCount จาก $requestedCount เพลงไปยัง $playlistName แล้ว"
        "fil" -> "Na-import ang $importedCount sa $requestedCount kanta sa $playlistName"
        "he" -> "יובאו $importedCount מתוך $requestedCount שירים אל $playlistName"
        else -> "Imported $importedCount of $requestedCount tracks into $playlistName"
    }
}

fun playlistImportFailureMessage(code: String): String = when (code) {
    "it" -> "Importazione non riuscita. Controlla il link o il backup e riprova."
    "es" -> "No se pudo importar. Comprueba el enlace o la copia de seguridad e inténtalo de nuevo."
    "fr" -> "L’importation a échoué. Vérifiez le lien ou la sauvegarde, puis réessayez."
    "de" -> "Import fehlgeschlagen. Prüfe den Link oder die Sicherung und versuche es erneut."
    "pt" -> "Não foi possível importar. Verifique o link ou a cópia de segurança e tente novamente."
    "nl" -> "Importeren mislukt. Controleer de link of back-up en probeer het opnieuw."
    "pl" -> "Import nie powiódł się. Sprawdź link lub kopię zapasową i spróbuj ponownie."
    "ro" -> "Importul nu a reușit. Verifică linkul sau copia de siguranță și încearcă din nou."
    "el" -> "Η εισαγωγή απέτυχε. Έλεγξε τον σύνδεσμο ή το αντίγραφο ασφαλείας και δοκίμασε ξανά."
    "sv" -> "Importen misslyckades. Kontrollera länken eller säkerhetskopian och försök igen."
    "da" -> "Importen mislykkedes. Kontrollér linket eller sikkerhedskopien, og prøv igen."
    "cs" -> "Import se nezdařil. Zkontrolujte odkaz nebo zálohu a zkuste to znovu."
    "uk" -> "Не вдалося імпортувати. Перевірте посилання або резервну копію й спробуйте ще раз."
    "ru" -> "Не удалось импортировать. Проверьте ссылку или резервную копию и повторите попытку."
    "tr" -> "İçe aktarma başarısız oldu. Bağlantıyı veya yedeği kontrol edip tekrar deneyin."
    "ar" -> "تعذّر الاستيراد. تحقّق من الرابط أو النسخة الاحتياطية ثم حاول مرة أخرى."
    "zh" -> "导入失败。请检查链接或备份内容后重试。"
    "ja" -> "インポートできませんでした。リンクまたはバックアップを確認して、もう一度お試しください。"
    "ko" -> "가져오기에 실패했습니다. 링크 또는 백업을 확인한 후 다시 시도하세요."
    "hi" -> "आयात नहीं हो सका। लिंक या बैकअप जाँचें और फिर से कोशिश करें।"
    "id" -> "Impor gagal. Periksa tautan atau cadangan lalu coba lagi."
    "vi" -> "Không thể nhập. Hãy kiểm tra liên kết hoặc bản sao lưu rồi thử lại."
    "th" -> "นำเข้าไม่สำเร็จ โปรดตรวจสอบลิงก์หรือข้อมูลสำรองแล้วลองอีกครั้ง"
    "fil" -> "Hindi na-import. Suriin ang link o backup at subukan ulit."
    "he" -> "הייבוא נכשל. בדקו את הקישור או את הגיבוי ונסו שוב."
    else -> "Import failed. Check the link or backup and try again."
}

private data class PlaylistImportFailureCopy(
    val invalidInput: String,
    val notAvailable: String,
    val tooLarge: String,
    val noMatches: String,
    val network: String,
    val providerChanged: String,
    val storage: String,
    val dismiss: String
)

private fun playlistImportFailureCopy(code: String): PlaylistImportFailureCopy = when (code) {
    "it" -> PlaylistImportFailureCopy("Il link o il backup non è riconosciuto. Controllalo e riprova.", "Non riesco a leggere questa playlist. Verifica che sia pubblica e accessibile.", "Questa playlist supera il limite di importazione supportato{limit}.", "Nessun brano compatibile è stato riconosciuto nel catalogo Levyra.", "Errore di rete durante l’importazione. Controlla la connessione e riprova.", "Il servizio musicale ha restituito un formato non riconosciuto. Riprova più tardi o usa un backup compatibile.", "Non è stato possibile salvare la playlist. Riprova.", "Nascondi suggerimento di importazione")
    "es" -> PlaylistImportFailureCopy("No se reconoce el enlace o la copia de seguridad. Compruébalo e inténtalo de nuevo.", "No se puede leer esta playlist. Comprueba que sea pública y accesible.", "Esta playlist supera el límite de importación admitido{limit}.", "No se pudo identificar ninguna canción compatible en el catálogo de Levyra.", "Se produjo un error de red durante la importación. Comprueba la conexión e inténtalo de nuevo.", "El servicio de música devolvió un formato no reconocido. Inténtalo más tarde o utiliza una copia de seguridad compatible.", "No se pudo guardar la playlist. Inténtalo de nuevo.", "Ocultar sugerencia de importación")
    "fr" -> PlaylistImportFailureCopy("Le lien ou la sauvegarde n’est pas reconnu. Vérifiez-le puis réessayez.", "Impossible de lire cette playlist. Vérifiez qu’elle est publique et accessible.", "Cette playlist dépasse la limite d’importation prise en charge{limit}.", "Aucun titre compatible n’a pu être identifié dans le catalogue Levyra.", "Une erreur réseau s’est produite pendant l’importation. Vérifiez votre connexion puis réessayez.", "Le service musical a renvoyé un format non reconnu. Réessayez plus tard ou utilisez une sauvegarde compatible.", "Impossible d’enregistrer la playlist. Réessayez.", "Masquer la suggestion d’importation")
    "de" -> PlaylistImportFailureCopy("Der Link oder die Sicherung wird nicht erkannt. Prüfe die Eingabe und versuche es erneut.", "Diese Playlist kann nicht gelesen werden. Prüfe, ob sie öffentlich und erreichbar ist.", "Diese Playlist überschreitet das unterstützte Importlimit{limit}.", "Im Levyra-Katalog konnten keine passenden Titel erkannt werden.", "Beim Import ist ein Netzwerkfehler aufgetreten. Prüfe die Verbindung und versuche es erneut.", "Der Musikdienst hat ein unbekanntes Format geliefert. Versuche es später erneut oder nutze eine kompatible Sicherung.", "Die Playlist konnte nicht gespeichert werden. Versuche es erneut.", "Importhinweis ausblenden")
    "pt" -> PlaylistImportFailureCopy("O link ou a cópia de segurança não foi reconhecido. Verifique e tente novamente.", "Não foi possível ler esta playlist. Verifique se é pública e está acessível.", "Esta playlist excede o limite de importação suportado{limit}.", "Não foi possível identificar faixas compatíveis no catálogo Levyra.", "Ocorreu um erro de rede durante a importação. Verifique a ligação e tente novamente.", "O serviço de música devolveu um formato não reconhecido. Tente mais tarde ou use uma cópia de segurança compatível.", "Não foi possível guardar a playlist. Tente novamente.", "Ocultar sugestão de importação")
    "nl" -> PlaylistImportFailureCopy("De link of back-up wordt niet herkend. Controleer deze en probeer opnieuw.", "Deze playlist kan niet worden gelezen. Controleer of deze openbaar en bereikbaar is.", "Deze playlist is groter dan de ondersteunde importlimiet{limit}.", "Er zijn geen geschikte nummers in de Levyra-catalogus gevonden.", "Er is een netwerkfout opgetreden tijdens het importeren. Controleer je verbinding en probeer opnieuw.", "De muziekdienst stuurde een onbekend formaat terug. Probeer het later opnieuw of gebruik een compatibele back-up.", "De playlist kon niet worden opgeslagen. Probeer opnieuw.", "Importsuggestie verbergen")
    "pl" -> PlaylistImportFailureCopy("Link lub kopia zapasowa nie zostały rozpoznane. Sprawdź je i spróbuj ponownie.", "Nie można odczytać tej playlisty. Sprawdź, czy jest publiczna i dostępna.", "Ta playlista przekracza obsługiwany limit importu{limit}.", "Nie udało się dopasować żadnych zgodnych utworów w katalogu Levyra.", "Podczas importu wystąpił błąd sieci. Sprawdź połączenie i spróbuj ponownie.", "Serwis muzyczny zwrócił nierozpoznany format. Spróbuj później lub użyj zgodnej kopii zapasowej.", "Nie udało się zapisać playlisty. Spróbuj ponownie.", "Ukryj sugestię importu")
    "ro" -> PlaylistImportFailureCopy("Linkul sau copia de siguranță nu este recunoscută. Verifică și încearcă din nou.", "Acest playlist nu poate fi citit. Verifică dacă este public și accesibil.", "Acest playlist depășește limita de import acceptată{limit}.", "Nu au fost identificate piese compatibile în catalogul Levyra.", "A apărut o eroare de rețea în timpul importului. Verifică conexiunea și încearcă din nou.", "Serviciul muzical a returnat un format nerecunoscut. Încearcă mai târziu sau folosește o copie de siguranță compatibilă.", "Playlistul nu a putut fi salvat. Încearcă din nou.", "Ascunde sugestia de import")
    "el" -> PlaylistImportFailureCopy("Ο σύνδεσμος ή το αντίγραφο ασφαλείας δεν αναγνωρίζεται. Έλεγξέ το και δοκίμασε ξανά.", "Δεν είναι δυνατή η ανάγνωση αυτής της playlist. Βεβαιώσου ότι είναι δημόσια και προσβάσιμη.", "Αυτή η playlist υπερβαίνει το υποστηριζόμενο όριο εισαγωγής{limit}.", "Δεν εντοπίστηκαν συμβατά κομμάτια στον κατάλογο Levyra.", "Παρουσιάστηκε σφάλμα δικτύου κατά την εισαγωγή. Έλεγξε τη σύνδεσή σου και δοκίμασε ξανά.", "Η μουσική υπηρεσία επέστρεψε μη αναγνωρίσιμη μορφή. Δοκίμασε αργότερα ή χρησιμοποίησε συμβατό αντίγραφο ασφαλείας.", "Δεν ήταν δυνατή η αποθήκευση της playlist. Δοκίμασε ξανά.", "Απόκρυψη πρότασης εισαγωγής")
    "sv" -> PlaylistImportFailureCopy("Länken eller säkerhetskopian känns inte igen. Kontrollera den och försök igen.", "Spellistan kan inte läsas. Kontrollera att den är offentlig och tillgänglig.", "Spellistan överskrider den importgräns som stöds{limit}.", "Inga kompatibla låtar kunde matchas i Levyra-katalogen.", "Ett nätverksfel inträffade under importen. Kontrollera anslutningen och försök igen.", "Musiktjänsten returnerade ett okänt format. Försök senare eller använd en kompatibel säkerhetskopia.", "Spellistan kunde inte sparas. Försök igen.", "Dölj importförslaget")
    "da" -> PlaylistImportFailureCopy("Linket eller sikkerhedskopien genkendes ikke. Kontrollér den, og prøv igen.", "Denne playliste kan ikke læses. Kontrollér, at den er offentlig og tilgængelig.", "Denne playliste overskrider den understøttede importgrænse{limit}.", "Ingen kompatible numre kunne matches i Levyra-kataloget.", "Der opstod en netværksfejl under importen. Kontrollér forbindelsen, og prøv igen.", "Musiktjenesten returnerede et ukendt format. Prøv igen senere, eller brug en kompatibel sikkerhedskopi.", "Playlisten kunne ikke gemmes. Prøv igen.", "Skjul importforslaget")
    "cs" -> PlaylistImportFailureCopy("Odkaz nebo záloha nebyly rozpoznány. Zkontrolujte je a zkuste to znovu.", "Tento playlist nelze načíst. Ověřte, že je veřejný a dostupný.", "Tento playlist překračuje podporovaný limit importu{limit}.", "V katalogu Levyra nebyly nalezeny žádné odpovídající skladby.", "Během importu došlo k chybě sítě. Zkontrolujte připojení a zkuste to znovu.", "Hudební služba vrátila nerozpoznaný formát. Zkuste to později nebo použijte kompatibilní zálohu.", "Playlist se nepodařilo uložit. Zkuste to znovu.", "Skrýt návrh importu")
    "uk" -> PlaylistImportFailureCopy("Посилання або резервну копію не розпізнано. Перевірте їх і спробуйте ще раз.", "Не вдається прочитати цей плейлист. Переконайтеся, що він загальнодоступний і доступний.", "Цей плейлист перевищує підтримуваний ліміт імпорту{limit}.", "У каталозі Levyra не вдалося знайти сумісні композиції.", "Під час імпорту сталася помилка мережі. Перевірте з’єднання й спробуйте ще раз.", "Музичний сервіс повернув нерозпізнаний формат. Спробуйте пізніше або скористайтеся сумісною резервною копією.", "Не вдалося зберегти плейлист. Спробуйте ще раз.", "Сховати підказку імпорту")
    "ru" -> PlaylistImportFailureCopy("Ссылка или резервная копия не распознаны. Проверьте их и повторите попытку.", "Не удаётся прочитать этот плейлист. Убедитесь, что он общедоступен и доступен.", "Этот плейлист превышает поддерживаемый лимит импорта{limit}.", "В каталоге Levyra не удалось найти подходящие треки.", "Во время импорта произошла сетевая ошибка. Проверьте подключение и повторите попытку.", "Музыкальный сервис вернул неизвестный формат. Попробуйте позже или используйте совместимую резервную копию.", "Не удалось сохранить плейлист. Повторите попытку.", "Скрыть подсказку импорта")
    "tr" -> PlaylistImportFailureCopy("Bağlantı veya yedek tanınmadı. Kontrol edip tekrar deneyin.", "Bu çalma listesi okunamıyor. Herkese açık ve erişilebilir olduğundan emin olun.", "Bu çalma listesi desteklenen içe aktarma sınırını aşıyor{limit}.", "Levyra kataloğunda eşleşen uyumlu parça bulunamadı.", "İçe aktarma sırasında ağ hatası oluştu. Bağlantınızı kontrol edip tekrar deneyin.", "Müzik servisi tanınmayan bir biçim döndürdü. Daha sonra tekrar deneyin veya uyumlu bir yedek kullanın.", "Çalma listesi kaydedilemedi. Tekrar deneyin.", "İçe aktarma önerisini gizle")
    "ar" -> PlaylistImportFailureCopy("لم يتم التعرّف على الرابط أو النسخة الاحتياطية. تحقّق منهما وحاول مرة أخرى.", "تعذّرت قراءة قائمة التشغيل. تأكد من أنها عامة ومتاحة.", "تتجاوز قائمة التشغيل حد الاستيراد المدعوم{limit}.", "لم يتم العثور على مقاطع متوافقة في كتالوج Levyra.", "حدث خطأ في الشبكة أثناء الاستيراد. تحقّق من اتصالك وحاول مرة أخرى.", "أعادت خدمة الموسيقى تنسيقًا غير معروف. حاول لاحقًا أو استخدم نسخة احتياطية متوافقة.", "تعذّر حفظ قائمة التشغيل. حاول مرة أخرى.", "إخفاء اقتراح الاستيراد")
    "zh" -> PlaylistImportFailureCopy("无法识别该链接或备份。请检查后重试。", "无法读取此播放列表。请确认它是公开且可访问的。", "此播放列表超过支持的导入限制{limit}。", "无法在 Levyra 曲库中匹配到兼容歌曲。", "导入时发生网络错误。请检查网络连接后重试。", "音乐服务返回了无法识别的格式。请稍后重试或使用兼容备份。", "无法保存播放列表。请重试。", "隐藏导入提示")
    "ja" -> PlaylistImportFailureCopy("リンクまたはバックアップを認識できません。内容を確認して、もう一度お試しください。", "このプレイリストを読み込めません。公開されていてアクセス可能か確認してください。", "このプレイリストは対応しているインポート上限を超えています{limit}。", "Levyra カタログで一致する互換トラックが見つかりませんでした。", "インポート中にネットワークエラーが発生しました。接続を確認して、もう一度お試しください。", "音楽サービスから認識できない形式が返されました。後でもう一度試すか、互換バックアップを使用してください。", "プレイリストを保存できませんでした。もう一度お試しください。", "インポートの案内を非表示")
    "ko" -> PlaylistImportFailureCopy("링크 또는 백업을 인식할 수 없습니다. 확인한 후 다시 시도하세요.", "이 플레이리스트를 읽을 수 없습니다. 공개 상태이며 접근 가능한지 확인하세요.", "이 플레이리스트는 지원되는 가져오기 한도를 초과합니다{limit}.", "Levyra 카탈로그에서 일치하는 호환 곡을 찾지 못했습니다.", "가져오는 중 네트워크 오류가 발생했습니다. 연결을 확인한 후 다시 시도하세요.", "음악 서비스가 인식할 수 없는 형식을 반환했습니다. 나중에 다시 시도하거나 호환 백업을 사용하세요.", "플레이리스트를 저장할 수 없습니다. 다시 시도하세요.", "가져오기 안내 숨기기")
    "hi" -> PlaylistImportFailureCopy("लिंक या बैकअप पहचाना नहीं गया। इसे जाँचें और फिर से कोशिश करें।", "यह प्लेलिस्ट पढ़ी नहीं जा सकती। सुनिश्चित करें कि यह सार्वजनिक और उपलब्ध है।", "यह प्लेलिस्ट समर्थित आयात सीमा से बड़ी है{limit}।", "Levyra कैटलॉग में कोई संगत गाना मेल नहीं खाया।", "आयात के दौरान नेटवर्क त्रुटि हुई। कनेक्शन जाँचें और फिर से कोशिश करें।", "संगीत सेवा ने अपरिचित प्रारूप लौटाया। बाद में फिर कोशिश करें या संगत बैकअप का उपयोग करें।", "प्लेलिस्ट सहेजी नहीं जा सकी। फिर से कोशिश करें।", "आयात सुझाव छिपाएँ")
    "id" -> PlaylistImportFailureCopy("Tautan atau cadangan tidak dikenali. Periksa lalu coba lagi.", "Playlist ini tidak dapat dibaca. Pastikan playlist bersifat publik dan dapat diakses.", "Playlist ini melebihi batas impor yang didukung{limit}.", "Tidak ada lagu kompatibel yang cocok di katalog Levyra.", "Terjadi kesalahan jaringan saat mengimpor. Periksa koneksi lalu coba lagi.", "Layanan musik mengembalikan format yang tidak dikenali. Coba lagi nanti atau gunakan cadangan yang kompatibel.", "Playlist tidak dapat disimpan. Coba lagi.", "Sembunyikan saran impor")
    "vi" -> PlaylistImportFailureCopy("Không nhận dạng được liên kết hoặc bản sao lưu. Hãy kiểm tra rồi thử lại.", "Không thể đọc playlist này. Hãy đảm bảo playlist ở chế độ công khai và có thể truy cập.", "Playlist này vượt quá giới hạn nhập được hỗ trợ{limit}.", "Không tìm thấy bài hát tương thích phù hợp trong danh mục Levyra.", "Đã xảy ra lỗi mạng khi nhập. Hãy kiểm tra kết nối rồi thử lại.", "Dịch vụ nhạc trả về định dạng không nhận dạng được. Hãy thử lại sau hoặc dùng bản sao lưu tương thích.", "Không thể lưu playlist. Hãy thử lại.", "Ẩn gợi ý nhập")
    "th" -> PlaylistImportFailureCopy("ไม่รู้จักลิงก์หรือข้อมูลสำรอง โปรดตรวจสอบแล้วลองอีกครั้ง", "ไม่สามารถอ่านเพลย์ลิสต์นี้ได้ โปรดตรวจสอบว่าเป็นสาธารณะและเข้าถึงได้", "เพลย์ลิสต์นี้เกินขีดจำกัดการนำเข้าที่รองรับ{limit}", "ไม่พบเพลงที่เข้ากันได้ในแค็ตตาล็อก Levyra", "เกิดข้อผิดพลาดของเครือข่ายระหว่างนำเข้า โปรดตรวจสอบการเชื่อมต่อแล้วลองอีกครั้ง", "บริการเพลงส่งรูปแบบที่ไม่รู้จัก โปรดลองอีกครั้งภายหลังหรือใช้ข้อมูลสำรองที่เข้ากันได้", "ไม่สามารถบันทึกเพลย์ลิสต์ได้ โปรดลองอีกครั้ง", "ซ่อนคำแนะนำการนำเข้า")
    "fil" -> PlaylistImportFailureCopy("Hindi nakilala ang link o backup. Suriin ito at subukan ulit.", "Hindi mabasa ang playlist na ito. Tiyaking pampubliko at naa-access ito.", "Lampas ang playlist na ito sa suportadong limitasyon ng pag-import{limit}.", "Walang tumugmang compatible na kanta sa catalog ng Levyra.", "Nagkaroon ng network error habang nag-i-import. Suriin ang koneksyon at subukan ulit.", "Nagbalik ang music service ng hindi makilalang format. Subukan mamaya o gumamit ng compatible na backup.", "Hindi ma-save ang playlist. Subukan ulit.", "Itago ang mungkahi sa pag-import")
    "he" -> PlaylistImportFailureCopy("הקישור או הגיבוי לא זוהו. בדקו אותם ונסו שוב.", "לא ניתן לקרוא את הפלייליסט הזה. ודאו שהוא ציבורי ונגיש.", "הפלייליסט חורג ממגבלת הייבוא הנתמכת{limit}.", "לא נמצאו שירים תואמים בקטלוג Levyra.", "אירעה שגיאת רשת במהלך הייבוא. בדקו את החיבור ונסו שוב.", "שירות המוזיקה החזיר פורמט לא מזוהה. נסו שוב מאוחר יותר או השתמשו בגיבוי תואם.", "לא ניתן לשמור את הפלייליסט. נסו שוב.", "הסתרת הצעת הייבוא")
    else -> PlaylistImportFailureCopy("The link or backup is not recognized. Check it and try again.", "This playlist cannot be read. Make sure it is public and accessible.", "This playlist exceeds the supported import limit{limit}.", "No compatible tracks could be matched in the Levyra catalog.", "A network error occurred while importing. Check your connection and try again.", "The music service returned an unrecognized format. Try again later or use a compatible backup.", "The playlist could not be saved. Try again.", "Hide import suggestion")
}

fun playlistImportFailureMessage(
    code: String,
    kind: PlaylistImportFailureKind,
    limit: Int? = null
): String {
    val copy = playlistImportFailureCopy(code)
    val raw = when (kind) {
        PlaylistImportFailureKind.INVALID_INPUT -> copy.invalidInput
        PlaylistImportFailureKind.NOT_AVAILABLE -> copy.notAvailable
        PlaylistImportFailureKind.TOO_LARGE -> copy.tooLarge
        PlaylistImportFailureKind.NO_MATCHES -> copy.noMatches
        PlaylistImportFailureKind.NETWORK -> copy.network
        PlaylistImportFailureKind.PROVIDER_CHANGED -> copy.providerChanged
        PlaylistImportFailureKind.STORAGE -> copy.storage
    }
    val limitText = limit?.let { value ->
        when (code) {
            "it" -> ": massimo $value brani"
            "es" -> ": máximo $value canciones"
            "fr" -> " : $value titres maximum"
            "de" -> ": maximal $value Titel"
            "pt" -> ": máximo de $value faixas"
            "nl" -> ": maximaal $value nummers"
            "pl" -> ": maksymalnie $value utworów"
            "ro" -> ": maximum $value piese"
            "el" -> ": έως $value κομμάτια"
            "sv" -> ": högst $value låtar"
            "da" -> ": højst $value numre"
            "cs" -> ": nejvýše $value skladeb"
            "uk" -> ": максимум $value композицій"
            "ru" -> ": максимум $value треков"
            "tr" -> ": en fazla $value parça"
            "ar" -> ": بحد أقصى $value مقطعًا"
            "zh" -> "：最多 $value 首歌曲"
            "ja" -> "：最大 $value 曲"
            "ko" -> ": 최대 $value곡"
            "hi" -> ": अधिकतम $value गाने"
            "id" -> ": maksimum $value lagu"
            "vi" -> ": tối đa $value bài hát"
            "th" -> ": สูงสุด $value เพลง"
            "fil" -> ": hanggang $value kanta"
            "he" -> ": עד $value שירים"
            else -> ": maximum $value tracks"
        }
    }.orEmpty()
    return raw.replace("{limit}", limitText)
}

fun playlistImportDismissMessage(code: String): String = playlistImportFailureCopy(code).dismiss
'''
)

write(
    "app/src/main/java/com/luc4n3x/levyra/domain/LevyraAudio.kt",
    '''package com.luc4n3x.levyra.domain

data class LevyraAudioPreset(
    val id: String,
    val fallbackLabel: String,
    val levels: List<Int>,
    val bassBoost: Int,
    val virtualizer: Int
)

data class LevyraAudioSettings(
    val equalizerEnabled: Boolean = false,
    val presetId: String = LevyraAudioPresets.FLAT,
    val bandLevels: List<Int> = LevyraAudioPresets.flatLevels,
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val preampDb: Float = 0f,
    val limiterEnabled: Boolean = true,
    val crossfadeSeconds: Int = 0,
    val djSoftMode: Boolean = false,
    val replayGainEnabled: Boolean = false,
    val playbackSpeed: Float = 1f,
    val pitch: Float = 1f,
    val gaplessEnabled: Boolean = true
) {
    fun normalized(): LevyraAudioSettings {
        val preset = LevyraAudioPresets.normalizePreset(presetId)
        val levels = bandLevels.takeIf { it.size == LevyraAudioPresets.bandCount } ?: LevyraAudioPresets.levelsFor(preset)
        return copy(
            presetId = preset,
            bandLevels = levels.map { it.coerceIn(-100, 100) },
            bassBoost = bassBoost.coerceIn(0, 100),
            virtualizer = virtualizer.coerceIn(0, 100),
            preampDb = preampDb.coerceIn(-12f, 3f),
            crossfadeSeconds = crossfadeSeconds.coerceIn(0, 12),
            playbackSpeed = playbackSpeed.coerceIn(0.5f, 2.0f),
            pitch = pitch.coerceIn(0.5f, 2.0f)
        )
    }
}

object LevyraAudioPresets {
    const val FLAT = "flat"
    const val BASS_BOOST = "bass_boost"
    const val VOCAL = "vocal"
    const val NIGHT = "night"
    const val GYM = "gym"
    const val CAR = "car"
    const val ROCK = "rock"
    const val POP = "pop"
    const val ELECTRONIC = "electronic"
    const val JAZZ = "jazz"
    const val ACOUSTIC = "acoustic"
    const val CLASSICAL = "classical"
    const val AIRPODS_PRO = "autoeq_airpods_pro"
    const val SONY_XM4 = "autoeq_sony_xm4"
    const val SONY_XM5 = "autoeq_sony_xm5"
    const val SENNHEISER_HD600 = "autoeq_hd600"
    const val bandCount = 10

    val flatLevels = listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

    val presets = listOf(
        LevyraAudioPreset(FLAT, "Flat", flatLevels, 0, 0),
        LevyraAudioPreset(BASS_BOOST, "Bass Boost", listOf(72, 58, 38, 18, 4, 0, 8, 16, 22, 24), 72, 18),
        LevyraAudioPreset(VOCAL, "Vocal", listOf(-20, -12, 0, 24, 48, 54, 42, 20, 8, 0), 8, 6),
        LevyraAudioPreset(NIGHT, "Night", listOf(-24, -18, -8, 4, 10, 12, 6, -2, -8, -16), 0, 0),
        LevyraAudioPreset(GYM, "Gym", listOf(76, 64, 42, 18, 4, 6, 22, 42, 56, 48), 80, 34),
        LevyraAudioPreset(CAR, "Car", listOf(44, 38, 26, 10, 0, 8, 24, 34, 38, 32), 48, 22),
        LevyraAudioPreset(ROCK, "Rock", listOf(52, 38, 16, -8, -14, 0, 18, 32, 44, 50), 50, 15),
        LevyraAudioPreset(POP, "Pop", listOf(-10, 24, 42, 36, 12, -8, -12, 14, 30, 22), 30, 10),
        LevyraAudioPreset(ELECTRONIC, "Electronic", listOf(68, 54, 28, 0, -16, 12, 24, 42, 58, 62), 65, 25),
        LevyraAudioPreset(JAZZ, "Jazz", listOf(24, 16, 8, 12, -10, -10, 0, 14, 28, 34), 20, 10),
        LevyraAudioPreset(ACOUSTIC, "Acoustic", listOf(28, 18, 10, 12, 18, 14, 22, 30, 26, 18), 15, 5),
        LevyraAudioPreset(CLASSICAL, "Classical", listOf(32, 24, 16, 8, -4, -4, 0, 16, 24, 28), 10, 15),
        LevyraAudioPreset(AIRPODS_PRO, "AirPods Pro · Device tune", listOf(-12, -6, 4, 8, 2, -4, 6, 12, 8, -4), 10, 10),
        LevyraAudioPreset(SONY_XM4, "Sony WH-1000XM4 · Device tune", listOf(-28, -18, -8, 2, 8, 6, 4, 14, 10, -8), 0, 10),
        LevyraAudioPreset(SONY_XM5, "Sony WH-1000XM5 · Device tune", listOf(-22, -14, -4, 4, 6, 4, 6, 12, 6, -6), 0, 10),
        LevyraAudioPreset(SENNHEISER_HD600, "Sennheiser HD600 · Device tune", listOf(42, 32, 14, 2, -2, -4, 2, 8, 4, -12), 35, 5)
    )

    fun normalizePreset(id: String): String = presets.firstOrNull { it.id == id }?.id ?: FLAT

    fun preset(id: String): LevyraAudioPreset = presets.firstOrNull { it.id == normalizePreset(id) } ?: presets.first()

    fun levelsFor(id: String): List<Int> = preset(id).levels

    fun labelFor(id: String): String = preset(id).fallbackLabel
}
'''
)

write(
    "app/src/test/java/com/luc4n3x/levyra/data/SponsorBlockRepositoryTest.kt",
    '''package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.SponsorSegment
import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SponsorBlockRepositoryTest {

    @Test
    fun rateLimitAndServerFailuresAreNotCached() {
        val fetcher = QueueSponsorBlockFetcher(
            response(429),
            response(500)
        )
        val repository = SponsorBlockRepository(fetcher) { 1_000L }

        assertTrue(repositorySegments(repository, "video").isEmpty())
        assertTrue(repositorySegments(repository, "video").isEmpty())
        assertEquals(2, fetcher.calls)
    }

    @Test
    fun negativeCacheExpiresAndThenAcceptsPositiveSegments() {
        var now = 10_000L
        val fetcher = QueueSponsorBlockFetcher(
            response(404),
            response(200, """[{"segment":[1.0,2.5],"category":"sponsor"}]""")
        )
        val repository = SponsorBlockRepository(fetcher) { now }

        assertTrue(repositorySegments(repository, "video").isEmpty())
        assertTrue(repositorySegments(repository, "video").isEmpty())
        assertEquals(1, fetcher.calls)

        now += SPONSORBLOCK_NEGATIVE_TTL_MS + 1L
        val refreshed = repositorySegments(repository, "video")
        assertEquals(1, refreshed.size)
        assertEquals(1_000L, refreshed.single().startMs)
        assertEquals(2_500L, refreshed.single().endMs)
        assertEquals(2, fetcher.calls)
    }

    @Test
    fun positivePublicationReplacesAnExistingNegativeEntry() {
        val cache = linkedMapOf<String, SponsorBlockCacheEntry>()
        val positive = listOf(SponsorSegment(2_000L, 3_000L, "intro"))

        publishSponsorBlockCacheResult(cache, "video", emptyList(), 1_000L)
        val published = publishSponsorBlockCacheResult(cache, "video", positive, 1_500L)

        assertEquals(positive, published)
        assertEquals(positive, cache["video"]?.segments)
    }

    @Test
    fun laterNegativePublicationCannotReplacePositiveSegments() {
        val cache = linkedMapOf<String, SponsorBlockCacheEntry>()
        val positive = listOf(SponsorSegment(4_000L, 5_000L, "outro"))

        publishSponsorBlockCacheResult(cache, "video", positive, 1_000L)
        val published = publishSponsorBlockCacheResult(cache, "video", emptyList(), 1_500L)

        assertEquals(positive, published)
        assertEquals(positive, cache["video"]?.segments)
    }

    @Test
    fun oversizedDeclaredBodyIsRejectedWithoutCaching() {
        val fetcher = QueueSponsorBlockFetcher(
            response(200, "[]", SPONSORBLOCK_MAX_RESPONSE_BYTES + 1L),
            response(200, "[]", SPONSORBLOCK_MAX_RESPONSE_BYTES + 1L)
        )
        val repository = SponsorBlockRepository(fetcher) { 1_000L }

        assertTrue(repositorySegments(repository, "video").isEmpty())
        assertTrue(repositorySegments(repository, "video").isEmpty())
        assertEquals(2, fetcher.calls)
    }

    @Test
    fun accessOrderedCacheRemainsBounded() {
        val cache = object : java.util.LinkedHashMap<String, SponsorBlockCacheEntry>(128, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, SponsorBlockCacheEntry>?): Boolean =
                size > SPONSORBLOCK_CACHE_LIMIT
        }
        repeat(SPONSORBLOCK_CACHE_LIMIT + 10) { index ->
            publishSponsorBlockCacheResult(cache, "video-$index", emptyList(), index.toLong())
        }
        assertEquals(SPONSORBLOCK_CACHE_LIMIT, cache.size)
    }

    private fun repositorySegments(repository: SponsorBlockRepository, videoId: String): List<SponsorSegment> =
        kotlinx.coroutines.runBlocking { repository.segments(videoId) }

    private class QueueSponsorBlockFetcher(vararg responses: SponsorBlockHttpResponse) : SponsorBlockHttpFetcher {
        private val queue = responses.toMutableList()
        var calls: Int = 0
            private set

        override fun fetch(url: String): SponsorBlockHttpResponse {
            calls += 1
            return queue.removeAt(0)
        }
    }

    private fun response(
        code: Int,
        body: String = "",
        declaredLength: Long = body.toByteArray().size.toLong()
    ): SponsorBlockHttpResponse = SponsorBlockHttpResponse(
        code = code,
        declaredLength = declaredLength,
        body = ByteArrayInputStream(body.toByteArray())
    )
}
'''
)

write(
    "app/src/test/java/com/luc4n3x/levyra/data/UniversalPlaylistImporterTest.kt",
    '''package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import java.io.ByteArrayInputStream
import java.net.Inet6Address
import java.net.InetAddress
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversalPlaylistImporterTest {

    @Test
    fun parsesSpotifyPlaylistTitleTrackCountAndUrlsWithoutDependingOnAttributeOrder() {
        val html = """
            <html><head>
            <meta content="Road &amp; Night | Spotify" property="og:title">
            <meta content="42" property="music:song_count">
            <meta name="music:song" content="https://open.spotify.com/track/trackOne?si=abc">
            <meta content="spotify:track:trackTwo" name="music:song">
            <meta property="music:song" content="https://open.spotify.com/track/trackOne?si=duplicate">
            </head></html>
        """.trimIndent()

        val page = parseSpotifyPlaylistPage(html)

        assertEquals("Road & Night", page.title)
        assertEquals(42, page.declaredTrackCount)
        assertEquals(listOf("https://open.spotify.com/track/trackOne", "https://open.spotify.com/track/trackTwo"), page.trackUrls)
    }

    @Test
    fun metaAttributeParserPreservesApostrophesInsideDoubleQuotedContent() {
        val html = """
            <head>
            <meta property="og:title" content="Rock 'n' Roll">
            <meta name="music:musician_description" content="Guns N' Roses">
            </head>
        """.trimIndent()

        val track = parseSpotifyTrackPage(html)

        assertEquals("Rock 'n' Roll", track?.title)
        assertEquals("Guns N' Roses", track?.artist)
    }

    @Test
    fun parsesSpotifyTrackMetadata() {
        val html = """
            <html><head>
            <meta property="og:title" content="A &amp; B">
            <meta content="Artist Name" name="music:musician_description">
            <meta name="music:duration" content="213">
            <meta content="https://image.test/cover.jpg" property="og:image">
            </head></html>
        """.trimIndent()

        val track = parseSpotifyTrackPage(html)

        assertNotNull(track)
        assertEquals("A & B", track?.title)
        assertEquals("Artist Name", track?.artist)
        assertEquals(213_000L, track?.durationMs)
        assertEquals("https://image.test/cover.jpg", track?.artworkUrl)
    }

    @Test
    fun fallsBackToTwitterDescriptionForArtist() {
        val html = """
            <meta property="og:title" content="Song Title">
            <meta name="twitter:description" content="Fallback Artist · Song Title · Song · 2026">
        """.trimIndent()

        assertEquals("Fallback Artist", parseSpotifyTrackPage(html)?.artist)
    }

    @Test
    fun genericSpotifyDescriptionUsesArtistFieldAfterSongDescriptor() {
        val html = """
            <meta property="og:title" content="Song Title">
            <meta name="description" content="Listen to Song Title on Spotify. Song · Real Artist · 2026">
        """.trimIndent()

        assertEquals("Real Artist", parseSpotifyTrackPage(html)?.artist)
    }

    @Test
    fun candidateScoringRejectsWrongLiveVersionAndPrefersStudioMatch() {
        val studio = track("studio", "My Song", "The Artist", 201_000L)
        val live = track("live", "My Song Live", "The Artist", 240_000L)
        val wrongArtist = track("wrong", "My Song", "Other Artist", 201_000L)

        val best = bestPlaylistImportCandidate("My Song", "The Artist", 201_000L, listOf(live, wrongArtist, studio))

        assertEquals(studio, best)
        assertTrue(playlistImportCandidateScore("My Song", "The Artist", 201_000L, studio) > playlistImportCandidateScore("My Song", "The Artist", 201_000L, live))
    }

    @Test
    fun candidateScoringRejectsUnrelatedFirstSearchResult() {
        val unrelated = track("wrong", "Completely Different", "Someone Else", 201_000L)
        assertNull(bestPlaylistImportCandidate("My Song", "The Artist", 201_000L, listOf(unrelated)))
    }

    @Test
    fun prefersExplicitDurationMsAndSupportsSecondOrMillisecondExports() {
        assertEquals(201_234L, importedDurationMs(JSONObject().put("durationMs", 201_234L).put("duration", 10L)))
        assertEquals(201_000L, importedDurationMs(JSONObject().put("duration", 201L)))
        assertEquals(201_234L, importedDurationMs(JSONObject().put("duration", 201_234L)))
    }

    @Test
    fun rejectsJsonImportAboveConfiguredTrackLimit() {
        assertTrue(jsonImportTrackCountAccepted(MAX_JSON_IMPORT_TRACKS))
        assertFalse(jsonImportTrackCountAccepted(MAX_JSON_IMPORT_TRACKS + 1))
    }

    @Test
    fun spotifyImportRequiresHttpsDefaultPortNoCredentialsAndAllowedHost() {
        assertNotNull(validateSpotifyImportUrl("https://open.spotify.com/playlist/example"))
        assertNotNull(validateSpotifyImportUrl("https://spotify.link/example"))
        assertNull(validateSpotifyImportUrl("http://open.spotify.com/playlist/example"))
        assertNull(validateSpotifyImportUrl("https://open.spotify.com:8443/playlist/example"))
        assertNull(validateSpotifyImportUrl("https://user:pass@open.spotify.com/playlist/example"))
        assertNull(validateSpotifyImportUrl("https://spotify.example.com/playlist/example"))
        assertNull(validateSpotifyImportUrl("https://example.com/playlist/example"))
    }

    @Test
    fun boundedReaderStopsBeforeOversizedBodyIsParsed() {
        assertEquals("hello", readUtf8Bounded(ByteArrayInputStream("hello".toByteArray()), 5L))
        assertNull(readUtf8Bounded(ByteArrayInputStream("hello!".toByteArray()), 5L))
    }

    @Test
    fun spotifyHeadOnlyDropsBodyMarkup() {
        val html = "<html><head><meta property=\"og:title\" content=\"Title\"></head><body>${"x".repeat(500)}</body></html>"
        val head = spotifyHeadOnly(html)
        assertTrue(head.endsWith("</head>"))
        assertFalse(head.contains("<body>"))
    }

    @Test
    fun spotifyImportAcceptsOnlyHtmlContentTypes() {
        assertTrue(spotifyHtmlContentTypeAccepted("text/html; charset=utf-8"))
        assertTrue(spotifyHtmlContentTypeAccepted("application/xhtml+xml"))
        assertFalse(spotifyHtmlContentTypeAccepted("application/json"))
        assertFalse(spotifyHtmlContentTypeAccepted("text/plain"))
        assertFalse(spotifyHtmlContentTypeAccepted(null))
    }

    @Test
    fun spotifyDestinationRejectsReservedIpv4Ranges() {
        listOf(
            "192.0.0.1",
            "192.0.2.1",
            "192.31.196.1",
            "192.52.193.1",
            "192.88.99.1",
            "192.175.48.1",
            "198.18.0.1",
            "198.51.100.1",
            "203.0.113.1"
        ).forEach { value -> assertFalse(value, isPublicNetworkAddress(InetAddress.getByName(value))) }
        assertTrue(isPublicNetworkAddress(InetAddress.getByName("8.8.8.8")))
    }

    @Test
    fun spotifyDestinationRejectsIpv4MappedIpv6PrivateAndReservedAddresses() {
        assertFalse(isPublicNetworkAddress(mappedIpv6(192, 168, 1, 10)))
        assertFalse(isPublicNetworkAddress(mappedIpv6(192, 0, 2, 10)))
        assertFalse(isPublicNetworkAddress(mappedIpv6(198, 18, 0, 10)))
    }

    private fun mappedIpv6(a: Int, b: Int, c: Int, d: Int): InetAddress {
        val bytes = ByteArray(16)
        bytes[10] = 0xFF.toByte()
        bytes[11] = 0xFF.toByte()
        bytes[12] = a.toByte()
        bytes[13] = b.toByte()
        bytes[14] = c.toByte()
        bytes[15] = d.toByte()
        return Inet6Address.getByAddress(null, bytes, -1)
    }

    private fun track(id: String, title: String, artist: String, durationMs: Long): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = "",
        durationMs = durationMs,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "test",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 50,
        cacheScore = 50,
        accentStart = 0,
        accentEnd = 0
    )
}
'''
)

# Keep domain models pure: localize preset labels at the Compose call site.
replace_exact(
    "app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt",
    "import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings\n",
    "import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings\nimport com.luc4n3x.levyra.ui.i18n.localizedAudioPresetLabel\n"
)
replace_exact(
    "app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt",
    """                                PremiumPresetChip(\n                                    label = preset.label,\n                                    selected = audioSettings.presetId == preset.id,""",
    """                                PremiumPresetChip(\n                                    label = strings.localizedAudioPresetLabel(preset.id, preset.fallbackLabel),\n                                    selected = audioSettings.presetId == preset.id,"""
)

# Replace only the import workflow in the large ViewModel.
viewmodel_path = ROOT / "app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt"
viewmodel = viewmodel_path.read_text(encoding="utf-8")
start = viewmodel.index("    fun importPlaylist(input: String) {")
end = viewmodel.index("    fun renamePlaylist(", start)
new_import_block = '''    fun importPlaylist(input: String) {
        if (playlistImportJob?.isActive == true) {
            _state.update { current ->
                current.copy(offlineExportMessage = playlistImportAlreadyRunningMessage(current.languageCode))
            }
            return
        }
        playlistImportJob = viewModelScope.launch {
            val importLanguage = _state.value.languageCode
            try {
                _state.update { current ->
                    current.copy(offlineExportMessage = playlistImportStartedMessage(current.languageCode))
                }
                val importer = com.luc4n3x.levyra.data.UniversalPlaylistImporter(
                    context = getApplication<Application>().applicationContext,
                    playlistStore = playlistStore,
                    youtubeRepository = repository
                )
                when (val result = importer.importFromUrlOrJson(input, languageCode = importLanguage)) {
                    is com.luc4n3x.levyra.data.PlaylistImportResult.Success -> {
                        _state.update { current ->
                            current.copy(
                                offlineExportMessage = playlistImportSuccessMessage(
                                    current.languageCode,
                                    result.importedCount,
                                    result.requestedCount,
                                    result.playlist.name
                                )
                            )
                        }
                        loadPlaylists()
                    }
                    is com.luc4n3x.levyra.data.PlaylistImportResult.Failure -> {
                        _state.update { current ->
                            current.copy(
                                offlineExportMessage = playlistImportFailureMessage(
                                    current.languageCode,
                                    result.kind,
                                    result.limit
                                )
                            )
                        }
                    }
                }
            } finally {
                playlistImportJob = null
            }
        }
    }

'''
viewmodel_path.write_text(viewmodel[:start] + new_import_block + viewmodel[end:], encoding="utf-8")

# Persist dismissal of the large import card, while keeping the compact action available.
prefs_path = ROOT / "app/src/main/java/com/luc4n3x/levyra/data/LevyraPreferences.kt"
prefs = prefs_path.read_text(encoding="utf-8")
marker = '''    fun setDynamicColor(value: Boolean) {
        write { it[KEY_DYNAMIC_COLOR] = value }
    }

'''
addition = marker + '''    fun playlistImportCardDismissed(): Boolean = read(false) { it[KEY_PLAYLIST_IMPORT_CARD_DISMISSED] ?: false }

    fun setPlaylistImportCardDismissed(value: Boolean) {
        write { it[KEY_PLAYLIST_IMPORT_CARD_DISMISSED] = value }
    }

'''
if prefs.count(marker) != 1:
    raise SystemExit("LevyraPreferences: dynamic color insertion marker mismatch")
prefs = prefs.replace(marker, addition, 1)
key_marker = '        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")\n'
if prefs.count(key_marker) != 1:
    raise SystemExit("LevyraPreferences: key insertion marker mismatch")
prefs = prefs.replace(key_marker, key_marker + '        val KEY_PLAYLIST_IMPORT_CARD_DISMISSED = booleanPreferencesKey("playlist_import_card_dismissed")\n', 1)
prefs_path.write_text(prefs, encoding="utf-8")

library_path = ROOT / "app/src/main/java/com/luc4n3x/levyra/ui/library/LevyraLibraryScreen.kt"
library = library_path.read_text(encoding="utf-8")
old = '''    val strings = LocalLevyraStrings.current
    val isItalian = strings.code == "it"
'''
new = '''    val strings = LocalLevyraStrings.current
    val isItalian = strings.code == "it"
    val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val libraryPreferences = remember(appContext) { com.luc4n3x.levyra.data.LevyraPreferences(appContext) }
'''
if library.count(old) != 1:
    raise SystemExit("LevyraLibraryScreen: header marker mismatch")
library = library.replace(old, new, 1)
library = library.replace(
    '    var showImportPlaylistCard by rememberSaveable { mutableStateOf(true) }',
    '    var showImportPlaylistCard by remember { mutableStateOf(!libraryPreferences.playlistImportCardDismissed()) }',
    1
)
library = library.replace(
    '''                                onClick = { showImportPlaylist = true },
                                onDismiss = { showImportPlaylistCard = false },
                                isItalian = isItalian
''',
    '''                                onClick = { showImportPlaylist = true },
                                onDismiss = {
                                    showImportPlaylistCard = false
                                    libraryPreferences.setPlaylistImportCardDismissed(true)
                                }
''',
    1
)
library = library.replace(
    '''        LibraryImportPlaylistDialog(
            isItalian = isItalian,
            onDismiss = { showImportPlaylist = false },''',
    '''        LibraryImportPlaylistDialog(
            onDismiss = { showImportPlaylist = false },''',
    1
)
library_path.write_text(library, encoding="utf-8")

ui_path = ROOT / "app/src/main/java/com/luc4n3x/levyra/ui/library/LibraryPlaylistImportUi.kt"
ui = ui_path.read_text(encoding="utf-8")
ui = ui.replace('import androidx.compose.foundation.layout.size\n', 'import androidx.compose.foundation.layout.size\nimport androidx.compose.foundation.layout.sizeIn\n', 1)
ui = ui.replace('import com.luc4n3x.levyra.ui.i18n.playlistImportCopy\n', 'import com.luc4n3x.levyra.ui.i18n.playlistImportCopy\nimport com.luc4n3x.levyra.ui.i18n.playlistImportDismissMessage\n', 1)
ui = ui.replace(
    '''internal fun LibraryImportPlaylistCard(
    onClick: () -> Unit,
    onDismiss: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") isItalian: Boolean = false
) {''',
    '''internal fun LibraryImportPlaylistCard(
    onClick: () -> Unit,
    onDismiss: () -> Unit = {}
) {''',
    1
)
ui = ui.replace(
    '''internal fun LibraryImportPlaylistDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit,
    @Suppress("UNUSED_PARAMETER") isItalian: Boolean = false
) {''',
    '''internal fun LibraryImportPlaylistDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {''',
    1
)
ui = ui.replace(
    '    val copy = LocalLevyraStrings.current.playlistImportCopy()\n',
    '    val strings = LocalLevyraStrings.current\n    val copy = strings.playlistImportCopy()\n'
)
ui = ui.replace(
    '''            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(30.dp)
            ) {''',
    '''            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(48.dp)
            ) {''',
    1
)
ui = ui.replace('contentDescription = copy.cancel,', 'contentDescription = playlistImportDismissMessage(strings.code),', 1)
ui = ui.replace(
    '''            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .combinedClickable(onClick = onClick)''',
    '''            modifier = Modifier
                .sizeIn(minHeight = 48.dp)
                .clip(RoundedCornerShape(16.dp))
                .combinedClickable(onClick = onClick)''',
    1
)
ui_path.write_text(ui, encoding="utf-8")

print("PR #319 hardening patch applied")
