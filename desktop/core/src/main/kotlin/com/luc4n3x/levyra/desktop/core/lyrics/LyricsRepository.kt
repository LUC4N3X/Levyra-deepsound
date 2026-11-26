package com.luc4n3x.levyra.desktop.core.lyrics

import com.luc4n3x.levyra.desktop.core.extractor.ExtractorHttp
import com.luc4n3x.levyra.desktop.core.model.Track
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

class LyricsRepository(
    private val client: OkHttpClient = ExtractorHttp.client,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val cache = ConcurrentHashMap<String, Lyrics>()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun lyricsFor(track: Track): Lyrics? = withContext(dispatcher) {
        if (track.title.isBlank()) return@withContext null
        cache[track.id]?.let { return@withContext it }

        val direct = fetch(getUrl(track))?.let(::parseSingle)
        val cleanTitle = cleanTitle(track.title)
        val leadArtist = leadArtist(track.artist)
        val resolved = direct
            ?: fetch(searchUrl(track.title, track.artist))?.let { parseFirstMatch(it, track.durationMs) }
            ?: if (cleanTitle != track.title || leadArtist != track.artist) {
                fetch(searchUrl(cleanTitle, leadArtist))?.let { parseFirstMatch(it, track.durationMs) }
            } else {
                null
            }
        if (resolved != null && !resolved.isEmpty) {
            cache[track.id] = resolved
        }
        resolved
    }

    private fun fetch(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .get()
            .header("User-Agent", ExtractorHttp.DESKTOP_USER_AGENT)
            .header("Accept", "application/json")
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) null else response.body.string()
            }
        }.getOrNull()
    }

    private fun parseSingle(body: String): Lyrics? =
        runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()?.let(::toLyrics)

    private fun parseFirstMatch(body: String, durationMs: Long): Lyrics? {
        val entries = runCatching { json.parseToJsonElement(body).jsonArray }.getOrNull() ?: return null
        val objects = entries.mapNotNull { element -> runCatching { element.jsonObject }.getOrNull() }
        val targetSeconds = durationMs / 1000.0
        val ordered = if (durationMs > 0L) {
            objects.sortedBy { entry ->
                val seconds = entry.text("duration").toDoubleOrNull() ?: return@sortedBy Double.MAX_VALUE
                kotlin.math.abs(seconds - targetSeconds)
            }.filter { entry ->
                val seconds = entry.text("duration").toDoubleOrNull()
                seconds == null || kotlin.math.abs(seconds - targetSeconds) <= MAX_DURATION_DRIFT_SECONDS
            }
        } else {
            objects
        }
        return ordered.asSequence().mapNotNull(::toLyrics).firstOrNull { !it.isEmpty }
    }

    private fun toLyrics(entry: JsonObject): Lyrics? {
        val synced = entry.text("syncedLyrics")
        val plain = entry.text("plainLyrics")
        if (synced.isBlank() && plain.isBlank()) return null
        val syncedLines = LrcParser.parse(synced)
        return if (syncedLines.isNotEmpty()) {
            Lyrics(lines = syncedLines, plainText = plain, synced = true, source = SOURCE)
        } else {
            Lyrics(lines = LrcParser.plainLines(plain), plainText = plain, synced = false, source = SOURCE)
        }
    }

    private fun getUrl(track: Track): String = buildString {
        append("https://lrclib.net/api/get?track_name=")
        append(encode(track.title))
        append("&artist_name=")
        append(encode(track.artist))
        if (track.album.isNotBlank()) {
            append("&album_name=")
            append(encode(track.album))
        }
        if (track.durationMs > 0L) {
            append("&duration=")
            append(track.durationMs / 1000L)
        }
    }

    private fun searchUrl(title: String, artist: String): String =
        "https://lrclib.net/api/search?track_name=${encode(title)}&artist_name=${encode(artist)}"

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private fun JsonObject.text(key: String): String =
        runCatching { this[key]?.jsonPrimitive?.content }.getOrNull().orEmpty()

    companion object {
        private const val SOURCE = "LRCLIB"
        private const val MAX_DURATION_DRIFT_SECONDS = 8.0
        private val decoration = Regex("""\s*[(\[][^)\]]*[)\]]""")
        private val featuring = Regex("""\s+(?:feat\.?|ft\.?|featuring)\s+.*$""", RegexOption.IGNORE_CASE)
        private val artistSeparator = Regex("""\s*(?:,|&|\bx\b|\bfeat\.?|\bft\.?|\bwith\b)\s*""", RegexOption.IGNORE_CASE)

        fun cleanTitle(title: String): String =
            title.replace(decoration, "").replace(featuring, "").trim().ifBlank { title.trim() }

        fun leadArtist(artist: String): String =
            artist.split(artistSeparator).firstOrNull { it.isNotBlank() }?.trim() ?: artist.trim()
    }
}
