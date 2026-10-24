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

    @Test
    fun `playing lyrics read the live player position instead of the published tick`() {
        val live = { 10_180L }
        assertEquals(10_180L, lyricsClockSourcePositionMs(reportedPositionMs = 10_000L, isPlaying = true, livePositionMs = live))
    }

    @Test
    fun `paused lyrics keep the published position`() {
        val live = { 0L }
        assertEquals(42_000L, lyricsClockSourcePositionMs(reportedPositionMs = 42_000L, isPlaying = false, livePositionMs = live))
    }

    @Test
    fun `missing live source falls back to the published position`() {
        assertEquals(7_500L, lyricsClockSourcePositionMs(reportedPositionMs = 7_500L, isPlaying = true, livePositionMs = null))
    }

    @Test
    fun `live sampling every frame keeps the rendered clock on the player position`() {
        assertFrameLoopTracksPlayer(frameMs = 16L, speed = 1f)
    }

    @Test
    fun `fast playback at a low frame rate does not fall behind`() {
        assertFrameLoopTracksPlayer(frameMs = 33L, speed = 2f)
    }

    @Test
    fun `janky frames at normal speed do not fall behind`() {
        assertFrameLoopTracksPlayer(frameMs = 67L, speed = 1f)
    }

    private fun assertFrameLoopTracksPlayer(frameMs: Long, speed: Float) {
        val clock = LyricsPlaybackClock(0L)
        var playerMs = 30_000L
        var nowMs = 1_000L
        clock.sync(playerMs, playing = true, speed = speed, nowRealtimeMs = nowMs)
        repeat(300) {
            nowMs += frameMs
            playerMs += (frameMs * speed).toLong()
            clock.advance(nowMs)
            clock.sync(playerMs, playing = true, speed = speed, nowRealtimeMs = nowMs)
            assertTrue("lag ${playerMs - clock.positionMs}", kotlin.math.abs(playerMs - clock.positionMs) <= 1L)
        }
    }

    @Test
    fun `seek while playing snaps to the live position on the next frame`() {
        val clock = LyricsPlaybackClock(0L)
        clock.sync(30_000L, playing = true, speed = 1f, nowRealtimeMs = 1_000L)
        clock.advance(1_000L)
        clock.advance(1_016L)
        clock.sync(95_000L, playing = true, speed = 1f, nowRealtimeMs = 1_016L)
        assertEquals(95_000L, clock.positionMs)
        clock.advance(1_032L)
        clock.sync(12_000L, playing = true, speed = 1f, nowRealtimeMs = 1_032L)
        assertEquals(12_000L, clock.positionMs)
    }

    @Test
    fun `pause freezes the clock on the reported position`() {
        val clock = LyricsPlaybackClock(0L)
        clock.sync(20_000L, playing = true, speed = 1f, nowRealtimeMs = 0L)
        clock.advance(500L)
        clock.sync(20_480L, playing = false, speed = 1f, nowRealtimeMs = 520L)
        clock.advance(5_000L)
        assertEquals(20_480L, clock.positionMs)
    }

    @Test
    fun `faster playback advances the clock between live samples`() {
        val clock = LyricsPlaybackClock(0L)
        clock.sync(10_000L, playing = true, speed = 2f, nowRealtimeMs = 0L)
        clock.advance(100L)
        assertEquals(10_200L, clock.positionMs)
    }
}
