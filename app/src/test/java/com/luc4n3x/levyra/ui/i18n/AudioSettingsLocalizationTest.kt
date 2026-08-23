package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class AudioSettingsLocalizationTest {
    @Test
    fun everySupportedLanguageShipsTheAudioScreenCopy() {
        val catalogCodes = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(catalogCodes, audioLocalizationCodes())
        LevyraStrings.all().forEach { strings ->
            assertTrue(strings.code, strings.audioSectionQuality.isNotBlank())
            assertTrue(strings.code, strings.audioSectionEqualizer.isNotBlank())
            assertTrue(strings.code, strings.audioSectionSpatial.isNotBlank())
            assertTrue(strings.code, strings.audioSectionDynamics.isNotBlank())
            assertTrue(strings.code, strings.audioSectionPlayback.isNotBlank())
            assertTrue(strings.code, strings.audioResetEqualizer.isNotBlank())
            assertTrue(strings.code, strings.audioPresetCustom.isNotBlank())
            assertTrue(strings.code, strings.audioBands.isNotBlank())
        }
    }

    @Test
    fun audioSectionTitleIsNotMarketingCopy() {
        val marketing = listOf("premium", "プレミアム", "프리미엄", "प्रीमियम", "cao cấp", "พรีเมียม")
        LevyraStrings.all().forEach { strings ->
            val title = strings.audioEngine.lowercase(Locale.ROOT)
            assertTrue(strings.code, title.isNotBlank())
            assertFalse(strings.code, marketing.any(title::contains))
        }
    }

    @Test
    fun localizedSectionTitlesAreActuallyTranslated() {
        val equalizerTitles = LevyraStrings.all().associate { it.code to it.audioSectionEqualizer }
        assertEquals("Equalizzatore", equalizerTitles.getValue("it"))
        assertEquals("Égaliseur", equalizerTitles.getValue("fr"))
        assertEquals("Эквалайзер", equalizerTitles.getValue("ru"))
        assertEquals("イコライザー", equalizerTitles.getValue("ja"))
        assertTrue(equalizerTitles.values.toSet().size >= 15)
    }

    @Test
    fun audioSectionLabelsStayDistinctInsideOneBundle() {
        LevyraStrings.all().forEach { strings ->
            val sections = listOf(
                strings.audioSectionQuality,
                strings.audioSectionEqualizer,
                strings.audioSectionSpatial,
                strings.audioSectionDynamics,
                strings.audioSectionPlayback
            )
            assertEquals(strings.code, sections.size, sections.toSet().size)
        }
    }
}
