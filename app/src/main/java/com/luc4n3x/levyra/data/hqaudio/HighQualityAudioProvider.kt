package com.luc4n3x.levyra.data.hqaudio

import java.util.Locale

enum class AudioQualityTier(val kbps: Int) {
    KBPS_320(320),
    KBPS_160(160),
    KBPS_96(96);

    companion object {
        fun fromKbps(value: Int): AudioQualityTier? = entries.firstOrNull { it.kbps == value }
    }
}

enum class HighQualityCodec(val label: String, val mimeType: String, val container: String, val lossless: Boolean) {
    AAC("AAC", "audio/mp4", "mp4", false),
    MP3("MP3", "audio/mpeg", "mp3", false),
    FLAC("FLAC", "audio/flac", "flac", true)
}

data class HighQualityStreamQuality(
    val codec: HighQualityCodec,
    val nominalKbps: Int?,
    val estimatedKbps: Int,
    val sampleRateHz: Int? = null,
    val bitDepth: Int? = null,
    val channels: Int? = null
) {
    val lossless: Boolean
        get() = codec.lossless

    val isHiRes: Boolean
        get() = lossless && ((bitDepth ?: 0) > CD_BIT_DEPTH || (sampleRateHz ?: 0) > CD_MAX_SAMPLE_RATE_HZ)

    val isCdQuality: Boolean
        get() = lossless && !isHiRes && bitDepth == CD_BIT_DEPTH && sampleRateHz != null

    val effectiveKbps: Int
        get() = nominalKbps?.takeIf { it > 0 } ?: estimatedKbps

    val rank: Long
        get() = if (lossless) {
            LOSSLESS_RANK_BASE + (bitDepth ?: CD_BIT_DEPTH) * 1_000_000L + (sampleRateHz ?: CD_SAMPLE_RATE_HZ)
        } else {
            effectiveKbps.toLong()
        }

    val label: String
        get() = if (lossless) {
            buildList {
                add(codec.label)
                bitDepth?.takeIf { it > 0 }?.let { add("$it-bit") }
                sampleRateHz?.takeIf { it > 0 }?.let { add(sampleRateLabel(it)) }
            }.joinToString(" · ")
        } else {
            "${codec.label} $effectiveKbps kbps"
        }

    val signature: String
        get() = if (lossless) "${codec.name}-${bitDepth ?: 0}-${sampleRateHz ?: 0}" else "${codec.name}-$effectiveKbps"

    fun substantiallyAbove(normalKbps: Int): Boolean = lossless || effectiveKbps >= normalKbps + MINIMUM_LOSSY_GAIN_KBPS

    companion object {
        const val CD_BIT_DEPTH = 16
        const val CD_SAMPLE_RATE_HZ = 44_100
        const val CD_MAX_SAMPLE_RATE_HZ = 48_000
        const val MINIMUM_LOSSY_GAIN_KBPS = 24
        const val STRONG_LOSSY_KBPS = 256
        private const val LOSSLESS_RANK_BASE = 1_000_000_000L

        fun lossy(codec: HighQualityCodec, nominalKbps: Int, estimatedKbps: Int): HighQualityStreamQuality =
            HighQualityStreamQuality(codec = codec, nominalKbps = nominalKbps, estimatedKbps = estimatedKbps)

        fun sampleRateLabel(sampleRateHz: Int): String {
            val text = if (sampleRateHz % 1_000 == 0) {
                (sampleRateHz / 1_000).toString()
            } else {
                String.format(Locale.ROOT, "%.1f", sampleRateHz / 1_000.0)
            }
            return "$text kHz"
        }
    }
}

enum class HighQualityPreference {
    BALANCED,
    MAXIMUM
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
    val quality: HighQualityStreamQuality,
    val mimeType: String,
    val container: String,
    val codec: String,
    val contentLength: Long,
    val expiresAtMs: Long
) {
    val estimatedKbps: Int
        get() = quality.estimatedKbps

    val host: String
        get() = url.substringAfter("://", "").substringBefore('/').substringBefore('?')

    fun isFresh(nowMs: Long, marginMs: Long): Boolean = url.isNotBlank() && nowMs + marginMs < expiresAtMs
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

    val supportsLookup: Boolean
        get() = true

    val losslessCapable: Boolean
        get() = false

    fun searchQueries(query: AlternativeTrackQuery): List<String> = AlternativeSearchPlan.queries(query)

    suspend fun search(query: String): ProviderSearchOutcome

    suspend fun lookup(providerTrackId: String): ProviderLookupOutcome

    suspend fun resolveStream(
        candidate: AlternativeTrackCandidate,
        preference: HighQualityPreference = HighQualityPreference.BALANCED
    ): ProviderStreamOutcome

    fun health(): List<ProviderBackendHealth> = emptyList()
}

object HighQualityTierPolicy {
    const val ASSUMED_NORMAL_KBPS = 160

    fun accepts(quality: HighQualityStreamQuality, normalKbps: Int?, normalAvailable: Boolean): Boolean {
        if (!normalAvailable) return true
        val baseline = normalKbps?.takeIf { it > 0 } ?: ASSUMED_NORMAL_KBPS
        return quality.substantiallyAbove(baseline)
    }

    fun isStrong(quality: HighQualityStreamQuality): Boolean =
        quality.lossless || quality.effectiveKbps >= HighQualityStreamQuality.STRONG_LOSSY_KBPS
}
