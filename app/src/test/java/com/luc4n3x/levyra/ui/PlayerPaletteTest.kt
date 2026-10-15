package com.luc4n3x.levyra.ui

import androidx.compose.ui.graphics.Color
import com.luc4n3x.levyra.domain.PlayerColorMode
import com.luc4n3x.levyra.ui.theme.LevyraThemes
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

    @Test
    fun `dynamic player colors defer to the artwork`() {
        assertEquals(null, playerFixedAccents(PlayerColorMode.Dynamic, LevyraThemes.byId(LevyraThemes.OCEAN), 0xFF336699.toInt()))
    }

    @Test
    fun `theme player colors come only from the selected theme`() {
        val ocean = LevyraThemes.byId(LevyraThemes.OCEAN)
        val ember = LevyraThemes.byId(LevyraThemes.EMBER)
        val first = playerFixedAccents(PlayerColorMode.Theme, ocean, 0)
        assertEquals(first, playerFixedAccents(PlayerColorMode.Theme, ocean, 0xFFFF0000.toInt()))
        assertNotEquals(first, playerFixedAccents(PlayerColorMode.Theme, ember, 0))
    }

    @Test
    fun `custom player colors build a two tone palette from one color`() {
        val pair = playerFixedAccents(PlayerColorMode.Custom, LevyraThemes.byId(LevyraThemes.OCEAN), 0xFF3366FF.toInt())!!
        assertEquals(Color(0xFF3366FF), pair.primary)
        assertNotEquals(pair.primary, pair.secondary)
        assertEquals(1f, pair.secondary.alpha, 0.001f)
    }

    @Test
    fun `nav artwork seed tames neon and keeps grey covers neutral`() {
        val neon = navArtworkSeedOf(Color(0xFF00FF00), Color(0xFFFF00FF))
        val (_, saturation, _) = neon.primary.hsvComponents()
        assertTrue(saturation <= 0.81f)
        val grey = navArtworkSeedOf(Color(0xFF101010), Color(0xFFF0F0F0))
        val (_, greySaturation, greyValue) = grey.primary.hsvComponents()
        assertTrue(greySaturation < 0.08f)
        assertTrue(greyValue >= 0.54f)
    }
}
