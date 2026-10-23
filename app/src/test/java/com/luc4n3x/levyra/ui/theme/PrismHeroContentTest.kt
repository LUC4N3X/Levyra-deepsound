package com.luc4n3x.levyra.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PrismHeroContentTest {

    @Test
    fun youTubeMusicPrimaryButtonUsesDarkGlyphOnItsWhiteHero() {
        val palette = youTubeMusicPalette()
        val surfaces = prismPlayerSurfaces(palette)

        assertEquals(palette.cyan, surfaces.hero)
        assertEquals(Color.Black, surfaces.heroContent)
        assertNotEquals(surfaces.hero, surfaces.heroContent)
    }

    @Test
    fun darkAccentsKeepTheirWhiteGlyph() {
        val palette = youTubeMusicPalette().copy(id = "synthetic_dark_accent", cyan = Color(0xFFC2185E))

        assertEquals(Color.White, prismHeroContent(palette))
    }
}
