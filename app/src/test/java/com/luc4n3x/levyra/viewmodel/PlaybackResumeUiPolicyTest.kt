package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.Track
import androidx.work.WorkInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackResumeUiPolicyTest {
    @Test
    fun pausedQueuePublicationRetainsResolvedPlaybackPayload() {
        val playing = track("video123456").copy(videoStreamUrl = "https://rr.example.test/video")
        val stored = playing.copy(streamUrl = "", videoStreamUrl = "")

        val synchronized = playbackTrackAfterQueueSync(playing, stored)

        assertSame(playing, synchronized)
        assertEquals(playing.streamUrl, synchronized?.streamUrl)
        assertEquals(playing.videoStreamUrl, synchronized?.videoStreamUrl)
    }

    @Test
    fun restoredQueueKeepsItsSelectedTrackWithoutAStreamUrl() {
        val restored = track("video123456").copy(streamUrl = "")

        assertSame(restored, playbackTrackAfterQueueSync(null, restored))
    }

    @Test
    fun queueSelectionChangeDoesNotReusePreviousTrackStream() {
        val previous = track("video123456")
        val selected = track("other123456").copy(streamUrl = "")

        assertSame(selected, playbackTrackAfterQueueSync(previous, selected))
    }

    @Test
    fun sameMetadataWithDifferentIdsDoesNotReusePreviousStream() {
        val previous = track("first").copy(videoUrl = "")
        val selected = track("second").copy(videoUrl = "", streamUrl = "")

        assertSame(selected, playbackTrackAfterQueueSync(previous, selected))
    }

    @Test
    fun exitBeforePlaybackPreservesRestoredPosition() {
        assertEquals(122_342L, playbackPositionForExit(0L, 122_342L))
    }

    @Test
    fun exitAfterPlaybackUsesActualPlayerPosition() {
        assertEquals(130_000L, playbackPositionForExit(130_000L, 122_342L))
        assertEquals(0L, playbackPositionForExit(0L, 0L))
    }

    @Test
    fun exitDuringResolutionDoesNotSavePreviousPlayerPosition() {
        assertEquals(0L, playbackPositionForExit(130_000L, 0L, 0L, isResolving = true))
        assertEquals(42_000L, playbackPositionForExit(130_000L, 42_000L, 0L, isResolving = true))
        assertEquals(42_000L, playbackPositionForExit(null, 0L, 42_000L))
    }

    @Test
    fun exitKeepsUnfinishedDownloadsAlive() {
        for (state in listOf(WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED)) {
            assertFalse(canTerminateProcessForExit(listOf(WorkInfo.State.SUCCEEDED, state)))
        }
        assertTrue(canTerminateProcessForExit(emptyList()))
        assertTrue(canTerminateProcessForExit(listOf(WorkInfo.State.SUCCEEDED, WorkInfo.State.FAILED, WorkInfo.State.CANCELLED)))
    }

    @Test
    fun unresolvedTrackAcceptsFreshQueueMetadata() {
        val unresolved = track("video123456").copy(streamUrl = "")
        val updated = unresolved.copy(title = "Updated song")

        assertSame(updated, playbackTrackAfterQueueSync(unresolved, updated))
    }

    @Test
    fun clearedQueueDoesNotKeepThePreviousPlayingTrack() {
        assertNull(playbackTrackAfterQueueSync(track("video123456"), null))
    }

    @Test
    fun sameTrackResolutionKeepsKnownTimelineWithoutOverridingPlayState() {
        val track = track("video123456")
        val previous = LevyraUiState(
            currentTrack = track,
            positionMs = 93_000L,
            bufferedPositionMs = 108_000L,
            durationMs = 180_000L,
            isPlaying = true
        )
        val resolving = previous.copy(
            currentTrack = track.copy(streamUrl = ""),
            isResolving = true,
            isPlaying = false,
            positionMs = 0L,
            bufferedPositionMs = 0L,
            durationMs = 0L
        )

        val stable = stabilizeResolvingPlaybackUi(previous, resolving)

        assertEquals(93_000L, stable.positionMs)
        assertEquals(108_000L, stable.bufferedPositionMs)
        assertEquals(180_000L, stable.durationMs)
        assertFalse(stable.isPlaying)
        assertTrue(stable.isResolving)
    }

    @Test
    fun sameTrackResolutionKeepsFreshNonZeroTimelineValues() {
        val track = track("video123456")
        val previous = LevyraUiState(
            currentTrack = track,
            positionMs = 93_000L,
            bufferedPositionMs = 108_000L,
            durationMs = 180_000L
        )
        val resolving = previous.copy(
            isResolving = true,
            positionMs = 94_000L,
            bufferedPositionMs = 110_000L,
            durationMs = 181_000L
        )

        assertEquals(resolving, stabilizeResolvingPlaybackUi(previous, resolving))
    }

    @Test
    fun differentTrackResolutionDoesNotReusePreviousPlaybackState() {
        val previous = LevyraUiState(
            currentTrack = track("video123456"),
            positionMs = 93_000L,
            bufferedPositionMs = 108_000L,
            durationMs = 180_000L,
            isPlaying = true
        )
        val resolving = LevyraUiState(
            currentTrack = track("other123456").copy(title = "Other Song"),
            isResolving = true,
            isPlaying = false,
            positionMs = 0L,
            bufferedPositionMs = 0L,
            durationMs = 175_000L
        )

        val stable = stabilizeResolvingPlaybackUi(previous, resolving)

        assertEquals(0L, stable.positionMs)
        assertEquals(0L, stable.bufferedPositionMs)
        assertEquals(175_000L, stable.durationMs)
        assertFalse(stable.isPlaying)
    }

    @Test
    fun completedResolutionPublishesFreshPlaybackState() {
        val track = track("video123456")
        val previous = LevyraUiState(
            currentTrack = track,
            positionMs = 93_000L,
            bufferedPositionMs = 108_000L,
            durationMs = 180_000L,
            isPlaying = true
        )
        val resolved = previous.copy(
            isResolving = false,
            isPlaying = false,
            positionMs = 0L,
            bufferedPositionMs = 0L,
            durationMs = 180_000L
        )

        assertEquals(resolved, stabilizeResolvingPlaybackUi(previous, resolved))
    }

    @Test
    fun stabilizerRetainsKnownTimelineAcrossCollectorRestart() {
        val track = track("video123456")
        val known = LevyraUiState(
            currentTrack = track,
            positionMs = 93_000L,
            bufferedPositionMs = 108_000L,
            durationMs = 180_000L,
            isPlaying = true
        )
        val resolving = known.copy(
            currentTrack = track.copy(streamUrl = ""),
            isResolving = true,
            isPlaying = false,
            positionMs = 0L,
            bufferedPositionMs = 0L,
            durationMs = 0L
        )
        val stabilizer = ResolvingPlaybackUiStabilizer(known)

        val beforeRestart = stabilizer.apply(resolving)
        val afterRestart = stabilizer.apply(resolving)

        assertEquals(93_000L, beforeRestart.positionMs)
        assertEquals(108_000L, beforeRestart.bufferedPositionMs)
        assertEquals(180_000L, beforeRestart.durationMs)
        assertEquals(93_000L, afterRestart.positionMs)
        assertEquals(108_000L, afterRestart.bufferedPositionMs)
        assertEquals(180_000L, afterRestart.durationMs)
        assertFalse(afterRestart.isPlaying)
    }

    private fun track(id: String) = Track(
        id = id,
        title = "Song",
        artist = "Artist",
        album = "Album",
        durationMs = 180_000L,
        streamUrl = "https://rr.example.test/audio",
        videoUrl = "https://www.youtube.com/watch?v=$id",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "YouTube Music",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 80,
        cacheScore = 80,
        accentStart = 0,
        accentEnd = 0
    )
}
