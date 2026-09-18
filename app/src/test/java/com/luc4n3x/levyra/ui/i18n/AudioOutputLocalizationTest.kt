package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioOutputLocalizationTest {
    @Test
    fun everySupportedLanguageShipsItsOwnAaudioOutputCopy() {
        val catalogCodes = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(catalogCodes, audioOutputLocalizationCodes())
        val english = LevyraStrings.forCode("en")
        LevyraStrings.all().forEach { strings ->
            assertTrue(strings.code, strings.audioOutputAaudio.contains("AAudio"))
            assertTrue(strings.code, strings.audioOutputAaudioSubtitle.isNotBlank())
            if (strings.code != "en") {
                assertNotEquals(strings.code, english.audioOutputAaudioSubtitle, strings.audioOutputAaudioSubtitle)
            }
        }
    }

    @Test
    fun subtitleNamesTheOutputPathWithoutLatencyOrBatteryClaims() {
        val latencyOrBatteryTerms = listOf("latency", "latenza", "battery", "batteria", "低延迟", "低遅延", "저지연")
        LevyraStrings.all().forEach { strings ->
            val subtitle = strings.audioOutputAaudioSubtitle
            assertTrue(strings.code, subtitle.contains("AAudio") && subtitle.contains("Oboe"))
            assertTrue(strings.code, latencyOrBatteryTerms.none { subtitle.contains(it, ignoreCase = true) })
        }
    }

    @Test
    fun italianCopyIsTranslated() {
        assertEquals("Uscita AAudio (Oboe)", LevyraStrings.forCode("it").audioOutputAaudio)
        assertEquals("Uscita audio nativa tramite AAudio e Oboe", LevyraStrings.forCode("it").audioOutputAaudioSubtitle)
    }
}
