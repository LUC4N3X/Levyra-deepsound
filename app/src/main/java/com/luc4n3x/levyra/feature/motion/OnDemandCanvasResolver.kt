package com.luc4n3x.levyra.feature.motion

import android.content.Context
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.util.LinkedHashMap
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Resolves Spotify Canvases on demand from a Levyra-owned resolver service when
 * local/curated community canvas lookups miss.
 *
 * Requests send only minimal recording metadata (isrc, title, artist, album, durationMs)
 * and never include user credentials, device identifiers, or playback history.
 */
class OnDemandCanvasResolver(
    private val context: Context? = null,
    private val client: OkHttpClient = context?.let { LevyraHttpClientFactory.media(it) }?.newBuilder()
        ?.connectTimeout(3, TimeUnit.SECONDS)
        ?.readTimeout(3, TimeUnit.SECONDS)
        ?.callTimeout(4, TimeUnit.SECONDS)
        ?.build()
        ?: OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .callTimeout(4, TimeUnit.SECONDS)
            .build(),
    private val resolverUrl: String = DEFAULT_RESOLVER_URL,
    private val networkPolicyCheck: () -> Boolean = {
        context?.let { MotionArtworkNetworkPolicy.canUseMotionArtwork(it) } ?: true
    }
) {
    private val negativeCacheLock = Any()
    private val negativeCache = object : LinkedHashMap<String, Long>(MAX_NEGATIVE_CACHE_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean =
            size > MAX_NEGATIVE_CACHE_ENTRIES
    }

    suspend fun resolve(identity: MotionTrackIdentity): MotionArtworkCandidate? = withContext(Dispatchers.IO) {
        if (resolverUrl.isBlank() || !networkPolicyCheck()) return@withContext null

        val cacheKey = onDemandCacheKey(identity)
        val now = System.currentTimeMillis()
        if (isNegativeCached(cacheKey, now)) {
            Timber.d("On-demand canvas negative cache hit for %s", identity.title)
            return@withContext null
        }

        val request = buildRequest(identity)
        try {
            val response = client.newCall(request).awaitCancellable()
            response.use { resp ->
                parseCandidate(resp, identity, cacheKey, now)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            Timber.d(error, "On-demand canvas resolver request failed for %s", identity.title)
            null
        }
    }

    private fun isNegativeCached(key: String, nowMs: Long): Boolean =
        synchronized(negativeCacheLock) {
            val expiresAt = negativeCache[key]
            expiresAt != null && nowMs < expiresAt
        }

    private fun buildRequest(identity: MotionTrackIdentity): Request {
        val requestJson = JSONObject().apply {
            if (identity.isrc.isNotBlank()) put("isrc", identity.isrc)
            put("title", identity.title)
            put("artist", identity.artists.firstOrNull() ?: "")
            if (identity.album.isNotBlank()) put("album", identity.album)
            if (identity.durationMs > 0L) put("durationMs", identity.durationMs)
        }
        val body = requestJson.toString().toRequestBody(JSON_MEDIA_TYPE)
        return Request.Builder()
            .url(resolverUrl)
            .post(body)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .build()
    }

    private fun parseCandidate(
        resp: Response,
        identity: MotionTrackIdentity,
        cacheKey: String,
        nowMs: Long,
    ): MotionArtworkCandidate? {
        if (!resp.isSuccessful) {
            if (resp.code in 400..404) {
                recordNegative(cacheKey, nowMs)
            }
            Timber.d("On-demand canvas resolver returned HTTP %d for %s", resp.code, identity.title)
            return null
        }
        val raw = resp.body.string()
        if (raw.length > MAX_RESPONSE_BYTES) {
            Timber.d("On-demand canvas resolver response too large (%d bytes)", raw.length)
            return null
        }
        val payload = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        if (payload.optString("status") != "resolved") {
            recordNegative(cacheKey, nowMs)
            return null
        }
        val rawUrl = payload.optString("url").trim()
        if (communityCanvasMediaUrl(rawUrl) == null) {
            recordNegative(cacheKey, nowMs)
            Timber.d("On-demand canvas returned non-allowlisted media URL for %s", identity.title)
            return null
        }
        Timber.d("On-demand canvas resolved successfully: %s", rawUrl)
        return MotionArtworkCandidate(
            provider = CommunityCanvasProvider.PROVIDER_ID,
            scope = MotionArtworkScope.TRACK,
            identity = identity,
            url = rawUrl,
            mimeType = if (rawUrl.substringBefore('?').endsWith(".m3u8", true)) {
                "application/x-mpegURL"
            } else {
                "video/mp4"
            },
            expiresAtMs = System.currentTimeMillis() + MOTION_ARTWORK_POSITIVE_TTL_MS
        )
    }

    private fun recordNegative(key: String, nowMs: Long) {
        synchronized(negativeCacheLock) {
            negativeCache[key] = nowMs + NEGATIVE_CACHE_TTL_MS
        }
    }

    companion object {
        const val DEFAULT_RESOLVER_URL = "https://canvas.levyra.org/v1/resolve"
        const val USER_AGENT = "Levyra/1.0 (Android; MotionArtwork)"
        private const val MAX_RESPONSE_BYTES = 64 * 1024  // 64 KB
        private const val MAX_NEGATIVE_CACHE_ENTRIES = 128
        private const val NEGATIVE_CACHE_TTL_MS = 10L * 60L * 1000L  // 10 minutes
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

internal fun onDemandCacheKey(identity: MotionTrackIdentity): String =
    if (identity.isrc.isNotBlank()) {
        "isrc:${identity.isrc.uppercase(Locale.ROOT)}"
    } else {
        listOf(
            normalizeMotionText(identity.title),
            identity.artists.map(::normalizeMotionText).sorted().joinToString(","),
            (identity.durationMs / 1000L).toString()
        ).joinToString("|")
    }

private suspend fun Call.awaitCancellable(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation {
        cancel()
    }
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response)
        }

        override fun onFailure(call: Call, e: IOException) {
            if (!continuation.isCancelled) {
                continuation.resumeWithException(e)
            }
        }
    })
}
