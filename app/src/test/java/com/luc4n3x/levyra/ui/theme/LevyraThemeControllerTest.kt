package com.luc4n3x.levyra.ui.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraThemeControllerTest {

    @Test
    fun amoledPaletteDoesNotImplicitlyEnablePureBlackMode() {
        try {
            LevyraThemeController.apply(LevyraThemes.AMOLED, pureBlack = false)
            assertFalse(LevyraIsPureBlack)

            LevyraThemeController.apply(LevyraThemes.AMOLED, pureBlack = true)
            assertTrue(LevyraIsPureBlack)

            LevyraThemeController.apply(LevyraThemes.MINIMAL_WHITE, pureBlack = true)
            assertFalse(LevyraIsPureBlack)
        } finally {
            LevyraThemeController.apply(LevyraThemes.COSMIC, pureBlack = false)
        }
    }
}
