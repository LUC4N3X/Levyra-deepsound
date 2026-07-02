package com.luc4n3x.levyra.player

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

class NormalizationAudioProcessor : AudioProcessor {

    var enabled: Boolean = false
        set(value) {
            field = value
            if (!value) {
                currentGain = 1.0f
            }
        }

    private var isActive = false
    private var inputAudioFormat = AudioFormat.NOT_SET
    private var outputAudioFormat = AudioFormat.NOT_SET
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER

    // AGC Parameters
    private var currentGain = 1.0f
    private val targetRms = 0.2f // Target RMS level
    private val maxGain = 4.0f
    private val minGain = 0.25f
    private val alpha = 0.001f // Smoothing factor

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
            if (outputBuffer.capacity() < size) {
                outputBuffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
            } else {
                outputBuffer.clear()
            }
            
            if (enabled) {
                applyAgc(inputBuffer.asReadOnlyBuffer(), outputBuffer)
            } else {
                outputBuffer.put(inputBuffer)
            }
            
            outputBuffer.flip()
            inputBuffer.position(limit) // Consume input
        }
    }

    private fun applyAgc(input: ByteBuffer, output: ByteBuffer) {
        val sampleCount = input.remaining() / 2
        var sumSquares = 0f
        
        // Prima passata: calcolo RMS del blocco
        input.mark()
        for (i in 0 until sampleCount) {
            val sample = input.short.toFloat() / Short.MAX_VALUE
            sumSquares += sample * sample
        }
        input.reset()
        
        val rms = Math.sqrt((sumSquares / sampleCount).toDouble()).toFloat()
        
        // Calcolo gain target
        val targetBlockGain = if (rms > 0.001f) targetRms / rms else 1.0f
        val clampedTargetGain = targetBlockGain.coerceIn(minGain, maxGain)
        
        // Aggiornamento gain morbido
        currentGain = currentGain * (1 - alpha) + clampedTargetGain * alpha
        
        // Seconda passata: applica gain
        for (i in 0 until sampleCount) {
            val sample = input.short.toFloat() / Short.MAX_VALUE
            val processedSample = (sample * currentGain).coerceIn(-1.0f, 1.0f)
            output.putShort((processedSample * Short.MAX_VALUE).toInt().toShort())
        }
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
        currentGain = 1.0f
    }

    override fun reset() {
        flush()
        isActive = false
        inputAudioFormat = AudioFormat.NOT_SET
        outputAudioFormat = AudioFormat.NOT_SET
    }
}
