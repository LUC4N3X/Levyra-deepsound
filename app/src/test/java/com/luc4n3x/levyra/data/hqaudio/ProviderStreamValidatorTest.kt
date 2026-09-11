package com.luc4n3x.levyra.data.hqaudio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderStreamValidatorTest {
    @Test
    fun genuine320StreamIsValid() {
        val validation = ProviderStreamValidator.validate(probeResponse(8_066_175L), AudioQualityTier.KBPS_320, 204)
        assertTrue(validation is StreamValidation.Valid)
        validation as StreamValidation.Valid
        assertEquals("audio/mp4", validation.mimeType)
        assertEquals("mp4", validation.container)
        assertEquals("mp4a", validation.codec)
        assertEquals(316, validation.estimatedKbps)
    }

    @Test
    fun fileThatIsReally96KbpsIsNotAccepted320() {
        val validation = ProviderStreamValidator.validate(probeResponse(2_457_549L), AudioQualityTier.KBPS_320, 204)
        assertEquals(StreamRejection.BITRATE_MISMATCH, (validation as StreamValidation.Invalid).rejection)
    }

    @Test
    fun forbiddenHtmlResponseIsRejected() {
        val validation = ProviderStreamValidator.validate(htmlResponse(403), AudioQualityTier.KBPS_320, 204)
        assertEquals(StreamRejection.HTTP_STATUS, (validation as StreamValidation.Invalid).rejection)
        assertEquals(403, validation.statusCode)
    }

    @Test
    fun successfulNonAudioResponseIsRejected() {
        val validation = ProviderStreamValidator.validate(probeResponse(8_000_000L, contentType = "text/html"), AudioQualityTier.KBPS_320, 200)
        assertEquals(StreamRejection.NOT_AUDIO, (validation as StreamValidation.Invalid).rejection)
    }

    @Test
    fun nonMp4ContainerIsRejected() {
        val validation = ProviderStreamValidator.validate(
            probeResponse(8_000_000L, body = ByteArray(64)),
            AudioQualityTier.KBPS_320,
            200
        )
        assertEquals(StreamRejection.UNSUPPORTED_CONTAINER, (validation as StreamValidation.Invalid).rejection)
    }

    @Test
    fun unknownLengthIsRejected() {
        val response = ProviderHttpResponse(206, mapOf("Content-Type" to "audio/mp4"), mp4ProbeBody())
        val validation = ProviderStreamValidator.validate(response, AudioQualityTier.KBPS_160, 200)
        assertEquals(StreamRejection.UNKNOWN_LENGTH, (validation as StreamValidation.Invalid).rejection)
    }

    @Test
    fun fullResponseUsesContentLength() {
        val response = ProviderHttpResponse(
            200,
            mapOf("Content-Type" to "audio/mp4", "Content-Length" to bytesFor(160, 200).toString()),
            mp4ProbeBody()
        )
        assertTrue(ProviderStreamValidator.validate(response, AudioQualityTier.KBPS_160, 200) is StreamValidation.Valid)
    }

    @Test
    fun bitrateToleranceIsStrictAroundNominalTier() {
        assertTrue(ProviderStreamValidator.bitrateMatches(316, AudioQualityTier.KBPS_320))
        assertTrue(ProviderStreamValidator.bitrateMatches(159, AudioQualityTier.KBPS_160))
        assertFalse(ProviderStreamValidator.bitrateMatches(160, AudioQualityTier.KBPS_320))
        assertFalse(ProviderStreamValidator.bitrateMatches(320, AudioQualityTier.KBPS_160))
    }
}
