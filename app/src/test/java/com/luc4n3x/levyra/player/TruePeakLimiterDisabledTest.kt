package com.luc4n3x.levyra.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TruePeakLimiterDisabledTest {
    @Test
    fun disabledLimiterIsTransparentAndDrainsLookahead() {
        val processor = TruePeakLimiterAudioProcessor().apply {
            enabled = false
            configure(AudioFormat(48_000, 2, C.ENCODING_PCM_FLOAT))
        }
        val samples = FloatArray(640) { index ->
            when (index % 4) {
                0 -> 0.82f
                1 -> -0.76f
                2 -> 0.31f
                else -> -0.24f
            }
        }

        processor.queueInput(floatPcm(*samples))
        val first = readFloat(processor.output)
        processor.queueEndOfStream()
        val output = first + readFloat(processor.output)

        assertEquals(samples.size, output.size)
        samples.zip(output).forEach { (expected, actual) ->
            assertTrue(abs(expected - actual) < 1e-7f)
        }
        assertEquals(1f, processor.currentGain, 0f)
        assertTrue(processor.isEnded)
    }

    private fun floatPcm(vararg samples: Float): ByteBuffer =
        ByteBuffer.allocateDirect(samples.size * Float.SIZE_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN)
            .apply {
                samples.forEach(::putFloat)
                flip()
            }

    private fun readFloat(buffer: ByteBuffer): List<Float> {
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        return buildList { while (buffer.remaining() >= Float.SIZE_BYTES) add(buffer.float) }
    }
}
