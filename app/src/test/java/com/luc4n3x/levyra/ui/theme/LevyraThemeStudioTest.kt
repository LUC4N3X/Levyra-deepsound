package com.luc4n3x.levyra.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraThemeStudioTest {

    @Test
    fun curatedPresetsLeadTheCatalogWithoutLosingLegacyThemes() {
        val ids = LevyraThemes.presets.map { it.id }

        assertEquals(
            listOf(
                LevyraThemes.LEVYRA_AURA,
                LevyraThemes.COVER_FLOW,
                LevyraThemes.AMOLED,
                LevyraThemes.FROST,
                LevyraThemes.OCEAN,
                LevyraThemes.EMBER
            ),
            ids.take(6)
        )
        listOf(
            LevyraThemes.COSMIC,
            LevyraThemes.NEON_CYAN,
            LevyraThemes.PURPLE_GLASS,
            LevyraThemes.MINIMAL_WHITE,
            LevyraThemes.MOOD_FLOW
        ).forEach { legacy ->
            assertTrue("Missing legacy preset $legacy", legacy in ids)
        }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun storedPresetIdsStillResolveAfterTheCatalogGrew() {
        LevyraThemes.presets.forEach { preset ->
            assertEquals(preset.id, LevyraThemes.byId(preset.id).id)
            assertEquals(preset.id, LevyraThemes.normalize(preset.id))
        }
        assertEquals(LevyraThemes.LEVYRA_AURA, LevyraThemes.byId("apple_music").id)
        assertEquals(LevyraThemes.LEVYRA_AURA, LevyraThemes.byId("").id)
        assertEquals(LevyraThemes.LEVYRA_AURA, LevyraThemes.byId("removed_theme").id)
    }

    @Test
    fun newPresetsKeepReadableForegroundAndDistinctSurfaces() {
        listOf(LevyraThemes.frost, LevyraThemes.ocean, LevyraThemes.ember).forEach { palette ->
            assertNotEquals(palette.black, palette.panel)
            assertNotEquals(palette.panel, palette.panelSoft)
            assertTrue(
                "Insufficient text contrast in ${palette.id}",
                luminanceGap(palette.text, palette.black) > 0.4f
            )
            assertTrue(
                "Muted text too close to the surface in ${palette.id}",
                luminanceGap(palette.muted, palette.panel) > 0.1f
            )
        }
        assertTrue(LevyraThemes.frost.isLight)
        assertFalse(LevyraThemes.ocean.isLight)
        assertFalse(LevyraThemes.ember.isLight)
    }

    @Test
    fun accentOverrideOnlyRetintsAccentRoles() {
        val base = LevyraThemes.ocean
        val accent = Color(0xFFFF375F)
        val tinted = LevyraThemeController.withAccent(base, accent)

        assertEquals(base.black, tinted.black)
        assertEquals(base.panel, tinted.panel)
        assertEquals(base.text, tinted.text)
        assertEquals(base.muted, tinted.muted)
        assertEquals(accent, tinted.blue)
        assertEquals(accent, tinted.pink)
        assertNotEquals(base.cyan, tinted.cyan)
    }

    @Test
    fun accentOverrideStaysReadableOnLightAndDarkPresets() {
        val accent = Color(0xFF64D2FF)
        val onDark = LevyraThemeController.withAccent(LevyraThemes.ocean, accent)
        val onLight = LevyraThemeController.withAccent(LevyraThemes.frost, accent)

        assertTrue(luminance(onDark.cyan) > luminance(accent))
        assertTrue(luminance(onLight.cyan) < luminance(accent))
    }

    @Test
    fun pureBlackOnlyDarkensDarkPresets() {
        val dark = LevyraThemeController.asPureBlack(LevyraThemes.ember)
        assertEquals(Color.Black, dark.black)
        assertEquals(Color.Black, dark.ink)
        assertEquals(LevyraThemes.ember.text, dark.text)

        val light = LevyraThemeController.asPureBlack(LevyraThemes.frost)
        assertEquals(LevyraThemes.frost, light)
    }

    @Test
    fun studioPreviewMirrorsTheResolvedPalette() {
        val plain = themeStudioPaletteFor(LevyraThemes.OCEAN, accent = 0, pureBlack = false)
        assertEquals(LevyraThemes.ocean.black, plain.background)
        assertEquals(LevyraThemes.ocean.panel, plain.surface)
        assertEquals(LevyraThemes.ocean.cyan, plain.accent)
        assertEquals(LevyraThemes.ocean.black, plain.onAccent)

        val accentValue = 0xFFBF5AF2.toInt()
        val accented = themeStudioPaletteFor(LevyraThemes.OCEAN, accentValue, pureBlack = false)
        assertNotEquals(plain.accent, accented.accent)
        assertEquals(plain.background, accented.background)

        val blackened = themeStudioPaletteFor(LevyraThemes.OCEAN, accent = 0, pureBlack = true)
        assertEquals(Color.Black, blackened.background)

        val light = themeStudioPaletteFor(LevyraThemes.FROST, accent = 0, pureBlack = true)
        assertEquals(LevyraThemes.frost.black, light.background)
        assertEquals(Color.White, light.onAccent)
    }

    @Test
    fun accentPaletteIsBoundedAndOpaque() {
        assertEquals(LevyraThemeAccents.size, LevyraThemeAccents.toSet().size)
        assertTrue(LevyraThemeAccents.size <= 12)
        LevyraThemeAccents.forEach { value ->
            assertEquals(1f, Color(value).alpha, 0.0001f)
            assertTrue(value != 0)
        }
    }

    private fun luminance(color: Color): Float =
        0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue

    private fun luminanceGap(first: Color, second: Color): Float =
        kotlin.math.abs(luminance(first) - luminance(second))
}
