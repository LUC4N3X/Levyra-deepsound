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
    fun scriptQueriesUseRomanizedVariantUntilEnoughRelevantSongsExist() {
        val sparse = SearchResults(songs = listOf(searchTestTrack("t1", "కరిగే లోగా", "Devi Sri Prasad")))
        val manyIrrelevant = SearchResults(songs = (1..6).map { searchTestTrack("x$it", "Other $it", "Someone") })
        val relevant = SearchResults(
            songs = listOf(
                searchTestTrack("t1", "కరిగే లోగా", "Devi Sri Prasad"),
                searchTestTrack("t2", "Karige Loga", "Devi Sri Prasad"),
                searchTestTrack("t3", "Karige Loga (From Aarya 2)", "Sagar")
            )
        )

        assertEquals("karige loga", regionalSearchFallbackQuery("కరిగే లోగా", sparse))
        assertEquals("karige loga", regionalSearchFallbackQuery("కరిగే లోగా", null))
        assertEquals("karige loga", regionalSearchFallbackQuery("కరిగే లోగా", manyIrrelevant))
        assertNull(regionalSearchFallbackQuery("కరిగే లోగా", relevant))
    }

    @Test
    fun latinQueriesFetchSongsOnlyWhenOverviewHasTooFewRelevantSongs() {
        val irrelevant = SearchResults(songs = (1..6).map { searchTestTrack("x$it", "Other $it", "Someone") })
        val relevant = SearchResults(
            songs = listOf(
                searchTestTrack("v1", "Albachiara", "Vasco Rossi"),
                searchTestTrack("v2", "Vita spericolata", "Vasco Rossi"),
                searchTestTrack("v3", "Sally", "Vasco Rossi")
            )
        )

        assertEquals("Karige Loga", regionalSearchFallbackQuery("Karige Loga", irrelevant))
        assertNull(regionalSearchFallbackQuery("Karige Loga", irrelevant, songsSectionLoaded = true))
        assertNull(regionalSearchFallbackQuery("Vasco Rossi", relevant))
        assertNull(regionalSearchFallbackQuery("a", irrelevant))
    }
}
