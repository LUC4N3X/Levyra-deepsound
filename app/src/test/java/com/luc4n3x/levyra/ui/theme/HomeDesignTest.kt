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
}
