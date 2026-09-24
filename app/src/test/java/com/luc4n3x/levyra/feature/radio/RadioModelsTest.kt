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
    fun searchMatchesPunctuationAndKeepsStaleButPlayableStations() {
        val target = station(
            uuid = "181-fm-salsa",
            name = "181.FM Salsa",
            url = "http://relay.181.fm:8098/",
            ok = false
        )
        val unrelated = station(
            uuid = "181-classic",
            name = "181.FM Classic Hits",
            url = "https://radio.example/classic"
        )

        val result = filterAndRankRadioSearchResults(listOf(unrelated, target), "181 salsa")

        assertEquals(listOf(target), result)
        assertTrue(radioStationMatchesSearch(target, "181 salsa"))
        assertTrue(radioStationMatchesSearch(target, "181.fm salsa"))
        assertFalse(radioStationMatchesSearch(unrelated, "181 salsa"))
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

    @Test
    fun playlistUrlIsNeverUsedAsAlternateStream() {
        val station = station(uuid = "181-salsa").copy(
            streamUrl = "http://www.181.fm/stream/pls/181-salsa.pls",
            resolvedStreamUrl = "http://listen.181fm.com/181-salsa_128k.mp3"
        )

        assertEquals("http://listen.181fm.com/181-salsa_128k.mp3", station.preferredStreamUrl)
        assertEquals("", station.alternateStreamUrl)
        assertEquals(station.preferredStreamUrl, liveRadioRetryPlan(station, station.preferredStreamUrl, 1)?.streamUrl)
        assertTrue(isRadioPlaylistUrl("https://radio.example/live.M3U?x=1"))
        assertFalse(isRadioPlaylistUrl("https://radio.example/live.m3u8"))
    }

    @Test
    fun searchRanksExactNameAboveMorePopularPartialMatches() {
        val exact = station(
            uuid = "181-salsa",
            name = "181.FM - Salsa",
            url = "http://listen.181fm.com/181-salsa_128k.mp3",
            votes = 800
        )
        val popular = station(
            uuid = "salsa-181",
            name = "Salsa Hits",
            url = "https://radio.example/salsa",
            votes = 90_000
        ).copy(tags = listOf("181", "fm"))

        val result = filterAndRankRadioSearchResults(listOf(popular, exact), "181.fm salsa")

        assertEquals(listOf(exact, popular), result)
        assertEquals(listOf("salsa", "181"), radioSearchRequestTokens("181.fm salsa"))
    }

    @Test
    fun searchIgnoresDiacriticsInNamesAndQueries() {
        val precomposed = station(uuid = "a", name = "Salsa Clásica Éxitos", url = "https://radio.example/a")
        val decomposed = station(uuid = "b", name = "Radio Clásica", url = "https://radio.example/b")

        assertTrue(radioStationMatchesSearch(precomposed, "salsa clasica exitos"))
        assertTrue(radioStationMatchesSearch(decomposed, "clásica"))
        assertEquals("181 fm awesome 80s", normalizeRadioName("181.FM - Awesome 80’s"))
        assertEquals("rocknroll", normalizeRadioName("Rock'n'Roll"))
        assertEquals("", station(uuid = "c").copy(codec = "UNKNOWN", bitrateKbps = 0).qualityLabel)
    }

    @Test
    fun playerTrackNeverCarriesUnverifiedRemoteArtwork() {
        val station = station(uuid = "a").copy(faviconUrl = "https://station.example/logo.png")

        assertEquals("", station.toTrack().thumbnailUrl)
        assertEquals("", station.toTrack().largeThumbnailUrl)
        assertEquals("file:///cache/a.img", station.toTrack(artwork = "file:///cache/a.img").thumbnailUrl)
    }

    @Test
    fun searchDropsSameStreamPublishedUnderDifferentNames() {
        val first = station(uuid = "a", name = "181.FM Salsa", url = "http://relay.181.fm:8098/", votes = 900)
        val second = station(uuid = "b", name = "181.FM - Salsa", url = "https://relay.181.fm:8098", votes = 10)

        assertEquals(listOf(first), filterAndRankRadioSearchResults(listOf(second, first), "181 salsa"))
    }

    @Test
    fun icyAdvertisementNeedsExplicitBroadcasterMarker() {
        val adswizz = liveRadioStreamMetadata("Shopify", "StreamTitle='Shopify';adw_ad='true';durationMilliseconds='30000';")
        val preroll = liveRadioStreamMetadata("", "StreamTitle='';insertionType='preroll';")
        val song = liveRadioStreamMetadata("Advert - Commercial Break", "StreamTitle='Advert - Commercial Break';StreamUrl='';")

        assertTrue(adswizz.advertisement)
        assertEquals("", adswizz.title)
        assertTrue(preroll.advertisement)
        assertFalse(song.advertisement)
        assertEquals("Advert - Commercial Break", song.title)
    }

    @Test
    fun icyTitleDropsUrlUuidAndHashSegmentsOnly() {
        assertEquals("Artist - Song", sanitizeLiveRadioTitle("Artist - Song - https://station.example/now"))
        assertEquals("Artist - Song", sanitizeLiveRadioTitle("Artist - Song || 0123456789abcdef0123"))
        assertEquals("AC/DC - T.N.T.", sanitizeLiveRadioTitle("  AC/DC - T.N.T.  "))
        assertEquals(
            "AITCH - RMB (RING MY BELL)",
            sanitizeLiveRadioTitle("AITCH~RMB (RING MY BELL)~~0~~131~2026-09-24T20:42:27~2026-09-24T20:42:27~Radio 105")
        )
        assertEquals("Up~Down Mix", sanitizeLiveRadioTitle("Up~Down Mix"))
        assertEquals("AC~DC~Live", sanitizeLiveRadioTitle("AC~DC~Live"))
        assertEquals("Sarah Brightman - Time To Say Goodbye", sanitizeLiveRadioTitle("Sarah Brightman - Time To Say Goodbye"))
    }

    @Test
    fun blankIcyBlockKeepsSongButEndsAdvertisement() {
        val song = LiveRadioStreamMetadata("Artist - Song")
        val ad = LiveRadioStreamMetadata(advertisement = true)
        val blank = LiveRadioStreamMetadata()

        assertEquals(song, nextLiveRadioStreamMetadata(song, blank))
        assertEquals(blank, nextLiveRadioStreamMetadata(ad, blank))
        assertEquals(ad, nextLiveRadioStreamMetadata(song, ad))
    }

    @Test
    fun nowPlayingHidesStationIdentAndLocalizesAdvertisement() {
        assertEquals("", liveRadioNowPlaying(LiveRadioStreamMetadata("181.FM Salsa"), "181.FM - Salsa", "Ad"))
        assertEquals("Ad", liveRadioNowPlaying(LiveRadioStreamMetadata(advertisement = true), "181.FM - Salsa", "Ad"))
        assertEquals("Artist - Song", liveRadioNowPlaying(LiveRadioStreamMetadata("Artist - Song"), "181.FM - Salsa", "Ad"))
        assertEquals("Pubblicità", LevyraLiveRadioCatalog.advertisement("it"))
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

        currentTime += 4 * 60 * 1000L
        pool.servers()
        assertEquals(1, lookupCount)

        currentTime += 2 * 60 * 1000L
        pool.servers()
        assertEquals(2, lookupCount)
    }
}
