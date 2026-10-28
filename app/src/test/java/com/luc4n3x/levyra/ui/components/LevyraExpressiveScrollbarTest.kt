package com.luc4n3x.levyra.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraExpressiveScrollbarTest {

    @Test
    fun `progress spans the scrollable range and stays bounded`() {
        assertEquals(0f, levyraScrollbarProgress(0, 0f, 100, 10f), 0.0001f)
        assertEquals(0.5f, levyraScrollbarProgress(45, 0f, 100, 10f), 0.0001f)
        assertEquals(1f, levyraScrollbarProgress(90, 0f, 100, 10f), 0.0001f)
        assertEquals(1f, levyraScrollbarProgress(99, 0.9f, 100, 10f), 0.0001f)
        assertTrue(levyraScrollbarProgress(10, 0.5f, 100, 10f) > levyraScrollbarProgress(10, 0f, 100, 10f))
    }

    @Test
    fun `empty or fully visible lists never report progress`() {
        assertEquals(0f, levyraScrollbarProgress(0, 0f, 0, 0f), 0.0001f)
        assertEquals(0f, levyraScrollbarProgress(0, 0f, 8, 12f), 0.0001f)
    }

    @Test
    fun `drag target maps the full track to the full list`() {
        assertEquals(0, levyraScrollbarTargetIndex(0f, 100, 10f))
        assertEquals(45, levyraScrollbarTargetIndex(0.5f, 100, 10f))
        assertEquals(90, levyraScrollbarTargetIndex(1f, 100, 10f))
        assertEquals(90, levyraScrollbarTargetIndex(4f, 100, 10f))
        assertEquals(0, levyraScrollbarTargetIndex(-1f, 100, 10f))
        assertEquals(0, levyraScrollbarTargetIndex(0.7f, 1, 1f))
        assertEquals(0, levyraScrollbarTargetIndex(0.7f, 0, 0f))
    }

    @Test
    fun `drag target round trips through progress`() {
        listOf(0, 7, 33, 61, 90).forEach { index ->
            val progress = levyraScrollbarProgress(index, 0f, 100, 10f)
            assertEquals(index, levyraScrollbarTargetIndex(progress, 100, 10f))
        }
    }

    @Test
    fun `scrollbar appears only for genuinely long content`() {
        assertFalse(levyraScrollbarWorthShowing(0, 0f))
        assertFalse(levyraScrollbarWorthShowing(12, 10f))
        assertFalse(levyraScrollbarWorthShowing(24, 10f))
        assertTrue(levyraScrollbarWorthShowing(25, 10f))
        assertTrue(levyraScrollbarWorthShowing(500, 9.5f))
        assertFalse(levyraScrollbarWorthShowing(500, 0f))
    }

    @Test
    fun `glyph uses the first letter or a hash for numbers`() {
        assertEquals("B", levyraScrollbarGlyph("beatles"))
        assertEquals("Ä", levyraScrollbarGlyph("  äänet"))
        assertEquals("#", levyraScrollbarGlyph("2Pac"))
        assertEquals("M", levyraScrollbarGlyph("(…) Mina"))
        assertNull(levyraScrollbarGlyph("   "))
        assertNull(levyraScrollbarGlyph(null))
    }
}
