package com.luc4n3x.levyra.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import com.luc4n3x.levyra.domain.ParametricEqBand
import com.luc4n3x.levyra.domain.ParametricEqProfile
import com.luc4n3x.levyra.domain.ParametricFilterType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraParametricEqualizerAudioProcessorTest {
    @Test
    fun `all supported filters produce finite stable coefficients`() {
        ParametricFilterType.entries.forEach { type ->
            val coefficients = LevyraParametricEqualizerAudioProcessor.BiquadCoefficients()

            assertTrue(coefficients.set(ParametricEqBand(1_000f, 8f, 0.7f, type), 48_000))
            listOf(coefficients.b0, coefficients.b1, coefficients.b2, coefficients.a1, coefficients.a2)
                .forEach { assertTrue(it.isFinite()) }
            assertTrue(1f + coefficients.a1 + coefficients.a2 > 0f)
            assertTrue(1f - coefficients.a1 + coefficients.a2 > 0f)
            assertTrue(1f - coefficients.a2 > 0f)
        }
    }

    @Test
    fun `disabled processor is transparent and converts pcm16 to float`() {
        val processor = LevyraParametricEqualizerAudioProcessor()
        val outputFormat = processor.configure(AudioFormat(48_000, 2, C.ENCODING_PCM_16BIT))
        processor.flush(AudioProcessor.StreamMetadata.DEFAULT)
        processor.queueInput(pcm16(1_000, -2_000, 16_384, -16_384))

        assertEquals(C.ENCODING_PCM_FLOAT, outputFormat.encoding)
        val actual = readFloat(processor.output)
        val expected = listOf(1_000f, -2_000f, 16_384f, -16_384f).map { it / 32_768f }
        expected.zip(actual).forEach { (left, right) -> assertEquals(left, right, 1e-7f) }
    }

    @Test
    fun `preamp is applied once without automatic duplicate attenuation`() {
        val processor = LevyraParametricEqualizerAudioProcessor().apply {
            setConfiguration(true, profile(preampDb = -6f, ParametricEqBand(1_000f, 0f, 1f, ParametricFilterType.PEAK)))
            configure(AudioFormat(48_000, 1, C.ENCODING_PCM_FLOAT))
            flush(AudioProcessor.StreamMetadata.DEFAULT)
        }
        processor.queueInput(floatPcm(*FloatArray(512) { 0.5f }))

        val expected = 0.5 * 10.0.pow(-6.0 / 20.0)
        assertEquals(expected, readFloat(processor.output).takeLast(64).average(), 1e-5)
    }

    @Test
    fun `filters above nyquist are skipped safely`() {
        val processor = LevyraParametricEqualizerAudioProcessor().apply {
            setConfiguration(true, profile(0f, ParametricEqBand(96_000f, 12f, 1f, ParametricFilterType.PEAK)))
            configure(AudioFormat(44_100, 1, C.ENCODING_PCM_FLOAT))
            flush(AudioProcessor.StreamMetadata.DEFAULT)
        }
        val input = floatArrayOf(0.2f, -0.4f, 0.8f)
        processor.queueInput(floatPcm(*input))

        input.zip(readFloat(processor.output)).forEach { (left, right) -> assertEquals(left, right, 1e-7f) }
    }

    @Test
    fun `filter histories stay isolated across channels and buffers`() {
        fun create() = LevyraParametricEqualizerAudioProcessor().apply {
            setConfiguration(true, profile(0f, ParametricEqBand(2_000f, 10f, 1f, ParametricFilterType.PEAK)))
            configure(AudioFormat(48_000, 2, C.ENCODING_PCM_FLOAT))
            flush(AudioProcessor.StreamMetadata.DEFAULT)
        }
        val interleaved = FloatArray(40).apply { this[0] = 0.8f }
        val contiguous = create().let { processor ->
            processor.queueInput(floatPcm(*interleaved))
            readFloat(processor.output)
        }
        val split = create().let { processor ->
            processor.queueInput(floatPcm(*interleaved.take(7).toFloatArray()))
            val first = readFloat(processor.output)
            processor.queueInput(floatPcm(*interleaved.drop(7).toFloatArray()))
            first + readFloat(processor.output)
        }

        contiguous.zip(split).forEach { (left, right) -> assertTrue(abs(left - right) < 1e-7f) }
        contiguous.filterIndexed { index, _ -> index % 2 == 1 }.forEach { assertTrue(abs(it) < 1e-7f) }
    }

    private fun profile(preampDb: Float, vararg bands: ParametricEqBand) = ParametricEqProfile(
        id = "test",
        name = "Test",
        preampDb = preampDb,
        bands = bands.toList()
    )

    private fun pcm16(vararg samples: Int): ByteBuffer =
        ByteBuffer.allocateDirect(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN).apply {
            samples.forEach { putShort(it.toShort()) }
            flip()
        }

    private fun floatPcm(vararg samples: Float): ByteBuffer =
        ByteBuffer.allocateDirect(samples.size * 4).order(ByteOrder.LITTLE_ENDIAN).apply {
            samples.forEach(::putFloat)
            flip()
        }

    private fun readFloat(buffer: ByteBuffer): List<Float> = buildList {
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        while (buffer.remaining() >= 4) add(buffer.float)
    }
}
