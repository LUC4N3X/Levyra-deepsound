package com.luc4n3x.levyra.domain

import java.io.IOException
import org.json.JSONObject

private const val QualityPayloadKey = "levyraDownloadQuality"

const val DOWNLOAD_QUALITY_UNAVAILABLE = "download_quality_unavailable"

class DownloadQualityUnavailableException : IOException(DOWNLOAD_QUALITY_UNAVAILABLE)

enum class DownloadQualitySource(val storageKey: String) {
    Lossless("lossless"),
    HighBitrate("high_bitrate"),
    Standard("standard");

    companion object {
        fun fromStorage(value: String): DownloadQualitySource? = entries.firstOrNull { it.storageKey == value }
    }
}

data class DownloadQualityChoice(
    val source: DownloadQualitySource,
    val kbps: Int = 0,
    val bitDepth: Int = 0,
    val sampleRateHz: Int = 0
) {
    val hiRes: Boolean
        get() = source == DownloadQualitySource.Lossless && (bitDepth > CD_BIT_DEPTH || sampleRateHz > CD_MAX_SAMPLE_RATE_HZ)

    val profileKey: String
        get() = when (source) {
            DownloadQualitySource.Lossless -> if (hiRes) "LosslessHiRes" else "Lossless"
            DownloadQualitySource.HighBitrate -> "HQ${kbps.coerceAtLeast(0)}"
            DownloadQualitySource.Standard -> "Standard"
        }

    internal fun toJson(): JSONObject = JSONObject()
        .put("source", source.storageKey)
        .put("kbps", kbps)
        .put("bitDepth", bitDepth)
        .put("sampleRateHz", sampleRateHz)

    internal companion object {
        const val CD_BIT_DEPTH = 16
        const val CD_MAX_SAMPLE_RATE_HZ = 48_000

        fun fromJson(json: JSONObject): DownloadQualityChoice? {
            val source = DownloadQualitySource.fromStorage(json.optString("source")) ?: return null
            return DownloadQualityChoice(
                source = source,
                kbps = json.optInt("kbps", 0).coerceAtLeast(0),
                bitDepth = json.optInt("bitDepth", 0).coerceAtLeast(0),
                sampleRateHz = json.optInt("sampleRateHz", 0).coerceAtLeast(0)
            )
        }
    }
}

internal fun withDownloadQualityChoice(payload: String, choice: DownloadQualityChoice?): String =
    if (choice == null) payload else JSONObject(payload).put(QualityPayloadKey, choice.toJson()).toString()

internal fun downloadQualityChoice(payload: String): DownloadQualityChoice? = runCatching {
    JSONObject(payload).optJSONObject(QualityPayloadKey)?.let(DownloadQualityChoice::fromJson)
}.getOrNull()
