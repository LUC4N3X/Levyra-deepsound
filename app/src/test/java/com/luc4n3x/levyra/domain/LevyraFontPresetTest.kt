package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LevyraFontPresetTest {

    @Test
    fun parsesPresetNamesIgnoringCaseAndWhitespace() {
        assertEquals(LevyraFontPreset.Manrope, LevyraFontPreset.from("  manROPE "))
        assertEquals(LevyraFontPreset.Montserrat, LevyraFontPreset.from("Montserrat"))
    }

    @Test
    fun unknownPresetFallsBackToOutfit() {
        assertEquals(LevyraFontPreset.Outfit, LevyraFontPreset.from("not-a-font"))
    }

    @Test
    fun systemPresetIsLanguageNeutralAndNeedsNoGoogleFont() {
        assertEquals("System", LevyraFontPreset.System.displayName)
        assertEquals(null, LevyraFontPreset.System.googleFontName)
    }
}
