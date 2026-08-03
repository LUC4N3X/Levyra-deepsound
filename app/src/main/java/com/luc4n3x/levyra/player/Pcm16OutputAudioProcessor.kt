package com.luc4n3x.levyra.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/**
 * Restores 16-bit PCM after Levyra's float DSP chain so Media3's downstream processors remain
 * compatible. In particular, SilenceSkippingAudioProcessor only accepts 16-bit PCM.
 */
class Pcm16OutputAudioProcessor : AudioProcessor {
    private var outputFormat = AudioFormat.NOT_SET
    private var buffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT &&
            inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT
        ) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        outputFormat = if (inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT) {
            AudioFormat(inputAudioFormat.sampleRate, inputAudioFormat.channelCount, C.ENCODING_PCM_16BIT)
        } else {
            AudioFormat.NOT_SET
        }
        return outputFormat
    }

    override fun isActive(): Boolean = outputFormat != AudioFormat.NOT_SET

    override fun queueInput(inputBuffer: ByteBuffer) {
        val limit = inputBuffer.limit()
        val inputSize = limit - inputBuffer.position()
        if (inputSize <= 0) {
            outputBuffer = AudioProcessor.EMPTY_BUFFER
            return
        }

        val output = replaceOutputBuffer(inputSize / 4 * 2)
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        while (inputBuffer.remaining() >= 4) {
            val sample = inputBuffer.float
            val bounded = if (sample.isFinite()) sample.coerceIn(-1f, 1f) else 0f
            val pcm16 = (bounded * 32768f)
                .roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            output.putShort(pcm16.toShort())
        }
        inputBuffer.position(limit)
        output.flip()
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        val output = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return output
    }

    override fun isEnded(): Boolean = inputEnded && !outputBuffer.hasRemaining()

    override fun flush(streamMetadata: AudioProcessor.StreamMetadata) {
        clearState()
    }

    override fun reset() {
        clearState()
        outputFormat = AudioFormat.NOT_SET
        buffer = AudioProcessor.EMPTY_BUFFER
    }

    private fun clearState() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
    }

    private fun replaceOutputBuffer(size: Int): ByteBuffer {
        if (buffer.capacity() < size) {
            buffer = ByteBuffer.allocateDirect(size)
        } else {
            buffer.clear()
        }
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        outputBuffer = buffer
        return buffer
    }
}
