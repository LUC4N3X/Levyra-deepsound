package com.luc4n3x.levyra.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VisualizerAudioProcessor : AudioProcessor {

    companion object {
        private val _waveformState = MutableStateFlow(FloatArray(0))
        val waveformState: StateFlow<FloatArray> = _waveformState.asStateFlow()
        private const val BARS_COUNT = 60
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
        val position = inputBuffer.position()
        val limit = inputBuffer.limit()
        val size = limit - position

        if (size <= 0) {
            outputBuffer = AudioProcessor.EMPTY_BUFFER
            return
        }

        extractWaveform(inputBuffer.asReadOnlyBuffer())

        val output = replaceOutputBuffer(size)
        output.put(inputBuffer)
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

    private fun extractWaveform(buffer: ByteBuffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        val sampleCount = buffer.remaining() / 2
        if (sampleCount <= 0) return

        val decimationFactor = maxOf(1, sampleCount / BARS_COUNT)
        val wave = FloatArray(BARS_COUNT)

        var waveIndex = 0
        var sampleIndex = 0

        while (sampleIndex < sampleCount && waveIndex < BARS_COUNT) {
            var sum = 0f
            var count = 0

            repeat(decimationFactor) {
                if (buffer.remaining() >= 2) {
                    val sample = buffer.short.toFloat() / Short.MAX_VALUE.toFloat()
                    sum += abs(sample)
                    count++
                }
            }

            if (count > 0) {
                wave[waveIndex] = sum / count
            }

            waveIndex++
            sampleIndex += decimationFactor
        }

        _waveformState.value = wave
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
        _waveformState.value = FloatArray(0)
    }

    private fun clearBufferedState() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
    }
}
