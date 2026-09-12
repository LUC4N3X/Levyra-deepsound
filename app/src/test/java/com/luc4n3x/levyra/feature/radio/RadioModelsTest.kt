package com.luc4n3x.levyra.feature.radio

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.ui.i18n.LevyraLiveRadioCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RadioModelsTest {
    @Test
    fun everyLevyraLanguageHasAnExactRadioPreference() {
        val codes = LevyraLanguageCatalog.languages.map { it.code }

        assertEquals(codes, codes.map { RadioLanguagePreferences.forLevyraLanguage(it).levyraCode })
        assertEquals("IT", RadioLanguagePreferences.forLevyraLanguage("it").primaryCountry)
        assertEquals("DE", RadioLanguagePreferences.forLevyraLanguage("de").primaryCountry)
        assertEquals("FR", RadioLanguagePreferences.forLevyraLanguage("fr").primaryCountry)
        assertEquals("ES", RadioLanguagePreferences.forLevyraLanguage("es").primaryCountry)
        assertEquals("GB", RadioLanguagePreferences.forLevyraLanguage("en").primaryCountry)
        assertEquals("Arabic", RadioLanguagePreferences.forLevyraLanguage("ar").radioLanguages.single())
    }

    @Test
    fun everyLevyraLanguageHasCompleteLiveRadioCopy() {
        LevyraLanguageCatalog.languages.forEach { language ->
            val strings = LevyraLiveRadioCatalog.forCode(language.code)
            assertTrue(strings.subtitle.isNotBlank())
            assertTrue(strings.searchHint.isNotBlank())
            assertTrue(RadioCategory.entries.map(strings::category).all(String::isNotBlank))
        }
    }

    @Test
    fun streamPolicyAllowsPublicHttpAndRejectsLocalTargets() {
        assertTrue(RadioUrlPolicy.isAllowed("https://stream.example.org/live.mp3"))
        assertTrue(RadioUrlPolicy.isAllowed("http://203.0.113.8:8000/radio"))
        assertFalse(RadioUrlPolicy.isAllowed("file:///data/local/tmp/audio"))
        assertFalse(RadioUrlPolicy.isAllowed("http://localhost:8000/live"))
        assertFalse(RadioUrlPolicy.isAllowed("http://127.0.0.1/live"))
        assertFalse(RadioUrlPolicy.isAllowed("http://192.168.1.20/live"))
        assertFalse(RadioUrlPolicy.isAllowed("https://user:pass@example.org/live"))
    }

    @Test
    fun rankingDropsBrokenUnsafeAndDuplicateStations() {
        val strong = station(uuid = "station-a", votes = 900, clicks = 20_000)
        val duplicate = station(uuid = "station-b", name = "  TEST--RADIO ", votes = 20, clicks = 10)
        val broken = station(uuid = "station-c", name = "Broken", ok = false)
        val private = station(uuid = "station-d", name = "Private", url = "http://10.0.0.4/live")

        val result = filterAndRankRadioStations(listOf(duplicate, broken, private, strong))

        assertEquals(listOf(strong), result)
    }

    @Test
    fun retryUsesAlternateOnceThenStopsAfterBoundedBackoff() {
        val station = station(uuid = "station-a").copy(
            streamUrl = "https://radio.example/original",
            resolvedStreamUrl = "https://radio.example/resolved"
        )

        assertEquals("https://radio.example/original", liveRadioRetryPlan(station, station.preferredStreamUrl, 1)?.streamUrl)
        assertEquals(3_000L, liveRadioRetryPlan(station, station.streamUrl, 2)?.delayMs)
        assertEquals(null, liveRadioRetryPlan(station, station.streamUrl, 4))
    }

    private fun station(
        uuid: String,
        name: String = "Test Radio",
        url: String = "https://stream.example.org/live",
        votes: Int = 100,
        clicks: Int = 1_000,
        ok: Boolean = true
    ) = RadioStation(
        uuid = uuid,
        name = name,
        streamUrl = url,
        resolvedStreamUrl = url,
        faviconUrl = "",
        homepageUrl = "",
        country = "Italy",
        countryCode = "IT",
        language = "Italian",
        tags = listOf("music"),
        codec = "MP3",
        bitrateKbps = 128,
        votes = votes,
        clickCount = clicks,
        lastCheckOk = ok
    )
}
