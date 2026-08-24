package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackResumeUiPolicyTest {
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
