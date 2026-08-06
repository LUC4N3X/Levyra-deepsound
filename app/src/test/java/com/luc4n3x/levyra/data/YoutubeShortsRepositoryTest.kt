package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
    fun localTimeoutFenceCancelsAStalledSearch() = runBlocking {
        var timedOut = false
        try {
            withShortsSearchTimeout(timeoutMs = 20L) {
                delay(250L)
            }
        } catch (_: TimeoutCancellationException) {
            timedOut = true
        }

        assertTrue(timedOut)
    }

    @Test
    fun retryBackoffIsBounded() {
        assertEquals(30_000L, youtubeShortsRetryDelayMs(1))
        assertEquals(60_000L, youtubeShortsRetryDelayMs(2))
        assertEquals(600_000L, youtubeShortsRetryDelayMs(5))
        assertEquals(600_000L, youtubeShortsRetryDelayMs(100))
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
