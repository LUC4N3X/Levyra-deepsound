package com.luc4n3x.levyra.desktop.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopSettingsTest {
    @Test
    fun crossfadeIsSnappedToASupportedStep() {
        assertEquals(0, DesktopSettings.normalizeCrossfade(-500))
        assertEquals(0, DesktopSettings.normalizeCrossfade(500))
        assertEquals(2_000, DesktopSettings.normalizeCrossfade(2_400))
        assertEquals(6_000, DesktopSettings.normalizeCrossfade(5_500))
        assertEquals(12_000, DesktopSettings.normalizeCrossfade(90_000))
        assertEquals(
            4_000,
            DesktopSettings(crossfadeMs = 4_100).sanitized().crossfadeMs
        )
    }

    @Test
    fun audioOutputDeviceIdIsTrimmedAndBounded() {
        val long = "x".repeat(DesktopSettings.MAX_OUTPUT_DEVICE_ID_LENGTH + 40)
        assertEquals(
            "{0.0.0}",
            DesktopSettings(audioOutputDeviceId = "  {0.0.0}  ").sanitized().audioOutputDeviceId
        )
        assertEquals(
            DesktopSettings.MAX_OUTPUT_DEVICE_ID_LENGTH,
            DesktopSettings(audioOutputDeviceId = long).sanitized().audioOutputDeviceId.length
        )
    }

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
    fun playbackSpeedStaysInsideTheEngineRange() {
        assertEquals(DesktopSettings.MAX_SPEED, DesktopSettings.normalizeSpeed(4f), 0.001f)
        assertEquals(DesktopSettings.MIN_SPEED, DesktopSettings.normalizeSpeed(0.1f), 0.001f)
        assertEquals(DesktopSettings.DEFAULT_SPEED, DesktopSettings.normalizeSpeed(Float.NaN), 0.001f)
        assertEquals(1.25f, DesktopSettings.normalizeSpeed(1.25f), 0.001f)
        assertEquals(1.5f, DesktopSettings(playbackSpeed = 1.5f).sanitized().playbackSpeed, 0.001f)
        assertEquals(
            DesktopSettings.MAX_SPEED,
            DesktopSettings(playbackSpeed = 9f).sanitized().playbackSpeed,
            0.001f
        )
    }

    @Test
    fun speedStepsStayInsideTheSupportedRange() {
        assertTrue(
            DesktopSettings.SPEED_STEPS.all { step ->
                step >= DesktopSettings.MIN_SPEED && step <= DesktopSettings.MAX_SPEED
            }
        )
        assertTrue(DesktopSettings.DEFAULT_SPEED in DesktopSettings.SPEED_STEPS)
    }

    @Test
    fun desktopPowerFeaturesAreOnByDefault() {
        val settings = DesktopSettings()

        assertTrue(settings.preloadNextTrack)
        assertTrue(settings.globalMediaKeys)
        assertEquals(DesktopSettings.DEFAULT_SPEED, settings.playbackSpeed, 0.001f)
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
