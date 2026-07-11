package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.LyricWord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale

class LyricsRepository(context: Context? = null) {
    private val appContext = context?.applicationContext
    private val cacheDir = appContext?.cacheDir?.let { File(it, "lyrics_pro") }
    private val youtubeTranscript = appContext?.let(::YoutubeTranscriptLyricsProvider)
    private val memory = LinkedHashMap<String, LyricsResult>()

    data class LyricsResult(
        val synced: Boolean,
        val lines: List<LyricLine>,
        val provider: String,
        val confidence: Int,
        val cached: Boolean
    )

    suspend fun fetch(
        title: String,
        artist: String,
        durationSec: Long,
        videoId: String = "",
        languageCode: String = "",
        translate: Boolean = false
    ): LyricsResult? = withContext(Dispatchers.IO) {
        val cleanTitle = cleanTitle(title)
        val cleanArtist = cleanArtist(artist)
        if (cleanTitle.length < 2 || cleanArtist.length < 2) return@withContext null
        val key = cacheKey(cleanTitle, cleanArtist, durationSec, languageCode, translate)
        memory[key]?.let { return@withContext it.copy(cached = true) }
        readCache(key)?.let { cached ->
            memory[key] = cached
            return@withContext cached
        }
        val candidates = mutableListOf<LyricsCandidate>()
        artistVariants(cleanArtist).forEach { artistVariant ->
            runCatching { getLrcLibExact(cleanTitle, artistVariant, durationSec) }
                .getOrNull()
                ?.let { candidates += it }
        }
        runCatching { searchLrcLib(cleanTitle, cleanArtist) }
            .getOrDefault(emptyList())
            .let { candidates += it }
        if (videoId.isNotBlank()) {
            runCatching { youtubeTranscript?.fetch(videoId, languageCode, translate) }
                .getOrNull()
                ?.takeIf { it.lines.isNotEmpty() }
                ?.let { transcript ->
                    val provider = buildString {
                        append("YouTube Transcript")
                        if (transcript.automatic) append(" Auto")
                        append(" · ").append(transcript.sourceLanguage)
                        if (transcript.translated) append(" → ").append(languageCode)
                    }
                    candidates += LyricsCandidate(
                        result = LyricsResult(true, transcript.lines, provider, if (transcript.automatic) 72 else 82, false),
                        title = cleanTitle,
                        artist = cleanArtist,
                        durationSec = durationSec
                    )
                }
        }
        if (candidates.none { it.result.lines.isNotEmpty() }) {
            runCatching { lyricsOvh(cleanTitle, cleanArtist) }
                .getOrNull()
                ?.let { candidates += it }
        }
        val best = LyricsResultRanker.best(candidates, LyricsRequest(cleanTitle, cleanArtist, durationSec))
            ?.let { normalizeTiming(it, durationSec) }
        if (best != null) {
            val stable = best.copy(cached = false)
            memory[key] = stable
            writeCache(key, stable)
        }
        best
    }

    private fun getLrcLibExact(title: String, artist: String, durationSec: Long): LyricsCandidate? {
        val url = buildString {
            append("https://lrclib.net/api/get?track_name=")
            append(enc(title))
            append("&artist_name=")
            append(enc(artist))
            if (durationSec > 0) append("&duration=").append(durationSec)
        }
        val body = httpGet(url, "application/json") ?: return null
        val json = JSONObject(body)
        val result = parseLrcLibEntry(json, "LRCLIB Exact") ?: return null
        return LyricsCandidate(result, json.optString("trackName", title), json.optString("artistName", artist), json.optLong("duration", durationSec))
    }

    private fun searchLrcLib(title: String, artist: String): List<LyricsCandidate> {
        val url = "https://lrclib.net/api/search?track_name=${enc(title)}&artist_name=${enc(artist)}"
        val body = httpGet(url, "application/json") ?: return emptyList()
        val array = JSONArray(body)
        val out = ArrayList<LyricsCandidate>()
        for (i in 0 until array.length()) {
            val json = array.optJSONObject(i) ?: continue
            val result = parseLrcLibEntry(json, "LRCLIB Search") ?: continue
            out += LyricsCandidate(result, json.optString("trackName", title), json.optString("artistName", artist), json.optLong("duration", 0L))
        }
        return out.take(12)
    }

    private fun lyricsOvh(title: String, artist: String): LyricsCandidate? {
        val url = "https://api.lyrics.ovh/v1/${encPath(artist)}/${encPath(title)}"
        val body = httpGet(url, "application/json") ?: return null
        val lyrics = JSONObject(body).optString("lyrics").trim()
        val lines = plainLines(lyrics)
        if (lines.isEmpty()) return null
        return LyricsCandidate(LyricsResult(false, lines, "Lyrics.ovh", 54, false), title, artist, 0L)
    }

    private fun parseLrcLibEntry(json: JSONObject, provider: String): LyricsResult? {
        val syncedText = json.optString("syncedLyrics").takeIf { it.isMeaningfulLyrics() }
        if (syncedText != null) {
            val lines = LrcLyricsParser.parse(syncedText)
            if (lines.isNotEmpty()) return LyricsResult(true, lines, provider, 88, false)
        }
        val plain = json.optString("plainLyrics").takeIf { it.isMeaningfulLyrics() } ?: return null
        val lines = plainLines(plain)
        if (lines.isEmpty()) return null
        return LyricsResult(false, lines, provider, 68, false)
    }

    private fun plainLines(text: String): List<LyricLine> {
        return text.split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filterNot { it.equals("embed", ignoreCase = true) }
            .mapIndexed { index, line -> LyricLine(index * 4200L, (index + 1) * 4200L, line, "") }
    }

    private fun httpGet(url: String, accept: String): String? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 9000
            readTimeout = 11000
            setRequestProperty("Accept", accept)
            setRequestProperty("Accept-Encoding", "identity")
            setRequestProperty("User-Agent", "LEVYRA Lyrics Engine Pro/1.0 Android")
        }
        return try {
            val code = connection.responseCode
            if (code !in 200..299) return null
            BufferedReader(InputStreamReader(connection.inputStream, StandardCharsets.UTF_8)).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun readCache(key: String): LyricsResult? {
        val dir = cacheDir ?: return null
        val file = File(dir, "$key.json")
        if (!file.isFile || System.currentTimeMillis() - file.lastModified() > CACHE_TTL_MS) return null
        return runCatching {
            val json = JSONObject(file.readText())
            val lines = json.optJSONArray("lines") ?: JSONArray()
            val parsed = ArrayList<LyricLine>()
            for (i in 0 until lines.length()) {
                val item = lines.optJSONObject(i) ?: continue
                val wordsJson = item.optJSONArray("words") ?: JSONArray()
                val words = ArrayList<LyricWord>()
                for (wordIndex in 0 until wordsJson.length()) {
                    val word = wordsJson.optJSONObject(wordIndex) ?: continue
                    words += LyricWord(
                        startMs = word.optLong("startMs"),
                        endMs = word.optLong("endMs"),
                        text = word.optString("text")
                    )
                }
                parsed += LyricLine(
                    startMs = item.optLong("startMs"),
                    endMs = item.optLong("endMs"),
                    text = item.optString("text"),
                    translated = item.optString("translated"),
                    words = words
                )
            }
            if (parsed.isEmpty()) null else LyricsResult(json.optBoolean("synced"), parsed, json.optString("provider"), json.optInt("confidence", 70), true)
        }.onFailure { Timber.w(it, "Lyrics cache restore failed") }.getOrNull()
    }

    private fun writeCache(key: String, result: LyricsResult) {
        val dir = cacheDir ?: return
        runCatching {
            if (!dir.isDirectory) dir.mkdirs()
            val lines = JSONArray()
            result.lines.take(500).forEach { line ->
                val words = JSONArray()
                line.words.take(80).forEach { word ->
                    words.put(
                        JSONObject()
                            .put("startMs", word.startMs)
                            .put("endMs", word.endMs)
                            .put("text", word.text)
                    )
                }
                lines.put(
                    JSONObject()
                        .put("startMs", line.startMs)
                        .put("endMs", line.endMs)
                        .put("text", line.text)
                        .put("translated", line.translated)
                        .put("words", words)
                )
            }
            File(dir, "$key.json").writeText(
                JSONObject()
                    .put("synced", result.synced)
                    .put("provider", result.provider)
                    .put("confidence", result.confidence)
                    .put("lines", lines)
                    .toString()
            )
        }.onFailure { Timber.w(it, "Lyrics cache save failed") }
    }

    private fun cleanTitle(title: String): String = title
        .replace(Regex("(?i)\\s*[(\\[].*?(remaster|radio edit|video|official|lyrics|prod\\.|feat\\.|ft\\.).*?[)\\]]"), "")
        .replace(Regex("(?i)\\s*-\\s*(official|video|audio|lyrics).*$"), "")
        .trim()

    private fun cleanArtist(artist: String): String = artist
        .replace(Regex("(?i)\\s*VEVO$"), "")
        .trim()

    private fun artistVariants(artist: String): List<String> {
        val split = artist.split(",", " e ", " & ", " feat", " ft", " x ", " X ")
            .map { it.trim() }
            .filter { it.length >= 2 }
        return (listOf(artist) + split).distinctBy { it.lowercase(Locale.ROOT) }.take(4)
    }

    private fun String.cleanComparable(): String = lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N} ]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun String.isMeaningfulLyrics(): Boolean {
        val clean = trim()
        return clean.length >= 16 && !clean.equals("null", ignoreCase = true)
    }

    private fun cacheKey(title: String, artist: String, durationSec: Long, languageCode: String, translate: Boolean): String {
        val seed = "${title.cleanComparable()}|${artist.cleanComparable()}|${durationSec.coerceAtLeast(0L) / 10L}|${languageCode.lowercase(Locale.ROOT)}|$translate"
        return MessageDigest.getInstance("SHA-256").digest(seed.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    private fun normalizeTiming(result: LyricsResult, durationSec: Long): LyricsResult {
        val durationMs = durationSec.coerceAtLeast(0L) * 1_000L
        if (durationMs <= 0L || result.lines.isEmpty()) return result
        val sorted = result.lines.sortedBy { it.startMs }
        val lastEnd = sorted.maxOfOrNull { it.endMs } ?: return result
        if (lastEnd <= 0L) return result
        val ratio = durationMs.toDouble() / lastEnd.toDouble()
        val shouldScale = ratio in 0.82..1.18 && kotlin.math.abs(durationMs - lastEnd) >= 3_000L
        val scaled = if (shouldScale) {
            sorted.map { line ->
                line.copy(
                    startMs = (line.startMs * ratio).toLong().coerceAtLeast(0L),
                    endMs = (line.endMs * ratio).toLong().coerceAtMost(durationMs),
                    words = line.words.map { word ->
                        word.copy(
                            startMs = (word.startMs * ratio).toLong().coerceAtLeast(0L),
                            endMs = (word.endMs * ratio).toLong().coerceAtMost(durationMs)
                        )
                    }
                )
            }
        } else {
            sorted
        }
        val corrected = scaled.mapIndexed { index, line ->
            val nextStart = scaled.getOrNull(index + 1)?.startMs
            val safeEnd = nextStart?.minus(60L)?.coerceAtLeast(line.startMs + 350L)
                ?: line.endMs.coerceAtMost(durationMs).coerceAtLeast(line.startMs + 350L)
            line.copy(endMs = safeEnd.coerceAtMost(durationMs))
        }
        return result.copy(lines = corrected)
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun encPath(value: String): String = value.split("/").joinToString("%2F") { enc(it) }

    companion object {
        private const val CACHE_TTL_MS = 30L * 24L * 60L * 60L * 1000L
    }
}
