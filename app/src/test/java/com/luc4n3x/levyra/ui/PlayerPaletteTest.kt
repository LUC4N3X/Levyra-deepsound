package com.luc4n3x.levyra.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerPaletteTest {

    @Test
    fun `harmonizePlayerAccents preserves non-clashing pairs`() {
        val primary = Color(0xFF1B5CFF)
        val secondary = Color(0xFFFF4FD8)
        val pair = harmonizePlayerAccents(primary, secondary)
        assertEquals(primary, pair.primary)
        assertEquals(secondary, pair.secondary)
    }

    @Test
    fun `harmonizePlayerAccents resolves red-green clash`() {
        val red = Color(0xFFFF2222)
        val green = Color(0xFF00EE33)
        val pair = harmonizePlayerAccents(red, green)
        assertEquals(red, pair.primary)
        // Secondary is harmonized with red anchor instead of keeping clashing green
        assertNotEquals(green, pair.secondary)
        assertEquals(1f, pair.secondary.alpha, 0.001f)
    }

    @Test
    fun `harmonizePlayerAccents normalizes alpha to opaque`() {
        val transparentPrimary = Color.Cyan.copy(alpha = 0.4f)
        val transparentSecondary = Color.Magenta.copy(alpha = 0.6f)
        val pair = harmonizePlayerAccents(transparentPrimary, transparentSecondary)
        assertEquals(1f, pair.primary.alpha, 0.001f)
        assertEquals(1f, pair.secondary.alpha, 0.001f)
    }
}
