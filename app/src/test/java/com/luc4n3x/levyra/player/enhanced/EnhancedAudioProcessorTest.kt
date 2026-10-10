package com.luc4n3x.levyra.player.enhanced

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EnhancedAudioProcessorTest {

    private lateinit var processor: EnhancedAudioProcessor
    private val pcm16Format = AudioFormat(44_100, 2, C.ENCODING_PCM_16BIT)
    private val pcmFloatFormat = AudioFormat(48_000, 2, C.ENCODING_PCM_FLOAT)

    @Before
    fun setup() {
        processor = EnhancedAudioProcessor()
    }

    @Test
    fun configure_supportsPcm16AndFloat() {
        val out16 = processor.configure(pcm16Format)
        assertEquals(pcm16Format, out16)
        assertTrue(processor.isActive)

        val outFloat = processor.configure(pcmFloatFormat)
        assertEquals(pcmFloatFormat, outFloat)
        assertTrue(processor.isActive)
    }

    @Test(expected = AudioProcessor.UnhandledAudioFormatException::class)
    fun configure_unsupportedFormat_throwsException() {
        val invalid = AudioFormat(44_100, 0, C.ENCODING_PCM_16BIT)
        processor.configure(invalid)
    }

    @Test
    fun queueInput_userDisabled_bypassesUntouched() {
        processor.configure(pcm16Format)
        processor.userEnabled = false

        val sampleCount = 256
        val buffer = ByteBuffer.allocateDirect(sampleCount * 2 * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until sampleCount * 2) {
            buffer.putShort((i * 100).toShort())
        }
        buffer.flip()

        processor.queueInput(buffer)
        val output = processor.output

        assertEquals(EnhancedAudioBypassReason.USER_DISABLED, processor.metricsState.value.bypassReason)
        assertTrue(processor.metricsState.value.bypassed)
        assertEquals(sampleCount * 4, output.remaining())

        buffer.position(0)
        while (buffer.hasRemaining()) {
            assertEquals(buffer.short, output.short)
        }
    }

    @Test
    fun queueInput_losslessSource_bypassesUntouched() {
        processor.configure(pcm16Format)
        processor.userEnabled = true
        processor.isLosslessSource = true

        val buffer = ByteBuffer.allocateDirect(128).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until 64) buffer.putShort((i * 50).toShort())
        buffer.flip()

        processor.queueInput(buffer)
        val output = processor.output

        assertEquals(EnhancedAudioBypassReason.ALREADY_LOSSLESS, processor.metricsState.value.bypassReason)
        assertTrue(processor.metricsState.value.bypassed)
        assertEquals(128, output.remaining())
    }

    @Test
    fun queueInput_remotePlayback_bypassesUntouched() {
        processor.configure(pcm16Format)
        processor.userEnabled = true
        processor.isRemotePlayback = true

        val buffer = ByteBuffer.allocateDirect(128).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until 64) buffer.putShort((i * 50).toShort())
        buffer.flip()

        processor.queueInput(buffer)
        val output = processor.output

        assertEquals(EnhancedAudioBypassReason.CAST_OR_REMOTE_PLAYBACK, processor.metricsState.value.bypassReason)
        assertTrue(processor.metricsState.value.bypassed)
        assertEquals(128, output.remaining())
    }

    @Test
    fun queueInput_pcmFloat_processesWithoutError() {
        processor.configure(pcmFloatFormat)
        processor.userEnabled = true

        val frames = 128
        val buffer = ByteBuffer.allocateDirect(frames * 2 * 4).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until frames * 2) {
            buffer.putFloat(0.25f)
        }
        buffer.flip()

        processor.queueInput(buffer)
        val output = processor.output

        assertEquals(frames * 2 * 4, output.remaining())
        while (output.hasRemaining()) {
            val s = output.float
            assertFalse(s.isNaN())
            assertTrue(s in -1f..1f)
        }
    }

    @Test
    fun endOfStream_and_flush_handleLifecycleCleanly() {
        processor.configure(pcm16Format)

        assertFalse(processor.isEnded)
        processor.queueEndOfStream()
        assertTrue(processor.isEnded)

        processor.flush(AudioProcessor.StreamMetadata.DEFAULT)
        assertFalse(processor.isEnded)
    }

    @Test
    fun reset_resetsConfiguration() {
        processor.configure(pcm16Format)
        assertTrue(processor.isActive)

        processor.reset()
        assertFalse(processor.isActive)
    }

    @Test
    fun reset_resetsEngineWithoutReleasingReusableProcessorEngine() {
        val engine = TrackingEngine()
        val proc = EnhancedAudioProcessor(dspEngine = engine)
        proc.configure(pcm16Format)

        proc.reset()

        assertEquals(1, engine.resetCount)
        assertEquals(0, engine.releaseCount)
    }

    @Test
    fun watchdog_singleSpike_doesNotTriggerBypass() {
        var callCount = 0
        var spike = false
        val clock = TimeProvider {
            callCount++
            if (spike && callCount % 2 == 0) {
                callCount * 1_000_000L + 10_000_000L
            } else {
                callCount * 1_000_000L
            }
        }
        val proc = EnhancedAudioProcessor(timeProvider = clock)
        proc.configure(pcm16Format)

        val frames = 128
        val buffer = ByteBuffer.allocateDirect(frames * 2 * 2).order(ByteOrder.LITTLE_ENDIAN)

        buffer.position(0)
        proc.queueInput(buffer)
        proc.output
        assertFalse(proc.metricsState.value.bypassReason == EnhancedAudioBypassReason.CPU_OVERLOAD)

        spike = true
        buffer.position(0)
        proc.queueInput(buffer)
        proc.output
        assertFalse("Single spike must not trigger overload bypass", proc.metricsState.value.bypassReason == EnhancedAudioBypassReason.CPU_OVERLOAD)

        spike = false
        buffer.position(0)
        proc.queueInput(buffer)
        proc.output
        assertFalse(proc.metricsState.value.bypassReason == EnhancedAudioBypassReason.CPU_OVERLOAD)
    }

    @Test
    fun watchdog_sustainedOverload_triggersCpuOverloadBypassAndRecovers() {
        var simulatedBlockDurationNs = 10_000_000L
        var time = 0L
        val clock = TimeProvider {
            val t = time
            time += simulatedBlockDurationNs
            t
        }
        val proc = EnhancedAudioProcessor(timeProvider = clock)
        proc.configure(pcm16Format)

        val frames = 128
        val buffer = ByteBuffer.allocateDirect(frames * 2 * 2).order(ByteOrder.LITTLE_ENDIAN)

        repeat(4) {
            buffer.position(0)
            proc.queueInput(buffer)
            proc.output
            assertFalse(proc.metricsState.value.bypassReason == EnhancedAudioBypassReason.CPU_OVERLOAD)
        }

        buffer.position(0)
        proc.queueInput(buffer)
        proc.output
        assertEquals(EnhancedAudioBypassReason.CPU_OVERLOAD, proc.metricsState.value.bypassReason)
        assertTrue(proc.metricsState.value.bypassed)

        repeat(50) {
            buffer.position(0)
            proc.queueInput(buffer)
            proc.output
        }

        simulatedBlockDurationNs = 1_000_000L
        buffer.position(0)
        proc.queueInput(buffer)
        proc.output
        assertFalse(proc.metricsState.value.bypassReason == EnhancedAudioBypassReason.CPU_OVERLOAD)
    }

    private class TrackingEngine : EnhancedAudioEngine {
        override val name: String = "tracking"
        var resetCount = 0
        var releaseCount = 0

        override fun configure(sampleRateHz: Int, channelCount: Int, config: EnhancedAudioConfig) = Unit

        override fun process(
            input: FloatArray,
            output: FloatArray,
            offset: Int,
            frames: Int,
            adaptiveResidualGain: Float,
            stereoCoherence: Float
        ): Boolean {
            System.arraycopy(input, offset, output, offset, frames * 2)
            return true
        }

        override fun reset() {
            resetCount++
        }

        override fun release() {
            releaseCount++
        }
    }
}
