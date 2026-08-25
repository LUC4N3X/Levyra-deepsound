package com.luc4n3x.levyra.ui.artwork

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkPreviewFileNameTest {

    @Test
    fun collapsesIllegalCharactersIntoSingleSpaces() {
        val name = artworkPreviewFileName("I Feel It Coming (feat. Daft Punk)", 42L)

        assertEquals("I Feel It Coming feat Daft Punk-42.jpg", name)
        assertFalse(name.contains("  "))
        assertTrue(name.endsWith("-42.jpg"))
    }

    @Test
    fun keepsUnicodeLettersAndDigits() {
        val name = artworkPreviewFileName("Café 77 – Été", 7L)

        assertTrue(name.startsWith("Café 77"))
        assertFalse(name.contains("  "))
    }

    @Test
    fun fallsBackWhenTitleHasNoUsableCharacters() {
        assertEquals("levyra-9.jpg", artworkPreviewFileName("///", 9L))
        assertEquals("levyra-9.jpg", artworkPreviewFileName("   ", 9L))
    }

    @Test
    fun boundsTheFileNameLength() {
        val name = artworkPreviewFileName("a".repeat(400), 1L)

        assertTrue(name.length <= 48 + "-1.jpg".length)
    }
}
