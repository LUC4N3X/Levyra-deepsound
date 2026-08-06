package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeShortsRepositoryTest {
    @Test
    fun extractorShortFlagIsAccepted() {
        assertTrue(
            isYoutubeShortCandidate(
                isShortFormContent = true,
                url = "https://www.youtube.com/watch?v=abcdefghijk",
                durationSeconds = 42L
            )
        )
    }

    @Test
    fun canonicalShortsUrlsAreAcceptedAsFallback() {
        assertTrue(
            isYoutubeShortCandidate(
                isShortFormContent = false,
                url = "https://www.youtube.com/shorts/abcdefghijk",
                durationSeconds = 58L
            )
        )
    }

    @Test
    fun threeMinuteShortsAreAcceptedButLongerVideosAreRejected() {
        assertTrue(
            isYoutubeShortCandidate(
                isShortFormContent = true,
                url = "https://www.youtube.com/watch?v=abcdefghijk",
                durationSeconds = 180L
            )
        )
        assertFalse(
            isYoutubeShortCandidate(
                isShortFormContent = true,
                url = "https://www.youtube.com/watch?v=abcdefghijk",
                durationSeconds = 181L
            )
        )
    }

    @Test
    fun longOrOrdinaryVideosAreRejected() {
        assertFalse(
            isYoutubeShortCandidate(
                isShortFormContent = true,
                url = "https://www.youtube.com/watch?v=abcdefghijk",
                durationSeconds = 480L
            )
        )
        assertFalse(
            isYoutubeShortCandidate(
                isShortFormContent = false,
                url = "https://www.youtube.com/watch?v=abcdefghijk",
                durationSeconds = 90L
            )
        )
    }

    @Test
    fun shortTracksAreRecognizedBySourceUrlOrType() {
        assertTrue(isYoutubeShortTrack(track(source = YOUTUBE_SHORTS_SOURCE)))
        assertTrue(isYoutubeShortTrack(track(videoUrl = "https://www.youtube.com/shorts/abcdefghijk")))
        assertTrue(isYoutubeShortTrack(track(videoType = "SHORTS")))
        assertFalse(isYoutubeShortTrack(track()))
    }

    private fun track(
        source: String = "YouTube Music",
        videoUrl: String = "https://www.youtube.com/watch?v=abcdefghijk",
        videoType: String = ""
    ): Track = Track(
        id = "abcdefghijk",
        title = "Title",
        artist = "Artist",
        album = "Album",
        durationMs = 60_000L,
        streamUrl = "",
        videoUrl = videoUrl,
        thumbnailUrl = "https://levyra.test/short.jpg",
        largeThumbnailUrl = "https://levyra.test/short-large.jpg",
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
