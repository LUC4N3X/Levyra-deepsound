package com.luc4n3x.levyra.desktop.core.sponsorblock

import com.luc4n3x.levyra.desktop.core.extractor.ExtractorHttp
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

class SponsorBlockRepository(
    private val client: OkHttpClient = ExtractorHttp.client,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val nowMs: () -> Long = System::currentTimeMillis
) {
    private val cache = object : LinkedHashMap<String, CachedSegments>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedSegments>): Boolean =
            size > CACHE_LIMIT
    }

    suspend fun segmentsFor(videoId: String): List<SponsorSegment> = withContext(dispatcher) {
        if (videoId.isBlank()) return@withContext emptyList()
        cached(videoId)?.let { return@withContext it }
        val response = fetch(videoId) ?: return@withContext emptyList()
        when (response) {
            is SponsorBlockResponse.Empty -> publish(videoId, emptyList())
            is SponsorBlockResponse.Body -> {
                val parsed = parseSponsorSegments(response.text, videoId) ?: return@withContext emptyList()
                publish(videoId, parsed)
            }
        }
    }

    private suspend fun fetch(videoId: String): SponsorBlockResponse? {
        val categories = URLEncoder.encode(
            SKIPPABLE_CATEGORIES.joinToString(",", prefix = "[", postfix = "]") { "\"$it\"" },
            StandardCharsets.UTF_8
        )
        val request = Request.Builder()
            .url("$SEGMENTS_ENDPOINT${hashPrefixOf(videoId)}?categories=$categories")
            .get()
            .header("User-Agent", ExtractorHttp.DESKTOP_USER_AGENT)
            .header("Accept", "application/json")
            .build()
        val call = client.newCall(request)
        val cancellationHandle = currentCoroutineContext()[Job]?.invokeOnCompletion {
            call.cancel()
        }
        return try {
            call.execute().use { response ->
                when {
                    response.code == HTTP_NOT_FOUND -> SponsorBlockResponse.Empty
                    !response.isSuccessful -> null
                    response.body.contentLength() > MAX_RESPONSE_BYTES -> null
                    else -> boundedBody(response)
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            null
        } finally {
            cancellationHandle?.dispose()
        }
    }

    private fun boundedBody(response: Response): SponsorBlockResponse? {
        val bytes = response.peekBody(MAX_RESPONSE_BYTES + 1).bytes()
        if (bytes.size > MAX_RESPONSE_BYTES) return null
        return SponsorBlockResponse.Body(bytes.toString(Charsets.UTF_8))
    }

    private fun cached(videoId: String): List<SponsorSegment>? = synchronized(cache) {
        val entry = cache[videoId] ?: return@synchronized null
        if (entry.expiresAtMs <= nowMs()) {
            cache.remove(videoId)
            null
        } else {
            entry.segments
        }
    }

    private fun publish(videoId: String, segments: List<SponsorSegment>): List<SponsorSegment> =
        synchronized(cache) {
            val ttl = if (segments.isEmpty()) NEGATIVE_TTL_MS else POSITIVE_TTL_MS
            cache[videoId] = CachedSegments(segments, nowMs() + ttl)
            segments
        }

    private data class CachedSegments(
        val segments: List<SponsorSegment>,
        val expiresAtMs: Long
    )

    private sealed interface SponsorBlockResponse {
        data object Empty : SponsorBlockResponse
        data class Body(val text: String) : SponsorBlockResponse
    }

    private companion object {
        const val SEGMENTS_ENDPOINT = "https://sponsor.ajay.app/api/skipSegments/"
        const val HTTP_NOT_FOUND = 404
        const val MAX_RESPONSE_BYTES = 512L * 1024L
        const val CACHE_LIMIT = 200
        const val NEGATIVE_TTL_MS = 2L * 60L * 1000L
        const val POSITIVE_TTL_MS = 30L * 60L * 1000L
    }
}

internal val SKIPPABLE_CATEGORIES = listOf(
    "sponsor",
    "selfpromo",
    "intro",
    "outro",
    "interaction",
    "music_offtopic",
    "preview"
)

internal fun hashPrefixOf(videoId: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(videoId.toByteArray(StandardCharsets.UTF_8))
        .take(HASH_PREFIX_BYTES)
        .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xFF) }

internal fun parseSponsorSegments(body: String, videoId: String): List<SponsorSegment>? {
    val entries = runCatching { sponsorJson.parseToJsonElement(body).jsonArray }.getOrNull() ?: return null
    entries.forEach { element ->
        val candidate = runCatching { element.jsonObject }.getOrNull() ?: return@forEach
        if (candidate.text("videoID") != videoId) return@forEach
        val segments = candidate.array("segments") ?: return null
        return segments.mapNotNull { entry -> toSponsorSegment(entry) }.sortedBy { segment -> segment.startMs }
    }
    return emptyList()
}

private const val HASH_PREFIX_BYTES = 2

private val sponsorJson = Json { ignoreUnknownKeys = true }

private fun toSponsorSegment(element: JsonElement): SponsorSegment? {
    val entry = runCatching { element.jsonObject }.getOrNull() ?: return null
    val range = entry.array("segment") ?: return null
    if (range.size < 2) return null
    val startMs = range[0].milliseconds() ?: return null
    val endMs = range[1].milliseconds() ?: return null
    if (endMs <= startMs) return null
    val category = entry.text("category").trim()
    if (category !in SKIPPABLE_CATEGORIES) return null
    return SponsorSegment(
        startMs = startMs,
        endMs = endMs,
        category = category,
        uuid = entry.text("UUID").trim(),
        actionType = entry.text("actionType").trim().ifBlank { SPONSOR_SEGMENT_ACTION_SKIP }
    )
}

private fun JsonObject.text(key: String): String =
    runCatching { this[key]?.jsonPrimitive?.content }.getOrNull().orEmpty()

private fun JsonObject.array(key: String): JsonArray? =
    runCatching { this[key]?.jsonArray }.getOrNull()

private fun JsonElement.milliseconds(): Long? {
    val seconds = runCatching { jsonPrimitive.content.toDouble() }.getOrNull() ?: return null
    if (!seconds.isFinite() || seconds < 0.0) return null
    return (seconds * 1000.0).toLong()
}
