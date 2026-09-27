package com.luc4n3x.levyra.player

import androidx.media3.common.Timeline
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.analytics.PlayerId
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.exoplayer.upstream.Allocator
import androidx.media3.exoplayer.upstream.DefaultAllocator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveStabilityLoadControlTest {

    private class FixedProfileSignals(private val profile: PlaybackStabilityProfile) : PlaybackStabilityProfileSource {
        override fun requestedProfile(): PlaybackStabilityProfile = profile
    }

    private class RecordingLoadControl(private val allocator: Allocator) : LoadControl {
        var tracksSelectedCalls = 0
        var onReleasedCalls = 0
        var shouldContinueLoadingCalls = 0
        var shouldContinuePreloadingCalls = 0

        override fun getAllocator(playerId: PlayerId): Allocator = allocator

        override fun shouldContinuePreloading(
            playerId: PlayerId,
            timeline: Timeline,
            mediaPeriodId: MediaSource.MediaPeriodId,
            bufferedDurationUs: Long
        ): Boolean {
            shouldContinuePreloadingCalls += 1
            return true
        }

        override fun onTracksSelected(
            parameters: LoadControl.Parameters,
            trackGroups: TrackGroupArray,
            trackSelections: Array<ExoTrackSelection?>
        ) {
            tracksSelectedCalls += 1
        }

        override fun onReleased(playerId: PlayerId) {
            onReleasedCalls += 1
        }

        override fun shouldContinueLoading(parameters: LoadControl.Parameters): Boolean {
            shouldContinueLoadingCalls += 1
            return true
        }
    }

    private fun parameters(playerId: PlayerId = PlayerId.UNSET) = LoadControl.Parameters(
        playerId,
        Timeline.EMPTY,
        MediaSource.MediaPeriodId("test-period"),
        0L,
        0L,
        1f,
        true,
        false,
        0L,
        0L
    )

    private fun emptyTrackSelections(): Array<ExoTrackSelection?> = arrayOfNulls(0)

    private fun recordingControl(): RecordingLoadControl = RecordingLoadControl(DefaultAllocator(true, 64 * 1024))

    @Test
    fun startsOnNormalProfile() {
        val control = AdaptiveStabilityLoadControl(
            normal = recordingControl(),
            stable = recordingControl(),
            signals = FixedProfileSignals(PlaybackStabilityProfile.Stable)
        )
        assertFalse(control.activeProfile == PlaybackStabilityProfile.Stable)
    }

    @Test
    fun getAllocatorAlwaysReturnsNormalAllocator() {
        val normal = recordingControl()
        val stable = recordingControl()
        val control = AdaptiveStabilityLoadControl(normal, stable, FixedProfileSignals(PlaybackStabilityProfile.Stable))
        val playerId = PlayerId.UNSET
        val allocatorBefore = control.getAllocator(playerId)
        control.onTracksSelected(parameters(playerId), TrackGroupArray.EMPTY, emptyTrackSelections())
        val allocatorAfter = control.getAllocator(playerId)
        assertSame(normal.getAllocator(playerId), allocatorBefore)
        assertSame(normal.getAllocator(playerId), allocatorAfter)
    }

    @Test
    fun onTracksSelectedBroadcastsToBothDelegatesRegardlessOfActiveProfile() {
        val normal = recordingControl()
        val stable = recordingControl()
        val control = AdaptiveStabilityLoadControl(normal, stable, FixedProfileSignals(PlaybackStabilityProfile.Stable))
        control.onTracksSelected(parameters(), TrackGroupArray.EMPTY, emptyTrackSelections())
        assertEquals(1, normal.tracksSelectedCalls)
        assertEquals(1, stable.tracksSelectedCalls)
    }

    @Test
    fun onReleasedBroadcastsToBothDelegates() {
        val normal = recordingControl()
        val stable = recordingControl()
        val control = AdaptiveStabilityLoadControl(normal, stable, FixedProfileSignals(PlaybackStabilityProfile.Normal))
        control.onReleased(PlayerId.UNSET)
        assertEquals(1, normal.onReleasedCalls)
        assertEquals(1, stable.onReleasedCalls)
    }

    @Test
    fun stableRequestBecomesActiveOnlyAtTracksSelectedBoundary() {
        val normal = recordingControl()
        val stable = recordingControl()
        val control = AdaptiveStabilityLoadControl(normal, stable, FixedProfileSignals(PlaybackStabilityProfile.Stable))
        control.shouldContinueLoading(parameters())
        assertEquals(1, normal.shouldContinueLoadingCalls)
        assertEquals(0, stable.shouldContinueLoadingCalls)
        control.onTracksSelected(parameters(), TrackGroupArray.EMPTY, emptyTrackSelections())
        control.shouldContinueLoading(parameters())
        assertEquals(1, normal.shouldContinueLoadingCalls)
        assertEquals(1, stable.shouldContinueLoadingCalls)
    }

    @Test
    fun normalRequestKeepsNormalProfileActive() {
        val normal = recordingControl()
        val stable = recordingControl()
        val control = AdaptiveStabilityLoadControl(normal, stable, FixedProfileSignals(PlaybackStabilityProfile.Normal))
        control.onTracksSelected(parameters(), TrackGroupArray.EMPTY, emptyTrackSelections())
        control.shouldContinueLoading(parameters())
        assertEquals(1, normal.shouldContinueLoadingCalls)
        assertEquals(0, stable.shouldContinueLoadingCalls)
    }

    @Test
    fun shouldContinuePreloadingDelegatesToActiveProfile() {
        val normal = recordingControl()
        val stable = recordingControl()
        val control = AdaptiveStabilityLoadControl(normal, stable, FixedProfileSignals(PlaybackStabilityProfile.Stable))
        val playerId = PlayerId.UNSET
        control.shouldContinuePreloading(playerId, Timeline.EMPTY, MediaSource.MediaPeriodId("p"), 0L)
        assertEquals(1, normal.shouldContinuePreloadingCalls)
        assertEquals(0, stable.shouldContinuePreloadingCalls)
        control.onTracksSelected(parameters(playerId), TrackGroupArray.EMPTY, emptyTrackSelections())
        control.shouldContinuePreloading(playerId, Timeline.EMPTY, MediaSource.MediaPeriodId("p"), 0L)
        assertEquals(1, normal.shouldContinuePreloadingCalls)
        assertEquals(1, stable.shouldContinuePreloadingCalls)
    }

    @Test
    fun stableProfileWidensBuffersWithoutBecomingUnbounded() {
        val normal = PlaybackBufferProfile(900, 24_000, 100, 300, 4_000)
        val stable = AdaptiveStabilityLoadControl.stableProfileOf(normal)
        assertTrue(stable.minBufferMs > normal.minBufferMs)
        assertTrue(stable.maxBufferMs > normal.maxBufferMs)
        assertTrue(stable.maxBufferMs < 120_000)
        assertEquals(normal.backBufferMs, stable.backBufferMs)
    }
}
