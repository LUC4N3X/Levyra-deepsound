package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.data.network.PipedStreamUrlPolicy
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

internal object PipedRescuePolicy {
    const val RESCUE_BUDGET_MS = 6_500L
    const val STREAM_PROBE_TIMEOUT_MS = 2_500L
    const val MAX_RESPONSE_BYTES = 1_048_576L
    const val MAX_NEGATIVE_CACHE_ENTRIES = 256
    const val INSTANCE_FAILURE_THRESHOLD = 2
    const val INSTANCE_COOLDOWN_MS = 60_000L
    const val MAX_INSTANCE_COOLDOWN_MS = 15L * 60L * 1_000L
    const val TIMEOUT_NEGATIVE_TTL_MS = 20_000L
    const val SERVER_NEGATIVE_TTL_MS = 30_000L
    const val FORBIDDEN_NEGATIVE_TTL_MS = 60_000L
    const val RATE_LIMIT_NEGATIVE_TTL_MS = 5L * 60L * 1_000L
    const val VIDEO_NOT_FOUND_TTL_MS = 10L * 60L * 1_000L
    const val INVALID_RESPONSE_TTL_MS = 45_000L
}

internal data class PipedInstance(
    val id: String,
    val apiBaseUrl: String
)

internal data class PipedResolvedStream(
    val url: String,
    val mimeType: String,
    val codec: String,
    val bitrate: Int,
    val instanceId: String,
    val proxied: Boolean,
    val expiresAtMs: Long
)

internal enum class PipedRequestKind {
    API,
    STREAM_PROBE
}

internal data class PipedHttpRequest(
    val url: String,
    val headers: Map<String, String>,
    val kind: PipedRequestKind,
    val maxBodyBytes: Long
)

internal data class PipedHttpResponse(
    val statusCode: Int,
    val body: String,
    val contentType: String,
    val latencyMs: Long
)

internal fun interface PipedHttpExchange {
    suspend fun execute(request: PipedHttpRequest): PipedHttpResponse
}

internal enum class PipedFailureKind {
    TIMEOUT,
    HTTP_403,
    VIDEO_NOT_FOUND,
    RATE_LIMITED,
    SERVER,
    INVALID_JSON,
    EMPTY_AUDIO,
    STREAM_FORBIDDEN,
    STREAM_UNREACHABLE,
    TRANSPORT
}

internal data class PipedInstanceSnapshot(
    val successes: Int,
    val failures: Int,
    val consecutiveFailures: Int,
    val averageLatencyMs: Long?,
    val lastSuccessAtMs: Long?,
    val lastFailureAtMs: Long?,
    val blockedUntilMs: Long
)

internal class PipedRescueClient(
    private val exchange: PipedHttpExchange = OkHttpPipedExchange(),
    private val instances: List<PipedInstance> = BOOTSTRAP_INSTANCES,
    private val clockMs: () -> Long = System::currentTimeMillis
) {
    private data class MutableHealth(
        var successes: Int = 0,
        var failures: Int = 0,
        var consecutiveFailures: Int = 0,
        var averageLatencyMs: Long? = null,
        var lastSuccessAtMs: Long? = null,
        var lastFailureAtMs: Long? = null,
        var blockedUntilMs: Long = 0L,
        var cooldownEscalations: Int = 0
    )

    private data class NegativeKey(
        val videoId: String,
        val instanceId: String?,
        val kind: PipedFailureKind,
        val resource: String = ""
    )

    private data class Candidate(
        val url: String,
        val mimeType: String,
        val codec: String,
        val bitrate: Int,
        val proxied: Boolean
    )

    private data class ResolutionAttempt(val value: PipedResolvedStream?)

    private sealed interface ApiLookup {
        data class Success(val response: PipedHttpResponse) : ApiLookup
        data object TryNext : ApiLookup
        data object VideoMissing : ApiLookup
    }

    private val lock = Any()
    private val instanceHealth = mutableMapOf<String, MutableHealth>()
    private val negativeCache = object : LinkedHashMap<NegativeKey, Long>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<NegativeKey, Long>?): Boolean =
            size > PipedRescuePolicy.MAX_NEGATIVE_CACHE_ENTRIES
    }

    suspend fun resolve(videoId: String, audioQuality: String): PipedResolvedStream? {
        if (!VIDEO_ID_REGEX.matches(videoId)) return null
        val attempt = withTimeoutOrNull(PipedRescuePolicy.RESCUE_BUDGET_MS) {
            ResolutionAttempt(resolveWithinBudget(videoId, audioQuality))
        }
        if (attempt == null) {
            Timber.w("Piped rescue timed out")
            return null
        }
        return attempt.value
    }

    internal fun instanceSnapshot(id: String): PipedInstanceSnapshot = synchronized(lock) {
        val health = instanceHealth[id] ?: MutableHealth()
        PipedInstanceSnapshot(
            successes = health.successes,
            failures = health.failures,
            consecutiveFailures = health.consecutiveFailures,
            averageLatencyMs = health.averageLatencyMs,
            lastSuccessAtMs = health.lastSuccessAtMs,
            lastFailureAtMs = health.lastFailureAtMs,
            blockedUntilMs = health.blockedUntilMs
        )
    }

    internal fun negativeCacheSize(): Int = synchronized(lock) {
        pruneNegativeCacheLocked(clockMs())
        negativeCache.size
    }

    private suspend fun resolveWithinBudget(videoId: String, audioQuality: String): PipedResolvedStream? {
        if (hasNegative(videoId, null, PipedFailureKind.VIDEO_NOT_FOUND)) return null
        Timber.i("Piped rescue activated")
        val candidates = orderedInstances(videoId)
        if (candidates.isEmpty()) return null
        var missingConfirmations = 0
        for (instance in candidates) {
            Timber.i("Piped instance %s selected", instance.id)
            val response = when (val lookup = fetchStreams(videoId, instance)) {
                is ApiLookup.Success -> lookup.response
                ApiLookup.TryNext -> continue
                ApiLookup.VideoMissing -> {
                    missingConfirmations++
                    continue
                }
            }
            val streams = parseInstanceCandidates(videoId, audioQuality, instance, response) ?: continue
            probeCandidates(videoId, instance, response, streams)?.let { return it }
        }
        if (candidates.size >= MIN_GLOBAL_MISSING_CONFIRMATIONS && missingConfirmations == candidates.size) {
            putNegative(videoId, null, PipedFailureKind.VIDEO_NOT_FOUND)
        }
        return null
    }

    private suspend fun fetchStreams(videoId: String, instance: PipedInstance): ApiLookup {
        val response = try {
            exchange.execute(
                PipedHttpRequest(
                    url = "${instance.apiBaseUrl}/streams/$videoId",
                    headers = API_HEADERS,
                    kind = PipedRequestKind.API,
                    maxBodyBytes = PipedRescuePolicy.MAX_RESPONSE_BYTES
                )
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            val kind = if (error.isTimeout()) PipedFailureKind.TIMEOUT else PipedFailureKind.TRANSPORT
            recordFailure(videoId, instance, kind, null)
            Timber.w("Piped instance %s %s", instance.id, kind.name.lowercase())
            return ApiLookup.TryNext
        }
        val failure = response.failureKind() ?: return ApiLookup.Success(response)
        if (failure == PipedFailureKind.VIDEO_NOT_FOUND) {
            putNegative(videoId, instance.id, failure)
            return ApiLookup.VideoMissing
        }
        recordFailure(videoId, instance, failure, response.latencyMs)
        return ApiLookup.TryNext
    }

    private fun parseInstanceCandidates(
        videoId: String,
        audioQuality: String,
        instance: PipedInstance,
        response: PipedHttpResponse
    ): List<Candidate>? {
        val candidates = try {
            parseCandidates(response.body, audioQuality)
        } catch (_: JSONException) {
            recordFailure(videoId, instance, PipedFailureKind.INVALID_JSON, response.latencyMs)
            return null
        }
        if (candidates.isEmpty()) {
            recordFailure(videoId, instance, PipedFailureKind.EMPTY_AUDIO, response.latencyMs)
            return null
        }
        return candidates
    }

    private suspend fun probeCandidates(
        videoId: String,
        instance: PipedInstance,
        apiResponse: PipedHttpResponse,
        candidates: List<Candidate>
    ): PipedResolvedStream? {
        for (candidate in candidates.take(MAX_STREAM_PROBES_PER_INSTANCE)) {
            val probe = probeCandidate(videoId, instance, candidate) ?: continue
            recordSuccess(instance, apiResponse.latencyMs + probe.latencyMs)
            Timber.i(
                "Piped %s audio selected: %s %dk via=%s",
                if (candidate.proxied) "proxy" else "direct",
                candidate.codec.ifBlank { candidate.mimeType },
                candidate.bitrate / 1_000,
                instance.id
            )
            return candidate.toResolvedStream(instance)
        }
        return null
    }

    private suspend fun probeCandidate(
        videoId: String,
        instance: PipedInstance,
        candidate: Candidate
    ): PipedHttpResponse? {
        val resource = candidate.resourceKey()
        if (hasNegative(videoId, instance.id, PipedFailureKind.STREAM_FORBIDDEN, resource) ||
            hasNegative(videoId, instance.id, PipedFailureKind.STREAM_UNREACHABLE, resource)
        ) return null
        val response = try {
            exchange.execute(PipedHttpRequest(candidate.url, STREAM_PROBE_HEADERS, PipedRequestKind.STREAM_PROBE, 0L))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            val kind = if (error.isTimeout()) PipedFailureKind.TIMEOUT else PipedFailureKind.STREAM_UNREACHABLE
            recordFailure(videoId, instance, kind, null, resource)
            return null
        }
        val failure = response.streamFailureKind() ?: return response
        recordFailure(videoId, instance, failure, response.latencyMs, resource)
        return null
    }

    private fun PipedHttpResponse.failureKind(): PipedFailureKind? = when (statusCode) {
        200 -> null
        403 -> PipedFailureKind.HTTP_403
        404 -> PipedFailureKind.VIDEO_NOT_FOUND
        429 -> PipedFailureKind.RATE_LIMITED
        in 500..599 -> PipedFailureKind.SERVER
        else -> PipedFailureKind.TRANSPORT
    }

    private fun PipedHttpResponse.streamFailureKind(): PipedFailureKind? = when {
        statusCode == 403 -> PipedFailureKind.STREAM_FORBIDDEN
        statusCode == 429 -> PipedFailureKind.RATE_LIMITED
        statusCode !in 200..299 -> PipedFailureKind.STREAM_UNREACHABLE
        contentType.isExplicitlyNonAudio() -> PipedFailureKind.STREAM_UNREACHABLE
        else -> null
    }

    private fun Candidate.toResolvedStream(instance: PipedInstance) = PipedResolvedStream(
        url = url,
        mimeType = mimeType,
        codec = codec,
        bitrate = bitrate,
        instanceId = instance.id,
        proxied = proxied,
        expiresAtMs = url.expirationMs()
    )

    private fun parseCandidates(body: String, audioQuality: String): List<Candidate> {
        val root = JSONObject(body)
        val proxyUrl = root.optString("proxyUrl")
        val streams = root.optJSONArray("audioStreams") ?: JSONArray()
        return buildList {
            for (index in 0 until streams.length()) {
                val stream = streams.optJSONObject(index) ?: continue
                val url = stream.optString("url")
                val mimeType = stream.optString("mimeType").substringBefore(';').lowercase()
                val codec = stream.optString("codec").lowercase()
                val bitrate = stream.optInt("bitrate").coerceAtLeast(0)
                if (stream.optBoolean("videoOnly", false) || !mimeType.startsWith("audio/")) continue
                if (!isCompatibleCodec(codec, mimeType)) continue
                val proxied = proxyUrl.isNotBlank() && PipedStreamUrlPolicy.isValidProxyStreamUrl(url, proxyUrl)
                if (!proxied && !isAllowedDirectStream(url)) continue
                add(Candidate(url, mimeType, codec, bitrate, proxied))
            }
        }.sortedWith(
            compareByDescending<Candidate> { it.proxied }
                .thenByDescending { codecRank(it.codec, it.mimeType) }
                .thenByDescending { qualityScore(it.bitrate, audioQuality) }
        )
    }

    private fun orderedInstances(videoId: String): List<PipedInstance> = synchronized(lock) {
        val now = clockMs()
        pruneNegativeCacheLocked(now)
        instances.filter { instance ->
            (instanceHealth[instance.id]?.blockedUntilMs ?: 0L) <= now &&
                negativeCache.none { (key, expiresAt) ->
                    expiresAt > now && key.videoId == videoId && key.instanceId == instance.id
                }
        }.sortedWith(
            compareByDescending<PipedInstance> { healthScore(instanceHealth[it.id]) }
                .thenBy { instanceHealth[it.id]?.averageLatencyMs ?: Long.MAX_VALUE }
                .thenBy { instances.indexOf(it) }
        )
    }

    private fun recordSuccess(instance: PipedInstance, latencyMs: Long) = synchronized(lock) {
        val now = clockMs()
        val health = instanceHealth.getOrPut(instance.id, ::MutableHealth)
        health.successes = (health.successes + 1).coerceAtMost(10_000)
        health.consecutiveFailures = 0
        health.averageLatencyMs = health.averageLatencyMs?.let { (it * 3L + latencyMs) / 4L } ?: latencyMs
        health.lastSuccessAtMs = now
        health.blockedUntilMs = 0L
        health.cooldownEscalations = 0
    }

    private fun recordFailure(
        videoId: String,
        instance: PipedInstance,
        kind: PipedFailureKind,
        latencyMs: Long?,
        resource: String = ""
    ) = synchronized(lock) {
        val now = clockMs()
        val health = instanceHealth.getOrPut(instance.id, ::MutableHealth)
        health.failures = (health.failures + 1).coerceAtMost(10_000)
        health.consecutiveFailures = (health.consecutiveFailures + 1).coerceAtMost(100)
        health.lastFailureAtMs = now
        if (latencyMs != null) {
            health.averageLatencyMs = health.averageLatencyMs?.let { (it * 3L + latencyMs) / 4L } ?: latencyMs
        }
        val cooldown = when (kind) {
            PipedFailureKind.RATE_LIMITED -> PipedRescuePolicy.RATE_LIMIT_NEGATIVE_TTL_MS
            PipedFailureKind.HTTP_403,
            PipedFailureKind.STREAM_FORBIDDEN -> PipedRescuePolicy.FORBIDDEN_NEGATIVE_TTL_MS
            else -> if (
                health.consecutiveFailures >= PipedRescuePolicy.INSTANCE_FAILURE_THRESHOLD &&
                health.blockedUntilMs <= now
            ) {
                escalatingCooldownMs(health.cooldownEscalations++)
            } else {
                0L
            }
        }
        if (cooldown > 0L) health.blockedUntilMs = now + cooldown
        putNegativeLocked(NegativeKey(videoId, instance.id, kind, resource), now + negativeTtl(kind))
    }

    private fun putNegative(videoId: String, instanceId: String?, kind: PipedFailureKind, resource: String = "") =
        synchronized(lock) {
            val now = clockMs()
            putNegativeLocked(NegativeKey(videoId, instanceId, kind, resource), now + negativeTtl(kind))
        }

    private fun putNegativeLocked(key: NegativeKey, expiresAt: Long) {
        pruneNegativeCacheLocked(clockMs())
        negativeCache[key] = expiresAt
    }

    private fun hasNegative(
        videoId: String,
        instanceId: String?,
        kind: PipedFailureKind,
        resource: String = ""
    ): Boolean = synchronized(lock) {
        val now = clockMs()
        pruneNegativeCacheLocked(now)
        (negativeCache[NegativeKey(videoId, instanceId, kind, resource)] ?: 0L) > now
    }

    private fun pruneNegativeCacheLocked(now: Long) {
        negativeCache.entries.removeAll { it.value <= now }
    }

    private fun negativeTtl(kind: PipedFailureKind): Long = when (kind) {
        PipedFailureKind.TIMEOUT -> PipedRescuePolicy.TIMEOUT_NEGATIVE_TTL_MS
        PipedFailureKind.RATE_LIMITED -> PipedRescuePolicy.RATE_LIMIT_NEGATIVE_TTL_MS
        PipedFailureKind.VIDEO_NOT_FOUND -> PipedRescuePolicy.VIDEO_NOT_FOUND_TTL_MS
        PipedFailureKind.HTTP_403,
        PipedFailureKind.STREAM_FORBIDDEN -> PipedRescuePolicy.FORBIDDEN_NEGATIVE_TTL_MS
        PipedFailureKind.SERVER -> PipedRescuePolicy.SERVER_NEGATIVE_TTL_MS
        PipedFailureKind.INVALID_JSON,
        PipedFailureKind.EMPTY_AUDIO -> PipedRescuePolicy.INVALID_RESPONSE_TTL_MS
        PipedFailureKind.STREAM_UNREACHABLE,
        PipedFailureKind.TRANSPORT -> PipedRescuePolicy.SERVER_NEGATIVE_TTL_MS
    }

    private fun escalatingCooldownMs(previousEscalations: Int): Long =
        (PipedRescuePolicy.INSTANCE_COOLDOWN_MS shl previousEscalations.coerceAtMost(MAX_COOLDOWN_DOUBLINGS))
            .coerceAtMost(PipedRescuePolicy.MAX_INSTANCE_COOLDOWN_MS)

    private fun healthScore(health: MutableHealth?): Int {
        if (health == null) return 50
        return (50 + health.successes.coerceAtMost(10) * 4 - health.consecutiveFailures * 18).coerceIn(0, 100)
    }

    private fun isCompatibleCodec(codec: String, mimeType: String): Boolean =
        codec.startsWith("mp4a") || codec.startsWith("opus") || codec.startsWith("vorbis") ||
            codec.startsWith("aac") || mimeType == "audio/mp4" || mimeType == "audio/webm"

    private fun isAllowedDirectStream(url: String): Boolean {
        val parsed = url.toHttpUrlOrNull() ?: return false
        if (!PipedStreamUrlPolicy.isSafeHttps(parsed)) return false
        return parsed.host == "googlevideo.com" || parsed.host.endsWith(".googlevideo.com")
    }

    private fun codecRank(codec: String, mimeType: String): Int = when {
        codec.startsWith("mp4a") || codec.startsWith("aac") || mimeType == "audio/mp4" -> 3
        codec.startsWith("opus") -> 2
        codec.startsWith("vorbis") -> 1
        else -> 0
    }

    private fun qualityScore(bitrate: Int, audioQuality: String): Int {
        val target = when (audioQuality.trim().lowercase()) {
            "low" -> 96_000
            "high" -> 256_000
            else -> 160_000
        }
        return if (bitrate <= target) bitrate else target - (bitrate - target)
    }

    private fun Candidate.resourceKey(): String {
        val parsed = url.toHttpUrlOrNull() ?: return ""
        return "${parsed.host}${parsed.encodedPath}"
    }

    private fun String.expirationMs(): Long {
        val seconds = toHttpUrlOrNull()?.queryParameter("expire")?.toLongOrNull() ?: return 0L
        return seconds * 1_000L
    }

    private fun String.isExplicitlyNonAudio(): Boolean {
        val normalized = substringBefore(';').trim().lowercase()
        if (normalized.isBlank() || normalized == "application/octet-stream") return false
        return !normalized.startsWith("audio/") && normalized != "video/mp4" && normalized != "video/webm"
    }

    private fun Throwable.isTimeout(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current is SocketTimeoutException) return true
            if (current is InterruptedIOException && current.message.orEmpty().contains("timeout", true)) return true
            current = current.cause
        }
        return false
    }

    companion object {
        internal val BOOTSTRAP_INSTANCES = listOf(
            PipedInstance("ducks.party", "https://pipedapi.ducks.party"),
            PipedInstance("private.coffee", "https://api.piped.private.coffee"),
            PipedInstance("minionflo.net", "https://api.piped.minionflo.net"),
            PipedInstance("wireway.ch", "https://pipedapi.wireway.ch")
        )
        private val VIDEO_ID_REGEX = Regex("[A-Za-z0-9_-]{11}")
        private val API_HEADERS = mapOf(
            "Accept" to "application/json",
            "User-Agent" to "Levyra/Android Piped Rescue"
        )
        private val STREAM_PROBE_HEADERS = mapOf(
            "Accept" to "*/*",
            "Accept-Encoding" to "identity",
            "Range" to "bytes=0-1",
            "User-Agent" to "Levyra/Android Piped Rescue"
        )
        private const val MAX_STREAM_PROBES_PER_INSTANCE = 2
        private const val MAX_COOLDOWN_DOUBLINGS = 4
        private const val MIN_GLOBAL_MISSING_CONFIRMATIONS = 2
    }
}

internal class OkHttpPipedExchange(
    private val apiClient: () -> OkHttpClient = LevyraHttpClientFactory::pipedApi,
    private val streamClient: () -> OkHttpClient = { LevyraHttpClientFactory.pipedStreaming() }
) : PipedHttpExchange {
    override suspend fun execute(request: PipedHttpRequest): PipedHttpResponse {
        val okhttpRequest = Request.Builder()
            .url(request.url)
            .get()
            .apply {
                request.headers.forEach { (name, value) -> header(name, value) }
            }
            .build()
        val client = if (request.kind == PipedRequestKind.API) {
            apiClient()
        } else {
            streamClient().newBuilder()
                .callTimeout(PipedRescuePolicy.STREAM_PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .build()
        }
        val startedAt = System.nanoTime()
        return client.newCall(okhttpRequest).await().use { response ->
            PipedHttpResponse(
                statusCode = response.code,
                body = if (request.maxBodyBytes > 0L) {
                    response.body?.readBounded(request.maxBodyBytes).orEmpty()
                } else {
                    ""
                },
                contentType = response.header("Content-Type").orEmpty(),
                latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt).coerceAtLeast(1L)
            )
        }
    }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response) { _, value, _ -> value.close() }
            }
        })
    }

    private fun okhttp3.ResponseBody.readBounded(maxBytes: Long): String {
        val declaredLength = contentLength()
        if (declaredLength > maxBytes) throw IOException("Piped response exceeded byte limit")
        val output = ByteArrayOutputStream(
            declaredLength.takeIf { it in 1..maxBytes }?.toInt() ?: 8192
        )
        byteStream().use { input ->
            val buffer = ByteArray(8192)
            var total = 0L
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                if (total > maxBytes) throw IOException("Piped response exceeded byte limit")
                output.write(buffer, 0, read)
            }
        }
        return output.toByteArray().toString(StandardCharsets.UTF_8)
    }
}
