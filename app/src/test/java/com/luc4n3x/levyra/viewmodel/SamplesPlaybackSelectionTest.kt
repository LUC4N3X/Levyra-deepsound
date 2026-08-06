package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.data.YOUTUBE_SHORTS_SOURCE
import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SamplesPlaybackSelectionTest {
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
        var sessionCaptured = false

        fun attemptPlayback(items: List<Track>, requested: Track) {
            val selected = selectYoutubeShortSample(items, requested) ?: return
            if (!sessionCaptured) sessionCaptured = true
            check(selected.id == requested.id)
        }

        attemptPlayback(listOf(ordinaryVideo), ordinaryVideo)
        assertFalse(sessionCaptured)

        attemptPlayback(listOf(validShort), validShort)
        assertTrue(sessionCaptured)
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
