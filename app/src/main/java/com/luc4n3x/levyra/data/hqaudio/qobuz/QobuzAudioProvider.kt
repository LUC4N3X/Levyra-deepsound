package com.luc4n3x.levyra.data.hqaudio.qobuz

import com.luc4n3x.levyra.data.hqaudio.AlternativeSearchPlan
import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackCandidate
import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackQuery
import com.luc4n3x.levyra.data.hqaudio.HighQualityAudioDiagnostics
import com.luc4n3x.levyra.data.hqaudio.HighQualityAudioProvider
import com.luc4n3x.levyra.data.hqaudio.HighQualityPreference
import com.luc4n3x.levyra.data.hqaudio.ProviderBackendHealth
import com.luc4n3x.levyra.data.hqaudio.ProviderCircuitBreaker
import com.luc4n3x.levyra.data.hqaudio.ProviderDestinationPolicy
import com.luc4n3x.levyra.data.hqaudio.ProviderFailure
import com.luc4n3x.levyra.data.hqaudio.ProviderHttpExchange
import com.luc4n3x.levyra.data.hqaudio.ProviderHttpRequest
import com.luc4n3x.levyra.data.hqaudio.ProviderHttpResponse
import com.luc4n3x.levyra.data.hqaudio.ProviderLookupOutcome
import com.luc4n3x.levyra.data.hqaudio.ProviderSearchOutcome
import com.luc4n3x.levyra.data.hqaudio.ProviderStreamOutcome
import com.luc4n3x.levyra.data.hqaudio.ProviderStreamValidator
import com.luc4n3x.levyra.data.hqaudio.ResolvedHighQualityStream
import com.luc4n3x.levyra.data.hqaudio.StreamRejection
import com.luc4n3x.levyra.data.hqaudio.StreamValidation
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.UnknownHostException
import java.util.Locale
import javax.net.ssl.SSLException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal class QobuzAudioProvider(
    private val exchange: ProviderHttpExchange,
    private val backends: List<QobuzBackend> = QobuzBackends.defaults,
    private val clock: () -> Long = System::currentTimeMillis,
    private val region: String = QobuzBackends.region(),
    private val requestBudgetMs: Long = REQUEST_BUDGET_MS
) : HighQualityAudioProvider {
    override val id: String = QOBUZ_PROVIDER_ID
    override val displayName: String = "Qobuz"
    override val supportsLookup: Boolean = false
    override val losslessCapable: Boolean = true

    private val breakers: Map<String, ProviderCircuitBreaker> = backends.associate { backend ->
        backend.id to ProviderCircuitBreaker(
            providerId = "$QOBUZ_PROVIDER_ID-${backend.id}",
            clock = clock,
            failureThreshold = BACKEND_FAILURE_THRESHOLD,
            baseOpenMs = BACKEND_OPEN_MS
        )
    }

    override fun searchQueries(query: AlternativeTrackQuery): List<String> {
        val isrc = query.isrc.uppercase(Locale.ROOT).filter { it in 'A'..'Z' || it in '0'..'9' }
        return buildList {
            if (isrc.length == ISRC_LENGTH) add(isrc)
            addAll(AlternativeSearchPlan.queries(query))
        }.distinct()
    }

    override suspend fun search(query: String): ProviderSearchOutcome {
        var failure = ProviderFailure.CIRCUIT_OPEN
        for (backend in backends) {
            when (val result = call(backend, OPERATION_SEARCH, backend.searchUrl(query), MAX_SEARCH_BODY_BYTES, ::interpretSearch)) {
                is BackendResult.Answered -> return ProviderSearchOutcome.Found(result.value)
                is BackendResult.Failed -> failure = moreInformative(failure, result.failure)
            }
        }
        return ProviderSearchOutcome.Failed(failure)
    }

    override suspend fun lookup(providerTrackId: String): ProviderLookupOutcome = ProviderLookupOutcome.Missing

    override suspend fun resolveStream(
        candidate: AlternativeTrackCandidate,
        preference: HighQualityPreference
    ): ProviderStreamOutcome {
        val ladder = QobuzFormat.ladder(
            candidate.maxBitDepth,
            candidate.maxSampleRateHz,
            maximum = preference == HighQualityPreference.MAXIMUM
        )
        val rejections = mutableListOf<StreamRejection>()
        val probedUrls = HashSet<String>()
        var failure = ProviderFailure.CIRCUIT_OPEN
        for (format in ladder) {
            var answered = false
            for (backend in backends) {
                currentCoroutineContext().ensureActive()
                val url = backend.streamUrl(candidate.providerTrackId, format)
                val grant = when (val result = call(backend, OPERATION_STREAM, url, MAX_STREAM_BODY_BYTES, ::interpretStream)) {
                    is BackendResult.Failed -> {
                        failure = moreInformative(failure, result.failure)
                        continue
                    }
                    is BackendResult.Answered -> result.value
                }
                answered = true
                if (grant !is QobuzStreamPayload.Granted) {
                    rejections += StreamRejection.NO_MEDIA
                    break
                }
                if (!probedUrls.add(grant.url)) break
                when (val media = probeMedia(candidate, format, grant.url)) {
                    is MediaProbe.Accepted -> return ProviderStreamOutcome.Resolved(media.stream)
                    is MediaProbe.Rejected -> {
                        rejections += media.rejection
                        if (media.rejection in terminalRejections) return ProviderStreamOutcome.Unavailable(rejections)
                    }
                }
                break
            }
            if (!answered) break
        }
        return if (rejections.isNotEmpty()) {
            ProviderStreamOutcome.Unavailable(rejections)
        } else {
            ProviderStreamOutcome.Failed(failure)
        }
    }

    override fun health(): List<ProviderBackendHealth> =
        backends.map { backend -> breakers.getValue(backend.id).snapshot(backend.id) }

    private suspend fun probeMedia(candidate: AlternativeTrackCandidate, format: QobuzFormat, url: String): MediaProbe {
        val host = HighQualityAudioDiagnostics.hostOf(url)
        val parsed = url.toHttpUrlOrNull()
        val rejection = when {
            parsed == null || !ProviderDestinationPolicy.allows(parsed) -> StreamRejection.UNSAFE_DESTINATION
            expiresAt(url) <= clock() + EXPIRY_SAFETY_MARGIN_MS -> StreamRejection.EXPIRED
            else -> null
        }
        if (rejection != null) return rejectMedia(candidate, format, host, rejection, 0)
        val response = try {
            withTimeoutOrNull(requestBudgetMs) {
                exchange.execute(ProviderHttpRequest(url, mediaHeaders(), ProviderStreamValidator.PROBE_BYTES))
            }
        } catch (error: IOException) {
            null
        } ?: return rejectMedia(candidate, format, host, StreamRejection.TRANSPORT, 0)
        val validation = if (format.lossless) {
            ProviderStreamValidator.validateFlac(response, candidate.durationSeconds)
        } else {
            ProviderStreamValidator.validateMp3(response, format.nominalKbps, candidate.durationSeconds)
        }
        return when (validation) {
            is StreamValidation.Invalid -> rejectMedia(candidate, format, host, validation.rejection, validation.statusCode)
            is StreamValidation.Valid -> {
                HighQualityAudioDiagnostics.streamValid(id, candidate.providerTrackId, format.name, host, validation)
                MediaProbe.Accepted(
                    ResolvedHighQualityStream(
                        providerId = id,
                        providerTrackId = candidate.providerTrackId,
                        url = url,
                        quality = validation.quality,
                        mimeType = validation.mimeType,
                        container = validation.container,
                        codec = validation.codec,
                        contentLength = validation.contentLength,
                        expiresAtMs = expiresAt(url)
                    )
                )
            }
        }
    }

    private fun rejectMedia(
        candidate: AlternativeTrackCandidate,
        format: QobuzFormat,
        host: String,
        rejection: StreamRejection,
        statusCode: Int
    ): MediaProbe.Rejected {
        HighQualityAudioDiagnostics.streamInvalid(id, candidate.providerTrackId, format.name, host, rejection, statusCode)
        return MediaProbe.Rejected(rejection)
    }

    private suspend fun <T> call(
        backend: QobuzBackend,
        operation: String,
        url: String,
        maxBodyBytes: Int,
        interpret: (ProviderHttpResponse) -> Verdict<T>
    ): BackendResult<T> {
        val breaker = breakers.getValue(backend.id)
        val permit = breaker.acquire()
        if (permit == ProviderCircuitBreaker.Permit.REJECTED) {
            HighQualityAudioDiagnostics.backend(id, backend.id, operation, ProviderFailure.CIRCUIT_OPEN.name, 0L)
            return BackendResult.Failed(ProviderFailure.CIRCUIT_OPEN)
        }
        val startedAt = clock()
        var settled = false
        try {
            val verdict = try {
                withTimeoutOrNull(requestBudgetMs) {
                    exchange.execute(ProviderHttpRequest(url, apiHeaders(backend), maxBodyBytes))
                }?.let { response -> transportVerdict(response) ?: interpret(response) }
                    ?: Verdict.Failure(ProviderFailure.TIMEOUT)
            } catch (error: IOException) {
                ioVerdict(error)
            }
            val latencyMs = clock() - startedAt
            settled = true
            return when (verdict) {
                is Verdict.Success -> {
                    breaker.onSuccess(permit, latencyMs)
                    HighQualityAudioDiagnostics.backend(id, backend.id, operation, OUTCOME_OK, latencyMs)
                    BackendResult.Answered(verdict.value)
                }
                is Verdict.Failure -> {
                    breaker.onFailure(permit, "$operation:${verdict.failure.name}", verdict.trip, verdict.minimumOpenMs)
                    HighQualityAudioDiagnostics.backend(id, backend.id, operation, verdict.failure.name, latencyMs)
                    BackendResult.Failed(verdict.failure)
                }
            }
        } finally {
            if (!settled) breaker.release(permit)
        }
    }

    private fun transportVerdict(response: ProviderHttpResponse): Verdict.Failure? = when {
        response.code == HTTP_FORBIDDEN -> Verdict.Failure(ProviderFailure.FORBIDDEN, trip = true)
        response.code == HTTP_TOO_MANY_REQUESTS ->
            Verdict.Failure(ProviderFailure.RATE_LIMITED, trip = true, minimumOpenMs = retryAfterMs(response))
        response.code >= HTTP_SERVER_ERROR -> Verdict.Failure(ProviderFailure.HTTP_ERROR, trip = true)
        response.isSuccessful && QobuzPayloadParser.looksLikeMarkup(response.bodyText) ->
            Verdict.Failure(ProviderFailure.MALFORMED_RESPONSE, trip = true)
        else -> null
    }

    private fun interpretSearch(response: ProviderHttpResponse): Verdict<List<AlternativeTrackCandidate>> {
        if (!response.isSuccessful) return Verdict.Failure(ProviderFailure.HTTP_ERROR, trip = response.code == HTTP_NOT_FOUND)
        return when (val payload = QobuzPayloadParser.search(response.bodyText, id)) {
            is QobuzSearchPayload.Tracks -> Verdict.Success(payload.candidates)
            QobuzSearchPayload.Captcha -> Verdict.Failure(ProviderFailure.FORBIDDEN, trip = true)
            QobuzSearchPayload.Rejected -> Verdict.Failure(ProviderFailure.HTTP_ERROR)
            QobuzSearchPayload.Malformed -> Verdict.Failure(ProviderFailure.MALFORMED_RESPONSE)
        }
    }

    private fun interpretStream(response: ProviderHttpResponse): Verdict<QobuzStreamPayload> {
        if (response.code == HTTP_NOT_FOUND) return Verdict.Success(QobuzStreamPayload.FormatUnavailable)
        if (!response.isSuccessful) return Verdict.Failure(ProviderFailure.HTTP_ERROR)
        return when (val payload = QobuzPayloadParser.stream(response.bodyText)) {
            is QobuzStreamPayload.Granted, QobuzStreamPayload.FormatUnavailable -> Verdict.Success(payload)
            QobuzStreamPayload.Captcha -> Verdict.Failure(ProviderFailure.FORBIDDEN, trip = true)
            QobuzStreamPayload.Malformed -> Verdict.Failure(ProviderFailure.MALFORMED_RESPONSE)
        }
    }

    private fun ioVerdict(error: IOException): Verdict.Failure = when (error) {
        is UnknownHostException, is ConnectException, is NoRouteToHostException, is SSLException ->
            Verdict.Failure(ProviderFailure.NETWORK, trip = true)
        is InterruptedIOException -> Verdict.Failure(ProviderFailure.TIMEOUT)
        else -> Verdict.Failure(ProviderFailure.NETWORK)
    }

    private fun retryAfterMs(response: ProviderHttpResponse): Long =
        response.header("Retry-After")?.trim()?.toLongOrNull()
            ?.takeIf { it > 0L }
            ?.let { (it * 1_000L).coerceAtMost(MAX_RETRY_AFTER_MS) }
            ?: 0L

    private fun expiresAt(url: String): Long {
        val signedUntilSeconds = url.toHttpUrlOrNull()?.queryParameter(EXPIRY_PARAMETER)?.toLongOrNull()
        return signedUntilSeconds?.let { it * 1_000L } ?: (clock() + UNSIGNED_STREAM_TTL_MS)
    }

    private fun apiHeaders(backend: QobuzBackend): Map<String, String> = buildMap {
        put("Accept", "application/json")
        put("User-Agent", USER_AGENT)
        put("Origin", backend.baseUrl)
        put("Referer", "${backend.baseUrl}/")
        if (backend.sendsRegion) put("Token-Country", region)
    }

    private fun mediaHeaders(): Map<String, String> =
        mapOf("User-Agent" to USER_AGENT, ProviderStreamValidator.probeRangeHeader())

    private fun moreInformative(current: ProviderFailure, next: ProviderFailure): ProviderFailure =
        if (current == ProviderFailure.CIRCUIT_OPEN) next else current

    private sealed interface Verdict<out T> {
        data class Success<T>(val value: T) : Verdict<T>

        data class Failure(
            val failure: ProviderFailure,
            val trip: Boolean = false,
            val minimumOpenMs: Long = 0L
        ) : Verdict<Nothing>
    }

    private sealed interface BackendResult<out T> {
        data class Answered<T>(val value: T) : BackendResult<T>
        data class Failed(val failure: ProviderFailure) : BackendResult<Nothing>
    }

    private sealed interface MediaProbe {
        data class Accepted(val stream: ResolvedHighQualityStream) : MediaProbe
        data class Rejected(val rejection: StreamRejection) : MediaProbe
    }

    companion object {
        const val QOBUZ_PROVIDER_ID = "qobuz"
        const val REQUEST_BUDGET_MS = 3_500L
        const val BACKEND_FAILURE_THRESHOLD = 2
        const val BACKEND_OPEN_MS = 60_000L
        const val MAX_RETRY_AFTER_MS = 10L * 60L * 1_000L
        const val UNSIGNED_STREAM_TTL_MS = 10L * 60L * 1_000L
        const val EXPIRY_SAFETY_MARGIN_MS = 120_000L
        const val MAX_SEARCH_BODY_BYTES = 1_048_576
        const val MAX_STREAM_BODY_BYTES = 65_536
        private const val ISRC_LENGTH = 12
        private const val EXPIRY_PARAMETER = "etsp"
        private const val OPERATION_SEARCH = "search"
        private const val OPERATION_STREAM = "stream"
        private const val OUTCOME_OK = "OK"
        private const val HTTP_FORBIDDEN = 403
        private const val HTTP_NOT_FOUND = 404
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private const val HTTP_SERVER_ERROR = 500
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36"
        private val terminalRejections = setOf(StreamRejection.PREVIEW, StreamRejection.DURATION_MISMATCH)
    }
}
