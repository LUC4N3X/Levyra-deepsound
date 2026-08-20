package com.luc4n3x.levyra.desktop.core.stream

import com.sun.net.httpserver.HttpServer
import java.net.InetAddress
import java.net.InetSocketAddress
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeStreamNetworkPolicyTest {
    @Test
    fun acceptsOnlyStrictGooglevideoHttpsTargets() {
        assertTrue(YoutubeStreamNetworkPolicy.isAllowedUrl("https://r1.googlevideo.com/videoplayback".toHttpUrl()))
        assertTrue(YoutubeStreamNetworkPolicy.isAllowedUrl("https://googlevideo.com/videoplayback".toHttpUrl()))
        assertFalse(YoutubeStreamNetworkPolicy.isAllowedUrl("http://r1.googlevideo.com/videoplayback".toHttpUrl()))
        assertFalse(YoutubeStreamNetworkPolicy.isAllowedUrl("https://user:pass@r1.googlevideo.com/videoplayback".toHttpUrl()))
        assertFalse(YoutubeStreamNetworkPolicy.isAllowedUrl("https://googlevideo.com:444/videoplayback".toHttpUrl()))
        assertFalse(YoutubeStreamNetworkPolicy.isAllowedUrl("https://googlevideo.com.example.test/videoplayback".toHttpUrl()))
        assertFalse(YoutubeStreamNetworkPolicy.isAllowedUrl("https://127.0.0.1/videoplayback".toHttpUrl()))
    }

    @Test
    fun rejectsNonPublicDestinations() {
        assertFalse(YoutubeStreamNetworkPolicy.isPublicInternetAddress(InetAddress.getByName("127.0.0.1")))
        assertFalse(YoutubeStreamNetworkPolicy.isPublicInternetAddress(InetAddress.getByName("10.0.0.1")))
        assertFalse(YoutubeStreamNetworkPolicy.isPublicInternetAddress(InetAddress.getByName("100.64.0.1")))
        assertFalse(YoutubeStreamNetworkPolicy.isPublicInternetAddress(InetAddress.getByName("2001:db8::1")))
        assertTrue(YoutubeStreamNetworkPolicy.isPublicInternetAddress(InetAddress.getByName("8.8.8.8")))
    }

    @Test
    fun probeRejectsRedirectBeforeConnectingOutsideTheValidatedHost() {
        val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
        server.createContext("/audio") { exchange ->
            exchange.responseHeaders.add("Location", "https://example.com/private")
            exchange.sendResponseHeaders(302, -1)
            exchange.close()
        }
        server.start()
        try {
            val url = "http://127.0.0.1:${server.address.port}/audio"
            val verified = probeDirectAudioUrlFast(
                url = url,
                client = OkHttpClient.Builder().followRedirects(false).build(),
                isFresh = { true },
                isAllowed = { it.host == "127.0.0.1" }
            )

            assertFalse(verified)
        } finally {
            server.stop(0)
        }
    }
}
