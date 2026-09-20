package com.luc4n3x.levyra.player

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackTileProjectionTest {

    @Test
    fun playingProjectsActivePauseAction() {
        val projection = playbackTileProjection(
            isPlaying = true,
            playWhenReady = true,
            playbackState = Player.STATE_READY,
            mediaItemCount = 3
        )

        assertEquals(PlaybackTileProjectionKind.Active, projection.kind)
        assertEquals(PlaybackTileAction.Pause, projection.action)
        assertEquals(PlaybackTileDescription.Playing, projection.description)
    }

    @Test
    fun bufferingAfterTapStillProjectsActive() {
        val projection = playbackTileProjection(
            isPlaying = false,
            playWhenReady = true,
            playbackState = Player.STATE_BUFFERING,
            mediaItemCount = 2
        )

        assertEquals(PlaybackTileProjectionKind.Active, projection.kind)
        assertEquals(PlaybackTileAction.Pause, projection.action)
        assertEquals(PlaybackTileDescription.Playing, projection.description)
    }

    @Test
    fun pausedWithResumableQueueProjectsResumeAction() {
        val projection = playbackTileProjection(
            isPlaying = false,
            playWhenReady = false,
            playbackState = Player.STATE_READY,
            mediaItemCount = 5
        )

        assertEquals(PlaybackTileProjectionKind.Inactive, projection.kind)
        assertEquals(PlaybackTileAction.Resume, projection.action)
        assertEquals(PlaybackTileDescription.Paused, projection.description)
    }

    @Test
    fun noResumableSessionProjectsOpenAppWithoutAutoplay() {
        val projection = playbackTileProjection(
            isPlaying = false,
            playWhenReady = false,
            playbackState = Player.STATE_IDLE,
            mediaItemCount = 0
        )

        assertEquals(PlaybackTileProjectionKind.Inactive, projection.kind)
        assertEquals(PlaybackTileAction.OpenApp, projection.action)
        assertEquals(PlaybackTileDescription.Idle, projection.description)
    }

    @Test
    fun endedWithPlayWhenReadyDoesNotProjectAsPlaying() {
        val projection = playbackTileProjection(
            isPlaying = false,
            playWhenReady = true,
            playbackState = Player.STATE_ENDED,
            mediaItemCount = 3
        )

        assertEquals(PlaybackTileProjectionKind.Inactive, projection.kind)
        assertEquals(PlaybackTileAction.OpenApp, projection.action)
        assertEquals(PlaybackTileDescription.Idle, projection.description)
    }

    @Test
    fun idleStateWithQueueDoesNotProjectAsResumable() {
        val projection = playbackTileProjection(
            isPlaying = false,
            playWhenReady = false,
            playbackState = Player.STATE_IDLE,
            mediaItemCount = 3
        )

        assertEquals(PlaybackTileProjectionKind.Inactive, projection.kind)
        assertEquals(PlaybackTileAction.OpenApp, projection.action)
        assertEquals(PlaybackTileDescription.Idle, projection.description)
    }
}
