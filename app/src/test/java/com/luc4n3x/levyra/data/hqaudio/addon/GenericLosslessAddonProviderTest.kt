package com.luc4n3x.levyra.data.hqaudio.addon

import com.luc4n3x.levyra.data.hqaudio.ProviderHttpExchange
import com.luc4n3x.levyra.data.hqaudio.ProviderHttpRequest
import com.luc4n3x.levyra.data.hqaudio.ProviderHttpResponse
import com.luc4n3x.levyra.data.hqaudio.ProviderSearchOutcome
import com.luc4n3x.levyra.data.hqaudio.ProviderStreamOutcome
import com.luc4n3x.levyra.data.hqaudio.StreamRejection
import com.luc4n3x.levyra.domain.AudioQualityPreference
import com.luc4n3x.levyra.domain.AudioQualityRequest
import com.luc4n3x.levyra.domain.AudioStreamPurpose
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenericLosslessAddonProviderTest {
    @Test
    fun genericProtocolReturnsOnlyAHeaderVerifiedFlacStream() = runBlocking {
        val now = 1_800_000_000_000L
        val exchange = ScriptedExchange { request ->
            when {
                request.url == "https://addon.example.org/manifest.json" -> json(
                    """{"resources":["search","stream"],"allowedHosts":["cdn.example.org"]}"""
                )
                request.url.startsWith("https://addon.example.org/search?") -> json(
                    """{"tracks":[{"id":"track-1","title":"Blinding Lights","artist":"The Weeknd","album":"After Hours","duration":200}]}"""
                )
                request.url.startsWith("https://addon.example.org/stream/track-1?") -> json(
                    """{"url":"https://cdn.example.org/audio/track-1.flac","expiresAtMs":${now + 600_000L}}"""
                )
                request.url == "https://cdn.example.org/audio/track-1.flac" -> flacResponse(96_000, 24, 2)
                else -> ProviderHttpResponse(404, emptyMap(), ByteArray(0))
            }
        }
        val provider = GenericLosslessAddonProvider(
            exchange = exchange,
            enabled = { true },
            baseUrl = { "https://addon.example.org" },
            clock = { now }
        )
        val request = AudioQualityRequest(
            preference = AudioQualityPreference.HI_RES,
            purpose = AudioStreamPurpose.DOWNLOAD,
            losslessEnabled = true,
            allowUpgrade = false
        )
        val candidate = ((provider.search("Blinding Lights The Weeknd", request) as ProviderSearchOutcome.Found)
            .candidates.single())

        val stream = (provider.resolveStream(candidate, request) as ProviderStreamOutcome.Resolved).stream

        assertTrue(stream.isLossless)
        assertEquals(96_000, stream.sampleRateHz)
        assertEquals(24, stream.bitDepth)
        assertEquals(AudioQualityPreference.HI_RES, stream.requestedQuality)
        assertTrue(exchange.requests.any { it.url.contains("quality=hi_res") })
        assertEquals(setOf("addon.example.org", "cdn.example.org"), exchange.requests.last().allowedHosts)
    }

    @Test
    fun insecureOrCredentialedAddonUrlsStayDisabled() {
        listOf(
            "http://addon.example.org",
            "https://user:password@addon.example.org",
            "https://addon.example.org:8443"
        ).forEach { url ->
            val provider = GenericLosslessAddonProvider(
                exchange = ScriptedExchange { ProviderHttpResponse(500, emptyMap(), ByteArray(0)) },
                enabled = { true },
                baseUrl = { url }
            )
            assertTrue(
                !provider.isEnabled(
                    AudioQualityRequest(
                        preference = AudioQualityPreference.CD_LOSSLESS,
                        losslessEnabled = true
                    )
                )
            )
        }
    }

    @Test
    fun failedAtmosUrlFallsBackToVerifiedStereoWithoutQuarantiningTheTrack() = runBlocking {
        val now = 1_800_000_000_000L
        val exchange = ScriptedExchange { request ->
            when {
                request.url == "https://addon.example.org/manifest.json" -> json(
                    """{"resources":["search","stream"],"allowedHosts":["cdn.example.org"]}"""
                )
                request.url.startsWith("https://addon.example.org/search?") -> json(
                    """{"tracks":[{"id":"track-1","title":"Blinding Lights","artist":"The Weeknd","duration":200}]}"""
                )
                request.url.contains("/stream/track-1?") && request.url.contains("quality=atmos") -> json(
                    """{"url":"https://cdn.example.org/audio/track-1-atmos.mpd","expiresAtMs":${now + 600_000L}}"""
                )
                request.url.contains("/stream/track-1?") && request.url.contains("quality=max") -> json(
                    """{"url":"https://cdn.example.org/audio/track-1-max.flac","expiresAtMs":${now + 600_000L}}"""
                )
                request.url.endsWith("track-1-atmos.mpd") -> atmosDashResponse()
                request.url.endsWith("track-1-max.flac") -> flacResponse(192_000, 24, 2)
                else -> ProviderHttpResponse(404, emptyMap(), ByteArray(0))
            }
        }
        val provider = GenericLosslessAddonProvider(
            exchange = exchange,
            enabled = { true },
            baseUrl = { "https://addon.example.org" },
            clock = { now }
        )
        val request = AudioQualityRequest(
            preference = AudioQualityPreference.DOLBY_ATMOS,
            losslessEnabled = true
        )
        val candidate = (provider.search("Blinding Lights", request) as ProviderSearchOutcome.Found).candidates.single()
        val first = (provider.resolveStream(candidate, request) as ProviderStreamOutcome.Resolved).stream
        assertTrue(first.isAtmos)

        provider.reportStreamFailure(candidate.providerTrackId, first.url, "decoder unsupported")
        val fallback = (provider.resolveStream(candidate, request) as ProviderStreamOutcome.Resolved).stream

        assertTrue(fallback.isLossless)
        assertTrue(!fallback.isAtmos)
        assertEquals(192_000, fallback.sampleRateHz)
    }

    @Test
    fun expiredProviderUrlsAreRejectedBeforeTheirPayloadIsProbed() = runBlocking {
        val now = 1_800_000_000_000L
        val exchange = ScriptedExchange { request ->
            when {
                request.url.endsWith("/manifest.json") -> json(
                    """{"resources":["search","stream"],"allowedHosts":["cdn.example.org"]}"""
                )
                request.url.contains("/search?") -> json(
                    """{"tracks":[{"id":"track-1","title":"Blinding Lights","artist":"The Weeknd","duration":200}]}"""
                )
                request.url.contains("/stream/track-1?") -> json(
                    """{"url":"https://cdn.example.org/audio/expired.flac","expiresAtMs":${now + 1_000L}}"""
                )
                else -> error("Expired media URL must not be probed")
            }
        }
        val provider = GenericLosslessAddonProvider(
            exchange = exchange,
            enabled = { true },
            baseUrl = { "https://addon.example.org" },
            clock = { now }
        )
        val request = AudioQualityRequest(
            preference = AudioQualityPreference.CD_LOSSLESS,
            losslessEnabled = true,
            allowUpgrade = false
        )
        val candidate = (provider.search("Blinding Lights", request) as ProviderSearchOutcome.Found).candidates.single()
        val outcome = provider.resolveStream(candidate, request) as ProviderStreamOutcome.Unavailable

        assertTrue(StreamRejection.URL_EXPIRED in outcome.rejections)
        assertTrue(exchange.requests.none { it.url.contains("expired.flac") })
    }

    private class ScriptedExchange(
        private val handler: suspend (ProviderHttpRequest) -> ProviderHttpResponse
    ) : ProviderHttpExchange {
        val requests = CopyOnWriteArrayList<ProviderHttpRequest>()

        override suspend fun execute(request: ProviderHttpRequest): ProviderHttpResponse {
            requests += request
            return handler(request)
        }
    }

    private fun json(body: String) = ProviderHttpResponse(
        200,
        mapOf("Content-Type" to "application/json"),
        body.toByteArray()
    )

    private fun flacResponse(sampleRate: Int, bitDepth: Int, channels: Int): ProviderHttpResponse {
        val bytes = ByteArray(42)
        "fLaC".toByteArray().copyInto(bytes)
        bytes[7] = 34
        val packed = (sampleRate.toLong() shl 44) or
            ((channels - 1).toLong() shl 41) or
            ((bitDepth - 1).toLong() shl 36)
        for (index in 0 until 8) bytes[18 + index] = (packed ushr ((7 - index) * 8)).toByte()
        return ProviderHttpResponse(
            206,
            mapOf("Content-Type" to "audio/flac", "Content-Range" to "bytes 0-41/12000000"),
            bytes
        )
    }

    private fun atmosDashResponse(): ProviderHttpResponse {
        val xml = """
            <MPD><Period><AdaptationSet codecs="ec-3" audioSamplingRate="48000" bitDepth="24">
            <AudioChannelConfiguration value="6"/>
            <SupplementalProperty schemeIdUri="tag:dolby.com,2018:dash:EC3_ExtensionType:2018" value="JOC"/>
            <Representation bandwidth="768000"/>
            </AdaptationSet></Period></MPD>
        """.trimIndent()
        return ProviderHttpResponse(
            200,
            mapOf("Content-Type" to "application/dash+xml"),
            xml.toByteArray()
        )
    }
}
