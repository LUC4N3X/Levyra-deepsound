package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SongMatchConfidenceTest {
    @Test
    fun identicalTitleAndArtistIsAConfidentMatch() {
        assertEquals(1.0, confidence("Blinding Lights", "The Weeknd", "Blinding Lights", "The Weeknd"), 0.0001)
    }

    @Test
    fun caseAndPunctuationDifferencesStillMatch() {
        assertTrue(
            confidence("Don't Start Now", "Dua Lipa", "DON'T START NOW!", "dua lipa") >= MIN_SONG_MATCH_CONFIDENCE
        )
    }

    @Test
    fun featuringCreditsAndMultipleArtistsStillMatch() {
        assertTrue(
            confidence(
                "Savage Remix",
                "Megan Thee Stallion feat. Beyoncé",
                "Savage Remix",
                "Megan Thee Stallion, Beyoncé"
            ) >= MIN_SONG_MATCH_CONFIDENCE
        )
    }

    @Test
    fun accentedCharactersMatchTheirPlainForm() {
        assertTrue(confidence("Déjà Vu", "Beyoncé", "Deja Vu", "Beyonce") >= MIN_SONG_MATCH_CONFIDENCE)
    }

    @Test
    fun cyrillicAndCjkTitlesAreMatched() {
        assertTrue(confidence("Кукушка", "Кино", "Кукушка", "Кино") >= MIN_SONG_MATCH_CONFIDENCE)
        assertTrue(confidence("紅蓮華", "LiSA", "紅蓮華", "LiSA") >= MIN_SONG_MATCH_CONFIDENCE)
    }

    @Test
    fun unrelatedCandidateIsRejected() {
        assertTrue(confidence("Bohemian Rhapsody", "Queen", "Tokyo Drift", "Teriyaki Boyz") < MIN_SONG_MATCH_CONFIDENCE)
    }

    @Test
    fun unrequestedLiveOrRemixVersionIsDemoted() {
        val studio = confidence("Numb", "Linkin Park", "Numb", "Linkin Park")
        val live = confidence("Numb", "Linkin Park", "Numb (Live at Milton Keynes)", "Linkin Park")
        assertTrue(live < studio)
        assertTrue(live < MIN_SONG_MATCH_CONFIDENCE)
    }

    @Test
    fun explicitlyRequestedRemixKeepsTheRemixCandidate() {
        val remix = confidence("Levels (Skrillex Remix)", "Avicii", "Levels - Skrillex Remix", "Avicii")
        val original = confidence("Levels (Skrillex Remix)", "Avicii", "Levels", "Avicii")
        assertTrue(remix >= MIN_SONG_MATCH_CONFIDENCE)
        assertTrue(original < remix)
    }

    @Test
    fun sameTitleFromAnotherArtistIsRejected() {
        assertTrue(confidence("Hello", "Adele", "Hello", "Martin Solveig") < MIN_SONG_MATCH_CONFIDENCE)
    }

    @Test
    fun channelPlaceholderArtistFallsBackToTheTitleOnly() {
        assertTrue(confidence("Bohemian Rhapsody", "Queen", "Bohemian Rhapsody", "YouTube") >= MIN_SONG_MATCH_CONFIDENCE)
        assertTrue(confidence("Bohemian Rhapsody", "Queen", "Bohemian Rhapsody", "") >= MIN_SONG_MATCH_CONFIDENCE)
    }

    @Test
    fun placeholderArtistDoesNotRescueAWrongTitle() {
        assertTrue(confidence("Bohemian Rhapsody", "Queen", "Tokyo Drift", "YouTube") < MIN_SONG_MATCH_CONFIDENCE)
    }

    @Test
    fun wrongFirstResultDoesNotWinOverTheCorrectCandidate() {
        val candidates = listOf(
            track("wrong", "Hello (Karaoke Version)", "Karaoke Universe"),
            track("cover", "Hello", "Piano Tribute Players"),
            track("right", "Hello", "Adele")
        )
        assertEquals("right", bestSongMatch("Hello", "Adele", candidates)?.id)
    }

    @Test
    fun noConfidentCandidateReturnsNoMatch() {
        val candidates = listOf(
            track("a", "Completely Different Song", "Another Artist"),
            track("b", "Second Unrelated Track", "Someone Else")
        )
        assertNull(bestSongMatch("Bohemian Rhapsody", "Queen", candidates))
    }

    @Test
    fun emptyCandidateListReturnsNoMatch() {
        assertNull(bestSongMatch("Bohemian Rhapsody", "Queen", emptyList()))
    }

    private fun confidence(
        requestedTitle: String,
        requestedArtist: String,
        candidateTitle: String,
        candidateArtist: String
    ): Double = songMatchConfidence(requestedTitle, requestedArtist, candidateTitle, candidateArtist)

    private fun track(id: String, title: String, artist: String): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = title,
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "https://www.youtube.com/watch?v=abcdefghijk",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "YouTube Music",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 70,
        cacheScore = 70,
        accentStart = 0,
        accentEnd = 0
    )
}
