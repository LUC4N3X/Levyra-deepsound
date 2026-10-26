package com.luc4n3x.levyra.data.hqaudio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LosslessStreamValidatorTest {
    private val hiResHeader = hex(
        "664c6143000000221000100000001200505e2b1103700316d00dc855b4b636043fa58d765c5be1e3f95f"
    )
    private val cdHeader = hex(
        "664c6143000000221200120000064b0042560ac442f000a3f2cc00000000000000000000000000000000"
    )

    @Test
    fun parsesHiResStreaminfo() {
        val info = FlacStreamInfo.parse(hiResHeader + ByteArray(64))!!
        assertEquals(176_400, info.sampleRateHz)
        assertEquals(24, info.bitsPerSample)
        assertEquals(2, info.channels)
        assertEquals(294.0, info.durationSeconds, 1.0)
    }

    @Test
    fun parsesCdStreaminfo() {
        val info = FlacStreamInfo.parse(cdHeader + ByteArray(64))!!
        assertEquals(44_100, info.sampleRateHz)
        assertEquals(16, info.bitsPerSample)
        assertEquals(243.6, info.durationSeconds, 0.5)
    }

    @Test
    fun rejectsNonFlacBodies() {
        assertNull(FlacStreamInfo.parse(mp4ProbeBody()))
        assertNull(FlacStreamInfo.parse(ByteArray(8)))
    }

    @Test
    fun classifiesHiResFlacFromARangedProbe() {
        val response = flacResponse(hiResHeader, totalBytes = 219_519_996L)
        val validation = ProviderStreamValidator.validateLossless(response, durationSeconds = 294)
        validation as StreamValidation.Valid
        assertEquals(AudioQualityTier.LOSSLESS_HI_RES, validation.tier)
        assertEquals("audio/flac", validation.mimeType)
        assertEquals(24, validation.bitDepth)
        assertEquals(176_400, validation.sampleRateHz)
        assertTrue(validation.estimatedKbps > 5_000)
    }

    @Test
    fun classifiesCdFlacAsStandardLossless() {
        val response = flacResponse(cdHeader, totalBytes = 31_788_420L)
        val validation = ProviderStreamValidator.validateLossless(response, durationSeconds = 245)
        validation as StreamValidation.Valid
        assertEquals(AudioQualityTier.LOSSLESS, validation.tier)
        assertEquals(16, validation.bitDepth)
    }

    @Test
    fun rejectsAacServedWhereFlacWasPromised() {
        val response = ProviderHttpResponse(
            206,
            mapOf("Content-Type" to "application/octet-stream", "Content-Range" to "bytes 0-8191/14868756"),
            mp4ProbeBody()
        )
        val validation = ProviderStreamValidator.validateLossless(response, durationSeconds = 369)
        assertEquals(StreamRejection.UNSUPPORTED_CONTAINER, (validation as StreamValidation.Invalid).rejection)
    }

    @Test
    fun rejectsPreviewClipsWhoseSampleCountDisagreesWithTheTrack() {
        val response = flacResponse(cdHeader, totalBytes = 31_788_420L)
        val validation = ProviderStreamValidator.validateLossless(response, durationSeconds = 30)
        assertEquals(StreamRejection.DURATION_MISMATCH, (validation as StreamValidation.Invalid).rejection)
    }

    @Test
    fun acceptsHighlyCompressibleFlacWithAMatchingSampleCount() {
        val response = flacResponse(cdHeader, totalBytes = 3_000_000L)
        val validation = ProviderStreamValidator.validateLossless(response, durationSeconds = 244)
        assertEquals(AudioQualityTier.LOSSLESS, (validation as StreamValidation.Valid).tier)
    }

    @Test
    fun losslessStreamsCarryAFormatLabelInsteadOfANominalBitrate() {
        val stream = ResolvedHighQualityStream(
            providerId = "monochrome",
            providerTrackId = "1",
            url = "https://tracks.monochrome.st/track/1",
            tier = AudioQualityTier.LOSSLESS_HI_RES,
            mimeType = "audio/flac",
            container = "flac",
            codec = "flac",
            contentLength = 219_519_996L,
            estimatedKbps = 5_973,
            expiresAtMs = Long.MAX_VALUE,
            bitDepth = 24,
            sampleRateHz = 176_400
        )
        assertEquals("FLAC 24-bit/176.4 kHz", stream.qualityLabel)
        assertEquals(5_973, stream.displayKbps)
        assertNull(AudioQualityTier.fromKbps(1_411))
    }

    private fun flacResponse(header: ByteArray, totalBytes: Long) = ProviderHttpResponse(
        206,
        mapOf("Content-Type" to "application/octet-stream", "Content-Range" to "bytes 0-8191/$totalBytes"),
        header + ByteArray(8_150)
    )

    private fun hex(value: String): ByteArray = value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
