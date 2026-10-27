package com.luc4n3x.levyra.domain

enum class HighQualityAudioMode(val storageValue: String) {
    OFF("off"),
    AUTOMATIC("automatic"),
    PREFER_320("prefer_320"),
    PREFER_LOSSLESS("prefer_lossless");

    val enabled: Boolean
        get() = this != OFF

    val lossless: Boolean
        get() = this == PREFER_LOSSLESS

    companion object {
        val DEFAULT = PREFER_320

        fun fromStorage(value: String?): HighQualityAudioMode {
            val clean = value.orEmpty().trim().lowercase()
            return entries.firstOrNull { it.storageValue == clean } ?: DEFAULT
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
