package com.luc4n3x.levyra.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeDesignTest {
    @Test
    fun `header keeps a premium touch friendly scale`() {
        assertTrue(LevyraHomeDesign.SettingsControlHeight >= LevyraPlayerDesign.MinimumTouchTarget)
        assertTrue(LevyraHomeDesign.MoodChipHeight >= LevyraPlayerDesign.MinimumTouchTarget)
        assertTrue(LevyraHomeDesign.HeaderCorner < LevyraHomeDesign.SettingsControlHeight)
        assertTrue(LevyraHomeDesign.HeaderPadding.value > 0f)
    }

    @Test
    fun `home hierarchy keeps a clear editorial scale`() {
        assertTrue(LevyraHomeDesign.HeaderCorner <= LevyraPlayerDesign.CornerLg)
        assertTrue(LevyraHomeDesign.HeroCorner > LevyraHomeDesign.ShelfCorner)
        assertTrue(LevyraHomeDesign.HeroHeight.value >= 240f)
        assertTrue(LevyraHomeDesign.SectionGap > LevyraHomeDesign.SectionGapCompact)
        assertEquals(18f, LevyraHomeDesign.HorizontalInset.value, 0f)
    }

    @Test
    fun `sections separate more than a header separates from its content`() {
        assertTrue(LevyraHomeDesign.SectionStride > LevyraHomeDesign.SectionStrideCompact)
        assertTrue(LevyraHomeDesign.SectionStride > LevyraHomeDesign.SectionGap * 2f)
        assertTrue(LevyraHomeDesign.SectionStrideCompact > LevyraHomeDesign.SectionGapCompact * 2f)
        assertTrue(LevyraHomeDesign.sectionLead(compact = false) > LevyraHomeDesign.sectionLead(compact = true))
        assertEquals(
            LevyraHomeDesign.SectionStride.value,
            (LevyraHomeDesign.sectionLead(compact = false) + LevyraHomeDesign.sectionGap(compact = false)).value,
            0.001f
        )
        assertEquals(
            LevyraHomeDesign.SectionStrideCompact.value,
            (LevyraHomeDesign.sectionLead(compact = true) + LevyraHomeDesign.sectionGap(compact = true)).value,
            0.001f
        )
    }

    @Test
    fun `artwork cards keep one radius family and two widths`() {
        assertTrue(LevyraHomeDesign.ArtworkCorner > LevyraHomeDesign.ShelfCorner)
        assertTrue(LevyraHomeDesign.ArtworkCorner < LevyraHomeDesign.HeroCorner)
        assertTrue(LevyraHomeDesign.ArtworkCardWidth > LevyraHomeDesign.ArtworkGridCardWidth)
        assertTrue(LevyraHomeDesign.ShelfItemGap.value > 0f)
    }
}
