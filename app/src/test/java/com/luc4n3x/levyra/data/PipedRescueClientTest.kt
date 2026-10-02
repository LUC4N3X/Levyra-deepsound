package com.luc4n3x.levyra.data

import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.yield
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PipedRescueClientTest {
    private val instanceA = PipedInstance("a", "https://api-a.example")
    private val instanceB = PipedInstance("b", "https://api-b.example")

    @Test
    fun deadInstanceFallsThroughToHealthyInstance() = runBlocking {
        val exchange = RecordingExchange { request ->
            when {
                request.url.startsWith(instanceA.apiBaseUrl) -> response(500)
                request.kind == PipedRequestKind.API -> response(200, proxyBody("https://proxy-b.example"))
                else -> response(206, contentType = "audio/mp4")
            }
        }
        val client = PipedRescueClient(exchange, listOf(instanceA, instanceB))

        val resolved = client.resolve(VIDEO_ID, "high")

        assertEquals("b", resolved?.instanceId)
        assertEquals(1, client.instanceSnapshot("a").failures)
        assertEquals(1, client.instanceSnapshot("b").successes)
    }

    @Test
    fun handles403429And5xxAsInstanceFailures() = runBlocking {
        for (status in listOf(403, 429, 503)) {
            val exchange = RecordingExchange { response(status) }
            val client = PipedRescueClient(exchange, listOf(instanceA))

            assertNull(client.resolve(VIDEO_ID, "auto"))
            assertEquals(1, client.instanceSnapshot("a").failures)
            assertTrue(client.negativeCacheSize() > 0)
        }
    }

    @Test
    fun singleInstance404FallsThroughToAnotherInstance() = runBlocking {
        val exchange = RecordingExchange { request ->
            when {
                request.url.startsWith(instanceA.apiBaseUrl) -> response(404)
                request.kind == PipedRequestKind.API -> response(200, proxyBody("https://proxy-b.example"))
                else -> response(206, contentType = "audio/mp4")
            }
        }
        val client = PipedRescueClient(exchange, listOf(instanceA, instanceB))

        val resolved = client.resolve(VIDEO_ID, "auto")

        assertEquals("b", resolved?.instanceId)
        assertEquals(0, client.instanceSnapshot("a").failures)
        assertEquals(1, client.instanceSnapshot("b").successes)
    }

    @Test
    fun allHealthyInstances404ThenCacheGlobalVideoMissing() = runBlocking {
        val exchange = RecordingExchange { response(404) }
        val client = PipedRescueClient(exchange, listOf(instanceA, instanceB))

        assertNull(client.resolve(VIDEO_ID, "auto"))
        assertNull(client.resolve(VIDEO_ID, "auto"))

        assertEquals(2, exchange.calls.get())
        assertEquals(0, client.instanceSnapshot("a").failures)
        assertEquals(0, client.instanceSnapshot("b").failures)
    }

    @Test
    fun timeoutUsesNegativeCacheInsteadOfHammeringSameInstance() = runBlocking {
        val exchange = RecordingExchange { throw SocketTimeoutException("timeout") }
        val client = PipedRescueClient(exchange, listOf(instanceA))

        assertNull(client.resolve(VIDEO_ID, "auto"))
        assertNull(client.resolve(VIDEO_ID, "auto"))

        assertEquals(1, exchange.calls.get())
        assertEquals(1, client.instanceSnapshot("a").failures)
    }

    @Test
    fun cooldownIsNotBypassedWhenEveryInstanceIsBlocked() = runBlocking {
        var nowMs = 10_000L
        val exchange = RecordingExchange { response(500) }
        val client = PipedRescueClient(exchange, listOf(instanceA), clockMs = { nowMs })

        assertNull(client.resolve(VIDEO_ID, "auto"))
        assertNull(client.resolve(SECOND_VIDEO_ID, "auto"))
        assertEquals(2, exchange.calls.get())
        assertTrue(client.instanceSnapshot("a").blockedUntilMs > nowMs)

        assertNull(client.resolve(THIRD_VIDEO_ID, "auto"))
        assertEquals(2, exchange.calls.get())

        nowMs += PipedRescuePolicy.INSTANCE_COOLDOWN_MS
        assertNull(client.resolve(FOURTH_VIDEO_ID, "auto"))
        assertEquals(3, exchange.calls.get())
    }

    @Test
    fun repeatedInstanceFailuresEscalateCooldownUpToCap() = runBlocking {
        var nowMs = 10_000L
        val exchange = RecordingExchange { response(500) }
        val client = PipedRescueClient(exchange, listOf(instanceA), clockMs = { nowMs })
        val videos = listOf(VIDEO_ID, SECOND_VIDEO_ID, THIRD_VIDEO_ID, FOURTH_VIDEO_ID, "aaaaaaaaaaa", "bbbbbbbbbbb", "ccccccccccc", "ddddddddddd")

        assertNull(client.resolve(videos[0], "auto"))
        assertNull(client.resolve(videos[1], "auto"))
        val cooldowns = mutableListOf(client.instanceSnapshot("a").blockedUntilMs - nowMs)
        for (video in videos.drop(2)) {
            nowMs = client.instanceSnapshot("a").blockedUntilMs
            assertNull(client.resolve(video, "auto"))
            cooldowns += client.instanceSnapshot("a").blockedUntilMs - nowMs
        }

        assertEquals(
            listOf(60_000L, 120_000L, 240_000L, 480_000L, 900_000L, 900_000L, 900_000L),
            cooldowns
        )
        assertEquals(videos.size, exchange.calls.get())
    }

    @Test
    fun multipleFailedProbesInOneResolutionEscalateCooldownOnce() = runBlocking {
        var nowMs = 10_000L
        val proxy = "https://proxy-a.example"
        val body = JSONObject()
            .put("proxyUrl", proxy)
            .put(
                "audioStreams",
                JSONArray()
                    .put(stream("$proxy/m4a/videoplayback?expire=2000000000&n=m4a", "audio/mp4", "mp4a.40.2", 128_000))
                    .put(stream("$proxy/opus/videoplayback?expire=2000000000&n=opus", "audio/webm", "opus", 160_000))
            )
            .toString()
        val exchange = RecordingExchange { request ->
            if (request.kind == PipedRequestKind.API) response(200, body) else response(503)
        }
        val client = PipedRescueClient(exchange, listOf(instanceA), clockMs = { nowMs })

        assertNull(client.resolve(VIDEO_ID, "auto"))
        val firstCooldown = client.instanceSnapshot("a").blockedUntilMs - nowMs
        nowMs = client.instanceSnapshot("a").blockedUntilMs
        assertNull(client.resolve(SECOND_VIDEO_ID, "auto"))
        val secondCooldown = client.instanceSnapshot("a").blockedUntilMs - nowMs

        assertEquals(PipedRescuePolicy.INSTANCE_COOLDOWN_MS, firstCooldown)
        assertEquals(PipedRescuePolicy.INSTANCE_COOLDOWN_MS * 2, secondCooldown)
        assertEquals(6, exchange.calls.get())
    }

    @Test
    fun successResetsEscalatedCooldown() = runBlocking {
        var nowMs = 10_000L
        var healthy = false
        val exchange = RecordingExchange { request ->
            when {
                !healthy -> response(500)
                request.kind == PipedRequestKind.API -> response(200, proxyBody("https://proxy-a.example"))
                else -> response(206, contentType = "audio/mp4")
            }
        }
        val client = PipedRescueClient(exchange, listOf(instanceA), clockMs = { nowMs })
        for (video in listOf(VIDEO_ID, SECOND_VIDEO_ID, THIRD_VIDEO_ID)) {
            nowMs = maxOf(nowMs, client.instanceSnapshot("a").blockedUntilMs)
            assertNull(client.resolve(video, "auto"))
        }
        nowMs = client.instanceSnapshot("a").blockedUntilMs
        healthy = true

        assertEquals("a", client.resolve(FOURTH_VIDEO_ID, "auto")?.instanceId)
        healthy = false
        assertNull(client.resolve("aaaaaaaaaaa", "auto"))
        assertNull(client.resolve("bbbbbbbbbbb", "auto"))

        assertEquals(PipedRescuePolicy.INSTANCE_COOLDOWN_MS, client.instanceSnapshot("a").blockedUntilMs - nowMs)
    }

    @Test
    fun invalidJsonAndEmptyAudioAreRejected() = runBlocking {
        val invalidClient = PipedRescueClient(
            RecordingExchange { response(200, "{not-json") },
            listOf(instanceA)
        )
        val emptyClient = PipedRescueClient(
            RecordingExchange { response(200, JSONObject().put("audioStreams", JSONArray()).toString()) },
            listOf(instanceA)
        )

        assertNull(invalidClient.resolve(VIDEO_ID, "auto"))
        assertNull(emptyClient.resolve(VIDEO_ID, "auto"))
        assertEquals(1, invalidClient.instanceSnapshot("a").failures)
        assertEquals(1, emptyClient.instanceSnapshot("a").failures)
    }

    @Test
    fun proxyStreamWinsAndSignedUrlIsPreservedExactly() = runBlocking {
        val signed = "https://proxy-a.example/videoplayback?expire=2000000000&n=a%2Bb%3D&range=0-"
        val direct = "https://rr1.googlevideo.com/videoplayback?expire=2000000000&n=direct"
        val body = JSONObject()
            .put("proxyUrl", "https://proxy-a.example")
            .put(
                "audioStreams",
                JSONArray()
                    .put(stream(direct, "audio/mp4", "mp4a.40.2", 256_000))
                    .put(stream(signed, "audio/mp4", "mp4a.40.2", 128_000))
            )
            .toString()
        val exchange = RecordingExchange { request ->
            if (request.kind == PipedRequestKind.API) response(200, body) else response(206, contentType = "audio/mp4")
        }
        val client = PipedRescueClient(exchange, listOf(instanceA))

        val resolved = client.resolve(VIDEO_ID, "high")

        assertEquals(signed, resolved?.url)
        assertTrue(resolved?.proxied == true)
    }

    @Test
    fun selectsHighestBitrateCompatibleAacStream() = runBlocking {
        val low = "https://proxy-a.example/videoplayback?n=low"
        val high = "https://proxy-a.example/videoplayback?n=high"
        val opus = "https://proxy-a.example/videoplayback?n=opus"
        val body = JSONObject()
            .put("proxyUrl", "https://proxy-a.example")
            .put(
                "audioStreams",
                JSONArray()
                    .put(stream(low, "audio/mp4", "mp4a.40.2", 96_000))
                    .put(stream(opus, "audio/webm", "opus", 192_000))
                    .put(stream(high, "audio/mp4", "mp4a.40.2", 160_000))
            )
            .toString()
        val exchange = RecordingExchange { request ->
            if (request.kind == PipedRequestKind.API) response(200, body) else response(206, contentType = "audio/mp4")
        }

        val resolved = PipedRescueClient(exchange, listOf(instanceA)).resolve(VIDEO_ID, "high")

        assertEquals(high, resolved?.url)
        assertEquals("mp4a.40.2", resolved?.codec)
    }

    @Test
    fun proxy403PenalizesSpecificStreamPath() = runBlocking {
        val exchange = RecordingExchange { request ->
            if (request.kind == PipedRequestKind.API) {
                response(200, proxyBody("https://proxy-a.example"))
            } else {
                response(403)
            }
        }
        val client = PipedRescueClient(exchange, listOf(instanceA))

        assertNull(client.resolve(VIDEO_ID, "auto"))

        assertEquals(1, client.instanceSnapshot("a").failures)
        assertTrue(client.negativeCacheSize() > 0)
    }

    @Test
    fun negativeCacheRemainsBounded() = runBlocking {
        val exchange = RecordingExchange { response(404) }
        val client = PipedRescueClient(exchange, listOf(instanceA))

        repeat(PipedRescuePolicy.MAX_NEGATIVE_CACHE_ENTRIES + 40) { index ->
            client.resolve(index.toString().padStart(11, '0'), "auto")
        }

        assertEquals(PipedRescuePolicy.MAX_NEGATIVE_CACHE_ENTRIES, client.negativeCacheSize())
    }

    @Test
    fun cancellationCancelsInFlightRescue() = runBlocking {
        val cancelled = AtomicBoolean(false)
        val exchange = PipedHttpExchange {
            suspendCancellableCoroutine { continuation ->
                continuation.invokeOnCancellation { cancelled.set(true) }
            }
        }
        val client = PipedRescueClient(exchange, listOf(instanceA))
        val job = launch { client.resolve(VIDEO_ID, "auto") }
        yield()

        job.cancelAndJoin()

        assertTrue(cancelled.get())
    }

    @Test
    fun trackChangeCancelsOldRescueWithoutPoisoningNextTrack() = runBlocking {
        val firstCancelled = AtomicBoolean(false)
        val exchange = RecordingExchange { request ->
            if (request.url.endsWith("/$VIDEO_ID")) {
                suspendCancellableCoroutine { continuation ->
                    continuation.invokeOnCancellation { firstCancelled.set(true) }
                }
            } else if (request.kind == PipedRequestKind.API) {
                response(200, proxyBody("https://proxy-a.example"))
            } else {
                response(206, contentType = "audio/mp4")
            }
        }
        val client = PipedRescueClient(exchange, listOf(instanceA))
        val first = launch { client.resolve(VIDEO_ID, "auto") }
        yield()
        first.cancelAndJoin()

        val next = client.resolve(SECOND_VIDEO_ID, "auto")

        assertTrue(firstCancelled.get())
        assertEquals("a", next?.instanceId)
    }

    private class RecordingExchange(
        private val handler: suspend (PipedHttpRequest) -> PipedHttpResponse
    ) : PipedHttpExchange {
        val calls = AtomicInteger(0)

        override suspend fun execute(request: PipedHttpRequest): PipedHttpResponse {
            calls.incrementAndGet()
            return handler(request)
        }
    }

    private fun response(
        status: Int,
        body: String = "",
        contentType: String = "application/json"
    ) = PipedHttpResponse(status, body, contentType, latencyMs = 25L)

    private fun proxyBody(proxyUrl: String): String = JSONObject()
        .put("proxyUrl", proxyUrl)
        .put(
            "audioStreams",
            JSONArray().put(
                stream(
                    "$proxyUrl/videoplayback?expire=2000000000&n=signed",
                    "audio/mp4",
                    "mp4a.40.2",
                    128_000
                )
            )
        )
        .toString()

    private fun stream(url: String, mime: String, codec: String, bitrate: Int): JSONObject = JSONObject()
        .put("url", url)
        .put("mimeType", mime)
        .put("codec", codec)
        .put("bitrate", bitrate)
        .put("videoOnly", false)

    private companion object {
        const val VIDEO_ID = "dQw4w9WgXcQ"
        const val SECOND_VIDEO_ID = "kJQP7kiw5Fk"
        const val THIRD_VIDEO_ID = "M7lc1UVf-VE"
        const val FOURTH_VIDEO_ID = "9bZkp7q19f0"
    }
}
