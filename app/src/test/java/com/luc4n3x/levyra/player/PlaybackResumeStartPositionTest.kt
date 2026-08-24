package com.luc4n3x.levyra.player

import com.luc4n3x.levyra.viewmodel.resumeStartPositionMs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Cold-resume regression coverage.
 *
 * The reported failure was: a track saved at ~46s showed the saved timeline after relaunch, but
 * pressing play restarted real Media3 playback from 0. The position survived every ViewModel hop
 * and was then handed to the session as two separate commands — `setMediaItem(item, 0)` followed
 * by `seekTo(46_000)`. `MediaSession.Callback.onAddMediaItems` resolves items asynchronously, so
 * the session applied the resolved items with `startPositionMs = 0` *after* the seek and discarded
 * it.
 *
 * These tests exercise the real decision chain that now carries the position and use
 * [SessionUnderAsyncItemResolution] — a model of that session behaviour — so a regression back to
 * "start at 0 then seek" fails here instead of only on a device.
 */
class PlaybackResumeStartPositionTest {

    /**
     * Models the observed Media3 session behaviour: media items set through a controller are
     * applied asynchronously, and applying them overwrites any position requested in between.
     */
    private class SessionUnderAsyncItemResolution {
        private var appliedPositionMs = 0L
        private var stagedStartPositionMs: Long? = null
        var playWhenReady: Boolean = false
            private set

        fun setMediaItem(startPositionMs: Long, playWhenReady: Boolean) {
            stagedStartPositionMs = startPositionMs
            this.playWhenReady = playWhenReady
        }

        /** A seek issued before the staged item is applied never reaches the real timeline. */
        fun seekTo(positionMs: Long) {
            if (stagedStartPositionMs != null) return
            appliedPositionMs = positionMs
        }

        fun pause() {
            playWhenReady = false
        }

        fun resolveItems() {
            stagedStartPositionMs?.let { appliedPositionMs = it }
            stagedStartPositionMs = null
        }

        fun realPositionMs(): Long = appliedPositionMs
    }

    /** The production decision chain from a restored pending position to `setMediaItem`. */
    private fun startPositionHandedToSession(
        pendingSeekMs: Long,
        durationMs: Long,
        sameTrack: Boolean,
        activePositionMs: Long,
        recoveryReplacement: Boolean = false
    ): Long {
        val resumeMs = resumeStartPositionMs(pendingSeekMs, durationMs)
        val requested = playbackStartPositionRequest(
            sameTrack = sameTrack,
            resumePositionMs = resumeMs,
            activePositionMs = activePositionMs
        )
        return replacementStartPosition(
            sameTrack = sameTrack,
            requestedPositionMs = requested,
            activePositionMs = activePositionMs,
            durationMs = durationMs,
            allowBackwardActivePosition = sameTrack && !recoveryReplacement
        )
    }

    @Test
    fun restoredPositionReachesRealPlaybackWhenTheSessionResolvesItemsLate() {
        val session = SessionUnderAsyncItemResolution()

        val startPositionMs = startPositionHandedToSession(
            pendingSeekMs = 46_000L,
            durationMs = 270_000L,
            sameTrack = false,
            activePositionMs = 0L
        )
        session.setMediaItem(startPositionMs, playWhenReady = true)
        session.resolveItems()

        assertEquals(46_000L, startPositionMs)
        assertEquals(46_000L, session.realPositionMs())
    }

    @Test
    fun startingAtZeroAndSeekingAfterwardsLosesTheRestoredPosition() {
        val session = SessionUnderAsyncItemResolution()

        session.setMediaItem(0L, playWhenReady = true)
        session.seekTo(46_000L)
        session.resolveItems()

        assertEquals(0L, session.realPositionMs())
    }

    @Test
    fun restoredPositionSurvivesAControllerThatWasNotConnectedYet() {
        // No controller yet: the request is parked and replayed through replaceSource on connect.
        val parkedPositionMs = resumeStartPositionMs(46_000L, 270_000L)

        val startPositionMs = replacementStartPosition(
            sameTrack = false,
            requestedPositionMs = parkedPositionMs,
            activePositionMs = 0L,
            durationMs = 270_000L
        )

        assertEquals(46_000L, startPositionMs)
    }

    @Test
    fun differentTrackStartsAtZeroAndDoesNotInheritThePreviousPosition() {
        val startPositionMs = startPositionHandedToSession(
            pendingSeekMs = 0L,
            durationMs = 292_000L,
            sameTrack = false,
            activePositionMs = 199_000L
        )

        assertEquals(0L, startPositionMs)
    }

    @Test
    fun anAlreadyLoadedTrackKeepsItsLivePlayerPosition() {
        val startPositionMs = startPositionHandedToSession(
            pendingSeekMs = 46_000L,
            durationMs = 270_000L,
            sameTrack = true,
            activePositionMs = 120_000L
        )

        assertEquals(120_000L, startPositionMs)
    }

    @Test
    fun pausedRestoreKeepsThePositionWithoutStartingPlayback() {
        val session = SessionUnderAsyncItemResolution()

        val startPositionMs = startPositionHandedToSession(
            pendingSeekMs = 79_318L,
            durationMs = 292_000L,
            sameTrack = false,
            activePositionMs = 0L
        )
        session.setMediaItem(startPositionMs, playWhenReady = true)
        session.pause()
        session.resolveItems()

        assertEquals(79_318L, session.realPositionMs())
        assertFalse(session.playWhenReady)
    }

    @Test
    fun reportedPositionKeepsTheRequestedStartUntilTheSessionAppliesTheItem() {
        // Item staged with startPositionMs = 46_000, controller still masking 0.
        assertEquals(46_000L, reportedPlaybackPositionMs(0L, 46_000L, mediaItemCount = 1))
        // Session applied the item: the live position wins again.
        assertEquals(46_120L, reportedPlaybackPositionMs(46_120L, 46_000L, mediaItemCount = 1))
        // Nothing staged, or nothing loaded: report the live position unchanged.
        assertEquals(0L, reportedPlaybackPositionMs(0L, null, mediaItemCount = 1))
        assertEquals(0L, reportedPlaybackPositionMs(0L, 46_000L, mediaItemCount = 0))
    }

    @Test
    fun positionsTooEarlyOrPastTheKnownDurationAreNotRestored() {
        assertEquals(0L, resumeStartPositionMs(1_500L, 270_000L))
        assertEquals(0L, resumeStartPositionMs(0L, 270_000L))
        assertEquals(0L, resumeStartPositionMs(270_000L, 270_000L))
        assertEquals(46_000L, resumeStartPositionMs(46_000L, 270_000L))
    }
}
