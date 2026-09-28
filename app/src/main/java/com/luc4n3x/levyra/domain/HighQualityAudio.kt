package com.luc4n3x.levyra.domain

import java.util.Locale

enum class HighQualityAudioMode(val storageValue: String) {
    OFF("off"),
    AUTOMATIC("automatic"),
    PREFER_320("prefer_320");

    val enabled: Boolean
        get() = this != OFF

    companion object {
        fun fromStorage(value: String?): HighQualityAudioMode {
            val clean = value.orEmpty().trim().lowercase()
            return entries.firstOrNull { it.storageValue == clean } ?: PREFER_320
        }
    }
}

enum class AudioQualityPreference(
    val storageValue: String,
    val maximumBitDepth: Int,
    val maximumSampleRateHz: Int
) {
    DOLBY_ATMOS("dolby_atmos", 24, 192_000),
    MAX_QUALITY("max_quality", 24, 192_000),
    HI_RES("hi_res", 24, 96_000),
    CD_LOSSLESS("cd_lossless", 16, 44_100),
    HIGH("high", 0, 0),
    NORMAL("normal", 0, 0),
    DATA_SAVER("data_saver", 0, 0);

    val requestsLossless: Boolean
        get() = this == DOLBY_ATMOS || this == MAX_QUALITY || this == HI_RES || this == CD_LOSSLESS

    fun losslessAttemptOrder(allowUpgrade: Boolean = true): List<AudioQualityPreference> = when (this) {
        DOLBY_ATMOS -> listOf(DOLBY_ATMOS, MAX_QUALITY, HI_RES, CD_LOSSLESS)
        MAX_QUALITY -> listOf(MAX_QUALITY, HI_RES, CD_LOSSLESS)
        HI_RES -> if (allowUpgrade) listOf(HI_RES, MAX_QUALITY, CD_LOSSLESS) else listOf(HI_RES, CD_LOSSLESS)
        CD_LOSSLESS -> if (allowUpgrade) listOf(CD_LOSSLESS, HI_RES, MAX_QUALITY) else listOf(CD_LOSSLESS)
        HIGH, NORMAL, DATA_SAVER -> emptyList()
    }.distinct()

    companion object {
        fun fromStorage(value: String?, fallback: AudioQualityPreference = MAX_QUALITY): AudioQualityPreference {
            val clean = value.orEmpty().trim().lowercase(Locale.ROOT)
            return entries.firstOrNull {
                it.storageValue == clean || it.name.lowercase(Locale.ROOT) == clean
            } ?: fallback
        }
    }
}

enum class AudioStreamPurpose {
    PLAYBACK,
    DOWNLOAD
}

data class AudioQualityRequest(
    val preference: AudioQualityPreference = AudioQualityPreference.MAX_QUALITY,
    val purpose: AudioStreamPurpose = AudioStreamPurpose.PLAYBACK,
    val losslessEnabled: Boolean = false,
    val allowUpgrade: Boolean = true
) {
    val cacheKey: String
        get() = "${purpose.name}:${preference.storageValue}:$losslessEnabled:$allowUpgrade"
}

enum class AlternativeMatchVerdict {
    EXACT,
    HIGH,
    REJECTED
}

data class AlternativeAudioSource(
    val providerId: String,
    val providerTrackId: String,
    val bitrateKbps: Int,
    val verdict: AlternativeMatchVerdict,
    val confidence: Int,
    val requestedQuality: AudioQualityPreference = AudioQualityPreference.HIGH,
    val deliveredQuality: String = "",
    val isLossless: Boolean = false,
    val isSpatial: Boolean = false,
    val isAtmos: Boolean = false
)
