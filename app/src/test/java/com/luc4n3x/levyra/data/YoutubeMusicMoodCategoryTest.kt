package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeMusicMoodCategoryTest {
    private val repository = YoutubeMusicRepository()

    @Test
    fun audioVariantReplacesVideoVariantWithoutMovingTheRecording() {
        val video = track(
            id = "video123456",
            title = "Midnight Drive",
            videoType = "MUSIC_VIDEO_TYPE_OMV",
            artwork = "https://i.ytimg.com/vi/video123456/hqdefault.jpg"
        )
        val another = track(
            id = "another12345",
            title = "Second Song",
            videoType = "MUSIC_VIDEO_TYPE_ATV",
            artwork = "https://lh3.googleusercontent.com/second=w544-h544"
        )
        val audio = track(
            id = "audio1234567",
            title = "Midnight Drive",
            videoType = "MUSIC_VIDEO_TYPE_ATV",
            artwork = "https://lh3.googleusercontent.com/midnight=w544-h544"
        )

        val result = repository.stabilizeMoodCategoryTracks(listOf(video, another, audio), 10)

        assertEquals(listOf("audio1234567", "another12345"), result.map { it.id })
        assertEquals("https://lh3.googleusercontent.com/midnight=w544-h544", result.first().thumbnailUrl)
    }

    @Test
    fun syntheticYoutubeFallbackIsNotShownAsAudioArtwork() {
        val audio = track(
            id = "audio1234567",
            title = "Midnight Drive",
            videoType = "MUSIC_VIDEO_TYPE_ATV",
            artwork = "https://i.ytimg.com/vi/audio1234567/hqdefault.jpg"
        )

        val result = repository.stabilizeMoodCategoryTracks(listOf(audio), 10).single()

        assertTrue(result.thumbnailUrl.isBlank())
        assertTrue(result.largeThumbnailUrl.isBlank())
    }

    private fun track(
        id: String,
        title: String,
        videoType: String,
        artwork: String
    ): Track = Track(
        id = id,
        title = title,
        artist = "Test Artist",
        album = "Test Album",
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "https://www.youtube.com/watch?v=$id",
        thumbnailUrl = artwork,
        largeThumbnailUrl = artwork,
        source = "YouTube Music",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 50,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0,
        videoType = videoType
    )
}
