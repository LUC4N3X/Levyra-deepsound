package com.luc4n3x.levyra.player

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
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

    private class ScriptedSignals(
        private val script: ArrayDeque<PlaybackStabilityProfile>
    ) : PlaybackStabilityProfileSource {
        var calls = 0
        override fun requestedProfile(): PlaybackStabilityProfile {
            calls += 1
            return if (script.isNotEmpty()) script.removeFirst() else PlaybackStabilityProfile.Normal
        }
    }

    private class FakeTimeline(
        private val mediaItems: List<MediaItem>,
        private val periodsPerWindow: Int
    ) : Timeline() {
        override fun getWindowCount(): Int = mediaItems.size
        override fun getPeriodCount(): Int = mediaItems.size * periodsPerWindow

        override fun getWindow(
            windowIndex: Int,
            window: Timeline.Window,
            defaultPositionProjectionUs: Long
        ): Timeline.Window {
            val item = mediaItems[windowIndex]
            val uid = "w$windowIndex"
            window.set(
                uid,
                item,
                null,
                0L,
                0L,
                0L,
                true,
                false,
                item.liveConfiguration,
                0L,
                1_000_000L,
                windowIndex * periodsPerWindow,
                windowIndex * periodsPerWindow + periodsPerWindow - 1,
                0L
            )
            return window
        }

        override fun getPeriod(periodIndex: Int, period: Timeline.Period, setIds: Boolean): Timeline.Period {
            val windowIndex = periodIndex / periodsPerWindow
            val uid = "p$periodIndex"
            period.set(uid, uid, windowIndex, 1_000_000L, 0L)
            return period
        }

        override fun getIndexOfPeriod(uid: Any): Int {
            val index = uid.toString().removePrefix("p").toIntOrNull() ?: return C.INDEX_UNSET
            return if (index in 0 until getPeriodCount()) index else C.INDEX_UNSET
        }

        override fun getUidOfPeriod(periodIndex: Int): Any = "p$periodIndex"
    }

    private class RecordingLoadControl(private val allocator: Allocator) : LoadControl {
        var tracksSelectedCalls = 0
        var onReleasedCalls = 0
        var shouldContinueLoadingCalls = 0
        var shouldContinuePreloadingCalls = 0

        override fun getAllocator(playerId: PlayerId): Allocator = allocator

        override fun onPrepared(playerId: PlayerId) {
        }

        override fun onStopped(playerId: PlayerId) {
        }

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

    private fun parameters(
        timeline: Timeline,
        periodUid: String,
        playerId: PlayerId = PlayerId.UNSET
    ) = LoadControl.Parameters(
        playerId,
        timeline,
        MediaSource.MediaPeriodId(periodUid),
        0L,
        0L,
        1f,
        true,
        false,
        0L,
        0L
    )

    private fun mediaItem(id: String): MediaItem = MediaItem.Builder().setMediaId(id).build()

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

    @Test
    fun sameMediaItemAcrossPeriodsDoesNotResampleProfile() {
        val normal = recordingControl()
        val stable = recordingControl()
        val signals = ScriptedSignals(ArrayDeque(listOf(PlaybackStabilityProfile.Stable)))
        val control = AdaptiveStabilityLoadControl(normal, stable, signals)
        val timeline = FakeTimeline(listOf(mediaItem("m1")), periodsPerWindow = 2)
        control.onTracksSelected(parameters(timeline, "p0"), TrackGroupArray.EMPTY, emptyTrackSelections())
        assertEquals(PlaybackStabilityProfile.Stable, control.activeProfile)
        control.onTracksSelected(parameters(timeline, "p1"), TrackGroupArray.EMPTY, emptyTrackSelections())
        assertEquals(1, signals.calls)
        assertEquals(PlaybackStabilityProfile.Stable, control.activeProfile)
    }

    @Test
    fun reselectionOfSameItemDoesNotResampleProfile() {
        val normal = recordingControl()
        val stable = recordingControl()
        val signals = ScriptedSignals(ArrayDeque(listOf(PlaybackStabilityProfile.Stable)))
        val control = AdaptiveStabilityLoadControl(normal, stable, signals)
        val timeline = FakeTimeline(listOf(mediaItem("m1")), periodsPerWindow = 1)
        control.onTracksSelected(parameters(timeline, "p0"), TrackGroupArray.EMPTY, emptyTrackSelections())
        control.onTracksSelected(parameters(timeline, "p0"), TrackGroupArray.EMPTY, emptyTrackSelections())
        assertEquals(1, signals.calls)
        assertEquals(PlaybackStabilityProfile.Stable, control.activeProfile)
    }

    @Test
    fun newMediaItemResamplesRequestedProfile() {
        val normal = recordingControl()
        val stable = recordingControl()
        val signals = ScriptedSignals(ArrayDeque(listOf(PlaybackStabilityProfile.Stable, PlaybackStabilityProfile.Normal)))
        val control = AdaptiveStabilityLoadControl(normal, stable, signals)
        val timeline = FakeTimeline(listOf(mediaItem("m1"), mediaItem("m2")), periodsPerWindow = 1)
        control.onTracksSelected(parameters(timeline, "p0"), TrackGroupArray.EMPTY, emptyTrackSelections())
        assertEquals(PlaybackStabilityProfile.Stable, control.activeProfile)
        control.onTracksSelected(parameters(timeline, "p1"), TrackGroupArray.EMPTY, emptyTrackSelections())
        assertEquals(2, signals.calls)
        assertEquals(PlaybackStabilityProfile.Normal, control.activeProfile)
    }

    @Test
    fun normalToStableAppliesOnlyOnNextMediaItem() {
        val normal = recordingControl()
        val stable = recordingControl()
        val signals = ScriptedSignals(ArrayDeque(listOf(PlaybackStabilityProfile.Normal, PlaybackStabilityProfile.Stable)))
        val control = AdaptiveStabilityLoadControl(normal, stable, signals)
        val timeline = FakeTimeline(listOf(mediaItem("m1"), mediaItem("m2")), periodsPerWindow = 1)
        control.onTracksSelected(parameters(timeline, "p0"), TrackGroupArray.EMPTY, emptyTrackSelections())
        assertEquals(PlaybackStabilityProfile.Normal, control.activeProfile)
        control.onTracksSelected(parameters(timeline, "p0"), TrackGroupArray.EMPTY, emptyTrackSelections())
        assertEquals(1, signals.calls)
        assertEquals(PlaybackStabilityProfile.Normal, control.activeProfile)
        control.onTracksSelected(parameters(timeline, "p1"), TrackGroupArray.EMPTY, emptyTrackSelections())
        assertEquals(2, signals.calls)
        assertEquals(PlaybackStabilityProfile.Stable, control.activeProfile)
    }

    @Test
    fun stableToNormalAppliesOnlyOnNextMediaItem() {
        val normal = recordingControl()
        val stable = recordingControl()
        val signals = ScriptedSignals(ArrayDeque(listOf(PlaybackStabilityProfile.Stable, PlaybackStabilityProfile.Normal)))
        val control = AdaptiveStabilityLoadControl(normal, stable, signals)
        val timeline = FakeTimeline(listOf(mediaItem("m1"), mediaItem("m2")), periodsPerWindow = 1)
        control.onTracksSelected(parameters(timeline, "p0"), TrackGroupArray.EMPTY, emptyTrackSelections())
        assertEquals(PlaybackStabilityProfile.Stable, control.activeProfile)
        control.onTracksSelected(parameters(timeline, "p1"), TrackGroupArray.EMPTY, emptyTrackSelections())
        assertEquals(PlaybackStabilityProfile.Normal, control.activeProfile)
        assertEquals(2, signals.calls)
    }

    @Test
    fun profileMappingIsBounded() {
        val normal = recordingControl()
        val stable = recordingControl()
        val control = AdaptiveStabilityLoadControl(normal, stable, FixedProfileSignals(PlaybackStabilityProfile.Normal))
        val timeline = FakeTimeline((1..12).map { mediaItem("m$it") }, periodsPerWindow = 1)
        (0..11).forEach { index ->
            control.onTracksSelected(parameters(timeline, "p$index"), TrackGroupArray.EMPTY, emptyTrackSelections())
        }
        assertTrue(control.trackedMediaItemCount() <= 8)
    }

    @Test
    fun onStoppedClearsProfileMapping() {
        val normal = recordingControl()
        val stable = recordingControl()
        val control = AdaptiveStabilityLoadControl(normal, stable, FixedProfileSignals(PlaybackStabilityProfile.Normal))
        val timeline = FakeTimeline(listOf(mediaItem("m1"), mediaItem("m2")), periodsPerWindow = 1)
        control.onTracksSelected(parameters(timeline, "p0"), TrackGroupArray.EMPTY, emptyTrackSelections())
        control.onTracksSelected(parameters(timeline, "p1"), TrackGroupArray.EMPTY, emptyTrackSelections())
        assertEquals(2, control.trackedMediaItemCount())
        control.onStopped(PlayerId.UNSET)
        assertEquals(0, control.trackedMediaItemCount())
    }

    @Test
    fun onReleasedClearsProfileMapping() {
        val normal = recordingControl()
        val stable = recordingControl()
        val control = AdaptiveStabilityLoadControl(normal, stable, FixedProfileSignals(PlaybackStabilityProfile.Normal))
        val timeline = FakeTimeline(listOf(mediaItem("m1"), mediaItem("m2")), periodsPerWindow = 1)
        control.onTracksSelected(parameters(timeline, "p0"), TrackGroupArray.EMPTY, emptyTrackSelections())
        control.onTracksSelected(parameters(timeline, "p1"), TrackGroupArray.EMPTY, emptyTrackSelections())
        assertEquals(2, control.trackedMediaItemCount())
        control.onReleased(PlayerId.UNSET)
        assertEquals(0, control.trackedMediaItemCount())
    }

    @Test
    fun twoWindowsWithSameMediaItemSampleProfileIndependently() {
        val normal = recordingControl()
        val stable = recordingControl()
        val signals = ScriptedSignals(ArrayDeque(listOf(PlaybackStabilityProfile.Normal, PlaybackStabilityProfile.Stable)))
        val control = AdaptiveStabilityLoadControl(normal, stable, signals)
        val timeline = FakeTimeline(listOf(mediaItem("m1"), mediaItem("m1")), periodsPerWindow = 1)

        control.onTracksSelected(parameters(timeline, "p0"), TrackGroupArray.EMPTY, emptyTrackSelections())
        assertEquals(PlaybackStabilityProfile.Normal, control.activeProfile)

        control.onTracksSelected(parameters(timeline, "p1"), TrackGroupArray.EMPTY, emptyTrackSelections())
        assertEquals(PlaybackStabilityProfile.Stable, control.activeProfile)
        assertEquals(2, signals.calls)
    }

    @Test
    fun preloadingNextItemDoesNotChangeActiveProfileOfCurrentItem() {
        val normal = recordingControl()
        val stable = recordingControl()
        val signals = ScriptedSignals(ArrayDeque(listOf(PlaybackStabilityProfile.Normal, PlaybackStabilityProfile.Stable)))
        val control = AdaptiveStabilityLoadControl(normal, stable, signals)
        val itemA = mediaItem("mA")
        val itemB = mediaItem("mB")
        val timeline = FakeTimeline(listOf(itemA, itemB), periodsPerWindow = 1)

        // Item A becomes current and activates Normal
        control.onMediaItemTransition(itemA, Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED)
        control.onTracksSelected(parameters(timeline, "p0"), TrackGroupArray.EMPTY, emptyTrackSelections())
        assertEquals(PlaybackStabilityProfile.Normal, control.activeProfile)

        // Item B is preloaded in the background (tracks selected) while Item A is still playing
        control.onTracksSelected(parameters(timeline, "p1"), TrackGroupArray.EMPTY, emptyTrackSelections())
        // Active profile MUST remain Normal for Item A!
        assertEquals(PlaybackStabilityProfile.Normal, control.activeProfile)

        // When playback transitions to Item B, it activates its profile (Stable)
        control.onMediaItemTransition(itemB, Player.MEDIA_ITEM_TRANSITION_REASON_AUTO)
        assertEquals(PlaybackStabilityProfile.Stable, control.activeProfile)
    }
}
