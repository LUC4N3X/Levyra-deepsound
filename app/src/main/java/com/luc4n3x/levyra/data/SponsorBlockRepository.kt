package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.SponsorSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * SponsorBlock community segments (sponsor.ajay.app) so LEVYRA can auto-skip the
 * non-music parts of YouTube videos — sponsor reads, intros, outros, etc.
 */
class SponsorBlockRepository {

    private val categories = listOf("sponsor", "selfpromo", "intro", "outro", "interaction", "music_offtopic", "preview")

    suspend fun segments(videoId: String): List<SponsorSegment> = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) return@withContext emptyList()
        val catsJson = categories.joinToString(",", prefix = "[", postfix = "]") { "\"$it\"" }
        val cats = URLEncoder.encode(catsJson, "UTF-8")
        val url = "https://sponsor.ajay.app/api/skipSegments?videoID=$videoId&categories=$cats"
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 9000
            readTimeout = 11000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "LEVYRA Music Player (Android)")
        }
        val code = connection.responseCode
        // 404 means "no segments for this video" — perfectly normal.
        if (code !in 200..299) return@withContext emptyList()
        val body = BufferedReader(InputStreamReader(connection.inputStream, StandardCharsets.UTF_8)).use { it.readText() }
        val array = runCatching { JSONArray(body) }.getOrNull() ?: return@withContext emptyList()
        val segments = mutableListOf<SponsorSegment>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val range = item.optJSONArray("segment") ?: continue
            if (range.length() < 2) continue
            val startMs = (range.optDouble(0, 0.0) * 1000).toLong()
            val endMs = (range.optDouble(1, 0.0) * 1000).toLong()
            if (endMs > startMs) {
                segments += SponsorSegment(startMs, endMs, item.optString("category", "sponsor"))
            }
        }
        segments.sortedBy { it.startMs }
    }
}
