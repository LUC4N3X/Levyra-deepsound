package com.luc4n3x.levyra.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

class StereoSpatialAudioProcessor : AudioProcessor {
    @Volatile
    var strength: Int = 0
        set(value) {
            field = value.coerceIn(0, 100)
        }

    private var isActive = false
    private var inputAudioFormat = AudioFormat.NOT_SET
    private var outputAudioFormat = AudioFormat.NOT_SET
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        this.inputAudioFormat = inputAudioFormat
        outputAudioFormat = inputAudioFormat
        isActive = true
        return outputAudioFormat
    }

    override fun isActive(): Boolean = isActive

    override fun queueInput(inputBuffer: ByteBuffer) {
        val limit = inputBuffer.limit()
        val size = limit - inputBuffer.position()
        if (size <= 0) {
            outputBuffer = AudioProcessor.EMPTY_BUFFER
            return
        }
        val output = replaceOutputBuffer(size)
        if (strength > 0 && inputAudioFormat.channelCount == 2) {
            processStereo(inputBuffer.asReadOnlyBuffer(), output)
        } else {
            output.put(inputBuffer)
        }
        output.flip()
        inputBuffer.position(limit)
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
        clearBufferedState()
    }

    override fun reset() {
        clearBufferedState()
        isActive = false
        inputAudioFormat = AudioFormat.NOT_SET
        outputAudioFormat = AudioFormat.NOT_SET
    }

    private fun replaceOutputBuffer(size: Int): ByteBuffer {
        outputBuffer = if (outputBuffer.capacity() < size) {
            ByteBuffer.allocateDirect(size)
        } else {
            outputBuffer.clear()
            outputBuffer
        }
        outputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        return outputBuffer
    }

    private fun processStereo(input: ByteBuffer, output: ByteBuffer) {
        input.order(ByteOrder.LITTLE_ENDIAN)
        output.order(ByteOrder.LITTLE_ENDIAN)
        val amount = strength / 100f
        val midGain = 1f - amount * 0.08f
        val sideGain = 1f + amount * 0.75f
        while (input.remaining() >= 4) {
            val left = input.short.toInt()
            val right = input.short.toInt()
            val mid = (left + right) * 0.5f * midGain
            val side = (left - right) * 0.5f * sideGain
            output.putShort(clampSample(mid + side))
            output.putShort(clampSample(mid - side))
        }
        while (input.hasRemaining()) output.put(input.get())
    }

    private fun clampSample(value: Float): Short {
        return value.roundToInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            .toShort()
    }

    private fun clearBufferedState() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
    }
}
