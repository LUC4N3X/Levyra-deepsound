package com.luc4n3x.levyra.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraDspAudioProcessorTest {
    @Test
    fun flatEqualizerIsTransparentForPcm16() {
        val processor = LevyraEqualizerAudioProcessor().apply {
            outputProfile = LevyraEqualizerAudioProcessor.OutputProfile.USB
            preampDb = 0f
            enabled = true
            setBandLevels(List(10) { 0 })
        }
        processor.configure(AudioFormat(48_000, 2, C.ENCODING_PCM_16BIT))
        val input = pcm16(1_000, -1_000, 12_000, -12_000)

        processor.queueInput(input)
        val samples = readFloat(processor.output).map { (it * 32768f).toInt() }

        listOf(1_000, -1_000, 12_000, -12_000).zip(samples).forEach { (expected, actual) ->
            assertTrue(abs(expected - actual) <= 1)
        }
    }

    @Test
    fun defaultSpeakerFlatEqualizerDoesNotAttenuate() {
        val processor = LevyraEqualizerAudioProcessor().apply {
            enabled = true
            setBandLevels(List(10) { 0 })
        }
        processor.configure(AudioFormat(48_000, 2, C.ENCODING_PCM_16BIT))
        val inputSamples = IntArray(48_000) { 8_000 }

        processor.queueInput(pcm16(*inputSamples))
        val output = readFloat(processor.output).takeLast(2_048)
        val average = output.sum() / output.size * 32_768f

        assertTrue(abs(average - 8_000f) < 100f)
    }

    @Test
    fun pcm16OddSizedBufferDiscardsIncompleteSample() {
        val processor = LevyraEqualizerAudioProcessor().apply { enabled = true }
        processor.configure(AudioFormat(48_000, 2, C.ENCODING_PCM_16BIT))
        val input = ByteBuffer.allocateDirect(5).order(ByteOrder.LITTLE_ENDIAN).apply {
            putShort(1_000.toShort())
            putShort((-1_000).toShort())
            put(0x7f.toByte())
            flip()
        }

        processor.queueInput(input)
        val output = readFloat(processor.output)

        assertEquals(2, output.size)
        assertEquals(input.limit(), input.position())
    }

    @Test
    fun channelInterleavingContinuesAcrossPcm16Buffers() {
        fun processor() = LevyraEqualizerAudioProcessor().apply {
            outputProfile = LevyraEqualizerAudioProcessor.OutputProfile.USB
            preampDb = 0f
            enabled = true
            setBandLevels(List(10) { index -> if (index == 7) 100 else 0 })
            configure(AudioFormat(48_000, 2, C.ENCODING_PCM_16BIT))
        }
        val samples = intArrayOf(12_000, 0, 0, 0, 0, 0, 0, 0)
        val contiguous = processor().let { equalizer ->
            equalizer.queueInput(pcm16(*samples))
            readFloat(equalizer.output)
        }
        val split = processor().let { equalizer ->
            equalizer.queueInput(pcm16(samples.first()))
            val first = readFloat(equalizer.output)
            equalizer.queueInput(pcm16(*samples.drop(1).toIntArray()))
            first + readFloat(equalizer.output)
        }

        contiguous.zip(split).forEach { (expected, actual) ->
            assertTrue(abs(expected - actual) < 1e-7f)
        }
    }

    @Test
    fun channelInterleavingContinuesAcrossFloatBuffers() {
        fun processor() = LevyraEqualizerAudioProcessor().apply {
            outputProfile = LevyraEqualizerAudioProcessor.OutputProfile.USB
            enabled = true
            setBandLevels(List(10) { index -> if (index == 7) 100 else 0 })
            configure(AudioFormat(48_000, 2, C.ENCODING_PCM_FLOAT))
        }
        val samples = floatArrayOf(0.4f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        val contiguous = processor().let { equalizer ->
            equalizer.queueInput(floatPcm(*samples))
            readFloat(equalizer.output)
        }
        val split = processor().let { equalizer ->
            equalizer.queueInput(floatPcm(samples.first()))
            val first = readFloat(equalizer.output)
            equalizer.queueInput(floatPcm(*samples.drop(1).toFloatArray()))
            first + readFloat(equalizer.output)
        }

        contiguous.zip(split).forEach { (expected, actual) ->
            assertTrue(abs(expected - actual) < 1e-7f)
        }
    }

    @Test
    fun equalizerAcceptsFloatPcm() {
        val processor = LevyraEqualizerAudioProcessor().apply {
            outputProfile = LevyraEqualizerAudioProcessor.OutputProfile.USB
        }

        val output = processor.configure(AudioFormat(44_100, 2, C.ENCODING_PCM_FLOAT))

        assertEquals(C.ENCODING_PCM_FLOAT, output.encoding)
    }

    @Test
    fun dspChainRestoresPcm16BeforeMedia3SilenceSkipping() {
        val inputFormat = AudioFormat(48_000, 2, C.ENCODING_PCM_FLOAT)
        val levyraProcessors = listOf(
            NormalizationAudioProcessor(),
            LevyraEqualizerAudioProcessor(),
            StereoSpatialAudioProcessor(),
            TruePeakLimiterAudioProcessor(),
            VisualizerAudioProcessor(),
            Pcm16OutputAudioProcessor()
        )

        val outputFormat = levyraProcessors.fold(inputFormat) { format, processor ->
            processor.configure(format)
        }

        assertEquals(C.ENCODING_PCM_16BIT, outputFormat.encoding)
        SilenceSkippingAudioProcessor().configure(outputFormat)
    }

    @Test
    fun pcm16OutputProcessorConvertsAndClampsFloatSamples() {
        val processor = Pcm16OutputAudioProcessor()
        processor.configure(AudioFormat(48_000, 2, C.ENCODING_PCM_FLOAT))
        val input = ByteBuffer.allocateDirect(5 * 4).order(ByteOrder.LITTLE_ENDIAN).apply {
            listOf(-1.5f, -1f, 0f, 1f, Float.NaN).forEach(::putFloat)
            flip()
        }

        processor.queueInput(input)
        val output = processor.output.order(ByteOrder.LITTLE_ENDIAN)
        val samples = buildList { while (output.remaining() >= 2) add(output.short.toInt()) }

        assertEquals(listOf(-32_768, -32_768, 0, 32_767, 0), samples)
    }

    @Test
    fun pcm16OutputProcessorBypassesAlreadyPcm16Input() {
        val processor = Pcm16OutputAudioProcessor()

        val output = processor.configure(AudioFormat(48_000, 2, C.ENCODING_PCM_16BIT))

        assertEquals(AudioFormat.NOT_SET, output)
        assertFalse(processor.isActive)
    }

    @Test
    fun audioProcessorsReuseDirectOutputBuffers() {
        val equalizer = LevyraEqualizerAudioProcessor().apply {
            configure(AudioFormat(48_000, 2, C.ENCODING_PCM_16BIT))
        }
        equalizer.queueInput(pcm16(1_000, -1_000))
        val equalizerBuffer = equalizer.output.apply { position(limit()) }
        equalizer.queueInput(pcm16(500, -500))
        assertSame(equalizerBuffer, equalizer.output)

        val converter = Pcm16OutputAudioProcessor().apply {
            configure(AudioFormat(48_000, 2, C.ENCODING_PCM_FLOAT))
        }
        converter.queueInput(floatPcm(0.25f, -0.25f))
        val converterBuffer = converter.output.apply { position(limit()) }
        converter.queueInput(floatPcm(0.1f, -0.1f))
        assertSame(converterBuffer, converter.output)

        val limiter = TruePeakLimiterAudioProcessor().apply {
            configure(AudioFormat(48_000, 1, C.ENCODING_PCM_FLOAT))
        }
        limiter.queueInput(floatPcm(*FloatArray(400)))
        val limiterBuffer = limiter.output.apply { position(limit()) }
        limiter.queueInput(floatPcm(*FloatArray(100)))
        assertSame(limiterBuffer, limiter.output)
    }

    @Test
    fun spatialWideningKeepsFullScaleFloatInputInRange() {
        val processor = StereoSpatialAudioProcessor().apply { strength = 100 }
        processor.configure(AudioFormat(48_000, 2, C.ENCODING_PCM_FLOAT))
        val input = floatPcm(1f, -1f)

        processor.queueInput(input)
        val output = readFloat(processor.output)

        assertTrue(output.all { abs(it) <= 1f })
    }

    @Test
    fun limiterDetectsInterSamplePeakBelowSampleCeiling() {
        val processor = TruePeakLimiterAudioProcessor()
        processor.configure(AudioFormat(48_000, 1, C.ENCODING_PCM_FLOAT))
        val samples = FloatArray(600) { index -> if (index % 4 < 2) 0.8f else -0.8f }

        processor.queueInput(floatPcm(*samples))
        val first = readFloat(processor.output)
        processor.queueEndOfStream()
        val all = first + readFloat(processor.output)

        assertEquals(samples.size, all.size)
        assertTrue(all.minOf { abs(it) } < 0.7f)
    }

    @Test
    fun limiterUsesLookaheadForGradualAttack() {
        val processor = TruePeakLimiterAudioProcessor()
        processor.configure(AudioFormat(48_000, 1, C.ENCODING_PCM_FLOAT))
        val samples = FloatArray(241)
        samples[200] = 1.2f

        processor.queueInput(floatPcm(*samples))

        assertTrue(processor.currentGain < 1f)
        assertTrue(processor.currentGain > 0.9f)
    }

    @Test
    fun limiterDrainsFinalLookaheadAndContainsLastFramePeak() {
        val processor = TruePeakLimiterAudioProcessor()
        processor.configure(AudioFormat(48_000, 1, C.ENCODING_PCM_FLOAT))
        val samples = FloatArray(300)
        samples[samples.lastIndex] = 1.2f
        val input = ByteBuffer.allocateDirect(samples.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        samples.forEach(input::putFloat)
        input.flip()

        processor.queueInput(input)
        val first = readFloat(processor.output)
        processor.queueEndOfStream()
        val drained = readFloat(processor.output)
        val all = first + drained

        assertEquals(samples.size, all.size)
        assertTrue(all.maxOf { abs(it) } <= 0.892f)
        assertTrue(processor.isEnded)
    }

    private fun pcm16(vararg samples: Int): ByteBuffer =
        ByteBuffer.allocateDirect(samples.size * 2)
            .order(ByteOrder.LITTLE_ENDIAN)
            .apply {
                samples.forEach { putShort(it.toShort()) }
                flip()
            }

    private fun floatPcm(vararg samples: Float): ByteBuffer =
        ByteBuffer.allocateDirect(samples.size * 4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .apply {
                samples.forEach(::putFloat)
                flip()
            }

    private fun readFloat(buffer: ByteBuffer): List<Float> {
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        return buildList { while (buffer.remaining() >= 4) add(buffer.float) }
    }
}
