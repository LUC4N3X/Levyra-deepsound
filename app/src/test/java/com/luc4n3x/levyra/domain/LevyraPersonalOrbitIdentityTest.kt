package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraPersonalOrbitIdentityTest {
    @Test
    fun deduplicatesSameRecordingAcrossDifferentYoutubeLinksAndArtistSeparators() {
        val audio = track(
            id = "audio123456",
            title = "Dai Dai (Official Audio)",
            artist = "Shakira & Burna Boy",
            videoUrl = "https://www.youtube.com/watch?v=audio123456"
        )
        val video = track(
            id = "video123456",
            title = "Dai Dai [Official Music Video]",
            artist = "Burna Boy, Shakira",
            videoUrl = "https://www.youtube.com/watch?v=video123456",
            counterpartVideoId = "video123456"
        )

        val result = LevyraPersonalOrbit.distinctRecordings(listOf(audio, video))

        assertEquals(1, result.size)
        assertEquals("video123456", result.single().counterpartVideoId)
        assertTrue(LevyraPersonalOrbit.sameRecording(audio, video))
    }

    @Test
    fun keepsMeaningfullyDifferentVersionsSeparate() {
        val studio = track(id = "studio12345", title = "Dai Dai", artist = "Shakira, Burna Boy")
        val live = track(id = "live1234567", title = "Dai Dai (Live)", artist = "Shakira & Burna Boy")

        assertEquals(2, LevyraPersonalOrbit.distinctRecordings(listOf(studio, live)).size)
    }

    private fun track(
        id: String,
        title: String,
        artist: String,
        videoUrl: String = "https://www.youtube.com/watch?v=$id",
        counterpartVideoId: String = ""
    ): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = "Album",
        durationMs = 220_000L,
        streamUrl = "",
        videoUrl = videoUrl,
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "YouTube Music",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 80,
        cacheScore = 80,
        accentStart = 0,
        accentEnd = 0,
        counterpartVideoId = counterpartVideoId
    )
}
