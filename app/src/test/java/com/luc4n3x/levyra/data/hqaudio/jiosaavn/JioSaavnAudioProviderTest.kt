package com.luc4n3x.levyra.data.hqaudio.jiosaavn

import com.luc4n3x.levyra.data.hqaudio.AudioQualityTier
import com.luc4n3x.levyra.data.hqaudio.ProviderCircuitBreaker
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
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JioSaavnAudioProviderTest {
    private class ScriptedExchange(
        @Volatile var handler: suspend (ProviderHttpRequest) -> ProviderHttpResponse
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
    private val profileRandom = Random(11)
    private val createdProfiles = CopyOnWriteArrayList<JioSaavnRequestProfile>()
    private val exclusions = CopyOnWriteArrayList<Set<IndianAddressBlock>>()

    @Volatile
    private var clockMs = 1_800_000_000_000L

    private val songBody = searchBody(saavnSong("pW-kkdqr", "Blinding Lights", listOf("The Weeknd"), "Blinding Lights", 204))
    private val localCandidate = candidate(duration = 200).copy(mediaToken = REAL_MEDIA_TOKEN)

    private fun breaker(threshold: Int = ProviderCircuitBreaker.DEFAULT_FAILURE_THRESHOLD) =
        ProviderCircuitBreaker("jiosaavn", { clockMs }, failureThreshold = threshold)

    private fun provider(
        exchange: ProviderHttpExchange,
        catalogBreaker: ProviderCircuitBreaker = breaker(),
        authorizationBreaker: ProviderCircuitBreaker = breaker()
    ) = JioSaavnAudioProvider(
        exchange = exchange,
        profileFactory = { excluded ->
            exclusions += excluded
            JioSaavnRequestProfile.create(profileRandom, excluded).also { createdProfiles += it }
        },
        clock = { clockMs },
        catalogCircuitBreaker = catalogBreaker,
        authorizationCircuitBreaker = authorizationBreaker
    )

    private fun ProviderCircuitBreaker.forceOpen() =
        repeat(ProviderCircuitBreaker.DEFAULT_FAILURE_THRESHOLD) { onFailure(acquire()) }

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

    private fun forwardedFor(request: ProviderHttpRequest): String? = request.headers["X-Forwarded-For"]

    private fun ScriptedExchange.apiRequests() = requests.filter { it.url.startsWith("https://www.jiosaavn.com/") }

    private fun ScriptedExchange.authorizations() = requests.count { it.url.contains("song.generateAuthToken") }

    private fun openCircuitWithTimeouts(exchange: ScriptedExchange, provider: JioSaavnAudioProvider) {
        exchange.handler = { throw SocketTimeoutException("timeout") }
        runBlocking {
            repeat(ProviderCircuitBreaker.DEFAULT_FAILURE_THRESHOLD) { provider.search("failing $it") }
        }
    }

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
    fun deniedAuthorizationStillTriesTheRemainingLocalTiers() {
        val exchange = ScriptedExchange { request ->
            when {
                request.url.contains("song.generateAuthToken") -> jsonResponse("{\"auth_url\":false,\"status\":\"success\"}")
                request.url.endsWith("_160.mp4") -> validFor(160)
                else -> htmlResponse(404)
            }
        }
        val stream = (runBlocking { provider(exchange).resolveStream(localCandidate) } as ProviderStreamOutcome.Resolved).stream
        assertEquals("https://aac.saavncdn.com/396/$REAL_MEDIA_STEM" + "_160.mp4", stream.url)
        assertEquals(1, exchange.authorizations())
    }

    @Test
    fun deniedAuthorizationWithoutUsableLocalMediaIsUnavailable() {
        val exchange = ScriptedExchange { request ->
            if (request.url.contains("song.generateAuthToken")) {
                jsonResponse("{\"auth_url\":false,\"status\":\"success\"}")
            } else {
                htmlResponse(404)
            }
        }
        val outcome = runBlocking { provider(exchange).resolveStream(localCandidate) }
        val rejections = (outcome as ProviderStreamOutcome.Unavailable).rejections
        assertEquals(StreamRejection.NO_MEDIA, rejections.last())
        assertEquals(3, rejections.count { it == StreamRejection.HTTP_STATUS })
    }

    @Test
    fun undecodableMediaTokenKeepsTheAuthorizedPath() {
        val exchange = streamExchange { tier -> if (tier == 320) validFor(320) else htmlResponse(404) }
        runBlocking { provider(exchange).resolveStream(candidate(duration = 200)) }
        assertEquals(1, exchange.authorizations())
    }

    @Test
    fun localMediaTokenResolves320WithoutAuthorization() {
        val exchange = streamExchange { tier -> if (tier == 320) validFor(320) else htmlResponse(404) }
        val outcome = runBlocking { provider(exchange).resolveStream(localCandidate) }
        val stream = (outcome as ProviderStreamOutcome.Resolved).stream
        assertEquals("https://aac.saavncdn.com/396/$REAL_MEDIA_STEM" + "_320.mp4", stream.url)
        assertEquals(AudioQualityTier.KBPS_320, stream.tier)
        assertEquals(320, stream.estimatedKbps)
        assertEquals("mp4a", stream.codec)
        assertEquals(clockMs + JioSaavnAudioProvider.OPEN_MEDIA_TTL_MS, stream.expiresAtMs)
        assertEquals(1, exchange.requests.size)
        assertTrue(exchange.apiRequests().isEmpty())
    }

    @Test
    fun invalidLocalMediaFallsBackToTheAuthorizedSignedStream() {
        val exchange = streamExchange(signedResponse = validFor(320)) { htmlResponse(404) }
        val outcome = runBlocking { provider(exchange).resolveStream(localCandidate) }
        assertEquals(signedUrl, (outcome as ProviderStreamOutcome.Resolved).stream.url)
        assertEquals(1, exchange.authorizations())
    }

    @Test
    fun localMediaIsValidatedBeforeUse() {
        val exchange = streamExchange(signedResponse = validFor(320)) { validFor(320, actualKbps = 96) }
        val outcome = runBlocking { provider(exchange).resolveStream(localCandidate) }
        assertEquals(signedUrl, (outcome as ProviderStreamOutcome.Resolved).stream.url)
        assertEquals(1, exchange.authorizations())
    }

    @Test
    fun localLowerTierServesWhenAuthorizationIsUnavailable() {
        val exchange = ScriptedExchange { request ->
            when {
                request.url.contains("song.generateAuthToken") -> throw SocketTimeoutException("timeout")
                request.url.endsWith("_160.mp4") -> validFor(160)
                else -> htmlResponse(404)
            }
        }
        val stream = (runBlocking { provider(exchange).resolveStream(localCandidate) } as ProviderStreamOutcome.Resolved).stream
        assertEquals("https://aac.saavncdn.com/396/$REAL_MEDIA_STEM" + "_160.mp4", stream.url)
        assertEquals(AudioQualityTier.KBPS_160, stream.tier)
    }

    @Test
    fun localMissAndAuthorizationOutageIsATransientFailure() {
        val exchange = ScriptedExchange { request ->
            if (request.url.contains("song.generateAuthToken")) throw SocketTimeoutException("timeout") else htmlResponse(404)
        }
        val outcome = runBlocking { provider(exchange).resolveStream(localCandidate) }
        assertEquals(ProviderFailure.TIMEOUT, (outcome as ProviderStreamOutcome.Failed).failure)
    }

    @Test
    fun sharedMediaLocationIsNotProbedTwice() {
        val sameLocation = "https://web.saavncdn.com/396/$REAL_MEDIA_STEM" + "_320.mp4?Expires=4102444800&Signature=s&Key-Pair-Id=k"
        val authorization = JSONObject().put("auth_url", sameLocation).put("status", "success").toString()
        val exchange = streamExchange(authorization = authorization) { htmlResponse(404) }
        val outcome = runBlocking { provider(exchange).resolveStream(localCandidate) }
        assertTrue(outcome is ProviderStreamOutcome.Unavailable)
        assertEquals(1, exchange.requests.count { it.url == "https://aac.saavncdn.com/396/$REAL_MEDIA_STEM" + "_320.mp4" })
    }

    @Test
    fun searchReturnsParsedCandidates() {
        val outcome = runBlocking { provider(ScriptedExchange { jsonResponse(songBody) }).search("blinding lights") }
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
    fun forbiddenProfileIsReplacedAndTheRequestRetriedOnce() {
        val exchange = ScriptedExchange { request ->
            if (forwardedFor(request) == createdProfiles.first().forwardedAddress) htmlResponse(403) else jsonResponse(songBody)
        }
        val outcome = runBlocking { provider(exchange).search("song") }
        assertTrue(outcome is ProviderSearchOutcome.Found)
        assertEquals(2, exchange.requests.size)
        assertEquals(2, createdProfiles.size)
        assertNotEquals(createdProfiles[0].block, createdProfiles[1].block)
        assertEquals(setOf(createdProfiles[0].block!!), exclusions.last())
    }

    @Test
    fun forbiddenRetryFailureStopsAfterTheAttemptBudget() {
        val exchange = ScriptedExchange { htmlResponse(403) }
        val outcome = runBlocking { provider(exchange).search("song") }
        assertEquals(ProviderFailure.FORBIDDEN, (outcome as ProviderSearchOutcome.Failed).failure)
        assertEquals(JioSaavnAudioProvider.MAX_API_ATTEMPTS, exchange.requests.size)
        assertEquals(2, exchange.requests.map(::forwardedFor).distinct().size)
    }

    @Test
    fun rateLimitedProfileCoolsDownItsBlockAndRotates() {
        val exchange = ScriptedExchange { request ->
            if (forwardedFor(request) == createdProfiles.first().forwardedAddress) htmlResponse(429) else jsonResponse(songBody)
        }
        val outcome = runBlocking { provider(exchange).search("song") }
        assertTrue(outcome is ProviderSearchOutcome.Found)
        assertEquals(setOf(createdProfiles[0].block!!), exclusions.last())
        assertNotEquals(createdProfiles[0].block, createdProfiles[1].block)
    }

    @Test
    fun cooledBlockBecomesEligibleAgainAfterItsCooldown() {
        val rejected = CopyOnWriteArrayList<String>()
        val exchange = ScriptedExchange { request ->
            if (forwardedFor(request) in rejected) htmlResponse(403) else jsonResponse(songBody)
        }
        val provider = provider(exchange)
        runBlocking { provider.currentProfile() }
        rejected += createdProfiles[0].forwardedAddress
        runBlocking { provider.search("first") }
        clockMs += JioSaavnAudioProvider.BLOCK_COOLDOWN_MS + 1
        rejected += createdProfiles[1].forwardedAddress
        runBlocking { provider.search("second") }
        assertEquals(setOf(createdProfiles[1].block!!), exclusions.last())
    }

    @Test
    fun whenEveryBlockIsCoolingDownTheSoonestToRecoverIsReused() {
        val exchange = ScriptedExchange {
            clockMs += 1_000L
            htmlResponse(403)
        }
        val provider = provider(exchange, breaker(threshold = 100))
        runBlocking {
            provider.search("one")
            provider.search("two")
            provider.currentProfile()
        }
        val blocks = createdProfiles.map { it.block!! }
        assertEquals(JioSaavnRequestProfile.addressBlocks.toSet(), blocks.take(4).toSet())
        assertEquals(JioSaavnRequestProfile.addressBlocks.toSet() - blocks[0], exclusions[4])
        assertEquals(blocks[0], blocks[4])
    }

    @Test
    fun concurrentRejectionsOfOneProfileRotateItOnlyOnce() {
        val concurrency = 4
        val arrived = AtomicInteger()
        val allArrived = CompletableDeferred<Unit>()
        val exchange = ScriptedExchange { request ->
            if (forwardedFor(request) == createdProfiles.first().forwardedAddress) {
                if (arrived.incrementAndGet() == concurrency) allArrived.complete(Unit)
                allArrived.await()
                htmlResponse(403)
            } else {
                jsonResponse(songBody)
            }
        }
        val breaker = breaker()
        val provider = provider(exchange, breaker)
        val outcomes = runBlocking {
            withTimeout(5_000L) {
                (1..concurrency).map { async(Dispatchers.Default) { provider.search("song $it") } }.awaitAll()
            }
        }
        assertTrue(outcomes.all { it is ProviderSearchOutcome.Found })
        assertEquals(2, createdProfiles.size)
        assertEquals(ProviderCircuitBreaker.State.CLOSED, breaker.currentState)
    }

    @Test
    fun notFoundLookupMeansTrackDisappeared() {
        val outcome = runBlocking { provider(ScriptedExchange { htmlResponse(404) }).lookup("pW-kkdqr") }
        assertEquals(ProviderLookupOutcome.Missing, outcome)
    }

    @Test
    fun timeoutRetriesOnceWithoutTreatingTheBlockAsBanned() {
        val exchange = ScriptedExchange { throw SocketTimeoutException("timeout") }
        val outcome = runBlocking { provider(exchange).search("song") }
        assertEquals(ProviderFailure.TIMEOUT, (outcome as ProviderSearchOutcome.Failed).failure)
        assertEquals(JioSaavnAudioProvider.MAX_API_ATTEMPTS, exchange.requests.size)
        assertEquals(1, createdProfiles.size)
    }

    @Test
    fun networkFailureRetriesOnceWithoutRotatingTheProfile() {
        val exchange = ScriptedExchange { throw IOException("connection reset") }
        val outcome = runBlocking { provider(exchange).search("song") }
        assertEquals(ProviderFailure.NETWORK, (outcome as ProviderSearchOutcome.Failed).failure)
        assertEquals(JioSaavnAudioProvider.MAX_API_ATTEMPTS, exchange.requests.size)
        assertEquals(1, createdProfiles.size)
    }

    @Test
    fun serverErrorsAreRetriedAtMostOnce() {
        val exchange = ScriptedExchange { htmlResponse(503) }
        val outcome = runBlocking { provider(exchange).search("song") }
        assertEquals(ProviderFailure.HTTP_ERROR, (outcome as ProviderSearchOutcome.Failed).failure)
        assertEquals(2, exchange.requests.size)
        assertEquals(1, createdProfiles.size)
    }

    @Test
    fun oneOperationWithFailedRetriesCountsAsASingleBreakerFailure() {
        val exchange = ScriptedExchange { throw SocketTimeoutException("timeout") }
        val breaker = breaker()
        val provider = provider(exchange, breaker)
        runBlocking { repeat(ProviderCircuitBreaker.DEFAULT_FAILURE_THRESHOLD - 1) { provider.search("failing $it") } }
        assertEquals((ProviderCircuitBreaker.DEFAULT_FAILURE_THRESHOLD - 1) * JioSaavnAudioProvider.MAX_API_ATTEMPTS, exchange.requests.size)
        assertEquals(ProviderCircuitBreaker.State.CLOSED, breaker.currentState)
        runBlocking { provider.search("third") }
        assertEquals(ProviderCircuitBreaker.State.OPEN, breaker.currentState)
    }

    @Test
    fun retrySuccessWithinOneOperationResetsTheBreaker() {
        var failNext = true
        val exchange = ScriptedExchange {
            if (failNext) {
                failNext = false
                throw SocketTimeoutException("timeout")
            }
            jsonResponse(songBody)
        }
        val breaker = breaker()
        val provider = provider(exchange, breaker)
        runBlocking {
            repeat(ProviderCircuitBreaker.DEFAULT_FAILURE_THRESHOLD * 2) {
                failNext = true
                assertTrue(provider.search("flaky $it") is ProviderSearchOutcome.Found)
            }
        }
        assertEquals(ProviderCircuitBreaker.State.CLOSED, breaker.currentState)
    }

    @Test
    fun threeFailedLogicalOperationsOpenTheCircuitAndLaterCallsSkipHttp() {
        val exchange = ScriptedExchange { jsonResponse(songBody) }
        val breaker = breaker()
        val provider = provider(exchange, breaker)
        openCircuitWithTimeouts(exchange, provider)
        val expectedRequests = ProviderCircuitBreaker.DEFAULT_FAILURE_THRESHOLD * JioSaavnAudioProvider.MAX_API_ATTEMPTS
        assertEquals(expectedRequests, exchange.requests.size)
        assertEquals(ProviderCircuitBreaker.State.OPEN, breaker.currentState)
        val outcome = runBlocking { provider.search("three") }
        assertEquals(ProviderFailure.CIRCUIT_OPEN, (outcome as ProviderSearchOutcome.Failed).failure)
        val lookup = runBlocking { provider.lookup("pW-kkdqr") }
        assertEquals(ProviderFailure.CIRCUIT_OPEN, (lookup as ProviderLookupOutcome.Failed).failure)
        assertEquals(expectedRequests, exchange.requests.size)
    }

    @Test
    fun authorizationFailuresDoNotOpenTheCatalogCircuit() {
        val detailsBody = JSONObject()
            .put("songs", JSONArray().put(saavnSong("pW-kkdqr", "Blinding Lights", listOf("The Weeknd"), "Blinding Lights", 204)))
            .toString()
        val exchange = ScriptedExchange { request ->
            when {
                request.url.contains("song.generateAuthToken") -> throw SocketTimeoutException("timeout")
                request.url.contains("song.getDetails") -> jsonResponse(detailsBody)
                else -> jsonResponse(songBody)
            }
        }
        val catalog = breaker()
        val authorization = breaker()
        val provider = provider(exchange, catalog, authorization)
        runBlocking { repeat(ProviderCircuitBreaker.DEFAULT_FAILURE_THRESHOLD) { provider.resolveStream(candidate(duration = 200)) } }
        assertEquals(ProviderCircuitBreaker.State.OPEN, authorization.currentState)
        assertEquals(ProviderCircuitBreaker.State.CLOSED, catalog.currentState)
        assertTrue(runBlocking { provider.search("still works") } is ProviderSearchOutcome.Found)
        assertTrue(runBlocking { provider.lookup("pW-kkdqr") } is ProviderLookupOutcome.Found)
    }

    @Test
    fun openAuthorizationCircuitStillResolvesLocalMediaWithoutAuthorization() {
        val authorization = breaker().apply { forceOpen() }
        val exchange = ScriptedExchange { request -> if (request.url.endsWith("_160.mp4")) validFor(160) else htmlResponse(404) }
        val outcome = runBlocking { provider(exchange, authorizationBreaker = authorization).resolveStream(localCandidate) }
        val stream = (outcome as ProviderStreamOutcome.Resolved).stream
        assertEquals("https://aac.saavncdn.com/396/$REAL_MEDIA_STEM" + "_160.mp4", stream.url)
        assertEquals(0, exchange.authorizations())
        assertTrue(exchange.apiRequests().isEmpty())
    }

    @Test
    fun openAuthorizationCircuitWithoutLocalMediaFailsFastAndKeepsTheCatalog() {
        val authorization = breaker().apply { forceOpen() }
        val exchange = ScriptedExchange { jsonResponse(songBody) }
        val provider = provider(exchange, authorizationBreaker = authorization)
        val outcome = runBlocking { provider.resolveStream(candidate(duration = 200)) }
        assertEquals(ProviderFailure.CIRCUIT_OPEN, (outcome as ProviderStreamOutcome.Failed).failure)
        assertTrue(exchange.requests.isEmpty())
        assertTrue(runBlocking { provider.search("song") } is ProviderSearchOutcome.Found)
    }

    @Test
    fun openCatalogCircuitDoesNotBlockStreamResolution() {
        val catalog = breaker().apply { forceOpen() }
        val exchange = streamExchange(signedResponse = validFor(320)) { tier -> if (tier == 320) validFor(320) else htmlResponse(404) }
        val provider = provider(exchange, catalogBreaker = catalog)
        assertTrue(runBlocking { provider.resolveStream(localCandidate) } is ProviderStreamOutcome.Resolved)
        assertEquals(0, exchange.authorizations())
        assertTrue(runBlocking { provider.resolveStream(candidate(duration = 200)) } is ProviderStreamOutcome.Resolved)
        assertEquals(1, exchange.authorizations())
        val search = runBlocking { provider.search("song") }
        assertEquals(ProviderFailure.CIRCUIT_OPEN, (search as ProviderSearchOutcome.Failed).failure)
    }

    @Test
    fun openCircuitStillServesLocalMediaWithoutApiTraffic() {
        val exchange = ScriptedExchange { jsonResponse(songBody) }
        val provider = provider(exchange)
        openCircuitWithTimeouts(exchange, provider)
        val before = exchange.requests.size
        exchange.handler = { request -> if (request.url.endsWith("_320.mp4")) validFor(320) else htmlResponse(404) }
        val outcome = runBlocking { provider.resolveStream(localCandidate) }
        assertTrue(outcome is ProviderStreamOutcome.Resolved)
        assertEquals(before + 1, exchange.requests.size)
    }

    @Test
    fun halfOpenProbeRecoversTheCircuit() {
        val exchange = ScriptedExchange { jsonResponse(songBody) }
        val breaker = breaker()
        val provider = provider(exchange, breaker)
        openCircuitWithTimeouts(exchange, provider)
        val before = exchange.requests.size
        exchange.handler = { jsonResponse(songBody) }
        clockMs += ProviderCircuitBreaker.DEFAULT_OPEN_MS
        val outcome = runBlocking { provider.search("again") }
        assertTrue(outcome is ProviderSearchOutcome.Found)
        assertEquals(before + 1, exchange.requests.size)
        assertEquals(ProviderCircuitBreaker.State.CLOSED, breaker.currentState)
    }

    @Test
    fun failedHalfOpenProbeIsNotRetried() {
        val exchange = ScriptedExchange { jsonResponse(songBody) }
        val breaker = breaker()
        val provider = provider(exchange, breaker)
        openCircuitWithTimeouts(exchange, provider)
        val before = exchange.requests.size
        clockMs += ProviderCircuitBreaker.DEFAULT_OPEN_MS
        val outcome = runBlocking { provider.search("again") }
        assertEquals(ProviderFailure.TIMEOUT, (outcome as ProviderSearchOutcome.Failed).failure)
        assertEquals(before + 1, exchange.requests.size)
        assertEquals(ProviderCircuitBreaker.State.OPEN, breaker.currentState)
    }

    @Test
    fun halfOpenLetsOnlyOneConcurrentRequestReachTheNetwork() {
        val exchange = ScriptedExchange { jsonResponse(songBody) }
        val provider = provider(exchange)
        openCircuitWithTimeouts(exchange, provider)
        val before = exchange.requests.size
        val release = CompletableDeferred<Unit>()
        exchange.handler = {
            release.await()
            jsonResponse(songBody)
        }
        clockMs += ProviderCircuitBreaker.DEFAULT_OPEN_MS
        runBlocking {
            withTimeout(5_000L) {
                val probe = async(Dispatchers.Default) { provider.search("probe") }
                while (exchange.requests.size == before) delay(5L)
                val others = (1..5).map { async(Dispatchers.Default) { provider.search("other $it") } }.awaitAll()
                assertTrue(others.all { (it as ProviderSearchOutcome.Failed).failure == ProviderFailure.CIRCUIT_OPEN })
                release.complete(Unit)
                assertTrue(probe.await() is ProviderSearchOutcome.Found)
            }
        }
        assertEquals(before + 1, exchange.requests.size)
    }

    @Test
    fun indiaProfileIsSentOnlyToProviderApi() {
        val exchange = streamExchange { tier -> if (tier == 320) validFor(320) else htmlResponse(404) }
        runBlocking { provider(exchange).resolveStream(candidate(duration = 200)) }
        val apiRequests = exchange.apiRequests()
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

    private companion object {
        const val REAL_MEDIA_TOKEN = "ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDy8IXxuTNJ1oLbvDGDneZj5h25kdaKPCof228ruhJnw7PIr7uKBnaPmxw7tS9a8Gtq"
        const val REAL_MEDIA_STEM = "eca27e31e93211051fa0de18160ea825"
    }
}
