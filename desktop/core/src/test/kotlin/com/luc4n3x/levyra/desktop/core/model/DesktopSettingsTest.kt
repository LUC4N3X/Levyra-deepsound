package com.luc4n3x.levyra.desktop.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopSettingsTest {
    @Test
    fun languageCatalogMatchesAndroidSupportedLanguages() {
        assertEquals(26, AppLanguage.entries.size)
        assertEquals(
            setOf(
                "en",
                "it",
                "es",
                "fr",
                "de",
                "pt",
                "nl",
                "pl",
                "ro",
                "el",
                "sv",
                "da",
                "cs",
                "uk",
                "ru",
                "tr",
                "ar",
                "zh",
                "ja",
                "ko",
                "hi",
                "id",
                "vi",
                "th",
                "fil",
                "he"
            ),
            AppLanguage.entries.map { it.tag }.toSet()
        )
    }

    @Test
    fun legacyLanguageAliasesAreNormalized() {
        assertEquals(AppLanguage.INDONESIAN, AppLanguage.fromTag("in-ID"))
        assertEquals(AppLanguage.FILIPINO, AppLanguage.fromTag("tl_PH"))
        assertEquals(AppLanguage.HEBREW, AppLanguage.fromTag("iw-IL"))
    }

    @Test
    fun rtlLanguagesAreExplicit() {
        assertTrue(AppLanguage.ARABIC.isRtl)
        assertTrue(AppLanguage.HEBREW.isRtl)
        assertFalse(AppLanguage.ITALIAN.isRtl)
    }

    @Test
    fun onboardingDataIsSanitizedWithoutLosingValidChoices() {
        val settings = DesktopSettings(
            language = AppLanguage.ITALIAN,
            contentCountry = " italy ",
            displayName = "  Luca  ",
            selectedTasteIds = setOf("HITS", "rap", "invalid"),
            onboardingCompleted = true,
            volume = 150
        ).sanitized()

        assertEquals("IT", settings.contentCountry)
        assertEquals("Luca", settings.displayName)
        assertEquals(setOf("hits", "rap"), settings.selectedTasteIds)
        assertTrue(settings.onboardingCompleted)
        assertEquals(100, settings.volume)
    }

    @Test
    fun blankCountryFallsBackToLanguageCountry() {
        val settings = DesktopSettings(
            language = AppLanguage.JAPANESE,
            contentCountry = ""
        ).sanitized()

        assertEquals("JP", settings.contentCountry)
    }
}
