package com.luc4n3x.levyra.feature.recognition

import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShazamSignatureGeneratorTest {

    @Test
    fun generatorRespectsDurationLimitOnSilentAudio() {
        val silentSamples = ShortArray(ShazamSignatureGenerator.SAMPLE_RATE_HZ * 10) { 0 }
        val generator = ShazamSignatureGenerator(maxDurationSeconds = 3.1)
        val sig = generator.generate(silentSamples)

        if (sig != null) {
            assertTrue(sig.sampleDurationMs <= 3200L)
        }
    }

    @Test
    fun generatorHaltsWhenEitherDurationOrPeakLimitIsReached() {
        val toneSamples = ShortArray(ShazamSignatureGenerator.SAMPLE_RATE_HZ * 5) { index ->
            val time = index.toDouble() / ShazamSignatureGenerator.SAMPLE_RATE_HZ
            ((sin(2.0 * PI * 440.0 * time) + sin(2.0 * PI * 880.0 * time)) * 15_000.0).toInt().toShort()
        }

        val limitedByDuration = ShazamSignatureGenerator(maxDurationSeconds = 2.0, maxPeaks = 1000).generate(toneSamples)
        assertNotNull(limitedByDuration)
        assertTrue(limitedByDuration!!.sampleDurationMs <= 2100L)

        val limitedByPeaks = ShazamSignatureGenerator(maxDurationSeconds = 10.0, maxPeaks = 10).generate(toneSamples)
        assertNotNull(limitedByPeaks)
        assertTrue(limitedByPeaks!!.peakCount in 1..10)
    }

    @Test
    fun generatorCanBeReusedAcrossMultipleInvocations() {
        val toneSamples = ShortArray(ShazamSignatureGenerator.SAMPLE_RATE_HZ * 3) { index ->
            val time = index.toDouble() / ShazamSignatureGenerator.SAMPLE_RATE_HZ
            ((sin(2.0 * PI * 440.0 * time) + sin(2.0 * PI * 880.0 * time)) * 15_000.0).toInt().toShort()
        }
        val generator = ShazamSignatureGenerator(maxDurationSeconds = 2.0, maxPeaks = 50)
        val first = generator.generate(toneSamples)
        val second = generator.generate(toneSamples)

        assertNotNull(first)
        assertNotNull(second)
        assertEquals(first!!.peakCount, second!!.peakCount)
        assertEquals(first.sampleDurationMs, second.sampleDurationMs)
    }

    @Test
    fun emptyOrOversizedAudioHandling() {
        assertNull(ShazamSignatureGenerator().generate(ShortArray(0)))
        assertNull(ShazamSignatureGenerator().generate(ShortArray(100)))
    }
}
