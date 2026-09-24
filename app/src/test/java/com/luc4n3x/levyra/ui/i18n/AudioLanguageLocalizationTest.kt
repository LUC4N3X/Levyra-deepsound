package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioLanguageLocalizationTest {

    @Test
    fun everySupportedLanguageShipsAudioLanguageCopy() {
        val catalogCodes = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(37, catalogCodes.size)
        assertEquals(catalogCodes, audioLanguageLocalizationCodes())

        LevyraStrings.all().forEach { strings ->
            assertTrue("audioLanguageTitle missing for ${strings.code}", strings.audioLanguageTitle.isNotBlank())
            assertTrue("audioLanguageSubtitle missing for ${strings.code}", strings.audioLanguageSubtitle.isNotBlank())
            assertTrue("audioLanguageOriginalDefault missing for ${strings.code}", strings.audioLanguageOriginalDefault.isNotBlank())
        }
    }

    @Test
    fun englishAndItalianCopyMatchExpectedWording() {
        val bundles = LevyraStrings.all().associateBy { it.code }
        val en = bundles.getValue("en")
        val it = bundles.getValue("it")

        assertEquals("Audio track language", en.audioLanguageTitle)
        assertEquals("Select preferred audio track language or keep original YouTube audio.", en.audioLanguageSubtitle)
        assertEquals("Original / Default", en.audioLanguageOriginalDefault)

        assertEquals("Lingua traccia audio", it.audioLanguageTitle)
        assertEquals("Seleziona la lingua audio preferita o mantieni l'audio originale di YouTube.", it.audioLanguageSubtitle)
        assertEquals("Originale / Predefinito", it.audioLanguageOriginalDefault)
    }

    @Test
    fun localizedTitlesHaveBroadDiversity() {
        val titles = LevyraStrings.all().map { it.audioLanguageTitle }.toSet()
        assertTrue("Expected broad translation diversity across 37 languages", titles.size >= 25)
    }
}
