package com.luc4n3x.levyra.player.waveseek

import java.util.Base64
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt

internal class WaveSeekEnvelope private constructor(
    private val frozen: FloatArray
) {
    companion object {
        const val BUCKETS = 512
        const val RESTING = 0.12f
        private const val VERSION = 1
        private const val HEADER_BYTES = 3

        fun measured(samples: FloatArray): WaveSeekEnvelope? {
            if (samples.isEmpty()) return null
            val measured = FloatArray(BUCKETS)
            for (index in measured.indices) {
                val start = floor(index.toDouble() * samples.size / BUCKETS).toInt()
                    .coerceIn(0, samples.lastIndex)
                val endExclusive = ceil((index + 1).toDouble() * samples.size / BUCKETS).toInt()
                    .coerceIn(start + 1, samples.size)
                var peak = 0f
                for (sourceIndex in start until endExclusive) {
                    val sample = samples[sourceIndex]
                    if (sample.isFinite()) {
                        peak = maxOf(peak, kotlin.math.abs(sample).coerceIn(0f, 1f))
                    }
                }
                val shaped = RESTING + (1f - RESTING) * sqrt(peak)
                measured[index] = quantize(shaped.coerceIn(RESTING, 1f))
            }
            return WaveSeekEnvelope(measured)
        }

        fun decodeFromString(encoded: String): WaveSeekEnvelope? {
            if (encoded.isBlank()) return null
            return runCatching {
                val bytes = Base64.getDecoder().decode(encoded)
                if (bytes.size != HEADER_BYTES + BUCKETS) return null
                if ((bytes[0].toInt() and 0xff) != VERSION) return null
                val count = ((bytes[1].toInt() and 0xff) shl 8) or (bytes[2].toInt() and 0xff)
                if (count != BUCKETS) return null
                val values = FloatArray(BUCKETS) { index ->
                    (bytes[HEADER_BYTES + index].toInt() and 0xff) / 255f
                }
                WaveSeekEnvelope(values)
            }.getOrNull()
        }

        private fun quantize(value: Float): Float =
            (value * 255f).toInt().coerceIn(0, 255) / 255f
    }

    fun bars(count: Int): FloatArray {
        if (count <= 0) return FloatArray(0)
        if (count == frozen.size) return frozen.copyOf()
        if (count < frozen.size) {
            return FloatArray(count) { index ->
                val start = floor(index.toDouble() * frozen.size / count).toInt()
                    .coerceIn(0, frozen.lastIndex)
                val endExclusive = ceil((index + 1).toDouble() * frozen.size / count).toInt()
                    .coerceIn(start + 1, frozen.size)
                var peak = RESTING
                for (sourceIndex in start until endExclusive) {
                    peak = maxOf(peak, frozen[sourceIndex])
                }
                peak
            }
        }
        return FloatArray(count) { index ->
            if (count == 1) {
                frozen.first()
            } else {
                val position = index.toDouble() * (frozen.size - 1) / (count - 1)
                val left = floor(position).toInt()
                val right = ceil(position).toInt().coerceAtMost(frozen.lastIndex)
                val fraction = (position - left).toFloat()
                frozen[left] + (frozen[right] - frozen[left]) * fraction
            }
        }
    }

    fun encodeToString(): String {
        val bytes = ByteArray(HEADER_BYTES + frozen.size)
        bytes[0] = VERSION.toByte()
        bytes[1] = ((frozen.size ushr 8) and 0xff).toByte()
        bytes[2] = (frozen.size and 0xff).toByte()
        frozen.forEachIndexed { index, value ->
            bytes[HEADER_BYTES + index] = (value.coerceIn(0f, 1f) * 255f).toInt().toByte()
        }
        return Base64.getEncoder().encodeToString(bytes)
    }
}
