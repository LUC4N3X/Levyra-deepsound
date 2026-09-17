package com.luc4n3x.levyra.player.waveseek

import org.junit.Assert.assertEquals
import org.junit.Test

class WaveSeekPollingPolicyTest {
    @Test
    fun `capture polls quickly only while playback is active`() {
        assertEquals(120L, waveSeekPollDelayMs(hasPlayer = true, hasCapture = true, isPlaying = true))
        assertEquals(750L, waveSeekPollDelayMs(hasPlayer = true, hasCapture = true, isPlaying = false))
    }

    @Test
    fun `idle runtime backs off aggressively`() {
        assertEquals(1_500L, waveSeekPollDelayMs(hasPlayer = false, hasCapture = false, isPlaying = false))
        assertEquals(1_000L, waveSeekPollDelayMs(hasPlayer = true, hasCapture = false, isPlaying = true))
    }
}
