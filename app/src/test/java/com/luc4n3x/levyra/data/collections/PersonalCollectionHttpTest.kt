package com.luc4n3x.levyra.data.collections

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class PersonalCollectionHttpTest {
    private data class Seen(val path: String, val authorization: String?, val referer: String?, val cookie: String?, val userAgent: String?)

    private val servers = mutableListOf<HttpServer>()
    private val seen = ConcurrentLinkedQueue<Seen>()
    private val permissive = CollectionAccessInterceptor { CollectionAddressClass.Private }
    private val client = PersonalCollectionHttp.newClient(permissive, readTimeoutMs = 1_000L, callTimeoutMs = 5_000L)
    private val openRule = CollectionAccessRule(allowCleartext = true, allowLocalNetwork = true)

    @Before
    fun setUp() {
        PersonalCollectionNetworkPolicy.replace(emptyMap())
    }

    @After
    fun tearDown() {
        servers.forEach { it.stop(0) }
        PersonalCollectionNetworkPolicy.replace(emptyMap())
    }

    private fun server(handler: (HttpExchange) -> Unit): String {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.executor = Executors.newCachedThreadPool()
        server.createContext("/") { exchange ->
            seen += Seen(
                exchange.requestURI.path,
                exchange.requestHeaders.getFirst("Authorization"),
                exchange.requestHeaders.getFirst("Referer"),
                exchange.requestHeaders.getFirst("Cookie"),
                exchange.requestHeaders.getFirst("User-Agent")
            )
            try {
                handler(exchange)
            } finally {
                exchange.close()
            }
        }
        server.start()
        servers += server
        return "http://127.0.0.1:${server.address.port}"
    }

    private fun HttpExchange.respond(code: Int, body: String = "", type: String = "audio/x-mpegurl", headers: Map<String, String> = emptyMap()) {
        responseHeaders.add("Content-Type", type)
        headers.forEach { (name, value) -> responseHeaders.add(name, value) }
        val bytes = body.toByteArray()
        sendResponseHeaders(code, if (bytes.isEmpty()) -1 else bytes.size.toLong())
        if (bytes.isNotEmpty()) responseBody.use { it.write(bytes) }
    }

    @Test
    fun fetchReturnsBodyAndValidators() = runBlocking {
        val base = server { it.respond(200, "#EXTM3U\na.mp3", headers = mapOf("ETag" to "W/\"v1\"", "Last-Modified" to "Mon, 01 Jan 2024 00:00:00 GMT")) }
        val result = PersonalCollectionHttp.fetchPlaylist("$base/list.m3u", openRule, client = client)
        result as PersonalCollectionHttp.PlaylistFetch.Fetched
        assertArrayEquals("#EXTM3U\na.mp3".toByteArray(), result.bytes)
        assertEquals("W/\"v1\"", result.etag)
        assertEquals("Mon, 01 Jan 2024 00:00:00 GMT", result.lastModified)
    }

    @Test
    fun conditionalRequestMapsNotModified() = runBlocking {
        val base = server { exchange ->
            if (exchange.requestHeaders.getFirst("If-None-Match") == "\"v1\"") exchange.respond(304) else exchange.respond(200, "a.mp3")
        }
        val result = PersonalCollectionHttp.fetchPlaylist("$base/list.m3u", openRule, etag = "\"v1\"", client = client)
        assertEquals(PersonalCollectionHttp.PlaylistFetch.NotModified, result)
    }

    @Test
    fun httpErrorsAndWebPagesBecomeTypedFailures() = runBlocking {
        val base = server { exchange ->
            when (exchange.requestURI.path) {
                "/missing" -> exchange.respond(404)
                "/login" -> exchange.respond(200, "<html>login</html>", type = "text/html; charset=utf-8")
                else -> exchange.respond(401)
            }
        }
        val missing = runCatching { PersonalCollectionHttp.fetchPlaylist("$base/missing", openRule, client = client) }.exceptionOrNull()
        assertEquals(404, (missing as CollectionHttpException).code)
        assertEquals(CollectionSyncError.NotFound, CollectionSyncError.from(missing))
        val login = runCatching { PersonalCollectionHttp.fetchPlaylist("$base/login", openRule, client = client) }.exceptionOrNull()
        assertTrue(login is CollectionNotPlaylistException)
        val denied = runCatching { PersonalCollectionHttp.fetchPlaylist("$base/secret", openRule, client = client) }.exceptionOrNull()
        assertEquals(CollectionSyncError.Unauthorized, CollectionSyncError.from(denied!!))
    }

    @Test
    fun oversizedPlaylistIsRejectedWithoutBufferingItAll() = runBlocking {
        val size = PersonalCollectionParser.MAX_PLAYLIST_BYTES + 1024
        val base = server { exchange ->
            exchange.responseHeaders.add("Content-Type", "text/plain")
            exchange.sendResponseHeaders(200, 0)
            exchange.responseBody.use { output ->
                val chunk = ByteArray(64 * 1024) { 'a'.code.toByte() }
                var written = 0
                while (written < size) {
                    output.write(chunk)
                    written += chunk.size
                }
            }
        }
        val error = runCatching { PersonalCollectionHttp.fetchPlaylist("$base/huge", openRule, client = client) }.exceptionOrNull()
        assertTrue("$error", error is CollectionPayloadTooLargeException)
    }

    @Test
    fun credentialsAreSentOnlyToTheirOriginAndDroppedAfterCrossOriginRedirect() = runBlocking {
        val other = server { it.respond(200, "b.mp3") }
        val origin = server { exchange ->
            if (exchange.requestURI.path == "/start") {
                exchange.respond(302, headers = mapOf("Location" to "$other/elsewhere.m3u"))
            } else {
                exchange.respond(200, "a.mp3")
            }
        }
        val originKey = PersonalCollectionUrlPolicy.origin("$origin/start")!!
        val rule = openRule.copy(credentials = mapOf(originKey to CollectionCredential(originKey, "alice", "pw")))
        val result = PersonalCollectionHttp.fetchPlaylist("$origin/start", rule, client = client)
        assertTrue(result is PersonalCollectionHttp.PlaylistFetch.Fetched)
        val start = seen.first { it.path == "/start" }
        val redirected = seen.first { it.path == "/elsewhere.m3u" }
        assertTrue(start.authorization.orEmpty().startsWith("Basic "))
        assertNull(redirected.authorization)
    }

    @Test
    fun refererCookieAndForeignAuthorizationNeverLeave() {
        val base = server { it.respond(200, "ok") }
        val request = Request.Builder()
            .url("$base/art.jpg")
            .header("Referer", "https://music.example.com/private/list.m3u")
            .header("Cookie", "session=1")
            .header("Authorization", "Bearer leaked")
            .tag(CollectionAccessRule::class.java, openRule)
            .build()
        client.newCall(request).execute().close()
        val received = seen.single()
        assertNull(received.referer)
        assertNull(received.cookie)
        assertNull(received.authorization)
        assertTrue(received.userAgent.orEmpty().startsWith("Levyra/"))
    }

    @Test
    fun cleartextIsRefusedWithoutConsent() = runBlocking {
        val base = server { it.respond(200, "a.mp3") }
        val error = runCatching {
            PersonalCollectionHttp.fetchPlaylist("$base/list.m3u", openRule.copy(allowCleartext = false), client = client)
        }.exceptionOrNull()
        assertEquals(CollectionAccessDenial.Cleartext, (error as CollectionAccessDeniedException).reason)
        assertTrue("request must not reach the server", seen.isEmpty())
    }

    @Test
    fun localNetworkIsRefusedUnlessTheCollectionAllowsIt() = runBlocking {
        val base = server { it.respond(200, "a.mp3") }
        val error = runCatching {
            PersonalCollectionHttp.fetchPlaylist("$base/list.m3u", openRule.copy(allowLocalNetwork = false), client = client)
        }.exceptionOrNull()
        assertEquals(CollectionAccessDenial.LocalNetwork, (error as CollectionAccessDeniedException).reason)
        assertTrue(seen.isEmpty())
    }

    @Test
    fun loopbackIsAlwaysBlockedWithTheRealClassifier() = runBlocking {
        val base = server { it.respond(200, "a.mp3") }
        val strict = PersonalCollectionHttp.newClient(readTimeoutMs = 1_000L, callTimeoutMs = 5_000L)
        val error = runCatching { PersonalCollectionHttp.fetchPlaylist("$base/list.m3u", openRule, client = strict) }.exceptionOrNull()
        assertEquals(CollectionAccessDenial.BlockedAddress, (error as CollectionAccessDeniedException).reason)
        assertTrue(seen.isEmpty())
    }

    @Test
    fun playbackRequestsUseOnlyTheirOwnCollectionRule() {
        val base = server { it.respond(200, "audio") }
        fun request(collectionId: String?) = Request.Builder().url("$base/a.mp3").apply {
            if (collectionId != null) header(PersonalCollectionNetworkPolicy.COLLECTION_HEADER, collectionId)
        }.build()
        val untagged = runCatching { client.newCall(request(null)).execute() }.exceptionOrNull()
        assertEquals(CollectionAccessDenial.UnknownHost, (untagged as CollectionAccessDeniedException).reason)
        PersonalCollectionNetworkPolicy.replace(
            mapOf(
                "consented" to openRule,
                "strict" to openRule.copy(allowCleartext = false)
            )
        )
        val strict = runCatching { client.newCall(request("strict")).execute() }.exceptionOrNull()
        assertEquals(CollectionAccessDenial.Cleartext, (strict as CollectionAccessDeniedException).reason)
        val unknown = runCatching { client.newCall(request("deleted")).execute() }.exceptionOrNull()
        assertEquals(CollectionAccessDenial.UnknownHost, (unknown as CollectionAccessDeniedException).reason)
        client.newCall(request("consented")).execute().use { assertEquals(200, it.code) }
        assertEquals(1, seen.size)
    }

    @Test
    fun internalCollectionHeaderNeverReachesTheServer() {
        var leaked: String? = "unset"
        val base = server { exchange ->
            leaked = exchange.requestHeaders.getFirst(PersonalCollectionNetworkPolicy.COLLECTION_HEADER)
            exchange.respond(200, "ok")
        }
        PersonalCollectionNetworkPolicy.replace(mapOf("c1" to openRule))
        client.newCall(
            Request.Builder().url("$base/a.mp3").header(PersonalCollectionNetworkPolicy.COLLECTION_HEADER, "c1").build()
        ).execute().close()
        assertNull(leaked)
    }

    @Test
    fun slowServerTimesOutAndIsClassified() = runBlocking {
        val base = server { exchange ->
            Thread.sleep(2_500)
            exchange.respond(200, "late")
        }
        val error = runCatching { PersonalCollectionHttp.fetchPlaylist("$base/slow", openRule, client = client) }.exceptionOrNull()
        assertTrue("$error", error is SocketTimeoutException || error is java.io.InterruptedIOException)
        assertEquals(CollectionSyncError.Timeout, CollectionSyncError.from(error!!))
    }

    @Test
    fun coroutineCancellationCancelsTheCall() = runBlocking {
        val base = server { exchange ->
            runCatching {
                Thread.sleep(4_000)
                exchange.respond(200, "late")
            }
        }
        val slowClient = PersonalCollectionHttp.newClient(permissive, readTimeoutMs = 10_000L, callTimeoutMs = 10_000L)
        val job = async { PersonalCollectionHttp.fetchPlaylist("$base/slow", openRule, client = slowClient) }
        delay(300)
        val started = System.nanoTime()
        job.cancel()
        runCatching { withTimeout(2_000) { job.await() } }
        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        assertTrue(job.isCancelled)
        assertTrue("cancellation took ${elapsedMs}ms", elapsedMs < 1_500)
        assertEquals(0, slowClient.dispatcher.runningCallsCount())
    }

    @Test
    fun artworkIsBoundedAndMustBeAnImage() = runBlocking {
        val png = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 0x0D, 0x0A, 0x1A, 0x0A) + ByteArray(64)
        val base = server { exchange ->
            when (exchange.requestURI.path) {
                "/ok.png" -> {
                    exchange.responseHeaders.add("Content-Type", "image/png")
                    exchange.sendResponseHeaders(200, png.size.toLong())
                    exchange.responseBody.use { it.write(png) }
                }
                "/page" -> exchange.respond(200, "<html></html>", type = "text/html")
                else -> exchange.respond(200, "not an image", type = "image/png")
            }
        }
        assertArrayEquals(png, PersonalCollectionHttp.fetchArtwork("$base/ok.png", openRule, client))
        assertNull(PersonalCollectionHttp.fetchArtwork("$base/page", openRule, client))
        assertNull(PersonalCollectionHttp.fetchArtwork("$base/fake.png", openRule, client))
    }

    @Test
    fun failureClassificationCoversNetworkShapes() {
        assertEquals(CollectionSyncError.Offline, CollectionSyncError.from(java.net.UnknownHostException("x")))
        assertEquals(CollectionSyncError.Offline, CollectionSyncError.from(java.net.ConnectException("x")))
        assertEquals(CollectionSyncError.Tls, CollectionSyncError.from(javax.net.ssl.SSLHandshakeException("x")))
        assertEquals(CollectionSyncError.Server, CollectionSyncError.from(CollectionHttpException(503)))
        assertEquals(CollectionSyncError.Unknown, CollectionSyncError.from(IllegalStateException("x")))
        try {
            CollectionSyncError.from(CollectionAccessDeniedException(CollectionAccessDenial.UnknownHost)).also {
                assertEquals(CollectionSyncError.BlockedAddress, it)
            }
        } catch (error: Throwable) {
            fail(error.toString())
        }
    }
}
