package com.luc4n3x.levyra.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumDescriptionMatchingTest {
    @Test
    fun requiresTheAlbumTitleNotOnlyTheArtist() {
        assertTrue(wikipediaAlbumTitleMatches("Che io mi aiuti", "Che io mi aiuti (album)"))
        assertFalse(wikipediaAlbumTitleMatches("Che io mi aiuti", "Alba (album di Ultimo)"))
        assertFalse(wikipediaAlbumTitleMatches("Che io mi aiuti", "Ultimo (cantante)"))
    }

    @Test
    fun toleratesCommonEditionSuffixes() {
        assertTrue(wikipediaAlbumTitleMatches("Che io mi aiuti (Deluxe Edition)", "Che io mi aiuti (album)"))
    }
}
