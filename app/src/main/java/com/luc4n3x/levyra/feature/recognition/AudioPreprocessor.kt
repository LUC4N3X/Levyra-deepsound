package com.luc4n3x.levyra.feature.recognition

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

object AudioPreprocessor {
    const val DEFAULT_NORMALIZATION_PEAK_RATIO = 0.9

    fun downmixStereoToMono(interleavedSamples: ShortArray): ShortArray {
        require(interleavedSamples.size % 2 == 0) {
            "Interleaved stereo PCM16 must contain complete left/right sample pairs"
        }
        val frameCount = interleavedSamples.size / 2
        val mono = ShortArray(frameCount)
        for (frame in 0 until frameCount) {
            val left = interleavedSamples[frame * 2].toInt()
            val right = interleavedSamples[frame * 2 + 1].toInt()
            mono[frame] = ((left + right) / 2).toShort()
        }
        return mono
    }

    fun resampleMono(samples: ShortArray, inputRateHz: Int, targetRateHz: Int): ShortArray {
        require(inputRateHz > 0) { "inputRateHz must be positive" }
        require(targetRateHz > 0) { "targetRateHz must be positive" }
        if (samples.isEmpty()) return ShortArray(0)
        if (inputRateHz == targetRateHz) return samples.copyOf()

        val outputLength = max(1, (samples.size.toDouble() * targetRateHz / inputRateHz).roundToInt())
        if (samples.size == 1) {
            return ShortArray(outputLength) { samples[0] }
        }

        val lastInputIndex = samples.size - 1
        val output = ShortArray(outputLength)
        for (index in 0 until outputLength) {
            val position = if (outputLength == 1) {
                0.0
            } else {
                index.toDouble() * lastInputIndex / (outputLength - 1)
            }
            val lowerIndex = position.toInt().coerceIn(0, lastInputIndex)
            val upperIndex = (lowerIndex + 1).coerceAtMost(lastInputIndex)
            val fraction = position - lowerIndex
            val lower = samples[lowerIndex].toInt()
            val upper = samples[upperIndex].toInt()
            val interpolated = lower + (upper - lower) * fraction
            output[index] = interpolated.roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        return output
    }

    fun normalizeAmplitude(
        samples: ShortArray,
        targetPeakRatio: Double = DEFAULT_NORMALIZATION_PEAK_RATIO
    ): ShortArray {
        require(targetPeakRatio > 0.0 && targetPeakRatio <= 1.0) {
            "targetPeakRatio must be within (0, 1]"
        }
        if (samples.isEmpty()) return ShortArray(0)
        val peak = samples.fold(0) { acc, sample -> max(acc, abs(sample.toInt())) }
        if (peak == 0) return samples.copyOf()

        val targetPeak = Short.MAX_VALUE * targetPeakRatio
        val gain = targetPeak / peak
        return ShortArray(samples.size) { index ->
            (samples[index] * gain).roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
    }
}
