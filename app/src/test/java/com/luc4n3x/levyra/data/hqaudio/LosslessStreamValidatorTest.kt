package com.luc4n3x.levyra.data.hqaudio

import com.luc4n3x.levyra.domain.AudioQualityPreference
import com.luc4n3x.levyra.domain.PlaybackDeliveryMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LosslessStreamValidatorTest {
    @Test
    fun genuineHiResFlacIsReadFromStreamInfoInsteadOfItsLabel() {
        val result = LosslessStreamValidator.validate(
            flacResponse(sampleRate = 192_000, bitDepth = 24, channels = 2),
            AudioQualityPreference.MAX_QUALITY,
            durationSeconds = 240
        ) as LosslessStreamValidation.Valid

        assertEquals(192_000, result.format.sampleRateHz)
        assertEquals(24, result.format.bitDepth)
        assertEquals(2, result.format.channels)
        assertTrue(result.format.isLossless)
        assertEquals(PlaybackDeliveryMethod.PROGRESSIVE, result.format.deliveryMethod)
    }

    @Test
    fun fakeFlacPayloadAndFakeAtmosAreRejected() {
        val fakeFlac = ProviderHttpResponse(
            206,
            mapOf("Content-Type" to "audio/flac", "Content-Range" to "bytes 0-63/1000000"),
            ByteArray(64)
        )
        assertEquals(
            StreamRejection.UNSUPPORTED_CONTAINER,
            (LosslessStreamValidator.validate(fakeFlac, AudioQualityPreference.CD_LOSSLESS, 200)
                as LosslessStreamValidation.Invalid).rejection
        )
        assertEquals(
            StreamRejection.FAKE_ATMOS,
            (LosslessStreamValidator.validate(
                flacResponse(48_000, 24, 2),
                AudioQualityPreference.DOLBY_ATMOS,
                200
            ) as LosslessStreamValidation.Invalid).rejection
        )
    }

    @Test
    fun dashAtmosRequiresAnEac3Codec() {
        val atmos = dashResponse("ec-3", sampleRate = 48_000, bitDepth = 24, channels = 6, atmos = true)
        val verified = LosslessStreamValidator.validate(atmos, AudioQualityPreference.DOLBY_ATMOS, 200)
            as LosslessStreamValidation.Valid
        assertTrue(verified.format.isAtmos)
        assertTrue(verified.format.isSpatial)
        assertFalse(verified.format.isLossless)

        val plainEac3 = dashResponse("ec-3", sampleRate = 48_000, bitDepth = 24, channels = 6)
        assertEquals(
            StreamRejection.FAKE_ATMOS,
            (LosslessStreamValidator.validate(plainEac3, AudioQualityPreference.DOLBY_ATMOS, 200)
                as LosslessStreamValidation.Invalid).rejection
        )
        val plainFlacDash = dashResponse("flac", sampleRate = 96_000, bitDepth = 24, channels = 2)
        assertEquals(
            StreamRejection.FAKE_ATMOS,
            (LosslessStreamValidator.validate(plainFlacDash, AudioQualityPreference.DOLBY_ATMOS, 200)
                as LosslessStreamValidation.Invalid).rejection
        )
    }

    @Test
    fun cdAndHiResPropertiesRemainDistinct() {
        val cd = LosslessStreamValidator.validate(
            flacResponse(44_100, 16, 2),
            AudioQualityPreference.CD_LOSSLESS,
            200
        ) as LosslessStreamValidation.Valid
        val hiRes = LosslessStreamValidator.validate(
            flacResponse(96_000, 24, 2),
            AudioQualityPreference.HI_RES,
            200
        ) as LosslessStreamValidation.Valid

        assertEquals(16, cd.format.bitDepth)
        assertEquals(44_100, cd.format.sampleRateHz)
        assertEquals(24, hiRes.format.bitDepth)
        assertEquals(96_000, hiRes.format.sampleRateHz)
    }

    @Test
    fun requestedMaxNeverRelabelsARealCdHeaderAsHiRes() {
        val delivered = LosslessStreamValidator.validate(
            flacResponse(44_100, 16, 2),
            AudioQualityPreference.MAX_QUALITY,
            200
        ) as LosslessStreamValidation.Valid

        assertEquals(16, delivered.format.bitDepth)
        assertEquals(44_100, delivered.format.sampleRateHz)
    }

    private fun flacResponse(sampleRate: Int, bitDepth: Int, channels: Int): ProviderHttpResponse {
        val bytes = ByteArray(42)
        bytes[0] = 'f'.code.toByte()
        bytes[1] = 'L'.code.toByte()
        bytes[2] = 'a'.code.toByte()
        bytes[3] = 'C'.code.toByte()
        bytes[4] = 0
        bytes[5] = 0
        bytes[6] = 0
        bytes[7] = 34
        val packed = (sampleRate.toLong() shl 44) or
            ((channels - 1).toLong() shl 41) or
            ((bitDepth - 1).toLong() shl 36)
        for (index in 0 until 8) {
            val shift = (7 - index) * 8
            bytes[18 + index] = (packed ushr shift).toByte()
        }
        return ProviderHttpResponse(
            206,
            mapOf("Content-Type" to "audio/flac", "Content-Range" to "bytes 0-41/12000000"),
            bytes
        )
    }

    private fun dashResponse(
        codec: String,
        sampleRate: Int,
        bitDepth: Int,
        channels: Int,
        atmos: Boolean = false
    ): ProviderHttpResponse {
        val extension = if (atmos) {
            "<SupplementalProperty schemeIdUri=\"tag:dolby.com,2018:dash:EC3_ExtensionType:2018\" value=\"JOC\"/>"
        } else {
            ""
        }
        val xml = """
            <?xml version="1.0"?>
            <MPD><Period><AdaptationSet mimeType="audio/mp4" codecs="$codec" audioSamplingRate="$sampleRate" bitDepth="$bitDepth">
            <AudioChannelConfiguration value="$channels"/>$extension<Representation bandwidth="768000"/>
            </AdaptationSet></Period></MPD>
        """.trimIndent()
        return ProviderHttpResponse(200, mapOf("Content-Type" to "application/dash+xml"), xml.toByteArray())
    }
}
