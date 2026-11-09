package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioTabsLocalizationTest {
    @Test
    fun everySupportedLanguageShipsAudioTabLabels() {
        val supported = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(supported, audioTabsLocalizationCodes())
        (supported + LevyraLanguageCatalog.BRAZILIAN_PORTUGUESE).forEach { code ->
            val copy = audioTabsCopy(code)
            assertTrue(code, listOf(copy.sound, copy.playback, copy.smart).all(String::isNotBlank))
        }
    }

    @Test
    fun labelsAreActuallyTranslated() {
        val english = audioTabsCopy("en")
        listOf("it", "ru", "ja", "ar", "zh-Hant").forEach { code ->
            val copy = audioTabsCopy(code)
            assertTrue(code, copy.sound != english.sound && copy.playback != english.playback)
        }
        assertEquals("Suono", audioTabsCopy("it").sound)
        assertEquals("Reprodução", audioTabsCopy("pt-BR").playback)
    }
}
