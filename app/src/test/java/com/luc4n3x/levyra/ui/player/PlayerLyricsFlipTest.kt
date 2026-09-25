package com.luc4n3x.levyra.ui.player

import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.LyricVocalRole
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerLyricsFlipTest {

    private val fling = 1_350f
    private val minFling = 60f

    @Test
    fun smallMovementStaysUndecided() {
        assertEquals(LyricsFlipAxis.Undecided, resolveLyricsFlipAxis(10f, 4f, slopPx = 24f))
    }

    @Test
    fun clearlyHorizontalMovementLocksHorizontal() {
        assertEquals(LyricsFlipAxis.Horizontal, resolveLyricsFlipAxis(-40f, 12f, slopPx = 24f))
    }

    @Test
    fun diagonalAndVerticalMovementLeaveTheGestureToScrolling() {
        assertEquals(LyricsFlipAxis.Vertical, resolveLyricsFlipAxis(30f, 28f, slopPx = 24f))
        assertEquals(LyricsFlipAxis.Vertical, resolveLyricsFlipAxis(4f, 40f, slopPx = 24f))
        assertEquals(LyricsFlipAxis.Undecided, resolveLyricsFlipAxis(Float.NaN, Float.NaN, slopPx = 24f))
    }

    @Test
    fun dragTowardStartMovesTowardLyricsAndMirrorsInRtl() {
        assertEquals(0.5f, lyricsFlipDragProgress(0f, -170f, 400f, rightToLeft = false), 0.0001f)
        assertEquals(0f, lyricsFlipDragProgress(0f, 170f, 400f, rightToLeft = false), 0f)
        assertEquals(0.5f, lyricsFlipDragProgress(0f, 170f, 400f, rightToLeft = true), 0.0001f)
        assertEquals(1f, lyricsFlipDragProgress(1f, -500f, 400f, rightToLeft = false), 0f)
        assertEquals(0.3f, lyricsFlipDragProgress(0.3f, 100f, 0f, rightToLeft = false), 0f)
    }

    @Test
    fun shortFastSwipeCommits() {
        val target = lyricsFlipSettleTarget(
            startFace = PlayerLyricsFace.Player,
            progress = 0.2f,
            dragPx = -70f,
            velocityPx = -2_000f,
            flingVelocityPx = fling,
            minFlingDistancePx = minFling,
            rightToLeft = false
        )
        assertEquals(PlayerLyricsFace.Lyrics, target)
    }

    @Test
    fun accidentalTwitchDoesNotChangeFace() {
        val target = lyricsFlipSettleTarget(
            startFace = PlayerLyricsFace.Player,
            progress = 0.05f,
            dragPx = -20f,
            velocityPx = -3_000f,
            flingVelocityPx = fling,
            minFlingDistancePx = minFling,
            rightToLeft = false
        )
        assertEquals(PlayerLyricsFace.Player, target)
    }

    @Test
    fun slowIncompleteSwipeSettlesBack() {
        val fromPlayer = lyricsFlipSettleTarget(PlayerLyricsFace.Player, 0.2f, -100f, -200f, fling, minFling, false)
        val fromLyrics = lyricsFlipSettleTarget(PlayerLyricsFace.Lyrics, 0.8f, 100f, 200f, fling, minFling, false)
        assertEquals(PlayerLyricsFace.Player, fromPlayer)
        assertEquals(PlayerLyricsFace.Lyrics, fromLyrics)
    }

    @Test
    fun slowLongSwipeCommitsBothWays() {
        assertEquals(
            PlayerLyricsFace.Lyrics,
            lyricsFlipSettleTarget(PlayerLyricsFace.Player, 0.45f, -160f, 0f, fling, minFling, false)
        )
        assertEquals(
            PlayerLyricsFace.Player,
            lyricsFlipSettleTarget(PlayerLyricsFace.Lyrics, 0.55f, 160f, 0f, fling, minFling, false)
        )
    }

    @Test
    fun flingAgainstTheTravelledDirectionFallsBackToDistance() {
        val target = lyricsFlipSettleTarget(PlayerLyricsFace.Player, 0.4f, -150f, 3_000f, fling, minFling, false)
        assertEquals(PlayerLyricsFace.Lyrics, target)
    }

    @Test
    fun depthFlipNeverShowsBothFacesOrAMirroredFace() {
        for (step in 0..100) {
            val progress = step / 100f
            val front = lyricsFlipFaceAlpha(progress, back = false, depth = true)
            val back = lyricsFlipFaceAlpha(progress, back = true, depth = true)
            assertEquals(1f, front + back, 0f)
            if (front > 0f) {
                assertTrue(abs(lyricsFlipFaceRotation(progress, back = false, rightToLeft = false)) < 90f)
            }
            if (back > 0f) {
                assertTrue(abs(lyricsFlipFaceRotation(progress, back = true, rightToLeft = false)) <= 90f)
            }
        }
        assertEquals(0f, lyricsFlipFaceRotation(1f, back = true, rightToLeft = false), 0f)
        assertEquals(0f, lyricsFlipFaceRotation(0f, back = false, rightToLeft = true), 0f)
    }

    @Test
    fun crossfadeNeverShowsBothFacesAtOnce() {
        for (step in 0..100) {
            val progress = step / 100f
            val front = lyricsFlipFaceAlpha(progress, back = false, depth = false)
            val back = lyricsFlipFaceAlpha(progress, back = true, depth = false)
            assertTrue(front == 0f || back == 0f)
        }
        assertEquals(1f, lyricsFlipFaceScale(0.5f, depth = false), 0f)
        assertTrue(lyricsFlipFaceScale(0.5f, depth = true) < 1f)
        assertEquals(1f, lyricsFlipFaceScale(1f, depth = true), 0.0001f)
    }

    @Test
    fun hiddenFrontFaceSwingsTheLyricsCardInAcrossTheWholeDrag() {
        assertEquals(0f, lyricsFlipFaceAlpha(0f, back = true, depth = true, frontVisible = false), 0f)
        assertEquals(1f, lyricsFlipFaceAlpha(0.1f, back = true, depth = true, frontVisible = false), 0f)
        assertEquals(0f, lyricsFlipFaceAlpha(0.6f, back = false, depth = true, frontVisible = false), 0f)
        assertEquals(0.4f, lyricsFlipFaceAlpha(0.4f, back = true, depth = false, frontVisible = false), 0.0001f)
        assertEquals(-81f, lyricsFlipFaceRotation(0.1f, back = true, rightToLeft = false, frontVisible = false), 0.0001f)
        assertEquals(81f, lyricsFlipFaceRotation(0.1f, back = true, rightToLeft = true, frontVisible = false), 0.0001f)
        assertEquals(36f, lyricsFlipFaceRotation(0.2f, back = false, rightToLeft = false), 0.0001f)
        assertEquals(0f, lyricsFlipFaceRotation(1f, back = true, rightToLeft = false, frontVisible = false), 0f)
    }

    @Test
    fun releaseVelocityFallsBackToAverageSpeedWhenTheTrackerHasTooFewSamples() {
        assertEquals(-2_000f, lyricsFlipReleaseVelocity(-2_000f, travelPx = -10f, elapsedMs = 100L), 0f)
        assertEquals(-2_000f, lyricsFlipReleaseVelocity(0f, travelPx = -160f, elapsedMs = 80L), 0.001f)
        assertEquals(0f, lyricsFlipReleaseVelocity(0f, travelPx = -160f, elapsedMs = 0L), 0f)
        assertEquals(0f, lyricsFlipReleaseVelocity(Float.NaN, travelPx = 0f, elapsedMs = 0L), 0f)
    }

    @Test
    fun settleDurationScalesWithRemainingDistance() {
        assertEquals(380, lyricsFlipSettleDurationMs(0f, 1f, depth = true))
        assertEquals(220, lyricsFlipSettleDurationMs(1f, 0f, depth = false))
        assertEquals(110, lyricsFlipSettleDurationMs(0.98f, 1f, depth = true))
    }

    @Test
    fun lyricsCardShowsOnlyReadableMainLines() {
        val lines = listOf(
            LyricLine(0L, 1_000L, "Intro credits", isMetadata = true),
            LyricLine(1_000L, 2_000L, "First line"),
            LyricLine(1_500L, 2_000L, "(echo)", role = LyricVocalRole.BACKGROUND),
            LyricLine(2_000L, 3_000L, "   "),
            LyricLine(3_000L, 4_000L, "Duet line", role = LyricVocalRole.DUET_LEFT)
        )
        assertEquals(listOf("First line", "Duet line"), playerLyricsCardLines(lines).map { it.text })
    }
}
