package com.luc4n3x.levyra.data.hqaudio.jiosaavn

import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackCandidate
import com.luc4n3x.levyra.data.hqaudio.AudioQualityTier
import com.luc4n3x.levyra.data.hqaudio.HighQualityAudioDiagnostics
import com.luc4n3x.levyra.data.hqaudio.HighQualityAudioProvider
import com.luc4n3x.levyra.data.hqaudio.ProviderBackendHealth
import com.luc4n3x.levyra.data.hqaudio.ProviderCircuitBreaker
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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal class JioSaavnAudioProvider(
    private val exchange: ProviderHttpExchange,
    private val profileFactory: (Set<IndianAddressBlock>) -> JioSaavnRequestProfile = { excluded ->
        JioSaavnRequestProfile.create(excluded = excluded)
    },
    private val clock: () -> Long = System::currentTimeMillis,
    private val catalogCircuitBreaker: ProviderCircuitBreaker = ProviderCircuitBreaker(CATALOG_CIRCUIT, clock),
    private val authorizationCircuitBreaker: ProviderCircuitBreaker = ProviderCircuitBreaker(AUTHORIZATION_CIRCUIT, clock)
) : HighQualityAudioProvider {
    override val id: String = JIOSAAVN_PROVIDER_ID
    override val displayName: String = "JioSaavn"

    private val profileLock = Any()
    private val blockCooldowns = HashMap<IndianAddressBlock, Long>()

    @Volatile
    private var profile: JioSaavnRequestProfile? = null

    @Volatile
    private var authorizedCdnRejectedUntilMs = 0L

    fun currentProfile(): JioSaavnRequestProfile = profile ?: synchronized(profileLock) {
        profile ?: profileFactory(excludedBlocks(clock())).also { created ->
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
        when (val result = api(JioSaavnEndpoints.search(query, SEARCH_RESULT_LIMIT), catalogCircuitBreaker)) {
            is ApiResult.Success -> JioSaavnPayloadParser.searchCandidates(result.body)
                ?.let { ProviderSearchOutcome.Found(it) }
                ?: ProviderSearchOutcome.Failed(ProviderFailure.MALFORMED_RESPONSE)
            is ApiResult.Failure -> ProviderSearchOutcome.Failed(result.failure)
        }

    override suspend fun lookup(providerTrackId: String): ProviderLookupOutcome =
        when (val result = api(JioSaavnEndpoints.songDetails(providerTrackId), catalogCircuitBreaker)) {
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

    override fun health(): List<ProviderBackendHealth> = listOf(
        catalogCircuitBreaker.snapshot(id),
        authorizationCircuitBreaker.snapshot(id)
    )

    override suspend fun resolveStream(candidate: AlternativeTrackCandidate): ProviderStreamOutcome {
        if (candidate.mediaToken.isBlank()) return ProviderStreamOutcome.Unavailable(listOf(StreamRejection.NO_MEDIA))
        val rejections = mutableListOf<StreamRejection>()
        val probed = mutableSetOf<String>()
        val direct = JioSaavnMediaLocation.fromMediaToken(candidate.mediaToken)
        if (direct != null) {
            HighQualityAudioDiagnostics.mediaRoute(id, candidate.providerTrackId, ROUTE_DIRECT, candidate.offers320, direct.openHost)
            probeTiers(candidate, direct, QUALITY_ORDER.take(1), rejections, probed)?.let { return it }
        }
        HighQualityAudioDiagnostics.mediaRoute(id, candidate.providerTrackId, ROUTE_AUTHORIZED, candidate.offers320, "-")
        val authorizeUrl = JioSaavnEndpoints.authorizeMedia(candidate.mediaToken, AudioQualityTier.KBPS_320)
        val authorization = when (val result = api(authorizeUrl, authorizationCircuitBreaker)) {
            is ApiResult.Success -> JioSaavnPayloadParser.mediaAuthorization(result.body)
            is ApiResult.Failure ->
                return directFallback(candidate, direct, rejections, probed) ?: ProviderStreamOutcome.Failed(result.failure)
        }
        val location = when (authorization) {
            is JioSaavnMediaAuthorization.Granted -> JioSaavnMediaLocation.parse(authorization.url)
                ?: return directFallback(candidate, direct, rejections, probed)
                    ?: ProviderStreamOutcome.Failed(ProviderFailure.MALFORMED_RESPONSE)
            JioSaavnMediaAuthorization.Denied ->
                return directFallback(candidate, direct, rejections, probed)
                    ?: ProviderStreamOutcome.Unavailable(rejections + StreamRejection.NO_MEDIA)
            JioSaavnMediaAuthorization.Malformed ->
                return directFallback(candidate, direct, rejections, probed)
                    ?: ProviderStreamOutcome.Failed(ProviderFailure.MALFORMED_RESPONSE)
        }
        return probeTiers(candidate, location, QUALITY_ORDER, rejections, probed)
            ?: directFallback(candidate, direct, rejections, probed)
            ?: ProviderStreamOutcome.Unavailable(rejections)
    }

    private suspend fun directFallback(
        candidate: AlternativeTrackCandidate,
        direct: JioSaavnMediaLocation?,
        rejections: MutableList<StreamRejection>,
        probed: MutableSet<String>
    ): ProviderStreamOutcome? = direct?.let { probeTiers(candidate, it, QUALITY_ORDER.drop(1), rejections, probed) }

    private suspend fun probeTiers(
        candidate: AlternativeTrackCandidate,
        location: JioSaavnMediaLocation,
        tiers: List<AudioQualityTier>,
        rejections: MutableList<StreamRejection>,
        probed: MutableSet<String>
    ): ProviderStreamOutcome.Resolved? {
        for (tier in tiers) {
            for (url in urlsFor(location, tier)) {
                if (!probed.add(url)) continue
                currentCoroutineContext().ensureActive()
                val host = HighQualityAudioDiagnostics.hostOf(url)
                val route = routeOf(location, url)
                when (val validation = probe(url, tier, candidate.durationSeconds)) {
                    is StreamValidation.Valid -> {
                        HighQualityAudioDiagnostics.streamValid(id, candidate.providerTrackId, route, tier, host, validation)
                        return ProviderStreamOutcome.Resolved(
                            ResolvedHighQualityStream(
                                providerId = id,
                                providerTrackId = candidate.providerTrackId,
                                url = url,
                                tier = validation.tier,
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
                        HighQualityAudioDiagnostics.streamInvalid(id, candidate.providerTrackId, route, tier, host, validation)
                        if (url == location.authorizedUrl && validation.statusCode == HTTP_FORBIDDEN) {
                            authorizedCdnRejectedUntilMs = clock() + AUTHORIZED_CDN_BACKOFF_MS
                        }
                    }
                }
            }
        }
        return null
    }

    private fun routeOf(location: JioSaavnMediaLocation, url: String): String = when {
        location.authorizedUrl.isEmpty() -> ROUTE_DIRECT
        url == location.authorizedUrl -> ROUTE_AUTHORIZED
        else -> ROUTE_AUTHORIZED_OPEN
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

    private suspend fun api(url: String, circuitBreaker: ProviderCircuitBreaker): ApiResult {
        val permit = circuitBreaker.acquire()
        if (permit == ProviderCircuitBreaker.Permit.REJECTED) return ApiResult.Failure(ProviderFailure.CIRCUIT_OPEN)
        var settled = false
        try {
            val attempts = if (permit == ProviderCircuitBreaker.Permit.PROBE) 1 else MAX_API_ATTEMPTS
            val startedAt = clock()
            val result = attemptApi(url, attempts)
            settled = true
            if (result is ApiResult.Failure && result.failure != ProviderFailure.NOT_FOUND) {
                circuitBreaker.onFailure(permit, result.failure.name)
            } else {
                circuitBreaker.onSuccess(permit, clock() - startedAt)
            }
            return result
        } finally {
            if (!settled) circuitBreaker.release(permit)
        }
    }

    private suspend fun attemptApi(url: String, attempts: Int): ApiResult {
        var lastFailure = ProviderFailure.NETWORK
        repeat(attempts) {
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
                response.code == HTTP_NOT_FOUND -> return ApiResult.Failure(ProviderFailure.NOT_FOUND)
                isProfileRejection(response) -> {
                    rejectProfile(session, response.code)
                    lastFailure = if (response.code == HTTP_FORBIDDEN) ProviderFailure.FORBIDDEN else ProviderFailure.RATE_LIMITED
                }
                response.code >= HTTP_SERVER_ERROR -> lastFailure = ProviderFailure.HTTP_ERROR
                else -> return ApiResult.Failure(ProviderFailure.HTTP_ERROR)
            }
        }
        return ApiResult.Failure(lastFailure)
    }

    private fun isProfileRejection(response: ProviderHttpResponse): Boolean =
        response.code == HTTP_FORBIDDEN || response.code == HTTP_TOO_MANY_REQUESTS

    private fun rejectProfile(rejected: JioSaavnRequestProfile, statusCode: Int) {
        val rotated = synchronized(profileLock) {
            if (profile === rejected) {
                rejected.block?.let { blockCooldowns[it] = clock() + BLOCK_COOLDOWN_MS }
                profile = null
                true
            } else {
                false
            }
        }
        if (rotated) {
            HighQualityAudioDiagnostics.profileRejected(
                id,
                rejected.operator,
                rejected.maskedAddress,
                statusCode,
                BLOCK_COOLDOWN_MS
            )
        }
    }

    private fun excludedBlocks(now: Long): Set<IndianAddressBlock> {
        blockCooldowns.values.removeAll { it <= now }
        if (blockCooldowns.size < JioSaavnRequestProfile.addressBlocks.size) return blockCooldowns.keys.toSet()
        val soonest = blockCooldowns.minBy { it.value }.key
        return blockCooldowns.keys - soonest
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
        const val BLOCK_COOLDOWN_MS = 10L * 60L * 1_000L
        const val CATALOG_CIRCUIT = "jiosaavn-catalog"
        const val AUTHORIZATION_CIRCUIT = "jiosaavn-authorization"
        private val QUALITY_ORDER = listOf(AudioQualityTier.KBPS_320, AudioQualityTier.KBPS_160, AudioQualityTier.KBPS_96)
        private const val ROUTE_DIRECT = "DIRECT_DECRYPTED"
        private const val ROUTE_AUTHORIZED = "AUTHORIZED"
        private const val ROUTE_AUTHORIZED_OPEN = "AUTHORIZED_OPEN"
        private const val HTTP_FORBIDDEN = 403
        private const val HTTP_NOT_FOUND = 404
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private const val HTTP_SERVER_ERROR = 500
    }
}
