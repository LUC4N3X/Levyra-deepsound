package com.luc4n3x.levyra.data.hqaudio

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
    NOT_FOUND,
    HTTP_ERROR,
    MALFORMED_RESPONSE
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
    val tier: AudioQualityTier,
    val mimeType: String,
    val container: String,
    val codec: String,
    val contentLength: Long,
    val estimatedKbps: Int,
    val expiresAtMs: Long
) {
    val host: String
        get() = url.substringAfter("://", "").substringBefore('/').substringBefore('?')

    fun isFresh(nowMs: Long, marginMs: Long): Boolean = url.isNotBlank() && nowMs + marginMs < expiresAtMs
}

interface HighQualityAudioProvider {
    val id: String
    val displayName: String

    suspend fun search(query: String): ProviderSearchOutcome

    suspend fun lookup(providerTrackId: String): ProviderLookupOutcome

    suspend fun resolveStream(candidate: AlternativeTrackCandidate): ProviderStreamOutcome
}

object HighQualityTierPolicy {
    const val MINIMUM_GAIN_KBPS = 24
    const val ASSUMED_NORMAL_KBPS = 160

    fun accepts(tier: AudioQualityTier, normalKbps: Int?, normalAvailable: Boolean): Boolean {
        if (!normalAvailable) return true
        val baseline = normalKbps?.takeIf { it > 0 } ?: ASSUMED_NORMAL_KBPS
        return tier.kbps >= baseline + MINIMUM_GAIN_KBPS
    }
}
