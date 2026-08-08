package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.SponsorSegment
import java.io.ByteArrayOutputStream
import java.io.Closeable
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
class SponsorBlockRepository internal constructor(
    private val fetcher: SponsorBlockHttpFetcher,
    private val clockMs: () -> Long
) {
    constructor() : this(UrlConnectionSponsorBlockHttpFetcher(), System::currentTimeMillis)

    private val categories = listOf("sponsor", "selfpromo", "intro", "outro", "interaction", "music_offtopic", "preview")

    private val cache = object : java.util.LinkedHashMap<String, SponsorBlockCacheEntry>(128, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, SponsorBlockCacheEntry>?): Boolean {
            return size > SPONSORBLOCK_CACHE_LIMIT
        }
    }

    suspend fun segments(videoId: String): List<SponsorSegment> = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) return@withContext emptyList()
        cachedSponsorBlockResult(cache, videoId, clockMs())?.let { return@withContext it }

        val catsJson = categories.joinToString(",", prefix = "[", postfix = "]") { ""$it"" }
        val cats = URLEncoder.encode(catsJson, "UTF-8")
        val url = "https://sponsor.ajay.app/api/skipSegments?videoID=$videoId&categories=$cats"

        val response = runCatching { fetcher.fetch(url) }.getOrNull() ?: return@withContext emptyList()
        response.use {
            when {
                response.code == HttpURLConnection.HTTP_NOT_FOUND -> {
                    return@withContext publishSponsorBlockCacheResult(cache, videoId, emptyList(), clockMs())
                }
                response.code !in 200..299 -> return@withContext emptyList()
                response.declaredLength > SPONSORBLOCK_MAX_RESPONSE_BYTES -> return@withContext emptyList()
            }

            val input = response.body ?: return@withContext emptyList()
            val body = readUtf8Bounded(input, SPONSORBLOCK_MAX_RESPONSE_BYTES)
                ?: return@withContext emptyList()
            val array = runCatching { JSONArray(body) }.getOrNull() ?: return@withContext emptyList()
            val parsed = buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val range = item.optJSONArray("segment") ?: continue
                    if (range.length() < 2) continue
                    val startMs = (range.optDouble(0, 0.0) * 1000).toLong()
                    val endMs = (range.optDouble(1, 0.0) * 1000).toLong()
                    if (endMs > startMs) {
                        add(SponsorSegment(startMs, endMs, item.optString("category", "sponsor")))
                    }
                }
            }.sortedBy { it.startMs }
            publishSponsorBlockCacheResult(cache, videoId, parsed, clockMs())
        }
    }
}

internal const val SPONSORBLOCK_MAX_RESPONSE_BYTES = 512L * 1024L
internal const val SPONSORBLOCK_CACHE_LIMIT = 200
internal const val SPONSORBLOCK_NEGATIVE_TTL_MS = 2L * 60L * 1000L
internal const val SPONSORBLOCK_POSITIVE_TTL_MS = 30L * 60L * 1000L

internal data class SponsorBlockCacheEntry(
    val segments: List<SponsorSegment>,
    val expiresAtMs: Long
)

internal fun cachedSponsorBlockResult(
    cache: MutableMap<String, SponsorBlockCacheEntry>,
    videoId: String,
    nowMs: Long
): List<SponsorSegment>? = synchronized(cache) {
    val entry = cache[videoId] ?: return@synchronized null
    if (entry.expiresAtMs <= nowMs) {
        cache.remove(videoId)
        null
    } else {
        entry.segments
    }
}

internal fun publishSponsorBlockCacheResult(
    cache: MutableMap<String, SponsorBlockCacheEntry>,
    videoId: String,
    result: List<SponsorSegment>,
    nowMs: Long
): List<SponsorSegment> = synchronized(cache) {
    val existing = cache[videoId]?.takeIf { it.expiresAtMs > nowMs }
    when {
        existing == null -> {
            cache[videoId] = SponsorBlockCacheEntry(result, nowMs + sponsorBlockTtl(result))
            result
        }
        existing.segments.isNotEmpty() -> existing.segments
        result.isNotEmpty() -> {
            cache[videoId] = SponsorBlockCacheEntry(result, nowMs + SPONSORBLOCK_POSITIVE_TTL_MS)
            result
        }
        else -> existing.segments
    }
}

private fun sponsorBlockTtl(segments: List<SponsorSegment>): Long =
    if (segments.isEmpty()) SPONSORBLOCK_NEGATIVE_TTL_MS else SPONSORBLOCK_POSITIVE_TTL_MS

internal fun interface SponsorBlockHttpFetcher {
    fun fetch(url: String): SponsorBlockHttpResponse
}

internal class SponsorBlockHttpResponse(
    val code: Int,
    val declaredLength: Long,
    val body: InputStream?,
    private val closeAction: () -> Unit = {}
) : Closeable {
    override fun close() {
        runCatching { body?.close() }
        closeAction()
    }
}

private class UrlConnectionSponsorBlockHttpFetcher : SponsorBlockHttpFetcher {
    override fun fetch(url: String): SponsorBlockHttpResponse {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 9000
            readTimeout = 11000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "LEVYRA Music Player (Android)")
        }
        return try {
            val code = connection.responseCode
            val body = if (code in 200..299) connection.inputStream else null
            SponsorBlockHttpResponse(
                code = code,
                declaredLength = connection.contentLengthLong,
                body = body,
                closeAction = connection::disconnect
            )
        } catch (error: Throwable) {
            connection.disconnect()
            throw error
        }
    }
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
