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
    private var inputFormat = AudioFormat.NOT_SET
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false
    private var configured = false

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT &&
            inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT
        ) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        inputFormat = inputAudioFormat
        configured = true
        return AudioFormat(
            inputAudioFormat.sampleRate,
            inputAudioFormat.channelCount,
            C.ENCODING_PCM_16BIT
        )
    }

    override fun isActive(): Boolean = configured && inputFormat.encoding == C.ENCODING_PCM_FLOAT

    override fun queueInput(inputBuffer: ByteBuffer) {
        val limit = inputBuffer.limit()
        val inputSize = limit - inputBuffer.position()
        if (inputSize <= 0) {
            outputBuffer = AudioProcessor.EMPTY_BUFFER
            return
        }

        if (inputFormat.encoding == C.ENCODING_PCM_16BIT) {
            val output = replaceOutputBuffer(inputSize)
            output.put(inputBuffer)
            output.flip()
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
        inputFormat = AudioFormat.NOT_SET
        configured = false
    }

    private fun clearState() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
    }

    private fun replaceOutputBuffer(size: Int): ByteBuffer {
        if (outputBuffer.capacity() < size) {
            outputBuffer = ByteBuffer.allocateDirect(size)
        } else {
            outputBuffer.clear()
        }
        outputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        return outputBuffer
    }
}
