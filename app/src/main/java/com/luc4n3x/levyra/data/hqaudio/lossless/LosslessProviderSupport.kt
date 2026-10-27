package com.luc4n3x.levyra.data.hqaudio.lossless

import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackCandidate
import com.luc4n3x.levyra.data.hqaudio.AudioQualityTier
import com.luc4n3x.levyra.data.hqaudio.HighQualityAudioDiagnostics
import com.luc4n3x.levyra.data.hqaudio.ProviderCircuitBreaker
import com.luc4n3x.levyra.data.hqaudio.ProviderFailure
import com.luc4n3x.levyra.data.hqaudio.ProviderHttpExchange
import com.luc4n3x.levyra.data.hqaudio.ProviderHttpRequest
import com.luc4n3x.levyra.data.hqaudio.ProviderHttpResponse
import com.luc4n3x.levyra.data.hqaudio.ProviderStreamOutcome
import com.luc4n3x.levyra.data.hqaudio.ProviderStreamValidator
import com.luc4n3x.levyra.data.hqaudio.ResolvedHighQualityStream
import com.luc4n3x.levyra.data.hqaudio.StreamValidation
import java.io.IOException
import java.io.InterruptedIOException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

internal class LosslessProviderSupport(
    private val providerId: String,
    private val exchange: ProviderHttpExchange,
    private val circuitBreaker: ProviderCircuitBreaker,
    private val clock: () -> Long
) {
    sealed interface Fetch {
        data class Received(val response: ProviderHttpResponse) : Fetch
        data class Failed(val failure: ProviderFailure) : Fetch
    }

    suspend fun api(url: String): Fetch = when (val fetch = fetch(url, JSON_HEADERS, MAX_API_BODY_BYTES)) {
        is Fetch.Failed -> fetch
        is Fetch.Received -> if (fetch.response.isSuccessful) fetch else Fetch.Failed(failureFor(fetch.response.code))
    }

    suspend fun probeFlac(
        candidate: AlternativeTrackCandidate,
        url: String,
        expiresAtMs: Long,
        route: String
    ): ProviderStreamOutcome {
        val response = when (val fetch = fetch(url, PROBE_HEADERS, ProviderStreamValidator.PROBE_BYTES)) {
            is Fetch.Failed -> return ProviderStreamOutcome.Failed(fetch.failure)
            is Fetch.Received -> fetch.response
        }
        if (isTransient(response.code)) return ProviderStreamOutcome.Failed(failureFor(response.code))
        val host = HighQualityAudioDiagnostics.hostOf(url)
        return when (val validation = ProviderStreamValidator.validateLossless(response, candidate.durationSeconds)) {
            is StreamValidation.Valid -> {
                HighQualityAudioDiagnostics.streamValid(providerId, candidate.providerTrackId, route, validation.tier, host, validation)
                ProviderStreamOutcome.Resolved(
                    ResolvedHighQualityStream(
                        providerId = providerId,
                        providerTrackId = candidate.providerTrackId,
                        url = url,
                        tier = validation.tier,
                        mimeType = validation.mimeType,
                        container = validation.container,
                        codec = validation.codec,
                        contentLength = validation.contentLength,
                        estimatedKbps = validation.estimatedKbps,
                        expiresAtMs = expiresAtMs,
                        bitDepth = validation.bitDepth,
                        sampleRateHz = validation.sampleRateHz
                    )
                )
            }
            is StreamValidation.Invalid -> {
                HighQualityAudioDiagnostics.streamInvalid(
                    providerId,
                    candidate.providerTrackId,
                    route,
                    AudioQualityTier.LOSSLESS,
                    host,
                    validation
                )
                ProviderStreamOutcome.Unavailable(listOf(validation.rejection))
            }
        }
    }

    fun reportLookupTimeout() {
        val permit = circuitBreaker.acquire()
        if (permit != ProviderCircuitBreaker.Permit.REJECTED) circuitBreaker.onFailure(permit, "LOOKUP_TIMEOUT")
    }

    fun health() = circuitBreaker.snapshot(providerId)

    private suspend fun fetch(url: String, headers: Map<String, String>, maxBytes: Int): Fetch {
        val permit = circuitBreaker.acquire()
        if (permit == ProviderCircuitBreaker.Permit.REJECTED) return Fetch.Failed(ProviderFailure.CIRCUIT_OPEN)
        var settled = false
        try {
            val startedAt = clock()
            val fetch = try {
                Fetch.Received(exchange.execute(ProviderHttpRequest(url, headers, maxBytes)))
            } catch (error: InterruptedIOException) {
                Fetch.Failed(ProviderFailure.TIMEOUT)
            } catch (error: IOException) {
                Fetch.Failed(ProviderFailure.NETWORK)
            }
            settled = true
            val failed = fetch is Fetch.Failed || (fetch is Fetch.Received && isTransient(fetch.response.code))
            if (fetch is Fetch.Received && fetch.response.code == HTTP_TOO_MANY_REQUESTS) {
                circuitBreaker.onRateLimited(retryAfterMs(fetch.response))
            } else if (failed) {
                val cause = (fetch as? Fetch.Failed)?.failure?.name ?: "HTTP_${(fetch as Fetch.Received).response.code}"
                circuitBreaker.onFailure(permit, cause)
            } else {
                circuitBreaker.onSuccess(permit, clock() - startedAt)
            }
            return fetch
        } finally {
            if (!settled) circuitBreaker.release(permit)
        }
    }

    private fun isTransient(code: Int): Boolean = code == HTTP_TOO_MANY_REQUESTS || code >= HTTP_SERVER_ERROR

    private fun retryAfterMs(response: ProviderHttpResponse): Long {
        val value = response.header("Retry-After").orEmpty().trim()
        val seconds = value.toLongOrNull()?.takeIf { it in 0L..86_400L }
        if (seconds != null) return seconds * 1_000L
        return runCatching {
            ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() - clock()
        }.getOrDefault(ProviderCircuitBreaker.DEFAULT_OPEN_MS)
            .coerceIn(0L, 86_400_000L)
    }

    private fun failureFor(code: Int): ProviderFailure = when {
        code == HTTP_NOT_FOUND -> ProviderFailure.NOT_FOUND
        code == HTTP_FORBIDDEN || code == HTTP_UNAUTHORIZED -> ProviderFailure.FORBIDDEN
        code == HTTP_TOO_MANY_REQUESTS -> ProviderFailure.RATE_LIMITED
        else -> ProviderFailure.HTTP_ERROR
    }

    companion object {
        const val MAX_API_BODY_BYTES = 1_048_576
        const val SEARCH_RESULT_LIMIT = 10
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_FORBIDDEN = 403
        private const val HTTP_NOT_FOUND = 404
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private const val HTTP_SERVER_ERROR = 500
        private val JSON_HEADERS = mapOf("Accept" to "application/json")
        private val PROBE_HEADERS = mapOf(ProviderStreamValidator.probeRangeHeader(), "Accept-Encoding" to "identity")
    }
}
