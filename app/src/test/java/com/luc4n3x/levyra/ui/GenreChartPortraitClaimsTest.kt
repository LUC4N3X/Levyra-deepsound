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

    @After
    fun reset() = GenreChartPortraitClaims.clear()

    @Test
    fun sharedLeadArtistCoversOnlyOneGenre() = runBlocking {
        val electronic = resolveDistinctGenreChartPortrait(listOf("The Weeknd", "David Guetta"), listOf(apple), "en:1", "electronic")
        val soul = resolveDistinctGenreChartPortrait(listOf("The Weeknd", "Frank Ocean"), listOf(apple), "en:1", "rnb-soul")

        assertEquals("weeknd.jpg", electronic)
        assertEquals("ocean.jpg", soul)
    }

    @Test
    fun sameZoneKeepsItsOwnPortrait() = runBlocking {
        resolveDistinctGenreChartPortrait(listOf("The Weeknd"), listOf(apple), "en:1", "electronic")

        assertEquals("weeknd.jpg", resolveDistinctGenreChartPortrait(listOf("The Weeknd"), listOf(apple), "en:1", "electronic"))
    }

    @Test
    fun cachedUrlBlocksTheSamePortraitElsewhere() = runBlocking {
        assertTrue(GenreChartPortraitClaims.claim("en:1", "electronic", null, "weeknd.jpg"))
        assertFalse(GenreChartPortraitClaims.claim("en:1", "rnb-soul", null, "weeknd.jpg"))

        assertEquals("ocean.jpg", resolveDistinctGenreChartPortrait(listOf("The Weeknd", "Frank Ocean"), listOf(apple), "en:1", "rnb-soul"))
    }

    @Test
    fun noFreePortraitFallsBackToBlank() = runBlocking {
        resolveDistinctGenreChartPortrait(listOf("The Weeknd"), listOf(apple), "en:1", "electronic")

        assertEquals("", resolveDistinctGenreChartPortrait(listOf("The Weeknd"), listOf(apple), "en:1", "rnb-soul"))
    }

    @Test
    fun newRotationStartsFresh() = runBlocking {
        resolveDistinctGenreChartPortrait(listOf("The Weeknd"), listOf(apple), "en:1", "electronic")

        assertEquals("weeknd.jpg", resolveDistinctGenreChartPortrait(listOf("The Weeknd"), listOf(apple), "en:2", "rnb-soul"))
    }
}
