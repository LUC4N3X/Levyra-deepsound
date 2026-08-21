package com.luc4n3x.levyra.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackMemoryGuardPolicyTest {

    private val gib = 1024L * 1024L * 1024L

    @Test
    fun `threshold scales with device memory`() {
        val eightGb = PlaybackMemoryGuardPolicy.thresholdBytes(8L * gib, lowRamDevice = false)
        val twelveGb = PlaybackMemoryGuardPolicy.thresholdBytes(12L * gib, lowRamDevice = false)
        assertTrue(twelveGb > eightGb)
    }

    @Test
    fun `threshold stays inside safety bounds`() {
        val tiny = PlaybackMemoryGuardPolicy.thresholdBytes(1L * gib, lowRamDevice = true)
        val huge = PlaybackMemoryGuardPolicy.thresholdBytes(64L * gib, lowRamDevice = false)
        assertEquals(512L * 1024L * 1024L, tiny)
        assertEquals(2048L * 1024L * 1024L, huge)
    }

    @Test
    fun `unknown device memory falls back to minimum threshold`() {
        assertEquals(512L * 1024L * 1024L, PlaybackMemoryGuardPolicy.thresholdBytes(0L, lowRamDevice = false))
    }

    @Test
    fun `low ram devices trip earlier than standard devices`() {
        val low = PlaybackMemoryGuardPolicy.thresholdBytes(8L * gib, lowRamDevice = true)
        val standard = PlaybackMemoryGuardPolicy.thresholdBytes(8L * gib, lowRamDevice = false)
        assertTrue(low < standard)
    }

    @Test
    fun `single spike does not count as sustained pressure`() {
        val threshold = 1024L
        val afterSpike = PlaybackMemoryGuardPolicy.nextHighSampleCount(0, 2048L, threshold)
        assertEquals(1, afterSpike)
        assertFalse(
            PlaybackMemoryGuardPolicy.shouldRecycle(
                highSamples = afterSpike,
                nowElapsedMs = 10_000L,
                lastRecycleElapsedMs = 0L
            )
        )
    }

    @Test
    fun `sample counter resets when memory returns below threshold`() {
        assertEquals(0, PlaybackMemoryGuardPolicy.nextHighSampleCount(2, 512L, 1024L))
    }

    @Test
    fun `sustained pressure trips the guard`() {
        assertTrue(
            PlaybackMemoryGuardPolicy.shouldRecycle(
                highSamples = PlaybackMemoryGuardPolicy.REQUIRED_HIGH_SAMPLES,
                nowElapsedMs = 10_000L,
                lastRecycleElapsedMs = 0L
            )
        )
    }

    @Test
    fun `cooldown blocks a second recycle loop`() {
        assertFalse(
            PlaybackMemoryGuardPolicy.shouldRecycle(
                highSamples = PlaybackMemoryGuardPolicy.REQUIRED_HIGH_SAMPLES,
                nowElapsedMs = 30_000L,
                lastRecycleElapsedMs = 10_000L
            )
        )
        assertTrue(
            PlaybackMemoryGuardPolicy.shouldRecycle(
                highSamples = PlaybackMemoryGuardPolicy.REQUIRED_HIGH_SAMPLES,
                nowElapsedMs = 10_000L + PlaybackMemoryGuardPolicy.COOLDOWN_MS,
                lastRecycleElapsedMs = 10_000L
            )
        )
    }

    @Test
    fun `music playback disables video tracks`() {
        assertTrue(PlaybackTrackSelectionPolicy.disableVideoTracks(videoMode = false))
    }

    @Test
    fun `video mode keeps video tracks enabled`() {
        assertFalse(PlaybackTrackSelectionPolicy.disableVideoTracks(videoMode = true))
    }
}
