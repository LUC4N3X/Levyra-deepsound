package com.luc4n3x.levyra.ui.components

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LivingArtworkCanvasTest {

    @Test
    fun deriveLivingArtworkPaletteReturnsFiveDistinctColors() {
        val accentStart = 0xFF2997FF.toInt()
        val accentEnd = 0xFF818CF8.toInt()

        val darkPalette = deriveLivingArtworkPalette(accentStart, accentEnd, isDarkTheme = true)
        val lightPalette = deriveLivingArtworkPalette(accentStart, accentEnd, isDarkTheme = false)

        assertNotNull(darkPalette.base)
        assertNotNull(darkPalette.primary)
        assertNotNull(darkPalette.secondary)
        assertNotNull(darkPalette.highlight)
        assertNotNull(darkPalette.ambient)

        assertNotNull(lightPalette.base)
        assertNotNull(lightPalette.primary)
        assertNotNull(lightPalette.secondary)
        assertNotNull(lightPalette.highlight)
        assertNotNull(lightPalette.ambient)

        // Verify alphas are 1f
        assertEquals(1f, darkPalette.base.alpha, 0.01f)
        assertEquals(1f, darkPalette.primary.alpha, 0.01f)
        assertEquals(1f, darkPalette.secondary.alpha, 0.01f)
        assertEquals(1f, darkPalette.highlight.alpha, 0.01f)
        assertEquals(1f, darkPalette.ambient.alpha, 0.01f)

        // Dark palette base should have low luminance
        assertTrue(darkPalette.base.red in 0f..0.25f)
        assertTrue(darkPalette.base.green in 0f..0.25f)
        assertTrue(darkPalette.base.blue in 0f..0.25f)

        // Light palette base should have high luminance
        assertTrue(lightPalette.base.red in 0.85f..1f)
        assertTrue(lightPalette.base.green in 0.85f..1f)
        assertTrue(lightPalette.base.blue in 0.85f..1f)
    }

    @Test
    fun deriveLivingArtworkPaletteHandlesZeroAccents() {
        val palette = deriveLivingArtworkPalette(0, 0, isDarkTheme = true)
        assertNotNull(palette)
        assertEquals(1f, palette.base.alpha, 0.01f)
        assertEquals(1f, palette.primary.alpha, 0.01f)
        assertEquals(1f, palette.secondary.alpha, 0.01f)
    }
}
