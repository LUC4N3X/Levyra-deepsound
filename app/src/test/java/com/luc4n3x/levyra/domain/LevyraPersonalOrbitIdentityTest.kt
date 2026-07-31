package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraPersonalOrbitIdentityTest {
    @Test
    fun deduplicatesSameRecordingAcrossDifferentYoutubeLinksAndFeaturingCredits() {
        val audio = track(
            id = "audio123456",
            title = "Cuore Rotto (Official Audio)",
            artist = "Tiziano Ferro"
        )
        val video = track(
            id = "video123456",
            title = "Cuore Rotto [Official Music Video]",
            artist = "Tiziano Ferro feat. Coco",
            counterpartVideoId = "video123456"
        )

        val result = LevyraPersonalOrbit.distinctRecordings(listOf(audio, video))

        assertEquals(1, result.size)
        assertEquals("video123456", result.single().counterpartVideoId)
        assertTrue(LevyraPersonalOrbit.sameRecording(audio, video))
    }

    @Test
    fun deduplicatesArtistsWhenCreditsUseDifferentOrderAndSeparators() {
        val first = track(id = "first123456", title = "Dai Dai", artist = "Shakira & Burna Boy")
        val second = track(id = "second12345", title = "Dai Dai", artist = "Burna Boy, Shakira")

        assertEquals(1, LevyraPersonalOrbit.distinctRecordings(listOf(first, second)).size)
    }

    @Test
    fun keepsSameTitleFromDifferentArtistsSeparate() {
        val first = track(id = "first123456", title = "Home", artist = "Artist One")
        val second = track(id = "second12345", title = "Home", artist = "Artist Two")

        assertFalse(LevyraPersonalOrbit.sameRecording(first, second))
        assertEquals(2, LevyraPersonalOrbit.distinctRecordings(listOf(first, second)).size)
    }

    @Test
    fun keepsMeaningfullyDifferentVersionsSeparate() {
        val studio = track(id = "studio12345", title = "Dai Dai", artist = "Shakira, Burna Boy")
        val live = track(
            id = "live1234567",
            title = "Dai Dai (Live)",
            artist = "Shakira & Burna Boy",
            durationMs = 260_000L
        )

        assertEquals(2, LevyraPersonalOrbit.distinctRecordings(listOf(studio, live)).size)
    }

    @Test
    fun doesNotStripVideoOrAudioInsideRealWords() {
        val videotape = track(id = "videotape01", title = "Videotape", artist = "Radiohead")
        val tape = track(id = "tape0000001", title = "Tape", artist = "Radiohead")
        val audioslave = track(id = "audioslave1", title = "Audioslave", artist = "Audioslave")
        val slave = track(id = "slave000001", title = "Slave", artist = "Audioslave")

        assertEquals(4, LevyraPersonalOrbit.distinctRecordings(listOf(videotape, tape, audioslave, slave)).size)
    }

    @Test
    fun fillsOrbitToTwentyWithTasteRankedUniqueDonors() {
        val favorite = track(id = "favorite001", title = "Seed", artist = "Sfera Ebbasta", moodTags = setOf("rap"))
        val donors = (1..25).map { index ->
            track(
                id = "donor${index.toString().padStart(6, '0')}",
                title = "Track $index",
                artist = if (index <= 10) "Sfera Ebbasta" else "Artist $index",
                moodTags = if (index <= 15) setOf("rap") else setOf("pop")
            )
        }

        val result = LevyraPersonalOrbit.build(
            currentTrack = favorite,
            recentSearches = listOf(favorite.copy(id = "sameOtherId", counterpartVideoId = "video123456")),
            favorites = listOf(favorite),
            tracks = donors,
            homeSections = emptyList(),
            charts = donors.reversed(),
            languageCode = "it"
        )

        assertEquals(20, result.size)
        assertEquals(20, LevyraPersonalOrbit.distinctRecordings(result).size)
        assertEquals("Seed", result.first().title)
    }

    @Test
    fun xInArtistNameIsNotTreatedAsASeparator() {
        val original = track(id = "xambassador", title = "Renegades", artist = "X Ambassadors")
        val unrelated = track(id = "ambassador1", title = "Renegades", artist = "Ambassadors")

        assertFalse(LevyraPersonalOrbit.sameRecording(original, unrelated))
    }

    private fun track(
        id: String,
        title: String,
        artist: String,
        durationMs: Long = 220_000L,
        counterpartVideoId: String = "",
        moodTags: Set<String> = emptySet()
    ): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = "Album",
        durationMs = durationMs,
        streamUrl = "",
        videoUrl = "https://www.youtube.com/watch?v=$id",
        thumbnailUrl = "https://i.scdn.co/image/${id.padEnd(22, '0')}",
        largeThumbnailUrl = "https://i.scdn.co/image/${id.padEnd(22, '0')}",
        source = "YouTube Music",
        moodTags = moodTags,
        energy = 50,
        vocal = 50,
        replayScore = 80,
        cacheScore = 80,
        accentStart = 0,
        accentEnd = 0,
        counterpartVideoId = counterpartVideoId
    )
}
