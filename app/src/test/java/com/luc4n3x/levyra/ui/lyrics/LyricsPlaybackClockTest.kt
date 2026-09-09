package com.luc4n3x.levyra.ui.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsPlaybackClockTest {

    @Test
    fun `paused playback freezes on the anchor`() {
        val position = projectedLyricsPositionMs(
            anchorPositionMs = 12_000L,
            anchorRealtimeMs = 1_000L,
            nowRealtimeMs = 9_000L,
            playing = false,
            speed = 1f
        )
        assertEquals(12_000L, position)
    }

    @Test
    fun `playing position advances with elapsed realtime`() {
        val position = projectedLyricsPositionMs(
            anchorPositionMs = 12_000L,
            anchorRealtimeMs = 1_000L,
            nowRealtimeMs = 1_240L,
            playing = true,
            speed = 1f
        )
        assertEquals(12_240L, position)
    }

    @Test
    fun `playback speed scales interpolation`() {
        val position = projectedLyricsPositionMs(
            anchorPositionMs = 0L,
            anchorRealtimeMs = 0L,
            nowRealtimeMs = 1_000L,
            playing = true,
            speed = 1.5f
        )
        assertEquals(1_500L, position)
    }

    @Test
    fun `speed is clamped to a sane range`() {
        val position = projectedLyricsPositionMs(0L, 0L, 1_000L, playing = true, speed = 40f)
        assertEquals((1_000L * LYRICS_CLOCK_MAX_SPEED).toLong(), position)
    }

    @Test
    fun `position never goes negative`() {
        assertEquals(0L, projectedLyricsPositionMs(-500L, 0L, 0L, playing = false, speed = 1f))
    }

    @Test
    fun `a seek snaps instead of converging`() {
        assertTrue(shouldSnapLyricsClock(renderedMs = 10_000L, reportedMs = 60_000L))
        assertEquals(60_000L, convergedLyricsAnchorMs(renderedMs = 10_000L, reportedMs = 60_000L))
    }

    @Test
    fun `small drift converges gradually without a visible jump`() {
        assertFalse(shouldSnapLyricsClock(renderedMs = 10_000L, reportedMs = 10_120L))
        val corrected = convergedLyricsAnchorMs(renderedMs = 10_000L, reportedMs = 10_120L)
        assertTrue(corrected > 10_000L)
        assertTrue(corrected <= 10_000L + LYRICS_CLOCK_MAX_CONVERGENCE_MS)
    }

    @Test
    fun `backward drift converges without rewinding past the cap`() {
        val corrected = convergedLyricsAnchorMs(renderedMs = 10_200L, reportedMs = 10_000L)
        assertTrue(corrected < 10_200L)
        assertTrue(corrected >= 10_200L - LYRICS_CLOCK_MAX_CONVERGENCE_MS)
    }
}
