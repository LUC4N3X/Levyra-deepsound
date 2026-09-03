package com.luc4n3x.levyra.ui

import org.junit.Assert.assertEquals
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
            assertTrue(exploreMoodPortraitCandidates(zoneId, "it", 0L).size >= 4)
        }
    }

    @Test
    fun portraitChoiceRotatesBetweenWindows() {
        val first = exploreMoodPortraitArtist("pop-global", "it", 3L)
        val next = exploreMoodPortraitArtist("pop-global", "it", 4L)

        assertNotEquals(first, next)
    }

    @Test
    fun italianRapTriesGlobalRapFallbackBeforeExhaustingLocalPool() {
        val italianRapArtists = setOf("Sfera Ebbasta", "Shiva", "Geolier", "Tony Boy", "Kid Yugi")
        val globalFallback = setOf("Central Cee", "Travis Scott")

        repeat(5) { bucket ->
            val candidates = exploreMoodPortraitCandidates("rap-drill", "it-IT", bucket.toLong())
            val lookupLimit = exploreMoodPortraitLookupLimit("rap-drill", candidates.size)

            assertEquals(9, candidates.size)
            assertEquals(candidates.size, candidates.toSet().size)
            assertEquals(5, lookupLimit)
            assertTrue(candidates.take(3).all { artist -> artist in italianRapArtists })
            assertTrue(candidates.take(lookupLimit).drop(3).all { artist -> artist in globalFallback })
        }
    }

    @Test
    fun italianRapKeepsAllItalianArtistsAvailableAfterFallbacks() {
        val italianRapArtists = setOf("Sfera Ebbasta", "Shiva", "Geolier", "Tony Boy", "Kid Yugi")
        val candidates = exploreMoodPortraitCandidates("rap-drill", "it", 4L)

        assertTrue(candidates.containsAll(italianRapArtists))
    }

    @Test
    fun lookupLimitNeverExceedsAvailableCandidates() {
        assertEquals(0, exploreMoodPortraitLookupLimit("rap-drill", 0))
        assertEquals(1, exploreMoodPortraitLookupLimit("rap-drill", 1))
        assertEquals(2, exploreMoodPortraitLookupLimit("pop-global", 8))
    }

    @Test
    fun localWaveUsesLocalizedArtistPoolWhenAvailable() {
        val italianArtists = setOf("Annalisa", "Mahmood", "Elodie", "Lazza", "Geolier")

        repeat(5) { bucket ->
            val candidates = exploreMoodPortraitCandidates("local-wave", "it", bucket.toLong())
            assertTrue(candidates.isNotEmpty())
            assertTrue(candidates.all { artist -> artist in italianArtists })
        }
    }

    @Test
    fun unknownGenreDoesNotTriggerPortraitLookup() {
        assertNull(exploreMoodPortraitArtist("unknown-zone", "it", 0L))
        assertTrue(exploreMoodPortraitCandidates("unknown-zone", "it", 0L).isEmpty())
    }
}
