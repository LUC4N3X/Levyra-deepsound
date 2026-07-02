package com.luc4n3x.levyra.player

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

class VisualizerAudioProcessor : AudioProcessor {

    companion object {
        private val _waveformState = MutableStateFlow(FloatArray(0))
        val waveformState: StateFlow<FloatArray> = _waveformState.asStateFlow()
        
        // Settings per downsampling dell'onda (es. vogliamo 60 barre)
        private const val BARS_COUNT = 60
    }

    private var isActive = false
    private var inputAudioFormat = AudioFormat.NOT_SET
    private var outputAudioFormat = AudioFormat.NOT_SET
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != androidx.media3.common.C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        this.inputAudioFormat = inputAudioFormat
        this.outputAudioFormat = inputAudioFormat
        isActive = true
        return inputAudioFormat
    }

    override fun isActive(): Boolean = isActive

    override fun queueInput(inputBuffer: ByteBuffer) {
        val position = inputBuffer.position()
        val limit = inputBuffer.limit()
        val size = limit - position
        
        if (size > 0) {
            // Processing audio to extract waveform
            extractWaveform(inputBuffer.asReadOnlyBuffer())
            
            // Pass through the audio data untouched
            if (outputBuffer.capacity() < size) {
                outputBuffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
            } else {
                outputBuffer.clear()
            }
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
        }
    }

    private fun extractWaveform(buffer: ByteBuffer) {
        val sampleCount = buffer.remaining() / 2 // 16-bit PCM = 2 bytes per sample
        if (sampleCount == 0) return
        
        // Simple decimation for waveform
        val decimationFactor = maxOf(1, sampleCount / BARS_COUNT)
        val wave = FloatArray(BARS_COUNT)
        
        var waveIdx = 0
        var i = 0
        while (i < sampleCount && waveIdx < BARS_COUNT) {
            var sum = 0f
            var count = 0
            for (j in 0 until decimationFactor) {
                if (buffer.remaining() >= 2) {
                    val sample = buffer.short.toFloat() / Short.MAX_VALUE
                    sum += abs(sample)
                    count++
                }
            }
            if (count > 0) {
                wave[waveIdx] = sum / count
            }
            waveIdx++
            i += decimationFactor
        }
        
        _waveformState.value = wave
    }

    override fun queueEndOfStream() {
        // Nothing to do
    }

    override fun getOutput(): ByteBuffer {
        val output = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return output
    }

    override fun isEnded(): Boolean = false

    override fun flush() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
    }

    override fun reset() {
        flush()
        isActive = false
        inputAudioFormat = AudioFormat.NOT_SET
        outputAudioFormat = AudioFormat.NOT_SET
        _waveformState.value = FloatArray(0)
    }
}
