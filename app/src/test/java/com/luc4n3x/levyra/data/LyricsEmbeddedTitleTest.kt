package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsEmbeddedTitleTest {
    @Test
    fun channelUploadTriesBothOrders() {
        assertEquals(
            listOf("Blinding Lights" to "The Weeknd", "The Weeknd" to "Blinding Lights"),
            embeddedTitleArtistCandidates("Blinding Lights - The Weeknd", "FitDance")
        )
    }

    @Test
    fun knownArtistSideIsUsed() {
        assertEquals(listOf("Starboy" to "The Weeknd"), embeddedTitleArtistCandidates("The Weeknd - Starboy", "The Weeknd"))
    }

    @Test
    fun plainTitlesHaveNoAlternates() {
        assertTrue(embeddedTitleArtistCandidates("Blinding Lights", "The Weeknd").isEmpty())
        assertTrue(embeddedTitleArtistCandidates("A - B - C", "X").isEmpty())
        assertTrue(embeddedTitleArtistCandidates("Bohemian Rhapsody - Remastered 2011", "Queen").isEmpty())
        assertTrue(embeddedTitleArtistCandidates("Blinding Lights - Live at the Kia Forum", "The Weeknd").isEmpty())
    }
}
