package com.luc4n3x.levyra.data.hqaudio

import com.luc4n3x.levyra.domain.AudioQualityPreference
import com.luc4n3x.levyra.domain.AudioQualityRequest
import com.luc4n3x.levyra.domain.PlaybackDeliveryMethod

enum class AudioQualityTier(val kbps: Int) {
    KBPS_320(320),
    KBPS_160(160),
    KBPS_96(96);

    companion object {
        fun fromKbps(value: Int): AudioQualityTier? = entries.firstOrNull { it.kbps == value }
    }
}

enum class ProviderFailure {
    TIMEOUT,
    NETWORK,
    FORBIDDEN,
    RATE_LIMITED,
    NOT_FOUND,
    HTTP_ERROR,
    MALFORMED_RESPONSE,
    CIRCUIT_OPEN
}

sealed interface ProviderSearchOutcome {
    data class Found(val candidates: List<AlternativeTrackCandidate>) : ProviderSearchOutcome
    data class Failed(val failure: ProviderFailure) : ProviderSearchOutcome
}

sealed interface ProviderLookupOutcome {
    data class Found(val candidate: AlternativeTrackCandidate) : ProviderLookupOutcome
    data object Missing : ProviderLookupOutcome
    data class Failed(val failure: ProviderFailure) : ProviderLookupOutcome
}

sealed interface ProviderStreamOutcome {
    data class Resolved(val stream: ResolvedHighQualityStream) : ProviderStreamOutcome
    data class Unavailable(val rejections: List<StreamRejection>) : ProviderStreamOutcome
    data class Failed(val failure: ProviderFailure) : ProviderStreamOutcome
}

data class ResolvedHighQualityStream(
    val providerId: String,
    val providerTrackId: String,
    val url: String,
    val tier: AudioQualityTier?,
    val mimeType: String,
    val container: String,
    val codec: String,
    val contentLength: Long,
    val estimatedKbps: Int,
    val expiresAtMs: Long,
    val sampleRateHz: Int = 0,
    val bitDepth: Int = 0,
    val channels: Int = 0,
    val isLossless: Boolean = false,
    val isSpatial: Boolean = false,
    val isAtmos: Boolean = false,
    val deliveryMethod: PlaybackDeliveryMethod = PlaybackDeliveryMethod.PROGRESSIVE,
    val requestedQuality: AudioQualityPreference = when (tier) {
        AudioQualityTier.KBPS_320 -> AudioQualityPreference.HIGH
        AudioQualityTier.KBPS_160 -> AudioQualityPreference.NORMAL
        AudioQualityTier.KBPS_96 -> AudioQualityPreference.DATA_SAVER
        null -> AudioQualityPreference.MAX_QUALITY
    }
) {
    val host: String
        get() = url.substringAfter("://", "").substringBefore('/').substringBefore('?')

    val deliveredKbps: Int
        get() = estimatedKbps.takeIf { it > 0 } ?: tier?.kbps ?: 0

    val displayKbps: Int
        get() = if (matchesNominalTier) tier?.kbps ?: estimatedKbps else estimatedKbps

    val qualityLabel: String
        get() = when {
            isAtmos -> "Dolby Atmos"
            isLossless && bitDepth >= 24 && sampleRateHz >= 96_000 ->
                "Hi-Res $bitDepth-bit / ${formatSampleRate(sampleRateHz)}"
            isLossless && bitDepth > 0 && sampleRateHz > 0 && bitDepth == 16 && sampleRateHz == 44_100 ->
                "CD Lossless 16-bit / 44.1 kHz"
            isLossless && bitDepth > 0 && sampleRateHz > 0 ->
                "Lossless $bitDepth-bit / ${formatSampleRate(sampleRateHz)}"
            isLossless -> "Lossless"
            tier != null && matchesNominalTier -> "${tier.kbps} kbps"
            estimatedKbps > 0 -> "~$estimatedKbps kbps"
            else -> "Verified audio"
        }

    val deliveredQuality: String
        get() = qualityLabel

    private val matchesNominalTier: Boolean
        get() = tier != null && (estimatedKbps <= 0 || ProviderStreamValidator.bitrateMatches(estimatedKbps, tier))

    fun isFresh(nowMs: Long, marginMs: Long): Boolean = url.isNotBlank() && nowMs + marginMs < expiresAtMs

    private fun formatSampleRate(value: Int): String = when {
        value % 1_000 == 0 -> "${value / 1_000} kHz"
        else -> "${value / 1_000.0} kHz"
    }
}

data class ProviderBackendHealth(
    val providerId: String,
    val backend: String,
    val state: String,
    val cooldownRemainingMs: Long,
    val consecutiveFailures: Int,
    val lastSuccessAtMs: Long,
    val lastFailureAtMs: Long,
    val lastFailure: String,
    val lastLatencyMs: Long
)

interface HighQualityAudioProvider {
    val id: String
    val displayName: String

    val requiresHighQualityMode: Boolean
        get() = true

    val quarantineTrackOnStreamFailure: Boolean
        get() = true

    suspend fun search(query: String): ProviderSearchOutcome

    suspend fun search(query: String, request: AudioQualityRequest): ProviderSearchOutcome = search(query)

    suspend fun lookup(providerTrackId: String): ProviderLookupOutcome

    suspend fun resolveStream(candidate: AlternativeTrackCandidate): ProviderStreamOutcome

    suspend fun resolveStream(candidate: AlternativeTrackCandidate, request: AudioQualityRequest): ProviderStreamOutcome =
        resolveStream(candidate)

    fun isEnabled(request: AudioQualityRequest): Boolean = true

    val resolutionTimeoutMs: Long
        get() = 8_000L

    fun health(): List<ProviderBackendHealth> = emptyList()

    fun reportStreamFailure(providerTrackId: String, url: String, reason: String) = Unit
}

object HighQualityTierPolicy {
    const val MINIMUM_GAIN_KBPS = 24
    const val ASSUMED_NORMAL_KBPS = 160

    fun accepts(alternativeKbps: Int, normalKbps: Int?, normalAvailable: Boolean): Boolean {
        if (!normalAvailable) return true
        val baseline = normalKbps?.takeIf { it > 0 } ?: ASSUMED_NORMAL_KBPS
        return alternativeKbps >= baseline + MINIMUM_GAIN_KBPS
    }

    fun accepts(stream: ResolvedHighQualityStream, normalKbps: Int?, normalAvailable: Boolean): Boolean {
        if (stream.isAtmos) return true
        if (stream.isLossless && stream.bitDepth >= 16 && stream.sampleRateHz >= 44_100) return true
        if (stream.isLossless && !normalAvailable) return true
        return accepts(stream.deliveredKbps, normalKbps, normalAvailable)
    }
}
