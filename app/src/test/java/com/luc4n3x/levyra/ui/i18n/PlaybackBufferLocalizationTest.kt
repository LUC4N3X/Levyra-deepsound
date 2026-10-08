package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackBufferLocalizationTest {
    @Test
    fun everySupportedLanguageShipsCompleteBufferCopy() {
        val supported = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(supported, playbackBufferLocalizationCodes())
        val english = playbackBufferCopy("en")
        supported.forEach { code ->
            val copy = playbackBufferCopy(code)
            val values = listOf(
                copy.title,
                copy.description,
                copy.automatic,
                copy.custom,
                copy.automaticDetail,
                copy.customDetail,
                copy.minimum,
                copy.maximum,
                copy.start,
                copy.afterInterruption,
                copy.presets,
                copy.reduced,
                copy.balanced,
                copy.high,
                copy.restoreAutomatic
            )
            assertTrue(code, values.all(String::isNotBlank))
            if (code != "en") assertNotEquals(code, english.description, copy.description)
        }
    }

    @Test
    fun italianCopyUsesRequestedTerminology() {
        val copy = playbackBufferCopy("it")
        assertEquals("Buffer di riproduzione", copy.title)
        assertEquals("Automatico", copy.automatic)
        assertEquals("Personalizzato", copy.custom)
        assertEquals("Buffer dopo un’interruzione", copy.afterInterruption)
    }
}
