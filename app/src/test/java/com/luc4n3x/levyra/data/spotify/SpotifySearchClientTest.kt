package com.luc4n3x.levyra.data.spotify

import com.luc4n3x.levyra.domain.Track
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger

class SpotifySearchClientTest {
    private fun cachedTrack() = Track(
        id = "abcdefghijk", title = "Cached song", artist = "Artist", album = "Album", durationMs = 120_000L,
        streamUrl = "", videoUrl = "https://www.youtube.com/watch?v=abcdefghijk",
        thumbnailUrl = "", largeThumbnailUrl = "", source = "YouTube Music", moodTags = emptySet(),
        energy = 0, vocal = 0, replayScore = 0, cacheScore = 0, accentStart = 0, accentEnd = 0
    )

    private val response = """{"data":{"searchV2":{
        "tracksV2":{"items":[{"item":{"data":{"id":"publicRecording","name":"Full catalog song",
        "artists":{"items":[{"profile":{"name":"Artist"}}]},"duration":{"totalMilliseconds":150000}}}}]},
        "artists":{"items":[{"data":{"profile":{"name":"Artist"},"visuals":{"avatarImage":{"sources":[
        {"url":"https://i.scdn.co/image/ab676161small","width":64,"height":64},
        {"url":"https://i.scdn.co/image/ab676161large","width":640,"height":640}]}}}}]},
        "playlists":{"items":[{"data":{"name":"Private account playlist","uri":"private:playlist"}}]}
    }}}"""

    @Test
    fun fullCatalogRunsEvenWhenCachedResultsExistAndUsesOnlyGatewayAuthentication() = runBlocking {
        val cached = cachedTrack()
        val requests = AtomicInteger()
        withServer { server, http ->
            server.createContext("/base/v1/catalog/search") { exchange ->
                requests.incrementAndGet()
                assertEquals("POST", exchange.requestMethod)
                assertEquals("synthetic-gateway-key", exchange.requestHeaders.getFirst("X-Levyra-Key"))
                assertEquals(null, exchange.requestHeaders.getFirst("Authorization"))
                assertEquals(null, exchange.requestHeaders.getFirst("Cookie"))
                val body = JSONObject(exchange.requestBody.bufferedReader().readText())
                assertEquals(setOf("query", "limit", "country"), body.keys().asSequence().toSet())
                assertEquals("artist song", body.getString("query"))
                assertEquals(25, body.getInt("limit"))
                assertEquals("IT", body.getString("country"))
                val bytes = response.toByteArray()
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            val client = client(server, http, catalog = { _, _ -> listOf(cached) }, country = { "it" })
            assertSame(cached, client.cachedSearch("artist song").songs.single())
            assertEquals(0, requests.get())
            val result = client.search(" artist song ", 100)
            assertEquals(1, requests.get())
            assertEquals("spotify:publicRecording", result.songs.first().id)
            assertEquals("", result.songs.first().streamUrl)
            assertSame(cached, result.songs.last())
            assertEquals("https://i.scdn.co/image/ab676161large", result.artists.single().thumbnailUrl)
            assertTrue(result.playlists.isEmpty())
        }
    }

    @Test
    fun ambiguousArtistNamesAreRejectedWhileUniqueArtistsRemain() = runBlocking {
        val homonyms = listOf(
            artistResponse("Common Name", "spotify:artist:0000000000000000000001", "first"),
            artistResponse("COMMON NAME", "spotify:artist:0000000000000000000002", "second")
        )
        listOf(homonyms, homonyms + artistResponse("Unique Artist", "spotify:artist:0000000000000000000003", "unique"))
            .forEachIndexed { index, items ->
                withServer { server, http ->
                    server.createContext("/base/v1/catalog/search") { exchange ->
                        val payload = JSONObject().put("data", JSONObject().put("searchV2", JSONObject()
                            .put("artists", JSONObject().put("items", org.json.JSONArray(items)))))
                        val bytes = payload.toString().toByteArray()
                        exchange.sendResponseHeaders(200, bytes.size.toLong())
                        exchange.responseBody.use { it.write(bytes) }
                    }
                    val result = client(server, http).search("common name")
                    assertEquals(if (index == 0) emptyList<String>() else listOf("Unique Artist"), result.artists.map { it.name })
                }
            }
    }

    @Test
    fun missingArtistUrisRequireAnUnambiguousImageIdentity() = runBlocking {
        withServer { server, http ->
            server.createContext("/base/v1/catalog/search") { exchange ->
                val items = listOf(artistResponse("Common Name", "", "first"), artistResponse("Common Name", "", "second"),
                    artistResponse("Unique Artist", "", "unique"), artistResponse("Unique Artist", "", "unique"))
                val payload = JSONObject().put("data", JSONObject().put("searchV2", JSONObject()
                    .put("artists", JSONObject().put("items", org.json.JSONArray(items)))))
                val bytes = payload.toString().toByteArray()
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            assertEquals(listOf("Unique Artist"), client(server, http).search("common name").artists.map { it.name })
        }
    }

    private fun artistResponse(name: String, uri: String, image: String): JSONObject = JSONObject().put("data", JSONObject()
        .put("uri", uri).put("profile", JSONObject().put("name", name))
        .put("visuals", JSONObject().put("avatarImage", JSONObject().put("sources", org.json.JSONArray()
            .put(JSONObject().put("url", "https://i.scdn.co/image/ab676161$image").put("width", 640).put("height", 640))))))

    @Test
    fun disabledOrUnsafeConfigurationNeverCreatesNetworkClient() = runBlocking {
        val cached = cachedTrack()
        listOf("", "http://example.test/v1/resolve", "https://user:password@example.test/v1/resolve",
            "https://example.test/v1/resolve?token=test", "https://example.test/v1/resolve#fragment",
            "https://example.test/other").forEach { url ->
            val client = SpotifySearchClient(
                clientFactory = { error("Network must remain disabled") }, catalogSearch = { _, _ -> listOf(cached) },
                resolverUrl = url, clientKey = "synthetic-gateway-key"
            )
            assertSame(cached, client.search("query").songs.single())
        }
        val client = SpotifySearchClient(clientFactory = { error("Disabled gateway key") },
            catalogSearch = { _, _ -> listOf(cached) }, resolverUrl = "https://example.test/v1/resolve", clientKey = "")
        assertSame(cached, client.search("query").songs.single())
    }

    @Test
    fun rateLimitFallsBackImmediatelyAndSuppressesAdditionalRequests() = runBlocking {
        val cached = cachedTrack()
        val requests = AtomicInteger()
        withServer { server, http ->
            server.createContext("/base/v1/catalog/search") { exchange ->
                requests.incrementAndGet()
                exchange.responseHeaders.add("Retry-After", "60")
                exchange.sendResponseHeaders(429, -1)
                exchange.close()
            }
            val client = client(server, http, catalog = { _, _ -> listOf(cached) })
            assertSame(cached, client.search("query").songs.single())
            assertSame(cached, client.search("other query").songs.single())
            assertEquals(1, requests.get())
        }
    }

    @Test
    fun rejectsRedirectAndFallsBackWithoutForwardingGatewayKey() = runBlocking {
        val requests = AtomicInteger()
        withServer { server, http ->
            server.createContext("/base/v1/catalog/search") { exchange ->
                exchange.responseHeaders.add("Location", "/redirected")
                exchange.sendResponseHeaders(302, -1)
                exchange.close()
            }
            server.createContext("/redirected") { exchange ->
                requests.incrementAndGet()
                exchange.sendResponseHeaders(200, -1)
                exchange.close()
            }
            val client = client(server, http)
            assertTrue(client.search("query").isEmpty)
            assertEquals(0, requests.get())
        }
    }

    @Test
    fun malformedResponseUsesCacheAndInvalidCountryUsesSafeDefault() = runBlocking {
        val cached = cachedTrack()
        withServer { server, http ->
            server.createContext("/base/v1/catalog/search") { exchange ->
                val body = JSONObject(exchange.requestBody.bufferedReader().readText())
                assertEquals("US", body.getString("country"))
                val bytes = "invalid json".toByteArray()
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            val client = client(server, http, catalog = { _, _ -> listOf(cached) }, country = { "ZZ" })
            assertSame(cached, client.search("query").songs.single())
        }
    }

    @Test
    fun unresolvedEditorialTracksRemainResolvableWithoutChangingMappedYoutubeIdentity() = runBlocking {
        val unresolved = cachedTrack().copy(id = "levyra-recording", videoUrl = "")
        val mapped = cachedTrack().copy(id = "levyra-mapped", audioVideoId = "abcdefghijk")
        val client = SpotifySearchClient(catalogSearch = { _, _ -> listOf(unresolved, mapped) },
            resolverUrl = "", clientKey = "")
        val result = client.cachedSearch("query")
        assertEquals("spotify:levyra-recording", result.songs.first().id)
        assertSame(mapped, result.songs.last())
    }

    @Test
    fun cancellationFromCacheIsPropagated() = runBlocking {
        val client = SpotifySearchClient(catalogSearch = { _, _ -> throw CancellationException("cancelled") },
            resolverUrl = "", clientKey = "")
        var cancelled = false
        try {
            client.cachedSearch("query")
        } catch (_: CancellationException) {
            cancelled = true
        }
        assertTrue(cancelled)
    }

    private fun client(server: HttpServer, http: OkHttpClient,
        catalog: suspend (String, Int) -> List<Track> = { _, _ -> emptyList() }, country: () -> String = { "US" }
    ) = SpotifySearchClient(clientFactory = { http }, catalogSearch = catalog, countryProvider = country,
        resolverUrl = "http://127.0.0.1:${server.address.port}/base/v1/resolve", clientKey = "synthetic-gateway-key",
        allowInsecureForTests = true)

    private suspend fun withServer(block: suspend (HttpServer, OkHttpClient) -> Unit) {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val http = OkHttpClient()
        server.start()
        try {
            block(server, http)
        } finally {
            server.stop(0)
            http.dispatcher.executorService.shutdownNow()
            http.connectionPool.evictAll()
        }
    }
}
