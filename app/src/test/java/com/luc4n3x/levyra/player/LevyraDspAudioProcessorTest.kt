package com.luc4n3x.levyra.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import org.junit.Assert.assertEquals
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
        assertEquals(outputFormat, SilenceSkippingAudioProcessor().configure(outputFormat))
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

    private fun readFloat(buffer: ByteBuffer): List<Float> {
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        return buildList { while (buffer.remaining() >= 4) add(buffer.float) }
    }
}
