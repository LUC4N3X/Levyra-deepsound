package com.luc4n3x.levyra.player

import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoMixPlannerTest {
    @Test
    fun crossfadeUsesEqualPowerCurve() {
        val start = equalPowerCrossfade(0f)
        val middle = equalPowerCrossfade(.5f)
        val end = equalPowerCrossfade(1f)

        assertEquals(1f, start.outgoing, .001f)
        assertEquals(0f, start.incoming, .001f)
        assertEquals(0.7071f, middle.outgoing, .001f)
        assertEquals(0.7071f, middle.incoming, .001f)
        assertEquals(0f, end.outgoing, .001f)
        assertEquals(1f, end.incoming, .001f)
    }

    @Test
    fun nativeVideoRepeatOneAndLowRamNeverStartSecondPlayer() {
        val settings = LevyraAudioSettings(crossfadeSeconds = 6)
        assertNull(planAutoMix(track(), track("next"), settings, RepeatMode.Off, videoMode = true, lowRam = false))
        assertNull(planAutoMix(track(), track("next"), settings, RepeatMode.One, videoMode = false, lowRam = false))
        assertNull(planAutoMix(track(), track("next"), settings, RepeatMode.Off, videoMode = false, lowRam = true))
    }

    @Test
    fun autoMixAdaptsDurationWithoutExceedingBounds() {
        val current = track().copy(energy = 70, vocal = 30)
        val next = track("next").copy(energy = 75, vocal = 30)
        val plan = planAutoMix(
            current,
            next,
            LevyraAudioSettings(crossfadeSeconds = 6, djSoftMode = true),
            RepeatMode.Off,
            videoMode = false,
            lowRam = false
        )
        assertEquals(7_500L, plan?.transitionMs)
    }

    @Test
    fun handoffResyncOnlyWhenPlayersDriftPastTolerance() {
        assertFalse(crossfadeHandoffNeedsResync(10_000L, 10_180L, toleranceMs = 250L))
        assertTrue(crossfadeHandoffNeedsResync(10_000L, 10_400L, toleranceMs = 250L))
    }

    @Test
    fun handoffSeekTargetsLiveSecondaryPositionAndClampsToDuration() {
        assertEquals(12_120L, crossfadeHandoffSeekPosition(12_000L, 180_000L, leadMs = 120L))
        assertEquals(19_999L, crossfadeHandoffSeekPosition(19_950L, 20_000L, leadMs = 120L))
    }

    @Test
    fun crossfadeStepScalesInverselyWithPlaybackSpeed() {
        assertEquals(50L, crossfadeStepWallClockMs(50L, 1.0f))
        assertEquals(25L, crossfadeStepWallClockMs(50L, 2.0f))
        assertEquals(100L, crossfadeStepWallClockMs(50L, 0.5f))
        assertEquals(40L, crossfadeStepWallClockMs(60L, 1.5f))
    }

    @Test
    fun crossfadeStepClampsToMinimumAndHandlesInvalidSpeedGracefully() {
        assertEquals(10L, crossfadeStepWallClockMs(5L, 2.0f, minWallClockMs = 10L))
        assertEquals(50L, crossfadeStepWallClockMs(50L, 0f))
        assertEquals(50L, crossfadeStepWallClockMs(50L, -1.5f))
        assertEquals(50L, crossfadeStepWallClockMs(50L, Float.NaN))
        assertEquals(50L, crossfadeStepWallClockMs(50L, Float.POSITIVE_INFINITY))
    }

    private fun track(id: String = "current") = Track(
        id = id,
        title = id,
        artist = "Artist",
        album = "Album",
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "test",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0
    )
}
