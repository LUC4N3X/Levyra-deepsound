package com.luc4n3x.levyra.data

import org.json.JSONObject

internal enum class YoutubeStreamingDataShape {
    DIRECT,
    CIPHERED,
    SABR_ONLY,
    DASH_ONLY,
    HLS_ONLY,
    EMPTY;

    companion object {
        fun of(streamingData: JSONObject?): YoutubeStreamingDataShape {
            if (streamingData == null) return EMPTY
            var formatCount = 0
            var direct = false
            var ciphered = false
            for (key in FORMAT_KEYS) {
                val formats = streamingData.optJSONArray(key) ?: continue
                for (index in 0 until formats.length()) {
                    val format = formats.optJSONObject(index) ?: continue
                    formatCount++
                    if (format.optString("url").isNotBlank()) {
                        direct = true
                    } else if (format.optString("signatureCipher").isNotBlank() || format.optString("cipher").isNotBlank()) {
                        ciphered = true
                    }
                }
            }
            return when {
                direct -> DIRECT
                ciphered -> CIPHERED
                streamingData.optString("serverAbrStreamingUrl").isNotBlank() && formatCount > 0 -> SABR_ONLY
                streamingData.optString("dashManifestUrl").isNotBlank() -> DASH_ONLY
                streamingData.optString("hlsManifestUrl").isNotBlank() -> HLS_ONLY
                else -> EMPTY
            }
        }

        private val FORMAT_KEYS = listOf("formats", "adaptiveFormats")
    }
}
