package com.luc4n3x.levyra.feature.recognition

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import kotlin.math.PI
import kotlin.math.sin
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
}
