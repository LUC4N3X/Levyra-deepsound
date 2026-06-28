package com.luc4n3x.levyra.player

import com.luc4n3x.levyra.data.TrackJson
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.viewmodel.youtubePlayableTrack
import org.junit.Assert.assertEquals
import org.junit.Test

class LevyraPlayerTest {
    @Test
    fun trackJsonDoesNotPersistTransientStreamUrl() {
        val json = TrackJson.toJson(track(streamUrl = "https://rr.example.test/audio?sig=abc"))
        val restored = TrackJson.fromJson(json)

        assertEquals("", restored?.streamUrl)
    }

    @Test
    fun youtubePlayableTrackUsesVideoIdFromChartVideoUrl() {
        val chartTrack = track(streamUrl = "").copy(
            id = "chart-abc",
            videoUrl = "https://www.youtube.com/watch?v=video-123"
        )

        val playable = youtubePlayableTrack(chartTrack)

        assertEquals("video-123", playable?.id)
        assertEquals("https://www.youtube.com/watch?v=video-123", playable?.videoUrl)
    }

    private fun track(streamUrl: String): Track = Track(
        id = "video-123",
        title = "Song",
        artist = "Artist",
        album = "Album",
        durationMs = 180_000L,
        streamUrl = streamUrl,
        videoUrl = "https://www.youtube.com/watch?v=video-123",
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
