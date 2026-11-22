package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualCraftLocalizationTest {
    @Test
    fun everySupportedLanguageShipsCompleteCopy() {
        val catalogCodes = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(catalogCodes, visualCraftLocalizationCodes())
        assertTrue(visualCraftIncompleteLocalizationCodes().toString(), visualCraftIncompleteLocalizationCodes().isEmpty())
    }

    @Test
    fun translatedLanguagesDoNotFallBackToEnglish() {
        val english = LevyraStrings.forCode("en").visualCraftCopy
        LevyraStrings.all().filter { it.code != "en" }.forEach { strings ->
            val copy = strings.visualCraftCopy
            assertNotEquals(strings.code, english[VisualCraftText.ArtworkCropSubtitle], copy[VisualCraftText.ArtworkCropSubtitle])
            assertNotEquals(strings.code, english[VisualCraftText.WallpaperColorsUnavailable], copy[VisualCraftText.WallpaperColorsUnavailable])
        }
    }
}
