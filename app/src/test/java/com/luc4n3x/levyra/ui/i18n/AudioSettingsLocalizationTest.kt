package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class AudioSettingsLocalizationTest {
    @Test
    fun everySupportedLanguageShipsPersonalizedSearchCopy() {
        val catalogCodes = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(catalogCodes, personalizedSearchLocalizationCodes())
        LevyraStrings.all().forEach { strings ->
            val copy = personalizedSearchCopy(strings.code)
            val values = listOf(
                copy.basedOnListening,
                copy.artistsForYou,
                copy.searchArtist,
                copy.findMoreLike,
                copy.backToAlbum
            )
            assertTrue(strings.code, values.all(String::isNotBlank))
            assertTrue(strings.code, values.drop(2).all { "%s" in it })
        }
    }

    @Test
    fun everySupportedLanguageShipsSpeedDialCopy() {
        val catalogCodes = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(catalogCodes, speedDialLocalizationCodes())
        LevyraStrings.all().forEach { strings ->
            val copy = strings.speedDialCopy()
            val values = listOf(
                copy.title, copy.addToHome, copy.removeFromHome, copy.homeFull, copy.moveEarlier, copy.moveLater,
                copy.song, copy.album, copy.artist, copy.playlist, copy.reorderHint
            )
            assertTrue(strings.code, values.all(String::isNotBlank))
            if (strings.code != "en") assertFalse(strings.code, copy.addToHome == speedDialCopyFor("en").addToHome)
        }
    }

    @Test
    fun everySupportedLanguageShipsParametricProfileCopy() {
        val catalogCodes = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(catalogCodes, parametricProfileLocalizationCodes())
        LevyraStrings.all().forEach { strings ->
            val copy = strings.parametricProfileCopy()
            assertTrue(strings.code, copy.band(3).contains("3"))
            assertTrue(strings.code, copy.range("1", "9").contains("1") && copy.range("1", "9").contains("9"))
            assertTrue(strings.code, copy.copyName("X", 2).contains("X") && copy.copyName("X", 2).contains("2"))
            assertTrue(strings.code, copy.deleteTitle("Y").contains("Y"))
        }
    }

    private fun speedDialCopyFor(code: String): SpeedDialCopy =
        LevyraStrings.all().first { it.code == code }.speedDialCopy()

    @Test
    fun everySupportedLanguageShipsParametricEqualizerCopy() {
        val catalogCodes = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(catalogCodes, parametricEqLocalizationCodes())
        LevyraStrings.all().forEach { strings ->
            val copy = strings.parametricEqCopy()
            val values = listOf(
                copy.graphicEq,
                copy.parametricEq,
                copy.parametricSubtitle,
                copy.activeProfile,
                copy.bands,
                copy.addBand,
                copy.removeBand,
                copy.frequency,
                copy.gain,
                copy.qFactor,
                copy.saveProfile,
                copy.reset,
                copy.peak,
                copy.lowShelf,
                copy.highShelf,
                copy.importHint,
                copy.invalidDetail
            )
            assertTrue(strings.code, values.all(String::isNotBlank))
        }
    }

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
