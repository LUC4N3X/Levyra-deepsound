package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartResumeTest {
    private var now = 0L
    private val tracker = SmartResumeTracker { now }
    private val song = SmartResumeMedia(key = "song", positionMs = 120_000L, durationMs = 240_000L, seekable = true, live = false)

    private fun resume(media: SmartResumeMedia = song, enabled: Boolean = true, profile: SmartResumeProfile = SmartResumeProfile.BALANCED, minPause: Int = 10) =
        tracker.onResumed(enabled, profile, minPause, media)

    @Test
    fun curveIsZeroBelowThresholdThenMonotonicAndCapped() {
        assertEquals(0L, SmartResumeCurve.rewindMs(9_999L, SmartResumeProfile.BALANCED, 10))
        var previous = 0L
        var second = 10L
        while (second <= 48L * 3600L) {
            val rewind = SmartResumeCurve.rewindMs(second * 1000L, SmartResumeProfile.BALANCED, 10)
            assertTrue("non monotonic at $second s", rewind >= previous)
            assertTrue(rewind <= 30_000L)
            previous = rewind
            second += 7L
        }
        assertEquals(30_000L, SmartResumeCurve.rewindMs(7L * 24L * 3600_000L, SmartResumeProfile.BALANCED, 10))
    }

    @Test
    fun curveGrowsSmoothlyWithoutLargeSteps() {
        var previous = SmartResumeCurve.rewindMs(10_000L, SmartResumeProfile.BALANCED, 5)
        var ms = 11_000L
        while (ms <= 6L * 3600_000L) {
            val rewind = SmartResumeCurve.rewindMs(ms, SmartResumeProfile.BALANCED, 5)
            assertTrue("jump of ${rewind - previous} ms at $ms", rewind - previous <= 500L)
            previous = rewind
            ms += 1_000L
        }
    }

    @Test
    fun expectedRewindsForTypicalInterruptions() {
        assertEquals(4_500L, SmartResumeCurve.rewindMs(30_000L, SmartResumeProfile.BALANCED, 10))
        assertEquals(6_000L, SmartResumeCurve.rewindMs(60_000L, SmartResumeProfile.BALANCED, 10))
        assertEquals(12_500L, SmartResumeCurve.rewindMs(5L * 60_000L, SmartResumeProfile.BALANCED, 10))
        assertEquals(25_500L, SmartResumeCurve.rewindMs(60L * 60_000L, SmartResumeProfile.BALANCED, 10))
        assertTrue(SmartResumeCurve.rewindMs(60_000L, SmartResumeProfile.GENTLE, 10) < SmartResumeCurve.rewindMs(60_000L, SmartResumeProfile.GENEROUS, 10))
    }

    @Test
    fun phoneCallRewindsOnceAfterAutomaticResume() {
        tracker.onInterrupted(SmartResumeCause.TransientFocusLoss, "song")
        now += 90_000L
        assertEquals(120_000L - SmartResumeCurve.rewindMs(90_000L, SmartResumeProfile.BALANCED, 10), resume())
        assertNull(resume())
        assertFalse(tracker.hasPendingInterruption)
    }

    @Test
    fun manualPauseNeverRewinds() {
        tracker.onInterrupted(SmartResumeCause.OutputLost, "song")
        tracker.onUserTookControl()
        now += 3_600_000L
        assertNull(resume())
    }

    @Test
    fun shortInterruptionDoesNotSeek() {
        tracker.onInterrupted(SmartResumeCause.TransientFocusLoss, "song")
        now += 4_000L
        assertNull(resume())
    }

    @Test
    fun liveAndUnseekableContentIsNeverTouched() {
        tracker.onInterrupted(SmartResumeCause.FocusLoss, "radio")
        now += 600_000L
        assertNull(resume(song.copy(key = "radio", live = true)))
        tracker.onInterrupted(SmartResumeCause.FocusLoss, "stream")
        now += 600_000L
        assertNull(resume(song.copy(key = "stream", seekable = false)))
        tracker.onInterrupted(SmartResumeCause.FocusLoss, "unknown")
        now += 600_000L
        assertNull(resume(song.copy(key = "unknown", durationMs = 0L)))
    }

    @Test
    fun trackChangeDuringInterruptionCancelsRewind() {
        tracker.onInterrupted(SmartResumeCause.OutputLost, "song")
        tracker.onMediaChanged("next")
        now += 600_000L
        assertNull(resume(song.copy(key = "next")))
    }

    @Test
    fun repeatedInterruptionsKeepTheEarliestStart() {
        tracker.onInterrupted(SmartResumeCause.TransientFocusLoss, "song")
        now += 30_000L
        tracker.onInterrupted(SmartResumeCause.FocusLoss, "song")
        now += 30_000L
        assertEquals(120_000L - SmartResumeCurve.rewindMs(60_000L, SmartResumeProfile.BALANCED, 10), resume())
    }

    @Test
    fun rewindNeverGoesBeforeTheStartAndSkipsTheFirstSeconds() {
        tracker.onInterrupted(SmartResumeCause.FocusLoss, "song")
        now += 3_600_000L
        assertEquals(0L, resume(song.copy(positionMs = 5_000L)))
        tracker.onInterrupted(SmartResumeCause.FocusLoss, "song")
        now += 3_600_000L
        assertNull(resume(song.copy(positionMs = 1_000L)))
    }

    @Test
    fun disabledFeatureConsumesTheInterruptionWithoutSeeking() {
        tracker.onInterrupted(SmartResumeCause.FocusLoss, "song")
        now += 600_000L
        assertNull(resume(enabled = false))
        assertFalse(tracker.hasPendingInterruption)
    }

    @Test
    fun settingsNormalizeAndDefaultOff() {
        val defaults = LevyraAudioSettings()
        assertFalse(defaults.smartResumeEnabled)
        assertFalse(defaults.adaptiveListeningEnabled)
        val normalized = defaults.copy(smartResumeMinPauseSeconds = 1, adaptiveListeningMaxBoostDb = 40f).normalized()
        assertEquals(SmartResumeCurve.MIN_PAUSE_SECONDS, normalized.smartResumeMinPauseSeconds)
        assertEquals(AdaptiveListeningLimits.MAX_BOOST_DB, normalized.adaptiveListeningMaxBoostDb)
    }
}
