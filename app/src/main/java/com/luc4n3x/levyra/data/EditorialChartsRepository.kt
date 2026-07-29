package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Reads Levyra's public, pre-normalized editorial catalog.
 *
 * The Android app never receives the source session credential and never contacts the upstream
 * editorial service. Missing countries intentionally return an empty list so the existing YouTube
 * Music and Apple chart providers remain the transparent fallback.
 */
internal class EditorialChartsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val httpClient = LevyraHttpClientFactory.feeds(appContext)
    private val refreshMutex = Mutex()
    private val cacheFile = File(appContext.filesDir, CACHE_RELATIVE_PATH)

    @Volatile
    private var memorySnapshot: CatalogSnapshot? = null

    @Volatile
    private var lastRefreshAttemptAt: Long = 0L

    suspend fun topTracks(country: String, limit: Int): List<Track> = withContext(Dispatchers.IO) {
        val market = country.trim().uppercase(Locale.ROOT).takeIf { it.length == 2 } ?: DEFAULT_MARKET
        val safeLimit = limit.coerceIn(1, 100)
        val snapshot = loadSnapshot() ?: return@withContext emptyList()
        snapshot.byMarket[market].orEmpty().take(safeLimit)
    }

    private suspend fun loadSnapshot(): CatalogSnapshot? {
        val now = System.currentTimeMillis()
        memorySnapshot?.let { cached ->
            if (now - cached.loadedAt in 0 until MEMORY_TTL_MS) return cached
        }
        return refreshMutex.withLock {
            val lockedNow = System.currentTimeMillis()
            memorySnapshot?.let { cached ->
                if (lockedNow - cached.loadedAt in 0 until MEMORY_TTL_MS) return@withLock cached
            }

            val stored = readStoredSnapshot()
            val shouldRefresh = lockedNow - lastRefreshAttemptAt !in 0 until REFRESH_RETRY_TTL_MS
            if (!shouldRefresh) {
                memorySnapshot = stored
                return@withLock stored
            }

            lastRefreshAttemptAt = lockedNow
            val remote = try {
                fetchRemoteSnapshot()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                Timber.w(error, "Editorial chart catalog refresh failed")
                null
            }
            if (remote != null) {
                persist(remote.rawJson)
                memorySnapshot = remote
                remote
            } else {
                memorySnapshot = stored
                stored
            }
        }
    }

    private suspend fun fetchRemoteSnapshot(): CatalogSnapshot? =
        suspendCancellableCoroutine { continuation ->
            val request = Request.Builder()
                .url(CATALOG_URL)
                .header("Accept", "application/json")
                .header("User-Agent", "Levyra/${BuildConfig.VERSION_NAME} Android")
                .cacheControl(REMOTE_CACHE_CONTROL)
                .build()
            val call = httpClient.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, error: IOException) {
                    if (continuation.isActive) continuation.resume(null)
                }

                override fun onResponse(call: Call, response: Response) {
                    val snapshot = runCatching {
                        response.use { current ->
                            if (!current.isSuccessful) return@use null
                            val body = current.body?.string()?.takeIf(String::isNotBlank) ?: return@use null
                            if (body.toByteArray(StandardCharsets.UTF_8).size > MAX_CATALOG_BYTES) {
                                return@use null
                            }
                            EditorialCatalogParser.parse(body, System.currentTimeMillis())
                        }
                    }.getOrNull()
                    if (continuation.isActive) continuation.resume(snapshot)
                }
            })
        }

    private fun readStoredSnapshot(): CatalogSnapshot? {
        if (!cacheFile.isFile || cacheFile.length() !in 1..MAX_CATALOG_BYTES.toLong()) return null
        val body = runCatching { cacheFile.readText(Charsets.UTF_8) }.getOrNull() ?: return null
        return EditorialCatalogParser.parse(body, System.currentTimeMillis())
    }

    private fun persist(body: String) {
        runCatching {
            val parent = cacheFile.parentFile ?: return
            parent.mkdirs()
            val temporary = File(parent, "${cacheFile.name}.tmp")
            temporary.writeText(body, Charsets.UTF_8)
            temporary.copyTo(cacheFile, overwrite = true)
            temporary.delete()
        }.onFailure { error ->
            Timber.w(error, "Unable to persist editorial chart catalog")
        }
    }

    private companion object {
        const val DEFAULT_MARKET = "IT"
        const val CACHE_RELATIVE_PATH = "editorial/charts-v1.json"
        const val MEMORY_TTL_MS = 30L * 60L * 1000L
        const val REFRESH_RETRY_TTL_MS = 5L * 60L * 1000L
        const val MAX_CATALOG_BYTES = 4 * 1024 * 1024
        const val CATALOG_URL =
            "https://raw.githubusercontent.com/LUC4N3X/Levyra-deepsound/editorial-data/catalog/editorial.json"

        val REMOTE_CACHE_CONTROL: CacheControl = CacheControl.Builder()
            .maxStale(7, TimeUnit.DAYS)
            .build()
    }
}

internal data class CatalogSnapshot(
    val byMarket: Map<String, List<Track>>,
    val loadedAt: Long,
    val rawJson: String
)

internal object EditorialCatalogParser {
    fun parse(body: String, loadedAt: Long): CatalogSnapshot? {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
        if (root.optInt("schemaVersion", -1) != SUPPORTED_SCHEMA_VERSION) return null
        val collections = root.optJSONArray("collections") ?: return null
        val byMarket = LinkedHashMap<String, List<Track>>()
        for (index in 0 until collections.length()) {
            val collection = collections.optJSONObject(index) ?: continue
            if (!collection.optString("kind").equals("chart", ignoreCase = true)) continue
            val market = collection.optString("market")
                .trim()
                .uppercase(Locale.ROOT)
                .takeIf { it.length == 2 }
                ?: continue
            val tracks = parseTracks(collection.optJSONArray("tracks"))
            if (tracks.isNotEmpty()) byMarket[market] = tracks
        }
        if (byMarket.isEmpty()) return null
        return CatalogSnapshot(byMarket = byMarket, loadedAt = loadedAt, rawJson = body)
    }

    private fun parseTracks(items: JSONArray?): List<Track> {
        if (items == null) return emptyList()
        val tracks = ArrayList<Track>(minOf(items.length(), MAX_TRACKS_PER_MARKET))
        for (index in 0 until items.length()) {
            if (tracks.size >= MAX_TRACKS_PER_MARKET) break
            val item = items.optJSONObject(index) ?: continue
            val title = item.optString("title").trim()
            val artist = parseArtists(item.optJSONArray("artists"))
            if (title.isBlank() || artist.isBlank()) continue
            val album = item.optJSONObject("album")
            val albumName = album?.optString("name").orEmpty().trim().ifBlank { EDITORIAL_ALBUM }
            val artwork = safeHttpsUrl(item.optString("artworkUrl"))
                .ifBlank { safeHttpsUrl(album?.optString("artworkUrl").orEmpty()) }
            val releaseDate = album?.optString("releaseDate").orEmpty().trim()
            val identity = chartIdentity("$title|$artist")
            val palette = PALETTES[identity.seed % PALETTES.size]
            tracks += Track(
                id = "chart-${identity.id}",
                title = title,
                artist = artist,
                album = albumName,
                durationMs = item.optLong("durationMs", 0L).coerceAtLeast(0L),
                streamUrl = "",
                videoUrl = "",
                thumbnailUrl = artwork,
                largeThumbnailUrl = artwork,
                source = EDITORIAL_SOURCE,
                moodTags = setOf("hit", "chart"),
                energy = 70,
                vocal = 55,
                replayScore = 95,
                cacheScore = 88,
                accentStart = palette.first,
                accentEnd = palette.second,
                isrc = item.optString("isrc").trim(),
                releaseDate = releaseDate,
                year = releaseDate.take(4).takeIf { it.length == 4 && it.all(Char::isDigit) }.orEmpty(),
                explicit = item.optBoolean("explicit", false),
                metadataProvider = EDITORIAL_SOURCE,
                metadataConfidence = 96
            )
        }
        return tracks.distinctBy { it.id }
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

    private fun safeHttpsUrl(value: String): String {
        val normalized = value.trim()
        return normalized.takeIf {
            it.length <= MAX_URL_LENGTH && it.startsWith("https://", ignoreCase = true)
        }.orEmpty()
    }

    private fun chartIdentity(value: String): ChartIdentity {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
        val seed = digest.take(4)
            .fold(0) { accumulator, byte ->
                (accumulator shl 8) or (byte.toInt() and 0xFF)
            } and Int.MAX_VALUE
        val id = digest.take(8).joinToString("") { byte -> "%02x".format(byte) }
        return ChartIdentity(seed = seed, id = id)
    }

    private data class ChartIdentity(val seed: Int, val id: String)

    private const val SUPPORTED_SCHEMA_VERSION = 1
    private const val MAX_TRACKS_PER_MARKET = 100
    private const val MAX_URL_LENGTH = 2_048
    private const val EDITORIAL_SOURCE = "Levyra Editorial"
    private const val EDITORIAL_ALBUM = "Levyra Top 50"

    private val PALETTES = listOf(
        0xFF00E5FF.toInt() to 0xFF7B42FF.toInt(),
        0xFF1B5CFF.toInt() to 0xFFFF4FD8.toInt(),
        0xFFFF7A18.toInt() to 0xFF8E57FF.toInt(),
        0xFF00D4A6.toInt() to 0xFFFF3B5C.toInt(),
        0xFFFFB000.toInt() to 0xFF00E5FF.toInt()
    )
}
