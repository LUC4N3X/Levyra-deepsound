package com.luc4n3x.levyra.feature.motion

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class OnDemandCanvasResolverTest {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun clientWithHandler(handler: (Request) -> Response): OkHttpClient {
        val interceptor = Interceptor { chain ->
            handler(chain.request())
        }
        return OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS)
            .callTimeout(2, TimeUnit.SECONDS)
            .build()
    }

    private fun jsonResponse(request: Request, code: Int, body: String): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code == 200) "OK" else "Error")
            .body(body.toResponseBody(jsonMediaType))
            .build()
    }

    private fun testIdentity(
        isrc: String = "USUM71900764",
        title: String = "Blinding Lights",
        artist: String = "The Weeknd",
        album: String = "After Hours",
        durationMs: Long = 200_000L
    ) = MotionTrackIdentity(
        title = title,
        artists = listOf(artist),
        album = album,
        durationMs = durationMs,
        isrc = isrc,
        upc = "",
        year = "2020",
        trackId = "track_123",
        albumId = "album_456"
    )

    @Test
    fun exactIsrcRequestPayloadAndResponseHandling() {
        runBlocking {
            var interceptedRequest: Request? = null
            var interceptedBody: String? = null

            val client = clientWithHandler { request ->
                interceptedRequest = request
                val buffer = okio.Buffer()
                request.body?.writeTo(buffer)
                interceptedBody = buffer.readUtf8()
                jsonResponse(
                    request,
                    200,
                    """{"status":"resolved","url":"https://canvaz.scdn.co/upload/artist/video/sample.cnvs.mp4"}"""
                )
            }

            val resolver = OnDemandCanvasResolver(
                client = client,
                resolverUrl = "https://canvas.levyra.org/v1/resolve",
                networkPolicyCheck = { true }
            )

            val identity = testIdentity()
            val candidate = resolver.resolve(identity)

            assertNotNull(candidate)
            assertEquals("https://canvaz.scdn.co/upload/artist/video/sample.cnvs.mp4", candidate?.url)
            assertEquals(CommunityCanvasProvider.PROVIDER_ID, candidate?.provider)
            assertEquals(MotionArtworkScope.TRACK, candidate?.scope)
            assertEquals("video/mp4", candidate?.mimeType)

            assertNotNull(interceptedRequest)
            assertEquals("https://canvas.levyra.org/v1/resolve", interceptedRequest?.url.toString())
            assertEquals("POST", interceptedRequest?.method)
            assertEquals("Levyra/1.0 (Android; MotionArtwork)", interceptedRequest?.header("User-Agent"))
            assertEquals("application/json", interceptedRequest?.header("Accept"))

            assertNotNull(interceptedBody)
            val bodyJson = JSONObject(interceptedBody!!)
            assertEquals("USUM71900764", bodyJson.getString("isrc"))
            assertEquals("Blinding Lights", bodyJson.getString("title"))
            assertEquals("The Weeknd", bodyJson.getString("artist"))
            assertEquals("After Hours", bodyJson.getString("album"))
            assertEquals(200_000L, bodyJson.getLong("durationMs"))
            assertFalse(bodyJson.has("sp_dc"))
            assertFalse(bodyJson.has("token"))
            assertFalse(bodyJson.has("deviceId"))
        }
    }

    @Test
    fun strictMetadataRequestWithoutIsrc() {
        runBlocking {
            var interceptedBody: String? = null

            val client = clientWithHandler { request ->
                val buffer = okio.Buffer()
                request.body?.writeTo(buffer)
                interceptedBody = buffer.readUtf8()
                jsonResponse(
                    request,
                    200,
                    """{"status":"resolved","url":"https://canvaz.scdn.co/upload/video/save_your_tears.mp4"}"""
                )
            }

            val resolver = OnDemandCanvasResolver(
                client = client,
                resolverUrl = "https://canvas.levyra.org/v1/resolve",
                networkPolicyCheck = { true }
            )

            val identity = testIdentity(isrc = "", title = "Save Your Tears")
            val candidate = resolver.resolve(identity)

            assertNotNull(candidate)
            assertEquals("https://canvaz.scdn.co/upload/video/save_your_tears.mp4", candidate?.url)

            assertNotNull(interceptedBody)
            val bodyJson = JSONObject(interceptedBody!!)
            assertFalse(bodyJson.has("isrc"))
            assertEquals("Save Your Tears", bodyJson.getString("title"))
            assertEquals("The Weeknd", bodyJson.getString("artist"))
        }
    }

    @Test
    fun metadataCacheKeyIncludesAlbumIdentity() {
        val studio = testIdentity(isrc = "", title = "Song", artist = "Artist", album = "Studio Album", durationMs = 200_000L)
        val live = testIdentity(isrc = "", title = "Song", artist = "Artist", album = "Live in Concert", durationMs = 200_000L)
        val keyStudio = onDemandCacheKey(studio)
        val keyLive = onDemandCacheKey(live)
        assertFalse(keyStudio == keyLive)
        assertTrue(keyStudio.contains("studio album"))
        assertTrue(keyLive.contains("live in concert"))
    }

    @Test
    fun invalidCanvasHostRejected() {
        runBlocking {
            val client = clientWithHandler { request ->
                jsonResponse(
                    request,
                    200,
                    """{"status":"resolved","url":"https://malicious.example.com/video.mp4"}"""
                )
            }

            val resolver = OnDemandCanvasResolver(
                client = client,
                networkPolicyCheck = { true }
            )

            val candidate = resolver.resolve(testIdentity())
            assertNull("Invalid canvas host should be rejected", candidate)
        }
    }

    @Test
    fun invalidNonMp4ExtensionRejected() {
        runBlocking {
            val client = clientWithHandler { request ->
                jsonResponse(
                    request,
                    200,
                    """{"status":"resolved","url":"https://canvaz.scdn.co/video.txt"}"""
                )
            }

            val resolver = OnDemandCanvasResolver(
                client = client,
                networkPolicyCheck = { true }
            )

            val candidate = resolver.resolve(testIdentity())
            assertNull("Non-MP4 extension should be rejected", candidate)
        }
    }

    @Test
    fun responseExceedingMaxSizeIsRejected() {
        runBlocking {
            val hugeBody = "a".repeat(100 * 1024)
            val client = clientWithHandler { request ->
                jsonResponse(request, 200, hugeBody)
            }
            val resolver = OnDemandCanvasResolver(client = client)
            val candidate = resolver.resolve(testIdentity())
            assertNull(candidate)
        }
    }

    @Test
    fun negativeCachingOn404OrMiss() {
        runBlocking {
            val requestCount = AtomicInteger(0)
            val client = clientWithHandler { request ->
                requestCount.incrementAndGet()
                jsonResponse(request, 404, """{"error":"not_found"}""")
            }

            val resolver = OnDemandCanvasResolver(
                client = client,
                networkPolicyCheck = { true }
            )

            val identity = testIdentity()
            val first = resolver.resolve(identity)
            assertNull(first)
            assertEquals(1, requestCount.get())

            val second = resolver.resolve(identity)
            assertNull(second)
            assertEquals("Negative cache hit should skip network call", 1, requestCount.get())
        }
    }

    @Test
    fun disabledMotionArtworkDoesNotCallRemoteResolver() {
        runBlocking {
            val requestCount = AtomicInteger(0)
            val client = clientWithHandler { request ->
                requestCount.incrementAndGet()
                jsonResponse(request, 200, """{"status":"resolved","url":"https://canvaz.scdn.co/v.mp4"}""")
            }

            val resolver = OnDemandCanvasResolver(
                client = client,
                networkPolicyCheck = { false }
            )

            val candidate = resolver.resolve(testIdentity())
            assertNull(candidate)
            assertEquals("Network policy check false must not invoke network", 0, requestCount.get())
        }
    }

    @Test
    fun coroutineCancellationCancelsOkHttpCall() {
        runBlocking {
            val requestStarted = CompletableDeferred<Unit>()

            val interceptor = Interceptor { chain ->
                val call = chain.call()
                requestStarted.complete(Unit)
                val start = System.currentTimeMillis()
                while (!call.isCanceled() && System.currentTimeMillis() - start < 1000) {
                    Thread.sleep(10)
                }
                if (call.isCanceled()) {
                    throw IOException("Canceled")
                }
                jsonResponse(chain.request(), 200, """{"status":"resolved","url":"https://canvaz.scdn.co/v.mp4"}""")
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build()

            val resolver = OnDemandCanvasResolver(
                client = client,
                networkPolicyCheck = { true }
            )

            val job = launch(Dispatchers.IO) {
                resolver.resolve(testIdentity())
            }

            requestStarted.await()
            job.cancelAndJoin()
            assertTrue(job.isCancelled)
        }
    }

    @Test
    fun trackChangeCancelsInFlightRequestAndSafelyResolvesNewTrack() {
        runBlocking {
            val trackARequestStarted = CompletableDeferred<Unit>()

            val client = clientWithHandler { request ->
                val buffer = okio.Buffer()
                request.body?.writeTo(buffer)
                val body = buffer.readUtf8()
                if (body.contains("Track A")) {
                    trackARequestStarted.complete(Unit)
                    val start = System.currentTimeMillis()
                    while (System.currentTimeMillis() - start < 1000) {
                        Thread.sleep(20)
                    }
                    jsonResponse(request, 200, """{"status":"resolved","url":"https://canvaz.scdn.co/track_a.mp4"}""")
                } else {
                    jsonResponse(request, 200, """{"status":"resolved","url":"https://canvaz.scdn.co/track_b.mp4"}""")
                }
            }

            val resolver = OnDemandCanvasResolver(
                client = client,
                networkPolicyCheck = { true }
            )

            val identityA = testIdentity(isrc = "AAA1", title = "Track A")
            val identityB = testIdentity(isrc = "BBB2", title = "Track B")

            var candidateA: MotionArtworkCandidate? = null
            val jobA = launch(Dispatchers.IO) {
                candidateA = resolver.resolve(identityA)
            }

            trackARequestStarted.await()
            jobA.cancelAndJoin()
            assertTrue(jobA.isCancelled)
            assertNull(candidateA)

            val candidateB = resolver.resolve(identityB)
            assertNotNull(candidateB)
            assertEquals("https://canvaz.scdn.co/track_b.mp4", candidateB?.url)
        }
    }

    @Test
    fun remoteFailurePreservesCurrentFallbackBehavior() {
        runBlocking {
            val client = clientWithHandler {
                throw IOException("Network unreachable")
            }

            val resolver = OnDemandCanvasResolver(
                client = client,
                networkPolicyCheck = { true }
            )

            val candidate = resolver.resolve(testIdentity())
            assertNull(candidate)
        }
    }
}
