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
        val codecTokens = codecs
            .flatMap { it.split(',') }
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter(String::isNotBlank)
        val hasFlac = codecTokens.any { it in FLAC_CODECS }
        val hasEac3 = codecTokens.any(::isEac3Codec)
        val hasOtherLossyAudio = codecTokens.any(::isKnownLossyAudioCodec)
        val hasAtmosSignal = codecTokens.any { it.contains("ec+3") } ||
            xml.contains("EC3_ExtensionType", ignoreCase = true) && xml.contains("JOC", ignoreCase = true)
        val isAtmos = hasEac3 && hasAtmosSignal

        if (!hasFlac && !hasEac3) {
            return LosslessStreamValidation.Invalid(StreamRejection.UNSUPPORTED_CODEC, response.code)
        }
        if (hasFlac && (hasEac3 || hasOtherLossyAudio)) {
            return LosslessStreamValidation.Invalid(StreamRejection.UNSUPPORTED_CODEC, response.code)
        }
        if (hasEac3 && hasOtherLossyAudio) {
            return LosslessStreamValidation.Invalid(StreamRejection.UNSUPPORTED_CODEC, response.code)
        }
        if (requested == AudioQualityPreference.DOLBY_ATMOS && !isAtmos) {
            return LosslessStreamValidation.Invalid(StreamRejection.FAKE_ATMOS, response.code)
        }
        if (requested != AudioQualityPreference.DOLBY_ATMOS && hasEac3) {
            return LosslessStreamValidation.Invalid(StreamRejection.UNSUPPORTED_CODEC, response.code)
        }

        val sampleRate = uniqueDashInt(dashSampleRate, xml)
        val bitDepth = uniqueDashInt(dashBitDepth, xml)
        val channels = uniqueDashInt(dashChannels, xml)
        val bitrate = uniqueDashLong(dashBandwidth, xml)
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
                        clean.contains("eac3") || clean.contains("flac") || clean.contains("fla1")
                }.orEmpty(),
                contentLength = 0L,
                estimatedKbps = bitrate,
                sampleRateHz = sampleRate,
                bitDepth = bitDepth,
                channels = channels,
                isLossless = hasFlac,
                isSpatial = isAtmos,
                isAtmos = isAtmos,
                deliveryMethod = PlaybackDeliveryMethod.DASH
            )
        )
    }

    private fun uniqueDashInt(regex: Regex, xml: String): Int = regex.findAll(xml)
        .mapNotNull { it.groupValues[1].toIntOrNull()?.takeIf { value -> value > 0 } }
        .distinct()
        .toList()
        .singleOrNull()
        ?: 0

    private fun uniqueDashLong(regex: Regex, xml: String): Long? = regex.findAll(xml)
        .mapNotNull { it.groupValues[1].toLongOrNull()?.takeIf { value -> value > 0L } }
        .distinct()
        .toList()
        .singleOrNull()

    private fun isEac3Codec(codec: String): Boolean =
        codec == "ec-3" || codec == "ec+3" || codec == "eac3" || codec == "ec3"

    private fun isKnownLossyAudioCodec(codec: String): Boolean =
        codec.startsWith("mp4a") || codec == "aac" || codec.startsWith("opus") ||
            codec.startsWith("vorbis") || codec == "ac-3" || codec == "ac3"

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
        val rawChannels = (packed ushr 41) and 0x7L
        val channels = (rawChannels + 1L).toInt()
        val rawBitDepth = (packed ushr 36) and 0x1fL
        val bitDepth = (rawBitDepth + 1L).toInt()
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
