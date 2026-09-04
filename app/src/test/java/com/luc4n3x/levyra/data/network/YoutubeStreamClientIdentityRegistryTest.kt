package com.luc4n3x.levyra.data.network

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class YoutubeStreamClientIdentityRegistryTest {
    private val visionOs = YoutubeStreamClientIdentity(
        clientName = "VISIONOS",
        clientHeaderName = "101",
        clientVersion = "1.04",
        userAgent = "com.google.visionos.youtube/1.04(RealityDevice17,1; U; CPU visionOS 26_6_0 like Mac OS X; US)",
        requiresPoToken = false,
        videoId = "dQw4w9WgXcQ"
    )

    private val web = YoutubeStreamClientIdentity(
        clientName = "WEB",
        clientHeaderName = "1",
        clientVersion = "2.20260805.01.00",
        userAgent = YoutubeClientIdentityInterceptor.PO_TOKEN_WEB_USER_AGENT,
        requiresPoToken = true,
        videoId = "dQw4w9WgXcQ"
    )

    @Before
    fun setUp() = YoutubeStreamClientIdentityRegistry.clear()

    @After
    fun tearDown() = YoutubeStreamClientIdentityRegistry.clear()

    @Test
    fun resolvedClientIdentitySurvivesRequestNumberAndMediaNetworkFailover() {
        val resolved = "https://rr3---sn-abc.googlevideo.com/videoplayback?id=o-media&itag=140&mime=audio%2Fmp4"
        YoutubeStreamClientIdentityRegistry.register(listOf(resolved), visionOs)

        val withRequestNumber = "$resolved&rn=7"
        val onAlternateNetwork =
            "https://rr9---sn-zyx.googlevideo.com/videoplayback?id=o-media&itag=140&mime=audio%2Fmp4&rn=7"

        assertEquals(visionOs, YoutubeStreamClientIdentityRegistry.find(resolved))
        assertEquals(visionOs, YoutubeStreamClientIdentityRegistry.find(withRequestNumber))
        assertEquals(visionOs, YoutubeStreamClientIdentityRegistry.find(onAlternateNetwork))
    }

    @Test
    fun distinctFormatsOfTheSameVideoKeepTheirOwnIdentity() {
        val audio = "https://rr3---sn-abc.googlevideo.com/videoplayback?id=o-media&itag=140"
        val video = "https://rr3---sn-abc.googlevideo.com/videoplayback?id=o-media&itag=137"
        YoutubeStreamClientIdentityRegistry.register(listOf(audio), visionOs)
        YoutubeStreamClientIdentityRegistry.register(listOf(video), web)

        assertEquals(visionOs, YoutubeStreamClientIdentityRegistry.find(audio))
        assertEquals(web, YoutubeStreamClientIdentityRegistry.find(video))
    }

    @Test
    fun unknownUrlReturnsNoIdentitySoTheExistingFallbackStillApplies() {
        assertNull(
            YoutubeStreamClientIdentityRegistry.find(
                "https://rr3---sn-abc.googlevideo.com/videoplayback?id=other&itag=251"
            )
        )
        assertNull(YoutubeStreamClientIdentityRegistry.find(""))
    }

    @Test
    fun urlWithoutMediaParametersIsStillMatchedExactly() {
        val manifest = "https://manifest.googlevideo.com/api/manifest/dash/expire/1"
        YoutubeStreamClientIdentityRegistry.register(listOf(manifest), visionOs)

        assertEquals(visionOs, YoutubeStreamClientIdentityRegistry.find(manifest))
        assertNull(YoutubeStreamClientIdentityRegistry.find("$manifest/other"))
    }

    @Test
    fun nativeClientMediaHeadersCarryItsOwnUserAgentAndNoBrowserNavigation() {
        val headers = visionOs.mediaRequestHeaders()

        assertEquals(visionOs.userAgent, headers["User-Agent"])
        assertNull(headers["Origin"])
        assertNull(headers["Referer"])
    }

    @Test
    fun webClientMediaHeadersCarryOriginAndReferer() {
        val headers = web.mediaRequestHeaders()

        assertEquals(YoutubeClientIdentityInterceptor.PO_TOKEN_WEB_USER_AGENT, headers["User-Agent"])
        assertEquals("https://www.youtube.com", headers["Origin"])
        assertEquals("https://www.youtube.com/", headers["Referer"])
        assertEquals("cross-site", headers["Sec-Fetch-Site"])
    }

    @Test
    fun musicWebClientMediaHeadersUseTheMusicOrigin() {
        val remix = web.copy(clientName = "WEB_REMIX", clientHeaderName = "67")

        val headers = remix.mediaRequestHeaders()

        assertEquals("https://music.youtube.com", headers["Origin"])
        assertEquals("https://music.youtube.com/", headers["Referer"])
    }

    @Test
    fun embeddedWebClientRefererCarriesTheResolvedVideoId() {
        val embedded = web.copy(clientName = "WEB_EMBEDDED_PLAYER", clientHeaderName = "56")

        assertEquals(
            "https://www.youtube.com/embed/dQw4w9WgXcQ",
            embedded.mediaRequestHeaders()["Referer"]
        )
        assertEquals(
            "https://www.youtube.com/embed/",
            embedded.copy(videoId = "").mediaRequestHeaders()["Referer"]
        )
    }

    @Test
    fun registryEvictsOldestEntriesInsteadOfGrowingWithPlayback() {
        repeat(400) { index ->
            YoutubeStreamClientIdentityRegistry.register(
                listOf("https://rr1---sn-abc.googlevideo.com/videoplayback?id=media$index&itag=140"),
                visionOs
            )
        }

        assertNull(
            YoutubeStreamClientIdentityRegistry.find(
                "https://rr1---sn-abc.googlevideo.com/videoplayback?id=media0&itag=140"
            )
        )
        assertEquals(
            visionOs,
            YoutubeStreamClientIdentityRegistry.find(
                "https://rr1---sn-abc.googlevideo.com/videoplayback?id=media399&itag=140"
            )
        )
    }

    @Test
    fun exactUrlIdentityWinsOverTheMediaFallbackOfAnotherStream() {
        val visionOsUrl =
            "https://rr3---sn-abc.googlevideo.com/videoplayback?id=o-media&itag=140&pot=vision"
        val webUrl =
            "https://rr7---sn-xyz.googlevideo.com/videoplayback?id=o-media&itag=140&pot=web"
        YoutubeStreamClientIdentityRegistry.register(listOf(visionOsUrl), visionOs)
        YoutubeStreamClientIdentityRegistry.register(listOf(webUrl), web)

        assertEquals(visionOs, YoutubeStreamClientIdentityRegistry.find(visionOsUrl))
        assertEquals(web, YoutubeStreamClientIdentityRegistry.find(webUrl))
    }

    @Test
    fun mediaFallbackStillAppliesToAnUrlThatWasNeverRegistered() {
        val registered = "https://rr3---sn-abc.googlevideo.com/videoplayback?id=o-media&itag=251"
        YoutubeStreamClientIdentityRegistry.register(listOf(registered), visionOs)

        assertEquals(
            visionOs,
            YoutubeStreamClientIdentityRegistry.find("$registered&rn=4")
        )
    }
}
