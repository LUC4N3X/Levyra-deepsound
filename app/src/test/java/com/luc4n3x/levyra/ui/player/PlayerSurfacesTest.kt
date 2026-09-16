package com.luc4n3x.levyra.ui.player

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.PlayerVisualMode
import com.luc4n3x.levyra.ui.PlayerDarkSurface
import com.luc4n3x.levyra.ui.playerCompositeOver
import com.luc4n3x.levyra.ui.playerContrastRatio
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LevyraPlayerShapes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerSurfacesTest {

    private val accents = listOf(
        Color(0xFFE53935),
        Color(0xFF1E88E5),
        Color(0xFFFFEB3B),
        Color(0xFF202020),
        Color(0xFFF5F5F5),
        Color(0xFF3A1F1A)
    )

    @Test
    fun `the play control stays readable for every artwork accent`() {
        accents.forEach { accent ->
            listOf(false, true).forEach { amoled ->
                val tokens = playerSurfaceTokens(accent, amoled)
                assertTrue(
                    "hero contrast for $accent",
                    playerContrastRatio(tokens.heroContent, tokens.hero) >= 4.5f
                )
            }
        }
    }

    @Test
    fun `active toggles keep readable content over their tinted fill`() {
        accents.forEach { accent ->
            val tonal = playerSurfaceTokens(accent, amoled = false)
            val tonalFill = tonal.active.playerCompositeOver(PlayerDarkSurface)
            assertTrue(playerContrastRatio(tonal.activeContent, tonalFill) >= 4.5f)

            val amoled = playerSurfaceTokens(accent, amoled = true)
            val amoledFill = amoled.active.playerCompositeOver(Color.Black)
            assertTrue(playerContrastRatio(amoled.activeContent, amoledFill) >= 4.5f)
        }
    }

    @Test
    fun `amoled controls stay near black and carry an outline`() {
        accents.forEach { accent ->
            val tokens = playerSurfaceTokens(accent, amoled = true)
            assertTrue(tokens.amoled)
            assertTrue(tokens.control.luminance() < 0.06f)
            assertTrue(tokens.controlQuiet.luminance() <= tokens.control.luminance())
            assertTrue(tokens.outline.alpha > 0f)
        }
        assertFalse(playerSurfaceTokens(accents.first(), amoled = false).amoled)
    }

    @Test
    fun `the visual mode button cycles through every mode`() {
        assertEquals(PlayerVisualMode.CanvasCard, nextPlayerVisualMode(PlayerVisualMode.Artwork))
        assertEquals(PlayerVisualMode.CanvasImmersive, nextPlayerVisualMode(PlayerVisualMode.CanvasCard))
        assertEquals(PlayerVisualMode.Artwork, nextPlayerVisualMode(PlayerVisualMode.CanvasImmersive))
    }

    @Test
    fun `playback speed labels drop redundant zeros`() {
        assertEquals("1", formatPlaybackSpeed(1f))
        assertEquals("1.25", formatPlaybackSpeed(1.25f))
        assertEquals("1.5", formatPlaybackSpeed(1.5f))
        assertEquals("0.75", formatPlaybackSpeed(0.75f))
    }

    @Test
    fun `artwork corners scale with size inside the design bounds`() {
        assertEquals(LevyraPlayerDesign.ArtworkCornerMin, LevyraPlayerShapes.artworkCorner(120.dp))
        assertEquals(LevyraPlayerDesign.ArtworkCornerMax, LevyraPlayerShapes.artworkCorner(900.dp))
        val phone = LevyraPlayerShapes.artworkCorner(360.dp)
        assertTrue(phone > LevyraPlayerDesign.ArtworkCornerMin && phone < LevyraPlayerDesign.ArtworkCornerMax)
    }
}
