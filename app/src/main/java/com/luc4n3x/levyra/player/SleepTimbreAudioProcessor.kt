package com.luc4n3x.levyra.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.abs

@UnstableApi
internal class SleepTimbreAudioProcessor : BaseAudioProcessor() {
    @Volatile
    private var targetSoftness: Float = 0f

    @Volatile
    private var filterSupportedForCurrentStream: Boolean = true

    private var currentSoftness: Float = 0f
    private var lowPassCoeff: Float = 0.25f
    private var filterState = FloatArray(MAX_CHANNELS)

    fun setTargetSoftness(softness: Float) {
        targetSoftness = softness.coerceIn(0f, 1f)
    }

    fun currentTargetSoftness(): Float = targetSoftness

    fun isFilteringSupported(): Boolean = filterSupportedForCurrentStream

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        val validChannels = inputAudioFormat.channelCount in 1..MAX_CHANNELS
        val validSampleRate = inputAudioFormat.sampleRate > 0
        val validEncoding = inputAudioFormat.encoding == C.ENCODING_PCM_16BIT ||
            inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT ||
            inputAudioFormat.encoding == C.ENCODING_PCM_24BIT ||
            inputAudioFormat.encoding == C.ENCODING_PCM_32BIT
        if (!validChannels || !validSampleRate || !validEncoding) {
            filterSupportedForCurrentStream = false
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        filterSupportedForCurrentStream = true
        val cutoffHz = 2_350f
        val dt = 1f / inputAudioFormat.sampleRate.toFloat()
        val rc = 1f / (2f * PI.toFloat() * cutoffHz)
        lowPassCoeff = (dt / (rc + dt)).coerceIn(0.04f, 0.65f)
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val desired = targetSoftness
        if (desired <= 0.0005f && currentSoftness <= 0.0005f) {
            currentSoftness = 0f
            val output = replaceOutputBuffer(remaining)
            output.put(inputBuffer)
            output.flip()
            return
        }

        val channels = inputAudioFormat.channelCount
        val output = replaceOutputBuffer(remaining)
        when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT -> processPcm16(inputBuffer, output, channels, desired)
            C.ENCODING_PCM_FLOAT -> processPcmFloat(inputBuffer, output, channels, desired)
            C.ENCODING_PCM_24BIT -> processPcm24(inputBuffer, output, channels, desired)
            C.ENCODING_PCM_32BIT -> processPcm32(inputBuffer, output, channels, desired)
            else -> output.put(inputBuffer)
        }
        output.flip()
    }

    private fun stepSoftness(desired: Float): Float {
        val diff = desired - currentSoftness
        currentSoftness = if (abs(diff) <= 0.0004f) {
            desired
        } else {
            (currentSoftness + diff * 0.0018f).coerceIn(0f, 1f)
        }
        return currentSoftness
    }

    private fun filterSample(channel: Int, drySample: Float, softness: Float): Float {
        val previous = filterState[channel]
        val lowPassed = previous + lowPassCoeff * (drySample - previous)
        filterState[channel] = if (abs(lowPassed) < 1e-9f) 0f else lowPassed
        val wetMix = (softness * 0.64f).coerceIn(0f, 0.64f)
        return (drySample * (1f - wetMix) + lowPassed * wetMix).coerceIn(-0.9999f, 0.9999f)
    }

    private fun processPcm16(
        input: ByteBuffer,
        output: ByteBuffer,
        channels: Int,
        desired: Float
    ) {
        val frameBytes = channels * 2
        while (input.remaining() >= frameBytes) {
            val softness = stepSoftness(desired)
            for (ch in 0 until channels) {
                val dry = (input.short / 32768f).coerceIn(-1f, 1f)
                val shaped = filterSample(ch, dry, softness)
                val pcm = (shaped * 32767f).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                output.putShort(pcm)
            }
        }
        while (input.hasRemaining()) {
            output.put(input.get())
        }
    }

    private fun processPcmFloat(
        input: ByteBuffer,
        output: ByteBuffer,
        channels: Int,
        desired: Float
    ) {
        val frameBytes = channels * 4
        while (input.remaining() >= frameBytes) {
            val softness = stepSoftness(desired)
            for (ch in 0 until channels) {
                val raw = input.float
                val dry = if (raw.isFinite()) raw.coerceIn(-1f, 1f) else 0f
                val shaped = filterSample(ch, dry, softness)
                output.putFloat(shaped)
            }
        }
        while (input.hasRemaining()) {
            output.put(input.get())
        }
    }

    private fun processPcm24(
        input: ByteBuffer,
        output: ByteBuffer,
        channels: Int,
        desired: Float
    ) {
        val frameBytes = channels * 3
        while (input.remaining() >= frameBytes) {
            val softness = stepSoftness(desired)
            for (ch in 0 until channels) {
                val b0 = input.get().toInt() and 0xFF
                val b1 = input.get().toInt() and 0xFF
                val b2 = input.get().toInt()
                val sampleInt = b0 or (b1 shl 8) or (b2 shl 16)
                val dry = (sampleInt / 8388608f).coerceIn(-1f, 1f)
                val shaped = filterSample(ch, dry, softness)
                val pcm = (shaped * 8388607f).toInt().coerceIn(-8388608, 8388607)
                output.put((pcm and 0xFF).toByte())
                output.put(((pcm shr 8) and 0xFF).toByte())
                output.put(((pcm shr 16) and 0xFF).toByte())
            }
        }
        while (input.hasRemaining()) {
            output.put(input.get())
        }
    }

    private fun processPcm32(
        input: ByteBuffer,
        output: ByteBuffer,
        channels: Int,
        desired: Float
    ) {
        val frameBytes = channels * 4
        while (input.remaining() >= frameBytes) {
            val softness = stepSoftness(desired)
            for (ch in 0 until channels) {
                val dry = (input.int.toDouble() / 2147483648.0).toFloat().coerceIn(-1f, 1f)
                val shaped = filterSample(ch, dry, softness)
                val pcm = (shaped.toDouble() * 2147483647.0).toLong()
                    .coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong())
                    .toInt()
                output.putInt(pcm)
            }
        }
        while (input.hasRemaining()) {
            output.put(input.get())
        }
    }

    override fun onFlush() {
        filterState.fill(0f)
        if (targetSoftness <= 0.0005f) {
            currentSoftness = 0f
        }
    }

    override fun onReset() {
        filterState.fill(0f)
        currentSoftness = 0f
        targetSoftness = 0f
        filterSupportedForCurrentStream = true
    }

    private companion object {
        const val MAX_CHANNELS = 8
    }
}
