package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.LyricLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** Real lyrics (synced when available) from the free lrclib.net API — no auth required. */
class LyricsRepository {

    data class LyricsResult(val synced: Boolean, val lines: List<LyricLine>)

    suspend fun fetch(title: String, artist: String, durationSec: Long): LyricsResult? = withContext(Dispatchers.IO) {
        val cleanTitle = cleanTitle(title)
        val cleanArtist = cleanArtist(artist)
        // Direct match first, then a fuzzy search fallback.
        val direct = runCatching { getExact(cleanTitle, cleanArtist, durationSec) }.getOrNull()
        if (direct != null) return@withContext direct
        runCatching { searchFirst(cleanTitle, cleanArtist) }.getOrNull()
    }

    private fun getExact(title: String, artist: String, durationSec: Long): LyricsResult? {
        val url = buildString {
            append("https://lrclib.net/api/get?track_name=")
            append(enc(title))
            append("&artist_name=")
            append(enc(artist))
            if (durationSec > 0) append("&duration=").append(durationSec)
        }
        val body = httpGet(url) ?: return null
        return parseEntry(JSONObject(body))
    }

    private fun searchFirst(title: String, artist: String): LyricsResult? {
        val url = "https://lrclib.net/api/search?track_name=${enc(title)}&artist_name=${enc(artist)}"
        val body = httpGet(url) ?: return null
        val array = JSONArray(body)
        for (i in 0 until array.length()) {
            val result = parseEntry(array.optJSONObject(i) ?: continue)
            if (result != null && result.lines.isNotEmpty()) return result
        }
        return null
    }

    private fun parseEntry(json: JSONObject): LyricsResult? {
        val synced = json.optString("syncedLyrics").takeIf { it.isNotBlank() }
        if (synced != null) {
            val lines = parseLrc(synced)
            if (lines.isNotEmpty()) return LyricsResult(true, lines)
        }
        val plain = json.optString("plainLyrics").takeIf { it.isNotBlank() } ?: return null
        val lines = plain.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            .map { LyricLine(0L, 0L, it, "") }
        if (lines.isEmpty()) return null
        return LyricsResult(false, lines)
    }

    private fun parseLrc(lrc: String): List<LyricLine> {
        val regex = Regex("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?]")
        val raw = mutableListOf<Pair<Long, String>>()
        lrc.split("\n").forEach { line ->
            val matches = regex.findAll(line).toList()
            if (matches.isEmpty()) return@forEach
            val text = line.substring(matches.last().range.last + 1).trim()
            matches.forEach { m ->
                val min = m.groupValues[1].toLong()
                val sec = m.groupValues[2].toLong()
                val frac = m.groupValues[3]
                val ms = when (frac.length) {
                    1 -> frac.toLong() * 100
                    2 -> frac.toLong() * 10
                    3 -> frac.toLong()
                    else -> 0L
                }
                raw += (min * 60_000L + sec * 1000L + ms) to text
            }
        }
        val sorted = raw.filter { it.second.isNotBlank() }.sortedBy { it.first }
        return sorted.mapIndexed { index, (start, text) ->
            val end = sorted.getOrNull(index + 1)?.first ?: (start + 6000L)
            LyricLine(start, end, text, "")
        }
    }

    private fun httpGet(url: String): String? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 12000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "LEVYRA Music Player (Android)")
        }
        val code = connection.responseCode
        if (code !in 200..299) return null
        return BufferedReader(InputStreamReader(connection.inputStream, StandardCharsets.UTF_8)).use { it.readText() }
    }

    private fun cleanTitle(title: String): String =
        title.replace(Regex("(?i)\\s*[(\\[].*?(remaster|radio edit|video|official|lyrics|prod\\.|feat\\.).*?[)\\]]"), "")
            .replace(Regex("(?i)\\s*-\\s*(official|video|audio).*$"), "")
            .trim()

    private fun cleanArtist(artist: String): String =
        artist.split(",", " e ", " & ", " feat", " ft", " x ").firstOrNull()?.trim().orEmpty().ifBlank { artist }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
}
