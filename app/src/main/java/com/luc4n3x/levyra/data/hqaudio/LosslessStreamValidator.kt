package com.luc4n3x.levyra.data.hqaudio

import com.luc4n3x.levyra.domain.AudioQualityPreference
import com.luc4n3x.levyra.domain.PlaybackDeliveryMethod
import java.util.Locale
import kotlin.math.roundToInt

internal data class VerifiedAudioFormat(
    val mimeType: String,
    val container: String,
    val codec: String,
    val contentLength: Long,
    val estimatedKbps: Int,
    val sampleRateHz: Int,
    val bitDepth: Int,
    val channels: Int,
    val isLossless: Boolean,
    val isSpatial: Boolean,
    val isAtmos: Boolean,
    val deliveryMethod: PlaybackDeliveryMethod
)

internal sealed interface LosslessStreamValidation {
    data class Valid(val format: VerifiedAudioFormat) : LosslessStreamValidation
    data class Invalid(val rejection: StreamRejection, val statusCode: Int = 0) : LosslessStreamValidation
}

internal object LosslessStreamValidator {
    const val MANIFEST_PROBE_BYTES = 131_072

    private val contentRangeTotal = Regex("bytes\\s+\\d+-\\d+/(\\d+)", RegexOption.IGNORE_CASE)
    private val dashCodec = Regex("codecs\\s*=\\s*[\"']([^\"']+)", RegexOption.IGNORE_CASE)
    private val dashSampleRate = Regex("audioSamplingRate\\s*=\\s*[\"'](\\d+)", RegexOption.IGNORE_CASE)
    private val dashBitDepth = Regex("(?:bitDepth|bitsPerSample)\\s*=\\s*[\"'](\\d+)", RegexOption.IGNORE_CASE)
    private val dashBandwidth = Regex("bandwidth\\s*=\\s*[\"'](\\d+)", RegexOption.IGNORE_CASE)
    private val dashChannels = Regex(
        "AudioChannelConfiguration[^>]+value\\s*=\\s*[\"'](\\d+)",
        RegexOption.IGNORE_CASE
    )

    fun validate(
        response: ProviderHttpResponse,
        requested: AudioQualityPreference,
        durationSeconds: Int
    ): LosslessStreamValidation {
        if (response.code != 200 && response.code != 206) {
            return LosslessStreamValidation.Invalid(StreamRejection.HTTP_STATUS, response.code)
        }
        if (response.body.isEmpty()) return LosslessStreamValidation.Invalid(StreamRejection.EMPTY_BODY, response.code)
        val mime = response.header("Content-Type").orEmpty().substringBefore(';').trim().lowercase(Locale.ROOT)
        return when {
            response.body.startsWithFlac() -> validateFlac(response, mime, requested, durationSeconds)
            response.body.looksLikeDash() -> validateDash(response, requested)
            else -> LosslessStreamValidation.Invalid(StreamRejection.UNSUPPORTED_CONTAINER, response.code)
        }
    }

    private fun validateFlac(
        response: ProviderHttpResponse,
        responseMime: String,
        requested: AudioQualityPreference,
        durationSeconds: Int
    ): LosslessStreamValidation {
        if (responseMime.isNotBlank() && responseMime !in FLAC_MIME_TYPES) {
            return LosslessStreamValidation.Invalid(StreamRejection.NOT_AUDIO, response.code)
        }
        val streamInfo = parseFlacStreamInfo(response.body)
            ?: return LosslessStreamValidation.Invalid(StreamRejection.MALFORMED_AUDIO, response.code)
        if (requested == AudioQualityPreference.DOLBY_ATMOS) {
            return LosslessStreamValidation.Invalid(StreamRejection.FAKE_ATMOS, response.code)
        }
        val totalBytes = totalLength(response)
        val estimatedKbps = if (totalBytes > 0L && durationSeconds > 0) {
            (totalBytes * 8.0 / durationSeconds / 1_000.0).roundToInt()
        } else {
            0
        }
        return LosslessStreamValidation.Valid(
            VerifiedAudioFormat(
                mimeType = "audio/flac",
                container = "flac",
                codec = "flac",
                contentLength = totalBytes.coerceAtLeast(0L),
                estimatedKbps = estimatedKbps,
                sampleRateHz = streamInfo.sampleRateHz,
                bitDepth = streamInfo.bitDepth,
                channels = streamInfo.channels,
                isLossless = true,
                isSpatial = false,
                isAtmos = false,
                deliveryMethod = PlaybackDeliveryMethod.PROGRESSIVE
            )
        )
    }

    private fun validateDash(
        response: ProviderHttpResponse,
        requested: AudioQualityPreference
    ): LosslessStreamValidation {
        val xml = response.body.toString(Charsets.UTF_8)
        val codecs = dashCodec.findAll(xml).map { it.groupValues[1] }.toList()
        val normalizedCodecs = codecs.joinToString(",").lowercase(Locale.ROOT)
        val isFlac = normalizedCodecs.split(',').any { it.trim() in FLAC_CODECS }
        val isEac3 = normalizedCodecs.contains("ec-3") || normalizedCodecs.contains("ec+3") ||
            normalizedCodecs.contains("eac3") ||
            normalizedCodecs.split(',').any { it.trim() == "ec3" }
        val hasAtmosSignal = normalizedCodecs.contains("ec+3") ||
            xml.contains("EC3_ExtensionType", ignoreCase = true) && xml.contains("JOC", ignoreCase = true)
        val isAtmos = isEac3 && hasAtmosSignal
        if (!isFlac && !isEac3) {
            return LosslessStreamValidation.Invalid(StreamRejection.UNSUPPORTED_CODEC, response.code)
        }
        if (requested == AudioQualityPreference.DOLBY_ATMOS && !isAtmos) {
            return LosslessStreamValidation.Invalid(StreamRejection.FAKE_ATMOS, response.code)
        }
        val sampleRate = dashSampleRate.findAll(xml).mapNotNull { it.groupValues[1].toIntOrNull() }.maxOrNull() ?: 0
        val bitDepth = dashBitDepth.findAll(xml).mapNotNull { it.groupValues[1].toIntOrNull() }.maxOrNull() ?: 0
        val channels = dashChannels.findAll(xml).mapNotNull { it.groupValues[1].toIntOrNull() }.maxOrNull() ?: 0
        val bitrate = dashBandwidth.findAll(xml)
            .mapNotNull { it.groupValues[1].toLongOrNull() }
            .maxOrNull()
            ?.div(1_000L)
            ?.coerceAtMost(Int.MAX_VALUE.toLong())
            ?.toInt()
            ?: 0
        return LosslessStreamValidation.Valid(
            VerifiedAudioFormat(
                mimeType = "application/dash+xml",
                container = "dash",
                codec = codecs.firstOrNull { codec ->
                    val clean = codec.lowercase(Locale.ROOT)
                    clean.contains("ec-3") || clean.contains("ec+3") ||
                        clean.contains("eac3") || clean.contains("flac")
                }.orEmpty(),
                contentLength = 0L,
                estimatedKbps = bitrate,
                sampleRateHz = sampleRate,
                bitDepth = bitDepth,
                channels = channels,
                isLossless = isFlac,
                isSpatial = isAtmos,
                isAtmos = isAtmos,
                deliveryMethod = PlaybackDeliveryMethod.DASH
            )
        )
    }

    private fun parseFlacStreamInfo(bytes: ByteArray): FlacStreamInfo? {
        if (bytes.size < 26 || !bytes.startsWithFlac()) return null
        val blockType = bytes[4].toInt() and 0x7f
        val blockLength = ((bytes[5].toInt() and 0xff) shl 16) or
            ((bytes[6].toInt() and 0xff) shl 8) or
            (bytes[7].toInt() and 0xff)
        if (blockType != 0 || blockLength < 34 || bytes.size < 8 + blockLength) return null
        var packed = 0L
        for (index in 18..25) packed = (packed shl 8) or (bytes[index].toLong() and 0xffL)
        val sampleRate = ((packed ushr 44) and 0xfffffL).toInt()
        val channels = (((packed ushr 41) and 0x7L) + 1L).toInt()
        val bitDepth = (((packed ushr 36) and 0x1fL) + 1L).toInt()
        if (sampleRate !in 8_000..768_000 || bitDepth !in 4..32 || channels !in 1..8) return null
        return FlacStreamInfo(sampleRate, bitDepth, channels)
    }

    private fun totalLength(response: ProviderHttpResponse): Long {
        response.header("Content-Range")
            ?.let { contentRangeTotal.find(it)?.groupValues?.getOrNull(1)?.toLongOrNull() }
            ?.let { return it }
        if (response.code != 200) return -1L
        return response.header("Content-Length")?.trim()?.toLongOrNull() ?: -1L
    }

    private fun ByteArray.startsWithFlac(): Boolean =
        size >= 4 && this[0] == 'f'.code.toByte() && this[1] == 'L'.code.toByte() &&
            this[2] == 'a'.code.toByte() && this[3] == 'C'.code.toByte()

    private fun ByteArray.looksLikeDash(): Boolean {
        val prefix = copyOfRange(0, minOf(size, 512)).toString(Charsets.UTF_8).trimStart()
        return prefix.startsWith("<?xml", ignoreCase = true) || prefix.startsWith("<MPD", ignoreCase = true)
    }

    private data class FlacStreamInfo(val sampleRateHz: Int, val bitDepth: Int, val channels: Int)

    private val FLAC_MIME_TYPES = setOf("audio/flac", "audio/x-flac", "application/octet-stream", "")
    private val FLAC_CODECS = setOf("flac", "fla1")
}
