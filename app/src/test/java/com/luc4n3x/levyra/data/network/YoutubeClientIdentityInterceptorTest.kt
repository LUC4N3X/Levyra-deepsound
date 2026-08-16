package com.luc4n3x.levyra.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import okhttp3.Request

class YoutubeClientIdentityInterceptorTest {

    @Test
    fun webPlayerUsesTheSameBrowserIdentityAsBotGuardMinting() {
        val request = playerRequest(
            clientName = "1",
            clientVersion = "2.20260630.01.00",
            userAgent = "Mozilla/5.0 Chrome/142.0.0.0",
            origin = "https://www.youtube.com",
            referer = "https://www.youtube.com/watch?v=abcdefghijk"
        )

        val normalized = YoutubeClientIdentityInterceptor.normalize(request)

        assertEquals(
            YoutubeClientIdentityInterceptor.PO_TOKEN_WEB_USER_AGENT,
            normalized.header("User-Agent")
        )
        assertEquals("https://www.youtube.com", normalized.header("Origin"))
        assertEquals(
            "https://www.youtube.com/watch?v=abcdefghijk",
            normalized.header("Referer")
        )
    }

    @Test
    fun visitorRequestChangesOnlyTheWebUserAgent() {
        val request = Request.Builder()
            .url("https://youtubei.googleapis.com/youtubei/v1/visitor_id")
            .header("User-Agent", "Mozilla/5.0 Chrome/142.0.0.0")
            .header("X-Youtube-Client-Name", "1")
            .header("X-Youtube-Client-Version", "2.20260630.01.00")
            .build()

        val normalized = YoutubeClientIdentityInterceptor.normalize(request)

        assertEquals(
            YoutubeClientIdentityInterceptor.PO_TOKEN_WEB_USER_AGENT,
            normalized.header("User-Agent")
        )
        assertNull(normalized.header("Origin"))
        assertNull(normalized.header("Referer"))
    }

    @Test
    fun androidMusicUsesANativeAppIdentityAndDropsBrowserNavigationHeaders() {
        val request = playerRequest(
            clientName = "21",
            clientVersion = "8.10.52",
            userAgent = "Mozilla/5.0 (Linux; Android 15) com.google.android.apps.youtube.music/8.10.52",
            origin = "https://www.youtube.com",
            referer = "https://www.youtube.com/watch?v=abcdefghijk"
        )

        val normalized = YoutubeClientIdentityInterceptor.normalize(request)

        assertEquals(
            "com.google.android.apps.youtube.music/8.10.52 (Linux; U; Android 15) gzip",
            normalized.header("User-Agent")
        )
        assertNull(normalized.header("Origin"))
        assertNull(normalized.header("Referer"))
    }

    @Test
    fun nativeClientsDoNotCarryBrowserNavigationHeaders() {
        listOf("3", "5", "28", "101").forEach { clientName ->
            val request = playerRequest(
                clientName = clientName,
                clientVersion = "1.0",
                userAgent = "native-client/1.0",
                origin = "https://www.youtube.com",
                referer = "https://www.youtube.com/watch?v=abcdefghijk"
            )

            val normalized = YoutubeClientIdentityInterceptor.normalize(request)

            assertEquals("native-client/1.0", normalized.header("User-Agent"))
            assertNull(normalized.header("Origin"))
            assertNull(normalized.header("Referer"))
        }
    }

    @Test
    fun webRemixUsesMusicOriginAndReferer() {
        val request = playerRequest(
            clientName = "67",
            clientVersion = "1.20260423.01.00",
            userAgent = "Mozilla/5.0 Chrome/142.0.0.0",
            origin = "https://www.youtube.com",
            referer = "https://www.youtube.com/watch?v=abcdefghijk"
        )

        val normalized = YoutubeClientIdentityInterceptor.normalize(request)

        assertEquals("https://music.youtube.com", normalized.header("Origin"))
        assertEquals(
            "https://music.youtube.com/watch?v=abcdefghijk",
            normalized.header("Referer")
        )
    }

    @Test
    fun embeddedClientPreservesOnlyAnEmbedReferer() {
        val valid = playerRequest(
            clientName = "56",
            clientVersion = "1.20260423.01.00",
            userAgent = "Mozilla/5.0 Chrome/142.0.0.0",
            origin = "https://www.youtube.com",
            referer = "https://www.youtube.com/embed/abcdefghijk"
        )
        val invalid = valid.newBuilder()
            .header("Referer", "https://www.youtube.com/watch?v=abcdefghijk")
            .build()

        assertEquals(
            "https://www.youtube.com/embed/abcdefghijk",
            YoutubeClientIdentityInterceptor.normalize(valid).header("Referer")
        )
        assertEquals(
            "https://www.youtube.com/",
            YoutubeClientIdentityInterceptor.normalize(invalid).header("Referer")
        )
    }

    @Test
    fun unknownClientIsLeftUntouched() {
        val request = playerRequest(
            clientName = "999",
            clientVersion = "1.0",
            userAgent = "custom/1.0",
            origin = "https://example.com",
            referer = "https://example.com/video"
        )

        assertSame(request, YoutubeClientIdentityInterceptor.normalize(request))
    }

    private fun playerRequest(
        clientName: String,
        clientVersion: String,
        userAgent: String,
        origin: String,
        referer: String
    ): Request {
        return Request.Builder()
            .url("https://youtubei.googleapis.com/youtubei/v1/player")
            .header("User-Agent", userAgent)
            .header("Origin", origin)
            .header("Referer", referer)
            .header("X-Youtube-Client-Name", clientName)
            .header("X-Youtube-Client-Version", clientVersion)
            .build()
    }
}
