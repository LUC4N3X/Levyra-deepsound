package com.luc4n3x.levyra.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.luc4n3x.levyra.ui.playerContrastRatio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraColorSchemeTest {

    private val palettes = LevyraThemes.presets + LevyraThemes.presets.map(LevyraThemeController::asPureBlack)

    @Test
    fun `tonal containers keep readable content for every preset`() {
        palettes.forEach { palette ->
            val scheme = levyraColorScheme(palette)
            listOf(
                scheme.onPrimaryContainer to scheme.primaryContainer,
                scheme.onSecondaryContainer to scheme.secondaryContainer,
                scheme.onTertiaryContainer to scheme.tertiaryContainer
            ).forEach { (content, container) ->
                assertTrue(
                    "${palette.id} container contrast",
                    playerContrastRatio(content, container) >= 4.5f
                )
            }
        }
    }

    @Test
    fun `accent roles keep readable content for every preset`() {
        palettes.forEach { palette ->
            val scheme = levyraColorScheme(palette)
            listOf(
                "primary" to (scheme.onPrimary to scheme.primary),
                "secondary" to (scheme.onSecondary to scheme.secondary),
                "tertiary" to (scheme.onTertiary to scheme.tertiary)
            ).forEach { (role, pair) ->
                val (content, accent) = pair
                assertTrue(
                    "${palette.id} $role contrast",
                    playerContrastRatio(content, accent) >= 4.5f
                )
            }
        }
    }

    @Test
    fun `accent roles keep brand colors that already pass`() {
        palettes.forEach { palette ->
            val scheme = levyraColorScheme(palette)
            if (playerContrastRatio(scheme.onPrimary, palette.cyan) >= 4.5f) {
                assertEquals(palette.cyan, scheme.primary)
            }
            assertEquals(scheme.primary, scheme.surfaceTint)
        }
    }

    @Test
    fun `minimal white primary darkens just enough for white content`() {
        val minimalWhite = LevyraThemes.presets.first { it.id == LevyraThemes.MINIMAL_WHITE }
        val scheme = levyraColorScheme(minimalWhite)
        assertEquals(Color.White, scheme.onPrimary)
        assertTrue(playerContrastRatio(Color.White, minimalWhite.cyan) < 4.5f)
        assertTrue(playerContrastRatio(scheme.onPrimary, scheme.primary) >= 4.5f)
        assertTrue(playerContrastRatio(scheme.onPrimary, scheme.primary) < 5.2f)
    }

    @Test
    fun `surface ladder follows the levyra palette`() {
        palettes.forEach { palette ->
            val scheme = levyraColorScheme(palette)
            assertEquals(palette.black, scheme.surfaceContainerLowest)
            assertEquals(palette.ink, scheme.surfaceContainerLow)
            assertEquals(palette.panel, scheme.surfaceContainer)
            assertEquals(palette.panelSoft, scheme.surfaceContainerHigh)
            assertTrue(playerContrastRatio(scheme.inverseOnSurface, scheme.inverseSurface) >= 4.5f)
        }
    }

    @Test
    fun `dark containers stay darker than their content`() {
        palettes.filterNot { it.isLight }.forEach { palette ->
            val scheme = levyraColorScheme(palette)
            assertTrue(scheme.primaryContainer.luminance() < scheme.onPrimaryContainer.luminance())
            assertTrue(scheme.secondaryContainer.luminance() < scheme.onSecondaryContainer.luminance())
        }
    }
}
