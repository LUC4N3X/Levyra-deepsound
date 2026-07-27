package com.luc4n3x.levyra.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeMusicResilienceClientTest {
    @Test
    fun emptyPrimaryResponseFallsBackToAndroidMusic() {
        val calls = mutableListOf<String>()
        val client = client { request ->
            calls += request.profile.id
            if (request.profile.id == "web-remix") {
                YoutubeMusicTransportResponse(200, "{}", 20L)
            } else {
                YoutubeMusicTransportResponse(200, validSearch(), 30L)
            }
        }

        val result = client.search("test song", "it")

        assertNotNull(result)
        assertEquals(listOf("web-remix", "android-music"), calls)
        assertEquals(1, client.diagnostics().getValue("web-remix").consecutiveFailures)
        assertEquals(1, client.diagnostics().getValue("android-music").successes)
    }

    @Test
    fun hardFailureTemporarilyRemovesPrimaryFromTheChain() {
        var now = 1_700_000_000_000L
        val calls = mutableListOf<String>()
        val client = client(clock = { now }) { request ->
            calls += request.profile.id
            if (request.profile.id == "web-remix") {
                YoutubeMusicTransportResponse(429, "rate limited", 15L)
            } else {
                YoutubeMusicTransportResponse(200, validSearch(), 25L)
            }
        }

        assertNotNull(client.search("first query", "it"))
        calls.clear()
        assertNotNull(client.search("second query", "it"))

        assertEquals("android-music", calls.first())
        assertTrue(client.diagnostics().getValue("web-remix").blockedUntilMs > now)
    }

    @Test
    fun successfulResponseIsReusedFromShortLivedCache() {
        var calls = 0
        val client = client { _ ->
            calls += 1
            YoutubeMusicTransportResponse(200, validSearch(), 12L)
        }

        val first = client.search("cached query", "it")
        val second = client.search("cached query", "it")

        assertNotNull(first)
        assertNotNull(second)
        assertEquals(1, calls)
    }

    @Test
    fun continuationPayloadSurvivesFallbackAndAcceptsContinuationContents() {
        val payloads = mutableListOf<JSONObject>()
        val client = client { request ->
            payloads += JSONObject(request.payload)
            if (request.profile.id == "web-remix") {
                YoutubeMusicTransportResponse(500, "server error", 40L)
            } else {
                YoutubeMusicTransportResponse(
                    200,
                    """{"continuationContents":{"musicPlaylistShelfContinuation":{"contents":[]}}}""",
                    35L
                )
            }
        }

        val result = client.browse("it", browseId = "", continuation = "CONTINUATION_TOKEN")

        assertNotNull(result)
        assertTrue(payloads.all { it.optString("continuation") == "CONTINUATION_TOKEN" })
        assertTrue(payloads.all { !it.has("browseId") })
    }


    @Test
    fun visitorDataIsLearnedPerProfileAndReused() {
        val visitorHeaders = mutableListOf<String>()
        var call = 0
        val client = client { request ->
            visitorHeaders += request.visitorData
            call += 1
            val body = if (call == 1) {
                """{"responseContext":{"visitorData":"VISITOR_123"},"contents":{"musicResponsiveListItemRenderer":{"title":"Track"}}}"""
            } else {
                validSearch()
            }
            YoutubeMusicTransportResponse(200, body, 10L)
        }

        assertNotNull(client.search("visitor query one", "it"))
        assertNotNull(client.search("visitor query two", "it"))

        assertEquals(listOf("", "VISITOR_123"), visitorHeaders)
    }

    @Test
    fun expiredCachePerformsANewRequest() {
        var now = 1_700_000_000_000L
        var calls = 0
        val client = client(clock = { now }) { _ ->
            calls += 1
            YoutubeMusicTransportResponse(200, validSearch(), 10L)
        }

        assertNotNull(client.search("expiring query", "it"))
        now += 76_000L
        assertNotNull(client.search("expiring query", "it"))

        assertEquals(2, calls)
    }

    private fun client(
        clock: () -> Long = { 1_700_000_000_000L },
        handler: (YoutubeMusicTransportRequest) -> YoutubeMusicTransportResponse
    ): YoutubeMusicResilienceClient {
        return YoutubeMusicResilienceClient(
            context = null,
            apiKey = "test-key",
            webRemixVersion = "1.20260423.01.00",
            clock = clock,
            transport = YoutubeMusicTransport(handler)
        )
    }

    private fun validSearch(): String =
        """{"contents":{"musicResponsiveListItemRenderer":{"title":"Track"}}}"""
}
