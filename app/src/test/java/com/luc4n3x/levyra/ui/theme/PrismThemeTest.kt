package com.luc4n3x.levyra.ui.theme

import com.luc4n3x.levyra.domain.*
import com.luc4n3x.levyra.ui.playerContrastRatio
import com.luc4n3x.levyra.ui.player.playerSheetColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.hct.Hct
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class PrismThemeTest {
    @After fun restore() { LevyraThemeController.apply(LevyraThemes.LEVYRA_AURA) }

    @Test fun appearancesHaveReadableContent() {
        listOf(PrismAppearance.Night, PrismAppearance.Light).forEach { appearance ->
            val p = prismPalette(PrismSettings(appearance = appearance))
            listOf(p.black, p.ink, p.panel, p.panelSoft).forEach { surface ->
                assertTrue("Text $appearance", playerContrastRatio(p.text, surface) >= 4.5f)
                assertTrue("Secondary $appearance", playerContrastRatio(p.muted, surface) >= 4.5f)
                assertTrue("Accent $appearance", playerContrastRatio(p.cyan, surface) >= 3f)
            }
            val scheme = levyraColorScheme(p)
            assertTrue(playerContrastRatio(scheme.primary, scheme.onPrimary) >= 4.5f)
        }
    }

    @Test fun nightIsDeepBlackWithFixedRaspberryAccentAndNoBlue() {
        val night = prismPalette()
        assertEquals(Color.Black, night.black)
        listOf(PrismAppearance.Night, PrismAppearance.Light).forEach { appearance ->
            val p = prismPalette(PrismSettings(appearance = appearance))
            listOf(p.cyan, p.blue, p.violet, p.pink, p.orange).forEach { accent ->
                val hue = Hct.fromInt(accent.toArgb()).hue
                assertTrue("Accent hue $hue $appearance", hue !in 180.0..300.0)
            }
            assertEquals(p, prismPalette(PrismSettings(appearance = appearance)))
        }
    }

    @Test fun torqueBandAndAccentStayReadableForAnyCover() {
        val covers = listOf(0xFFFFFF00, 0xFF0000FF, 0xFF00FF00, 0xFF222222, 0xFFFFFFFF, 0xFF8B5A2B, 0xFFFF0000)
        covers.forEach { cover ->
            val p = torquePalette(TorqueSettings(TorqueAccent.Cover), cover.toInt())
            assertTrue("Band $cover", playerContrastRatio(Color.White, p.blue) >= 3f)
            listOf(p.black, p.ink, p.panel, p.panelSoft).forEach { surface ->
                assertTrue("Accent $cover", playerContrastRatio(p.cyan, surface) >= 3f)
                assertTrue("Text $cover", playerContrastRatio(p.text, surface) >= 4.5f)
                assertTrue("Muted $cover", playerContrastRatio(p.muted, surface) >= 4.5f)
            }
        }
    }

    @Test fun torqueFallsBackToRedAndRedIgnoresCover() {
        assertEquals(Color(TorqueRed), torquePalette().blue)
        assertEquals(torquePalette(TorqueSettings(TorqueAccent.Red), 0xFF00FF00.toInt()), torquePalette(TorqueSettings(TorqueAccent.Red)))
        LevyraThemeController.apply(LevyraThemes.FROST, skin = LevyraSkin.Torque)
        assertTrue(LevyraIsTorque)
        assertTrue(LevyraIsPrism)
        assertTrue(LevyraSkinManagesLayout)
    }

    @Test fun ivoryDefaultsToLightAndKeepsInkReadableInBothAppearances() {
        assertTrue(ivoryPalette().isLight)
        assertFalse(ivoryPalette(IvorySettings(IvoryAppearance.Dark)).isLight)
        assertTrue(ivoryPalette(IvorySettings(IvoryAppearance.Automatic), systemDark = false).isLight)
        assertFalse(ivoryPalette(IvorySettings(IvoryAppearance.Automatic), systemDark = true).isLight)
        IvoryAppearance.entries.forEach { appearance ->
            val p = ivoryPalette(IvorySettings(appearance), systemDark = true)
            assertTrue(playerContrastRatio(p.text, p.black) >= 7f)
            assertTrue(playerContrastRatio(p.muted, p.black) >= 4.5f)
            assertTrue(playerContrastRatio(p.black, p.blue) >= 7f)
        }
        LevyraThemeController.apply(LevyraThemes.FROST, skin = LevyraSkin.Ivory)
        assertTrue(LevyraIsIvory)
        assertTrue(LevyraIsPrism)
        assertFalse(LevyraIsTorque)
        assertTrue(LevyraSkinManagesLayout)
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

    @Test fun switchingSkinsUpdatesMiniPlayerCornerAfterDesignerNormalization() {
        val saved = LevyraMiniPlayerStudioSettings()
        LevyraThemeController.apply(LevyraThemes.COSMIC, skin = LevyraSkin.Prism)
        assertEquals(20, skinMiniPlayerCorner(prismMiniSettings(saved).activeTuning()).value.toInt())
        LevyraThemeController.apply(LevyraThemes.COSMIC, skin = LevyraSkin.Torque)
        assertEquals(2, skinMiniPlayerCorner(prismMiniSettings(saved).activeTuning()).value.toInt())
        LevyraThemeController.apply(LevyraThemes.COSMIC, skin = LevyraSkin.Prism)
        assertEquals(20, skinMiniPlayerCorner(prismMiniSettings(saved).activeTuning()).value.toInt())
        LevyraThemeController.apply(LevyraThemes.COSMIC, skin = LevyraSkin.Nocturne78)
        val nocturneTuning = nocturneMiniSettings(saved).activeTuning()
        assertEquals(10, skinMiniPlayerCorner(nocturneTuning).value.toInt())
        LevyraThemeController.apply(LevyraThemes.COSMIC, skin = LevyraSkin.Default)
        assertEquals(saved.activeTuning().cornerDp, skinMiniPlayerCorner(saved.activeTuning()).value.toInt())
        assertEquals(LevyraMiniPlayerStudioSettings(), saved)
    }
}
