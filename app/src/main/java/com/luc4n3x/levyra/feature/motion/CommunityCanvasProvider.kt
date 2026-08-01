package com.luc4n3x.levyra.feature.motion

import android.content.Context
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Curated community canvases are an optional, read-only motion-artwork source.
 *
 * The remote catalog never controls arbitrary destinations: both catalog parsing and the shared
 * motion-artwork verifier enforce HTTPS, known media hosts and MP4/HLS file types.
 */
class CommunityCanvasProvider(context: Context) : MotionArtworkProvider {
    override val id: String = PROVIDER_ID

    private val client: OkHttpClient = LevyraHttpClientFactory.media(context).newBuilder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.SECONDS)
        .build()
    private val catalogMutex = Mutex()

    @Volatile
    private var cachedEntries: List<CommunityCanvasEntry> = emptyList()

    @Volatile
    private var catalogExpiresAtMs: Long = 0L

    override suspend fun find(identity: MotionTrackIdentity): MotionArtworkProviderResult {
        return try {
            val entries = catalog()
            val now = System.currentTimeMillis()
            val candidates = communityCanvasCandidates(identity, entries, now)
            if (candidates.isEmpty()) MotionArtworkProviderResult.NoMatch
            else MotionArtworkProviderResult.Found(candidates)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.d(error, "Community canvas provider failed")
            MotionArtworkProviderResult.Failed(error)
        }
    }

    private suspend fun catalog(): List<CommunityCanvasEntry> {
        val now = System.currentTimeMillis()
        cachedEntries.takeIf { it.isNotEmpty() && now < catalogExpiresAtMs }?.let { return it }
        return catalogMutex.withLock {
            val refreshedAt = System.currentTimeMillis()
            cachedEntries.takeIf { it.isNotEmpty() && refreshedAt < catalogExpiresAtMs }?.let {
                return@withLock it
            }
            val parsed = loadFirstUsableCatalog()
            cachedEntries = parsed
            catalogExpiresAtMs = refreshedAt + CATALOG_TTL_MS
            parsed
        }
    }

    private suspend fun loadFirstUsableCatalog(): List<CommunityCanvasEntry> {
        var lastError: Throwable? = null
        val deadlineMs = System.currentTimeMillis() + CATALOG_BUDGET_MS
        for ((index, source) in CATALOG_URLS.withIndex()) {
            val remainingMs = deadlineMs - System.currentTimeMillis()
            if (remainingMs <= 0L) break
            val sliceMs = (remainingMs / (CATALOG_URLS.size - index))
                .coerceAtLeast(MIN_SOURCE_BUDGET_MS)
            val payload = try {
                withTimeoutOrNull(sliceMs) { fetchCatalog(source) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                lastError = error
                continue
            }
            if (payload == null) {
                lastError = CommunityCanvasException("Community canvas source timed out")
                continue
            }
            val document = parseCommunityCanvasDocument(payload)
            if (isUsableCatalog(source, document)) return document.entries
            lastError = CommunityCanvasException("Community canvas catalog is not usable")
        }
        throw lastError ?: CommunityCanvasException("Community canvas catalog is unavailable")
    }

    private fun isUsableCatalog(source: String, catalog: CommunityCanvasCatalog): Boolean =
        if (source == MIRROR_CATALOG_URL) {
            catalog.version == MIRROR_CATALOG_VERSION &&
                catalog.entries.size >= MIN_MIRROR_CATALOG_ENTRIES
        } else {
            catalog.entries.isNotEmpty()
        }

    private suspend fun fetchCatalog(source: String): String = suspendCancellableCoroutine { continuation ->
        val request = Request.Builder()
            .url(source)
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
            .build()
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) {
                    continuation.resumeWithException(
                        CommunityCanvasException("Unable to load community canvas catalog", e)
                    )
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val payload = runCatching { readCatalogPayload(response) }
                if (!continuation.isActive) return
                payload.fold(
                    onSuccess = { body -> continuation.resume(body) },
                    onFailure = { error ->
                        continuation.resumeWithException(
                            error as? CommunityCanvasException
                                ?: CommunityCanvasException("Unable to load community canvas catalog", error)
                        )
                    }
                )
            }
        })
    }

    private fun readCatalogPayload(response: Response): String = response.use { current ->
        if (!current.isSuccessful) {
            throw CommunityCanvasException("Community canvas HTTP ${current.code}")
        }
        val body = current.body
        if (body.contentLength() > MAX_CATALOG_BYTES) {
            throw CommunityCanvasException("Community canvas catalog is too large")
        }
        val input = body.byteStream()
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > MAX_CATALOG_BYTES) {
                throw CommunityCanvasException("Community canvas catalog is too large")
            }
            output.write(buffer, 0, read)
        }
        output.toString(Charsets.UTF_8.name()).takeIf { it.isNotBlank() }
            ?: throw CommunityCanvasException("Community canvas catalog is empty")
    }

    companion object {
        const val PROVIDER_ID = "community-canvas"
        const val MIRROR_CATALOG_URL =
            "https://raw.githubusercontent.com/LUC4N3X/Levyra-deepsound/canvas-data/catalog/community-canvas.json"
        const val UPSTREAM_CATALOG_URL =
            "https://raw.githubusercontent.com/vivizzz007/vivimusicanvas/main/canvas.json"
        val CATALOG_URLS = listOf(MIRROR_CATALOG_URL, UPSTREAM_CATALOG_URL)
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/142 Mobile Safari/537.36 Levyra/CommunityCanvas"
        const val MAX_CATALOG_BYTES = 1024 * 1024
        const val CATALOG_TTL_MS = 6L * 60L * 60L * 1000L
        const val CATALOG_BUDGET_MS = 6_000L
        const val MIN_SOURCE_BUDGET_MS = 2_000L
        const val MIRROR_CATALOG_VERSION = 1
        const val MIN_MIRROR_CATALOG_ENTRIES = 100
    }
}

internal data class CommunityCanvasEntry(
    val song: String,
    val artist: String,
    val album: String,
    val url: String,
    val scope: MotionArtworkScope,
    val pathScope: MotionArtworkScope? = null,
    val isrc: String = "",
    val width: Int? = null,
    val height: Int? = null
)

internal data class CommunityCanvasCatalog(
    val version: Int,
    val entries: List<CommunityCanvasEntry>
)

internal fun parseCommunityCanvasCatalog(payload: String): List<CommunityCanvasEntry> =
    parseCommunityCanvasDocument(payload).entries

internal fun parseCommunityCanvasDocument(payload: String): CommunityCanvasCatalog {
    val root = runCatching { JSONObject(payload) }.getOrNull()
        ?: return CommunityCanvasCatalog(0, emptyList())
    val version = root.optInt("version")
    val items = root.optJSONArray("items") ?: return CommunityCanvasCatalog(version, emptyList())
    val output = ArrayList<CommunityCanvasEntry>(items.length())
    val seen = HashSet<String>()
    for (index in 0 until items.length()) {
        val item = items.optJSONObject(index) ?: continue
        val song = item.optString("song").trim()
        val artist = item.optString("artist").trim()
        val album = item.optString("album").trim()
        val rawUrl = item.optString("url").trim()
        val url = rawUrl.toHttpUrlOrNull() ?: continue
        if (song.isBlank() || artist.isBlank() || album.isBlank()) continue
        if (url.scheme != "https" || url.port != 443 || url.host.lowercase(Locale.ROOT) !in COMMUNITY_MEDIA_HOSTS) {
            continue
        }
        val extension = url.encodedPath.substringAfterLast('.', "").lowercase(Locale.ROOT)
        if (extension !in COMMUNITY_MEDIA_EXTENSIONS) continue
        val declaredScope = when (item.optString("scope").trim().lowercase(Locale.ROOT)) {
            "album" -> MotionArtworkScope.ALBUM
            "track", "song" -> MotionArtworkScope.TRACK
            else -> null
        }
        val scope = declaredScope ?: MotionArtworkScope.TRACK
        val pathScope = if (declaredScope == null) communityCanvasScopeFromPath(url) else null
        val key = listOf(
            normalizeMotionText(song),
            normalizeMotionText(artist),
            normalizeMotionText(album),
            rawUrl,
            scope.name
        ).joinToString("|")
        if (!seen.add(key)) continue
        output += CommunityCanvasEntry(
            song = song,
            artist = artist,
            album = album,
            url = rawUrl,
            scope = scope,
            pathScope = pathScope,
            isrc = communityCanvasIsrc(item.optString("isrc")),
            width = item.optInt("width").takeIf { it > 0 },
            height = item.optInt("height").takeIf { it > 0 }
        )
    }
    return CommunityCanvasCatalog(version, output)
}

private fun communityCanvasScopeFromPath(url: HttpUrl): MotionArtworkScope? {
    val marker = url.pathSegments
        .dropLast(1)
        .map { segment -> segment.lowercase(Locale.ROOT) }
        .lastOrNull { segment -> segment == "album" || segment == "song" }
        ?: return null
    return if (marker == "album") MotionArtworkScope.ALBUM else MotionArtworkScope.TRACK
}

private fun communityCanvasIsrc(value: String): String {
    val normalized = value.trim().uppercase(Locale.ROOT)
    return if (COMMUNITY_ISRC_PATTERN.matches(normalized)) normalized else ""
}

internal fun communityCanvasCandidates(
    identity: MotionTrackIdentity,
    entries: List<CommunityCanvasEntry>,
    nowMs: Long
): List<MotionArtworkCandidate> {
    if (identity.title.isBlank() || identity.artists.isEmpty()) return emptyList()
    val inferredAlbumEntries = entries
        .groupBy { entry ->
            listOf(
                normalizeMotionText(entry.artist),
                normalizeMotionText(entry.album),
                entry.url
            ).joinToString("|")
        }
        .values
        .mapNotNull { group ->
            val first = group.firstOrNull() ?: return@mapNotNull null
            if (
                first.scope == MotionArtworkScope.ALBUM ||
                first.pathScope == MotionArtworkScope.ALBUM ||
                group.map { it.song }.distinct().size >= 2
            ) {
                first.copy(scope = MotionArtworkScope.ALBUM)
            } else {
                null
            }
        }
    return (entries + inferredAlbumEntries)
        .asSequence()
        .map { entry -> entry to communityCanvasQuickScore(identity, entry) }
        .filter { (_, score) -> score >= MIN_COMMUNITY_QUICK_SCORE }
        .sortedByDescending { (_, score) -> score }
        .distinctBy { (entry, _) -> communityCanvasCandidateKey(entry) }
        .take(MAX_COMMUNITY_CANDIDATES)
        .map { (entry, _) ->
            MotionArtworkCandidate(
                provider = CommunityCanvasProvider.PROVIDER_ID,
                scope = entry.scope,
                identity = MotionTrackIdentity(
                    title = entry.song,
                    artists = splitArtists(entry.artist),
                    album = entry.album,
                    durationMs = 0L,
                    isrc = if (entry.scope == MotionArtworkScope.TRACK) entry.isrc else "",
                    upc = "",
                    year = "",
                    trackId = "",
                    albumId = ""
                ),
                url = entry.url,
                mimeType = if (entry.url.substringBefore('?').endsWith(".m3u8", true)) {
                    "application/x-mpegURL"
                } else {
                    "video/mp4"
                },
                width = entry.width,
                height = entry.height,
                expiresAtMs = nowMs + MOTION_ARTWORK_POSITIVE_TTL_MS
            )
        }
        .toList()
}

private fun communityCanvasCandidateKey(entry: CommunityCanvasEntry): String = when (entry.scope) {
    MotionArtworkScope.ALBUM -> listOf(
        MotionArtworkScope.ALBUM.name,
        normalizeMotionText(entry.artist),
        normalizeMotionText(entry.album),
        entry.url
    ).joinToString("|")
    MotionArtworkScope.TRACK -> listOf(
        MotionArtworkScope.TRACK.name,
        entry.url,
        normalizeMotionText(entry.song)
    ).joinToString("|")
}

private fun communityCanvasQuickScore(
    identity: MotionTrackIdentity,
    entry: CommunityCanvasEntry
): Int {
    val entryArtists = splitArtists(entry.artist)
    if (!primaryMotionArtistMatches(identity.artists, entryArtists)) return Int.MIN_VALUE
    if (
        entry.scope == MotionArtworkScope.TRACK &&
        identity.isrc.isNotBlank() &&
        entry.isrc.isNotBlank()
    ) {
        return if (identity.isrc == entry.isrc) 100 else Int.MIN_VALUE
    }
    val titleScore = communityTextSimilarity(identity.title, entry.song)
    val albumScore = communityTextSimilarity(identity.album, entry.album)
    return if (entry.scope == MotionArtworkScope.ALBUM) {
        (albumScore * 100.0).toInt()
    } else {
        (titleScore * 78.0 + albumScore * 22.0).toInt()
    }
}

private fun communityTextSimilarity(first: String, second: String): Double {
    val left = normalizeMotionText(first)
    val right = normalizeMotionText(second)
    if (left.isBlank() || right.isBlank()) return 0.0
    if (left == right) return 1.0
    val leftTokens = left.split(' ').filter(String::isNotBlank).toSet()
    val rightTokens = right.split(' ').filter(String::isNotBlank).toSet()
    if (leftTokens.isEmpty() || rightTokens.isEmpty()) return 0.0
    return (2.0 * leftTokens.intersect(rightTokens).size.toDouble()) /
        (leftTokens.size + rightTokens.size).toDouble()
}

internal val COMMUNITY_MEDIA_HOSTS = setOf(
    "vivimusicanvas.mkmdevilmi.workers.dev",
    "vivimusicanvas-mtih.vercel.app"
)

private val COMMUNITY_MEDIA_EXTENSIONS = setOf("mp4", "m3u8")
private val COMMUNITY_ISRC_PATTERN = Regex("^[A-Z]{2}[A-Z0-9]{3}[0-9]{7}$")
private const val MIN_COMMUNITY_QUICK_SCORE = 70
private const val MAX_COMMUNITY_CANDIDATES = 8

private class CommunityCanvasException(message: String, cause: Throwable? = null) : IOException(message, cause)
