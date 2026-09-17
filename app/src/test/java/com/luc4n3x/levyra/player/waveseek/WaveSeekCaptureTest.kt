package com.luc4n3x.levyra.player.waveseek

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WaveSeekCaptureTest {
    @Test
    fun `continuous playback becomes a full measured envelope`() {
        val durationMs = 180_000L
        val capture = WaveSeekCapture(durationMs)

        var positionMs = 0L
        while (positionMs < durationMs) {
            val amplitude = 0.12f + (positionMs / 1_000L % 7L).toFloat() * 0.1f
            capture.record(positionMs, floatArrayOf(amplitude, amplitude * 0.8f))
            positionMs += 120L
        }
        capture.record(durationMs - 1L, floatArrayOf(0.65f))

        val envelope = capture.snapshotIfReady()

        assertNotNull(envelope)
        assertTrue(requireNotNull(envelope).bars(96).any { it > WaveSeekEnvelope.RESTING + 0.2f })
    }

    @Test
    fun `sparse playback never pretends to be a full waveform`() {
        val capture = WaveSeekCapture(180_000L)

        capture.record(0L, floatArrayOf(0.6f))
        capture.record(45_000L, floatArrayOf(0.7f))
        capture.record(120_000L, floatArrayOf(0.5f))

        assertNull(capture.snapshotIfReady())
    }

    @Test
    fun `large seek does not paint the skipped region`() {
        val capture = WaveSeekCapture(120_000L)

        capture.record(1_000L, floatArrayOf(0.8f))
        capture.record(1_120L, floatArrayOf(0.7f))
        capture.record(90_000L, floatArrayOf(0.9f))

        assertTrue(capture.coverageFraction() < 0.1f)
        assertNull(capture.snapshotIfReady())
    }

    @Test
    fun `invalid observations do not increase coverage`() {
        val capture = WaveSeekCapture(90_000L)

        capture.record(-1L, floatArrayOf(0.8f))
        capture.record(10_000L, FloatArray(0))
        capture.record(10_000L, floatArrayOf(Float.NaN, Float.POSITIVE_INFINITY))

        assertTrue(capture.coverageFraction() == 0f)
    }
}
