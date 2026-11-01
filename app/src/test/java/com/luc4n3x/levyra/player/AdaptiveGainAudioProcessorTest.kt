package com.luc4n3x.levyra.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import com.luc4n3x.levyra.domain.AdaptiveListeningSnapshot
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveGainAudioProcessorTest {
    @After
    fun tearDown() {
        AdaptiveListeningBus.configure(AdaptiveListeningConfig(enabled = false))
    }

    private fun buffer(frames: Int, value: Short): ByteBuffer {
        val data = ByteBuffer.allocateDirect(frames * 4).order(ByteOrder.LITTLE_ENDIAN)
        repeat(frames) {
            data.putShort(value)
            data.putShort((-value).toShort())
        }
        data.flip()
        return data
    }

    private fun AdaptiveGainAudioProcessor.run(input: ByteBuffer): ByteBuffer {
        queueInput(input)
        return getOutput().order(ByteOrder.LITTLE_ENDIAN)
    }

    @Test
    fun boostedSixteenBitAudioNeverHardClips() {
        AdaptiveListeningBus.configure(AdaptiveListeningConfig(enabled = true, maxBoostDb = 6f))
        AdaptiveListeningBus.publish(AdaptiveListeningSnapshot(boostDb = 6f))
        val processor = AdaptiveGainAudioProcessor(meterOutput = false)
        processor.configure(AudioProcessor.AudioFormat(48_000, 2, C.ENCODING_PCM_16BIT))
        processor.flush(AudioProcessor.StreamMetadata.DEFAULT)
        repeat(700) { processor.run(buffer(1_024, 2_000)) }
        assertTrue(AdaptiveListeningBus.appliedBoostDb > 5.5f)
        val loud = processor.run(buffer(1_024, 30_000))
        var peak = 0
        while (loud.hasRemaining()) peak = maxOf(peak, kotlin.math.abs(loud.getShort().toInt()))
        assertTrue("peak $peak", peak <= (0.98 * 32768).toInt() + 1)
        assertTrue(peak > 30_000)
    }

    @Test
    fun zeroTargetPassesAudioThroughUntouched() {
        AdaptiveListeningBus.configure(AdaptiveListeningConfig(enabled = false))
        val processor = AdaptiveGainAudioProcessor(meterOutput = false)
        processor.configure(AudioProcessor.AudioFormat(48_000, 2, C.ENCODING_PCM_16BIT))
        processor.flush(AudioProcessor.StreamMetadata.DEFAULT)
        val out = processor.run(buffer(256, 12_345))
        assertTrue(out.getShort(0) == 12_345.toShort())
    }
}
