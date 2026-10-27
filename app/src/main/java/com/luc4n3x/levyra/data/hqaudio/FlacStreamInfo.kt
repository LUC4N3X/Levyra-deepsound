package com.luc4n3x.levyra.data.hqaudio

import kotlin.math.abs
import kotlin.math.max

internal data class FlacStreamInfo(
    val sampleRateHz: Int,
    val channels: Int,
    val bitsPerSample: Int,
    val totalSamples: Long
) {
    val durationSeconds: Double
        get() = if (sampleRateHz > 0) totalSamples.toDouble() / sampleRateHz else 0.0

    fun durationMatches(expectedSeconds: Int): Boolean {
        if (totalSamples <= 0L) return true
        val tolerance = max(MIN_DURATION_TOLERANCE_SECONDS, expectedSeconds * DURATION_TOLERANCE_RATIO)
        return abs(durationSeconds - expectedSeconds) <= tolerance
    }

    companion object {
        private const val MIN_DURATION_TOLERANCE_SECONDS = 5.0
        private const val DURATION_TOLERANCE_RATIO = 0.03
        private const val HEADER_BYTES = 42
        private const val STREAMINFO_TYPE = 0
        private const val STREAMINFO_LENGTH = 34
        private const val MAX_SAMPLE_RATE_HZ = 655_350
        private val MAGIC = byteArrayOf(0x66, 0x4C, 0x61, 0x43)

        fun parse(body: ByteArray): FlacStreamInfo? {
            if (body.size < HEADER_BYTES) return null
            if ((0 until 4).any { body[it] != MAGIC[it] }) return null
            if (u(body, 4) and 0x7F != STREAMINFO_TYPE) return null
            val length = (u(body, 5) shl 16) or (u(body, 6) shl 8) or u(body, 7)
            if (length != STREAMINFO_LENGTH) return null
            val sampleRate = (u(body, 18) shl 12) or (u(body, 19) shl 4) or (u(body, 20) shr 4)
            val channels = ((u(body, 20) shr 1) and 0x07) + 1
            val bitsPerSample = (((u(body, 20) and 0x01) shl 4) or (u(body, 21) shr 4)) + 1
            val totalSamples = ((u(body, 21) and 0x0F).toLong() shl 32) or
                (u(body, 22).toLong() shl 24) or
                (u(body, 23).toLong() shl 16) or
                (u(body, 24).toLong() shl 8) or
                u(body, 25).toLong()
            if (sampleRate <= 0 || sampleRate > MAX_SAMPLE_RATE_HZ) return null
            if (bitsPerSample < 4) return null
            return FlacStreamInfo(sampleRate, channels, bitsPerSample, totalSamples)
        }

        private fun u(body: ByteArray, index: Int): Int = body[index].toInt() and 0xFF
    }
}
