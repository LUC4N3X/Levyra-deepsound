package com.luc4n3x.levyra.feature.radio

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.ui.i18n.LevyraLiveRadioCatalog
import java.net.InetAddress
import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
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
            assertTrue(strings.exploreSubtitle.isNotBlank())
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
    fun publicDnsAllowsPublicAddressAndRejectsPrivateOrLoopback() {
        val publicIp = InetAddress.getByName("93.184.216.34")
        val loopback = InetAddress.getByName("127.0.0.1")
        val privateIp = InetAddress.getByName("192.168.1.1")
        val ipv6Loopback = InetAddress.getByName("::1")

        val allowedDns = RadioUrlPolicy.publicDns { listOf(publicIp) }
        assertEquals(listOf(publicIp), allowedDns.lookup("stream.example.org"))

        val loopbackDns = RadioUrlPolicy.publicDns { listOf(loopback) }
        try {
            loopbackDns.lookup("localhost")
            fail("Expected UnknownHostException for loopback")
        } catch (_: UnknownHostException) {
        }

        val privateDns = RadioUrlPolicy.publicDns { listOf(privateIp) }
        try {
            privateDns.lookup("internal.lan")
            fail("Expected UnknownHostException for private IP")
        } catch (_: UnknownHostException) {
        }

        val ipv6LoopbackDns = RadioUrlPolicy.publicDns { listOf(ipv6Loopback) }
        try {
            ipv6LoopbackDns.lookup("local6")
            fail("Expected UnknownHostException for IPv6 loopback")
        } catch (_: UnknownHostException) {
        }

        val emptyDns = RadioUrlPolicy.publicDns { emptyList() }
        try {
            emptyDns.lookup("empty.example.org")
            fail("Expected UnknownHostException for empty address list")
        } catch (_: UnknownHostException) {
        }
    }

    @Test
    fun safeFaviconUrlRejectsUnsafeTargetsAndAcceptsPublicHttp() {
        val base = station(uuid = "test-station")
        assertEquals("", base.copy(faviconUrl = "http://127.0.0.1/icon.png").safeFaviconUrl)
        assertEquals("", base.copy(faviconUrl = "http://192.168.1.1/icon.png").safeFaviconUrl)
        assertEquals("", base.copy(faviconUrl = "http://10.0.0.1/icon.png").safeFaviconUrl)
        assertEquals("", base.copy(faviconUrl = "http://localhost/icon.png").safeFaviconUrl)
        assertEquals("", base.copy(faviconUrl = "http://[::1]/icon.png").safeFaviconUrl)
        assertEquals("", base.copy(faviconUrl = "file:///data/local/tmp/icon.png").safeFaviconUrl)
        assertEquals("https://radio.example/icon.png", base.copy(faviconUrl = "https://radio.example/icon.png").safeFaviconUrl)
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

    @Test
    fun internetRequiredStringIsProperlyLocalized() {
        val nonEnglishCodes = listOf("uk", "ru", "tr", "ja", "ko", "hi", "th", "he")
        nonEnglishCodes.forEach { code ->
            val message = LevyraLiveRadioCatalog.forCode(code).internetRequired
            assertFalse("Language $code contains unlocalized 'Internet': $message", message.contains("Internet"))
        }
    }

    @Test
    fun serverPoolFallbackExpiresFasterThanDiscoveredServers() = kotlinx.coroutines.runBlocking {
        var currentTime = 1_000L
        var lookupCount = 0
        val pool = RadioBrowserServerPool(
            lookup = {
                lookupCount++
                emptyArray()
            },
            now = { currentTime }
        )

        pool.servers()
        assertEquals(1, lookupCount)

        // Before fallback TTL (5 min), should use cache
        currentTime += 4 * 60 * 1000L
        pool.servers()
        assertEquals(1, lookupCount)

        // After fallback TTL (5 min), should query again
        currentTime += 2 * 60 * 1000L
        pool.servers()
        assertEquals(2, lookupCount)
    }
}
