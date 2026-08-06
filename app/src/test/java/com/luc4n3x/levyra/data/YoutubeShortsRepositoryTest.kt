package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeShortsRepositoryTest {
    @Test
    fun shortsUrlsAreAcceptedWithoutDependingOnTheTitle() {
        assertTrue(
            isYoutubeShortCandidate(
                url = "https://www.youtube.com/shorts/abcdefghijk",
                title = "A vertical music clip",
                durationSeconds = 42L
            )
        )
    }

    @Test
    fun taggedShortsRequireAShortFormDuration() {
        assertTrue(
            isYoutubeShortCandidate(
                url = "https://www.youtube.com/watch?v=abcdefghijk",
                title = "Official clip #Shorts",
                durationSeconds = 58L
            )
        )
        assertFalse(
            isYoutubeShortCandidate(
                url = "https://www.youtube.com/watch?v=abcdefghijk",
                title = "Official clip #Shorts",
                durationSeconds = 480L
            )
        )
    }

    @Test
    fun ordinaryMusicVideosAreRejected() {
        assertFalse(
            isYoutubeShortCandidate(
                url = "https://www.youtube.com/watch?v=abcdefghijk",
                title = "Official Music Video",
                durationSeconds = 210L
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
