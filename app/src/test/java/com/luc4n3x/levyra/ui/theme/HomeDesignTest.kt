package com.luc4n3x.levyra.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeDesignTest {
    @Test
    fun `quick access keeps a compact touch friendly scale`() {
        assertTrue(LevyraHomeDesign.TileHeight >= LevyraPlayerDesign.MinimumTouchTarget)
        assertTrue(LevyraHomeDesign.ArtworkSize <= LevyraHomeDesign.TileHeight)
        assertTrue(LevyraHomeDesign.TileGap.value > 0f)
        assertTrue(LevyraHomeDesign.TileCorner < LevyraHomeDesign.TileHeight)
        assertEquals(
            14f,
            (LevyraHomeDesign.TileHeight - LevyraHomeDesign.ArtworkSize).value,
            0f
        )
    }

    @Test
    fun `home hierarchy stays tighter than the expressive player scale`() {
        assertTrue(LevyraHomeDesign.TileCorner < LevyraPlayerDesign.CornerMd)
        assertTrue(LevyraHomeDesign.HeroCorner <= LevyraPlayerDesign.CornerMd)
        assertTrue(LevyraHomeDesign.AtmosphereHeight > LevyraHomeDesign.HeroHeight)
        assertEquals(18f, LevyraHomeDesign.HorizontalInset.value, 0f)
    }
}
