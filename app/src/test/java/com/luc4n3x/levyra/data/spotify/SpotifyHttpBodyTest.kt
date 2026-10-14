package com.luc4n3x.levyra.data.spotify

import okhttp3.MediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.OkHttpClient
import okhttp3.Request
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import okio.Buffer
import okio.BufferedSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SpotifyHttpBodyTest {
    @Test
    fun acceptsBoundedJsonBody() {
        val body = "{\"ok\":true}".toResponseBody(null)

        assertEquals("{\"ok\":true}", body.readSpotifyJsonBody())
    }

    @Test
    fun rejectsOversizedDeclaredBody() {
        val body = "x".repeat((SPOTIFY_MAX_JSON_RESPONSE_BYTES + 1L).toInt()).toResponseBody(null)

        assertNull(body.readSpotifyJsonBody())
    }

    @Test
    fun rejectsOversizedStreamWhenLengthIsUnknown() {
        val payload = Buffer().write(ByteArray((SPOTIFY_MAX_JSON_RESPONSE_BYTES + 1L).toInt()))
        val body = object : ResponseBody() {
            override fun contentType(): MediaType? = null
            override fun contentLength(): Long = -1L
            override fun source(): BufferedSource = payload
        }

        assertNull(body.readSpotifyJsonBody())
    }

    @Test
    fun observesRateLimitResponseWithoutAcceptingItAsMetadata() = runBlocking {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            exchange.responseHeaders.add("Retry-After", "60")
            exchange.sendResponseHeaders(429, -1)
            exchange.close()
        }
        server.start()
        val client = OkHttpClient()
        try {
            var status = 0
            var retryAfter = ""
            val request = Request.Builder().url("http://127.0.0.1:${server.address.port}/").build()
            val result = client.newCall(request).awaitSpotifyJson {
                status = it.code
                retryAfter = it.header("Retry-After").orEmpty()
            }
            assertNull(result)
            assertEquals(429, status)
            assertEquals("60", retryAfter)
        } finally {
            server.stop(0)
            client.dispatcher.executorService.shutdownNow()
            client.connectionPool.evictAll()
        }
    }

    @Test
    fun coroutineCancellationCancelsTheUnderlyingHttpCall() = runBlocking {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            entered.countDown()
            release.await(3, TimeUnit.SECONDS)
            exchange.close()
        }
        server.start()
        val client = OkHttpClient()
        try {
            val request = Request.Builder().url("http://127.0.0.1:${server.address.port}/").build()
            val call = client.newCall(request)
            val pending = async(Dispatchers.IO) { call.awaitSpotifyJson() }
            assertTrue(entered.await(3, TimeUnit.SECONDS))
            pending.cancelAndJoin()
            assertTrue(call.isCanceled())
        } finally {
            release.countDown()
            server.stop(0)
            client.dispatcher.executorService.shutdownNow()
            client.connectionPool.evictAll()
        }
    }
}
