package com.luc4n3x.levyra.desktop.core.charts

import com.luc4n3x.levyra.desktop.core.extractor.ExtractorHttp
import com.luc4n3x.levyra.desktop.core.model.Track
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

data class EditorialShelf(val id: String, val title: String, val tracks: List<Track>)
data class EditorialFeed(val charts: Map<String, List<Track>>, val releases: Map<String, List<Track>>,
                         val genres: List<EditorialShelf>, val generatedAtMs: Long)

class EditorialCatalog(private val cacheFile: Path? = null, private val clock: () -> Long = System::currentTimeMillis) {
    private val mutex = Mutex()
    private var cached: EditorialFeed? = null
    private var checkedAt = 0L

    suspend fun load(): EditorialFeed? = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (clock() - checkedAt < 30 * 60_000L) return@withLock cached?.takeIf { usable(it.generatedAtMs, clock()) }
            if (cached == null && cacheFile != null && Files.isRegularFile(cacheFile) && Files.size(cacheFile) <= MAX_BYTES) {
                cached = runCatching { parse(Files.readString(cacheFile), clock()) }.getOrNull()
            }
            checkedAt = clock()
            val fresh = runCatching {
                ExtractorHttp.client.newCall(Request.Builder().url(CATALOG_URL).build()).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val bytes = response.peekBody(MAX_BYTES + 1).bytes()
                    if (bytes.size > MAX_BYTES) return@use null
                    val body = bytes.toString(Charsets.UTF_8)
                    val parsed = parse(body, clock()) ?: return@use null
                    if (cacheFile != null) {
                        Files.createDirectories(cacheFile.parent)
                        val temporary = Files.createTempFile(cacheFile.parent, "editorial-", ".part")
                        try {
                            Files.writeString(temporary, body)
                            try {
                                Files.move(temporary, cacheFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                            } catch (_: AtomicMoveNotSupportedException) {
                                Files.move(temporary, cacheFile, StandardCopyOption.REPLACE_EXISTING)
                            }
                        } finally { Files.deleteIfExists(temporary) }
                    }
                    parsed
                }
            }.getOrNull()
            if (fresh != null) cached = fresh
            cached?.takeIf { usable(it.generatedAtMs, clock()) }
        }
    }

    companion object {
        private const val MAX_BYTES = 4L * 1024 * 1024
        private const val CATALOG_URL = "https://raw.githubusercontent.com/LUC4N3X/Levyra-deepsound/editorial-data/catalog/editorial.json"
        private val videoId = Regex("[A-Za-z0-9_-]{11}")
        private fun usable(generated: Long, now: Long) = now - generated in -600_000L..172_800_000L

        internal fun parse(body: String, now: Long): EditorialFeed? {
            val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
            if (root.optInt("schemaVersion") != 1) return null
            val generated = runCatching { Instant.parse(root.optString("generatedAt")).toEpochMilli() }.getOrNull() ?: return null
            if (!usable(generated, now)) return null
            val collections = root.optJSONArray("collections") ?: return null
            val charts = linkedMapOf<String, List<Track>>()
            val releases = linkedMapOf<String, List<Track>>()
            val genres = ArrayList<EditorialShelf>()
            for (index in 0 until collections.length().coerceAtMost(1000)) {
                val collection = collections.optJSONObject(index) ?: continue
                val kind = collection.optString("kind").lowercase(Locale.ROOT)
                val tracks = tracks(collection.optJSONArray("tracks"))
                if (tracks.isEmpty()) continue
                val market = collection.optString("market").uppercase(Locale.ROOT)
                when (kind) {
                    "chart" -> if (market.length == 2) charts[market] = tracks
                    "release" -> if (market.length == 2) releases[market] = tracks
                    "genre" -> {
                        val id = collection.optString("youtubeParams").take(1024)
                        if (id.isNotBlank()) genres += EditorialShelf(id, collection.optString("title"), tracks)
                    }
                }
            }
            return EditorialFeed(charts, releases, genres.distinctBy { it.id }, generated)
        }

        private fun tracks(items: JSONArray?): List<Track> {
            if (items == null) return emptyList()
            return (0 until items.length().coerceAtMost(100)).mapNotNull { index ->
                val item = items.optJSONObject(index) ?: return@mapNotNull null
                val title = item.optString("title").trim()
                val artists = item.optJSONArray("artists") ?: return@mapNotNull null
                val artist = (0 until artists.length().coerceAtMost(30)).mapNotNull { artists.optJSONObject(it)?.optString("name") }
                    .filter { it.isNotBlank() }.distinct().joinToString(", ")
                if (title.isBlank() || artist.isBlank()) return@mapNotNull null
                val mapping = item.optJSONObject("youtubeMusic")
                val mappedId = mapping?.optString("audioVideoId").orEmpty().takeIf {
                    videoId.matches(it) && mapping!!.optInt("audioConfidence", mapping.optInt("confidence")) >= 82
                }.orEmpty()
                val rawArtwork = item.optString("artworkUrl").take(512)
                val artwork = rawArtwork.toHttpUrlOrNull()?.takeIf {
                    it.isHttps && it.port == 443 && it.username.isEmpty() && it.password.isEmpty() &&
                        (it.host == "i.scdn.co" || it.host.endsWith(".scdn.co") || it.host == "image-cdn-ak.spotifycdn.com")
                }?.toString().orEmpty()
                Track(item.optString("id").ifBlank { "editorial:$title|$artist" }, title, artist,
                    item.optJSONObject("album")?.optString("name").orEmpty(),
                    if (mappedId.isNotBlank()) Track.watchUrlOf(mappedId) else "", artwork,
                    item.optLong("durationMs").coerceAtLeast(0), explicit = if (item.has("explicit")) item.optBoolean("explicit") else null,
                    isrc = item.optString("isrc"))
            }.distinctBy { it.id }
        }
    }
}
