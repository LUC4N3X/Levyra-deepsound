package com.luc4n3x.levyra.player.waveseek

import kotlin.math.abs

internal class WaveSeekCapture(
    private val durationMs: Long
) {
    companion object {
        private const val MAX_CONTINUOUS_GAP_MS = 1_500L
        private const val MINIMUM_COVERAGE = 0.985f
    }

    private val peaks = FloatArray(WaveSeekEnvelope.BUCKETS)
    private val observed = BooleanArray(WaveSeekEnvelope.BUCKETS)
    private var observedCount = 0
    private var lastPositionMs: Long? = null
    private var lastAmplitude = 0f

    fun record(positionMs: Long, waveform: FloatArray) {
        if (durationMs <= 0L || positionMs < 0L || waveform.isEmpty()) return
        var amplitude = 0f
        var hasFiniteSample = false
        waveform.forEach { sample ->
            if (sample.isFinite()) {
                hasFiniteSample = true
                amplitude = maxOf(amplitude, abs(sample).coerceIn(0f, 1f))
            }
        }
        if (!hasFiniteSample) return

        val position = positionMs.coerceAtMost((durationMs - 1L).coerceAtLeast(0L))
        val currentBucket = bucketFor(position)
        val previousPosition = lastPositionMs
        if (
            previousPosition != null &&
            position >= previousPosition &&
            position - previousPosition <= MAX_CONTINUOUS_GAP_MS
        ) {
            val previousBucket = bucketFor(previousPosition)
            val span = (currentBucket - previousBucket).coerceAtLeast(0)
            for (offset in 0..span) {
                val fraction = if (span == 0) 1f else offset.toFloat() / span.toFloat()
                val interpolated = lastAmplitude + (amplitude - lastAmplitude) * fraction
                recordBucket(previousBucket + offset, interpolated)
            }
        } else {
            recordBucket(currentBucket, amplitude)
        }

        lastPositionMs = position
        lastAmplitude = amplitude
    }

    fun coverageFraction(): Float =
        observedCount.toFloat() / WaveSeekEnvelope.BUCKETS.toFloat()

    fun snapshotIfReady(): WaveSeekEnvelope? {
        if (coverageFraction() < MINIMUM_COVERAGE) return null
        return WaveSeekEnvelope.measured(filledPeaks())
    }

    private fun bucketFor(positionMs: Long): Int {
        if (durationMs <= 1L) return 0
        val fraction = positionMs.toDouble() / durationMs.toDouble()
        return (fraction * WaveSeekEnvelope.BUCKETS)
            .toInt()
            .coerceIn(0, WaveSeekEnvelope.BUCKETS - 1)
    }

    private fun recordBucket(index: Int, amplitude: Float) {
        if (index !in peaks.indices) return
        peaks[index] = maxOf(peaks[index], amplitude.coerceIn(0f, 1f))
        if (!observed[index]) {
            observed[index] = true
            observedCount++
        }
    }

    private fun filledPeaks(): FloatArray {
        val result = peaks.copyOf()
        var previousObserved = -1
        var index = 0
        while (index < result.size) {
            if (observed[index]) {
                previousObserved = index
                index++
                continue
            }
            val gapStart = index
            while (index < result.size && !observed[index]) index++
            val nextObserved = index.takeIf { it < result.size } ?: -1
            for (gapIndex in gapStart until index) {
                result[gapIndex] = when {
                    previousObserved >= 0 && nextObserved >= 0 -> {
                        val fraction = (gapIndex - previousObserved).toFloat() /
                            (nextObserved - previousObserved).toFloat()
                        result[previousObserved] +
                            (result[nextObserved] - result[previousObserved]) * fraction
                    }
                    previousObserved >= 0 -> result[previousObserved]
                    nextObserved >= 0 -> result[nextObserved]
                    else -> 0f
                }
            }
        }
        return result
    }
}
