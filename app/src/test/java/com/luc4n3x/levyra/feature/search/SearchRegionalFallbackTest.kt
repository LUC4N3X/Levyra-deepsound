package com.luc4n3x.levyra.feature.search

import com.luc4n3x.levyra.domain.SearchResults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchRegionalFallbackTest {
    @Test
    fun teluguScriptRomanizesToCommonLatinSpelling() {
        assertEquals("karige loga", southIndianLatinVariant("కరిగే లోగా"))
        assertEquals("andam", southIndianLatinVariant("అందం"))
    }

    @Test
    fun tamilScriptRomanizesWithPulliAndLongVowels() {
        assertEquals("kaathal", southIndianLatinVariant("காதல்"))
        assertEquals("vaa vaaththi", southIndianLatinVariant("வா வாத்தி"))
    }

    @Test
    fun mixedQueryKeepsLatinWords() {
        assertEquals("aarya 2 karige loga", southIndianLatinVariant("Aarya 2 కరిగే లోగా"))
    }

    @Test
    fun fallbackOnlyRunsForSparseSouthIndianScriptQueries() {
        val sparse = SearchResults(songs = listOf(searchTestTrack("t1", "కరిగే లోగా", "Devi Sri Prasad")))
        val rich = SearchResults(songs = (1..REGIONAL_FALLBACK_MIN_SONGS).map { searchTestTrack("t$it", "Song $it", "Artist") })

        assertEquals("karige loga", regionalSearchFallbackQuery("కరిగే లోగా", sparse))
        assertEquals("karige loga", regionalSearchFallbackQuery("కరిగే లోగా", null))
        assertNull(regionalSearchFallbackQuery("కరిగే లోగా", rich))
        assertNull(regionalSearchFallbackQuery("karige loga", sparse))
        assertNull(regionalSearchFallbackQuery("Vasco Rossi", sparse))
        assertNull(regionalSearchFallbackQuery("अंदाज़", sparse))
    }
}
