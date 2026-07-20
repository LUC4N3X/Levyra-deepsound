package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

private const val RYD_API_HOST = "returnyoutubedislikeapi.com"
private const val RYD_API_BASE_URL = "https://$RYD_API_HOST"
private val YOUTUBE_VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")

internal data class ReturnYoutubeDislikeEstimate(
    val videoId: String,
    val likes: Long,
    val dislikes: Long,
    val rawLikes: Long,
    val rawDislikes: Long,
    val viewCount: Long,
    val rating: Double,
    val deleted: Boolean
)

internal sealed interface ReturnYoutubeDislikeResult {
    data class Available(val estimate: ReturnYoutubeDislikeEstimate) : ReturnYoutubeDislikeResult
    data object NotFound : ReturnYoutubeDislikeResult
    data class RateLimited(val retryAfterMs: Long) : ReturnYoutubeDislikeResult
    data class Failed(val cause: Throwable? = null) : ReturnYoutubeDislikeResult
}

/**
 * Read-only client for Return YouTube Dislike.
 *
 * The service returns an estimate, not an official YouTube dislike counter. Levyra never submits
 * votes to it and never presents the result as an official count.
 */
internal class ReturnYoutubeDislikeRepository {
    private val client = LevyraHttpClientFactory.general().newBuilder()
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .writeTimeout(4, TimeUnit.SECONDS)
        .callTimeout(8, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cache = ConcurrentHashMap<String, CachedEstimate>()
    private val inFlight = ConcurrentHashMap<String, Deferred<ReturnYoutubeDislikeResult>>()
    private val blockedUntilMs = AtomicLong(0L)

    suspend fun estimate(videoId: String): ReturnYoutubeDislikeResult {
        val normalizedId = videoId.trim()
        if (!YOUTUBE_VIDEO_ID.matches(normalizedId)) {
            return ReturnYoutubeDislikeResult.Failed(IllegalArgumentException("Invalid YouTube video id"))
        }

        val now = System.currentTimeMillis()
        cache[normalizedId]?.let { cached ->
            if (now < cached.expiresAtMs) {
                val cachedResult = cached.result
                if (cachedResult is ReturnYoutubeDislikeResult.RateLimited) {
                    val remainingMs = blockedUntilMs.get() - now
                    if (remainingMs > 0L) {
                        return ReturnYoutubeDislikeResult.RateLimited(remainingMs)
                    }
                    cache.remove(normalizedId, cached)
                } else {
                    return cachedResult
                }
            } else {
                cache.remove(normalizedId, cached)
            }
        }

        val blockedUntil = blockedUntilMs.get()
        if (now < blockedUntil) {
            return ReturnYoutubeDislikeResult.RateLimited(blockedUntil - now)
        }

        val created = scope.async(start = CoroutineStart.LAZY) { fetch(normalizedId) }
        created.invokeOnCompletion { inFlight.remove(normalizedId, created) }
        val shared = inFlight.putIfAbsent(normalizedId, created) ?: created
        if (shared === created) {
            created.start()
        } else {
            created.cancel()
        }
        return shared.await()
    }

    fun close() {
        scope.cancel()
        inFlight.clear()
        cache.clear()
    }

    private suspend fun fetch(videoId: String): ReturnYoutubeDislikeResult = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$RYD_API_BASE_URL/votes?videoId=$videoId")
            .get()
            .header("Accept", "application/json")
            .header("User-Agent", "Levyra-Android/1.0 (read-only engagement metadata)")
            .build()

        val result = try {
            client.newCall(request).execute().use { response ->
                // Redirects are deliberately disabled. The API endpoint is fixed and must not move
                // requests to an untrusted destination.
                if (response.isRedirect) {
                    ReturnYoutubeDislikeResult.Failed(IOException("Unexpected RYD redirect"))
                } else {
                    when (response.code) {
                        200 -> {
                            val body = response.body.string()
                            val estimate = parseReturnYoutubeDislikeResponse(body, videoId)
                            if (estimate == null) {
                                ReturnYoutubeDislikeResult.Failed(IOException("Invalid RYD response"))
                            } else {
                                ReturnYoutubeDislikeResult.Available(estimate)
                            }
                        }
                        404 -> ReturnYoutubeDislikeResult.NotFound
                        429 -> {
                            val retryAfterMs = response.header("Retry-After")
                                ?.trim()
                                ?.toLongOrNull()
                                ?.coerceIn(1L, 3_600L)
                                ?.times(1_000L)
                                ?: DEFAULT_RATE_LIMIT_BACKOFF_MS
                            blockedUntilMs.set(System.currentTimeMillis() + retryAfterMs)
                            ReturnYoutubeDislikeResult.RateLimited(retryAfterMs)
                        }
                        else -> ReturnYoutubeDislikeResult.Failed(IOException("RYD HTTP ${response.code}"))
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            Timber.d(error, "Return YouTube Dislike unavailable for %s", videoId)
            ReturnYoutubeDislikeResult.Failed(error)
        }

        val ttlMs = when (result) {
            is ReturnYoutubeDislikeResult.Available -> POSITIVE_TTL_MS
            ReturnYoutubeDislikeResult.NotFound -> NOT_FOUND_TTL_MS
            is ReturnYoutubeDislikeResult.RateLimited -> result.retryAfterMs.coerceAtLeast(MIN_RATE_LIMIT_CACHE_MS)
            is ReturnYoutubeDislikeResult.Failed -> FAILURE_TTL_MS
        }
        cacheResult(videoId, result, ttlMs)
        result
    }

    private fun cacheResult(videoId: String, result: ReturnYoutubeDislikeResult, ttlMs: Long) {
        val now = System.currentTimeMillis()
        if (cache.size >= MAX_CACHE_ENTRIES) {
            cache.entries.removeIf { now >= it.value.expiresAtMs }
            if (cache.size >= MAX_CACHE_ENTRIES) {
                cache.entries.minByOrNull { it.value.expiresAtMs }?.let { cache.remove(it.key, it.value) }
            }
        }
        cache[videoId] = CachedEstimate(result, now + ttlMs)
    }

    private data class CachedEstimate(
        val result: ReturnYoutubeDislikeResult,
        val expiresAtMs: Long
    )

    private companion object {
        const val MAX_CACHE_ENTRIES = 256
        const val POSITIVE_TTL_MS = 2L * 60L * 60L * 1_000L
        const val NOT_FOUND_TTL_MS = 30L * 60L * 1_000L
        const val FAILURE_TTL_MS = 60L * 1_000L
        const val DEFAULT_RATE_LIMIT_BACKOFF_MS = 15L * 60L * 1_000L
        const val MIN_RATE_LIMIT_CACHE_MS = 60L * 1_000L
    }
}

internal fun parseReturnYoutubeDislikeResponse(
    json: String,
    expectedVideoId: String
): ReturnYoutubeDislikeEstimate? {
    if (!YOUTUBE_VIDEO_ID.matches(expectedVideoId)) return null
    return runCatching {
        val root = JSONObject(json)
        val responseId = root.optString("id").trim()
        if (responseId != expectedVideoId) return null

        val likes = root.optLongStrict("likes") ?: return null
        val dislikes = root.optLongStrict("dislikes") ?: return null
        val rawLikes = root.optLongStrict("rawLikes") ?: 0L
        val rawDislikes = root.optLongStrict("rawDislikes") ?: 0L
        val parsedViewCount = root.optLongStrict("viewCount")
        if (parsedViewCount != null && parsedViewCount < 0L) return null
        val viewCount = parsedViewCount ?: -1L
        val rating = root.optDouble("rating", 0.0).takeIf(Double::isFinite) ?: 0.0
        val deleted = root.optBoolean("deleted", false)

        if (likes < 0L || dislikes < 0L || rawLikes < 0L || rawDislikes < 0L) return null

        ReturnYoutubeDislikeEstimate(
            videoId = responseId,
            likes = likes,
            dislikes = dislikes,
            rawLikes = rawLikes,
            rawDislikes = rawDislikes,
            viewCount = viewCount,
            rating = rating,
            deleted = deleted
        )
    }.getOrNull()
}

private fun JSONObject.optLongStrict(name: String): Long? {
    if (!has(name) || isNull(name)) return null
    return when (val value = opt(name)) {
        null -> null
        is Number -> value.toLong()
        is String -> value.trim().toLongOrNull()
        else -> null
    }
}
