package com.luc4n3x.levyra.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

class LevyraEqualizerAudioProcessor : AudioProcessor {
    enum class OutputProfile { SPEAKER, WIRED, BLUETOOTH, USB }

    @Volatile var enabled: Boolean = false
    @Volatile var preampDb: Float = -3f
    @Volatile var bassBoost: Int = 0
    @Volatile var outputProfile: OutputProfile = OutputProfile.SPEAKER

    @Volatile
    private var requestedLevels = IntArray(BAND_COUNT)

    private var inputFormat = AudioFormat.NOT_SET
    private var outputFormat = AudioFormat.NOT_SET
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false
    private var configured = false
    private var channelStates = emptyArray<Array<FilterState>>()
    private val filters = Array(BAND_COUNT) { Biquad() }
    private val currentDb = FloatArray(BAND_COUNT)
    private var currentMix = 0f

    fun setBandLevels(levels: List<Int>) {
        requestedLevels = IntArray(BAND_COUNT) { index -> levels.getOrElse(index) { 0 }.coerceIn(-100, 100) }
    }

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT && inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        inputFormat = inputAudioFormat
        outputFormat = AudioFormat(inputAudioFormat.sampleRate, inputAudioFormat.channelCount, C.ENCODING_PCM_FLOAT)
        configured = true
        channelStates = Array(inputAudioFormat.channelCount) { Array(BAND_COUNT) { FilterState() } }
        rebuildFilters(force = true)
        return outputFormat
    }

    override fun isActive(): Boolean = configured

    override fun queueInput(inputBuffer: ByteBuffer) {
        val limit = inputBuffer.limit()
        val size = limit - inputBuffer.position()
        if (size <= 0) {
            outputBuffer = AudioProcessor.EMPTY_BUFFER
            return
        }
        val outputSize = if (inputFormat.encoding == C.ENCODING_PCM_16BIT) size / 2 * 4 else size
        val output = replaceOutputBuffer(outputSize)
        rebuildFilters(force = false)
        when (inputFormat.encoding) {
            C.ENCODING_PCM_FLOAT -> processFloat(inputBuffer, output)
            else -> processPcm16(inputBuffer, output)
        }
        output.flip()
        inputBuffer.position(limit)
    }

    private fun processPcm16(input: ByteBuffer, output: ByteBuffer) {
        input.order(ByteOrder.LITTLE_ENDIAN)
        output.order(ByteOrder.LITTLE_ENDIAN)
        var channel = 0
        while (input.remaining() >= 2) {
            val dry = input.short / 32768f
            val wet = processSample(dry, channel)
            output.putFloat(wet)
            channel = (channel + 1) % inputFormat.channelCount
        }
        while (input.hasRemaining()) output.put(input.get())
    }

    private fun processFloat(input: ByteBuffer, output: ByteBuffer) {
        input.order(ByteOrder.LITTLE_ENDIAN)
        output.order(ByteOrder.LITTLE_ENDIAN)
        var channel = 0
        while (input.remaining() >= 4) {
            output.putFloat(processSample(input.float, channel))
            channel = (channel + 1) % inputFormat.channelCount
        }
        while (input.hasRemaining()) output.put(input.get())
    }

    private fun processSample(dry: Float, channel: Int): Float {
        val targetMix = if (enabled) 1f else 0f
        currentMix += (targetMix - currentMix) * MIX_SMOOTHING
        if (currentMix < 0.0001f && !enabled) return dry
        var wet = dry * dbToLinear(effectivePreampDb())
        filters.forEachIndexed { index, filter ->
            val state = channelStates[channel][index]
            val output = filter.b0 * wet + state.z1
            state.z1 = filter.b1 * wet - filter.a1 * output + state.z2
            state.z2 = filter.b2 * wet - filter.a2 * output
            wet = output
        }
        return dry + (wet - dry) * currentMix
    }

    private fun rebuildFilters(force: Boolean) {
        val levels = requestedLevels
        repeat(BAND_COUNT) { index ->
            val userDb = levels[index] / 100f * MAX_BAND_DB
            val bassDb = bassBoost.coerceIn(0, 100) / 100f * BASS_MAX_DB * BASS_WEIGHTS[index]
            val target = userDb + bassDb + routeCompensation(outputProfile, index)
            currentDb[index] = if (force) target else currentDb[index] + (target - currentDb[index]) * COEFFICIENT_SMOOTHING
            updatePeakingFilter(filters[index], inputFormat.sampleRate, FREQUENCIES[index], Q, currentDb[index])
        }
    }

    private fun effectivePreampDb(): Float {
        val highestBoost = currentDb.maxOrNull()?.coerceAtLeast(0f) ?: 0f
        return preampDb.coerceIn(-12f, 3f) - highestBoost
    }

    private fun updatePeakingFilter(filter: Biquad, sampleRate: Int, frequency: Float, q: Float, gainDb: Float) {
        val boundedFrequency = frequency.coerceAtMost(sampleRate * 0.45f)
        val a = 10.0.pow(gainDb / 40.0).toFloat()
        val omega = (2.0 * PI * boundedFrequency / sampleRate).toFloat()
        val alpha = sin(omega) / (2f * q)
        val cosOmega = cos(omega)
        val a0 = 1f + alpha / a
        filter.b0 = (1f + alpha * a) / a0
        filter.b1 = (-2f * cosOmega) / a0
        filter.b2 = (1f - alpha * a) / a0
        filter.a1 = (-2f * cosOmega) / a0
        filter.a2 = (1f - alpha / a) / a0
    }

    private fun routeCompensation(profile: OutputProfile, index: Int): Float = when (profile) {
        OutputProfile.SPEAKER -> SPEAKER_COMPENSATION[index]
        OutputProfile.WIRED -> WIRED_COMPENSATION[index]
        OutputProfile.BLUETOOTH -> BLUETOOTH_COMPENSATION[index]
        OutputProfile.USB -> USB_COMPENSATION[index]
    }

    private fun dbToLinear(db: Float): Float = 10.0.pow(db / 20.0).toFloat()

    override fun queueEndOfStream() { inputEnded = true }

    override fun getOutput(): ByteBuffer {
        val output = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return output
    }

    override fun isEnded(): Boolean = inputEnded && !outputBuffer.hasRemaining()

    override fun flush(streamMetadata: AudioProcessor.StreamMetadata) = clearState()

    override fun reset() {
        clearState()
        configured = false
        inputFormat = AudioFormat.NOT_SET
        outputFormat = AudioFormat.NOT_SET
        channelStates = emptyArray()
    }

    private fun clearState() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
        currentMix = if (enabled) 1f else 0f
        channelStates.forEach { channel -> channel.forEach(FilterState::clear) }
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

    private class Biquad(
        var b0: Float = 1f,
        var b1: Float = 0f,
        var b2: Float = 0f,
        var a1: Float = 0f,
        var a2: Float = 0f
    )

    private class FilterState(var z1: Float = 0f, var z2: Float = 0f) {
        fun clear() { z1 = 0f; z2 = 0f }
    }

    companion object {
        const val BAND_COUNT = 10
        internal val FREQUENCIES = floatArrayOf(31f, 62f, 125f, 250f, 500f, 1_000f, 2_000f, 4_000f, 8_000f, 16_000f)
        private val BASS_WEIGHTS = floatArrayOf(1f, 0.82f, 0.48f, 0.18f, 0f, 0f, 0f, 0f, 0f, 0f)
        private val SPEAKER_COMPENSATION = floatArrayOf(-1.5f, -1f, 0f, 0.5f, 0f, 0f, 0.5f, 0.8f, 0.5f, 0f)
        private val WIRED_COMPENSATION = FloatArray(BAND_COUNT)
        private val BLUETOOTH_COMPENSATION = floatArrayOf(0f, 0f, -0.3f, -0.3f, 0f, 0f, 0.2f, 0.5f, 0.5f, 0f)
        private val USB_COMPENSATION = FloatArray(BAND_COUNT)
        private const val Q = 1.15f
        private const val MAX_BAND_DB = 12f
        private const val BASS_MAX_DB = 6f
        private const val COEFFICIENT_SMOOTHING = 0.22f
        private const val MIX_SMOOTHING = 0.0018f
    }
}
