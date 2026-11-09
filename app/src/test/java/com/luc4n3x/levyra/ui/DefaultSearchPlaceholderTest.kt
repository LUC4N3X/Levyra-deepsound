package com.luc4n3x.levyra.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultSearchPlaceholderTest {
    @Test
    fun quotedArtistIsHighlightedWithoutQuotes() {
        val text = emphasizedPlaceholder("Cerca l'artista “Bresh”", Color.Red)
        assertEquals("Cerca l'artista Bresh", text.text)
        val span = text.spanStyles.single()
        assertEquals(Color.Red, span.item.color)
        assertEquals("Bresh", text.text.substring(span.start, span.end))
    }

    @Test
    fun plainPlaceholderStaysUnstyled() {
        val text = emphasizedPlaceholder("Cerca", Color.Red)
        assertEquals("Cerca", text.text)
        assertTrue(text.spanStyles.isEmpty())
    }
}
