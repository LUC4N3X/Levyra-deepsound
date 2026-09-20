package com.luc4n3x.levyra.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackTileProjectionTest {

    @Test
    fun playingProjectsActivePauseAction() {
        val projection = playbackTileProjection(
            isPlaying = true,
            playWhenReady = true,
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
            mediaItemCount = 0
        )

        assertEquals(PlaybackTileProjectionKind.Inactive, projection.kind)
        assertEquals(PlaybackTileAction.OpenApp, projection.action)
        assertEquals(PlaybackTileDescription.Idle, projection.description)
    }
}
