package com.luc4n3x.levyra.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.hct.Hct
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSoundtrackHeroStyleTest {
    private fun contrast(a: Color, b: Color): Float {
        val lighter = maxOf(a.luminance(), b.luminance())
        val darker = minOf(a.luminance(), b.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    private fun hue(color: Color) = Hct.fromInt(color.toArgb()).hue

    private fun hueDistance(first: Double, second: Double): Double {
        val delta = abs(first - second) % 360.0
        return minOf(delta, 360.0 - delta)
    }

    @Test
    fun `colourful portrait drives the hero seed`() {
        val portrait = Color(0xFFB0336A)
        assertEquals(portrait, homeSoundtrackHeroSeed(portrait, Color(0xFF246BCE)))
    }

    @Test
    fun `grey portrait falls back to the theme primary`() {
        val primary = Color(0xFF246BCE)
        assertEquals(primary, homeSoundtrackHeroSeed(Color(0xFF3A3A3C), primary))
    }

    @Test
    fun `hero action keeps its seed hue and readable content in both themes`() {
        val seed = Color(0xFFB0336A)
        listOf(false, true).forEach { isLight ->
            val tones = homeSoundtrackHeroTones(seed, isLight)
            assertTrue(contrast(tones.action, tones.onAction) >= 4.5f)
            assertTrue(contrast(tones.chip.copy(alpha = 1f), tones.onChip) >= 4.5f)
            assertTrue(hueDistance(hue(seed), hue(tones.action)) < 20.0)
        }
    }

    @Test
    fun `washed out seeds still produce a vivid action`() {
        val tones = homeSoundtrackHeroTones(Color(0xFF8C8F9E), isLight = false)
        assertTrue(Hct.fromInt(tones.action.toArgb()).chroma >= 20.0)
    }
}
