package com.luc4n3x.levyra.data.hqaudio.qobuz

import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackCandidate
import com.luc4n3x.levyra.data.hqaudio.HighQualityCodec
import com.luc4n3x.levyra.data.hqaudio.HighQualityPreference
import com.luc4n3x.levyra.data.hqaudio.ProviderCircuitBreaker
import com.luc4n3x.levyra.data.hqaudio.ProviderFailure
import com.luc4n3x.levyra.data.hqaudio.ProviderHttpExchange
import com.luc4n3x.levyra.data.hqaudio.ProviderHttpRequest
import com.luc4n3x.levyra.data.hqaudio.ProviderHttpResponse
import com.luc4n3x.levyra.data.hqaudio.ProviderSearchOutcome
import com.luc4n3x.levyra.data.hqaudio.ProviderStreamOutcome
import com.luc4n3x.levyra.data.hqaudio.StreamRejection
import com.luc4n3x.levyra.data.hqaudio.flacProbeBody
import com.luc4n3x.levyra.data.hqaudio.flacResponse
import com.luc4n3x.levyra.data.hqaudio.htmlResponse
import com.luc4n3x.levyra.data.hqaudio.jsonResponse
import com.luc4n3x.levyra.data.hqaudio.mp3Response
import com.luc4n3x.levyra.data.hqaudio.query
import com.luc4n3x.levyra.data.hqaudio.qobuzMediaUrl
import com.luc4n3x.levyra.data.hqaudio.qobuzSearchBody
import com.luc4n3x.levyra.data.hqaudio.qobuzStreamBody
import com.luc4n3x.levyra.data.hqaudio.qobuzTrack
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QobuzAudioProviderTest {
    private var nowMs = System.currentTimeMillis()
    private val primary = QobuzBackend("alpha", "https://qobuz.kennyy.com.br", sendsRegion = false)
    private val secondary = QobuzBackend("beta", "https://trypt-hifi-dl-456461932686.us-west1.run.app", sendsRegion = true)

    private class RoutedExchange(
        private val route: suspend (ProviderHttpRequest) -> ProviderHttpResponse
    ) : ProviderHttpExchange {
        val requests = CopyOnWriteArrayList<ProviderHttpRequest>()

        override suspend fun execute(request: ProviderHttpRequest): ProviderHttpResponse {
            requests += request
            return route(request)
        }

        fun count(host: String, path: String): Int =
            requests.count { it.url.toHttpUrl().host == host && it.url.toHttpUrl().encodedPath.endsWith(path) }
    }

    private fun provider(exchange: ProviderHttpExchange, requestBudgetMs: Long = 1_000L) = QobuzAudioProvider(
        exchange = exchange,
        backends = listOf(primary, secondary),
        clock = { nowMs },
        region = "IT",
        requestBudgetMs = requestBudgetMs
    )

    private fun candidate(
        id: String = "12345",
        bitDepth: Int = 24,
        sampleRateHz: Int = 96_000,
        duration: Int = 200
    ) = AlternativeTrackCandidate(
        providerId = "qobuz",
        providerTrackId = id,
        title = "Blinding Lights",
        primaryArtists = listOf("The Weeknd"),
        featuredArtists = emptyList(),
        album = "After Hours",
        durationSeconds = duration,
        explicit = false,
        isrc = "USUG11904206",
        offers320 = true,
        maxBitDepth = bitDepth,
        maxSampleRateHz = sampleRateHz
    )

    private fun searchOk() = jsonResponse(qobuzSearchBody(qobuzTrack(12345L)))

    private fun ProviderHttpRequest.quality(): Int? = url.toHttpUrl().queryParameter("quality")?.toInt()

    private fun ProviderHttpRequest.isMedia(): Boolean = url.contains("akamaized.net")

    private fun ProviderHttpRequest.isPrimary(): Boolean = url.toHttpUrl().host == primary.host

    @Test
    fun searchAdaptsQobuzTracksIntoLevyraCandidates() {
        val body = qobuzSearchBody(
            qobuzTrack(1L, title = "Blinding Lights", version = "Live", samplingRateKhz = 44.1, bitDepth = 16),
            qobuzTrack(2L, streamable = false),
            qobuzTrack(3L, title = "Blinding Lights", version = "Remastered 2020")
        )
        val outcome = runBlocking { provider(RoutedExchange { jsonResponse(body) }).search("Blinding Lights") }
        val candidates = (outcome as ProviderSearchOutcome.Found).candidates
        assertEquals(listOf("1", "3"), candidates.map { it.providerTrackId })
        assertEquals("Blinding Lights (Live)", candidates[0].title)
        assertEquals(16, candidates[0].maxBitDepth)
        assertEquals(44_100, candidates[0].maxSampleRateHz)
        assertEquals("USUG11904206", candidates[0].isrc)
        assertEquals(false, candidates[0].explicit)
        assertEquals(96_000, candidates[1].maxSampleRateHz)
    }

    @Test
    fun exactIsrcIsSearchedFirst() {
        val queries = provider(RoutedExchange { searchOk() }).searchQueries(query(isrc = "us-ug1-19-04206"))
        assertEquals("USUG11904206", queries.first())
        val withoutIsrc = provider(RoutedExchange { searchOk() }).searchQueries(query())
        assertFalse(withoutIsrc.any { it.matches(Regex("[A-Z0-9]{12}")) })
    }

    @Test
    fun serverErrorFailsOverToSecondBackendAndSkipsDeadHostAfterwards() {
        val exchange = RoutedExchange { request -> if (request.isPrimary()) htmlResponse(503) else searchOk() }
        val provider = provider(exchange)
        assertTrue(runBlocking { provider.search("a") } is ProviderSearchOutcome.Found)
        assertTrue(runBlocking { provider.search("b") } is ProviderSearchOutcome.Found)
        assertEquals(1, exchange.count(primary.host, "get-music"))
        assertEquals(2, exchange.count(secondary.host, "get-music"))
        val health = provider.health().first { it.backend == "alpha" }
        assertEquals(ProviderCircuitBreaker.State.OPEN.name, health.state)
        assertTrue(health.cooldownRemainingMs > 0L)
    }

    @Test
    fun forbiddenBackendTripsImmediately() {
        val exchange = RoutedExchange { request -> if (request.isPrimary()) htmlResponse(403) else searchOk() }
        val provider = provider(exchange)
        runBlocking { provider.search("a") }
        runBlocking { provider.search("b") }
        assertEquals(1, exchange.count(primary.host, "get-music"))
    }

    @Test
    fun rateLimitHonoursRetryAfterCooldown() {
        val limited = ProviderHttpResponse(429, mapOf("Retry-After" to "300"), ByteArray(0))
        val exchange = RoutedExchange { request -> if (request.isPrimary()) limited else searchOk() }
        val provider = provider(exchange)
        runBlocking { provider.search("a") }
        val health = provider.health().first { it.backend == "alpha" }
        assertTrue(health.cooldownRemainingMs >= 300_000L)
        assertTrue(health.lastFailure.contains(ProviderFailure.RATE_LIMITED.name))
    }

    @Test
    fun everyBackendDeadFailsFastThenStopsCallingThem() {
        val exchange = RoutedExchange { throw UnknownHostException("gone") }
        val provider = provider(exchange)
        assertEquals(ProviderFailure.NETWORK, (runBlocking { provider.search("a") } as ProviderSearchOutcome.Failed).failure)
        assertEquals(2, exchange.requests.size)
        assertEquals(ProviderFailure.CIRCUIT_OPEN, (runBlocking { provider.search("b") } as ProviderSearchOutcome.Failed).failure)
        assertEquals(2, exchange.requests.size)
    }

    @Test
    fun halfOpenProbeRecoversBackend() {
        var healthy = false
        val exchange = RoutedExchange { if (healthy) searchOk() else throw UnknownHostException("gone") }
        val provider = provider(exchange)
        runBlocking { provider.search("a") }
        nowMs += QobuzAudioProvider.BACKEND_OPEN_MS + 1L
        healthy = true
        assertTrue(runBlocking { provider.search("b") } is ProviderSearchOutcome.Found)
        assertEquals(ProviderCircuitBreaker.State.CLOSED.name, provider.health().first { it.backend == "alpha" }.state)
    }

    @Test
    fun timeoutIsBoundedByRequestBudget() {
        val exchange = RoutedExchange {
            delay(5_000L)
            searchOk()
        }
        val started = System.nanoTime()
        val outcome = runBlocking { provider(exchange, requestBudgetMs = 100L).search("a") }
        val elapsedMs = (System.nanoTime() - started) / 1_000_000L
        assertEquals(ProviderFailure.TIMEOUT, (outcome as ProviderSearchOutcome.Failed).failure)
        assertTrue("elapsed=$elapsedMs", elapsedMs < 2_000L)
    }

    @Test
    fun socketTimeoutIsClassifiedAsTimeoutWithoutImmediateTrip() {
        val exchange = RoutedExchange { throw SocketTimeoutException("slow") }
        val provider = provider(exchange)
        runBlocking { provider.search("a") }
        assertEquals(ProviderCircuitBreaker.State.CLOSED.name, provider.health().first { it.backend == "alpha" }.state)
        runBlocking { provider.search("b") }
        assertEquals(ProviderCircuitBreaker.State.OPEN.name, provider.health().first { it.backend == "alpha" }.state)
    }

    @Test
    fun malformedJsonAndOfflineHtmlAreProviderFailures() {
        val offline = ProviderHttpResponse(200, mapOf("Content-Type" to "text/html"), "<!doctype html><title>Offline</title>".toByteArray())
        val exchange = RoutedExchange { request -> if (request.isPrimary()) offline else jsonResponse("{not json") }
        val outcome = runBlocking { provider(exchange).search("a") }
        assertEquals(ProviderFailure.MALFORMED_RESPONSE, (outcome as ProviderSearchOutcome.Failed).failure)
    }

    @Test
    fun captchaIsTreatedAsForbidden() {
        val captcha = jsonResponse(JSONObject().put("success", false).put("error", "Captcha required.").toString())
        val outcome = runBlocking { provider(RoutedExchange { captcha }).search("a") }
        assertEquals(ProviderFailure.FORBIDDEN, (outcome as ProviderSearchOutcome.Failed).failure)
    }

    @Test
    fun missingSearchEndpointIsBackendFailureNotCatalogMiss() {
        val outcome = runBlocking { provider(RoutedExchange { htmlResponse(404) }).search("a") }
        assertTrue(outcome is ProviderSearchOutcome.Failed)
        val emptyCatalog = runBlocking { provider(RoutedExchange { jsonResponse(qobuzSearchBody()) }).search("a") }
        assertEquals(emptyList<AlternativeTrackCandidate>(), (emptyCatalog as ProviderSearchOutcome.Found).candidates)
    }

    @Test
    fun hiResFlacIsResolvedWithTruthfulMetadata() {
        val exchange = RoutedExchange { request ->
            when {
                request.isMedia() -> flacResponse(60_000_000L)
                else -> jsonResponse(qobuzStreamBody(qobuzMediaUrl(request.quality() ?: 0)))
            }
        }
        val outcome = runBlocking { provider(exchange).resolveStream(candidate(), HighQualityPreference.MAXIMUM) }
        val stream = (outcome as ProviderStreamOutcome.Resolved).stream
        assertEquals(HighQualityCodec.FLAC, stream.quality.codec)
        assertEquals(24, stream.quality.bitDepth)
        assertEquals(96_000, stream.quality.sampleRateHz)
        assertTrue(stream.quality.isHiRes)
        assertNull(stream.quality.nominalKbps)
        assertEquals(2_400, stream.quality.estimatedKbps)
        assertEquals("FLAC · 24-bit · 96 kHz", stream.quality.label)
        assertEquals("audio/flac", stream.mimeType)
        assertEquals(listOf(7), exchange.requests.mapNotNull { it.quality() })
    }

    @Test
    fun hiRes192IsRequestedOnlyWhenCatalogAdvertisesIt() {
        val exchange = RoutedExchange { request ->
            when {
                request.isMedia() -> flacResponse(100_000_000L, flacProbeBody(sampleRateHz = 192_000, bitDepth = 24))
                else -> jsonResponse(qobuzStreamBody(qobuzMediaUrl(request.quality() ?: 0), samplingRateKhz = 192.0))
            }
        }
        val outcome = runBlocking {
            provider(exchange).resolveStream(candidate(sampleRateHz = 192_000), HighQualityPreference.MAXIMUM)
        }
        val quality = (outcome as ProviderStreamOutcome.Resolved).stream.quality
        assertEquals(192_000, quality.sampleRateHz)
        assertEquals("FLAC · 24-bit · 192 kHz", quality.label)
        assertEquals(listOf(27), exchange.requests.mapNotNull { it.quality() })
    }

    @Test
    fun balancedPreferenceStopsAtCdQuality() {
        val exchange = RoutedExchange { request ->
            when {
                request.isMedia() -> flacResponse(22_500_000L, flacProbeBody(sampleRateHz = 44_100, bitDepth = 16))
                else -> jsonResponse(qobuzStreamBody(qobuzMediaUrl(request.quality() ?: 0), 16, 44.1))
            }
        }
        val outcome = runBlocking { provider(exchange).resolveStream(candidate(), HighQualityPreference.BALANCED) }
        val quality = (outcome as ProviderStreamOutcome.Resolved).stream.quality
        assertTrue(quality.isCdQuality)
        assertFalse(quality.isHiRes)
        assertEquals("FLAC · 16-bit · 44.1 kHz", quality.label)
        assertEquals(listOf(6), exchange.requests.mapNotNull { it.quality() })
    }

    @Test
    fun backendMetadataClaimsAreOverriddenByTheActualStream() {
        val exchange = RoutedExchange { request ->
            when {
                request.isMedia() -> flacResponse(22_500_000L, flacProbeBody(sampleRateHz = 44_100, bitDepth = 16))
                else -> jsonResponse(qobuzStreamBody(qobuzMediaUrl(request.quality() ?: 0), 24, 96.0))
            }
        }
        val quality = (runBlocking { provider(exchange).resolveStream(candidate(), HighQualityPreference.MAXIMUM) }
            as ProviderStreamOutcome.Resolved).stream.quality
        assertEquals(16, quality.bitDepth)
        assertEquals(44_100, quality.sampleRateHz)
        assertFalse(quality.isHiRes)
    }

    @Test
    fun unavailableFormatWalksDownTheQualityLadder() {
        val refused = jsonResponse(JSONObject().put("success", false).put("error", "format unavailable").toString())
        val exchange = RoutedExchange { request ->
            when {
                request.isMedia() -> flacResponse(22_500_000L, flacProbeBody(sampleRateHz = 44_100, bitDepth = 16))
                request.quality() == 7 -> refused
                else -> jsonResponse(qobuzStreamBody(qobuzMediaUrl(request.quality() ?: 0), 16, 44.1))
            }
        }
        val provider = provider(exchange)
        val outcome = runBlocking { provider.resolveStream(candidate(), HighQualityPreference.MAXIMUM) }
        assertTrue(outcome is ProviderStreamOutcome.Resolved)
        assertEquals(listOf(7, 7, 6), exchange.requests.mapNotNull { it.quality() })
        assertEquals(0, provider.health().first().consecutiveFailures)
        assertEquals(ProviderCircuitBreaker.State.CLOSED.name, provider.health().first().state)
    }

    @Test
    fun lossyFallbackIsModeledAsMp3Not320Flac() {
        val refused = jsonResponse(JSONObject().put("success", false).put("error", "no").toString())
        val exchange = RoutedExchange { request ->
            when {
                request.isMedia() -> mp3Response(8_000_000L)
                request.quality() == 5 -> jsonResponse(qobuzStreamBody(qobuzMediaUrl(5)))
                else -> refused
            }
        }
        val quality = (runBlocking { provider(exchange).resolveStream(candidate(), HighQualityPreference.BALANCED) }
            as ProviderStreamOutcome.Resolved).stream.quality
        assertEquals(HighQualityCodec.MP3, quality.codec)
        assertFalse(quality.lossless)
        assertEquals(320, quality.effectiveKbps)
    }

    @Test
    fun previewIsRejectedWithoutTryingLowerFormats() {
        val exchange = RoutedExchange { request ->
            when {
                request.isMedia() -> flacResponse(9_000_000L, flacProbeBody(seconds = 30))
                else -> jsonResponse(qobuzStreamBody(qobuzMediaUrl(request.quality() ?: 0)))
            }
        }
        val outcome = runBlocking { provider(exchange).resolveStream(candidate(), HighQualityPreference.MAXIMUM) }
        assertEquals(listOf(StreamRejection.PREVIEW), (outcome as ProviderStreamOutcome.Unavailable).rejections)
        assertEquals(1, exchange.requests.count { it.isMedia() })
    }

    @Test
    fun differentLengthRecordingIsRejected() {
        val exchange = RoutedExchange { request ->
            when {
                request.isMedia() -> flacResponse(80_000_000L, flacProbeBody(seconds = 260))
                else -> jsonResponse(qobuzStreamBody(qobuzMediaUrl(request.quality() ?: 0)))
            }
        }
        val outcome = runBlocking { provider(exchange).resolveStream(candidate(), HighQualityPreference.MAXIMUM) }
        assertEquals(listOf(StreamRejection.DURATION_MISMATCH), (outcome as ProviderStreamOutcome.Unavailable).rejections)
    }

    @Test
    fun expiredSignedUrlIsNeverProbed() {
        val expired = nowMs / 1_000L - 10L
        val exchange = RoutedExchange { request -> jsonResponse(qobuzStreamBody(qobuzMediaUrl(request.quality() ?: 0, expired))) }
        val outcome = runBlocking { provider(exchange).resolveStream(candidate(), HighQualityPreference.BALANCED) }
        assertTrue((outcome as ProviderStreamOutcome.Unavailable).rejections.all { it == StreamRejection.EXPIRED })
        assertEquals(0, exchange.requests.count { it.isMedia() })
    }

    @Test
    fun signedExpiryBecomesStreamExpiry() {
        val expiresAtSeconds = nowMs / 1_000L + 900L
        val exchange = RoutedExchange { request ->
            when {
                request.isMedia() -> flacResponse(60_000_000L)
                else -> jsonResponse(qobuzStreamBody(qobuzMediaUrl(request.quality() ?: 0, expiresAtSeconds)))
            }
        }
        val stream = (runBlocking { provider(exchange).resolveStream(candidate(), HighQualityPreference.MAXIMUM) }
            as ProviderStreamOutcome.Resolved).stream
        assertEquals(expiresAtSeconds * 1_000L, stream.expiresAtMs)
    }

    @Test
    fun htmlOrJsonMasqueradingAsMediaIsRejected() {
        val exchange = RoutedExchange { request ->
            when {
                request.isMedia() && request.url.contains("fmt=7") -> htmlResponse(200)
                request.isMedia() -> jsonResponse("{\"error\":\"denied\"}")
                else -> jsonResponse(qobuzStreamBody(qobuzMediaUrl(request.quality() ?: 0)))
            }
        }
        val outcome = runBlocking { provider(exchange).resolveStream(candidate(), HighQualityPreference.MAXIMUM) }
        val rejections = (outcome as ProviderStreamOutcome.Unavailable).rejections
        assertTrue(rejections.all { it == StreamRejection.NOT_AUDIO })
    }

    @Test
    fun streamOnUnapprovedHostIsRejectedBeforeAnyRequest() {
        val exchange = RoutedExchange { jsonResponse(qobuzStreamBody("https://127.0.0.1/file.flac")) }
        val outcome = runBlocking { provider(exchange).resolveStream(candidate(), HighQualityPreference.BALANCED) }
        assertTrue(StreamRejection.UNSAFE_DESTINATION in (outcome as ProviderStreamOutcome.Unavailable).rejections)
        assertEquals(0, exchange.requests.count { it.url.contains("127.0.0.1") })
    }

    @Test
    fun streamBackendOutageIsFailureNotMissingTrack() {
        val exchange = RoutedExchange { throw UnknownHostException("gone") }
        val outcome = runBlocking { provider(exchange).resolveStream(candidate(), HighQualityPreference.MAXIMUM) }
        assertEquals(ProviderFailure.NETWORK, (outcome as ProviderStreamOutcome.Failed).failure)
        assertEquals(2, exchange.requests.size)
    }

    @Test
    fun missingTrackOnBackendIsUnavailableNotOutage() {
        val outcome = runBlocking {
            provider(RoutedExchange { htmlResponse(404) }).resolveStream(candidate(), HighQualityPreference.BALANCED)
        }
        assertTrue(outcome is ProviderStreamOutcome.Unavailable)
    }

    @Test
    fun streamRequestsAreBoundedEvenWhenEveryFormatFails() {
        val exchange = RoutedExchange { request ->
            when {
                request.isMedia() -> htmlResponse(403)
                else -> jsonResponse(qobuzStreamBody(qobuzMediaUrl(request.quality() ?: 0)))
            }
        }
        runBlocking { provider(exchange).resolveStream(candidate(sampleRateHz = 192_000), HighQualityPreference.MAXIMUM) }
        assertTrue(exchange.requests.size <= QobuzFormat.entries.size * 2 * 2)
    }

    @Test
    fun trypTRootLevelStreamWithoutSuccessFlagIsGranted() {
        val trypt = JSONObject().put("url", qobuzMediaUrl(7)).put("bit_depth", 24).put("sampling_rate", 96.0).toString()
        val payload = QobuzPayloadParser.stream(trypt)
        assertEquals(qobuzMediaUrl(7), (payload as QobuzStreamPayload.Granted).url)
        assertEquals(24, payload.bitDepth)
        assertEquals(96_000, payload.sampleRateHz)
        val refused = JSONObject().put("success", false).put("error", "no").toString()
        assertEquals(QobuzStreamPayload.FormatUnavailable, QobuzPayloadParser.stream(refused))
        val preview = JSONObject().put("directUrl", qobuzMediaUrl(6)).put("previewDetected", true).toString()
        assertEquals(QobuzStreamPayload.FormatUnavailable, QobuzPayloadParser.stream(preview))
    }

    @Test
    fun emptyCatalogOnOneBackendContinuesToTheNext() {
        val exchange = RoutedExchange { request ->
            if (request.isPrimary()) jsonResponse(qobuzSearchBody()) else searchOk()
        }
        val outcome = runBlocking { provider(exchange).search("a") }
        assertEquals(listOf("12345"), (outcome as ProviderSearchOutcome.Found).candidates.map { it.providerTrackId })
        assertEquals(1, exchange.count(secondary.host, "get-music"))
    }

    @Test
    fun unavailableFormatIsRetriedOnTheNextBackendBeforeLoweringQuality() {
        val refused = jsonResponse(JSONObject().put("success", false).put("error", "format unavailable").toString())
        val exchange = RoutedExchange { request ->
            when {
                request.isMedia() -> flacResponse(60_000_000L)
                request.isPrimary() -> refused
                else -> jsonResponse(JSONObject().put("url", qobuzMediaUrl(request.quality() ?: 0)).toString())
            }
        }
        val quality = (runBlocking { provider(exchange).resolveStream(candidate(), HighQualityPreference.MAXIMUM) }
            as ProviderStreamOutcome.Resolved).stream.quality
        assertEquals(24, quality.bitDepth)
        assertEquals(listOf(7, 7), exchange.requests.mapNotNull { it.quality() })
    }

    @Test
    fun maximumKeepsCdFallbackButPicksRealHiResFromAnotherBackend() {
        val exchange = RoutedExchange { request ->
            when {
                request.isMedia() && request.url.contains("uid=cd") ->
                    flacResponse(22_500_000L, flacProbeBody(sampleRateHz = 44_100, bitDepth = 16))
                request.isMedia() -> flacResponse(60_000_000L)
                request.isPrimary() -> jsonResponse(qobuzStreamBody(qobuzMediaUrl(7).replace("uid=1", "uid=cd")))
                else -> jsonResponse(qobuzStreamBody(qobuzMediaUrl(7)))
            }
        }
        val quality = (runBlocking { provider(exchange).resolveStream(candidate(), HighQualityPreference.MAXIMUM) }
            as ProviderStreamOutcome.Resolved).stream.quality
        assertTrue(quality.isHiRes)
        assertEquals(96_000, quality.sampleRateHz)
    }

    @Test
    fun maximumReturnsTheCdFallbackWhenNoBackendHasRealHiRes() {
        val exchange = RoutedExchange { request ->
            when {
                request.isMedia() -> flacResponse(22_500_000L, flacProbeBody(sampleRateHz = 44_100, bitDepth = 16))
                else -> jsonResponse(qobuzStreamBody(qobuzMediaUrl(7).replace("uid=1", "uid=${request.url.hashCode()}")))
            }
        }
        val quality = (runBlocking { provider(exchange).resolveStream(candidate(), HighQualityPreference.MAXIMUM) }
            as ProviderStreamOutcome.Resolved).stream.quality
        assertTrue(quality.isCdQuality)
        assertTrue(exchange.requests.mapNotNull { it.quality() }.all { it == 7 })
    }
}
