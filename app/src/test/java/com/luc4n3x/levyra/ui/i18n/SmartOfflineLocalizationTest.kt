package com.luc4n3x.levyra.ui.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartOfflineLocalizationTest {
    @Test
    fun everySupportedLanguageProvidesSmartOfflineLabels() {
        val codes = LevyraStrings.supportedCodes()
        assertEquals(37, codes.size)
        codes.forEach { code ->
            val copy = LevyraStrings.forCode(code).smartOfflineCopy()
            assertTrue("Missing labels for $code", listOf(
                copy.title, copy.subtitle, copy.enabled, copy.enabledSubtitle,
                copy.storageLimit, copy.storageLimitSubtitle, copy.custom,
                copy.customStorage, copy.customStorageSubtitle, copy.megabytes,
                copy.wifiOnly, copy.wifiOnlySubtitle, copy.chargingOnly,
                copy.chargingOnlySubtitle, copy.preferFavorites, copy.preferFavoritesSubtitle,
                copy.excludedArtists, copy.excludedArtistsSubtitle,
                copy.excludedPlaylists, copy.excludedPlaylistsSubtitle,
                copy.commaSeparatedHint, copy.save, copy.refresh,
                copy.refreshSubtitle, copy.updatedToday, copy.neverUpdated,
                copy.protectedManual
            ).all(String::isNotBlank))
        }
    }

    @Test
    fun translatedDescriptionsDifferFromEnglish() {
        val english = LevyraStrings.forCode("en").smartOfflineCopy().subtitle
        listOf("it", "es", "fr", "de", "ru", "ar", "ja", "zh-Hant", "fa").forEach { code ->
            assertFalse("Missing translation for $code", LevyraStrings.forCode(code).smartOfflineCopy().subtitle == english)
        }
    }
}
