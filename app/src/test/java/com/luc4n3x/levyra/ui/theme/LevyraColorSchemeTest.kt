package com.luc4n3x.levyra.ui.theme

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
    fun `surface ladder follows the levyra palette`() {
        palettes.forEach { palette ->
            val scheme = levyraColorScheme(palette)
            assertEquals(palette.black, scheme.surfaceContainerLowest)
            assertEquals(palette.ink, scheme.surfaceContainerLow)
            assertEquals(palette.panel, scheme.surfaceContainer)
            assertEquals(palette.panelSoft, scheme.surfaceContainerHigh)
            assertEquals(palette.cyan, scheme.primary)
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
