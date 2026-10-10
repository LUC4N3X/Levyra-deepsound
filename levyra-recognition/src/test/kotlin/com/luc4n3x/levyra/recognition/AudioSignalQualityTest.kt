package com.luc4n3x.levyra.recognition

import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioSignalQualityTest {
    @Test
    fun pureSilenceIsSilent() {
        val quality = AudioSignalQuality.analyze(ShortArray(16_000))

        assertTrue(quality.isSilent)
        assertEquals(0.0, quality.rms, 1e-9)
        assertEquals(0.0, quality.nonZeroRatio, 1e-9)
    }

    @Test
    fun fullScaleSquareWaveIsNotTooQuietAndClips() {
        val samples = ShortArray(16_000) { index ->
            if (index % 2 == 0) Short.MAX_VALUE else Short.MIN_VALUE
        }
        val quality = AudioSignalQuality.analyze(samples)

        assertFalse(quality.isTooQuiet)
        assertEquals(32_767, quality.peakAmplitude)
        assertEquals(1.0, quality.clippingRatio, 1e-9)
    }

    @Test
    fun quietSineIsTooQuiet() {
        val samples = sineWave(amplitude = 30.0, sampleCount = 16_000)
        val quality = AudioSignalQuality.analyze(samples)

        assertFalse(quality.isSilent)
        assertTrue(quality.isTooQuiet)
    }

    @Test
    fun normalSineIsNeitherSilentNorTooQuiet() {
        val samples = sineWave(amplitude = 8_000.0, sampleCount = 16_000)
        val quality = AudioSignalQuality.analyze(samples)

        assertFalse(quality.isSilent)
        assertFalse(quality.isTooQuiet)
        assertEquals(0.0, quality.clippingRatio, 1e-9)
    }

    @Test
    fun emptyArrayIsSilent() {
        val quality = AudioSignalQuality.analyze(ShortArray(0))

        assertEquals(0, quality.sampleCount)
        assertTrue(quality.isSilent)
    }

    @Test
    fun shortMinValueDoesNotOverflowPeakAmplitude() {
        val samples = ShortArray(4) { Short.MIN_VALUE }
        val quality = AudioSignalQuality.analyze(samples)

        assertEquals(32_767, quality.peakAmplitude)
    }

    @Test
    fun rmsOfConstantAmplitudeSignalEqualsThatAmplitude() {
        val amplitude = 12_345.0
        val samples = ShortArray(20_000) { index ->
            if (index % 2 == 0) amplitude.toInt().toShort() else (-amplitude).toInt().toShort()
        }
        val quality = AudioSignalQuality.analyze(samples)

        assertEquals(amplitude, quality.rms, 1e-6)
    }

    private fun sineWave(amplitude: Double, sampleCount: Int): ShortArray =
        ShortArray(sampleCount) { index ->
            val time = index.toDouble() / AcousticFingerprintEngine.SAMPLE_RATE_HZ
            (amplitude * sin(2.0 * PI * 440.0 * time)).toInt().toShort()
        }
}
