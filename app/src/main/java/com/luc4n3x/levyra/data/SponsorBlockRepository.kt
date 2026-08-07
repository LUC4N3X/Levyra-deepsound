package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.SponsorSegment
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * SponsorBlock community segments (sponsor.ajay.app) so LEVYRA can auto-skip the
 * non-music parts of YouTube videos — sponsor reads, intros, outros, etc.
 */
class SponsorBlockRepository {

    private val categories = listOf("sponsor", "selfpromo", "intro", "outro", "interaction", "music_offtopic", "preview")

    private val cache = object : java.util.LinkedHashMap<String, List<SponsorSegment>>(128, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<SponsorSegment>>?): Boolean {
            return size > 200
        }
    }

    suspend fun segments(videoId: String): List<SponsorSegment> = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) return@withContext emptyList()
        synchronized(cache) {
            cache[videoId]?.let { return@withContext it }
        }
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
        try {
            val code = connection.responseCode
            // 404 means "no segments for this video" and is safe to negative-cache.
            if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                return@withContext publishSponsorBlockCacheResult(cache, videoId, emptyList())
            }
            // Do not poison the in-memory cache for rate limits or transient server/network failures.
            if (code !in 200..299) return@withContext emptyList()

            val declaredLength = connection.contentLengthLong
            if (declaredLength > SPONSORBLOCK_MAX_RESPONSE_BYTES) return@withContext emptyList()
            val body = connection.inputStream.use { input ->
                readUtf8Bounded(input, SPONSORBLOCK_MAX_RESPONSE_BYTES) ?: return@withContext emptyList()
            }
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
            val sortedSegments = segments.sortedBy { it.startMs }
            publishSponsorBlockCacheResult(cache, videoId, sortedSegments)
        } finally {
            connection.disconnect()
        }
    }
}

private const val SPONSORBLOCK_MAX_RESPONSE_BYTES = 512L * 1024L

internal fun publishSponsorBlockCacheResult(
    cache: MutableMap<String, List<SponsorSegment>>,
    videoId: String,
    result: List<SponsorSegment>
): List<SponsorSegment> = synchronized(cache) {
    cache[videoId] ?: result.also { cache[videoId] = it }
}

internal fun readUtf8Bounded(input: InputStream, maxBytes: Long): String? {
    require(maxBytes > 0L)
    val output = ByteArrayOutputStream(minOf(maxBytes, 16L * 1024L).toInt())
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        total += read
        if (total > maxBytes) return null
        output.write(buffer, 0, read)
    }
    return output.toString(StandardCharsets.UTF_8.name())
}