package com.luc4n3x.levyra.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import com.luc4n3x.levyra.domain.NightListeningPreset
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NightListeningAudioProcessorTest {
    private val format = AudioProcessor.AudioFormat(48_000, 2, C.ENCODING_PCM_FLOAT)

    private fun processor(preset: NightListeningPreset?, audioFormat: AudioProcessor.AudioFormat = format) =
        NightListeningAudioProcessor().apply {
            setConfiguration(preset != null, preset ?: NightListeningPreset.BALANCED, 0f)
            configure(audioFormat)
            flush(AudioProcessor.StreamMetadata.DEFAULT)
        }

    private fun sine(amplitude: Float, frames: Int, phaseStart: Int = 0): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(frames * 8).order(ByteOrder.LITTLE_ENDIAN)
        repeat(frames) { frame ->
            val sample = amplitude * sin(2.0 * PI * 440.0 * (frame + phaseStart) / 48_000.0).toFloat()
            buffer.putFloat(sample)
            buffer.putFloat(sample)
        }
        buffer.flip()
        return buffer
    }

    private fun steadyPeak(target: NightListeningAudioProcessor, amplitude: Float): Float {
        var peak = 0f
        repeat(12) { block ->
            target.queueInput(sine(amplitude, 4_800, block * 4_800))
            val output = target.output.order(ByteOrder.LITTLE_ENDIAN)
            if (block >= 8) {
                while (output.remaining() >= 4) peak = max(peak, abs(output.float))
            }
        }
        return peak
    }

    private fun db(value: Float) = 20f * log10(value)

    @Test
    fun disabledProcessorIsBitTransparent() {
        val target = processor(null)
        val input = sine(0.5f, 4_800)
        val expected = ByteArray(input.remaining()).also { input.duplicate().get(it) }
        target.queueInput(input)
        val output = target.output
        val actual = ByteArray(output.remaining()).also { output.get(it) }
        assertTrue(expected.contentEquals(actual))
        assertFalse(target.processing)
    }

    @Test
    fun everyPresetNarrowsDynamicRangeAndStrongerPresetsNarrowMore() {
        val inputRangeDb = db(0.9f) - db(0.02f)
        var previousRange = inputRangeDb
        NightListeningPreset.entries.forEach { preset ->
            val range = db(steadyPeak(processor(preset), 0.9f)) - db(steadyPeak(processor(preset), 0.02f))
            assertTrue("${preset.name} range $range previous $previousRange", range < previousRange)
            previousRange = range
        }
    }

    @Test
    fun fullScalePeaksAreNotBoostedAboveInputOnceSettled() {
        NightListeningPreset.entries.forEach { preset ->
            assertTrue(preset.name, steadyPeak(processor(preset), 1f) < 1f)
        }
    }

    @Test
    fun telemetryReportsRealReductionOnlyWhileCompressing() {
        val target = processor(NightListeningPreset.NIGHT)
        steadyPeak(target, 0.9f)
        assertTrue(target.processing)
        assertTrue(target.currentGainReductionDb > 10f)
        val quiet = processor(NightListeningPreset.NIGHT)
        steadyPeak(quiet, 0.001f)
        assertEquals(0f, quiet.currentGainReductionDb, 0.01f)
    }

    @Test
    fun enablingAndDisablingRampsWithoutGainStep() {
        val target = processor(null)
        target.queueInput(sine(0.5f, 4_800))
        target.output
        target.setConfiguration(true, NightListeningPreset.NIGHT, 0f)
        target.queueInput(sine(0.5f, 240, 4_800))
        val output = target.output.order(ByteOrder.LITTLE_ENDIAN)
        var previous = 0f
        var largestJump = 0f
        var first = true
        while (output.remaining() >= 8) {
            val left = output.float
            output.float
            if (!first) largestJump = max(largestJump, abs(left - previous))
            previous = left
            first = false
        }
        assertTrue("jump $largestJump", largestJump < 0.08f)
    }

    @Test
    fun sixteenBitOutputNeverWraps() {
        val pcm16 = AudioProcessor.AudioFormat(48_000, 2, C.ENCODING_PCM_16BIT)
        val target = processor(NightListeningPreset.NIGHT, pcm16)
        val buffer = ByteBuffer.allocateDirect(4_800 * 4).order(ByteOrder.LITTLE_ENDIAN)
        repeat(4_800) {
            buffer.putShort(Short.MAX_VALUE)
            buffer.putShort(Short.MIN_VALUE)
        }
        buffer.flip()
        target.queueInput(buffer)
        val output = target.output.order(ByteOrder.LITTLE_ENDIAN)
        while (output.remaining() >= 4) {
            assertTrue(output.short >= 0)
            assertTrue(output.short <= 0)
        }
    }
}
