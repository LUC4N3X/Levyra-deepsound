package com.luc4n3x.levyra.feature.motion

import android.content.Context
import com.luc4n3x.levyra.BuildConfig
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
import okhttp3.ResponseBody
import org.json.JSONObject
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.util.LinkedHashMap
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resumeWithException

internal sealed interface OnDemandCanvasResolution {
    data class Found(val candidate: MotionArtworkCandidate) : OnDemandCanvasResolution
    data object NoMatch : OnDemandCanvasResolution
    data object Unavailable : OnDemandCanvasResolution
    data class Failed(val cause: Throwable? = null) : OnDemandCanvasResolution
}

private data class NegativeCacheRequestState(
    val key: String,
    val nowMs: Long,
    val generation: Long
)

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
    private val resolverUrl: String = BuildConfig.CANVAS_RESOLVER_URL,
    private val clientKey: String = BuildConfig.CANVAS_CLIENT_KEY,
    private val networkPolicyCheck: () -> Boolean = {
        context?.let { MotionArtworkNetworkPolicy.canUseMotionArtwork(it) } ?: true
    }
) {
    private val negativeCacheLock = Any()
    private val negativeCache = object : LinkedHashMap<String, Long>(MAX_NEGATIVE_CACHE_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean =
            size > MAX_NEGATIVE_CACHE_ENTRIES
    }
    private var negativeCacheGeneration = 0L

    suspend fun resolve(identity: MotionTrackIdentity): MotionArtworkCandidate? =
        when (val resolution = resolveResult(identity)) {
            is OnDemandCanvasResolution.Found -> resolution.candidate
            OnDemandCanvasResolution.NoMatch,
            OnDemandCanvasResolution.Unavailable,
            is OnDemandCanvasResolution.Failed -> null
        }

    internal suspend fun resolveResult(
        identity: MotionTrackIdentity
    ): OnDemandCanvasResolution = withContext(Dispatchers.IO) {
        if (resolverUrl.isBlank() || clientKey.isBlank() || !networkPolicyCheck()) {
            return@withContext OnDemandCanvasResolution.Unavailable
        }

        val cacheKey = onDemandCacheKey(identity)
        val now = System.currentTimeMillis()
        val requestState = negativeCacheRequestState(cacheKey, now)
        if (requestState == null) {
            Timber.d("On-demand canvas negative cache hit for %s", identity.title)
            return@withContext OnDemandCanvasResolution.NoMatch
        }

        try {
            val response = client.newCall(buildRequest(identity)).awaitCancellable()
            response.use { resp ->
                parseResolution(resp, identity, requestState)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Timber.d(error, "On-demand canvas resolver request failed for %s", identity.title)
            OnDemandCanvasResolution.Failed(error)
        }
    }

    internal fun invalidate(identity: MotionTrackIdentity) {
        val cacheKey = onDemandCacheKey(identity)
        synchronized(negativeCacheLock) {
            negativeCache.remove(cacheKey)
            negativeCacheGeneration += 1L
        }
        Timber.d("On-demand canvas negative cache invalidated for %s", identity.title)
    }

    private fun negativeCacheRequestState(
        key: String,
        nowMs: Long
    ): NegativeCacheRequestState? = synchronized(negativeCacheLock) {
        val expiresAt = negativeCache[key]
        if (expiresAt != null && nowMs < expiresAt) {
            return@synchronized null
        }
        if (expiresAt != null) negativeCache.remove(key)
        NegativeCacheRequestState(
            key = key,
            nowMs = nowMs,
            generation = negativeCacheGeneration
        )
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
            .header("X-Levyra-Key", clientKey)
            .build()
    }

    private fun parseResolution(
        resp: Response,
        identity: MotionTrackIdentity,
        requestState: NegativeCacheRequestState
    ): OnDemandCanvasResolution =
        if (resp.isSuccessful) {
            parseSuccessfulResolution(resp, identity, requestState)
        } else {
            parseHttpFailure(resp, identity, requestState)
        }

    private fun parseHttpFailure(
        resp: Response,
        identity: MotionTrackIdentity,
        requestState: NegativeCacheRequestState
    ): OnDemandCanvasResolution =
        if (resp.code == HttpURLConnection.HTTP_NOT_FOUND) {
            recordNegative(requestState)
            Timber.d("On-demand canvas resolver returned conclusive HTTP %d for %s", resp.code, identity.title)
            OnDemandCanvasResolution.NoMatch
        } else {
            val failure = IOException("On-demand canvas resolver HTTP ${resp.code}")
            Timber.d(failure, "On-demand canvas resolver transient HTTP failure for %s", identity.title)
            OnDemandCanvasResolution.Failed(failure)
        }

    private fun parseSuccessfulResolution(
        resp: Response,
        identity: MotionTrackIdentity,
        requestState: NegativeCacheRequestState
    ): OnDemandCanvasResolution {
        val declaredLength = resp.body.contentLength()
        return if (declaredLength > MAX_RESPONSE_BYTES) {
            val failure = IOException("On-demand canvas resolver response is too large")
            Timber.d(failure, "On-demand canvas resolver response declared size too large (%d bytes)", declaredLength)
            OnDemandCanvasResolution.Failed(failure)
        } else {
            parseBoundedResponseBody(resp, identity, requestState)
        }
    }

    private fun parseBoundedResponseBody(
        resp: Response,
        identity: MotionTrackIdentity,
        requestState: NegativeCacheRequestState
    ): OnDemandCanvasResolution {
        val raw = resp.body.readBoundedUtf8(MAX_RESPONSE_BYTES)
        return if (raw == null) {
            val failure = IOException("On-demand canvas resolver response exceeded size limit")
            Timber.d(failure, "On-demand canvas resolver response body exceeded %d bytes", MAX_RESPONSE_BYTES)
            OnDemandCanvasResolution.Failed(failure)
        } else {
            val payload = runCatching { JSONObject(raw) }.getOrNull()
            payload?.let { parsed ->
                parsePayloadResolution(parsed, identity, requestState)
            } ?: OnDemandCanvasResolution.Failed(
                IOException("On-demand canvas resolver returned invalid JSON")
            )
        }
    }

    private fun parsePayloadResolution(
        payload: JSONObject,
        identity: MotionTrackIdentity,
        requestState: NegativeCacheRequestState
    ): OnDemandCanvasResolution =
        when (payload.optString("status").trim().lowercase(Locale.ROOT)) {
            "miss" -> {
                recordNegative(requestState)
                OnDemandCanvasResolution.NoMatch
            }
            "resolved" -> parseResolvedPayload(payload, identity, requestState)
            else -> OnDemandCanvasResolution.Failed(
                IOException("On-demand canvas resolver returned an unexpected status")
            )
        }

    private fun parseResolvedPayload(
        payload: JSONObject,
        identity: MotionTrackIdentity,
        requestState: NegativeCacheRequestState
    ): OnDemandCanvasResolution {
        val rawUrl = payload.optString("url").trim()
        return if (communityCanvasMediaUrl(rawUrl) == null) {
            recordNegative(requestState)
            Timber.d("On-demand canvas returned non-allowlisted media URL for %s", identity.title)
            OnDemandCanvasResolution.NoMatch
        } else {
            Timber.d("On-demand canvas resolved successfully: %s", rawUrl)
            OnDemandCanvasResolution.Found(
                MotionArtworkCandidate(
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
            )
        }
    }

    private fun recordNegative(requestState: NegativeCacheRequestState) {
        synchronized(negativeCacheLock) {
            if (negativeCacheGeneration == requestState.generation) {
                negativeCache[requestState.key] = requestState.nowMs + NEGATIVE_CACHE_TTL_MS
            }
        }
    }

    companion object {
        const val USER_AGENT = "Levyra/1.0 (Android; MotionArtwork)"
        private const val MAX_RESPONSE_BYTES = 64 * 1024
        private const val MAX_NEGATIVE_CACHE_ENTRIES = 128
        private const val NEGATIVE_CACHE_TTL_MS = 10L * 60L * 1000L
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
            normalizeMotionText(identity.album),
            (identity.durationMs / 1000L).toString()
        ).joinToString("|")
    }

private fun ResponseBody.readBoundedUtf8(maxBytes: Int): String? {
    if (contentLength() > maxBytes) return null
    return byteStream().use { input ->
        val output = ByteArrayOutputStream(minOf(maxBytes, 8 * 1024))
        val buffer = ByteArray(4 * 1024)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            if (total > maxBytes) return null
            output.write(buffer, 0, count)
        }
        output.toString(Charsets.UTF_8.name())
    }
}

private suspend fun Call.awaitCancellable(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation {
        cancel()
    }
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            if (continuation.isCancelled) {
                response.close()
                return
            }
            continuation.resume(response) { _, _, _ ->
                response.close()
            }
        }

        override fun onFailure(call: Call, e: IOException) {
            if (!continuation.isCancelled) {
                continuation.resumeWithException(e)
            }
        }
    })
}
