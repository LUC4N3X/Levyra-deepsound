package com.luc4n3x.levyra.ui

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExploreMoodArtworkCardTest {
    @Test
    fun supportedGenresAlwaysHavePortraitCandidates() {
        val zoneIds = listOf(
            "nuove-uscite",
            "local-wave",
            "rap-drill",
            "elettronica",
            "pop-global",
            "rnb-soul",
            "rock-alt",
            "latino",
            "lofi-chill",
            "anime-jpop"
        )

        zoneIds.forEach { zoneId ->
            assertNotNull(exploreMoodPortraitArtist(zoneId, "it", 0L))
        }
    }

    @Test
    fun portraitChoiceRotatesBetweenWindows() {
        val first = exploreMoodPortraitArtist("pop-global", "it", 3L)
        val next = exploreMoodPortraitArtist("pop-global", "it", 4L)

        assertNotEquals(first, next)
    }

    @Test
    fun italianRapUsesItalianArtistPool() {
        val italianRapArtists = setOf("Sfera Ebbasta", "Shiva", "Geolier", "Tony Boy", "Kid Yugi")

        repeat(5) { bucket ->
            val artist = exploreMoodPortraitArtist("rap-drill", "it-IT", bucket.toLong())
            assertTrue(artist in italianRapArtists)
        }
    }

    @Test
    fun localWaveUsesLocalizedArtistPoolWhenAvailable() {
        val italianArtists = setOf("Annalisa", "Mahmood", "Elodie", "Lazza", "Geolier")

        repeat(5) { bucket ->
            val artist = exploreMoodPortraitArtist("local-wave", "it", bucket.toLong())
            assertTrue(artist in italianArtists)
        }
    }

    @Test
    fun unknownGenreDoesNotTriggerPortraitLookup() {
        assertNull(exploreMoodPortraitArtist("unknown-zone", "it", 0L))
    }
}
