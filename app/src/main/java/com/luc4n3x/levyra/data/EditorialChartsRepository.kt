package com.luc4n3x.levyra.data

import android.content.Context
import android.util.AtomicFile
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.Response
import okhttp3.brotli.BrotliInterceptor
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Reads Levyra's public, pre-normalized editorial ranking catalog.
 *
 * The source credential never enters the app, and neither do source page URLs or URIs. ISRC is retained as a public recording identity. Cover
 * artwork is the one published source-hosted asset: on-device lookups cannot match every charting
 * track, so the catalog carries the album cover and [publishedArtworkUrl] re-checks its host here
 * rather than trusting the publication guard alone.
 *
 * A single process-wide instance owns the remote refresh, memory snapshot and AtomicFile cache so
 * Home and Android Auto cannot race.
 */
internal class EditorialChartsRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshGuard = Any()
    private val cacheFile = AtomicFile(File(appContext.filesDir, CACHE_RELATIVE_PATH))
    private val httpClient = LevyraHttpClientFactory.media(appContext).newBuilder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(BrotliInterceptor)
        .cache(Cache(File(appContext.cacheDir, HTTP_CACHE_DIRECTORY), HTTP_CACHE_BYTES))
        .build()

    @Volatile
    private var memorySnapshot: CatalogSnapshot? = null

    @Volatile
    private var refreshDeferred: Deferred<CatalogSnapshot?>? = null

    @Volatile
    private var lastRefreshFailureAt: Long = 0L

    fun warm() {
        refreshAsync()
    }

    suspend fun cachedTopTracks(country: String, limit: Int): List<Track> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val snapshot = usableSnapshot(now)
        if (snapshot == null) {
            warm()
            return@withContext emptyList()
        }
        if (snapshot.needsRefresh(now)) warm()
        snapshot.tracks(country, limit)
    }

    suspend fun cachedNewReleaseTracks(country: String, limit: Int): List<Track> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val snapshot = usableSnapshot(now)
        if (snapshot == null) {
            warm()
            return@withContext emptyList()
        }
        if (snapshot.needsRefresh(now)) warm()
        snapshot.newReleases(country, limit)
    }

    suspend fun newReleaseTracks(country: String, limit: Int): List<Track> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val snapshot = usableSnapshot(now) ?: refreshAsync().await() ?: return@withContext emptyList()
        if (snapshot.needsRefresh(now)) warm()
        snapshot.newReleases(country, limit)
    }

    suspend fun cachedAllMarkets(limit: Int): Map<String, List<Track>> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val snapshot = usableSnapshot(now) ?: refreshAsync().await() ?: return@withContext emptyMap()
        if (snapshot.needsRefresh(now)) warm()
        val safeLimit = limit.coerceIn(1, 100)
        snapshot.byMarket
            .mapValues { (_, tracks) -> tracks.take(safeLimit) }
            .filterValues { it.isNotEmpty() }
    }

    private fun refreshAsync(): Deferred<CatalogSnapshot?> = synchronized(refreshGuard) {
        refreshDeferred?.takeIf { it.isActive }?.let { return@synchronized it }
        val now = System.currentTimeMillis()
        if (now - lastRefreshFailureAt in 0 until REFRESH_RETRY_TTL_MS) {
            return@synchronized CompletableDeferred(usableSnapshot(now))
        }
        scope.async {
            val stored = usableSnapshot(System.currentTimeMillis())
            val remote = fetchRemoteSnapshot()
            if (remote != null) {
                persist(remote.rawJson)
                memorySnapshot = remote
                lastRefreshFailureAt = 0L
                remote
            } else {
                lastRefreshFailureAt = System.currentTimeMillis()
                stored
            }
        }.also { created ->
            refreshDeferred = created
            created.invokeOnCompletion {
                synchronized(refreshGuard) {
                    if (refreshDeferred === created) refreshDeferred = null
                }
            }
        }
    }

    private fun usableSnapshot(now: Long): CatalogSnapshot? {
        memorySnapshot?.let { cached ->
            if (cached.isUsable(now)) return cached
            memorySnapshot = null
        }
        val stored = readStoredSnapshot(now) ?: return null
        memorySnapshot = stored
        return stored
    }

    private suspend fun fetchRemoteSnapshot(): CatalogSnapshot? =
        suspendCancellableCoroutine { continuation ->
            val request = Request.Builder()
                .url(CATALOG_URL)
                .header("Accept", "application/json")
                .header("User-Agent", "Levyra/${BuildConfig.VERSION_NAME} Android")
                .cacheControl(CacheControl.FORCE_NETWORK)
                .build()
            val call = httpClient.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resume(null)
                }

                override fun onResponse(call: Call, response: Response) {
                    val snapshot = runCatching {
                        response.use { current ->
                            if (!current.isSuccessful) return@use null
                            val body = current.body
                            val bytes = body.byteStream().readBounded(MAX_CATALOG_BYTES) ?: return@use null
                            if (bytes.isEmpty()) return@use null
                            EditorialCatalogParser.parse(
                                body = bytes.toString(StandardCharsets.UTF_8),
                                loadedAt = System.currentTimeMillis(),
                            )
                        }
                    }.getOrNull()
                    if (continuation.isActive) continuation.resume(snapshot)
                }
            })
        }

    private fun readStoredSnapshot(now: Long): CatalogSnapshot? {
        val bytes = runCatching {
            cacheFile.openRead().use { it.readBounded(MAX_CATALOG_BYTES) }
        }.getOrNull() ?: return null
        val snapshot = EditorialCatalogParser.parse(
            body = bytes.toString(StandardCharsets.UTF_8),
            loadedAt = now,
        ) ?: return null
        return snapshot.takeIf { it.isUsable(now) }
    }

    private fun persist(body: String) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        if (bytes.size !in 1..MAX_CATALOG_BYTES) return
        val stream = runCatching { cacheFile.startWrite() }.getOrNull() ?: return
        try {
            stream.write(bytes)
            stream.fd.sync()
            cacheFile.finishWrite(stream)
        } catch (error: Throwable) {
            cacheFile.failWrite(stream)
            Timber.w(error, "Unable to persist editorial chart catalog")
        }
    }

    private fun InputStream.readBounded(maxBytes: Int): ByteArray? {
        val output = ByteArrayOutputStream(minOf(maxBytes, 64 * 1024))
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            total += read
            if (total > maxBytes) return null
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    companion object {
        @Volatile
        private var instance: EditorialChartsRepository? = null

        fun get(context: Context): EditorialChartsRepository {
            return instance ?: synchronized(this) {
                instance ?: EditorialChartsRepository(context.applicationContext).also { instance = it }
            }
        }

        private const val CACHE_RELATIVE_PATH = "editorial/charts-v2.json"
        private const val HTTP_CACHE_DIRECTORY = "levyra_editorial_http"
        private const val HTTP_CACHE_BYTES = 4L * 1024L * 1024L
        private const val REFRESH_RETRY_TTL_MS = 5L * 60L * 1000L
        private const val MAX_CATALOG_BYTES = 2 * 1024 * 1024
        private const val CATALOG_URL =
            "https://raw.githubusercontent.com/LUC4N3X/Levyra-deepsound/editorial-data/catalog/editorial.json"
    }
}

internal data class CatalogSnapshot(
    val byMarket: Map<String, List<Track>>,
    val releaseByMarket: Map<String, List<Track>>,
    val generatedAtMs: Long,
    val loadedAt: Long,
    val rawJson: String,
) {
    fun isUsable(now: Long): Boolean {
        val age = now - generatedAtMs
        return age in -MAX_FUTURE_SKEW_MS..MAX_CATALOG_AGE_MS
    }

    fun needsRefresh(now: Long): Boolean = now - generatedAtMs > REFRESH_AFTER_MS

    fun tracks(country: String, limit: Int): List<Track> {
        val market = country.trim().uppercase(Locale.ROOT).takeIf { it.length == 2 } ?: DEFAULT_MARKET
        return byMarket[market].orEmpty().take(limit.coerceIn(1, MAX_TRACKS_PER_MARKET))
    }

    fun newReleases(country: String, limit: Int): List<Track> {
        val market = country.trim().uppercase(Locale.ROOT).takeIf { it.length == 2 } ?: DEFAULT_MARKET
        val localized = releaseByMarket[market].orEmpty()
        val fallback = releaseByMarket["US"].orEmpty()
        return localized.ifEmpty { fallback }.take(limit.coerceIn(1, MAX_TRACKS_PER_MARKET))
    }

    private companion object {
        const val DEFAULT_MARKET = "IT"
        const val MAX_TRACKS_PER_MARKET = 100
        const val REFRESH_AFTER_MS = 30L * 60L * 1000L
        const val MAX_CATALOG_AGE_MS = 48L * 60L * 60L * 1000L
        const val MAX_FUTURE_SKEW_MS = 10L * 60L * 1000L
    }
}

internal object EditorialCatalogParser {
    fun parse(body: String, loadedAt: Long): CatalogSnapshot? {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
        if (root.optInt("schemaVersion", -1) != SUPPORTED_SCHEMA_VERSION) return null
        val generatedAtMs = parseInstant(root.optString("generatedAt")) ?: return null
        val collections = root.optJSONArray("collections") ?: return null
        val byMarket = LinkedHashMap<String, List<Track>>()
        val releaseByMarket = LinkedHashMap<String, List<Track>>()
        for (index in 0 until collections.length()) {
            val collection = collections.optJSONObject(index) ?: continue
            val kind = collection.optString("kind").trim().lowercase(Locale.ROOT)
            if (kind != "chart" && kind != "release") continue
            val market = collection.optString("market")
                .trim()
                .uppercase(Locale.ROOT)
                .takeIf { it.length == 2 }
                ?: continue
            val tracks = parseTracks(collection.optJSONArray("tracks"), kind)
            if (tracks.isEmpty()) continue
            if (kind == "release") releaseByMarket[market] = tracks else byMarket[market] = tracks
        }
        if (byMarket.isEmpty()) return null
        return CatalogSnapshot(
            byMarket = byMarket,
            releaseByMarket = releaseByMarket,
            generatedAtMs = generatedAtMs,
            loadedAt = loadedAt,
            rawJson = body,
        )
    }

    private fun parseTracks(items: JSONArray?, kind: String): List<Track> {
        if (items == null) return emptyList()
        val tracks = ArrayList<Track>(minOf(items.length(), MAX_TRACKS_PER_MARKET))
        val releaseCollection = kind.equals("release", ignoreCase = true)
        for (index in 0 until items.length()) {
            if (tracks.size >= MAX_TRACKS_PER_MARKET) break
            val item = items.optJSONObject(index) ?: continue
            val title = item.optString("title").trim()
            val artist = parseArtists(item.optJSONArray("artists"))
            if (title.isBlank() || artist.isBlank()) continue
            val album = item.optJSONObject("album")
            val releaseDate = album?.optString("releaseDate").orEmpty().trim()
            val identity = chartIdentity("$title|$artist")
            val catalogTrackId = publishedCatalogTrackId(item.optString("id"))
                .ifBlank { "chart-${identity.id}" }
            val palette = PALETTES[identity.seed % PALETTES.size]
            val artwork = publishedArtworkUrl(item.optString("artworkUrl"))
            val youtubeMusic = item.optJSONObject("youtubeMusic")
            val genericYoutubeConfidence = youtubeMusic?.optInt("confidence", 0)?.coerceIn(0, 100) ?: 0
            val youtubeAudioConfidence = youtubeMusic
                ?.optInt("audioConfidence", genericYoutubeConfidence)
                ?.coerceIn(0, 100)
                ?: 0
            val youtubeVideoConfidence = youtubeMusic
                ?.optInt("videoConfidence", genericYoutubeConfidence)
                ?.coerceIn(0, 100)
                ?: 0
            val youtubeAudioVideoId = publishedYoutubeVideoId(youtubeMusic?.optString("audioVideoId"))
                .takeIf { youtubeAudioConfidence >= MIN_AUDIO_MAPPING_CONFIDENCE }
                .orEmpty()
            val youtubeOfficialVideoId = publishedYoutubeVideoId(youtubeMusic?.optString("videoId"))
                .takeIf {
                    youtubeAudioVideoId.isNotBlank() &&
                        youtubeVideoConfidence >= MIN_OFFICIAL_VIDEO_CONFIDENCE
                }
                .orEmpty()
            val youtubePlaybackId = youtubeAudioVideoId
            val albumBrowseId = publishedYoutubeBrowseId(youtubeMusic?.optString("albumBrowseId"))
            val artistBrowseId = publishedYoutubeBrowseId(youtubeMusic?.optString("artistBrowseId"))
            val youtubeConfidence = maxOf(
                genericYoutubeConfidence,
                youtubeAudioConfidence,
                youtubeVideoConfidence,
            )
            tracks += Track(
                id = catalogTrackId,
                title = title,
                artist = artist,
                album = album?.optString("name").orEmpty().trim().ifBlank {
                    if (releaseCollection) title else EDITORIAL_ALBUM
                },
                durationMs = item.optLong("durationMs", 0L).coerceAtLeast(0L),
                streamUrl = "",
                videoUrl = youtubePlaybackId
                    .takeIf(String::isNotBlank)
                    ?.let { "https://www.youtube.com/watch?v=$it" }
                    .orEmpty(),
                thumbnailUrl = artwork,
                largeThumbnailUrl = artwork,
                source = if (releaseCollection) EDITORIAL_RELEASE_SOURCE else EDITORIAL_SOURCE,
                moodTags = if (releaseCollection) {
                    setOf("new-release", "editorial")
                } else {
                    setOf("hit", "chart")
                },
                energy = 70,
                vocal = 55,
                replayScore = if (releaseCollection) 90 else 95,
                cacheScore = 88,
                accentStart = palette.first,
                accentEnd = palette.second,
                releaseDate = releaseDate,
                year = releaseDate.take(4).takeIf { it.length == 4 && it.all(Char::isDigit) }.orEmpty(),
                explicit = item.optBoolean("explicit", false),
                isrc = item.optString("isrc").uppercase(Locale.ROOT).filter(Char::isLetterOrDigit),
                albumBrowseId = albumBrowseId,
                artistBrowseIds = listOfNotNull(artistBrowseId.takeIf(String::isNotBlank)),
                counterpartVideoId = youtubeOfficialVideoId,
                // videoType describes the identity in videoUrl. The official OMV is a
                // counterpart, while the primary playback identity remains the ATV.
                videoType = youtubeAudioVideoId.takeIf(String::isNotBlank)
                    ?.let { "MUSIC_VIDEO_TYPE_ATV" }
                    .orEmpty(),
                audioVideoId = youtubeAudioVideoId,
                metadataProvider = when {
                    releaseCollection && youtubePlaybackId.isNotBlank() -> "$EDITORIAL_RELEASE_SOURCE + YouTube Music"
                    releaseCollection -> EDITORIAL_RELEASE_SOURCE
                    youtubePlaybackId.isNotBlank() -> "$EDITORIAL_SOURCE + YouTube Music"
                    else -> EDITORIAL_SOURCE
                },
                metadataConfidence = if (youtubePlaybackId.isBlank()) 94 else youtubeConfidence,
            )
        }
        return tracks.distinctBy { it.id }
    }

    /**
     * Accepts a published cover URL only when it is HTTPS on the source's image CDN.
     *
     * The catalog is fetched from a remote branch, so the publication guard is not the only thing
     * standing between it and the image loader: an unexpected or tampered payload must not be able to
     * point Coil at an arbitrary host, embed credentials, or pin a custom port. A rejected value
     * degrades to on-device artwork lookup rather than failing the row.
     */
    private fun publishedCatalogTrackId(value: String?): String {
        val normalized = value.orEmpty().trim()
        return normalized.takeIf {
            it.length in 1..128 && it.matches(Regex("[A-Za-z0-9_-]+"))
        }.orEmpty()
    }

    private fun publishedYoutubeVideoId(value: String?): String {
        val normalized = value.orEmpty().trim()
        return normalized.takeIf { it.matches(Regex("[A-Za-z0-9_-]{11}")) }.orEmpty()
    }

    private fun publishedYoutubeBrowseId(value: String?): String {
        val normalized = value.orEmpty().trim()
        return normalized.takeIf { it.length <= 128 && it.matches(Regex("[A-Za-z0-9_-]+")) }.orEmpty()
    }

    private fun publishedArtworkUrl(value: String?): String {
        val normalized = value.orEmpty().trim()
        if (normalized.isEmpty() || normalized.length > MAX_ARTWORK_URL_LENGTH) return ""
        val url = normalized.toHttpUrlOrNull() ?: return ""
        if (url.scheme != "https") return ""
        if (url.port != HTTPS_DEFAULT_PORT) return ""
        if (url.encodedUsername.isNotEmpty() || url.encodedPassword.isNotEmpty()) return ""
        if (normalized.contains('@')) return ""
        val host = url.host.lowercase(Locale.ROOT)
        val allowed = host == "i.scdn.co" ||
            host.endsWith(".scdn.co") ||
            host == "image-cdn-ak.spotifycdn.com"
        return if (allowed) normalized else ""
    }

    private fun parseArtists(items: JSONArray?): String {
        if (items == null) return ""
        return buildList {
            for (index in 0 until items.length()) {
                val name = items.optJSONObject(index)?.optString("name").orEmpty().trim()
                if (name.isNotBlank()) add(name)
            }
        }.distinct().joinToString(", ")
    }

    private fun parseInstant(value: String): Long? {
        return try {
            Instant.parse(value.trim()).toEpochMilli()
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun chartIdentity(value: String): ChartIdentity {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
        val seed = digest.take(4).fold(0) { accumulator, byte ->
            (accumulator shl 8) or (byte.toInt() and 0xFF)
        } and Int.MAX_VALUE
        val id = digest.take(8).joinToString("") { byte -> "%02x".format(byte) }
        return ChartIdentity(seed = seed, id = id)
    }

    private data class ChartIdentity(val seed: Int, val id: String)

    private const val SUPPORTED_SCHEMA_VERSION = 1
    private const val MAX_TRACKS_PER_MARKET = 100
    private const val EDITORIAL_SOURCE = "Levyra Editorial"
    private const val EDITORIAL_RELEASE_SOURCE = "Levyra Editorial Releases"
    private const val EDITORIAL_ALBUM = "Levyra Top 50"
    private const val MAX_ARTWORK_URL_LENGTH = 512
    private const val HTTPS_DEFAULT_PORT = 443
    private const val MIN_AUDIO_MAPPING_CONFIDENCE = 82
    private const val MIN_OFFICIAL_VIDEO_CONFIDENCE = 90

    private val PALETTES = listOf(
        0xFF00E5FF.toInt() to 0xFF7B42FF.toInt(),
        0xFF1B5CFF.toInt() to 0xFFFF4FD8.toInt(),
        0xFFFF7A18.toInt() to 0xFF8E57FF.toInt(),
        0xFF00D4A6.toInt() to 0xFFFF3B5C.toInt(),
        0xFFFFB000.toInt() to 0xFF00E5FF.toInt(),
    )
}
