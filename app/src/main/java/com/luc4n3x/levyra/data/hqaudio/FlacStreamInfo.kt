package com.luc4n3x.levyra.data.hqaudio

internal data class FlacStreamInfo(
    val sampleRateHz: Int,
    val channels: Int,
    val bitDepth: Int,
    val totalSamples: Long
) {
    val plausible: Boolean
        get() = sampleRateHz in MIN_SAMPLE_RATE_HZ..MAX_SAMPLE_RATE_HZ &&
            channels in 1..MAX_CHANNELS &&
            bitDepth in MIN_BIT_DEPTH..MAX_BIT_DEPTH

    val durationSeconds: Double?
        get() = if (totalSamples > 0L && sampleRateHz > 0) totalSamples.toDouble() / sampleRateHz else null

    companion object {
        private const val MIN_SAMPLE_RATE_HZ = 8_000
        private const val MAX_SAMPLE_RATE_HZ = 768_000
        private const val MAX_CHANNELS = 8
        private const val MIN_BIT_DEPTH = 8
        private const val MAX_BIT_DEPTH = 32
        private const val ID3_HEADER_BYTES = 10
        private const val ID3_FOOTER_FLAG = 0x10
        private const val STREAMINFO_TYPE = 0
        private const val STREAMINFO_BYTES = 34
        private val flacMarker = "fLaC".toByteArray(Charsets.US_ASCII)

        fun parse(body: ByteArray): FlacStreamInfo? {
            val start = markerOffset(body) ?: return null
            val header = start + flacMarker.size
            if (body.size < header + 4 + STREAMINFO_BYTES) return null
            if ((u8(body, header) and 0x7F) != STREAMINFO_TYPE) return null
            val length = (u8(body, header + 1) shl 16) or (u8(body, header + 2) shl 8) or u8(body, header + 3)
            if (length < STREAMINFO_BYTES) return null
            val info = header + 4
            val sampleRate = (u8(body, info + 10) shl 12) or (u8(body, info + 11) shl 4) or (u8(body, info + 12) ushr 4)
            val channels = ((u8(body, info + 12) ushr 1) and 0x07) + 1
            val bitDepth = (((u8(body, info + 12) and 0x01) shl 4) or (u8(body, info + 13) ushr 4)) + 1
            val totalSamples = ((u8(body, info + 13) and 0x0F).toLong() shl 32) or
                (u8(body, info + 14).toLong() shl 24) or
                (u8(body, info + 15).toLong() shl 16) or
                (u8(body, info + 16).toLong() shl 8) or
                u8(body, info + 17).toLong()
            return FlacStreamInfo(sampleRate, channels, bitDepth, totalSamples)
        }

        private fun markerOffset(body: ByteArray): Int? {
            var offset = 0
            if (body.size >= ID3_HEADER_BYTES && body[0] == 'I'.code.toByte() && body[1] == 'D'.code.toByte() &&
                body[2] == '3'.code.toByte()
            ) {
                val size = ((u8(body, 6) and 0x7F) shl 21) or ((u8(body, 7) and 0x7F) shl 14) or
                    ((u8(body, 8) and 0x7F) shl 7) or (u8(body, 9) and 0x7F)
                val footer = if ((u8(body, 5) and ID3_FOOTER_FLAG) != 0) ID3_HEADER_BYTES else 0
                offset = ID3_HEADER_BYTES + size + footer
            }
            if (body.size < offset + flacMarker.size) return null
            return offset.takeIf { flacMarker.indices.all { index -> body[offset + index] == flacMarker[index] } }
        }

        private fun u8(body: ByteArray, index: Int): Int = body[index].toInt() and 0xFF
    }
}
