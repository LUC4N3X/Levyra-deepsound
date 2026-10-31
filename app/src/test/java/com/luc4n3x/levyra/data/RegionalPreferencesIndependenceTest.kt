package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.ChartsCatalog
import com.luc4n3x.levyra.domain.LevyraContentLocales
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.LevyraMusicRegion
import com.luc4n3x.levyra.ui.i18n.LevyraRegionalStrings
import com.luc4n3x.levyra.ui.regional.preferredAudioLanguageOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionalPreferencesIndependenceTest {
    @Test
    fun blankAndAutomaticAliasesNormalizeToAutomatic() {
        listOf(null, "", "   ", "auto", "AUTO", "original", "Default", "-", "_").forEach { value ->
            assertEquals("", AudioLanguageIntelligence.normalizeLanguage(value))
        }
    }

    @Test
    fun languageCodesNormalizeToLowercaseCanonicalTags() {
        assertEquals("it", AudioLanguageIntelligence.normalizeLanguage(" IT "))
        assertEquals("pt-br", AudioLanguageIntelligence.normalizeLanguage("pt_BR"))
        assertEquals("zh-hant", AudioLanguageIntelligence.normalizeLanguage("zh-Hant"))
        assertEquals("he", AudioLanguageIntelligence.normalizeLanguage("iw"))
        assertEquals("id", AudioLanguageIntelligence.normalizeLanguage("in"))
        assertEquals("fil", AudioLanguageIntelligence.normalizeLanguage("tl"))
    }

    @Test
    fun everySelectableAudioLanguageRoundTripsThroughStoredValue() {
        LevyraLanguageCatalog.selectableLanguages.forEach { language ->
            val stored = AudioLanguageIntelligence.normalizeLanguage(language.code)

            assertEquals(stored, AudioLanguageIntelligence.normalizeLanguage(stored))
            assertEquals(language, preferredAudioLanguageOption(stored))
        }
    }

    @Test
    fun audioLanguageOptionKeepsAutomaticAndUnknownValuesDistinct() {
        assertNull(preferredAudioLanguageOption(""))
        assertNull(preferredAudioLanguageOption("xx"))
        assertEquals("en", preferredAudioLanguageOption("en-US")?.code)
    }

    @Test
    fun explicitRegionKeepsCountryWhileInterfaceLanguageChanges() {
        val russian = LevyraContentLocales.catalogLocale("ru")
        val inItaly = LevyraContentLocales.withMusicRegion(russian, "it")

        assertEquals(russian.hl, inItaly.hl)
        assertEquals("IT", inItaly.gl)
        assertEquals("it", inItaly.chartRegionId)
        assertEquals("it", ChartsCatalog.regionAfterLanguageChange("it", "it", "en", "ru").id)
    }

    @Test
    fun automaticRegionFollowsLanguage() {
        val russian = LevyraContentLocales.catalogLocale("ru")

        assertEquals(russian, LevyraContentLocales.withMusicRegion(russian, LevyraMusicRegion.AUTO))
        assertEquals(
            ChartsCatalog.defaultRegionForLanguage("ru").id,
            ChartsCatalog.regionAfterLanguageChange(
                storedRegionId = LevyraMusicRegion.AUTO,
                currentRegionId = ChartsCatalog.defaultRegionForLanguage("en").id,
                previousLanguageCode = "en",
                languageCode = "ru"
            ).id
        )
    }

    @Test
    fun regionalCopyIsTranslatedForEverySelectableLanguage() {
        val english = LevyraRegionalStrings.forCode("en")

        assertEquals(
            LevyraLanguageCatalog.selectableLanguages.map { it.code }.toSet(),
            LevyraRegionalStrings.localizedCodes
        )
        LevyraLanguageCatalog.selectableLanguages
            .filter { it.code != "en" }
            .forEach { language ->
                val copy = LevyraRegionalStrings.forCode(language.code)

                assertNotEquals(language.code, english.audioLanguageTitle, copy.audioLanguageTitle)
                assertNotEquals(language.code, english.fallbackNote, copy.fallbackNote)
                assertTrue(language.code, copy.automaticBody.isNotBlank())
            }
    }
}
