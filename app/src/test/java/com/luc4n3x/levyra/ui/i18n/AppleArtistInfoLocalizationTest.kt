package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppleArtistInfoLocalizationTest {
    @Test
    fun everySupportedLanguageShipsArtistInfoCopy() {
        val catalogCodes = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(catalogCodes, appleArtistInfoLocalizationCodes())
        LevyraStrings.all().forEach { strings ->
            val copy = appleArtistInfoCopy(strings.code)
            assertTrue(strings.code, listOf(copy.about, copy.hometown, copy.born, copy.formed, copy.genre, copy.topVideos).all(String::isNotBlank))
        }
    }

    @Test
    fun topVideosTitleIsTranslated() {
        assertEquals("Top video", appleArtistInfoCopy("it").topVideos)
        val english = appleArtistInfoCopy("en").topVideos
        listOf("es", "fr", "de", "ru", "ja", "zh", "ar").forEach { code ->
            assertNotEquals(code, english, appleArtistInfoCopy(code).topVideos)
        }
    }
}
