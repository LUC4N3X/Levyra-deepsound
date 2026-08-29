package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.PlaybackDeliveryMethod
import com.luc4n3x.levyra.domain.PlaybackStreamKind
import com.luc4n3x.levyra.player.sabr.SabrStreamSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackSabrDeliveryTest {
    private val endpoint = "https://rr5---sn-a.googlevideo.com/videoplayback?mn=sn-a,sn-b&expire=1788000000"
    private val ustreamer = byteArrayOf(1, 2, 3, 4)
    private val expiresAtMs = 1_788_000_000_000L

    @Test
    fun audioDescriptorCarriesEnoughForTheDataSourceAndForLevyraUrlChecks() {
        val descriptor = descriptor(audio(itag = 140))

        assertNotNull(descriptor)
        assertEquals(PlaybackDeliveryMethod.SABR, descriptor!!.deliveryMethod)
        assertEquals(PlaybackStreamKind.AUDIO, descriptor.kind)
        assertEquals("audio/mp4", descriptor.mimeType)
        assertEquals(140, descriptor.itag)
        assertTrue(descriptor.url.contains("mime=audio%2Fmp4"))
        assertTrue(descriptor.url.contains("expire=1788000000"))
        assertTrue(SabrStreamSpec.isSabrUri(descriptor.url))

        val spec = SabrStreamSpec.parse(descriptor.url)
        assertNotNull(spec)
        assertEquals(140, spec!!.format.itag)
        assertEquals(9_397_248L, spec.contentLength)
        assertEquals(213_090L, spec.durationMs)
        assertNull(spec.companionAudioFormat)
    }

    @Test
    fun videoDescriptorAlwaysCarriesThePairedAudioFormat() {
        val withAudio = descriptor(video(itag = 133, height = 240), companionAudio = audio(itag = 140))
        val withoutAudio = descriptor(video(itag = 133, height = 240), companionAudio = null)

        assertNotNull(withAudio)
        assertNull(withoutAudio)
        val spec = SabrStreamSpec.parse(withAudio!!.url)
        assertEquals(140, spec?.companionAudioFormat?.itag)
        assertTrue(spec?.videoTrack == true)
    }

    @Test
    fun descriptorsAreRefusedWhenTheDeliveryContextIsIncomplete() {
        assertNull(descriptor(audio(itag = 140), endpointUrl = "https://evil.test/videoplayback"))
        assertNull(descriptor(audio(itag = 140), ustreamerConfig = ByteArray(0)))
        assertNull(descriptor(audio(itag = 140), durationMs = 0L))
        assertNull(descriptor(audio(itag = 140, lastModified = 0L)))
        assertNull(descriptor(audio(itag = 0)))
        assertNull(descriptor(audio(itag = 140, contentLength = 0L)))
        assertNull(descriptor(audio(itag = 140, mimeType = "text/html")))
    }

    @Test
    fun audioOrderingFollowsTheRequestedQuality() {
        val candidates = listOf(
            audio(itag = 139, averageBitrate = 48_000),
            audio(itag = 140, averageBitrate = 128_000),
            video(itag = 133, height = 240)
        )

        assertEquals(
            listOf(140, 139),
            orderSabrAudioCandidates(candidates, preferHighestBitrate = true).map { it.itag }
        )
        assertEquals(
            listOf(139, 140),
            orderSabrAudioCandidates(candidates, preferHighestBitrate = false).map { it.itag }
        )
    }

    @Test
    fun videoOrderingNeverExceedsTheHeightTheDeviceWasAllowed() {
        val candidates = listOf(
            video(itag = 137, height = 1080),
            video(itag = 136, height = 720),
            video(itag = 133, height = 240),
            audio(itag = 140)
        )

        assertEquals(
            listOf(136, 133),
            orderSabrVideoCandidates(candidates, maxHeight = 720).map { it.itag }
        )
        assertEquals(
            listOf(137, 136, 133),
            orderSabrVideoCandidates(candidates, maxHeight = 0).map { it.itag }
        )
        assertTrue(orderSabrVideoCandidates(candidates, maxHeight = 100).isEmpty())
    }

    @Test
    fun ustreamerConfigAcceptsUrlSafeBase64WithOrWithoutPadding() {
        assertEquals("hi", decodeSabrUstreamerConfig("aGk=")?.toString(Charsets.UTF_8))
        assertEquals("hi", decodeSabrUstreamerConfig("aGk")?.toString(Charsets.UTF_8))
        assertTrue(decodeSabrUstreamerConfig("Cs0JCowGCAAQgAUY6AIl-n6qPi0AAIA_")!!.isNotEmpty())
        assertNull(decodeSabrUstreamerConfig(""))
        assertNull(decodeSabrUstreamerConfig("   "))
        assertNull(decodeSabrUstreamerConfig("!!!"))
    }

    private fun descriptor(
        candidate: SabrFormatCandidate,
        companionAudio: SabrFormatCandidate? = null,
        endpointUrl: String = endpoint,
        ustreamerConfig: ByteArray = ustreamer,
        durationMs: Long = 213_090L
    ) = buildSabrStreamDescriptor(
        endpointUrl = endpointUrl,
        ustreamerConfig = ustreamerConfig,
        candidate = candidate,
        companionAudio = companionAudio,
        durationMs = durationMs,
        clientName = 5,
        clientVersion = "20.10.4",
        userAgent = "com.google.ios.youtube/20.10.4",
        expiresAtMs = expiresAtMs
    )

    private fun audio(
        itag: Int,
        lastModified: Long = 1_766_955_925_572_207L,
        contentLength: Long = 9_397_248L,
        averageBitrate: Int = 128_000,
        mimeType: String = "audio/mp4; codecs=\"mp4a.40.2\""
    ) = SabrFormatCandidate(
        itag = itag,
        lastModified = lastModified,
        mimeType = mimeType,
        contentLength = contentLength,
        averageBitrate = averageBitrate
    )

    private fun video(itag: Int, height: Int) = SabrFormatCandidate(
        itag = itag,
        lastModified = 1_766_961_065_074_107L,
        mimeType = "video/mp4; codecs=\"avc1.4d401f\"",
        contentLength = 84_074L,
        averageBitrate = height * 1_000,
        height = height,
        width = height * 16 / 9
    )
}
