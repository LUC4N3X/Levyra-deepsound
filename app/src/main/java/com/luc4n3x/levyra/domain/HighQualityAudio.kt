package com.luc4n3x.levyra.domain

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
    val confidence: Int
)
