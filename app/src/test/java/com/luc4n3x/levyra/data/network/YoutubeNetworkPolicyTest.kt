package com.luc4n3x.levyra.data.network

import com.luc4n3x.levyra.data.network.byedpi.ByeDpiSupervisor
import com.luc4n3x.levyra.domain.LevyraNetworkSettings
import com.luc4n3x.levyra.domain.LevyraProxyMode
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class YoutubeNetworkPolicyTest {

    @Before
    @After
    fun resetState() {
        ByeDpiSupervisor.resetForTesting()
        LevyraNetworkConfiguration.apply(LevyraNetworkSettings(), "")
    }

    @Test
    fun hostClassificationCorrectlyIdentifiesDomains() {
        assertTrue(YoutubeNetworkPolicy.isYoutubeHost("www.youtube.com"))
        assertTrue(YoutubeNetworkPolicy.isYoutubeHost("music.youtube.com"))
        assertTrue(YoutubeNetworkPolicy.isYoutubeHost("youtubei.googleapis.com"))
        assertTrue(YoutubeNetworkPolicy.isYoutubeHost("rr1---sn-4g5ednls.googlevideo.com"))
        assertTrue(YoutubeNetworkPolicy.isYoutubeHost("i.ytimg.com"))
        assertFalse(YoutubeNetworkPolicy.isYoutubeHost("jiosaavn.com"))
        assertFalse(YoutubeNetworkPolicy.isYoutubeHost("saavncdn.com"))
        assertFalse(YoutubeNetworkPolicy.isYoutubeHost("example.com"))

        assertTrue(YoutubeNetworkPolicy.isYoutubeMediaHost("googlevideo.com"))
        assertTrue(YoutubeNetworkPolicy.isYoutubeMediaHost("rr2---sn-oxun-xx.googlevideo.com"))
        assertFalse(YoutubeNetworkPolicy.isYoutubeMediaHost("music.youtube.com"))
        assertFalse(YoutubeNetworkPolicy.isYoutubeMediaHost("jiosaavn.com"))

        assertTrue(YoutubeNetworkPolicy.isJioSaavnHost("www.jiosaavn.com"))
        assertTrue(YoutubeNetworkPolicy.isJioSaavnHost("aac.saavncdn.com"))
        assertFalse(YoutubeNetworkPolicy.isJioSaavnHost("googlevideo.com"))
        assertFalse(YoutubeNetworkPolicy.isJioSaavnHost("youtube.com"))
    }

    @Test
    fun baselineRoutesDirectWhenBothMechanismsAreDisabled() {
        val settings = LevyraNetworkSettings(
            byeDpiEnabled = false,
            youtubeRegionProfileEnabled = false
        )
        val youtubeMediaUri = URI("https://rr1---sn-4g5ednls.googlevideo.com/videoplayback?id=123")
        val youtubeApiUri = URI("https://www.youtube.com/youtubei/v1/player")
        val jioSaavnUri = URI("https://www.jiosaavn.com/api.php?__call=song.getDetails")
        val generalUri = URI("https://api.github.com/repos")

        assertEquals(listOf(Proxy.NO_PROXY), YoutubeNetworkPolicy.selectProxies(youtubeMediaUri, settings))
        assertEquals(listOf(Proxy.NO_PROXY), YoutubeNetworkPolicy.selectProxies(youtubeApiUri, settings))
        assertEquals(listOf(Proxy.NO_PROXY), YoutubeNetworkPolicy.selectProxies(jioSaavnUri, settings))
        assertEquals(listOf(Proxy.NO_PROXY), YoutubeNetworkPolicy.selectProxies(generalUri, settings))
    }

    @Test
    fun byeDpiRoutesOnlyYoutubeWhenRunning() {
        val testPort = 1088
        ByeDpiSupervisor.setRunningForTesting(testPort)
        val settings = LevyraNetworkSettings(
            byeDpiEnabled = true,
            youtubeRegionProfileEnabled = false
        )

        val youtubeMediaUri = URI("https://rr1---sn-4g5ednls.googlevideo.com/videoplayback")
        val youtubeApiUri = URI("https://music.youtube.com/youtubei/v1/browse")
        val jioSaavnUri = URI("https://aac.saavncdn.com/123/sample.mp4")
        val generalUri = URI("https://api.spotify.com/v1/artists")

        val mediaProxies = YoutubeNetworkPolicy.selectProxies(youtubeMediaUri, settings)
        assertEquals(1, mediaProxies.size)
        val selectedProxy = mediaProxies.first()
        assertEquals(Proxy.Type.SOCKS, selectedProxy.type())
        val address = selectedProxy.address() as InetSocketAddress
        assertEquals("127.0.0.1", address.hostString)
        assertEquals(testPort, address.port)

        val apiProxies = YoutubeNetworkPolicy.selectProxies(youtubeApiUri, settings)
        assertEquals(1, apiProxies.size)
        assertEquals(Proxy.Type.SOCKS, apiProxies.first().type())

        val jioProxies = YoutubeNetworkPolicy.selectProxies(jioSaavnUri, settings)
        assertEquals(listOf(Proxy.NO_PROXY), jioProxies)

        val genProxies = YoutubeNetworkPolicy.selectProxies(generalUri, settings)
        assertEquals(listOf(Proxy.NO_PROXY), genProxies)
    }

    @Test
    fun byeDpiFailsOverGracefullyWhenStoppedOrDegraded() {
        val settings = LevyraNetworkSettings(byeDpiEnabled = true)
        val youtubeMediaUri = URI("https://rr1---sn-4g5ednls.googlevideo.com/videoplayback")

        ByeDpiSupervisor.resetForTesting()
        assertEquals(listOf(Proxy.NO_PROXY), YoutubeNetworkPolicy.selectProxies(youtubeMediaUri, settings))

        ByeDpiSupervisor.setRunningForTesting(1088)
        repeat(3) { ByeDpiSupervisor.recordConnectionFailure() }
        assertTrue(ByeDpiSupervisor.isTemporarilyDegraded())
        assertEquals(listOf(Proxy.NO_PROXY), YoutubeNetworkPolicy.selectProxies(youtubeMediaUri, settings))

        ByeDpiSupervisor.recordConnectionSuccess()
        assertFalse(ByeDpiSupervisor.isTemporarilyDegraded())
        val recovered = YoutubeNetworkPolicy.selectProxies(youtubeMediaUri, settings)
        assertEquals(Proxy.Type.SOCKS, recovered.first().type())
    }

    @Test
    fun proxySelectorConnectFailedTriggersCircuitBreaker() {
        val testPort = 1088
        ByeDpiSupervisor.setRunningForTesting(testPort)
        val settings = LevyraNetworkSettings(byeDpiEnabled = true)
        val selector = YoutubeNetworkPolicy.createProxySelector(settings)
        val uri = URI("https://rr1---sn-4g5ednls.googlevideo.com/videoplayback")
        val byeDpiAddress = InetSocketAddress.createUnresolved("127.0.0.1", testPort)

        assertFalse(ByeDpiSupervisor.isTemporarilyDegraded())
        selector.connectFailed(uri, byeDpiAddress, IOException("Connection refused"))
        selector.connectFailed(uri, byeDpiAddress, IOException("Connection refused"))
        selector.connectFailed(uri, byeDpiAddress, IOException("Connection refused"))

        assertTrue(ByeDpiSupervisor.isTemporarilyDegraded())
        assertEquals(listOf(Proxy.NO_PROXY), selector.select(uri))
    }

    @Test
    fun externalProxyPrecedenceAndStreamBypass() {
        val externalHost = "192.168.1.100"
        val externalPort = 8080
        val externalSettings = LevyraNetworkSettings(
            proxyMode = LevyraProxyMode.Http,
            proxyHost = externalHost,
            proxyPort = externalPort,
            bypassProxyForStreams = false,
            byeDpiEnabled = true
        )
        LevyraNetworkConfiguration.apply(externalSettings, "")
        ByeDpiSupervisor.setRunningForTesting(1088)

        val youtubeMediaUri = URI("https://rr1---sn-4g5ednls.googlevideo.com/videoplayback")
        val youtubeApiUri = URI("https://www.youtube.com/youtubei/v1/player")
        val jioSaavnUri = URI("https://www.jiosaavn.com/api.php")

        val mediaProxiesNoBypass = YoutubeNetworkPolicy.selectProxies(youtubeMediaUri, externalSettings)
        assertEquals(Proxy.Type.HTTP, mediaProxiesNoBypass.first().type())
        assertEquals(externalHost, (mediaProxiesNoBypass.first().address() as InetSocketAddress).hostString)

        val bypassSettings = externalSettings.copy(bypassProxyForStreams = true)
        LevyraNetworkConfiguration.apply(bypassSettings, "")

        val mediaProxiesWithBypass = YoutubeNetworkPolicy.selectProxies(youtubeMediaUri, bypassSettings)
        assertEquals(Proxy.Type.SOCKS, mediaProxiesWithBypass.first().type())
        assertEquals(1088, (mediaProxiesWithBypass.first().address() as InetSocketAddress).port)

        val apiProxiesWithBypass = YoutubeNetworkPolicy.selectProxies(youtubeApiUri, bypassSettings)
        assertEquals(Proxy.Type.HTTP, apiProxiesWithBypass.first().type())
        assertEquals(externalHost, (apiProxiesWithBypass.first().address() as InetSocketAddress).hostString)

        val jioProxiesWithBypass = YoutubeNetworkPolicy.selectProxies(jioSaavnUri, bypassSettings)
        assertEquals(listOf(Proxy.NO_PROXY), jioProxiesWithBypass)
    }
}
