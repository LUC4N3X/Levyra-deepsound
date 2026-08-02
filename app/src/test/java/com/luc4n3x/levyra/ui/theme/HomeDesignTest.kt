package com.luc4n3x.levyra.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeDesignTest {
    @Test
    fun `header keeps a premium touch friendly scale`() {
        assertTrue(LevyraHomeDesign.SettingsControlHeight >= LevyraPlayerDesign.MinimumTouchTarget)
        assertTrue(LevyraHomeDesign.HeaderCorner < LevyraHomeDesign.SettingsControlHeight)
        assertTrue(LevyraHomeDesign.HeaderPadding.value > 0f)
    }

    @Test
    fun `home hierarchy stays tighter than the expressive player scale`() {
        assertTrue(LevyraHomeDesign.HeaderCorner <= LevyraPlayerDesign.CornerLg)
        assertTrue(LevyraHomeDesign.HeroCorner <= LevyraPlayerDesign.CornerMd)
        assertTrue(LevyraHomeDesign.AtmosphereHeight > LevyraHomeDesign.HeroHeight)
        assertEquals(18f, LevyraHomeDesign.HorizontalInset.value, 0f)
    }
}
