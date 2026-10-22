package com.luc4n3x.levyra.feature.radio

import com.luc4n3x.levyra.domain.HomeSectionId
import com.luc4n3x.levyra.domain.HomeStyle
import com.luc4n3x.levyra.domain.LevyraHomeStudioSettings
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.ui.i18n.radioLibraryCopy
import org.junit.Assert.*
import org.junit.Test

class CustomRadioStationTest {
    @Test
    fun publicStreamsAndPlaylistsHaveStableLocalIdentities() {
        listOf("http://relay.example:8000/live", "https://radio.example/live.m3u", "https://radio.example/live.m3u8", "https://radio.example/live.pls").forEach { url ->
            val station = customRadioStation("  Моё радио  ", url, "")
            assertTrue(station.isCustom)
            assertEquals("Моё радио", station.name)
            assertEquals(station.streamUrl, station.preferredStreamUrl)
            assertEquals(station.uuid, customRadioStation("Renamed", url, "", station).uuid)
            assertTrue(station.toTrack().isLiveRadio())
        }
    }

    @Test
    fun unsafeUrlsAndInvalidFieldsAreRejected() {
        listOf("file:///etc/passwd", "ftp://radio.example/live", "https://u:p@radio.example/live", "http://127.0.0.1/live",
            "http://10.0.0.2/live", "http://100.64.0.1/live", "http://[::1]/live", "http://[fd00::1]/live",
            "http://[fe80::1]/live", "http://224.0.0.1/live", "http://radio.local/live", "http://radio.example:65536/live",
            "https://radio.example/\nstream", "http://2130706433/live", "https://radio.example/" + "a".repeat(4096)).forEach { url ->
            assertNull(url, canonicalRadioUrl(url))
        }
        assertError(CustomRadioError.Name) { customRadioStation(" ", "https://radio.example/live", "") }
        assertError(CustomRadioError.StreamUrl) { customRadioStation("Radio", "invalid", "") }
        assertError(CustomRadioError.ImageUrl) { customRadioStation("Radio", "https://radio.example/live", "file:///image.png") }
        assertTrue(RadioUrlPolicy.isAllowed("https://fcradio.example/live"))
    }

    @Test
    fun favoriteRadioVisibilityPersistsForEveryHomeStyleAndUpgradesOldLayouts() {
        HomeStyle.entries.forEach { style ->
            val defaults = LevyraHomeStudioSettings(style = style)
            assertTrue(defaults.activeLayout().order.contains(HomeSectionId.FavoriteRadio))
            val hidden = defaults.updateLayout(style) { it.withVisibility(HomeSectionId.FavoriteRadio, false) }
            assertTrue(LevyraHomeStudioSettings.decode(hidden.encode()).activeLayout().isHidden(HomeSectionId.FavoriteRadio))
            val oldJson = defaults.encode().replace("\"favorite_radio\",", "").replace(",\"favorite_radio\"", "")
            assertTrue(LevyraHomeStudioSettings.decode(oldJson).activeLayout().order.contains(HomeSectionId.FavoriteRadio))
        }
    }

    @Test
    fun everySupportedLanguageHasRadioManagementCopy() {
        LevyraLanguageCatalog.languages.forEach { language ->
            val copy = radioLibraryCopy(language.code)
            assertTrue(copy.favorites.isNotBlank())
            assertTrue(copy.add.isNotBlank())
            assertTrue(copy.noResultsHelp.isNotBlank())
            CustomRadioError.entries.forEach { assertTrue(copy.error(it).isNotBlank()) }
        }
    }

    private fun assertError(reason: CustomRadioError, action: () -> Unit) {
        try { action(); fail("Expected $reason") } catch (error: CustomRadioException) { assertEquals(reason, error.reason) }
    }
}
