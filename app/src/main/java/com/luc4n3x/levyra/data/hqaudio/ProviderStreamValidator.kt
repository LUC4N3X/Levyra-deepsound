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
    BITRATE_MISMATCH,
    INVALID_METADATA,
    PREVIEW,
    TRUNCATED,
    DURATION_MISMATCH,
    EXPIRED,
    UNSAFE_DESTINATION
}

sealed interface StreamValidation {
    data class Valid(
        val mimeType: String,
        val container: String,
        val codec: String,
        val contentLength: Long,
        val estimatedKbps: Int,
        val quality: HighQualityStreamQuality
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
    private const val MIN_FLAC_COMPRESSION_RATIO = 0.12
    private const val MAX_FLAC_EXPANSION_RATIO = 1.05
    private const val PREVIEW_BITRATE_RATIO = 0.5
    private const val DURATION_TOLERANCE_SECONDS = 6.0
    private const val PREVIEW_MAX_SECONDS = 35.0
    private val acceptedFlacMimeTypes = setOf(
        "audio/flac",
        "audio/x-flac",
        "application/flac",
        "application/octet-stream",
        "binary/octet-stream"
    )
    private val acceptedMp3MimeTypes = setOf(
        "audio/mpeg",
        "audio/mp3",
        "audio/mpeg3",
        "application/octet-stream",
        "binary/octet-stream"
    )

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
            estimatedKbps = estimatedKbps,
            quality = HighQualityStreamQuality.lossy(HighQualityCodec.AAC, tier.kbps, estimatedKbps)
        )
    }

    fun validateFlac(response: ProviderHttpResponse, durationSeconds: Int): StreamValidation {
        rejectNonMedia(response, acceptedFlacMimeTypes)?.let { return it }
        val streamInfo = FlacStreamInfo.parse(response.body)
            ?: return StreamValidation.Invalid(StreamRejection.UNSUPPORTED_CONTAINER, response.code)
        if (!streamInfo.plausible) return StreamValidation.Invalid(StreamRejection.INVALID_METADATA, response.code)
        val totalBytes = totalLength(response)
        if (totalBytes == null || totalBytes <= 0L || durationSeconds <= 0) {
            return StreamValidation.Invalid(StreamRejection.UNKNOWN_LENGTH, response.code)
        }
        val headerSeconds = streamInfo.durationSeconds
        if (headerSeconds != null) {
            durationRejection(headerSeconds, durationSeconds)?.let { return StreamValidation.Invalid(it, response.code) }
        }
        val playedSeconds = headerSeconds ?: durationSeconds.toDouble()
        val estimatedKbps = (totalBytes * 8.0 / playedSeconds / 1000.0).roundToInt()
        val pcmKbps = streamInfo.sampleRateHz.toLong() * streamInfo.bitDepth * streamInfo.channels / 1000.0
        if (estimatedKbps < pcmKbps * MIN_FLAC_COMPRESSION_RATIO) {
            return StreamValidation.Invalid(StreamRejection.TRUNCATED, response.code)
        }
        if (estimatedKbps > pcmKbps * MAX_FLAC_EXPANSION_RATIO) {
            return StreamValidation.Invalid(StreamRejection.BITRATE_MISMATCH, response.code)
        }
        return StreamValidation.Valid(
            mimeType = HighQualityCodec.FLAC.mimeType,
            container = HighQualityCodec.FLAC.container,
            codec = "flac",
            contentLength = totalBytes,
            estimatedKbps = estimatedKbps,
            quality = HighQualityStreamQuality(
                codec = HighQualityCodec.FLAC,
                nominalKbps = null,
                estimatedKbps = estimatedKbps,
                sampleRateHz = streamInfo.sampleRateHz,
                bitDepth = streamInfo.bitDepth,
                channels = streamInfo.channels
            )
        )
    }

    fun validateMp3(response: ProviderHttpResponse, nominalKbps: Int, durationSeconds: Int): StreamValidation {
        rejectNonMedia(response, acceptedMp3MimeTypes)?.let { return it }
        if (!startsLikeMp3(response.body)) {
            return StreamValidation.Invalid(StreamRejection.UNSUPPORTED_CONTAINER, response.code)
        }
        val totalBytes = totalLength(response)
        if (totalBytes == null || totalBytes <= 0L || durationSeconds <= 0) {
            return StreamValidation.Invalid(StreamRejection.UNKNOWN_LENGTH, response.code)
        }
        val estimatedKbps = (totalBytes * 8.0 / durationSeconds / 1000.0).roundToInt()
        if (estimatedKbps < nominalKbps * PREVIEW_BITRATE_RATIO) {
            return StreamValidation.Invalid(StreamRejection.PREVIEW, response.code)
        }
        if (!bitrateMatches(estimatedKbps, nominalKbps)) {
            return StreamValidation.Invalid(StreamRejection.BITRATE_MISMATCH, response.code)
        }
        return StreamValidation.Valid(
            mimeType = HighQualityCodec.MP3.mimeType,
            container = HighQualityCodec.MP3.container,
            codec = "mp3",
            contentLength = totalBytes,
            estimatedKbps = estimatedKbps,
            quality = HighQualityStreamQuality.lossy(HighQualityCodec.MP3, nominalKbps, estimatedKbps)
        )
    }

    internal fun durationRejection(actualSeconds: Double, expectedSeconds: Int): StreamRejection? {
        val delta = actualSeconds - expectedSeconds
        return when {
            delta < -DURATION_TOLERANCE_SECONDS && actualSeconds <= PREVIEW_MAX_SECONDS -> StreamRejection.PREVIEW
            delta < -DURATION_TOLERANCE_SECONDS -> StreamRejection.TRUNCATED
            delta > DURATION_TOLERANCE_SECONDS -> StreamRejection.DURATION_MISMATCH
            else -> null
        }
    }

    private fun rejectNonMedia(response: ProviderHttpResponse, accepted: Set<String>): StreamValidation.Invalid? {
        if (response.code != 200 && response.code != 206) {
            return StreamValidation.Invalid(StreamRejection.HTTP_STATUS, response.code)
        }
        if (response.body.isEmpty()) return StreamValidation.Invalid(StreamRejection.EMPTY_BODY, response.code)
        val mimeType = response.header("Content-Type").orEmpty().substringBefore(';').trim().lowercase(Locale.ROOT)
        if (mimeType !in accepted) return StreamValidation.Invalid(StreamRejection.NOT_AUDIO, response.code)
        return null
    }

    private fun startsLikeMp3(body: ByteArray): Boolean {
        if (body.size < 4) return false
        if (body[0] == 'I'.code.toByte() && body[1] == 'D'.code.toByte() && body[2] == '3'.code.toByte()) return true
        return (body[0].toInt() and 0xFF) == 0xFF && (body[1].toInt() and 0xE0) == 0xE0
    }

    fun bitrateMatches(estimatedKbps: Int, tier: AudioQualityTier): Boolean = bitrateMatches(estimatedKbps, tier.kbps)

    fun bitrateMatches(estimatedKbps: Int, nominalKbps: Int): Boolean {
        val lower = nominalKbps * LOWER_BITRATE_RATIO
        val upper = nominalKbps * UPPER_BITRATE_RATIO + UPPER_BITRATE_SLACK_KBPS
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
