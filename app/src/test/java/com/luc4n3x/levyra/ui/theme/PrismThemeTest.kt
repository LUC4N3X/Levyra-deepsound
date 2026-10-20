package com.luc4n3x.levyra.ui.theme

import com.luc4n3x.levyra.domain.*
import com.luc4n3x.levyra.ui.playerContrastRatio
import com.luc4n3x.levyra.ui.player.playerSheetColor
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class PrismThemeTest {
    @After fun restore() { LevyraThemeController.apply(LevyraThemes.LEVYRA_AURA) }

    @Test fun artworkPresetsAndAppearancesHaveReadableContent() {
        val seeds = listOf(0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFF222222, 0xFFFFFFFF)
        PrismColors.entries.forEach { preset ->
            listOf(PrismAppearance.Night, PrismAppearance.Light).forEach { appearance ->
                seeds.forEach { seed ->
                    val p = prismPalette(PrismSettings(appearance = appearance, colors = preset), artworkStart = seed.toInt(), artworkEnd = seed.toInt())
                    listOf(p.black, p.ink, p.panel, p.panelSoft).forEach { surface ->
                        assertTrue("Text $appearance $preset", playerContrastRatio(p.text, surface) >= 4.5f)
                        assertTrue("Secondary $appearance $preset", playerContrastRatio(p.muted, surface) >= 4.5f)
                        assertTrue("Accent $appearance $preset", playerContrastRatio(p.cyan, surface) >= 3f)
                    }
                    val scheme = levyraColorScheme(p)
                    assertTrue(playerContrastRatio(scheme.primary, scheme.onPrimary) >= 4.5f)
                }
            }
        }
    }

    @Test fun fixedPresetsIgnoreArtworkWhileAdaptiveResponds() {
        PrismColors.entries.forEach { colors ->
            val settings = PrismSettings(colors = colors)
            val red = prismPalette(settings, artworkStart = 0xFFFF0000.toInt(), artworkEnd = 0xFFFF5500.toInt())
            val blue = prismPalette(settings, artworkStart = 0xFF0000FF.toInt(), artworkEnd = 0xFF0088FF.toInt())
            if (colors == PrismColors.Adaptive) assertNotEquals(red.cyan, blue.cyan) else assertEquals(red, blue)
        }
    }

    @Test fun playerSheetsKeepReadableContentInBothAppearances() {
        listOf(PrismAppearance.Light, PrismAppearance.Night).forEach { appearance ->
            val settings = PrismSettings(appearance = appearance)
            LevyraThemeController.apply(LevyraThemes.COSMIC, skin = LevyraSkin.Prism, prism = settings)
            val surfaces = prismPlayerSurfaces(LevyraActivePalette)
            assertTrue(playerContrastRatio(surfaces.content, playerSheetColor(surfaces)) >= 4.5f)
            assertTrue(playerContrastRatio(surfaces.contentMuted, playerSheetColor(surfaces)) >= 4.5f)
        }
    }

    @Test fun automaticFollowsSystemAndNightDoesNot() {
        assertTrue(prismPalette(PrismSettings(appearance = PrismAppearance.Automatic), systemDark = false).isLight)
        assertFalse(prismPalette(PrismSettings(appearance = PrismAppearance.Automatic), systemDark = true).isLight)
        assertFalse(prismPalette(systemDark = false).isLight)
    }

    @Test fun switchingSkinsRestoresDefaultAndNocturneExactly() {
        LevyraThemeController.apply(LevyraThemes.FROST, accentOverride = 0xFF3355AA.toInt())
        val saved = LevyraActivePalette
        LevyraThemeController.apply(LevyraThemes.FROST, skin = LevyraSkin.Prism)
        assertTrue(LevyraSkinManagesLayout)
        assertTrue(LevyraIsPrism)
        LevyraThemeController.apply(LevyraThemes.FROST, skin = LevyraSkin.Nocturne78)
        assertEquals(Nocturne78Palette, LevyraActivePalette)
        assertFalse(LevyraIsPrism)
        LevyraThemeController.apply(LevyraThemes.FROST, accentOverride = 0xFF3355AA.toInt())
        assertEquals(saved, LevyraActivePalette)
        assertFalse(LevyraSkinManagesLayout)
    }

    @Test fun projectedDockSettingsKeepSavedCustomizationIntact() {
        val mini = LevyraMiniPlayerStudioSettings()
        val nav = LevyraNavigationStudioSettings()
        assertEquals(mini.activeTuning().controls, prismMiniSettings(mini).activeTuning().controls)
        assertEquals(nav.activeTuning().order, prismNavSettings(nav).activeTuning().order)
        assertEquals(nav.activeTuning().hidden, prismNavSettings(nav).activeTuning().hidden)
        assertEquals(LevyraMiniPlayerStudioSettings(), mini)
        assertEquals(LevyraNavigationStudioSettings(), nav)
    }
}
