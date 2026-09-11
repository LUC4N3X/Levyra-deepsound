package com.luc4n3x.levyra.data.hqaudio

import java.util.Locale
import kotlin.math.roundToInt

enum class StreamRejection {
    NO_MEDIA,
    TRANSPORT,
    HTTP_STATUS,
    EMPTY_BODY,
    NOT_AUDIO,
    UNSUPPORTED_CONTAINER,
    UNKNOWN_LENGTH,
    BITRATE_MISMATCH
}

sealed interface StreamValidation {
    data class Valid(
        val mimeType: String,
        val container: String,
        val codec: String,
        val contentLength: Long,
        val estimatedKbps: Int
    ) : StreamValidation

    data class Invalid(
        val rejection: StreamRejection,
        val statusCode: Int = 0
    ) : StreamValidation
}

internal object ProviderStreamValidator {
    const val PROBE_BYTES = 8_192
    private const val LOWER_BITRATE_RATIO = 0.85
    private const val UPPER_BITRATE_RATIO = 1.15
    private const val UPPER_BITRATE_SLACK_KBPS = 8

    private val contentRangeTotal = Regex("bytes\\s+\\d+-\\d+/(\\d+)", RegexOption.IGNORE_CASE)
    private val acceptedMimeTypes = setOf(
        "audio/mp4",
        "audio/x-m4a",
        "audio/m4a",
        "audio/aac",
        "audio/mp4a-latm",
        "video/mp4",
        "application/octet-stream"
    )
    private val fileTypeBox = "ftyp".toByteArray(Charsets.US_ASCII)
    private val aacSampleEntry = "mp4a".toByteArray(Charsets.US_ASCII)

    fun probeRangeHeader(): Pair<String, String> = "Range" to "bytes=0-${PROBE_BYTES - 1}"

    fun validate(response: ProviderHttpResponse, tier: AudioQualityTier, durationSeconds: Int): StreamValidation {
        if (response.code != 200 && response.code != 206) {
            return StreamValidation.Invalid(StreamRejection.HTTP_STATUS, response.code)
        }
        val body = response.body
        if (body.isEmpty()) return StreamValidation.Invalid(StreamRejection.EMPTY_BODY, response.code)
        val mimeType = response.header("Content-Type").orEmpty().substringBefore(';').trim().lowercase(Locale.ROOT)
        if (mimeType !in acceptedMimeTypes) return StreamValidation.Invalid(StreamRejection.NOT_AUDIO, response.code)
        if (!startsWithFileType(body)) return StreamValidation.Invalid(StreamRejection.UNSUPPORTED_CONTAINER, response.code)
        val totalBytes = totalLength(response)
        if (totalBytes == null || totalBytes <= 0L || durationSeconds <= 0) {
            return StreamValidation.Invalid(StreamRejection.UNKNOWN_LENGTH, response.code)
        }
        val estimatedKbps = (totalBytes * 8.0 / durationSeconds / 1000.0).roundToInt()
        if (!bitrateMatches(estimatedKbps, tier)) {
            return StreamValidation.Invalid(StreamRejection.BITRATE_MISMATCH, response.code)
        }
        return StreamValidation.Valid(
            mimeType = "audio/mp4",
            container = "mp4",
            codec = if (contains(body, aacSampleEntry)) "mp4a" else "",
            contentLength = totalBytes,
            estimatedKbps = estimatedKbps
        )
    }

    fun bitrateMatches(estimatedKbps: Int, tier: AudioQualityTier): Boolean {
        val lower = tier.kbps * LOWER_BITRATE_RATIO
        val upper = tier.kbps * UPPER_BITRATE_RATIO + UPPER_BITRATE_SLACK_KBPS
        return estimatedKbps >= lower && estimatedKbps <= upper
    }

    private fun totalLength(response: ProviderHttpResponse): Long? {
        response.header("Content-Range")
            ?.let { contentRangeTotal.find(it)?.groupValues?.getOrNull(1)?.toLongOrNull() }
            ?.let { return it }
        if (response.code != 200) return null
        return response.header("Content-Length")?.trim()?.toLongOrNull()
    }

    private fun startsWithFileType(body: ByteArray): Boolean {
        if (body.size < 8) return false
        return (0 until 4).all { index -> body[4 + index] == fileTypeBox[index] }
    }

    private fun contains(body: ByteArray, pattern: ByteArray): Boolean {
        if (body.size < pattern.size) return false
        for (start in 0..body.size - pattern.size) {
            var matched = true
            for (offset in pattern.indices) {
                if (body[start + offset] != pattern[offset]) {
                    matched = false
                    break
                }
            }
            if (matched) return true
        }
        return false
    }
}
