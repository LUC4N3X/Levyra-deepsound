package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSearchLiveUpdateLocalizationTest {
    @Test
    fun everyCatalogLanguageShipsSettingsSearchAndLiveUpdateCopy() {
        val codes = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(codes, settingsSearchLocalizationCodes())
        assertEquals(codes, liveUpdateLocalizationCodes())
        LevyraStrings.all().forEach { strings ->
            assertTrue(strings.code, strings.settingsSearchPlaceholder.isNotBlank())
            assertTrue(strings.code, strings.settingsSearchEmpty.isNotBlank())
            assertTrue(strings.code, strings.liveUpdatePlaybackChannel.isNotBlank())
        }
    }

    @Test
    fun nonEnglishLanguagesDoNotFallBackToEnglish() {
        val english = LevyraStrings.forCode("en")
        LevyraStrings.all().filterNot { it.code == "en" }.forEach { strings ->
            assertNotEquals(strings.code, english.settingsSearchPlaceholder, strings.settingsSearchPlaceholder)
            assertNotEquals(strings.code, english.settingsSearchEmpty, strings.settingsSearchEmpty)
            assertNotEquals(strings.code, english.liveUpdatePlaybackChannel, strings.liveUpdatePlaybackChannel)
        }
    }

    @Test
    fun englishAndItalianCopyMatchTheProductWording() {
        assertEquals("Search settings", LevyraStrings.forCode("en").settingsSearchPlaceholder)
        assertEquals("No settings found", LevyraStrings.forCode("en").settingsSearchEmpty)
        assertEquals("Cerca nelle impostazioni", LevyraStrings.forCode("it").settingsSearchPlaceholder)
        assertEquals("Nessuna impostazione trovata", LevyraStrings.forCode("it").settingsSearchEmpty)
    }
}
