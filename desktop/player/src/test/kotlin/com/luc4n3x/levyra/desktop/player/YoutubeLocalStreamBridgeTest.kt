package com.luc4n3x.levyra.desktop.player

import com.sun.net.httpserver.HttpServer
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class YoutubeLocalStreamBridgeTest {
    private val closeables = ArrayList<AutoCloseable>()
    private val tokenCounter = AtomicInteger()

    @After
    fun tearDown() {
        closeables.asReversed().forEach { it.close() }
    }

    @Test
    fun openEndedRangeIsStreamedThroughBoundedUpstreamRanges() {
        val media = ByteArray(2_500) { (it % 251).toByte() }
        val upstream = BoundedRangeUpstream(media).also(closeables::add)
        val bridge = YoutubeLocalStreamBridge(
            upstreamClient = OkHttpClient(),
            upstreamUrlValidator = { true },
            upstreamAddressValidator = { true },
            tokenGenerator = { "test-token" },
            upstreamChunkBytes = 1_024L
        ).also(closeables::add)
        val localUrl = bridge.openSession(upstream.url)

        val response = OkHttpClient().newCall(
            Request.Builder()
                .url(localUrl)
                .header("Range", "bytes=0-")
                .build()
        ).execute()

        response.use {
            assertEquals(206, it.code)
            assertEquals("bytes 0-2499/2500", it.header("Content-Range"))
            assertEquals("2500", it.header("Content-Length"))
            assertEquals("bytes", it.header("Accept-Ranges"))
            assertEquals("audio/webm", it.header("Content-Type"))
            assertArrayEquals(media, it.body.bytes())
        }
        assertEquals(
            listOf("bytes=0-1023", "bytes=1024-2047", "bytes=2048-2499"),
            upstream.ranges.toList()
        )
        assertEquals("127.0.0.1", bridge.bindAddress.hostAddress)
        assertTrue(bridge.port > 0)
        assertTrue(bridge.didOpenUpstream(localUrl))
    }

    @Test
    fun seekRangeStartsAtRequestedOffsetAndRemainsBoundedUpstream() {
        val media = ByteArray(2_500) { (it % 251).toByte() }
        val upstream = BoundedRangeUpstream(media).also(closeables::add)
        val bridge = bridge(chunkBytes = 1_024L).also(closeables::add)
        val localUrl = bridge.openSession(upstream.url)

        val response = OkHttpClient().newCall(
            Request.Builder()
                .url(localUrl)
                .header("Range", "bytes=1000-")
                .build()
        ).execute()

        response.use {
            assertEquals(206, it.code)
            assertEquals("bytes 1000-2499/2500", it.header("Content-Range"))
            assertEquals("1500", it.header("Content-Length"))
            assertArrayEquals(media.copyOfRange(1_000, media.size), it.body.bytes())
        }
        assertEquals(
            listOf("bytes=1000-2023", "bytes=2024-2499"),
            upstream.ranges.toList()
        )
    }

    @Test
    fun upstream200IsForwardedForUnrangedRequest() {
        val media = ByteArray(4_096) { (it % 199).toByte() }
        val upstream = FullBodyUpstream(media).also(closeables::add)
        val bridge = bridge(chunkBytes = 8_192L).also(closeables::add)
        val localUrl = bridge.openSession(upstream.url)

        OkHttpClient().newCall(Request.Builder().url(localUrl).build()).execute().use {
            assertEquals(200, it.code)
            assertEquals("4096", it.header("Content-Length"))
            assertEquals("audio/mp4", it.header("Content-Type"))
            assertArrayEquals(media, it.body.bytes())
        }
    }

    @Test
    fun upstreamFailuresAreForwardedAndClassified() {
        val expected = mapOf(403 to "HTTP_403", 410 to "HTTP_410", 416 to "HTTP_416", 429 to "HTTP_429")
        expected.forEach { (status, reason) ->
            val upstream = StatusUpstream(status).also(closeables::add)
            val bridge = bridge(chunkBytes = 1_024L).also(closeables::add)
            val localUrl = bridge.openSession(upstream.url)

            OkHttpClient().newCall(
                Request.Builder().url(localUrl).header("Range", "bytes=0-").build()
            ).execute().use {
                assertEquals(status, it.code)
                if (status == 416) assertEquals("bytes */4096", it.header("Content-Range"))
            }
            assertEquals(reason, bridge.failureReason(localUrl))
        }
    }

    @Test
    fun expired403IsClassifiedSeparately() {
        val upstream = StatusUpstream(403).also(closeables::add)
        val bridge = bridge(chunkBytes = 1_024L).also(closeables::add)
        val localUrl = bridge.openSession("${upstream.url}?expire=1")

        OkHttpClient().newCall(Request.Builder().url(localUrl).build()).execute().use {
            assertEquals(403, it.code)
        }
        assertEquals("STREAM_EXPIRED", bridge.failureReason(localUrl))
    }

    @Test
    fun parallelSessionsDoNotReplaceEachOther() {
        val firstMedia = ByteArray(2_048) { 11 }
        val secondMedia = ByteArray(3_072) { 22 }
        val firstUpstream = BoundedRangeUpstream(firstMedia).also(closeables::add)
        val secondUpstream = BoundedRangeUpstream(secondMedia).also(closeables::add)
        val bridge = bridge(chunkBytes = 1_024L).also(closeables::add)
        val firstUrl = bridge.openSession(firstUpstream.url)
        val secondUrl = bridge.openSession(secondUpstream.url)
        assertNotEquals(firstUrl, secondUrl)

        val first = CompletableFuture.supplyAsync { readAll(firstUrl) }
        val second = CompletableFuture.supplyAsync { readAll(secondUrl) }

        assertArrayEquals(firstMedia, first.get(5, TimeUnit.SECONDS))
        assertArrayEquals(secondMedia, second.get(5, TimeUnit.SECONDS))
    }

    @Test
    fun closingSessionCancelsInFlightUpstreamCall() {
        val upstream = BlockingUpstream().also(closeables::add)
        val bridge = bridge(chunkBytes = 1_024L).also(closeables::add)
        val localUrl = bridge.openSession(upstream.url)
        val request = CompletableFuture.runAsync {
            runCatching {
                OkHttpClient().newCall(
                    Request.Builder().url(localUrl).header("Range", "bytes=0-").build()
                ).execute().use { it.body.bytes() }
            }
        }
        assertTrue(upstream.started.await(5, TimeUnit.SECONDS))

        bridge.closeSession(localUrl)

        request.get(5, TimeUnit.SECONDS)
        upstream.release.countDown()
    }

    @Test
    fun closeStopsAcceptingNewSessions() {
        val upstream = StatusUpstream(403).also(closeables::add)
        val bridge = bridge(chunkBytes = 1_024L)
        bridge.close()

        assertThrows(IllegalStateException::class.java) {
            bridge.openSession(upstream.url)
        }
    }

    @Test
    fun defaultPolicyRejectsNonYoutubeAndLoopbackTargets() {
        val bridge = YoutubeLocalStreamBridge().also(closeables::add)

        assertThrows(IllegalArgumentException::class.java) {
            bridge.openSession("https://example.com/audio.webm")
        }
        assertThrows(IllegalArgumentException::class.java) {
            bridge.openSession("http://127.0.0.1/audio.webm")
        }
    }

    @Test
    fun dnsPolicyRejectsPrivateResolutionForAllowedYoutubeHost() {
        val bridge = YoutubeLocalStreamBridge(
            upstreamClient = OkHttpClient(),
            upstreamAddressResolver = { listOf(InetAddress.getByName("127.0.0.1")) },
            tokenGenerator = { "private-dns" }
        ).also(closeables::add)
        val localUrl = bridge.openSession("https://r1.googlevideo.com/videoplayback")

        OkHttpClient().newCall(Request.Builder().url(localUrl).build()).execute().use {
            assertEquals(502, it.code)
        }
        assertEquals("BRIDGE_UPSTREAM_FAILED", bridge.failureReason(localUrl))
    }

    @Test
    fun redirectTargetIsValidatedBeforeFollowing() {
        val upstream = RedirectUpstream().also(closeables::add)
        val allowedInitial = upstream.url
        val bridge = YoutubeLocalStreamBridge(
            upstreamClient = OkHttpClient(),
            upstreamUrlValidator = { it.toString() == allowedInitial },
            upstreamAddressValidator = { true },
            tokenGenerator = { "redirect-policy" }
        ).also(closeables::add)
        val localUrl = bridge.openSession(allowedInitial)

        OkHttpClient().newCall(Request.Builder().url(localUrl).build()).execute().use {
            assertEquals(502, it.code)
        }
        assertEquals("BRIDGE_UPSTREAM_FAILED", bridge.failureReason(localUrl))
    }

    private fun readAll(url: String): ByteArray = OkHttpClient().newCall(
        Request.Builder().url(url).header("Range", "bytes=0-").build()
    ).execute().use { response ->
        assertEquals(206, response.code)
        response.body.bytes()
    }

    private fun bridge(chunkBytes: Long): YoutubeLocalStreamBridge = YoutubeLocalStreamBridge(
        upstreamClient = OkHttpClient(),
        upstreamUrlValidator = { true },
        upstreamAddressValidator = { true },
        tokenGenerator = { "test-token-${tokenCounter.incrementAndGet()}" },
        upstreamChunkBytes = chunkBytes
    )

    private class FullBodyUpstream(private val media: ByteArray) : AutoCloseable {
        private val server = loopbackServer { exchange ->
            exchange.responseHeaders.add("Content-Type", "audio/mp4")
            exchange.sendResponseHeaders(200, media.size.toLong())
            exchange.responseBody.use { it.write(media) }
        }
        val url: String = "http://127.0.0.1:${server.address.port}/audio"

        override fun close() {
            server.stop(0)
        }
    }

    private class StatusUpstream(private val status: Int) : AutoCloseable {
        private val server = loopbackServer { exchange ->
            if (status == 416) exchange.responseHeaders.add("Content-Range", "bytes */4096")
            exchange.sendResponseHeaders(status, -1L)
            exchange.close()
        }
        val url: String = "http://127.0.0.1:${server.address.port}/audio"

        override fun close() {
            server.stop(0)
        }
    }

    private class BlockingUpstream : AutoCloseable {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        private val server = loopbackServer { exchange ->
            started.countDown()
            release.await(10, TimeUnit.SECONDS)
            runCatching { exchange.sendResponseHeaders(503, -1L) }
            exchange.close()
        }
        val url: String = "http://127.0.0.1:${server.address.port}/audio"

        override fun close() {
            release.countDown()
            server.stop(0)
        }
    }

    private class RedirectUpstream : AutoCloseable {
        private val server = loopbackServer { exchange ->
            exchange.responseHeaders.add("Location", "https://example.com/private")
            exchange.sendResponseHeaders(302, -1L)
            exchange.close()
        }
        val url: String = "http://127.0.0.1:${server.address.port}/audio"

        override fun close() {
            server.stop(0)
        }
    }

    private class BoundedRangeUpstream(
        private val media: ByteArray
    ) : AutoCloseable {
        val ranges = CopyOnWriteArrayList<String>()
        private val server = HttpServer.create(
            InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0),
            8
        ).apply {
            createContext("/audio") { exchange ->
                val range = exchange.requestHeaders.getFirst("Range").orEmpty()
                ranges += range
                val match = BOUNDED_RANGE.matchEntire(range)
                if (match == null) {
                    exchange.sendResponseHeaders(403, -1L)
                    exchange.close()
                    return@createContext
                }
                val start = match.groupValues[1].toLong()
                val requestedEnd = match.groupValues[2].toLong()
                if (start >= media.size) {
                    exchange.responseHeaders.add("Content-Range", "bytes */${media.size}")
                    exchange.sendResponseHeaders(416, -1L)
                    exchange.close()
                    return@createContext
                }
                val end = requestedEnd.coerceAtMost(media.lastIndex.toLong())
                val length = end - start + 1L
                exchange.responseHeaders.add("Accept-Ranges", "bytes")
                exchange.responseHeaders.add("Content-Type", "audio/webm")
                exchange.responseHeaders.add("Content-Range", "bytes $start-$end/${media.size}")
                exchange.sendResponseHeaders(206, length)
                exchange.responseBody.use { output ->
                    output.write(media, start.toInt(), length.toInt())
                }
            }
            start()
        }

        val url: String = "http://127.0.0.1:${server.address.port}/audio"

        override fun close() {
            server.stop(0)
        }

        private companion object {
            val BOUNDED_RANGE = Regex("bytes=(\\d+)-(\\d+)")
        }
    }

    private companion object {
        fun loopbackServer(handler: com.sun.net.httpserver.HttpHandler): HttpServer = HttpServer.create(
            InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0),
            8
        ).apply {
            createContext("/audio", handler)
            start()
        }
    }
}
