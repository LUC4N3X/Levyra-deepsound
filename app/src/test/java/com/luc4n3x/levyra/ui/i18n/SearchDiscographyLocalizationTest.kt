package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchDiscographyLocalizationTest {
    @Test
    fun everySupportedLanguageShipsDiscographyCopy() {
        val catalogCodes = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(catalogCodes, searchDiscographyLocalizationCodes())
        LevyraStrings.all().forEach { strings ->
            val copy = searchDiscographyCopy(strings.code)
            assertTrue(strings.code, listOf(copy.discography, copy.album, copy.single, copy.featuredOn).all(String::isNotBlank))
        }
    }

    @Test
    fun translatedDiscographyTitlesDoNotFallBackToEnglish() {
        val english = searchDiscographyCopy("en")
        assertNotEquals(english.discography, searchDiscographyCopy("it").discography)
        assertNotEquals(english.featuredOn, searchDiscographyCopy("de").featuredOn)
        assertEquals(english, searchDiscographyCopy("unsupported"))
    }
}
