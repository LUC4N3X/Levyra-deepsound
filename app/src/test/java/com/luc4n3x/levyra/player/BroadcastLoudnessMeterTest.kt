package com.luc4n3x.levyra.player

import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BroadcastLoudnessMeterTest {
    @Test
    fun kWeightingMatchesBs1770ReferenceCoefficientsAt48k() {
        val (shelf, highPass) = BroadcastLoudnessMeter.kWeightingStages(48_000)

        assertArrayEquals(
            doubleArrayOf(1.53512485958697, -2.69169618940638, 1.19839281085285, -1.69065929318241, 0.73248077421585),
            shelf,
            1e-12
        )
        assertArrayEquals(
            doubleArrayOf(1.0, -2.0, 1.0, -1.99004745483398, 0.99007225036621),
            highPass,
            1e-12
        )
    }

    @Test
    fun stereoSineAtMinus23DbfsReadsMinus23Lufs() {
        val meter = BroadcastLoudnessMeter().apply { configure(48_000, 2) }
        val tone = ToneGenerator(48_000, 2)

        tone.feed(meter, amplitudeDbfs = -23.0, seconds = 20.0)

        assertEquals(-23.0, meter.integratedLoudnessLufs(), 0.1)
    }

    @Test
    fun fullScaleSineOnSingleChannelReadsMinus3LkfsAt44k() {
        val meter = BroadcastLoudnessMeter().apply { configure(44_100, 1) }
        val tone = ToneGenerator(44_100, 1)

        tone.feed(meter, amplitudeDbfs = 0.0, seconds = 10.0)

        assertEquals(-3.01, meter.integratedLoudnessLufs(), 0.1)
    }

    @Test
    fun relativeGateExcludesQuietPassagesAsInEbuTech3341Case3() {
        val meter = BroadcastLoudnessMeter().apply { configure(48_000, 2) }
        val tone = ToneGenerator(48_000, 2)

        tone.feed(meter, amplitudeDbfs = -36.0, seconds = 10.0)
        tone.feed(meter, amplitudeDbfs = -23.0, seconds = 60.0)
        tone.feed(meter, amplitudeDbfs = -36.0, seconds = 10.0)

        assertEquals(-23.0, meter.integratedLoudnessLufs(), 0.1)
    }

    @Test
    fun digitalSilenceIsRemovedByAbsoluteGate() {
        val meter = BroadcastLoudnessMeter().apply { configure(48_000, 2) }

        repeat(48_000 * 5) {
            meter.push(0.0, 0)
            meter.push(0.0, 1)
        }

        assertEquals(0, meter.measuredBlocks)
        assertTrue(meter.integratedLoudnessLufs().isNaN())
    }

    @Test
    fun firstBlockNeedsFourHundredMillisecondsThenAdvancesEveryHundred() {
        val meter = BroadcastLoudnessMeter().apply { configure(48_000, 1) }
        val tone = ToneGenerator(48_000, 1)

        tone.feed(meter, amplitudeDbfs = -20.0, seconds = 0.39)
        assertEquals(0, meter.measuredBlocks)
        tone.feed(meter, amplitudeDbfs = -20.0, seconds = 0.31)
        assertEquals(4, meter.measuredBlocks)
    }

    @Test
    fun resetForgetsPreviousProgramme() {
        val meter = BroadcastLoudnessMeter().apply { configure(48_000, 2) }
        val tone = ToneGenerator(48_000, 2)
        tone.feed(meter, amplitudeDbfs = -10.0, seconds = 5.0)

        meter.reset()
        tone.feed(meter, amplitudeDbfs = -30.0, seconds = 5.0)

        assertEquals(-30.0, meter.integratedLoudnessLufs(), 0.1)
    }

    private class ToneGenerator(private val sampleRate: Int, private val channels: Int) {
        private var phase = 0.0

        fun feed(meter: BroadcastLoudnessMeter, amplitudeDbfs: Double, seconds: Double) {
            val amplitude = 10.0.pow(amplitudeDbfs / 20.0)
            val increment = 2.0 * PI * TONE_HZ / sampleRate
            repeat((seconds * sampleRate).toInt()) {
                val sample = amplitude * sin(phase)
                phase += increment
                for (channel in 0 until channels) meter.push(sample, channel)
            }
        }
    }

    private companion object {
        const val TONE_HZ = 997.0
    }
}
