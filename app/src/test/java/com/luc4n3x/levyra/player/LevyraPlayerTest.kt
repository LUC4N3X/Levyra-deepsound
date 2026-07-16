package com.luc4n3x.levyra.player

import com.luc4n3x.levyra.data.TrackJson
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.viewmodel.playbackIdentity
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
    fun trackJsonPersistsYoutubeLoudnessMetadata() {
        val json = TrackJson.toJson(
            track(streamUrl = "").copy(
                youtubeLoudnessDb = 5.5f,
                youtubePerceptualLoudnessDb = 3.25f
            )
        )

        val restored = TrackJson.fromJson(json)

        assertEquals(5.5f, restored?.youtubeLoudnessDb)
        assertEquals(3.25f, restored?.youtubePerceptualLoudnessDb)
    }

    @Test
    fun playbackIdentitySeparatesTracksWithBlankIds() {
        val first = track(streamUrl = "").copy(
            id = "",
            title = "First song",
            artist = "First artist",
            videoUrl = ""
        )
        val second = track(streamUrl = "").copy(
            id = "",
            title = "Second song",
            artist = "Second artist",
            videoUrl = ""
        )

        assertEquals("first artist|first song", playbackIdentity(first))
        assertEquals("second artist|second song", playbackIdentity(second))
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

    @Test
    fun normalizationUsesPerceptualLoudnessBeforeStandardLoudness() {
        val processor = NormalizationAudioProcessor()

        processor.setYoutubeLoudness(6.0f, 3.0f)

        assertEquals(0.7079f, processor.metadataGain() ?: 0.0f, 0.001f)
    }

    @Test
    fun normalizationConvertsPositiveLoudnessToAttenuation() {
        val processor = NormalizationAudioProcessor()

        processor.setYoutubeLoudness(6.0f, null)

        assertEquals(0.5012f, processor.metadataGain() ?: 0.0f, 0.001f)
    }

    @Test
    fun normalizationDoesNotBoostNegativeYoutubeLoudness() {
        val processor = NormalizationAudioProcessor()

        processor.setYoutubeLoudness(-4.0f, null)

        assertEquals(1.0f, processor.metadataGain() ?: 0.0f, 0.0f)
    }
}
