package com.luc4n3x.levyra.ui.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraLyricsFontStringsTest {
    @Test
    fun everySupportedLanguageHasItsOwnFontLibraryCopy() {
        val supportedCodes = LevyraStrings.all().map { it.code }.toSet()
        assertEquals(supportedCodes, lyricsFontLocalizationCodes())
        val english = lyricsFontLocalizationEntries("en")
        supportedCodes.forEach { code ->
            val entries = lyricsFontLocalizationEntries(code)
            assertEquals("Incomplete bundle for $code", lyricsFontKeys, entries.keys)
            assertTrue(entries.values.all { it.isNotBlank() })
            if (code != "en") assertNotEquals("English fallback for $code", english["lyricsFontsHint"], entries["lyricsFontsHint"])
        }
    }

    @Test
    fun stringsAreExposedThroughLevyraStrings() {
        assertEquals("I miei font", LevyraStrings.forCode("it").lyricsFontsTitle)
        assertEquals("我的字型", LevyraStrings.forCode("zh-Hant").lyricsFontsTitle)
        assertEquals("Huge", LevyraStrings.forCode("en").lyricsStudioSizeHuge)
    }
}
