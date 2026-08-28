package com.luc4n3x.levyra.recognition

import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FingerprintEscalationTest {
    @Test
    fun ladderHasTwoRungs() {
        assertEquals(2, FingerprintEscalation.LADDER.size)
    }

    @Test
    fun rungZeroMatchesDefaultProfile() {
        val rungZero = FingerprintEscalation.LADDER[0]
        val defaultProfile = FingerprintProfile()

        assertEquals(defaultProfile.durationTargetSeconds, rungZero.durationTargetSeconds, 1e-9)
        assertEquals(defaultProfile.peakTarget, rungZero.peakTarget)
        assertEquals(defaultProfile.hardMaxDurationSeconds, rungZero.hardMaxDurationSeconds, 1e-9)
        assertEquals(defaultProfile.hardPeakLimit, rungZero.hardPeakLimit)
    }

    @Test
    fun rungOneWidensDurationAndPeakTarget() {
        val rungOne = FingerprintEscalation.LADDER[1]

        assertEquals(9.0, rungOne.durationTargetSeconds, 1e-9)
        assertEquals(2_048, rungOne.peakTarget)
    }

    @Test
    fun higherRungProducesStrictlyMoreSignalThanFirstRung() {
        val samples = multiToneSignal(seconds = 12)

        val rungZeroSignature = AcousticFingerprintEngine(FingerprintEscalation.LADDER[0]).fingerprint(samples)
        val rungOneSignature = AcousticFingerprintEngine(FingerprintEscalation.LADDER[1]).fingerprint(samples)

        assertNotNull(rungZeroSignature)
        assertNotNull(rungOneSignature)
        requireNotNull(rungZeroSignature)
        requireNotNull(rungOneSignature)

        assertTrue(rungOneSignature.peakCount > rungZeroSignature.peakCount)
        assertTrue(rungOneSignature.sampleDurationMs > rungZeroSignature.sampleDurationMs)
    }

    private fun multiToneSignal(seconds: Int): ShortArray {
        val sampleRate = AcousticFingerprintEngine.SAMPLE_RATE_HZ
        val durationSeconds = seconds.toDouble()
        val sweeps = arrayOf(
            doubleArrayOf(300.0, 500.0),
            doubleArrayOf(600.0, 1_400.0),
            doubleArrayOf(1_500.0, 3_400.0),
            doubleArrayOf(3_600.0, 5_400.0)
        )
        return ShortArray(sampleRate * seconds) { index ->
            val time = index.toDouble() / sampleRate
            var sum = 0.0
            for ((startHz, endHz) in sweeps) {
                val phase = 2.0 * PI * (startHz * time + (endHz - startHz) * time * time / (2.0 * durationSeconds))
                sum += sin(phase)
            }
            (sum * 8_000.0).toInt().toShort()
        }
    }
}
