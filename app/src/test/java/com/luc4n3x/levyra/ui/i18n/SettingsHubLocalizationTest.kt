package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsHubLocalizationTest {
    @Test
    fun everySupportedLanguageShipsCompleteSettingsHubCopy() {
        val catalogCodes = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(catalogCodes, settingsHubLocalizationCodes())
        assertTrue(settingsHubIncompleteLocalizationCodes().toString(), settingsHubIncompleteLocalizationCodes().isEmpty())
    }

    @Test
    fun brazilianPortugueseShipsItsOwnCompleteCopy() {
        val brazil = LevyraStrings.forCode("pt-BR").settingsHubCopy
        val portugal = LevyraStrings.forCode("pt").settingsHubCopy
        assertTrue(brazil.complete)
        assertEquals("Quando baixar", brazil[SettingsHubText.WhenToDownload])
        assertNotEquals(portugal[SettingsHubText.WhenToDownload], brazil[SettingsHubText.WhenToDownload])
    }

    @Test
    fun translatedLanguagesDoNotFallBackToEnglishGroupTitles() {
        val english = LevyraStrings.forCode("en").settingsHubCopy
        LevyraStrings.all().filter { it.code != "en" }.forEach { strings ->
            assertNotEquals(strings.code, english[SettingsHubText.StudiosHint], strings.settingsHubCopy[SettingsHubText.StudiosHint])
        }
    }
}
