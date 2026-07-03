package com.luc4n3x.levyra.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

class NormalizationAudioProcessor : AudioProcessor {

    @Volatile
    var enabled: Boolean = false
        set(value) {
            field = value
            if (!value) {
                currentGain = 1.0f
                targetGain = 1.0f
            }
        }

    private var isActive = false
    private var inputAudioFormat = AudioFormat.NOT_SET
    private var outputAudioFormat = AudioFormat.NOT_SET
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false

    private var currentGain = 1.0f
    private var targetGain = 1.0f

    private val targetRms = 0.145f
    private val silenceRms = 0.006f
    private val maxBoost = 1.65f
    private val minGain = 0.55f
    private val boostSmoothing = 0.012f
    private val cutSmoothing = 0.18f

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
        val position = inputBuffer.position()
        val limit = inputBuffer.limit()
        val size = limit - position

        if (size <= 0) {
            outputBuffer = AudioProcessor.EMPTY_BUFFER
            return
        }

        val output = replaceOutputBuffer(size)

        if (enabled) {
            normalizePcm16(inputBuffer.asReadOnlyBuffer(), output, size)
        } else {
            output.put(inputBuffer)
        }

        output.flip()
        inputBuffer.position(limit)
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

    private fun normalizePcm16(input: ByteBuffer, output: ByteBuffer, size: Int) {
        input.order(ByteOrder.LITTLE_ENDIAN)
        output.order(ByteOrder.LITTLE_ENDIAN)

        val evenSize = size - (size % 2)
        val sampleCount = evenSize / 2

        if (sampleCount <= 0) {
            while (input.hasRemaining()) {
                output.put(input.get())
            }
            return
        }

        val startPosition = input.position()
        var sumSquares = 0.0

        repeat(sampleCount) {
            val sample = input.short.toFloat() / Short.MAX_VALUE.toFloat()
            sumSquares += sample * sample
        }

        input.position(startPosition)

        val rms = sqrt(sumSquares / sampleCount).toFloat()
        val blockGain = if (rms <= silenceRms) {
            1.0f
        } else {
            (targetRms / rms).coerceIn(minGain, maxBoost)
        }

        targetGain = blockGain
        val smoothing = if (targetGain < currentGain) cutSmoothing else boostSmoothing
        currentGain += (targetGain - currentGain) * smoothing

        repeat(sampleCount) {
            val sample = input.short.toInt()
            val processed = (sample * currentGain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            output.putShort(processed.toShort())
        }

        if (input.hasRemaining()) {
            output.put(input.get())
        }
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

    override fun flush() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
        currentGain = 1.0f
        targetGain = 1.0f
    }

    override fun reset() {
        flush()
        isActive = false
        inputAudioFormat = AudioFormat.NOT_SET
        outputAudioFormat = AudioFormat.NOT_SET
    }
}
