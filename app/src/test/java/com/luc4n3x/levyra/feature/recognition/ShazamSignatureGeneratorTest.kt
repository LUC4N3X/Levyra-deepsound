package com.luc4n3x.levyra.feature.recognition

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShazamSignatureGeneratorTest {

    @Test
    fun generatorReturnsNullForSilentAudio() {
        val silentSamples = ShortArray(ShazamSignatureGenerator.SAMPLE_RATE_HZ * 10) { 0 }

        assertNull(ShazamSignatureGenerator().generate(silentSamples))
    }

    @Test
    fun generatorContinuesPastDurationUntilPeakTargetIsSatisfied() {
        val samples = compositeTone(seconds = 2)
        val signature = ShazamSignatureGenerator(
            maxDurationSeconds = 0.1,
            maxPeaks = Int.MAX_VALUE
        ).generate(samples)

        assertNotNull(signature)
        assertTrue(signature!!.sampleDurationMs >= 1_900L)
    }

    @Test
    fun encodedSignatureUsesShazamMagicHeader() {
        val signature = ShazamSignatureGenerator(
            maxDurationSeconds = 0.8,
            maxPeaks = 1
        ).generate(compositeTone(seconds = 2))

        assertNotNull(signature)
        val header = ByteBuffer.wrap(signature!!.payload).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(0xCAFE2580.toInt(), header.int)
        header.int // CRC32
        header.int // encoded size
        assertEquals(0x94119C00.toInt(), header.int)
    }

    @Test
    fun generatorCanBeReusedAcrossMultipleInvocations() {
        val samples = compositeTone(seconds = 3)
        val generator = ShazamSignatureGenerator(maxDurationSeconds = 2.0, maxPeaks = 50)
        val first = generator.generate(samples)
        val second = generator.generate(samples)

        assertNotNull(first)
        assertNotNull(second)
        assertEquals(first!!.peakCount, second!!.peakCount)
        assertEquals(first.sampleDurationMs, second.sampleDurationMs)
        assertTrue(first.payload.contentEquals(second.payload))
    }

    @Test
    fun emptyOrTooShortAudioReturnsNull() {
        assertNull(ShazamSignatureGenerator().generate(ShortArray(0)))
        assertNull(ShazamSignatureGenerator().generate(ShortArray(100)))
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
}
