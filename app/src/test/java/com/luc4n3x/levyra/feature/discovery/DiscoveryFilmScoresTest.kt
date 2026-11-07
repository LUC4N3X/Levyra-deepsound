package com.luc4n3x.levyra.feature.discovery

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryFilmScoresTest {
    private fun track(title: String, album: String = "") = Track(
        id = title,
        title = title,
        artist = "Artist",
        album = album,
        durationMs = 0,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "test",
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0
    )

    @Test
    fun filmScoresAreRecognised() {
        assertTrue(DiscoveryFilmScores.matches(track("Swan Lake", "Black Swan (Original Motion Picture Soundtrack)")))
        assertTrue(DiscoveryFilmScores.matches(track("Carol Anne's Theme", "Poltergeist (Archive Collection)")))
        assertTrue(DiscoveryFilmScores.matches(track("The Wheel - From The Hunger Games: Sunrise on the Reaping")))
        assertTrue(DiscoveryFilmScores.matches(track("Main Title", "The Witch (Original Score)")))
        assertTrue(DiscoveryFilmScores.matches(track("Opening", "Dune OST")))
    }

    @Test
    fun regularSongsPass() {
        assertFalse(DiscoveryFilmScores.matches(track("Housewife", "Housewife")))
        assertFalse(DiscoveryFilmScores.matches(track("Mr. Brightside", "Hot Fuss")))
        assertFalse(DiscoveryFilmScores.matches(track("Lost in the Score")))
        assertFalse(DiscoveryFilmScores.matches(track("Ghost Town", "Ghosts")))
    }
}
