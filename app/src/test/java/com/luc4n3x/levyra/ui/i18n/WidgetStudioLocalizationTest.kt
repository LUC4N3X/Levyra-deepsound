package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetStudioLocalizationTest {
    @Test
    fun everySupportedLanguageShipsCompleteWidgetStudioCopy() {
        val catalogCodes = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(catalogCodes, widgetStudioLocalizationCodes())
        assertTrue(widgetStudioIncompleteLocalizationCodes().toString(), widgetStudioIncompleteLocalizationCodes().isEmpty())
    }

    @Test
    fun translatedLanguagesDoNotFallBackToEnglish() {
        val english = LevyraStrings.forCode("en").widgetStudioCopy
        LevyraStrings.all().filter { it.code != "en" }.forEach { strings ->
            val copy = strings.widgetStudioCopy
            assertNotEquals(strings.code, english[WidgetStudioText.Intro], copy[WidgetStudioText.Intro])
            assertNotEquals(strings.code, english[WidgetStudioText.ProgressRefreshHint], copy[WidgetStudioText.ProgressRefreshHint])
        }
    }
}
