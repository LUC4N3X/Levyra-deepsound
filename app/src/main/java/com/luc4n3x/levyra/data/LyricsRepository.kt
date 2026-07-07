package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.domain.LyricLine
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
import kotlin.math.absoluteValue

class LyricsRepository(context: Context? = null) {
    private val cacheDir = context?.applicationContext?.cacheDir?.let { File(it, "lyrics_pro") }
    private val memory = LinkedHashMap<String, LyricsResult>()

    data class LyricsResult(
        val synced: Boolean,
        val lines: List<LyricLine>,
        val provider: String,
        val confidence: Int,
        val cached: Boolean
    )

    private data class Candidate(
        val result: LyricsResult,
        val title: String,
        val artist: String,
        val durationSec: Long
    )

    suspend fun fetch(title: String, artist: String, durationSec: Long): LyricsResult? = withContext(Dispatchers.IO) {
        val cleanTitle = cleanTitle(title)
        val cleanArtist = cleanArtist(artist)
        if (cleanTitle.length < 2 || cleanArtist.length < 2) return@withContext null
        val key = cacheKey(cleanTitle, cleanArtist, durationSec)
        memory[key]?.let { return@withContext it.copy(cached = true) }
        readCache(key)?.let { cached ->
            memory[key] = cached
            return@withContext cached
        }
        val candidates = mutableListOf<Candidate>()
        artistVariants(cleanArtist).forEach { artistVariant ->
            runCatching { getLrcLibExact(cleanTitle, artistVariant, durationSec) }
                .getOrNull()
                ?.let { candidates += it }
        }
        runCatching { searchLrcLib(cleanTitle, cleanArtist) }
            .getOrDefault(emptyList())
            .let { candidates += it }
        if (candidates.none { it.result.lines.isNotEmpty() }) {
            runCatching { lyricsOvh(cleanTitle, cleanArtist) }
                .getOrNull()
                ?.let { candidates += it }
        }
        val best = candidates
            .map { candidate -> candidate.result.copy(confidence = score(candidate, cleanTitle, cleanArtist, durationSec)) }
            .filter { it.lines.isNotEmpty() && it.confidence >= 42 }
            .sortedWith(compareByDescending<LyricsResult> { it.synced }.thenByDescending { it.confidence }.thenByDescending { it.lines.size })
            .firstOrNull()
        if (best != null) {
            val stable = best.copy(cached = false)
            memory[key] = stable
            writeCache(key, stable)
        }
        best
    }

    private fun getLrcLibExact(title: String, artist: String, durationSec: Long): Candidate? {
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
        return Candidate(result, json.optString("trackName", title), json.optString("artistName", artist), json.optLong("duration", durationSec))
    }

    private fun searchLrcLib(title: String, artist: String): List<Candidate> {
        val url = "https://lrclib.net/api/search?track_name=${enc(title)}&artist_name=${enc(artist)}"
        val body = httpGet(url, "application/json") ?: return emptyList()
        val array = JSONArray(body)
        val out = ArrayList<Candidate>()
        for (i in 0 until array.length()) {
            val json = array.optJSONObject(i) ?: continue
            val result = parseLrcLibEntry(json, "LRCLIB Search") ?: continue
            out += Candidate(result, json.optString("trackName", title), json.optString("artistName", artist), json.optLong("duration", 0L))
        }
        return out.take(12)
    }

    private fun lyricsOvh(title: String, artist: String): Candidate? {
        val url = "https://api.lyrics.ovh/v1/${encPath(artist)}/${encPath(title)}"
        val body = httpGet(url, "application/json") ?: return null
        val lyrics = JSONObject(body).optString("lyrics").trim()
        val lines = plainLines(lyrics)
        if (lines.isEmpty()) return null
        return Candidate(LyricsResult(false, lines, "Lyrics.ovh", 54, false), title, artist, 0L)
    }

    private fun parseLrcLibEntry(json: JSONObject, provider: String): LyricsResult? {
        val syncedText = json.optString("syncedLyrics").takeIf { it.isMeaningfulLyrics() }
        if (syncedText != null) {
            val lines = parseLrc(syncedText)
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

    private fun parseLrc(lrc: String): List<LyricLine> {
        val regex = Regex("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?]")
        val raw = mutableListOf<Pair<Long, String>>()
        lrc.split("\n").forEach { line ->
            val matches = regex.findAll(line).toList()
            if (matches.isEmpty()) return@forEach
            val text = line.substring(matches.last().range.last + 1).trim()
            matches.forEach { match ->
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val frac = match.groupValues[3]
                val ms = when (frac.length) {
                    1 -> frac.toLongOrNull()?.times(100L) ?: 0L
                    2 -> frac.toLongOrNull()?.times(10L) ?: 0L
                    3 -> frac.toLongOrNull() ?: 0L
                    else -> 0L
                }
                raw += (min * 60_000L + sec * 1000L + ms) to text
            }
        }
        val sorted = raw.filter { it.second.isNotBlank() }.sortedBy { it.first }
        return sorted.mapIndexed { index, (start, text) ->
            val end = sorted.getOrNull(index + 1)?.first?.minus(80L)?.coerceAtLeast(start + 800L) ?: (start + 6000L)
            LyricLine(start, end, text, "")
        }
    }

    private fun score(candidate: Candidate, title: String, artist: String, durationSec: Long): Int {
        val titleScore = similarity(candidate.title.cleanComparable(), title.cleanComparable())
        val artistScore = similarity(candidate.artist.cleanComparable(), artist.cleanComparable())
        val durationScore = when {
            durationSec <= 0L || candidate.durationSec <= 0L -> 12
            (candidate.durationSec - durationSec).absoluteValue <= 3L -> 18
            (candidate.durationSec - durationSec).absoluteValue <= 8L -> 12
            (candidate.durationSec - durationSec).absoluteValue <= 16L -> 6
            else -> -12
        }
        val syncScore = if (candidate.result.synced) 18 else 4
        val sizeScore = candidate.result.lines.size.coerceAtMost(40) / 2
        return (titleScore * 36 / 100 + artistScore * 26 / 100 + durationScore + syncScore + sizeScore).coerceIn(0, 100)
    }

    private fun similarity(left: String, right: String): Int {
        if (left.isBlank() || right.isBlank()) return 0
        if (left == right) return 100
        if (left.contains(right) || right.contains(left)) return 84
        val a = left.split(" ").filter { it.isNotBlank() }.toSet()
        val b = right.split(" ").filter { it.isNotBlank() }.toSet()
        if (a.isEmpty() || b.isEmpty()) return 0
        val intersection = a.intersect(b).size
        val union = a.union(b).size.coerceAtLeast(1)
        return (intersection * 100 / union).coerceIn(0, 100)
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
                parsed += LyricLine(item.optLong("startMs"), item.optLong("endMs"), item.optString("text"), item.optString("translated"))
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
                lines.put(JSONObject().put("startMs", line.startMs).put("endMs", line.endMs).put("text", line.text).put("translated", line.translated))
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

    private fun cacheKey(title: String, artist: String, durationSec: Long): String {
        val seed = "${title.cleanComparable()}|${artist.cleanComparable()}|${durationSec.coerceAtLeast(0L) / 10L}"
        return MessageDigest.getInstance("SHA-256").digest(seed.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun encPath(value: String): String = value.split("/").joinToString("%2F") { enc(it) }

    companion object {
        private const val CACHE_TTL_MS = 30L * 24L * 60L * 60L * 1000L
    }
}
