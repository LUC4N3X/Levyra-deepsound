package com.luc4n3x.levyra.data.hqaudio

import com.luc4n3x.levyra.domain.AudioQualityPreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResolvedHighQualityStreamQualityTest {
    @Test
    fun unknownLosslessMetadataDoesNotInventCdSpecs() {
        val stream = stream(
            bitDepth = 0,
            sampleRateHz = 0,
            isLossless = true
        )

        assertEquals("Lossless", stream.qualityLabel)
        assertFalse(stream.qualityLabel.contains("16-bit"))
        assertFalse(stream.qualityLabel.contains("44.1"))
    }

    @Test
    fun exactCdFormatUsesCdLosslessLabel() {
        val stream = stream(
            bitDepth = 16,
            sampleRateHz = 44_100,
            isLossless = true
        )

        assertEquals("CD Lossless 16-bit / 44.1 kHz", stream.qualityLabel)
    }

    @Test
    fun verifiedHiResFormatUsesMeasuredValues() {
        val stream = stream(
            bitDepth = 24,
            sampleRateHz = 96_000,
            isLossless = true
        )

        assertEquals("Hi-Res 24-bit / 96 kHz", stream.qualityLabel)
    }

    @Test
    fun subCdLosslessDoesNotAutomaticallyReplaceAnAvailableHighQualityStream() {
        val stream = stream(
            bitDepth = 8,
            sampleRateHz = 22_050,
            isLossless = true,
            estimatedKbps = 0
        )

        assertFalse(HighQualityTierPolicy.accepts(stream, normalKbps = 320, normalAvailable = true))
    }

    @Test
    fun verifiedCdLosslessCanReplaceAnAvailableLossyStream() {
        val stream = stream(
            bitDepth = 16,
            sampleRateHz = 44_100,
            isLossless = true,
            estimatedKbps = 0
        )

        assertTrue(HighQualityTierPolicy.accepts(stream, normalKbps = 320, normalAvailable = true))
    }

    private fun stream(
        bitDepth: Int,
        sampleRateHz: Int,
        isLossless: Boolean,
        estimatedKbps: Int = 0
    ) = ResolvedHighQualityStream(
        providerId = "lossless-addon",
        providerTrackId = "track-1",
        url = "https://cdn.example.org/audio",
        tier = null,
        mimeType = "audio/flac",
        container = "flac",
        codec = "flac",
        contentLength = 0L,
        estimatedKbps = estimatedKbps,
        expiresAtMs = Long.MAX_VALUE,
        sampleRateHz = sampleRateHz,
        bitDepth = bitDepth,
        channels = 2,
        isLossless = isLossless,
        requestedQuality = AudioQualityPreference.MAX_QUALITY
    )
}
