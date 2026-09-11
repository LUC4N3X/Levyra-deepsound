package com.luc4n3x.levyra.data.hqaudio.jiosaavn

import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackCandidate
import com.luc4n3x.levyra.data.hqaudio.AudioQualityTier
import com.luc4n3x.levyra.data.hqaudio.HighQualityAudioDiagnostics
import com.luc4n3x.levyra.data.hqaudio.HighQualityAudioProvider
import com.luc4n3x.levyra.data.hqaudio.ProviderFailure
import com.luc4n3x.levyra.data.hqaudio.ProviderHttpExchange
import com.luc4n3x.levyra.data.hqaudio.ProviderHttpRequest
import com.luc4n3x.levyra.data.hqaudio.ProviderLookupOutcome
import com.luc4n3x.levyra.data.hqaudio.ProviderSearchOutcome
import com.luc4n3x.levyra.data.hqaudio.ProviderStreamOutcome
import com.luc4n3x.levyra.data.hqaudio.ProviderStreamValidator
import com.luc4n3x.levyra.data.hqaudio.ResolvedHighQualityStream
import com.luc4n3x.levyra.data.hqaudio.StreamRejection
import com.luc4n3x.levyra.data.hqaudio.StreamValidation
import java.io.IOException
import java.io.InterruptedIOException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal class JioSaavnAudioProvider(
    private val exchange: ProviderHttpExchange,
    private val profileFactory: () -> JioSaavnRequestProfile = { JioSaavnRequestProfile.create() },
    private val clock: () -> Long = System::currentTimeMillis
) : HighQualityAudioProvider {
    override val id: String = JIOSAAVN_PROVIDER_ID
    override val displayName: String = "JioSaavn"

    private val profileLock = Any()

    @Volatile
    private var profile: JioSaavnRequestProfile? = null

    @Volatile
    private var authorizedCdnRejectedUntilMs = 0L

    fun currentProfile(): JioSaavnRequestProfile = profile ?: synchronized(profileLock) {
        profile ?: profileFactory().also { created ->
            profile = created
            HighQualityAudioDiagnostics.indiaProfile(
                id,
                created.operator,
                created.maskedAddress,
                JioSaavnRequestProfile.ACCEPT_LANGUAGE
            )
        }
    }

    override suspend fun search(query: String): ProviderSearchOutcome =
        when (val result = api(JioSaavnEndpoints.search(query, SEARCH_RESULT_LIMIT))) {
            is ApiResult.Success -> JioSaavnPayloadParser.searchCandidates(result.body)
                ?.let { ProviderSearchOutcome.Found(it) }
                ?: ProviderSearchOutcome.Failed(ProviderFailure.MALFORMED_RESPONSE)
            is ApiResult.Failure -> ProviderSearchOutcome.Failed(result.failure)
        }

    override suspend fun lookup(providerTrackId: String): ProviderLookupOutcome =
        when (val result = api(JioSaavnEndpoints.songDetails(providerTrackId))) {
            is ApiResult.Success -> when (val details = JioSaavnPayloadParser.songDetails(result.body, providerTrackId)) {
                is JioSaavnSongDetails.Found -> ProviderLookupOutcome.Found(details.candidate)
                JioSaavnSongDetails.Missing -> ProviderLookupOutcome.Missing
                JioSaavnSongDetails.Malformed -> ProviderLookupOutcome.Failed(ProviderFailure.MALFORMED_RESPONSE)
            }
            is ApiResult.Failure -> if (result.failure == ProviderFailure.NOT_FOUND) {
                ProviderLookupOutcome.Missing
            } else {
                ProviderLookupOutcome.Failed(result.failure)
            }
        }

    override suspend fun resolveStream(candidate: AlternativeTrackCandidate): ProviderStreamOutcome {
        if (candidate.mediaToken.isBlank()) return ProviderStreamOutcome.Unavailable(listOf(StreamRejection.NO_MEDIA))
        val body = when (val result = api(JioSaavnEndpoints.authorizeMedia(candidate.mediaToken, AudioQualityTier.KBPS_320))) {
            is ApiResult.Success -> result.body
            is ApiResult.Failure -> return ProviderStreamOutcome.Failed(result.failure)
        }
        val location = when (val authorization = JioSaavnPayloadParser.mediaAuthorization(body)) {
            is JioSaavnMediaAuthorization.Granted -> JioSaavnMediaLocation.parse(authorization.url)
                ?: return ProviderStreamOutcome.Failed(ProviderFailure.MALFORMED_RESPONSE)
            JioSaavnMediaAuthorization.Denied -> return ProviderStreamOutcome.Unavailable(listOf(StreamRejection.NO_MEDIA))
            JioSaavnMediaAuthorization.Malformed -> return ProviderStreamOutcome.Failed(ProviderFailure.MALFORMED_RESPONSE)
        }
        val rejections = mutableListOf<StreamRejection>()
        for (tier in tiersFor(candidate)) {
            for (url in urlsFor(location, tier)) {
                currentCoroutineContext().ensureActive()
                val host = HighQualityAudioDiagnostics.hostOf(url)
                when (val validation = probe(url, tier, candidate.durationSeconds)) {
                    is StreamValidation.Valid -> {
                        HighQualityAudioDiagnostics.streamValid(id, candidate.providerTrackId, tier, host, validation)
                        return ProviderStreamOutcome.Resolved(
                            ResolvedHighQualityStream(
                                providerId = id,
                                providerTrackId = candidate.providerTrackId,
                                url = url,
                                tier = tier,
                                mimeType = validation.mimeType,
                                container = validation.container,
                                codec = validation.codec,
                                contentLength = validation.contentLength,
                                estimatedKbps = validation.estimatedKbps,
                                expiresAtMs = expiresAtFor(location, url)
                            )
                        )
                    }
                    is StreamValidation.Invalid -> {
                        rejections += validation.rejection
                        HighQualityAudioDiagnostics.streamInvalid(
                            id,
                            candidate.providerTrackId,
                            tier,
                            host,
                            validation.rejection,
                            validation.statusCode
                        )
                        if (url == location.authorizedUrl && validation.statusCode == HTTP_FORBIDDEN) {
                            authorizedCdnRejectedUntilMs = clock() + AUTHORIZED_CDN_BACKOFF_MS
                        }
                    }
                }
            }
        }
        return ProviderStreamOutcome.Unavailable(rejections)
    }

    private fun tiersFor(candidate: AlternativeTrackCandidate): List<AudioQualityTier> =
        if (candidate.offers320) {
            listOf(AudioQualityTier.KBPS_320, AudioQualityTier.KBPS_160, AudioQualityTier.KBPS_96)
        } else {
            listOf(AudioQualityTier.KBPS_160, AudioQualityTier.KBPS_96)
        }

    private fun urlsFor(location: JioSaavnMediaLocation, tier: AudioQualityTier): List<String> = buildList {
        if (location.authorizedTier == tier && clock() >= authorizedCdnRejectedUntilMs) add(location.authorizedUrl)
        add(location.openUrl(tier))
    }.distinct()

    private fun expiresAtFor(location: JioSaavnMediaLocation, url: String): Long {
        val now = clock()
        if (url == location.authorizedUrl) {
            location.authorizedExpiresAtMs?.let { return (it - AUTHORIZED_EXPIRY_MARGIN_MS).coerceAtLeast(now) }
        }
        return now + OPEN_MEDIA_TTL_MS
    }

    private suspend fun probe(url: String, tier: AudioQualityTier, durationSeconds: Int): StreamValidation {
        val headers = currentProfile().mediaHeaders() + ProviderStreamValidator.probeRangeHeader()
        val response = try {
            exchange.execute(ProviderHttpRequest(url, headers, ProviderStreamValidator.PROBE_BYTES))
        } catch (error: IOException) {
            return StreamValidation.Invalid(StreamRejection.TRANSPORT)
        }
        return ProviderStreamValidator.validate(response, tier, durationSeconds)
    }

    private suspend fun api(url: String): ApiResult {
        var lastFailure = ProviderFailure.NETWORK
        repeat(MAX_API_ATTEMPTS) {
            val session = currentProfile()
            val response = try {
                exchange.execute(ProviderHttpRequest(url, session.apiHeaders(), MAX_API_BODY_BYTES))
            } catch (error: InterruptedIOException) {
                lastFailure = ProviderFailure.TIMEOUT
                null
            } catch (error: IOException) {
                lastFailure = ProviderFailure.NETWORK
                null
            }
            when {
                response == null -> Unit
                response.isSuccessful -> return ApiResult.Success(response.bodyText)
                response.code == HTTP_FORBIDDEN -> {
                    rotateProfile(session)
                    return ApiResult.Failure(ProviderFailure.FORBIDDEN)
                }
                response.code == HTTP_NOT_FOUND -> return ApiResult.Failure(ProviderFailure.NOT_FOUND)
                response.code == HTTP_TOO_MANY_REQUESTS || response.code >= HTTP_SERVER_ERROR ->
                    lastFailure = ProviderFailure.HTTP_ERROR
                else -> return ApiResult.Failure(ProviderFailure.HTTP_ERROR)
            }
        }
        return ApiResult.Failure(lastFailure)
    }

    private fun rotateProfile(rejected: JioSaavnRequestProfile) {
        synchronized(profileLock) {
            if (profile === rejected) profile = null
        }
    }

    private sealed interface ApiResult {
        data class Success(val body: String) : ApiResult
        data class Failure(val failure: ProviderFailure) : ApiResult
    }

    companion object {
        const val SEARCH_RESULT_LIMIT = 10
        const val MAX_API_ATTEMPTS = 2
        const val MAX_API_BODY_BYTES = 1_048_576
        const val AUTHORIZED_CDN_BACKOFF_MS = 30L * 60L * 1_000L
        const val AUTHORIZED_EXPIRY_MARGIN_MS = 2L * 60L * 1_000L
        const val OPEN_MEDIA_TTL_MS = 3L * 60L * 60L * 1_000L
        private const val HTTP_FORBIDDEN = 403
        private const val HTTP_NOT_FOUND = 404
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private const val HTTP_SERVER_ERROR = 500
    }
}
