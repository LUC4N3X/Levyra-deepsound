package com.luc4n3x.levyra.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

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
    fun rateLimitTemporarilyRemovesPrimaryFromTheChain() {
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
    fun searchSuggestionsUsesMusicEndpointAndInputPayload() {
        val requests = mutableListOf<YoutubeMusicTransportRequest>()
        val client = client { request ->
            requests += request
            YoutubeMusicTransportResponse(200, validSuggestions(), 9L)
        }

        val result = client.searchSuggestions("daft punk", "it")

        assertNotNull(result)
        assertEquals(1, requests.size)
        assertEquals("music/get_search_suggestions", requests.single().path)
        assertEquals("web-remix", requests.single().profile.id)
        assertEquals("daft punk", JSONObject(requests.single().payload).optString("input"))
    }

    @Test
    fun suggestionFailureDoesNotPenalizePrimarySearchProfile() {
        val calls = mutableListOf<String>()
        var suggestionRequest = true
        val client = client { request ->
            calls += "${request.path}:${request.profile.id}"
            if (suggestionRequest) {
                YoutubeMusicTransportResponse(500, "suggestion endpoint failure", 10L)
            } else {
                YoutubeMusicTransportResponse(200, validSearch(), 10L)
            }
        }

        assertNull(client.searchSuggestions("daft punk", "it"))
        assertTrue(client.diagnostics().isEmpty())

        suggestionRequest = false
        assertNotNull(client.search("public search", "it"))

        assertEquals("search:web-remix", calls.last())
        assertEquals(0, client.diagnostics().getValue("web-remix").failures)
        assertEquals(1, client.diagnostics().getValue("web-remix").successes)
    }

    @Test
    fun searchAcceptsTabbedSearchResultsRendererCarryingResults() {
        val client = client { _ ->
            YoutubeMusicTransportResponse(
                200,
                """{"contents":{"tabbedSearchResultsRenderer":{"tabs":[{"tabRenderer":{"content":{"sectionListRenderer":{"contents":[{"itemSectionRenderer":{"contents":[{"musicResponsiveListItemRenderer":{"title":"Track"}}]}}]}}}}]}}}""",
                10L
            )
        }

        assertNotNull(client.search("alternate renderer", "it"))
    }

    @Test
    fun emptyTabbedSearchResultsRendererKeepsTheFallbackChainRunning() {
        val calls = mutableListOf<String>()
        val client = client { request ->
            calls += request.profile.id
            if (calls.size < 2) {
                YoutubeMusicTransportResponse(200, """{"contents":{"tabbedSearchResultsRenderer":{"tabs":[]}}}""", 10L)
            } else {
                YoutubeMusicTransportResponse(200, validSearch(), 10L)
            }
        }

        assertNotNull(client.search("structural wrapper only", "it"))
        assertTrue(calls.size >= 2)
    }

    @Test
    fun emptyItemSectionRendererIsNotAcceptedAsSearchContent() {
        val client = client { _ ->
            YoutubeMusicTransportResponse(
                200,
                """{"contents":{"sectionListRenderer":{"contents":[{"itemSectionRenderer":{"contents":[]}}]}}}""",
                10L
            )
        }

        assertNull(client.search("empty item section", "it"))
    }

    @Test
    fun searchContinuationWithResultsRemainsUseful() {
        val client = client { _ ->
            YoutubeMusicTransportResponse(
                200,
                """{"continuationContents":{"musicShelfContinuation":{"contents":[{"musicResponsiveListItemRenderer":{"title":"Track"}}]}}}""",
                10L
            )
        }

        assertNotNull(client.search("continuation", "it", continuation = "TOKEN"))
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

    @Test
    fun requestSpecificDenialsNeverBlockTheWholeFallbackChain() {
        val publicRequest = AtomicBoolean(false)
        val calls = mutableListOf<String>()
        val client = client { request ->
            calls += request.profile.id
            if (publicRequest.get()) {
                YoutubeMusicTransportResponse(200, validSearch(), 10L)
            } else {
                YoutubeMusicTransportResponse(403, "private or region restricted resource", 10L)
            }
        }

        assertNull(client.browse("it", browseId = "PRIVATE"))
        assertTrue(client.diagnostics().values.all { it.blockedUntilMs == 0L })

        calls.clear()
        publicRequest.set(true)
        assertNotNull(client.search("public song", "it"))
        assertEquals(listOf("web-remix"), calls)
    }

    @Test
    fun profileSpecificDenialIsDeprioritizedAfterFallbackRecovery() {
        val calls = mutableListOf<String>()
        val client = client { request ->
            calls += request.profile.id
            if (request.profile.id == "web-remix") {
                YoutubeMusicTransportResponse(403, "bot blocked", 10L)
            } else {
                YoutubeMusicTransportResponse(200, validSearch(), 10L)
            }
        }

        assertNotNull(client.search("first public query", "it"))
        assertEquals(listOf("web-remix", "android-music"), calls)
        assertEquals(1, client.diagnostics().getValue("web-remix").consecutiveDenials)
        assertEquals(0L, client.diagnostics().getValue("web-remix").blockedUntilMs)

        calls.clear()
        assertNotNull(client.search("second public query", "it"))
        assertEquals(listOf("android-music"), calls)
    }

    @Test
    fun failedConcurrentRequestsShareOneFallbackChain() {
        val calls = AtomicInteger()
        val firstEntered = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        val client = client { _ ->
            if (calls.incrementAndGet() == 1) {
                firstEntered.countDown()
                releaseFirst.await(5, TimeUnit.SECONDS)
            }
            YoutubeMusicTransportResponse(500, "server error", 10L)
        }
        val executor = Executors.newFixedThreadPool(3)

        try {
            val leader = executor.submit<JSONObject?> { client.search("same failing query", "it") }
            assertTrue(firstEntered.await(5, TimeUnit.SECONDS))
            val waiterOne = executor.submit<JSONObject?> { client.search("same failing query", "it") }
            val waiterTwo = executor.submit<JSONObject?> { client.search("same failing query", "it") }
            assertTrue(awaitCondition { client.inFlightReferenceCounts().any { it == 3 } })
            releaseFirst.countDown()

            assertNull(leader.get(5, TimeUnit.SECONDS))
            assertNull(waiterOne.get(5, TimeUnit.SECONDS))
            assertNull(waiterTwo.get(5, TimeUnit.SECONDS))
            assertEquals(5, calls.get())
        } finally {
            releaseFirst.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun fallbackChainHonorsOneOverallDeadline() {
        var now = 1_700_000_000_000L
        val timeouts = mutableListOf<Long>()
        val client = client(clock = { now }) { request ->
            timeouts += request.timeoutMs
            now += 12_000L
            YoutubeMusicTransportResponse(500, "server error", 12_000L)
        }

        assertNull(client.search("deadline query", "it"))
        assertEquals(listOf(14_000L, 14_000L, 11_000L), timeouts)
    }

    @Test
    fun interruptionStopsTheFallbackChainImmediately() {
        val calls = AtomicInteger()
        val client = client { _ ->
            calls.incrementAndGet()
            throw InterruptedException("cancelled")
        }

        try {
            assertNull(client.search("interrupted query", "it"))
            assertEquals(1, calls.get())
            assertTrue(Thread.currentThread().isInterrupted)
        } finally {
            Thread.interrupted()
        }
    }

    @Test
    fun hardBlockedProfilesAreNotRetriedBeforeTheirDeadline() {
        var now = 1_700_000_000_000L
        val calls = AtomicInteger()
        val client = client(clock = { now }) { _ ->
            calls.incrementAndGet()
            YoutubeMusicTransportResponse(429, "rate limited", 10L)
        }

        assertNull(client.search("first rate limited query", "it"))
        val deadlines = client.diagnostics().mapValues { it.value.blockedUntilMs }
        assertEquals(5, calls.get())

        now += 1_000L
        assertNull(client.search("second rate limited query", "it"))

        assertEquals(5, calls.get())
        assertEquals(deadlines, client.diagnostics().mapValues { it.value.blockedUntilMs })
    }

    @Test
    fun oversizedResponsesAreNotRetainedInMemoryCache() {
        val calls = AtomicInteger()
        val oversized = """{"contents":{"musicResponsiveListItemRenderer":{"title":"${"x".repeat(600_000)}"}}}"""
        val client = client { _ ->
            calls.incrementAndGet()
            YoutubeMusicTransportResponse(200, oversized, 10L)
        }

        assertNotNull(client.search("large response", "it"))
        assertEquals(0, client.cacheDiagnostics().entries)
        assertEquals(0L, client.cacheDiagnostics().bytes)
        assertNotNull(client.search("large response", "it"))
        assertEquals(2, calls.get())
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
            monotonicClock = clock,
            transport = YoutubeMusicTransport(handler)
        )
    }

    private fun awaitCondition(timeoutMs: Long = 5_000L, condition: () -> Boolean): Boolean {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
        while (System.nanoTime() < deadline) {
            if (condition()) return true
            Thread.yield()
        }
        return condition()
    }

    private fun validSearch(): String =
        """{"contents":{"musicResponsiveListItemRenderer":{"title":"Track"}}}"""

    private fun validSuggestions(): String =
        """{"contents":[{"searchSuggestionsSectionRenderer":{"contents":[{"searchSuggestionRenderer":{"navigationEndpoint":{"searchEndpoint":{"query":"daft punk"}}}}]}}]}"""

    @Test
    fun browseStopsWhenThePolicyDisablesBrowseOnEveryClient() {
        val disabled = PlaybackCompatibilityPolicy.bundled().copy(
            clientOverrides = listOf(
                "WEB_REMIX",
                "ANDROID_MUSIC",
                "ANDROID",
                "IOS",
                "WEB"
            ).associateWith {
                PlaybackClientOverride(
                    capabilities = mapOf(PlaybackClientCapability.BROWSE to false)
                )
            }
        )
        PlaybackClientCapabilities.publish(disabled)
        try {
            val calls = AtomicInteger(0)
            val client = client { _ ->
                calls.incrementAndGet()
                YoutubeMusicTransportResponse(200, validSearch(), 10L)
            }

            assertNull(client.search("test song", "it"))
            assertEquals(0, calls.get())
        } finally {
            PlaybackClientCapabilities.publish(PlaybackCompatibilityPolicy.bundled())
        }
    }

    @Test
    fun browseKeepsTheClientsThePolicyStillPermits() {
        val partial = PlaybackCompatibilityPolicy.bundled().copy(
            clientOverrides = mapOf(
                "WEB_REMIX" to PlaybackClientOverride(
                    capabilities = mapOf(PlaybackClientCapability.BROWSE to false)
                )
            )
        )
        PlaybackClientCapabilities.publish(partial)
        try {
            val calls = mutableListOf<String>()
            val client = client { request ->
                calls += request.profile.id
                YoutubeMusicTransportResponse(200, validSearch(), 10L)
            }

            assertNotNull(client.search("test song", "it"))
            assertTrue(calls.isNotEmpty())
            assertTrue(calls.none { it == "web-remix" })
        } finally {
            PlaybackClientCapabilities.publish(PlaybackCompatibilityPolicy.bundled())
        }
    }
}
