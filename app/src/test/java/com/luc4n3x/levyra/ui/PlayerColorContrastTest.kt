package com.luc4n3x.levyra.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerColorContrastTest {

    @Test
    fun `playerContrastRatio satisfies WCAG bounds`() {
        val whiteOnBlack = playerContrastRatio(Color.White, Color.Black)
        assertEquals(21f, whiteOnBlack, 0.1f)

        val sameColor = playerContrastRatio(Color.White, Color.White)
        assertEquals(1f, sameColor, 0.01f)

        val grayOnBlack = playerContrastRatio(Color(0xFF888888), Color.Black)
        assertTrue(grayOnBlack > 1f && grayOnBlack < 21f)
    }

    @Test
    fun `playerCompositeOver blends semi-transparent colors correctly`() {
        val semiWhite = Color.White.copy(alpha = 0.5f)
        val blended = semiWhite.playerCompositeOver(Color.Black)
        assertEquals(1f, blended.alpha, 0.001f)
        assertEquals(0.5f, blended.red, 0.01f)
        assertEquals(0.5f, blended.green, 0.01f)
        assertEquals(0.5f, blended.blue, 0.01f)
    }

    @Test
    fun `playerContentColor selects high contrast text on PlayerDarkSurface`() {
        val darkSurface = PlayerDarkSurface
        val content = Color.White.playerContentColor(listOf(darkSurface), PlayerMinimumContrast)
        val ratio = playerContrastRatio(content, darkSurface)
        assertTrue("contrast ratio $ratio must be >= 4.5", ratio >= PlayerMinimumContrast)
    }

    @Test
    fun `playerContentColor selects dark text on bright surfaces`() {
        val brightSurface = Color(0xFFEEEEEE)
        val content = Color.White.playerContentColor(listOf(brightSurface), PlayerMinimumContrast)
        val ratio = playerContrastRatio(content, brightSurface)
        assertTrue("contrast ratio $ratio must be >= 4.5", ratio >= PlayerMinimumContrast)
    }

    @Test
    fun `playerAdjustBackgroundFor meets target contrast`() {
        val text = Color.White
        val brightBackground = Color(0xFFDDDDDD)
        val adjustment = brightBackground.playerAdjustBackgroundFor(text, PlayerMinimumContrast)
        assertTrue("adjustment must be valid", adjustment.valid)
        val ratio = playerContrastRatio(text, adjustment.color)
        assertTrue("contrast ratio $ratio must be >= 4.5", ratio >= PlayerMinimumContrast)
    }

    @Test
    fun `playerContrastGradient yields readable gradient endpoints`() {
        val start = Color(0xFFFF5500)
        val end = Color(0xFFFFCC00)
        val gradient = playerContrastGradient(start, end, PlayerMinimumContrast)
        val ratioStart = playerContrastRatio(gradient.content, gradient.start)
        val ratioEnd = playerContrastRatio(gradient.content, gradient.end)
        assertTrue("start ratio $ratioStart must be >= 4.5", ratioStart >= PlayerMinimumContrast)
        assertTrue("end ratio $ratioEnd must be >= 4.5", ratioEnd >= PlayerMinimumContrast)
    }
}
