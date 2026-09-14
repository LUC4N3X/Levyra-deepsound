package com.luc4n3x.levyra.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VolumeNormalizationAudioProcessorTest {
    @Test
    fun metadataAttenuationAppliesFromFirstSampleOfFreshStream() {
        val processor = processor()
        processor.setYoutubeLoudness(6f, null)

        val output = processor.processPcm16(constant(16_000, frames = 256))

        assertTrue(output.all { abs(it - 8_019) <= 1 })
    }

    @Test
    fun quietMetadataIsNeverBoosted() {
        val processor = processor()
        processor.setYoutubeLoudness(-6f, -20f)
        val input = constant(9_000, frames = 512)

        assertArrayEquals(input, processor.processPcm16(input))
    }

    @Test
    fun untaggedProgrammeWaitsForEvidenceThenSettlesOnStreamingReference() {
        val processor = processor()
        processor.setYoutubeLoudness(null, null)
        val tone = Tone()

        tone.feed(processor, amplitudeDbfs = -6.0, seconds = 2.0)
        assertEquals(1f, processor.appliedGain, 0f)

        tone.feed(processor, amplitudeDbfs = -6.0, seconds = 10.0)
        assertEquals(10.0.pow(-8.0 / 20.0).toFloat(), processor.appliedGain, 0.01f)
    }

    @Test
    fun untaggedQuietProgrammeIsNeverBoosted() {
        val processor = processor()
        processor.setYoutubeLoudness(null, null)

        Tone().feed(processor, amplitudeDbfs = -30.0, seconds = 12.0)

        assertEquals(1f, processor.appliedGain, 0f)
    }

    @Test
    fun untaggedFullScaleProgrammeNeverExceedsInputPeak() {
        val processor = processor()
        processor.setYoutubeLoudness(null, null)

        val peaks = Tone().feed(processor, amplitudeDbfs = -0.5, seconds = 8.0)

        assertTrue(peaks.output <= peaks.input)
        assertTrue(processor.appliedGain < 0.5f)
    }

    @Test
    fun seekWithinSameTrackKeepsSettledGain() {
        val processor = processor()
        processor.setYoutubeLoudness(null, null)
        val tone = Tone()
        tone.feed(processor, amplitudeDbfs = -6.0, seconds = 12.0)
        val settled = processor.appliedGain

        processor.flush(AudioProcessor.StreamMetadata.DEFAULT)
        tone.feed(processor, amplitudeDbfs = -6.0, seconds = 0.2)

        assertEquals(settled, processor.appliedGain, 0.005f)
    }

    @Test
    fun nextUntaggedTrackStartsUnprocessedAfterFlush() {
        val processor = processor()
        processor.setYoutubeLoudness(null, null)
        Tone().feed(processor, amplitudeDbfs = -6.0, seconds = 12.0)

        processor.flush(AudioProcessor.StreamMetadata.DEFAULT)
        processor.setYoutubeLoudness(null, null)
        val input = constant(12_000, frames = 256)

        assertArrayEquals(input, processor.processPcm16(input))
    }

    @Test
    fun crossfadeHandoffGainSeedsNextUntaggedStream() {
        val processor = processor()
        processor.continueFromGain(0.5f)
        processor.setYoutubeLoudness(null, null)

        val output = processor.processPcm16(constant(16_000, frames = 256))

        assertTrue(output.all { abs(it - 8_000) <= 1 })
    }

    @Test
    fun handoffGainIsIgnoredWhenNextTrackHasMetadata() {
        val processor = processor()
        processor.continueFromGain(0.5f)
        processor.setYoutubeLoudness(0f, null)
        val input = constant(16_000, frames = 256)

        assertArrayEquals(input, processor.processPcm16(input))
    }

    @Test
    fun metadataArrivingMidStreamRampsInsteadOfStepping() {
        val processor = processor()
        processor.setYoutubeLoudness(null, null)
        processor.processPcm16(constant(16_000, frames = 24_000))

        processor.setYoutubeLoudness(6f, null)
        val ramp = processor.processPcm16(constant(16_000, frames = 480))

        assertTrue(ramp.first() >= 15_990)
        assertTrue(ramp.last() in 14_900..14_980)
        assertTrue(ramp.toList().zipWithNext().all { (previous, next) -> next <= previous })
        repeat(30) { processor.processPcm16(constant(16_000, frames = 480)) }
        assertEquals(0.5012f, processor.appliedGain, 0.001f)
    }

    @Test
    fun disabledProcessorIsTransparent() {
        val processor = processor().apply { enabled = false }
        processor.setYoutubeLoudness(6f, null)
        val input = constant(20_000, frames = 256)

        assertArrayEquals(input, processor.processPcm16(input))
        assertEquals(1f, processor.appliedGain, 0f)
    }

    @Test
    fun floatInputKeepsOversForDownstreamLimiter() {
        val processor = NormalizationAudioProcessor().apply {
            enabled = true
            configure(AudioFormat(48_000, 2, C.ENCODING_PCM_FLOAT))
        }
        processor.setYoutubeLoudness(0f, null)
        val input = ByteBuffer.allocateDirect(4 * 4).order(ByteOrder.LITTLE_ENDIAN).apply {
            listOf(1.2f, -1.2f, Float.NaN, 0.5f).forEach(::putFloat)
            flip()
        }

        processor.queueInput(input)
        val output = processor.output.order(ByteOrder.LITTLE_ENDIAN)
        val samples = buildList { while (output.remaining() >= 4) add(output.float) }

        assertEquals(listOf(1.2f, -1.2f, 0f, 0.5f), samples)
    }

    private fun processor() = NormalizationAudioProcessor().apply {
        enabled = true
        configure(AudioFormat(SAMPLE_RATE, CHANNELS, C.ENCODING_PCM_16BIT))
    }

    private fun constant(value: Int, frames: Int) = ShortArray(frames * CHANNELS) { value.toShort() }

    private fun VolumeNormalizationAudioProcessor.processPcm16(samples: ShortArray): ShortArray {
        val input = ByteBuffer.allocateDirect(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        samples.forEach(input::putShort)
        input.flip()
        queueInput(input)
        val output = output.order(ByteOrder.LITTLE_ENDIAN)
        return ShortArray(output.remaining() / 2) { output.short }
    }

    private class Peaks(val input: Int, val output: Int)

    private inner class Tone {
        private var phase = 0.0

        fun feed(processor: VolumeNormalizationAudioProcessor, amplitudeDbfs: Double, seconds: Double): Peaks {
            val amplitude = 10.0.pow(amplitudeDbfs / 20.0) * 32_767.0
            val increment = 2.0 * PI * TONE_HZ / SAMPLE_RATE
            var remaining = (seconds * SAMPLE_RATE).toInt()
            var inputPeak = 0
            var outputPeak = 0
            while (remaining > 0) {
                val frames = minOf(CHUNK_FRAMES, remaining)
                val chunk = ShortArray(frames * CHANNELS)
                for (frame in 0 until frames) {
                    val sample = (amplitude * sin(phase)).roundToInt()
                    phase += increment
                    inputPeak = maxOf(inputPeak, abs(sample))
                    for (channel in 0 until CHANNELS) chunk[frame * CHANNELS + channel] = sample.toShort()
                }
                processor.processPcm16(chunk).forEach { outputPeak = maxOf(outputPeak, abs(it.toInt())) }
                remaining -= frames
            }
            return Peaks(inputPeak, outputPeak)
        }
    }

    private companion object {
        const val SAMPLE_RATE = 48_000
        const val CHANNELS = 2
        const val CHUNK_FRAMES = 1_024
        const val TONE_HZ = 997.0
    }
}
