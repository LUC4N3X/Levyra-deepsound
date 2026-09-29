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

enum class AudioQualityPreference(val storageValue: String) {
    HIGH("high"),
    DATA_SAVER("data_saver");

    companion object {
        fun fromStorage(value: String?, fallback: AudioQualityPreference = HIGH): AudioQualityPreference {
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
    val preference: AudioQualityPreference = AudioQualityPreference.HIGH,
    val purpose: AudioStreamPurpose = AudioStreamPurpose.PLAYBACK
) {
    val cacheKey: String
        get() = "${purpose.name}:${preference.storageValue}"
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
    val isLossless: Boolean = false
)
