package com.luc4n3x.levyra.feature.recognition

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.CRC32
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShazamRecognitionProviderTest {

    @Test
    fun responseParserExtractsRichMetadataAndRejectsMalformedUrls() {
        val payload = """
            {
              "track": {
                "key": "123",
                "title": "Song",
                "subtitle": "Artist",
                "url": "https://www.shazam.com/track/123",
                "isrc": "USAAA1234567",
                "images": {"coverarthq": "https://images.example/cover.jpg"},
                "genres": {"primary": "Pop"},
                "sections": [{"type":"SONG","metadata":[
                  {"title":"Album","text":"Album name"},
                  {"title":"Released","text":"12 May 2024"},
                  {"title":"Label","text":"Label name"}
                ]}],
                "hub": {"options":[{"type":"VIDEO","actions":[
                  {"uri":"https://www.youtube.com/watch?v=abcdefghijk"}
                ]}]}
              }
            }
        """.trimIndent()

        val match = parseShazamResponse(payload) as RecognitionOutcome.Match

        assertEquals("Song", match.result.title)
        assertEquals("Artist", match.result.artist)
        assertEquals("Album name", match.result.album)
        assertEquals("2024", match.result.year)
        assertEquals("Label name", match.result.label)
        assertEquals("abcdefghijk", match.result.youtubeVideoId)
        assertEquals(SHAZAM_PROVIDER_ID, match.result.provider)

        val malformed = parseShazamResponse(
            """{"track":{"title":"Song","subtitle":"Artist","url":"https://","images":{"coverart":"https://"}}}"""
        ) as RecognitionOutcome.Match
        assertTrue(malformed.result.externalId.isEmpty())
        assertTrue(malformed.result.artworkUrl.isEmpty())
    }

    @Test
    fun missingTrackIsNoMatchAndMalformedJsonIsNetworkError() {
        assertEquals(RecognitionOutcome.NoMatch, parseShazamResponse("{}"))
        assertEquals(
            RecognitionOutcome.Error(RecognitionErrorKind.Network),
            parseShazamResponse("not-json")
        )
    }

    @Test
    fun requestBodyUsesCurrentShazamMobileShape() {
        val signature = ShazamSignature(
            payload = byteArrayOf(1, 2, 3, 4),
            sampleDurationMs = 3_120L,
            peakCount = 12
        )
        val before = System.currentTimeMillis()
        val body = ShazamRecognitionProvider().requestBody(signature)
        val after = System.currentTimeMillis()

        assertEquals("Europe/Moscow", body.getString("timezone"))
        assertTrue(body.getLong("timestamp") in before..after)
        assertEquals(0, body.getJSONObject("context").length())
        assertEquals(0, body.getJSONObject("geolocation").length())

        val encodedSignature = body.getJSONObject("signature")
        assertEquals(3_120L, encodedSignature.getLong("samplems"))
        assertTrue(
            encodedSignature.getString("uri")
                .startsWith(ShazamSignatureGenerator.SIGNATURE_URI_PREFIX)
        )
        assertFalse(encodedSignature.has("timestamp"))
    }

    @Test
    fun discoveryEndpointCarriesCurrentRecognitionParameters() {
        val endpoint = defaultShazamEndpoint("REQUEST", "DEVICE")

        assertTrue(endpoint.startsWith("https://amp.shazam.com/discovery/v5/en-US/US/android/-/tag/REQUEST/DEVICE?"))
        assertTrue(endpoint.contains("shazamapiversion=v3"))
        assertTrue(endpoint.contains("hubv5minorversion=v5.1"))
        assertTrue(endpoint.contains("hidelb=true"))
        assertTrue(endpoint.contains("video=v3"))
    }

    @Test
    fun signatureIsDeterministicAndCarriesAValidChecksum() {
        val samples = ShortArray(ShazamSignatureGenerator.SAMPLE_RATE_HZ * 4) { index ->
            val time = index.toDouble() / ShazamSignatureGenerator.SAMPLE_RATE_HZ
            ((sin(2.0 * PI * 440.0 * time) + sin(2.0 * PI * 1_100.0 * time)) * 12_000.0).toInt().toShort()
        }

        val first = ShazamSignatureGenerator().generate(samples)
        val second = ShazamSignatureGenerator().generate(samples)

        assertEquals(first, second)
        requireNotNull(first)
        assertTrue(first.peakCount > 0)
        assertTrue(first.sampleDurationMs > 0L)
        val buffer = ByteBuffer.wrap(first.payload).order(ByteOrder.LITTLE_ENDIAN)
        val storedChecksum = buffer.getInt(4)
        val computedChecksum = CRC32().apply {
            update(first.payload, 8, first.payload.size - 8)
        }.value.toInt()
        assertEquals(computedChecksum, storedChecksum)
    }

    @Test
    fun ungenerableSignatureYieldsFingerprintErrorNotNoMatch() = runBlocking {
        // Loud but shorter than the fingerprint engine's minimum hop size (128 samples), so
        // AudioSignalQuality reports it as non-silent but the engine cannot derive peaks from it.
        val tooShortButLoud = ShortArray(64) { 20_000 }
        val fingerprint = AudioFingerprint(tooShortButLoud, ShazamSignatureGenerator.SAMPLE_RATE_HZ, 4L)
        val provider = ShazamRecognitionProvider(
            endpointFactory = { _, _ -> throw AssertionError("must not build a request for an ungenerable signature") }
        )

        assertEquals(RecognitionOutcome.Error(RecognitionErrorKind.Fingerprint), provider.identify(fingerprint))
    }

    @Test
    fun classify404AsNetworkErrorAnd429StaysUnavailable() {
        assertEquals(RecognitionOutcome.Error(RecognitionErrorKind.Network), classifyShazamHttpFailure(404))
        assertEquals(RecognitionOutcome.Error(RecognitionErrorKind.Unavailable), classifyShazamHttpFailure(429))
    }

    @Test
    fun silentAudioYieldsFingerprintErrorWithZeroHttpCalls() = runBlocking {
        val fakeServer = FakeShazamServer(listOf(MATCH_BODY))
        try {
            val silentSamples = ShortArray(ShazamSignatureGenerator.SAMPLE_RATE_HZ * 4) { 0 }
            val fingerprint = AudioFingerprint(silentSamples, ShazamSignatureGenerator.SAMPLE_RATE_HZ, 4_000L)
            val provider = ShazamRecognitionProvider(endpointFactory = fakeServer.endpointFactory)

            val outcome = provider.identify(fingerprint)

            assertEquals(RecognitionOutcome.Error(RecognitionErrorKind.Fingerprint), outcome)
            assertEquals(0, fakeServer.requestCount.get())
        } finally {
            fakeServer.stop()
        }
    }

    @Test
    fun ladderEscalatesToRungTwoAfterANoMatchAndIssuesTwoRequests() = runBlocking {
        val fakeServer = FakeShazamServer(listOf(NO_MATCH_BODY, MATCH_BODY))
        try {
            val samples = compositeTone(seconds = 12)
            val fingerprint = AudioFingerprint(samples, ShazamSignatureGenerator.SAMPLE_RATE_HZ, 12_000L)
            val provider = ShazamRecognitionProvider(endpointFactory = fakeServer.endpointFactory)

            val outcome = provider.identify(fingerprint)

            assertTrue(outcome is RecognitionOutcome.Match)
            assertEquals(2, fakeServer.requestCount.get())
        } finally {
            fakeServer.stop()
        }
    }

    @Test
    fun ladderShortCircuitsOnFirstRungMatch() = runBlocking {
        val fakeServer = FakeShazamServer(listOf(MATCH_BODY, MATCH_BODY))
        try {
            val samples = compositeTone(seconds = 12)
            val fingerprint = AudioFingerprint(samples, ShazamSignatureGenerator.SAMPLE_RATE_HZ, 12_000L)
            val provider = ShazamRecognitionProvider(endpointFactory = fakeServer.endpointFactory)

            val outcome = provider.identify(fingerprint)

            assertTrue(outcome is RecognitionOutcome.Match)
            assertEquals(1, fakeServer.requestCount.get())
        } finally {
            fakeServer.stop()
        }
    }

    private fun compositeTone(seconds: Int): ShortArray =
        ShortArray(ShazamSignatureGenerator.SAMPLE_RATE_HZ * seconds) { index ->
            val time = index.toDouble() / ShazamSignatureGenerator.SAMPLE_RATE_HZ
            (
                (
                    sin(2.0 * PI * 440.0 * time) +
                        sin(2.0 * PI * 880.0 * time) +
                        0.5 * sin(2.0 * PI * 1_760.0 * time)
                    ) * 12_000.0
                ).toInt().toShort()
        }

    private companion object {
        const val NO_MATCH_BODY = "{}"
        const val MATCH_BODY = """{"track":{"key":"1","title":"Song","subtitle":"Artist"}}"""
    }

    /** Minimal loopback HTTP fake standing in for Shazam's endpoint, serving [bodies] in order. */
    private class FakeShazamServer(private val bodies: List<String>) {
        val requestCount = AtomicInteger(0)
        private val server: HttpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/") { exchange ->
                exchange.requestBody.use { it.readBytes() }
                val index = requestCount.getAndIncrement()
                val payload = bodies.getOrElse(index) { bodies.last() }.toByteArray(Charsets.UTF_8)
                exchange.sendResponseHeaders(200, payload.size.toLong())
                exchange.responseBody.use { it.write(payload) }
            }
            start()
        }

        val endpointFactory: (String, String) -> String = { _, _ -> "http://127.0.0.1:${server.address.port}/tag" }

        fun stop() = server.stop(0)
    }
}
