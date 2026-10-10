package com.luc4n3x.levyra.player.waveseek

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WaveSeekEnvelopeTest {

    @Test
    fun `measured waveform resamples without losing a narrow peak`() {
        val samples = FloatArray(WaveSeekEnvelope.BUCKETS) { 0.08f }
        samples[WaveSeekEnvelope.BUCKETS / 2] = 1f

        val envelope = WaveSeekEnvelope.measured(samples)
        val bars = requireNotNull(envelope).bars(64)

        assertEquals(64, bars.size)
        assertTrue(bars.maxOrNull()!! > 0.85f)
        assertTrue(bars.all { it in WaveSeekEnvelope.RESTING..1f })
    }

    @Test
    fun `flat waveform remains visually stable instead of amplifying noise`() {
        val envelope = requireNotNull(
            WaveSeekEnvelope.measured(FloatArray(WaveSeekEnvelope.BUCKETS) { 0.42f })
        )

        val bars = envelope.bars(72)

        assertTrue(bars.all { kotlin.math.abs(it - bars.first()) < 0.0001f })
        assertTrue(bars.first() in 0.45f..0.85f)
    }

    @Test
    fun `serialization round trip preserves the frozen envelope`() {
        val samples = FloatArray(WaveSeekEnvelope.BUCKETS) { index ->
            (index % 37).toFloat() / 36f
        }
        val original = requireNotNull(WaveSeekEnvelope.measured(samples))

        val restored = WaveSeekEnvelope.decodeFromString(original.encodeToString())

        requireNotNull(restored)
        assertArrayEquals(original.bars(96), restored.bars(96), 0.0001f)
    }

    @Test
    fun `invalid persisted data is rejected`() {
        assertEquals(null, WaveSeekEnvelope.decodeFromString("not-base64"))
        assertEquals(null, WaveSeekEnvelope.decodeFromString(""))
    }

    @Test
    fun `bar request with non-positive count is empty`() {
        val envelope = requireNotNull(
            WaveSeekEnvelope.measured(FloatArray(WaveSeekEnvelope.BUCKETS) { 0.5f })
        )

        assertEquals(0, envelope.bars(0).size)
        assertEquals(0, envelope.bars(-4).size)
    }
}
