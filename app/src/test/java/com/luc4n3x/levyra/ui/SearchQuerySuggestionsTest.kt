package com.luc4n3x.levyra.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchQuerySuggestionsTest {

    @Test
    fun `suggestions drop the echo of the query and duplicates`() {
        val refined = refineSearchQuerySuggestions(
            query = "Take On Me a-ha ",
            suggestions = listOf("take on me a-ha", "take on me  degli a-ha", "Take on me degli a-ha", " ", "take on me (2017 acoustic) a-ha")
        )
        assertEquals(listOf("take on me degli a-ha", "take on me (2017 acoustic) a-ha"), refined)
    }

    @Test
    fun `segments highlight only the words the query does not contain`() {
        val segments = searchSuggestionSegments("take on me a-ha", "take on me (2017 acoustic) a-ha")
        assertEquals(
            listOf(
                SearchSuggestionSegment("take on me ", novel = false),
                SearchSuggestionSegment("(2017 acoustic) ", novel = true),
                SearchSuggestionSegment("a-ha", novel = false)
            ),
            segments
        )
    }

    @Test
    fun `segments rebuild the original suggestion text`() {
        val suggestion = "take on me a-ha top hits 2026"
        val segments = searchSuggestionSegments("Take on me", suggestion)
        assertEquals(suggestion, segments.joinToString("") { it.text })
    }

    @Test
    fun `empty query marks the whole suggestion as novel`() {
        assertEquals(listOf(SearchSuggestionSegment("lofi beats", novel = true)), searchSuggestionSegments("  ", "lofi beats"))
    }
}
