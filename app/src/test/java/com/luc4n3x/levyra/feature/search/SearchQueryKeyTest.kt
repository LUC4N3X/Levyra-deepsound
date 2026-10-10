package com.luc4n3x.levyra.feature.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchQueryKeyTest {
    @Test
    fun `whitespace and case collapse to one key`() {
        assertEquals("geo lier", searchQueryKey("  GEO   Lier "))
    }

    @Test
    fun `latin accents and compatibility forms are folded`() {
        assertEquals("beyonce", searchQueryKey("Beyoncé"))
        assertEquals("abc", searchQueryKey("ＡＢＣ"))
        assertEquals("istanbul", searchQueryKey("İstanbul"))
    }

    @Test
    fun `punctuation becomes a word separator`() {
        assertEquals("i p me", searchQueryKey("I P' ME"))
        assertEquals("ac dc", searchQueryKey("AC/DC"))
    }

    @Test
    fun `non latin scripts keep their letters and combining marks`() {
        assertEquals("米津玄師", searchQueryKey("米津玄師"))
        assertEquals("हिंदी", searchQueryKey("हिंदी"))
    }

    @Test
    fun `searchable query needs two characters and a letter or digit`() {
        assertFalse(isSearchableQuery("a"))
        assertFalse(isSearchableQuery("!!"))
        assertTrue(isSearchableQuery("ok"))
    }

    @Test
    fun `related keys are prefix relatives only`() {
        assertTrue(areRelatedSearchKeys("geo", "geolier"))
        assertTrue(areRelatedSearchKeys("geolier", "geoli"))
        assertFalse(areRelatedSearchKeys("geolier", "adele"))
        assertFalse(areRelatedSearchKeys("", "adele"))
    }

    @Test
    fun `query suggestions drop the typed query duplicates and shown entities`() {
        val merged = mergeSearchQuerySuggestions(
            currentQuery = "geolier",
            remote = listOf("Geolier", "geolier i p me", "GEOLIER I P' ME", "geolier sirio", " ", "geolier anna"),
            shownEntityKeys = setOf("geolier sirio")
        )

        assertEquals(listOf("geolier i p me", "geolier anna"), merged)
    }

    @Test
    fun `query suggestions respect the limit`() {
        val merged = mergeSearchQuerySuggestions("a b", (1..20).map { "song $it" }, limit = 3)

        assertEquals(listOf("song 1", "song 2", "song 3"), merged)
    }
}
