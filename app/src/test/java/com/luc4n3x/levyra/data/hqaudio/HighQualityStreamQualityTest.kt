package com.luc4n3x.levyra.data.hqaudio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HighQualityStreamQualityTest {
    private val aac320 = HighQualityStreamQuality.lossy(HighQualityCodec.AAC, 320, 316)
    private val cd = HighQualityStreamQuality(HighQualityCodec.FLAC, null, 880, 44_100, 16, 2)
    private val hiRes96 = HighQualityStreamQuality(HighQualityCodec.FLAC, null, 2_400, 96_000, 24, 2)
    private val hiRes192 = HighQualityStreamQuality(HighQualityCodec.FLAC, null, 4_900, 192_000, 24, 2)

    @Test
    fun jioSaavn320IsLossyAacAt320() {
        assertFalse(aac320.lossless)
        assertEquals(320, aac320.effectiveKbps)
        assertEquals("AAC 320 kbps", aac320.label)
        assertFalse(aac320.isCdQuality)
        assertFalse(aac320.isHiRes)
    }

    @Test
    fun flacIsNeverReportedAsFake320() {
        assertNull(cd.nominalKbps)
        assertEquals(880, cd.effectiveKbps)
        assertEquals("FLAC · 16-bit · 44.1 kHz", cd.label)
        assertTrue(cd.isCdQuality)
        assertFalse(cd.isHiRes)
        assertEquals("FLAC · 24-bit · 96 kHz", hiRes96.label)
        assertTrue(hiRes96.isHiRes)
        assertEquals("FLAC · 24-bit · 192 kHz", hiRes192.label)
        assertTrue(hiRes192.isHiRes)
    }

    @Test
    fun rankOrdersHiResAboveCdAboveLossy() {
        assertTrue(hiRes192.rank > hiRes96.rank)
        assertTrue(hiRes96.rank > cd.rank)
        assertTrue(cd.rank > aac320.rank)
        assertTrue(cd.rank > HighQualityStreamQuality.lossy(HighQualityCodec.MP3, 320, 4_000).rank)
    }

    @Test
    fun losslessIsAcceptedOverAnyLossyNormalStream() {
        assertTrue(HighQualityTierPolicy.accepts(cd, normalKbps = 256, normalAvailable = true))
        assertTrue(HighQualityTierPolicy.accepts(cd, normalKbps = 320, normalAvailable = true))
        assertFalse(HighQualityTierPolicy.accepts(aac320, normalKbps = 320, normalAvailable = true))
        assertTrue(HighQualityTierPolicy.accepts(aac320, normalKbps = 128, normalAvailable = true))
    }

    @Test
    fun weakLossyStreamIsNotStrong() {
        assertTrue(HighQualityTierPolicy.isStrong(aac320))
        assertTrue(HighQualityTierPolicy.isStrong(cd))
        assertFalse(HighQualityTierPolicy.isStrong(HighQualityStreamQuality.lossy(HighQualityCodec.AAC, 160, 160)))
    }

    @Test
    fun flacStreamInfoIsParsedFromProbe() {
        val info = FlacStreamInfo.parse(flacProbeBody(sampleRateHz = 88_200, bitDepth = 24, channels = 2, seconds = 180))!!
        assertEquals(88_200, info.sampleRateHz)
        assertEquals(24, info.bitDepth)
        assertEquals(2, info.channels)
        assertEquals(180.0, info.durationSeconds!!, 0.001)
        assertTrue(info.plausible)
    }

    @Test
    fun flacBehindId3TagIsParsed() {
        val id3 = byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(), 4, 0, 0, 0, 0, 0, 20) + ByteArray(20)
        val info = FlacStreamInfo.parse(id3 + flacProbeBody(sampleRateHz = 44_100, bitDepth = 16))
        assertEquals(44_100, info?.sampleRateHz)
    }

    @Test
    fun nonFlacBodyHasNoStreamInfo() {
        assertNull(FlacStreamInfo.parse(mp4ProbeBody()))
        assertNull(FlacStreamInfo.parse("<html>".toByteArray()))
        assertNull(FlacStreamInfo.parse(ByteArray(0)))
    }

    @Test
    fun validFlacProbeIsAccepted() {
        val validation = ProviderStreamValidator.validateFlac(flacResponse(60_000_000L), 200)
        assertTrue(validation is StreamValidation.Valid)
        assertEquals(2_400, (validation as StreamValidation.Valid).quality.estimatedKbps)
    }

    @Test
    fun truncatedFlacIsRejected() {
        val validation = ProviderStreamValidator.validateFlac(flacResponse(1_000_000L), 200)
        assertEquals(StreamRejection.TRUNCATED, (validation as StreamValidation.Invalid).rejection)
    }

    @Test
    fun flacPreviewIsRejected() {
        val validation = ProviderStreamValidator.validateFlac(flacResponse(9_000_000L, flacProbeBody(seconds = 30)), 200)
        assertEquals(StreamRejection.PREVIEW, (validation as StreamValidation.Invalid).rejection)
    }

    @Test
    fun implausibleFlacHeaderIsRejected() {
        val validation = ProviderStreamValidator.validateFlac(flacResponse(60_000_000L, flacProbeBody(sampleRateHz = 1_000)), 200)
        assertEquals(StreamRejection.INVALID_METADATA, (validation as StreamValidation.Invalid).rejection)
    }

    @Test
    fun flacBitrateAbovePcmCeilingIsRejected() {
        val validation = ProviderStreamValidator.validateFlac(
            flacResponse(200_000_000L, flacProbeBody(sampleRateHz = 44_100, bitDepth = 16)),
            200
        )
        assertEquals(StreamRejection.BITRATE_MISMATCH, (validation as StreamValidation.Invalid).rejection)
    }

    @Test
    fun serverErrorAndUnknownLengthAreRejected() {
        assertEquals(StreamRejection.HTTP_STATUS, (ProviderStreamValidator.validateFlac(htmlResponse(500), 200) as StreamValidation.Invalid).rejection)
        val noLength = ProviderHttpResponse(206, mapOf("Content-Type" to "audio/flac"), flacProbeBody())
        assertEquals(StreamRejection.UNKNOWN_LENGTH, (ProviderStreamValidator.validateFlac(noLength, 200) as StreamValidation.Invalid).rejection)
    }

    @Test
    fun mp3PreviewAndMismatchAreRejected() {
        assertTrue(ProviderStreamValidator.validateMp3(mp3Response(8_000_000L), 320, 200) is StreamValidation.Valid)
        assertEquals(
            StreamRejection.PREVIEW,
            (ProviderStreamValidator.validateMp3(mp3Response(1_200_000L), 320, 200) as StreamValidation.Invalid).rejection
        )
        assertEquals(
            StreamRejection.BITRATE_MISMATCH,
            (ProviderStreamValidator.validateMp3(mp3Response(5_000_000L), 320, 200) as StreamValidation.Invalid).rejection
        )
        assertEquals(
            StreamRejection.NOT_AUDIO,
            (ProviderStreamValidator.validateMp3(mp3Response(8_000_000L, "application/json"), 320, 200) as StreamValidation.Invalid).rejection
        )
    }
}
