package com.luc4n3x.levyra.player

import androidx.media3.common.Player
import androidx.media3.common.C
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class PlaybackStabilitySignalsTest {

    private class FakeClock(var value: Long = 0L) {
        fun advance(deltaMs: Long) {
            value += deltaMs
        }
    }

    private fun signals(clock: FakeClock) = PlaybackStabilitySignals(nowMs = clock::value)

    private fun PlaybackStabilitySignals.startPlayback() {
        onPlayWhenReadyChanged(true, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
        onPlaybackStateChanged(Player.STATE_READY)
    }

    private fun PlaybackStabilitySignals.rebuffer() {
        onPlaybackStateChanged(Player.STATE_BUFFERING)
        onPlaybackStateChanged(Player.STATE_READY)
    }

    @Test
    fun startsInNormalProfile() {
        val signals = signals(FakeClock())
        assertEquals(PlaybackStabilityProfile.Normal, signals.requestedProfile())
    }

    @Test
    fun singleIsolatedRebufferDoesNotEscalate() {
        val clock = FakeClock()
        val signals = signals(clock)
        signals.startPlayback()
        signals.rebuffer()
        assertEquals(PlaybackStabilityProfile.Normal, signals.requestedProfile())
    }

    @Test
    fun repeatedRebufferEscalatesToStable() {
        val clock = FakeClock()
        val signals = signals(clock)
        signals.startPlayback()
        signals.rebuffer()
        clock.advance(1_000L)
        signals.rebuffer()
        assertEquals(PlaybackStabilityProfile.Stable, signals.requestedProfile())
    }

    @Test
    fun rebufferOutsideWindowDoesNotAccumulate() {
        val clock = FakeClock()
        val signals = signals(clock)
        signals.startPlayback()
        signals.rebuffer()
        clock.advance(200_000L)
        signals.rebuffer()
        assertEquals(PlaybackStabilityProfile.Normal, signals.requestedProfile())
    }

    @Test
    fun seekDoesNotCountAsRebuffer() {
        val clock = FakeClock()
        val signals = signals(clock)
        signals.startPlayback()
        signals.onDiscontinuity()
        signals.onPlaybackStateChanged(Player.STATE_BUFFERING)
        signals.onPlaybackStateChanged(Player.STATE_READY)
        signals.onDiscontinuity()
        signals.onPlaybackStateChanged(Player.STATE_BUFFERING)
        signals.onPlaybackStateChanged(Player.STATE_READY)
        assertEquals(PlaybackStabilityProfile.Normal, signals.requestedProfile())
    }

    @Test
    fun trackTransitionBufferingDoesNotCountAsRebuffer() {
        val clock = FakeClock()
        val signals = signals(clock)
        signals.startPlayback()
        repeat(5) {
            signals.onDiscontinuity()
            signals.onPlaybackStateChanged(Player.STATE_BUFFERING)
            signals.onPlaybackStateChanged(Player.STATE_READY)
        }
        assertEquals(PlaybackStabilityProfile.Normal, signals.requestedProfile())
    }

    @Test
    fun repeatedLoadFailureEscalatesToStable() {
        val signals = signals(FakeClock())
        signals.onLoadOutcome(failed = true, wasCanceled = false)
        signals.onLoadOutcome(failed = true, wasCanceled = false)
        signals.onLoadOutcome(failed = true, wasCanceled = false)
        assertEquals(PlaybackStabilityProfile.Stable, signals.requestedProfile())
    }

    @Test
    fun cancelledLoadDoesNotCountAsFailure() {
        val signals = signals(FakeClock())
        repeat(5) { signals.onLoadOutcome(failed = true, wasCanceled = true) }
        assertEquals(PlaybackStabilityProfile.Normal, signals.requestedProfile())
    }

    @Test
    fun successfulLoadResetsFailureStreak() {
        val signals = signals(FakeClock())
        signals.onLoadOutcome(failed = true, wasCanceled = false)
        signals.onLoadOutcome(failed = true, wasCanceled = false)
        signals.onLoadOutcome(failed = false, wasCanceled = false)
        signals.onLoadOutcome(failed = true, wasCanceled = false)
        assertEquals(PlaybackStabilityProfile.Normal, signals.requestedProfile())
    }

    @Test
    fun staysStableBeforeMinimumDwellElapses() {
        val clock = FakeClock()
        val signals = signals(clock)
        signals.startPlayback()
        signals.rebuffer()
        clock.advance(1_000L)
        signals.rebuffer()
        assertEquals(PlaybackStabilityProfile.Stable, signals.requestedProfile())
        clock.advance(5_000L)
        assertEquals(PlaybackStabilityProfile.Stable, signals.requestedProfile())
    }

    @Test
    fun recoversToNormalAfterDwellWithNoFurtherSignals() {
        val clock = FakeClock()
        val signals = signals(clock)
        signals.startPlayback()
        signals.rebuffer()
        clock.advance(1_000L)
        signals.rebuffer()
        assertEquals(PlaybackStabilityProfile.Stable, signals.requestedProfile())
        clock.advance(130_000L)
        assertEquals(PlaybackStabilityProfile.Normal, signals.requestedProfile())
    }

    @Test
    fun newSignalDuringDwellExtendsStableWindow() {
        val clock = FakeClock()
        val signals = signals(clock)
        signals.startPlayback()
        signals.rebuffer()
        clock.advance(1_000L)
        signals.rebuffer()
        clock.advance(115_000L)
        signals.rebuffer()
        clock.advance(1_000L)
        signals.rebuffer()
        clock.advance(10_000L)
        assertEquals(PlaybackStabilityProfile.Stable, signals.requestedProfile())
    }

    @Test
    fun noOscillationAcrossManyShortIntervalChecks() {
        val clock = FakeClock()
        val signals = signals(clock)
        signals.startPlayback()
        signals.rebuffer()
        clock.advance(1_000L)
        signals.rebuffer()
        var sawNormalAfterEscalation = false
        repeat(20) {
            clock.advance(5_000L)
            if (signals.requestedProfile() == PlaybackStabilityProfile.Normal) sawNormalAfterEscalation = true
        }
        assertEquals(false, sawNormalAfterEscalation)
    }

    @Test
    fun irrelevantTrackTypesDoNotCountAsFailures() {
        val signals = signals(FakeClock())
        val dummyEventTime = AnalyticsListener.EventTime(0L, Timeline.EMPTY, 0, null, 0L, Timeline.EMPTY, 0, null, 0L, 0L)
        val dummyLoadEventInfo = org.mockito.Mockito.mock(LoadEventInfo::class.java)
        val subtitleLoad = MediaLoadData(C.DATA_TYPE_MEDIA, C.TRACK_TYPE_TEXT, null, 0, null, 0L, 0L)
        signals.onLoadError(dummyEventTime, dummyLoadEventInfo, subtitleLoad, IOException(), false)
        signals.onLoadError(dummyEventTime, dummyLoadEventInfo, subtitleLoad, IOException(), false)
        signals.onLoadError(dummyEventTime, dummyLoadEventInfo, subtitleLoad, IOException(), false)
        assertEquals(PlaybackStabilityProfile.Normal, signals.requestedProfile())
    }

    @Test
    fun nonMediaDataTypesDoNotCountAsFailures() {
        val signals = signals(FakeClock())
        val dummyEventTime = AnalyticsListener.EventTime(0L, Timeline.EMPTY, 0, null, 0L, Timeline.EMPTY, 0, null, 0L, 0L)
        val dummyLoadEventInfo = org.mockito.Mockito.mock(LoadEventInfo::class.java)
        val manifestLoad = MediaLoadData(C.DATA_TYPE_MANIFEST, C.TRACK_TYPE_DEFAULT, null, 0, null, 0L, 0L)
        signals.onLoadError(dummyEventTime, dummyLoadEventInfo, manifestLoad, IOException(), false)
        signals.onLoadError(dummyEventTime, dummyLoadEventInfo, manifestLoad, IOException(), false)
        signals.onLoadError(dummyEventTime, dummyLoadEventInfo, manifestLoad, IOException(), false)
        assertEquals(PlaybackStabilityProfile.Normal, signals.requestedProfile())
    }

    @Test
    fun preloadFailuresDoNotCountForCurrentTrack() {
        val signals = signals(FakeClock())
        val currentPeriodId = androidx.media3.exoplayer.source.MediaSource.MediaPeriodId("current")
        val preloadPeriodId = androidx.media3.exoplayer.source.MediaSource.MediaPeriodId("preload")

        val currentEventTime = AnalyticsListener.EventTime(0L, Timeline.EMPTY, 0, currentPeriodId, 0L, Timeline.EMPTY, 0, currentPeriodId, 0L, 0L)
        val preloadEventTime = AnalyticsListener.EventTime(0L, Timeline.EMPTY, 0, preloadPeriodId, 0L, Timeline.EMPTY, 0, preloadPeriodId, 0L, 0L)

        signals.onPlaybackStateChanged(currentEventTime, Player.STATE_READY) // Sets playingPeriodUid to "current"

        val dummyLoadEventInfo = org.mockito.Mockito.mock(LoadEventInfo::class.java)
        val audioLoad = MediaLoadData(C.DATA_TYPE_MEDIA, C.TRACK_TYPE_AUDIO, null, 0, null, 0L, 0L)

        signals.onLoadError(preloadEventTime, dummyLoadEventInfo, audioLoad, IOException(), false)
        signals.onLoadError(preloadEventTime, dummyLoadEventInfo, audioLoad, IOException(), false)
        signals.onLoadError(preloadEventTime, dummyLoadEventInfo, audioLoad, IOException(), false)
        assertEquals(PlaybackStabilityProfile.Normal, signals.requestedProfile())
    }

    @Test
    fun differentTrackTypesKeepFailureStreaksSeparate() {
        val signals = signals(FakeClock())
        signals.onLoadOutcome(failed = true, wasCanceled = false, trackType = C.TRACK_TYPE_AUDIO)
        signals.onLoadOutcome(failed = true, wasCanceled = false, trackType = C.TRACK_TYPE_AUDIO)
        assertEquals(PlaybackStabilityProfile.Normal, signals.requestedProfile())

        signals.onLoadOutcome(failed = false, wasCanceled = false, trackType = C.TRACK_TYPE_VIDEO)

        signals.onLoadOutcome(failed = true, wasCanceled = false, trackType = C.TRACK_TYPE_AUDIO)
        assertEquals(PlaybackStabilityProfile.Stable, signals.requestedProfile())
    }

    @Test
    fun seamlessTransitionDoesNotSuppressSubsequentRealRebuffer() {
        val clock = FakeClock()
        val signals = signals(clock)
        signals.startPlayback()

        val dummyPosition = org.mockito.Mockito.mock(Player.PositionInfo::class.java)
        signals.onPositionDiscontinuity(dummyPosition, dummyPosition, Player.DISCONTINUITY_REASON_AUTO_TRANSITION)
        signals.onMediaItemTransition(null, Player.MEDIA_ITEM_TRANSITION_REASON_AUTO)

        signals.rebuffer()
        clock.advance(1_000L)
        signals.rebuffer()
        assertEquals(PlaybackStabilityProfile.Stable, signals.requestedProfile())
    }
}
