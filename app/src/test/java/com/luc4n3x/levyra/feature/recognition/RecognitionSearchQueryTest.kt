package com.luc4n3x.levyra.feature.recognition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecognitionSearchQueryTest {

    @Test
    fun combinesTitleAndArtist() {
        val result = RecognitionResult(title = "Song Title", artist = "The Artist")

        val query = RecognitionSearchQuery.from(result)

        assertEquals("Song Title The Artist", query)
    }

    @Test
    fun fallsBackToTitleOnlyWhenArtistIsBlank() {
        val result = RecognitionResult(title = "Solo Title", artist = "   ")

        val query = RecognitionSearchQuery.from(result)

        assertEquals("Solo Title", query)
    }

    @Test
    fun fallsBackToArtistOnlyWhenTitleIsBlank() {
        val result = RecognitionResult(title = "", artist = "Solo Artist")

        val query = RecognitionSearchQuery.from(result)

        assertEquals("Solo Artist", query)
    }

    @Test
    fun returnsEmptyStringWhenBothAreBlank() {
        val result = RecognitionResult(title = "  ", artist = "")

        val query = RecognitionSearchQuery.from(result)

        assertEquals("", query)
    }

    @Test
    fun collapsesInternalWhitespaceAndControlCharacters() {
        val result = RecognitionResult(title = "Song\n\tTitle", artist = "Weird    Artist")

        val query = RecognitionSearchQuery.from(result)

        assertEquals("Song Title Weird Artist", query)
    }

    @Test
    fun boundsQueryLength() {
        val longTitle = "T".repeat(200)
        val result = RecognitionResult(title = longTitle, artist = "Artist")

        val query = RecognitionSearchQuery.from(result)

        assertTrue(query.length <= RecognitionSearchQuery.MAX_QUERY_LENGTH)
    }
}
