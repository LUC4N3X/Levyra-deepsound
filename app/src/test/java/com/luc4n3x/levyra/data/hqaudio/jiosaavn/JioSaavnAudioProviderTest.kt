package com.luc4n3x.levyra.data.hqaudio.jiosaavn

import com.luc4n3x.levyra.data.hqaudio.AudioQualityTier
import com.luc4n3x.levyra.data.hqaudio.ProviderFailure
import com.luc4n3x.levyra.data.hqaudio.ProviderHttpExchange
import com.luc4n3x.levyra.data.hqaudio.ProviderHttpRequest
import com.luc4n3x.levyra.data.hqaudio.ProviderHttpResponse
import com.luc4n3x.levyra.data.hqaudio.ProviderLookupOutcome
import com.luc4n3x.levyra.data.hqaudio.ProviderSearchOutcome
import com.luc4n3x.levyra.data.hqaudio.ProviderStreamOutcome
import com.luc4n3x.levyra.data.hqaudio.StreamRejection
import com.luc4n3x.levyra.data.hqaudio.bytesFor
import com.luc4n3x.levyra.data.hqaudio.candidate
import com.luc4n3x.levyra.data.hqaudio.htmlResponse
import com.luc4n3x.levyra.data.hqaudio.jsonResponse
import com.luc4n3x.levyra.data.hqaudio.probeResponse
import com.luc4n3x.levyra.data.hqaudio.saavnSong
import com.luc4n3x.levyra.data.hqaudio.searchBody
import java.net.SocketTimeoutException
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JioSaavnAudioProviderTest {
    private class ScriptedExchange(
        private val handler: (ProviderHttpRequest) -> ProviderHttpResponse
    ) : ProviderHttpExchange {
        val requests = CopyOnWriteArrayList<ProviderHttpRequest>()

        override suspend fun execute(request: ProviderHttpRequest): ProviderHttpResponse {
            requests += request
            return handler(request)
        }
    }

    private val signedUrl = "https://web.saavncdn.com/820/abcdef_320.mp4?Expires=4102444800&Signature=abc&Key-Pair-Id=key"
    private val authBody = JSONObject().put("auth_url", signedUrl).put("type", "mp4").put("status", "success").toString()
    private val tierPattern = Regex("_(\\d+)\\.mp4$")
    private var profilesCreated = 0
    private val now = 1_800_000_000_000L

    private fun provider(exchange: ProviderHttpExchange) = JioSaavnAudioProvider(
        exchange = exchange,
        profileFactory = {
            profilesCreated += 1
            JioSaavnRequestProfile("49.40.10.${20 + profilesCreated}", "Reliance Jio")
        },
        clock = { now }
    )

    private fun streamExchange(
        signedResponse: ProviderHttpResponse = htmlResponse(403),
        authorization: String = authBody,
        open: (Int) -> ProviderHttpResponse
    ) = ScriptedExchange { request ->
        when {
            request.url.contains("song.generateAuthToken") -> jsonResponse(authorization)
            request.url.startsWith("https://web.saavncdn.com/") -> signedResponse
            request.url.startsWith("https://aac.saavncdn.com/") ->
                open(tierPattern.find(request.url)?.groupValues?.get(1)?.toInt() ?: 0)
            else -> htmlResponse(404)
        }
    }

    private fun validFor(tier: Int, actualKbps: Int = tier) = probeResponse(bytesFor(actualKbps, 200))

    @Test
    fun genuine320IsResolvedFromOpenCdnWhenSignedCdnRejectsTheRealAddress() {
        val exchange = streamExchange { tier -> if (tier == 320) validFor(320) else htmlResponse(404) }
        val provider = provider(exchange)
        val outcome = runBlocking { provider.resolveStream(candidate(duration = 200)) }
        val stream = (outcome as ProviderStreamOutcome.Resolved).stream
        assertEquals(AudioQualityTier.KBPS_320, stream.tier)
        assertEquals("https://aac.saavncdn.com/820/abcdef_320.mp4", stream.url)
        assertEquals(320, stream.estimatedKbps)
        assertEquals("audio/mp4", stream.mimeType)
        runBlocking { provider.resolveStream(candidate(duration = 200)) }
        assertEquals(1, exchange.requests.count { it.url.startsWith("https://web.saavncdn.com/") })
    }

    @Test
    fun signedStreamIsUsedWhenReachableAndKeepsItsExpiry() {
        val exchange = streamExchange(signedResponse = validFor(320)) { htmlResponse(404) }
        val stream = (runBlocking { provider(exchange).resolveStream(candidate(duration = 200)) } as ProviderStreamOutcome.Resolved).stream
        assertEquals(signedUrl, stream.url)
        assertEquals(4_102_444_800_000L - JioSaavnAudioProvider.AUTHORIZED_EXPIRY_MARGIN_MS, stream.expiresAtMs)
    }

    @Test
    fun invalid320FallsBackToGenuine160() {
        val exchange = streamExchange { tier ->
            when (tier) {
                320 -> validFor(320, actualKbps = 96)
                160 -> validFor(160)
                else -> htmlResponse(404)
            }
        }
        val stream = (runBlocking { provider(exchange).resolveStream(candidate(duration = 200)) } as ProviderStreamOutcome.Resolved).stream
        assertEquals(AudioQualityTier.KBPS_160, stream.tier)
    }

    @Test
    fun unavailable320IsNotEvenRequested() {
        val exchange = streamExchange { tier -> if (tier == 160) validFor(160) else htmlResponse(404) }
        val stream = (runBlocking { provider(exchange).resolveStream(candidate(duration = 200, offers320 = false)) } as ProviderStreamOutcome.Resolved).stream
        assertEquals(AudioQualityTier.KBPS_160, stream.tier)
        assertFalse(exchange.requests.any { it.url.endsWith("_320.mp4") })
    }

    @Test
    fun invalidDirectStreamsAreUnavailable() {
        val exchange = streamExchange { htmlResponse(404) }
        val outcome = runBlocking { provider(exchange).resolveStream(candidate(duration = 200)) }
        val rejections = (outcome as ProviderStreamOutcome.Unavailable).rejections
        assertTrue(rejections.all { it == StreamRejection.HTTP_STATUS })
        assertEquals(4, rejections.size)
    }

    @Test
    fun deniedMediaAuthorizationIsUnavailable() {
        val exchange = streamExchange(authorization = "{\"auth_url\":false,\"status\":\"success\"}") { validFor(it) }
        val outcome = runBlocking { provider(exchange).resolveStream(candidate(duration = 200)) }
        assertEquals(listOf(StreamRejection.NO_MEDIA), (outcome as ProviderStreamOutcome.Unavailable).rejections)
    }

    @Test
    fun searchReturnsParsedCandidates() {
        val body = searchBody(saavnSong("pW-kkdqr", "Blinding Lights", listOf("The Weeknd"), "Blinding Lights", 204))
        val outcome = runBlocking { provider(ScriptedExchange { jsonResponse(body) }).search("blinding lights") }
        val found = (outcome as ProviderSearchOutcome.Found).candidates
        assertEquals("pW-kkdqr", found.single().providerTrackId)
        assertEquals(204, found.single().durationSeconds)
    }

    @Test
    fun emptySearchIsNotAnError() {
        val outcome = runBlocking { provider(ScriptedExchange { jsonResponse("{\"total\":0,\"start\":1,\"results\":[]}") }).search("zzqq") }
        assertTrue((outcome as ProviderSearchOutcome.Found).candidates.isEmpty())
    }

    @Test
    fun malformedResponseIsReported() {
        val outcome = runBlocking { provider(ScriptedExchange { jsonResponse("<html>maintenance</html>") }).search("song") }
        assertEquals(ProviderFailure.MALFORMED_RESPONSE, (outcome as ProviderSearchOutcome.Failed).failure)
    }

    @Test
    fun forbiddenResponseRotatesTheRequestProfile() {
        val exchange = ScriptedExchange { htmlResponse(403) }
        val provider = provider(exchange)
        val outcome = runBlocking { provider.search("song") }
        assertEquals(ProviderFailure.FORBIDDEN, (outcome as ProviderSearchOutcome.Failed).failure)
        assertEquals(1, exchange.requests.size)
        runBlocking { provider.search("song") }
        assertEquals(2, profilesCreated)
    }

    @Test
    fun notFoundLookupMeansTrackDisappeared() {
        val outcome = runBlocking { provider(ScriptedExchange { htmlResponse(404) }).lookup("pW-kkdqr") }
        assertEquals(ProviderLookupOutcome.Missing, outcome)
    }

    @Test
    fun timeoutRetriesOnceThenFails() {
        val exchange = ScriptedExchange { throw SocketTimeoutException("timeout") }
        val outcome = runBlocking { provider(exchange).search("song") }
        assertEquals(ProviderFailure.TIMEOUT, (outcome as ProviderSearchOutcome.Failed).failure)
        assertEquals(JioSaavnAudioProvider.MAX_API_ATTEMPTS, exchange.requests.size)
    }

    @Test
    fun serverErrorsAreRetriedAtMostOnce() {
        val exchange = ScriptedExchange { htmlResponse(503) }
        val outcome = runBlocking { provider(exchange).search("song") }
        assertEquals(ProviderFailure.HTTP_ERROR, (outcome as ProviderSearchOutcome.Failed).failure)
        assertEquals(2, exchange.requests.size)
    }

    @Test
    fun indiaProfileIsSentOnlyToProviderApi() {
        val exchange = streamExchange { tier -> if (tier == 320) validFor(320) else htmlResponse(404) }
        runBlocking { provider(exchange).resolveStream(candidate(duration = 200)) }
        val apiRequests = exchange.requests.filter { it.url.startsWith("https://www.jiosaavn.com/") }
        val mediaRequests = exchange.requests.filterNot { it.url.startsWith("https://www.jiosaavn.com/") }
        assertTrue(apiRequests.isNotEmpty())
        assertTrue(mediaRequests.isNotEmpty())
        apiRequests.forEach { request ->
            assertTrue(request.headers.getValue("Accept-Language").contains("en-IN"))
            val forwarded = request.headers.getValue("X-Forwarded-For")
            assertEquals(forwarded, request.headers.getValue("X-Real-IP"))
            assertTrue(JioSaavnRequestProfile.isIndianAddress(forwarded))
        }
        mediaRequests.forEach { request ->
            assertFalse(request.headers.containsKey("X-Forwarded-For"))
            assertFalse(request.headers.containsKey("X-Real-IP"))
            assertFalse(request.headers.containsKey("Accept-Language"))
            assertEquals("bytes=0-8191", request.headers["Range"])
        }
    }
}
