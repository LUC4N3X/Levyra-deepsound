package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SamplesStateIsolationTest {
    @Test
    fun publishingSamplesDoesNotReplaceHomeMusicVideos() {
        val homeVideo = track("home-video", "Music video")
        val sample = track("sample", "Vertical sample")
        val initial = LevyraUiState(exploreVideos = listOf(homeVideo))

        val published = initial.withPublishedSamples(listOf(sample))

        assertEquals(listOf(homeVideo), published.exploreVideos)
        assertEquals(listOf(sample), published.exploreSamples)
    }

    @Test
    fun pausedRestoreDefersPlaybackSideEffects() {
        assertFalse(shouldDispatchPlaybackStartSideEffects(startPaused = true))
    }

    private fun track(id: String, title: String): Track = Track(
        id = id,
        title = title,
        artist = "Artist",
        album = "Album",
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "https://www.youtube.com/watch?v=abcdefghijk",
        thumbnailUrl = "https://example.test/$id.jpg",
        largeThumbnailUrl = "https://example.test/$id-large.jpg",
        source = "Test",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 70,
        cacheScore = 70,
        accentStart = 0,
        accentEnd = 0
    )
}
