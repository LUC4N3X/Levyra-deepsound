package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlternativeAudioLocalizationTest {
    @Test
    fun everySupportedLanguageShipsTheAlternativeAudioCopy() {
        val catalogCodes = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(catalogCodes, alternativeAudioLocalizationCodes())
        LevyraStrings.all().forEach { strings ->
            assertTrue(strings.code, strings.alternativeAudioTitle.isNotBlank())
            assertTrue(strings.code, strings.alternativeAudioSubtitle.isNotBlank())
            assertTrue(strings.code, strings.alternativeAudioOff.isNotBlank())
            assertTrue(strings.code, strings.alternativeAudioAutomatic.isNotBlank())
            assertTrue(strings.code, strings.alternativeAudioPrefer320.contains("320"))
        }
    }

    @Test
    fun modeLabelsStayDistinctInsideOneBundle() {
        LevyraStrings.all().forEach { strings ->
            val labels = listOf(strings.alternativeAudioOff, strings.alternativeAudioAutomatic, strings.alternativeAudioPrefer320)
            assertEquals(strings.code, labels.size, labels.toSet().size)
        }
    }

    @Test
    fun englishAndItalianCopyMatchTheProductWording() {
        val bundles = LevyraStrings.all().associateBy { it.code }
        assertEquals("High-quality alternative audio", bundles.getValue("en").alternativeAudioTitle)
        assertEquals(
            "Use a higher-quality external audio stream when Levyra can verify that it is the exact same track.",
            bundles.getValue("en").alternativeAudioSubtitle
        )
        assertEquals("Audio alternativo ad alta qualità", bundles.getValue("it").alternativeAudioTitle)
    }
}
