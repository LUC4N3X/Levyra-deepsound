package com.luc4n3x.levyra.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.luc4n3x.levyra.domain.LevyraSkin
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Nocturne78Test {
    @After
    fun restoreDefault() {
        LevyraThemeController.apply(LevyraThemes.LEVYRA_AURA)
    }

    @Test
    fun fixedSkinIgnoresArtworkAccentPureBlackAndLightPalette() {
        LevyraThemeController.apply(
            presetId = LevyraThemes.MINIMAL_WHITE,
            coverAccentStart = 0xFFFF0000.toInt(), coverAccentEnd = 0xFF00FF00.toInt(),
            pureBlack = true, accentOverride = 0xFF0000FF.toInt(), skin = LevyraSkin.Nocturne78
        )
        assertEquals(Nocturne78Palette, LevyraActivePalette)
        assertFalse(LevyraActivePalette.isLight)
        assertEquals(Color.Black, LevyraInk)
    }

    @Test
    fun returningToDefaultRestoresTheSelectedPaletteAndAccent() {
        val accent = 0xFF60A5FA.toInt()
        LevyraThemeController.apply(LevyraThemes.FROST, accentOverride = accent)
        val original = LevyraActivePalette
        LevyraThemeController.apply(LevyraThemes.FROST, accentOverride = accent, skin = LevyraSkin.Nocturne78)
        assertTrue(LevyraIsNocturne)
        LevyraThemeController.apply(LevyraThemes.FROST, accentOverride = accent, skin = LevyraSkin.Default)
        assertEquals(original, LevyraActivePalette)
        assertFalse(LevyraIsNocturne)
    }

    @Test
    fun textAndInteractiveColorsMeetContrastOnAllNocturneSurfaces() {
        val p = Nocturne78Palette
        val surfaces = listOf(p.black, p.ink, p.panel, p.panelSoft, Nocturne78Design.variant)
        surfaces.forEach { surface ->
            listOf(p.text, p.muted, p.cyan).forEach { foreground ->
                val contrast = (foreground.luminance() + 0.05f) / (surface.luminance() + 0.05f)
                assertTrue("Contrast $contrast on $surface", contrast >= 4.5f)
            }
        }
        val scheme = levyraColorScheme(p)
        assertTrue((scheme.primary.luminance() + 0.05f) / (scheme.onPrimary.luminance() + 0.05f) >= 4.5f)
    }

    @Test
    fun missingOrUnknownSkinUsesDefault() {
        assertEquals(LevyraSkin.Default, LevyraSkin.from(""))
        assertEquals(LevyraSkin.Default, LevyraSkin.from("future_skin"))
    }
}
