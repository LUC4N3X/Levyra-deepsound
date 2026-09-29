package com.luc4n3x.levyra.data.hqaudio.addon

import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackCandidate
import com.luc4n3x.levyra.data.hqaudio.ConfigurableProviderDestinationPolicy
import com.luc4n3x.levyra.data.hqaudio.ConfigurableProviderPlaybackPolicy
import com.luc4n3x.levyra.data.hqaudio.LosslessStreamValidation
import com.luc4n3x.levyra.data.hqaudio.LosslessStreamValidator
import com.luc4n3x.levyra.data.hqaudio.ProviderBackendHealth
import com.luc4n3x.levyra.data.hqaudio.ProviderCircuitBreaker
import com.luc4n3x.levyra.data.hqaudio.ProviderFailure
import com.luc4n3x.levyra.data.hqaudio.ProviderHttpExchange
import com.luc4n3x.levyra.data.hqaudio.ProviderHttpRequest
import com.luc4n3x.levyra.data.hqaudio.ProviderLookupOutcome
import com.luc4n3x.levyra.data.hqaudio.ProviderSearchOutcome
import com.luc4n3x.levyra.data.hqaudio.ProviderStreamOutcome
import com.luc4n3x.levyra.data.hqaudio.ResolvedHighQualityStream
import com.luc4n3x.levyra.data.hqaudio.StreamRejection
import com.luc4n3x.levyra.data.hqaudio.VerifiedAudioFormat
import com.luc4n3x.levyra.domain.AudioQualityPreference
import com.luc4n3x.levyra.domain.AudioQualityRequest
import com.luc4n3x.levyra.domain.AudioStreamPurpose
import java.io.IOException
import java.io.InterruptedIOException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

internal class GenericLosslessAddonProvider(
    private val exchange: ProviderHttpExchange,
    private val enabled: () -> Boolean,
    private val baseUrl: () -> String,
    private val clock: () -> Long = System::currentTimeMillis,
    private val catalogCircuitBreaker: ProviderCircuitBreaker = ProviderCircuitBreaker(CATALOG_CIRCUIT, clock),
    private val streamCircuitBreaker: ProviderCircuitBreaker = ProviderCircuitBreaker(STREAM_CIRCUIT, clock)
) : com.luc4n3x.levyra.data.hqaudio.HighQualityAudioProvider {
    override val id: String = PROVIDER_ID
    override val displayName: String = "Lossless Addon"
    override val requiresHighQualityMode: Boolean = false
    override val quarantineTrackOnStreamFailure: Boolean = false
    override val resolutionTimeoutMs: Long = RESOLUTION_TIMEOUT_MS

    private val candidateLock = Any()
    private val candidates = LinkedHashMap<String, CachedCandidate>(MAX_CANDIDATE_CACHE, 0.75f, true)

    @Volatile
    private var manifestCache: ManifestCache? = null

    private val failedStreams = ConcurrentHashMap<String, Long>()

    override fun isEnabled(request: AudioQualityRequest): Boolean =
        enabled() && request.losslessEnabled && request.preference.requestsLossless && configuredBase() != null

    override suspend fun search(query: String): ProviderSearchOutcome = search(
        query,
        AudioQualityRequest(losslessEnabled = true)
    )

    override suspend fun search(query: String, request: AudioQualityRequest): ProviderSearchOutcome {
        if (!isEnabled(request)) return ProviderSearchOutcome.Found(emptyList())
        val manifest = loadManifest() ?: return ProviderSearchOutcome.Failed(ProviderFailure.NETWORK)
        val preferred = request.preference.losslessAttemptOrder(request.allowUpgrade).firstOrNull()
            ?: return ProviderSearchOutcome.Found(emptyList())
        val url = endpoint(manifest.base, "search")
            ?.newBuilder()
            ?.addQueryParameter("q", query.trim())
            ?.addQueryParameter("quality", preferred.protocolValue)
            ?.addQueryParameter("atmos", if (preferred == AudioQualityPreference.DOLBY_ATMOS) "auto" else "none")
            ?.build()
            ?: return ProviderSearchOutcome.Failed(ProviderFailure.MALFORMED_RESPONSE)
        return when (val result = executeJson(url, manifest.allowedHosts, MAX_SEARCH_BODY_BYTES, catalogCircuitBreaker)) {
            is JsonResult.Success -> {
                val parsed = parseCandidates(result.body)
                    ?: return ProviderSearchOutcome.Failed(ProviderFailure.MALFORMED_RESPONSE)
                rememberCandidates(parsed)
                ProviderSearchOutcome.Found(parsed)
            }
            is JsonResult.Failure -> ProviderSearchOutcome.Failed(result.failure)
        }
    }

    override suspend fun lookup(providerTrackId: String): ProviderLookupOutcome {
        val cached = synchronized(candidateLock) { candidates[providerTrackId] } ?: return ProviderLookupOutcome.Missing
        if (cached.expiresAtMs <= clock()) {
            synchronized(candidateLock) { candidates.remove(providerTrackId) }
            return ProviderLookupOutcome.Missing
        }
        return ProviderLookupOutcome.Found(cached.candidate)
    }

    override suspend fun resolveStream(candidate: AlternativeTrackCandidate): ProviderStreamOutcome = resolveStream(
        candidate,
        AudioQualityRequest(losslessEnabled = true)
    )

    override suspend fun resolveStream(
        candidate: AlternativeTrackCandidate,
        request: AudioQualityRequest
    ): ProviderStreamOutcome {
        if (!isEnabled(request)) return ProviderStreamOutcome.Unavailable(listOf(StreamRejection.NO_MEDIA))
        val manifest = loadManifest() ?: return ProviderStreamOutcome.Failed(ProviderFailure.NETWORK)
        val rejections = mutableListOf<StreamRejection>()
        var bestVerified: ResolvedHighQualityStream? = null
        for (attempt in request.preference.losslessAttemptOrder(request.allowUpgrade)) {
            currentCoroutineContext().ensureActive()
            when (val outcome = evaluateStreamAttempt(manifest, candidate, request, attempt, rejections)) {
                is AttemptResolution.Resolved -> return ProviderStreamOutcome.Resolved(outcome.stream)
                is AttemptResolution.CandidateVerified -> {
                    if (bestVerified == null || qualityRank(outcome.stream) > qualityRank(bestVerified)) {
                        bestVerified = outcome.stream
                    }
                }
                is AttemptResolution.Failed -> return ProviderStreamOutcome.Failed(outcome.failure)
                is AttemptResolution.Continue -> Unit
            }
        }
        return bestVerified?.let(ProviderStreamOutcome::Resolved)
            ?: ProviderStreamOutcome.Unavailable(rejections.ifEmpty { listOf(StreamRejection.NO_MEDIA) })
    }

    private suspend fun evaluateStreamAttempt(
        manifest: ManifestCache,
        candidate: AlternativeTrackCandidate,
        request: AudioQualityRequest,
        attempt: AudioQualityPreference,
        rejections: MutableList<StreamRejection>
    ): AttemptResolution {
        val streamUrl = endpoint(manifest.base, "stream", candidate.providerTrackId)
            ?.newBuilder()
            ?.addQueryParameter("quality", attempt.protocolValue)
            ?.addQueryParameter("atmos", if (attempt == AudioQualityPreference.DOLBY_ATMOS) "auto" else "none")
            ?.addQueryParameter("intent", request.purpose.name.lowercase(Locale.ROOT))
            ?.build()
            ?: return AttemptResolution.Failed(ProviderFailure.MALFORMED_RESPONSE)
        when (val result = executeJson(streamUrl, manifest.allowedHosts, MAX_STREAM_BODY_BYTES, streamCircuitBreaker)) {
            is JsonResult.Failure -> {
                if (result.failure == ProviderFailure.NOT_FOUND) {
                    rejections += StreamRejection.NO_MEDIA
                    return AttemptResolution.Continue
                }
                return AttemptResolution.Failed(result.failure)
            }
            is JsonResult.Success -> {
                val payload = parseStream(result.body) ?: run {
                    rejections += StreamRejection.NO_MEDIA
                    return AttemptResolution.Continue
                }
                val mediaUrl = payload.url.toHttpUrlOrNull()
                if (mediaUrl == null || !ConfigurableProviderDestinationPolicy.allows(mediaUrl, manifest.allowedHosts) || isFailedStream(mediaUrl.toString())) {
                    rejections += StreamRejection.TRANSPORT
                    return AttemptResolution.Continue
                }
                val expiresAtMs = streamExpiry(payload.expiresAtMs, mediaUrl)
                if (expiresAtMs <= clock() + MINIMUM_URL_LIFETIME_MS) {
                    rejections += StreamRejection.URL_EXPIRED
                    return AttemptResolution.Continue
                }
                val probe = try {
                    probe(mediaUrl, manifest.allowedHosts)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: IOException) {
                    rejections += StreamRejection.TRANSPORT
                    return AttemptResolution.Continue
                }
                return when (val validation = LosslessStreamValidator.validate(probe, attempt, candidate.durationSeconds)) {
                    is LosslessStreamValidation.Invalid -> {
                        rejections += validation.rejection
                        AttemptResolution.Continue
                    }
                    is LosslessStreamValidation.Valid -> {
                        if (request.purpose == AudioStreamPurpose.DOWNLOAD &&
                            validation.format.deliveryMethod != com.luc4n3x.levyra.domain.PlaybackDeliveryMethod.PROGRESSIVE
                        ) {
                            rejections += StreamRejection.UNSUPPORTED_CONTAINER
                            AttemptResolution.Continue
                        } else {
                            if (request.purpose == AudioStreamPurpose.PLAYBACK) {
                                ConfigurableProviderPlaybackPolicy.register(mediaUrl, manifest.allowedHosts, expiresAtMs)
                            }
                            val resolved = validation.format.toResolved(candidate, request.preference, mediaUrl.toString(), expiresAtMs)
                            if (satisfiesAttempt(validation.format, attempt)) {
                                AttemptResolution.Resolved(resolved)
                            } else {
                                AttemptResolution.CandidateVerified(resolved)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun health(): List<ProviderBackendHealth> = listOf(
        catalogCircuitBreaker.snapshot(id),
        streamCircuitBreaker.snapshot(id)
    )

    override fun reportStreamFailure(providerTrackId: String, url: String, reason: String) {
        if (url.isBlank()) return
        if (failedStreams.size >= MAX_FAILED_STREAMS) {
            val now = clock()
            failedStreams.entries.removeIf { it.value <= now }
            if (failedStreams.size >= MAX_FAILED_STREAMS) {
                failedStreams.entries.minByOrNull { it.value }?.let { failedStreams.remove(it.key, it.value) }
            }
        }
        failedStreams[url] = clock() + FAILED_STREAM_COOLDOWN_MS
    }

    private fun isFailedStream(url: String): Boolean {
        val until = failedStreams[url] ?: return false
        if (until > clock()) return true
        failedStreams.remove(url, until)
        return false
    }

    private suspend fun loadManifest(): ManifestCache? {
        val base = configuredBase() ?: return null
        manifestCache?.takeIf { it.base == base && it.expiresAtMs > clock() }?.let { return it }
        val baseHost = ConfigurableProviderDestinationPolicy.normalizeHost(base.host)
        val url = endpoint(base, "manifest.json") ?: return null
        val result = executeJson(url, setOf(baseHost), MAX_MANIFEST_BODY_BYTES, catalogCircuitBreaker)
        if (result !is JsonResult.Success) return null
        val root = runCatching { JSONObject(result.body) }.getOrNull() ?: return null
        val resources = root.optJSONArray("resources").stringValues()
        if (resources.isNotEmpty() && ("search" !in resources || "stream" !in resources)) return null
        val allowedHosts = buildSet {
            add(baseHost)
            root.optJSONArray("allowedHosts").stringValues().forEach { raw ->
                val host = ConfigurableProviderDestinationPolicy.normalizeHost(raw)
                if (host.isNotBlank() && host.none { it == '/' || it == ':' || it == '@' }) add(host)
            }
        }
        return ManifestCache(base, allowedHosts, clock() + MANIFEST_TTL_MS).also { manifestCache = it }
    }

    private suspend fun probe(url: HttpUrl, allowedHosts: Set<String>) = exchange.execute(
        ProviderHttpRequest(
            url = url.toString(),
            headers = mapOf("Range" to "bytes=0-${LosslessStreamValidator.MANIFEST_PROBE_BYTES - 1}"),
            maxBodyBytes = LosslessStreamValidator.MANIFEST_PROBE_BYTES,
            allowedHosts = allowedHosts
        )
    )

    private suspend fun executeJson(
        url: HttpUrl,
        allowedHosts: Set<String>,
        maxBytes: Int,
        circuitBreaker: ProviderCircuitBreaker
    ): JsonResult {
        val permit = circuitBreaker.acquire()
        if (permit == ProviderCircuitBreaker.Permit.REJECTED) return JsonResult.Failure(ProviderFailure.CIRCUIT_OPEN)
        var settled = false
        try {
            val startedAt = clock()
            val response = try {
                exchange.execute(
                    ProviderHttpRequest(
                        url = url.toString(),
                        headers = mapOf("Accept" to "application/json"),
                        maxBodyBytes = maxBytes,
                        allowedHosts = allowedHosts
                    )
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: InterruptedIOException) {
                null
            } catch (error: IOException) {
                null
            }
            val result = when {
                response == null -> JsonResult.Failure(ProviderFailure.NETWORK)
                response.code == 404 -> JsonResult.Failure(ProviderFailure.NOT_FOUND)
                response.code == 403 -> JsonResult.Failure(ProviderFailure.FORBIDDEN)
                response.code == 429 -> JsonResult.Failure(ProviderFailure.RATE_LIMITED)
                !response.isSuccessful -> JsonResult.Failure(ProviderFailure.HTTP_ERROR)
                response.body.isEmpty() -> JsonResult.Failure(ProviderFailure.MALFORMED_RESPONSE)
                else -> JsonResult.Success(response.bodyText)
            }
            settled = true
            if (result is JsonResult.Success || (result is JsonResult.Failure && result.failure == ProviderFailure.NOT_FOUND)) {
                circuitBreaker.onSuccess(permit, clock() - startedAt)
            } else {
                circuitBreaker.onFailure(permit, (result as JsonResult.Failure).failure.name)
            }
            return result
        } finally {
            if (!settled) circuitBreaker.release(permit)
        }
    }

    private fun parseCandidates(body: String): List<AlternativeTrackCandidate>? {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val tracks = root.optJSONArray("tracks") ?: return null
        return buildList {
            for (index in 0 until minOf(tracks.length(), MAX_SEARCH_RESULTS)) {
                val item = tracks.optJSONObject(index) ?: continue
                val trackId = item.optString("id").trim()
                val title = item.optString("title").trim()
                val artist = item.optString("artist").trim()
                val duration = item.optDouble("duration", 0.0).roundToInt()
                if (trackId.isBlank() || title.isBlank() || artist.isBlank() || duration <= 0) continue
                add(
                    AlternativeTrackCandidate(
                        providerId = id,
                        providerTrackId = trackId,
                        title = title,
                        primaryArtists = listOf(artist),
                        featuredArtists = item.optJSONArray("featuredArtists").stringValues(),
                        album = item.optString("album"),
                        durationSeconds = duration,
                        explicit = item.optNullableBoolean("explicit"),
                        isrc = item.optString("isrc"),
                        offers320 = null,
                        language = item.optString("language"),
                        releaseYear = item.optInt("releaseYear", 0)
                    )
                )
            }
        }
    }

    private fun parseStream(body: String): StreamPayload? {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val url = root.optString("url").trim()
        if (url.isBlank() || root.optBoolean("encrypted", false)) return null
        val rawExpiry = when {
            root.has("expiresAtMs") -> root.optLong("expiresAtMs", 0L)
            root.has("expiresAt") -> root.optLong("expiresAt", 0L)
            else -> 0L
        }
        return StreamPayload(url, rawExpiry)
    }

    private fun rememberCandidates(found: List<AlternativeTrackCandidate>) = synchronized(candidateLock) {
        val now = clock()
        candidates.entries.removeAll { it.value.expiresAtMs <= now }
        found.forEach { candidate -> candidates[candidate.providerTrackId] = CachedCandidate(candidate, now + CANDIDATE_TTL_MS) }
        while (candidates.size > MAX_CANDIDATE_CACHE) candidates.remove(candidates.entries.first().key)
    }

    private fun configuredBase(): HttpUrl? {
        val normalized = baseUrl().trim().trimEnd('/').removeSuffix("/manifest.json")
        val parsed = normalized.toHttpUrlOrNull() ?: return null
        val host = ConfigurableProviderDestinationPolicy.normalizeHost(parsed.host)
        return parsed.takeIf {
            it.isHttps && it.port == 443 && it.username.isEmpty() && it.password.isEmpty() &&
                it.query == null && it.fragment == null && host.isNotBlank()
        }
    }

    private fun endpoint(base: HttpUrl, vararg segments: String): HttpUrl? = runCatching {
        base.newBuilder().apply { segments.forEach(::addPathSegment) }.build()
    }.getOrNull()

    private fun streamExpiry(raw: Long, url: HttpUrl): Long {
        val fromPayload = normalizeEpoch(raw)
        val fromUrl = sequenceOf("expires", "expire", "exp")
            .mapNotNull { normalizeEpoch(url.queryParameter(it)?.toLongOrNull() ?: 0L).takeIf { value -> value > 0L } }
            .minOrNull()
            ?: 0L
        return listOf(fromPayload, fromUrl).filter { it > 0L }.minOrNull() ?: (clock() + DEFAULT_STREAM_TTL_MS)
    }

    private fun normalizeEpoch(value: Long): Long = when {
        value <= 0L -> 0L
        value < 10_000_000_000L -> value * 1_000L
        else -> value
    }

    private fun VerifiedAudioFormat.toResolved(
        candidate: AlternativeTrackCandidate,
        requested: AudioQualityPreference,
        url: String,
        expiresAtMs: Long
    ) = ResolvedHighQualityStream(
        providerId = id,
        providerTrackId = candidate.providerTrackId,
        url = url,
        tier = null,
        mimeType = mimeType,
        container = container,
        codec = codec,
        contentLength = contentLength,
        estimatedKbps = estimatedKbps,
        expiresAtMs = expiresAtMs,
        sampleRateHz = sampleRateHz,
        bitDepth = bitDepth,
        channels = channels,
        isLossless = isLossless,
        isSpatial = isSpatial,
        isAtmos = isAtmos,
        deliveryMethod = deliveryMethod,
        requestedQuality = requested
    )

    private fun satisfiesAttempt(format: VerifiedAudioFormat, attempt: AudioQualityPreference): Boolean = when (attempt) {
        AudioQualityPreference.DOLBY_ATMOS -> format.isAtmos
        AudioQualityPreference.MAX_QUALITY -> format.isLossless && format.bitDepth >= 24 && format.sampleRateHz > 96_000
        AudioQualityPreference.HI_RES -> format.isLossless && format.bitDepth >= 24 && format.sampleRateHz > 44_100
        AudioQualityPreference.CD_LOSSLESS -> format.isLossless
        AudioQualityPreference.HIGH, AudioQualityPreference.NORMAL, AudioQualityPreference.DATA_SAVER -> false
    }

    private fun qualityRank(stream: ResolvedHighQualityStream): Int = when {
        stream.isAtmos -> 5
        stream.isLossless && stream.bitDepth >= 24 && stream.sampleRateHz > 96_000 -> 4
        stream.isLossless && stream.bitDepth >= 24 && stream.sampleRateHz > 44_100 -> 3
        stream.isLossless -> 2
        else -> 1
    }

    private data class ManifestCache(val base: HttpUrl, val allowedHosts: Set<String>, val expiresAtMs: Long)
    private data class CachedCandidate(val candidate: AlternativeTrackCandidate, val expiresAtMs: Long)
    private data class StreamPayload(val url: String, val expiresAtMs: Long)

    private sealed interface AttemptResolution {
        data class Resolved(val stream: ResolvedHighQualityStream) : AttemptResolution
        data class CandidateVerified(val stream: ResolvedHighQualityStream) : AttemptResolution
        data class Failed(val failure: ProviderFailure) : AttemptResolution
        object Continue : AttemptResolution
    }

    private sealed interface JsonResult {
        data class Success(val body: String) : JsonResult
        data class Failure(val failure: ProviderFailure) : JsonResult
    }

    companion object {
        const val PROVIDER_ID = "lossless-addon"
        const val RESOLUTION_TIMEOUT_MS = 3_800L
        const val MAX_SEARCH_RESULTS = 12
        const val MAX_CANDIDATE_CACHE = 128
        const val MAX_FAILED_STREAMS = 128
        const val CANDIDATE_TTL_MS = 30L * 60L * 1_000L
        const val MANIFEST_TTL_MS = 60L * 60L * 1_000L
        const val DEFAULT_STREAM_TTL_MS = 30L * 60L * 1_000L
        const val MINIMUM_URL_LIFETIME_MS = 90_000L
        const val FAILED_STREAM_COOLDOWN_MS = 10L * 60L * 1_000L
        const val MAX_MANIFEST_BODY_BYTES = 64 * 1_024
        const val MAX_SEARCH_BODY_BYTES = 512 * 1_024
        const val MAX_STREAM_BODY_BYTES = 128 * 1_024
        const val CATALOG_CIRCUIT = "lossless-addon-catalog"
        const val STREAM_CIRCUIT = "lossless-addon-stream"
    }
}

private val AudioQualityPreference.protocolValue: String
    get() = when (this) {
        AudioQualityPreference.DOLBY_ATMOS -> "atmos"
        AudioQualityPreference.MAX_QUALITY -> "max"
        AudioQualityPreference.HI_RES -> "hi_res"
        AudioQualityPreference.CD_LOSSLESS -> "lossless"
        AudioQualityPreference.HIGH -> "high"
        AudioQualityPreference.NORMAL -> "normal"
        AudioQualityPreference.DATA_SAVER -> "data_saver"
    }

private fun JSONArray?.stringValues(): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) optString(index).trim().takeIf(String::isNotBlank)?.let(::add)
    }
}

private fun JSONObject.optNullableBoolean(key: String): Boolean? =
    if (has(key) && !isNull(key)) optBoolean(key) else null
