package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.data.YOUTUBE_SHORTS_SOURCE
import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SamplesPlaybackSelectionTest {
    @Test
    fun canvasSampleUsesMainPlayerForAudioOnly() {
        val clip = track("spotify-clip-ashe", YOUTUBE_SHORTS_SOURCE, "", "SHORTS")

        assertFalse(samplePlaybackVideoMode(clip))
        assertEquals(clip, selectYoutubeShortSample(listOf(clip), clip))
    }

    @Test
    fun nativeSampleKeepsMainPlayerVideoAndAudio() {
        val clip = track("shortvideo1", YOUTUBE_SHORTS_SOURCE, "https://www.youtube.com/shorts/shortvideo1", "SHORTS")

        assertTrue(samplePlaybackVideoMode(clip))
    }

    @Test
    fun canvasSelectionSurvivesAudioResolutionAddingYoutubeIdentity() {
        val clip = track("spotify-clip-ashe", YOUTUBE_SHORTS_SOURCE, "", "SHORTS")
        val resolved = clip.copy(videoUrl = "https://www.youtube.com/watch?v=shortvideo1", audioVideoId = "shortvideo1")
        val other = clip.copy(id = "spotify-clip-other")

        assertTrue(sameYoutubeShortSample(clip, resolved))
        assertFalse(sameYoutubeShortSample(clip, other))
        assertEquals(clip, selectYoutubeShortSample(listOf(other, clip), resolved))
    }

    @Test
    fun shortsWithIdenticalMetadataRemainDifferentPlaybackSelections() {
        val first = track("shortvideo1", YOUTUBE_SHORTS_SOURCE, "https://www.youtube.com/shorts/shortvideo1", "SHORTS")
        val second = track("shortvideo2", YOUTUBE_SHORTS_SOURCE, "https://www.youtube.com/shorts/shortvideo2", "SHORTS")

        assertEquals(second, selectYoutubeShortSample(listOf(first, second), second))
        assertFalse(sameYoutubeShortSample(first, second))
    }

    @Test
    fun rejectedNonShortDoesNotCaptureBeforeFollowingValidShort() {
        val ordinaryVideo = track(
            id = "ordinary001",
            source = "YouTube Music",
            videoUrl = "https://www.youtube.com/watch?v=ordinary001",
            videoType = ""
        )
        val validShort = track(
            id = "shortvideo1",
            source = YOUTUBE_SHORTS_SOURCE,
            videoUrl = "https://www.youtube.com/shorts/shortvideo1",
            videoType = "SHORTS"
        )
        var capturedSelection: Track? = null

        fun attemptPlayback(items: List<Track>, requested: Track) {
            if (capturedSelection == null) {
                capturedSelection = selectYoutubeShortSample(items, requested)
            }
        }

        attemptPlayback(listOf(ordinaryVideo), ordinaryVideo)
        assertNull(capturedSelection)

        attemptPlayback(listOf(validShort), validShort)
        assertEquals(validShort, capturedSelection)
    }

    @Test
    fun officialYoutubeMusicVideoIsNotPlayableAsAShort() {
        val musicVideo = track(
            id = "musicvideo1",
            source = "YouTube Music",
            videoUrl = "https://www.youtube.com/watch?v=musicvideo1",
            videoType = "MUSIC_VIDEO_TYPE_OMV"
        )

        assertNull(selectYoutubeShortSample(listOf(musicVideo), musicVideo))
    }

    private fun track(
        id: String,
        source: String,
        videoUrl: String,
        videoType: String
    ): Track = Track(
        id = id,
        title = "Title",
        artist = "Artist",
        album = "Album",
        durationMs = 60_000L,
        streamUrl = "",
        videoUrl = videoUrl,
        thumbnailUrl = "https://levyra.test/$id.jpg",
        largeThumbnailUrl = "https://levyra.test/${id}-large.jpg",
        source = source,
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 70,
        cacheScore = 70,
        accentStart = 0xFF00E5FF.toInt(),
        accentEnd = 0xFF2979FF.toInt(),
        videoType = videoType
    )
}
