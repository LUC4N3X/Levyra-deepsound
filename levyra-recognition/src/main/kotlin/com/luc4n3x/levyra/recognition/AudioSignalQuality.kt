package com.luc4n3x.levyra.recognition

import kotlin.math.abs
import kotlin.math.sqrt

class AudioSignalQuality(
    val sampleCount: Int,
    val rms: Double,
    val peakAmplitude: Int,
    val nonZeroRatio: Double,
    val clippingRatio: Double
) {
    val isSilent: Boolean = peakAmplitude == 0 || nonZeroRatio < 0.001
    val isTooQuiet: Boolean = !isSilent && rms < SILENCE_RMS_FLOOR

    companion object {
        const val SILENCE_RMS_FLOOR: Double = 60.0
        const val CLIPPING_THRESHOLD: Int = 32_000

        fun analyze(pcm16Mono: ShortArray): AudioSignalQuality {
            if (pcm16Mono.isEmpty()) {
                return AudioSignalQuality(
                    sampleCount = 0,
                    rms = 0.0,
                    peakAmplitude = 0,
                    nonZeroRatio = 0.0,
                    clippingRatio = 0.0
                )
            }

            var sumOfSquares = 0.0
            var peak = 0
            var nonZeroCount = 0
            var clippingCount = 0
            for (sample in pcm16Mono) {
                val value = sample.toInt()
                val magnitude = abs(value).coerceAtMost(32_767)
                sumOfSquares += value.toDouble() * value.toDouble()
                if (magnitude > peak) peak = magnitude
                if (value != 0) nonZeroCount++
                if (magnitude >= CLIPPING_THRESHOLD) clippingCount++
            }

            return AudioSignalQuality(
                sampleCount = pcm16Mono.size,
                rms = sqrt(sumOfSquares / pcm16Mono.size),
                peakAmplitude = peak,
                nonZeroRatio = nonZeroCount.toDouble() / pcm16Mono.size,
                clippingRatio = clippingCount.toDouble() / pcm16Mono.size
            )
        }
    }
}
