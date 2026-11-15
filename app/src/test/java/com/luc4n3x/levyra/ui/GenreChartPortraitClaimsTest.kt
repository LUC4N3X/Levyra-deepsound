package com.luc4n3x.levyra.ui

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GenreChartPortraitClaimsTest {
    private val portraits = mapOf("The Weeknd" to "weeknd.jpg", "David Guetta" to "guetta.jpg", "Frank Ocean" to "ocean.jpg")
    private val apple: suspend (String) -> String = { artist -> portraits[artist].orEmpty() }

    private suspend fun portrait(candidates: List<String>, providers: List<suspend (String) -> String>, scope: String, zone: String): String =
        resolveDistinctGenreChartPortrait(candidates, providers, scope, zone)?.url.orEmpty()

    @After
    fun reset() = GenreChartPortraitClaims.clear()

    @Test
    fun sharedLeadArtistCoversOnlyOneGenre() = runBlocking {
        val electronic = portrait(listOf("The Weeknd", "David Guetta"), listOf(apple), "en:1", "electronic")
        val soul = portrait(listOf("The Weeknd", "Frank Ocean"), listOf(apple), "en:1", "rnb-soul")

        assertEquals("weeknd.jpg", electronic)
        assertEquals("ocean.jpg", soul)
    }

    @Test
    fun sameZoneKeepsItsOwnPortrait() = runBlocking {
        portrait(listOf("The Weeknd"), listOf(apple), "en:1", "electronic")

        assertEquals("weeknd.jpg", portrait(listOf("The Weeknd"), listOf(apple), "en:1", "electronic"))
    }

    @Test
    fun cachedUrlBlocksTheSamePortraitElsewhere() = runBlocking {
        assertTrue(GenreChartPortraitClaims.claim("en:1", "electronic", null, "weeknd.jpg"))
        assertFalse(GenreChartPortraitClaims.claim("en:1", "rnb-soul", null, "weeknd.jpg"))

        assertEquals("ocean.jpg", portrait(listOf("The Weeknd", "Frank Ocean"), listOf(apple), "en:1", "rnb-soul"))
    }

    @Test
    fun noFreePortraitFallsBackToBlank() = runBlocking {
        portrait(listOf("The Weeknd"), listOf(apple), "en:1", "electronic")

        assertEquals("", portrait(listOf("The Weeknd"), listOf(apple), "en:1", "rnb-soul"))
    }

    @Test
    fun cachedArtistBlocksTheSameArtistUnderAnotherImage() = runBlocking {
        assertTrue(GenreChartPortraitClaims.claim("en:1", "electronic", "The Weeknd", "weeknd-apple.jpg"))

        assertEquals("ocean.jpg", portrait(listOf("The Weeknd", "Frank Ocean"), listOf(apple), "en:1", "rnb-soul"))
    }

    @Test
    fun newRotationStartsFresh() = runBlocking {
        portrait(listOf("The Weeknd"), listOf(apple), "en:1", "electronic")

        assertEquals("weeknd.jpg", portrait(listOf("The Weeknd"), listOf(apple), "en:2", "rnb-soul"))
    }

    @Test
    fun anOlderLiveRotationKeepsItsClaims() = runBlocking {
        portrait(listOf("The Weeknd"), listOf(apple), "en:1", "electronic")
        portrait(listOf("The Weeknd"), listOf(apple), "en:2", "electronic")

        assertEquals("ocean.jpg", portrait(listOf("The Weeknd", "Frank Ocean"), listOf(apple), "en:1", "rnb-soul"))
    }
}
