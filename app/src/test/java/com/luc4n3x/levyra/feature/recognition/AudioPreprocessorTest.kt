package com.luc4n3x.levyra.feature.recognition

import kotlin.math.abs
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioPreprocessorTest {

    @Test
    fun downmixAveragesInterleavedStereoChannels() {
        val interleaved = shortArrayOf(1000, -1000, 500, 1500, -300, -300)

        val mono = AudioPreprocessor.downmixStereoToMono(interleaved)

        assertEquals(3, mono.size)
        assertEquals(0, mono[0].toInt())
        assertEquals(1000, mono[1].toInt())
        assertEquals(-300, mono[2].toInt())
    }

    @Test
    fun downmixRejectsOddLengthInput() {
        assertThrows(IllegalArgumentException::class.java) {
            AudioPreprocessor.downmixStereoToMono(shortArrayOf(1, 2, 3))
        }
    }

    @Test
    fun resampleProducesLengthMatchingRateRatio() {
        val input = ShortArray(44_100)

        val resampled = AudioPreprocessor.resampleMono(input, inputRateHz = 44_100, targetRateHz = 16_000)

        assertEquals(16_000, resampled.size)
    }

    @Test
    fun resampleUpsamplingDoublesLength() {
        val input = ShortArray(8_000)

        val resampled = AudioPreprocessor.resampleMono(input, inputRateHz = 8_000, targetRateHz = 16_000)

        assertEquals(16_000, resampled.size)
    }

    @Test
    fun resamplePreservesEndpointsForRamp() {
        val input = ShortArray(100) { index -> index.toShort() }

        val resampled = AudioPreprocessor.resampleMono(input, inputRateHz = 100, targetRateHz = 50)

        assertEquals(50, resampled.size)
        assertEquals(0, resampled.first().toInt())
        assertEquals(99, resampled.last().toInt())
    }

    @Test
    fun resampleWithMatchingRatesReturnsEquivalentCopy() {
        val input = shortArrayOf(10, 20, 30)

        val resampled = AudioPreprocessor.resampleMono(input, inputRateHz = 16_000, targetRateHz = 16_000)

        assertEquals(input.toList(), resampled.toList())
    }

    @Test
    fun resampleHandlesEmptyAndSingleSampleInput() {
        assertEquals(0, AudioPreprocessor.resampleMono(ShortArray(0), 44_100, 16_000).size)

        val single = AudioPreprocessor.resampleMono(shortArrayOf(500), 44_100, 16_000)
        assertTrue(single.isNotEmpty())
        single.forEach { assertEquals(500, it.toInt()) }
    }

    @Test
    fun resampleRejectsNonPositiveRates() {
        assertThrows(IllegalArgumentException::class.java) {
            AudioPreprocessor.resampleMono(shortArrayOf(1, 2), inputRateHz = 0, targetRateHz = 16_000)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AudioPreprocessor.resampleMono(shortArrayOf(1, 2), inputRateHz = 16_000, targetRateHz = -1)
        }
    }

    @Test
    fun normalizeScalesPeakToTargetRatio() {
        val input = shortArrayOf(100, -200, 150, -50)

        val normalized = AudioPreprocessor.normalizeAmplitude(input, targetPeakRatio = 0.9)

        val expectedPeak = (Short.MAX_VALUE * 0.9).roundToInt()
        val actualPeak = normalized.maxOf { abs(it.toInt()) }
        assertTrue(abs(actualPeak - expectedPeak) <= 1)
        normalized.forEach { sample -> assertTrue(abs(sample.toInt()) <= Short.MAX_VALUE) }
    }

    @Test
    fun normalizeLeavesSilenceUnchanged() {
        val silence = ShortArray(10)

        val normalized = AudioPreprocessor.normalizeAmplitude(silence)

        assertEquals(silence.toList(), normalized.toList())
    }

    @Test
    fun normalizeRejectsInvalidTargetRatio() {
        assertThrows(IllegalArgumentException::class.java) {
            AudioPreprocessor.normalizeAmplitude(shortArrayOf(1, 2), targetPeakRatio = 0.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AudioPreprocessor.normalizeAmplitude(shortArrayOf(1, 2), targetPeakRatio = 1.5)
        }
    }
}
