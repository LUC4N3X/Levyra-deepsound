package com.luc4n3x.levyra.feature.motion

import android.content.Context
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
import java.util.LinkedHashMap
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Curated community canvases are an optional, read-only motion-artwork source.
 *
 * The provider prefers Levyra's hash-sharded index. The app downloads one tiny manifest and only
 * the shards that can contain the current recording, so catalog growth does not increase APK size
 * or require loading the complete catalog. The legacy flat mirror and upstream catalog remain a
 * rollout fallback while the indexed mirror is unavailable.
 */
class CommunityCanvasProvider(context: Context) : MotionArtworkProvider {
    override val id: String = PROVIDER_ID

    private val client: OkHttpClient = LevyraHttpClientFactory.media(context).newBuilder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.SECONDS)
        .build()
    private val catalogMutex = Mutex()
    private val indexManifestMutex = Mutex()
    private val indexShardCacheMutex = Mutex()

    @Volatile
    private var cachedEntries: List<CommunityCanvasEntry> = emptyList()

    @Volatile
    private var catalogExpiresAtMs: Long = 0L

    @Volatile
    private var cachedIndexManifest: CommunityCanvasIndexManifest? = null

    @Volatile
    private var indexManifestExpiresAtMs: Long = 0L

    private val indexShardCache = object : LinkedHashMap<String, CachedCommunityCanvasIndexShard>(
        MAX_CACHED_INDEX_SHARDS,
        0.75f,
        true
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, CachedCommunityCanvasIndexShard>?
        ): Boolean = size > MAX_CACHED_INDEX_SHARDS
    }

    override suspend fun find(identity: MotionTrackIdentity): MotionArtworkProviderResult {
        return try {
            val entries = when (val indexed = indexedEntries(identity)) {
                is CommunityCanvasIndexLookup.Available -> indexed.entries
                CommunityCanvasIndexLookup.Unavailable -> catalog()
            }
            val candidates = communityCanvasCandidates(identity, entries, System.currentTimeMillis())
            if (candidates.isEmpty()) MotionArtworkProviderResult.NoMatch
            else MotionArtworkProviderResult.Found(candidates)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.d(error, "Community canvas provider failed")
            MotionArtworkProviderResult.Failed(error)
        }
    }

    private suspend fun indexedEntries(identity: MotionTrackIdentity): CommunityCanvasIndexLookup {
        val lookupKeys = communityCanvasLookupKeys(identity)
        if (lookupKeys.isEmpty()) return CommunityCanvasIndexLookup.Unavailable
        val indexBudgetMs = communityCanvasIndexBudgetMs(
            MotionArtworkRuntime.snapshot().value.requestTimeoutMs
        )
        if (indexBudgetMs <= 0L) return CommunityCanvasIndexLookup.Unavailable
        return withTimeoutOrNull(indexBudgetMs) {
            val manifest = indexManifest() ?: return@withTimeoutOrNull CommunityCanvasIndexLookup.Unavailable
            val requests = lookupKeys
                .groupBy { key -> communityCanvasShardPrefix(key, manifest.prefixChars) }
                .filterKeys(manifest::hasShard)
            if (requests.isEmpty()) {
                return@withTimeoutOrNull CommunityCanvasIndexLookup.Available(emptyList())
            }
            val loaded = coroutineScope {
                requests.map { (prefix, acceptedKeys) ->
                    async {
                        try {
                            IndexedShardResult(
                                acceptedHashes = acceptedKeys.map(::communityCanvasLookupHash).toSet(),
                                rows = indexShard(manifest, prefix)
                            )
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Throwable) {
                            Timber.d(error, "Community canvas index shard %s failed", prefix)
                            null
                        }
                    }
                }.awaitAll()
            }
            val successful = loaded.filterNotNull()
            val entries = successful
                .flatMap { result ->
                    result.rows.asSequence()
                        .filter { indexed -> indexed.lookupHash in result.acceptedHashes }
                        .map { indexed -> indexed.toCatalogEntry(identity) }
                        .toList()
                }
                .distinctBy { entry ->
                    listOf(entry.scope.name, entry.url, normalizeMotionText(entry.song)).joinToString("|")
                }
            when {
                entries.isNotEmpty() -> CommunityCanvasIndexLookup.Available(entries)
                successful.size == loaded.size -> CommunityCanvasIndexLookup.Available(emptyList())
                else -> CommunityCanvasIndexLookup.Unavailable
            }
        } ?: CommunityCanvasIndexLookup.Unavailable
    }

    private suspend fun indexManifest(): CommunityCanvasIndexManifest? {
        val now = System.currentTimeMillis()
        cachedIndexManifest?.takeIf { now < indexManifestExpiresAtMs }?.let { return it }
        return indexManifestMutex.withLock {
            val refreshedAt = System.currentTimeMillis()
            cachedIndexManifest?.takeIf { refreshedAt < indexManifestExpiresAtMs }?.let {
                return@withLock it
            }
            val payload = try {
                withTimeoutOrNull(INDEX_MANIFEST_TIMEOUT_MS) {
                    fetchPayload(INDEX_MANIFEST_URL, MAX_INDEX_MANIFEST_BYTES)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                null
            } ?: return@withLock null
            val manifest = parseCommunityCanvasIndexManifest(payload)
                ?.takeIf { it.largestShardBytes <= MAX_INDEX_SHARD_BYTES }
                ?: return@withLock null
            cachedIndexManifest = manifest
            indexManifestExpiresAtMs = refreshedAt + INDEX_CACHE_TTL_MS
            manifest
        }
    }

    private suspend fun indexShard(
        manifest: CommunityCanvasIndexManifest,
        prefix: String
    ): List<CommunityCanvasIndexedEntry> {
        val cacheKey = "${manifest.cacheKey}:$prefix"
        val now = System.currentTimeMillis()
        indexShardCacheMutex.withLock {
            val cached = indexShardCache[cacheKey]
            if (cached != null && now < cached.expiresAtMs) return cached.rows
            if (cached != null) indexShardCache.remove(cacheKey)
        }
        val url = "$INDEX_ROOT_URL/${manifest.shardDirectory}/shards/$prefix.json"
        val payload = fetchPayload(url, MAX_INDEX_SHARD_BYTES)
        val rows = parseCommunityCanvasIndexShard(payload)
        if (rows.isEmpty()) throw CommunityCanvasException("Community canvas index shard is empty")
        indexShardCacheMutex.withLock {
            indexShardCache[cacheKey] = CachedCommunityCanvasIndexShard(
                rows = rows,
                expiresAtMs = System.currentTimeMillis() + INDEX_CACHE_TTL_MS
            )
        }
        return rows
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
            val sourcesLeft = CATALOG_URLS.size - index
            val sliceMs = maxOf(MIN_SOURCE_BUDGET_MS, remainingMs / sourcesLeft)
                .coerceAtMost(remainingMs)
            val payload = try {
                withTimeoutOrNull(sliceMs) { fetchPayload(source, MAX_CATALOG_BYTES) }
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

    private suspend fun fetchPayload(source: String, maxBytes: Int): String =
        suspendCancellableCoroutine { continuation ->
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
                            CommunityCanvasException("Unable to load community canvas data", e)
                        )
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val payload = runCatching { readPayload(response, maxBytes) }
                    if (!continuation.isActive) return
                    payload.fold(
                        onSuccess = { body -> continuation.resume(body) },
                        onFailure = { error ->
                            continuation.resumeWithException(
                                error as? CommunityCanvasException
                                    ?: CommunityCanvasException("Unable to load community canvas data", error)
                            )
                        }
                    )
                }
            })
        }

    private fun readPayload(response: Response, maxBytes: Int): String = response.use { current ->
        if (!current.isSuccessful) {
            throw CommunityCanvasException("Community canvas HTTP ${current.code}")
        }
        val body = current.body
        if (body.contentLength() > maxBytes) {
            throw CommunityCanvasException("Community canvas response is too large")
        }
        val input = body.byteStream()
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > maxBytes) {
                throw CommunityCanvasException("Community canvas response is too large")
            }
            output.write(buffer, 0, read)
        }
        output.toString(Charsets.UTF_8.name()).takeIf { it.isNotBlank() }
            ?: throw CommunityCanvasException("Community canvas response is empty")
    }

    companion object {
        const val PROVIDER_ID = "community-canvas"
        const val INDEX_ROOT_URL =
            "https://raw.githubusercontent.com/LUC4N3X/Levyra-deepsound/canvas-data/catalog/index/v2"
        const val INDEX_MANIFEST_URL = "$INDEX_ROOT_URL/manifest.json"
        const val MIRROR_CATALOG_URL =
            "https://raw.githubusercontent.com/LUC4N3X/Levyra-deepsound/canvas-data/catalog/community-canvas.json"
        const val UPSTREAM_CATALOG_URL =
            "https://raw.githubusercontent.com/vivizzz007/vivimusicanvas/main/canvas.json"
        val CATALOG_URLS = listOf(MIRROR_CATALOG_URL, UPSTREAM_CATALOG_URL)
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/142 Mobile Safari/537.36 Levyra/CommunityCanvas"
        const val MAX_CATALOG_BYTES = 1024 * 1024
        const val MAX_INDEX_MANIFEST_BYTES = 256 * 1024
        const val MAX_INDEX_SHARD_BYTES = 192 * 1024
        const val CATALOG_TTL_MS = 6L * 60L * 60L * 1000L
        const val INDEX_CACHE_TTL_MS = 6L * 60L * 60L * 1000L
        const val CATALOG_BUDGET_MS = 6_000L
        const val INDEX_LOOKUP_BUDGET_MS = 4_500L
        const val CATALOG_FALLBACK_RESERVE_MS = 4_000L
        const val INDEX_MANIFEST_TIMEOUT_MS = 1_800L
        const val MIN_SOURCE_BUDGET_MS = 2_000L
        const val MIRROR_CATALOG_VERSION = 1
        const val MIN_MIRROR_CATALOG_ENTRIES = 100
        const val MAX_CACHED_INDEX_SHARDS = 8
    }
}

internal fun communityCanvasIndexBudgetMs(providerTimeoutMs: Long): Long =
    minOf(
        CommunityCanvasProvider.INDEX_LOOKUP_BUDGET_MS,
        (providerTimeoutMs - CommunityCanvasProvider.CATALOG_FALLBACK_RESERVE_MS)
            .coerceAtLeast(0L)
    )

private sealed interface CommunityCanvasIndexLookup {
    data class Available(val entries: List<CommunityCanvasEntry>) : CommunityCanvasIndexLookup
    data object Unavailable : CommunityCanvasIndexLookup
}

private data class CachedCommunityCanvasIndexShard(
    val rows: List<CommunityCanvasIndexedEntry>,
    val expiresAtMs: Long
)

private data class IndexedShardResult(
    val acceptedHashes: Set<String>,
    val rows: List<CommunityCanvasIndexedEntry>
)

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
        val entry = parseCommunityCanvasEntry(items.optJSONObject(index) ?: continue) ?: continue
        val key = listOf(
            normalizeMotionText(entry.song),
            normalizeMotionText(entry.artist),
            normalizeMotionText(entry.album),
            entry.url,
            entry.scope.name
        ).joinToString("|")
        if (seen.add(key)) output += entry
    }
    return CommunityCanvasCatalog(version, output)
}

internal fun parseCommunityCanvasEntry(item: JSONObject): CommunityCanvasEntry? {
    val song = item.optString("song").trim()
    val artist = item.optString("artist").trim()
    val album = item.optString("album").trim()
    val rawUrl = item.optString("url").trim()
    val url = communityCanvasMediaUrl(rawUrl) ?: return null
    if (song.isBlank() || artist.isBlank() || album.isBlank()) return null
    val declaredScope = when (item.optString("scope").trim().lowercase(Locale.ROOT)) {
        "album" -> MotionArtworkScope.ALBUM
        "track", "song" -> MotionArtworkScope.TRACK
        else -> null
    }
    return CommunityCanvasEntry(
        song = song,
        artist = artist,
        album = album,
        url = rawUrl,
        scope = declaredScope ?: MotionArtworkScope.TRACK,
        pathScope = if (declaredScope == null) communityCanvasScopeFromPath(url) else null,
        isrc = communityCanvasIsrc(item.optString("isrc")),
        width = item.optInt("width").takeIf { it > 0 },
        height = item.optInt("height").takeIf { it > 0 }
    )
}

internal fun communityCanvasMediaUrl(rawUrl: String): HttpUrl? {
    val url = rawUrl.toHttpUrlOrNull() ?: return null
    if (url.scheme != "https" || url.port != 443 || url.host.lowercase(Locale.ROOT) !in COMMUNITY_MEDIA_HOSTS) {
        return null
    }
    val extension = url.encodedPath.substringAfterLast('.', "").lowercase(Locale.ROOT)
    return url.takeIf { extension in COMMUNITY_MEDIA_EXTENSIONS }
}

private fun communityCanvasScopeFromPath(url: HttpUrl): MotionArtworkScope? {
    val marker = url.pathSegments
        .dropLast(1)
        .map { segment -> segment.lowercase(Locale.ROOT) }
        .lastOrNull { segment -> segment == "album" || segment == "song" }
        ?: return null
    return if (marker == "album") MotionArtworkScope.ALBUM else MotionArtworkScope.TRACK
}

internal fun communityCanvasIsrc(value: String): String {
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
internal val COMMUNITY_ISRC_PATTERN = Regex("^[A-Z]{2}[A-Z0-9]{3}[0-9]{7}$")
private const val MIN_COMMUNITY_QUICK_SCORE = 70
private const val MAX_COMMUNITY_CANDIDATES = 8

private class CommunityCanvasException(message: String, cause: Throwable? = null) : IOException(message, cause)