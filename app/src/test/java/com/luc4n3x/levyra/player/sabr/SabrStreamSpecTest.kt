package com.luc4n3x.levyra.player.sabr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SabrStreamSpecTest {
    private val endpoint = "https://rr5---sn-a.googlevideo.com/videoplayback?mn=sn-a,sn-b&sabr=1"

    @Test
    fun roundTripsThroughTheMediaUri() {
        val spec = spec()

        assertEquals(spec, SabrStreamSpec.parse(spec.toUri()))
    }

    @Test
    fun toleratesTheDescriptiveQuerySuffixTheResolverAppends() {
        val uri = spec().toUri() + "?itag=140&mime=audio%2Fmp4&expire=1788000000"

        assertEquals(spec(), SabrStreamSpec.parse(uri))
    }

    @Test
    fun rejectsEndpointsOutsideTheGooglevideoSurface() {
        val hostile = listOf(
            "http://rr5---sn-a.googlevideo.com/videoplayback",
            "https://evil.test/videoplayback",
            "https://rr5---sn-a.googlevideo.com.evil.test/videoplayback",
            "https://user:pass@rr5---sn-a.googlevideo.com/videoplayback",
            "https://rr5---sn-a.googlevideo.com:8443/videoplayback",
            "https://127.0.0.1/videoplayback",
            "https://localhost/videoplayback",
            "https://googlevideo.com.evil.test/videoplayback",
            ""
        )

        hostile.forEach { url ->
            assertFalse(url, SabrEndpoint.isAllowed(url))
            assertNull(url, SabrStreamSpec.parse(spec(endpointUrl = url).toUri()))
        }
    }

    @Test
    fun acceptsTheGooglevideoEndpointsSabrActuallyUses() {
        assertTrue(SabrEndpoint.isAllowed(endpoint))
        assertTrue(SabrEndpoint.isAllowed("https://rr2---sn-hpa7zns6.googlevideo.com/videoplayback"))
    }

    @Test
    fun rejectsMalformedOrEmptyDescriptors() {
        assertNull(SabrStreamSpec.parse("https://example.test/media.mp4"))
        assertNull(SabrStreamSpec.parse(SabrStreamSpec.SCHEME_PREFIX))
        assertNull(SabrStreamSpec.parse(SabrStreamSpec.SCHEME_PREFIX + "not base64"))
        assertNull(SabrStreamSpec.parse(spec(contentLength = 0L).toUri()))
        assertNull(SabrStreamSpec.parse(spec(durationMs = 0L).toUri()))
        assertNull(SabrStreamSpec.parse(spec(itag = 0).toUri()))
        assertNull(SabrStreamSpec.parse(spec(ustreamerConfig = ByteArray(0)).toUri()))
    }

    @Test
    fun keepsTheCompanionAudioFormatOnlyWhenItIsPresent() {
        assertNotNull(
            SabrStreamSpec.parse(spec(companionAudio = SabrFormatId(140, 7L)).toUri())?.companionAudioFormat
        )
        assertNull(SabrStreamSpec.parse(spec().toUri())?.companionAudioFormat)
    }

    @Test
    fun theUriIsRecognisableWithoutParsing() {
        assertTrue(SabrStreamSpec.isSabrUri(spec().toUri()))
        assertFalse(SabrStreamSpec.isSabrUri("https://rr5---sn-a.googlevideo.com/videoplayback"))
    }

    private fun spec(
        endpointUrl: String = endpoint,
        itag: Int = 140,
        contentLength: Long = 9_397_248L,
        durationMs: Long = 213_090L,
        companionAudio: SabrFormatId? = null,
        ustreamerConfig: ByteArray = byteArrayOf(10, 20, 30, 40)
    ) = SabrStreamSpec(
        endpointUrl = endpointUrl,
        ustreamerConfig = ustreamerConfig,
        format = SabrFormatId(itag, 1_766_955_925_572_207L),
        companionAudioFormat = companionAudio,
        contentLength = contentLength,
        durationMs = durationMs,
        videoTrack = false,
        clientName = 5,
        clientVersion = "20.10.4",
        userAgent = "com.google.ios.youtube/20.10.4"
    )
}
