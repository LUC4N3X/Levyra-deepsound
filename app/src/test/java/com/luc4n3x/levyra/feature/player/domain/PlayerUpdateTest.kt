package com.luc4n3x.levyra.feature.player.domain

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerUpdateTest {
    @Test
    fun trackRequestStartsResolutionAndLyricsLoad() {
        val track = track(id = "one", streamUrl = "")
        val result = PlayerUpdate.update(PlayerModel(), PlayerEvent.TrackRequested(track))

        assertEquals(track, result.model.currentTrack)
        assertTrue(result.model.isPlaying)
        assertTrue(result.model.isResolving)
        assertTrue(result.effects.contains(PlayerEffect.ResolveTrack(track, false)))
        assertTrue(result.effects.contains(PlayerEffect.LoadLyrics(track)))
    }

    @Test
    fun resolvedTrackStartsPlaybackWhenModelWasPlaying() {
        val unresolved = track(id = "one", streamUrl = "")
        val resolved = unresolved.copy(streamUrl = "https://cdn.levyra.test/audio.m4a")
        val result = PlayerUpdate.update(PlayerModel(currentTrack = unresolved, isPlaying = true, isResolving = true), PlayerEvent.ResolveSucceeded(resolved))

        assertEquals(resolved, result.model.currentTrack)
        assertTrue(result.model.isPlaying)
        assertFalse(result.model.isResolving)
        assertTrue(result.effects.contains(PlayerEffect.StartPlayback(resolved)))
    }

    @Test
    fun pausePersistsSnapshotAndStopsPlayback() {
        val current = track(id = "one", streamUrl = "https://cdn.levyra.test/audio.m4a")
        val result = PlayerUpdate.update(PlayerModel(currentTrack = current, isPlaying = true, positionMs = 9000L), PlayerEvent.PauseClicked)

        assertFalse(result.model.isPlaying)
        assertTrue(result.effects.contains(PlayerEffect.PausePlayback))
        assertTrue(result.effects.contains(PlayerEffect.PersistSnapshot(current, 9000L, false)))
    }

    @Test
    fun failedResolutionNormalizesEmptyErrorMessage() {
        val result = PlayerUpdate.update(PlayerModel(isPlaying = true, isResolving = true), PlayerEvent.ResolveFailed("   "))

        assertFalse(result.model.isPlaying)
        assertFalse(result.model.isResolving)
        assertEquals("Riproduzione non riuscita", result.model.errorMessage)
        assertTrue(result.effects.contains(PlayerEffect.ReportPlaybackError("Riproduzione non riuscita")))
    }

    @Test
    fun videoModeChangeClearsModelStreamUrlsBeforeResolving() {
        val resolvedAudioTrack = track(id = "one", streamUrl = "https://cdn.levyra.test/audio.m4a").copy(
            videoStreamUrl = "https://cdn.levyra.test/video.m3u8"
        )
        val result = PlayerUpdate.update(
            PlayerModel(
                currentTrack = resolvedAudioTrack,
                isPlaying = true,
                isResolving = false,
                isVideoMode = false
            ),
            PlayerEvent.VideoModeChanged(true)
        )
        val unresolvedTrack = resolvedAudioTrack.copy(streamUrl = "", videoStreamUrl = "")

        assertEquals(unresolvedTrack, result.model.currentTrack)
        assertTrue(result.model.isVideoMode)
        assertTrue(result.model.isResolving)
        assertTrue(result.effects.contains(PlayerEffect.ResolveTrack(unresolvedTrack, true)))

        val replayDuringResolution = PlayerUpdate.update(result.model, PlayerEvent.PlayClicked)

        assertTrue(replayDuringResolution.model.isResolving)
        assertFalse(replayDuringResolution.effects.any { it is PlayerEffect.StartPlayback })
        assertFalse(replayDuringResolution.effects.any { it is PlayerEffect.ResolveTrack })
    }

    private fun track(id: String, streamUrl: String): Track = Track(
        id = id,
        title = "Track $id",
        artist = "Levyra Test Artist",
        album = "Levyra Test Album",
        durationMs = 180000L,
        streamUrl = streamUrl,
        videoUrl = "https://youtube.com/watch?v=$id",
        thumbnailUrl = "https://cdn.levyra.test/$id.jpg",
        largeThumbnailUrl = "https://cdn.levyra.test/${id}_large.jpg",
        source = "test",
        moodTags = setOf("test"),
        energy = 70,
        vocal = 50,
        replayScore = 90,
        cacheScore = 80,
        accentStart = 0xFF00E5FF.toInt(),
        accentEnd = 0xFF7C4DFF.toInt()
    )
}
