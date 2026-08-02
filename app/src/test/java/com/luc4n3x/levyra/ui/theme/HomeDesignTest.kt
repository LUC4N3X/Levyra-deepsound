package com.luc4n3x.levyra.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeDesignTest {
    @Test
    fun `quick access keeps a compact touch friendly scale`() {
        assertTrue(LevyraHomeDesign.TileHeight >= LevyraPlayerDesign.MinimumTouchTarget)
        assertTrue(LevyraHomeDesign.ArtworkSize <= LevyraHomeDesign.TileHeight)
        assertTrue(LevyraHomeDesign.TileGap > LevyraHomeDesign.TileBorderDark.alpha.dpCompat())
    }

    @Test
    fun `home hierarchy stays tighter than the expressive player scale`() {
        assertTrue(LevyraHomeDesign.TileCorner < LevyraPlayerDesign.CornerMd)
        assertTrue(LevyraHomeDesign.HeroCorner <= LevyraPlayerDesign.CornerMd)
        assertEquals(18f, LevyraHomeDesign.HorizontalInset.value, 0f)
    }
}

private fun Float.dpCompat() = androidx.compose.ui.unit.dp
