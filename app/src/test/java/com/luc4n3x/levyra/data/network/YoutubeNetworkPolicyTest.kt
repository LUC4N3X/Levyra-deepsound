package com.luc4n3x.levyra.data.network

import com.luc4n3x.levyra.data.network.byedpi.ByeDpiSupervisor
import com.luc4n3x.levyra.domain.LevyraNetworkSettings
import com.luc4n3x.levyra.domain.LevyraProxyMode
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class YoutubeNetworkPolicyTest {
    private var nowMs = 0L

    private val direct = listOf(Proxy.NO_PROXY)
    private val mediaHost = "rr1---sn-4g5ednls.googlevideo.com"
    private val apiHost = "music.youtube.com"
    private val mediaUri = URI("https://$mediaHost/videoplayback")
    private val apiUri = URI("https://$apiHost/youtubei/v1/browse")
    private val jioApiUri = URI("https://www.jiosaavn.com/api.php?__call=song.getDetails")
    private val jioCdnUri = URI("https://aac.saavncdn.com/123/sample_320.mp4")
    private val generalUri = URI("https://api.github.com/repos")

    @Before
    @After
    fun resetState() {
        nowMs = 1_000L
        ByeDpiSupervisor.resetForTesting { nowMs }
        LevyraNetworkConfiguration.apply(LevyraNetworkSettings(), "")
    }

    private fun externalProxySettings(
        bypassProxyForStreams: Boolean,
        byeDpiEnabled: Boolean = true,
        proxyHost: String = "192.168.1.100"
    ) = LevyraNetworkSettings(
        proxyMode = LevyraProxyMode.Http,
        proxyHost = proxyHost,
        proxyPort = 8080,
        bypassProxyForStreams = bypassProxyForStreams,
        byeDpiEnabled = byeDpiEnabled
    )

    private fun assertExternalHttp(proxies: List<Proxy>) {
        assertEquals(1, proxies.size)
        assertEquals(Proxy.Type.HTTP, proxies.first().type())
        assertEquals("192.168.1.100", (proxies.first().address() as InetSocketAddress).hostString)
    }

    private fun assertTunnel(port: Int, tunnel: InetSocketAddress?) {
        requireNotNull(tunnel)
        assertEquals("127.0.0.1", tunnel.address.hostAddress)
        assertFalse(tunnel.isUnresolved)
        assertEquals(port, tunnel.port)
    }

    @Test
    fun hostClassificationCorrectlyIdentifiesDomains() {
        assertTrue(YoutubeNetworkPolicy.isYoutubeHost("www.youtube.com"))
        assertTrue(YoutubeNetworkPolicy.isYoutubeHost("music.youtube.com"))
        assertTrue(YoutubeNetworkPolicy.isYoutubeHost("youtubei.googleapis.com"))
        assertTrue(YoutubeNetworkPolicy.isYoutubeHost(mediaHost))
        assertTrue(YoutubeNetworkPolicy.isYoutubeHost("i.ytimg.com"))
        assertTrue(YoutubeNetworkPolicy.isYoutubeHost("RR1.GoogleVideo.com."))
        assertFalse(YoutubeNetworkPolicy.isYoutubeHost("jiosaavn.com"))
        assertFalse(YoutubeNetworkPolicy.isYoutubeHost("saavncdn.com"))
        assertFalse(YoutubeNetworkPolicy.isYoutubeHost("example.com"))
        assertFalse(YoutubeNetworkPolicy.isYoutubeHost("notyoutube.com"))
        assertFalse(YoutubeNetworkPolicy.isYoutubeHost("youtube.com.evil.example"))
        assertFalse(YoutubeNetworkPolicy.isYoutubeHost("evilgooglevideo.com"))
        assertFalse(YoutubeNetworkPolicy.isYoutubeHost("142.250.1.2"))

        assertTrue(YoutubeNetworkPolicy.isYoutubeMediaHost("googlevideo.com"))
        assertTrue(YoutubeNetworkPolicy.isYoutubeMediaHost("rr2---sn-oxun-xx.googlevideo.com"))
        assertFalse(YoutubeNetworkPolicy.isYoutubeMediaHost("music.youtube.com"))
        assertFalse(YoutubeNetworkPolicy.isYoutubeMediaHost("jiosaavn.com"))
        assertFalse(YoutubeNetworkPolicy.isYoutubeMediaHost("evilgooglevideo.com"))
    }

    @Test
    fun byeDpiNeverAppearsAsAProxyThatWouldLeakTheHostnameToItsResolver() {
        ByeDpiSupervisor.setRunningForTesting(1088)
        val settings = LevyraNetworkSettings(byeDpiEnabled = true)

        for (uri in listOf(mediaUri, apiUri, jioApiUri, jioCdnUri, generalUri)) {
            assertEquals(direct, YoutubeNetworkPolicy.selectProxies(uri, settings))
        }
    }

    @Test
    fun byeDpiRouteCoversOnlyYoutubeHostsWhenEnabled() {
        val enabled = LevyraNetworkSettings(byeDpiEnabled = true)
        val disabled = LevyraNetworkSettings(byeDpiEnabled = false)

        assertTrue(YoutubeNetworkPolicy.routesThroughByeDpi(mediaHost, enabled, streamBypass = false))
        assertTrue(YoutubeNetworkPolicy.routesThroughByeDpi(apiHost, enabled, streamBypass = false))
        assertTrue(YoutubeNetworkPolicy.routesThroughByeDpi("i.ytimg.com", enabled, streamBypass = false))
        assertFalse(YoutubeNetworkPolicy.routesThroughByeDpi("www.jiosaavn.com", enabled, streamBypass = false))
        assertFalse(YoutubeNetworkPolicy.routesThroughByeDpi("aac.saavncdn.com", enabled, streamBypass = true))
        assertFalse(YoutubeNetworkPolicy.routesThroughByeDpi("api.github.com", enabled, streamBypass = false))
        assertFalse(YoutubeNetworkPolicy.routesThroughByeDpi(null, enabled, streamBypass = false))
        assertFalse(YoutubeNetworkPolicy.routesThroughByeDpi(mediaHost, disabled, streamBypass = false))
        assertFalse(YoutubeNetworkPolicy.routesThroughByeDpi(mediaHost, disabled, streamBypass = true))
    }

    @Test
    fun explicitExternalProxyWinsOverByeDpiUnlessStreamBypassSendsMediaToByeDpi() {
        val noBypass = externalProxySettings(bypassProxyForStreams = false)
        assertExternalHttp(YoutubeNetworkPolicy.selectProxies(mediaUri, noBypass))
        assertExternalHttp(YoutubeNetworkPolicy.selectProxies(apiUri, noBypass))
        assertFalse(YoutubeNetworkPolicy.routesThroughByeDpi(mediaHost, noBypass, streamBypass = false))
        assertFalse(YoutubeNetworkPolicy.routesThroughByeDpi(apiHost, noBypass, streamBypass = false))

        val bypass = externalProxySettings(bypassProxyForStreams = true)
        assertEquals(direct, YoutubeNetworkPolicy.selectProxies(mediaUri, bypass))
        assertTrue(YoutubeNetworkPolicy.routesThroughByeDpi(mediaHost, bypass, streamBypass = false))
        assertExternalHttp(YoutubeNetworkPolicy.selectProxies(apiUri, bypass))
        assertFalse(YoutubeNetworkPolicy.routesThroughByeDpi(apiHost, bypass, streamBypass = false))
    }

    @Test
    fun externalProxyWithoutByeDpiKeepsLegacyRouting() {
        val noBypass = externalProxySettings(bypassProxyForStreams = false, byeDpiEnabled = false)
        assertExternalHttp(YoutubeNetworkPolicy.selectProxies(mediaUri, noBypass))

        val bypass = externalProxySettings(bypassProxyForStreams = true, byeDpiEnabled = false)
        assertEquals(direct, YoutubeNetworkPolicy.selectProxies(mediaUri, bypass))
        assertExternalHttp(YoutubeNetworkPolicy.selectProxies(apiUri, bypass))
        assertFalse(YoutubeNetworkPolicy.routesThroughByeDpi(mediaHost, bypass, streamBypass = true))
    }

    @Test
    fun nonYoutubeRoutingMatchesLegacyExternalProxyBehaviour() {
        for (bypass in listOf(true, false)) {
            val settings = externalProxySettings(bypassProxyForStreams = bypass)
            assertExternalHttp(YoutubeNetworkPolicy.selectProxies(generalUri, settings))
            assertExternalHttp(YoutubeNetworkPolicy.selectProxies(jioApiUri, settings))
            assertExternalHttp(YoutubeNetworkPolicy.selectProxies(jioCdnUri, settings))
        }
    }

    @Test
    fun blankExternalProxyHostDoesNotSuppressByeDpi() {
        val settings = externalProxySettings(bypassProxyForStreams = false, proxyHost = "")

        assertEquals(direct, YoutubeNetworkPolicy.selectProxies(mediaUri, settings))
        assertTrue(YoutubeNetworkPolicy.routesThroughByeDpi(mediaHost, settings, streamBypass = false))
    }

    @Test
    fun nullUriRoutesDirect() {
        assertEquals(direct, YoutubeNetworkPolicy.selectProxies(null, externalProxySettings(bypassProxyForStreams = false)))
    }

    @Test
    fun tunnelIsNumericLoopbackWhenRunning() {
        ByeDpiSupervisor.setRunningForTesting(1088)

        assertTunnel(1088, ByeDpiSupervisor.acquireTunnel())
    }

    @Test
    fun noTunnelWhenSupervisorIsStoppedOrFailed() {
        assertNull(ByeDpiSupervisor.acquireTunnel())

        ByeDpiSupervisor.setStartingForTesting(1088)
        ByeDpiSupervisor.finishStartingForTesting(running = false)
        assertNull(ByeDpiSupervisor.acquireTunnel())
    }

    @Test
    fun circuitBreakerOpensAfterRepeatedFailuresAndRecoversAfterCooldownProbe() {
        ByeDpiSupervisor.setRunningForTesting(1088)

        repeat(3) { ByeDpiSupervisor.recordConnectionFailure() }
        assertTrue(ByeDpiSupervisor.isTemporarilyDegraded())
        assertNull(ByeDpiSupervisor.acquireTunnel())

        nowMs += 29_999L
        assertNull(ByeDpiSupervisor.acquireTunnel())

        nowMs += 1L
        assertTunnel(1088, ByeDpiSupervisor.acquireTunnel())
        assertNull(ByeDpiSupervisor.acquireTunnel())

        ByeDpiSupervisor.recordConnectionSuccess()
        assertFalse(ByeDpiSupervisor.isTemporarilyDegraded())
        assertTunnel(1088, ByeDpiSupervisor.acquireTunnel())
        assertTunnel(1088, ByeDpiSupervisor.acquireTunnel())
    }

    @Test
    fun failedProbeKeepsByeDpiSuspendedForAnotherCooldown() {
        ByeDpiSupervisor.setRunningForTesting(1088)
        repeat(3) { ByeDpiSupervisor.recordConnectionFailure() }
        nowMs += 30_000L
        assertTunnel(1088, ByeDpiSupervisor.acquireTunnel())

        ByeDpiSupervisor.recordConnectionFailure()

        assertNull(ByeDpiSupervisor.acquireTunnel())
        nowMs += 29_999L
        assertNull(ByeDpiSupervisor.acquireTunnel())
        nowMs += 1L
        assertTunnel(1088, ByeDpiSupervisor.acquireTunnel())
    }

    @Test
    fun startupWindowWaitsForByeDpiInsteadOfLeakingDirect() {
        ByeDpiSupervisor.setStartingForTesting(1088)
        val finisher = Thread {
            Thread.sleep(75)
            ByeDpiSupervisor.finishStartingForTesting(running = true)
        }.also { it.start() }

        val tunnel = ByeDpiSupervisor.acquireTunnel()
        finisher.join()

        assertTunnel(1088, tunnel)
    }

    @Test
    fun startupFailureFallsBackToDirect() {
        ByeDpiSupervisor.setStartingForTesting(1088)
        val finisher = Thread {
            Thread.sleep(75)
            ByeDpiSupervisor.finishStartingForTesting(running = false)
        }.also { it.start() }

        val tunnel = ByeDpiSupervisor.acquireTunnel()
        finisher.join()

        assertNull(tunnel)
    }

    @Test
    fun awaitRunningTimesOutWhenStartupNeverCompletes() {
        ByeDpiSupervisor.setStartingForTesting(1088)

        assertFalse(ByeDpiSupervisor.awaitRunning(50L))
        assertTrue(ByeDpiSupervisor.isEngaged())
        assertFalse(ByeDpiSupervisor.isRunning())
    }
}
