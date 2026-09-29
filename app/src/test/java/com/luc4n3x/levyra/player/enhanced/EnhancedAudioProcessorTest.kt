package com.luc4n3x.levyra.player.enhanced

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
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

        // Verify exact byte equality
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
}
