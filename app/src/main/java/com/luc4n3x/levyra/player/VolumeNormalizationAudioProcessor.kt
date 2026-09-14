package com.luc4n3x.levyra.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

/*
 * Originally inspired by Metrolist's GPL-3.0 volume normalization processor.
 * Rewritten around attenuation-only YouTube loudness metadata and a gated BS.1770-4 fallback estimate.
 */
@UnstableApi
open class VolumeNormalizationAudioProcessor : AudioProcessor {

    @Volatile
    var enabled: Boolean = false

    @Volatile
    var appliedGain: Float = 1f
        private set

    @Volatile
    private var request = LoudnessRequest.NONE

    @Volatile
    private var handoffGain = Float.NaN

    private val meter = BroadcastLoudnessMeter()
    private var inputAudioFormat = AudioFormat.NOT_SET
    private var bytesPerSample = 0
    private var configured = false
    private var buffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false
    private var currentGain = 1f
    private var generationStartGain = 1f
    private var appliedGeneration = UNSYNCED_GENERATION
    private var framesSinceFlush = 0L

    fun setYoutubeLoudness(loudnessDb: Float?, perceptualLoudnessDb: Float?) {
        request = LoudnessRequest(
            fixedGain = relativeLoudnessDb(loudnessDb, perceptualLoudnessDb)?.let(::attenuationForRelativeLoudness),
            generation = request.generation + 1
        )
    }

    fun setTargetGain(gainMb: Int) {
        request = LoudnessRequest(
            fixedGain = 10.0.pow(gainMb / 2000.0).toFloat().coerceIn(MIN_GAIN, 1f),
            generation = request.generation + 1
        )
    }

    fun continueFromGain(gain: Float) {
        handoffGain = if (gain.isFinite()) gain.coerceIn(MIN_GAIN, 1f) else Float.NaN
    }

    internal fun metadataGain(): Float? = request.fixedGain

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.channelCount <= 0 || inputAudioFormat.sampleRate <= 0) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }

        bytesPerSample = when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT -> 2
            C.ENCODING_PCM_24BIT -> 3
            C.ENCODING_PCM_32BIT -> 4
            C.ENCODING_PCM_FLOAT -> 4
            else -> throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }

        this.inputAudioFormat = inputAudioFormat
        meter.configure(inputAudioFormat.sampleRate, inputAudioFormat.channelCount)
        appliedGeneration = UNSYNCED_GENERATION
        framesSinceFlush = 0L
        configured = true
        return inputAudioFormat
    }

    override fun isActive(): Boolean = configured

    override fun queueInput(inputBuffer: ByteBuffer) {
        val inputSize = inputBuffer.remaining()
        if (inputSize <= 0) {
            outputBuffer = AudioProcessor.EMPTY_BUFFER
            return
        }

        val output = replaceOutputBuffer(inputSize)
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        output.order(ByteOrder.LITTLE_ENDIAN)

        val frameSize = bytesPerSample * inputAudioFormat.channelCount
        val frames = if (frameSize > 0) inputSize / frameSize else 0
        if (!enabled || frames == 0) {
            if (!enabled) bypass(frames)
            output.put(inputBuffer)
            output.flip()
            return
        }

        val active = request
        synchronizeRequest(active)
        val measuring = active.fixedGain == null
        val endGain = plannedGain(active, frames)
        processFrames(inputBuffer, output, frames, currentGain, endGain, measuring)
        currentGain = endGain
        appliedGain = endGain
        framesSinceFlush += frames

        while (inputBuffer.hasRemaining()) {
            output.put(inputBuffer.get())
        }
        output.flip()
    }

    private fun bypass(frames: Int) {
        currentGain = 1f
        appliedGain = 1f
        appliedGeneration = UNSYNCED_GENERATION
        framesSinceFlush += frames
    }

    private fun synchronizeRequest(active: LoudnessRequest) {
        if (active.generation == appliedGeneration) return
        appliedGeneration = active.generation
        meter.reset()
        val handoff = handoffGain
        handoffGain = Float.NaN
        generationStartGain = active.fixedGain ?: handoff.takeUnless { it.isNaN() } ?: 1f
        if (framesSinceFlush == 0L) currentGain = generationStartGain
    }

    private fun plannedGain(active: LoudnessRequest, frames: Int): Float {
        val fixedGain = active.fixedGain
        val measuredGain = if (fixedGain == null && meter.measuredBlocks >= MIN_MEASURED_BLOCKS) {
            attenuationForMeasuredLoudness(meter.integratedLoudnessLufs())
        } else {
            null
        }
        val target = fixedGain ?: measuredGain ?: generationStartGain
        val rateDbPerSecond = when {
            measuredGain == null -> TRANSITION_RATE_DB_PER_SECOND
            target < currentGain -> CUT_RATE_DB_PER_SECOND
            else -> RELEASE_RATE_DB_PER_SECOND
        }
        return slewGain(currentGain, target, rateDbPerSecond * frames / inputAudioFormat.sampleRate)
    }

    private fun processFrames(
        input: ByteBuffer,
        output: ByteBuffer,
        frames: Int,
        startGain: Float,
        endGain: Float,
        measuring: Boolean
    ) {
        val channels = inputAudioFormat.channelCount
        val step = (endGain - startGain) / frames
        var gain = startGain
        for (frame in 0 until frames) {
            gain = if (frame == frames - 1) endGain else gain + step
            for (channel in 0 until channels) {
                transferSample(input, output, gain, channel, measuring)
            }
        }
    }

    private fun transferSample(
        input: ByteBuffer,
        output: ByteBuffer,
        gain: Float,
        channel: Int,
        measuring: Boolean
    ) {
        when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT -> {
                val sample = input.short.toInt()
                if (measuring) meter.push(sample * PCM_16_SCALE, channel)
                val processed = (sample * gain).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                output.putShort(processed.toShort())
            }

            C.ENCODING_PCM_24BIT -> {
                val sample = readPcm24(input)
                if (measuring) meter.push(sample * PCM_24_SCALE, channel)
                writePcm24(output, (sample * gain).roundToInt().coerceIn(PCM_24_MIN, PCM_24_MAX))
            }

            C.ENCODING_PCM_32BIT -> {
                val sample = input.int
                if (measuring) meter.push(sample * PCM_32_SCALE, channel)
                val processed = (sample.toDouble() * gain)
                    .coerceIn(Int.MIN_VALUE.toDouble(), Int.MAX_VALUE.toDouble())
                    .toLong()
                    .toInt()
                output.putInt(processed)
            }

            C.ENCODING_PCM_FLOAT -> {
                val raw = input.float
                val sample = if (raw.isFinite()) raw else 0f
                if (measuring) meter.push(sample.toDouble(), channel)
                output.putFloat(sample * gain)
            }
        }
    }

    private fun readPcm24(input: ByteBuffer): Int {
        val b0 = input.get().toInt() and 0xFF
        val b1 = input.get().toInt() and 0xFF
        val b2 = input.get().toInt()
        return (b2 shl 16) or (b1 shl 8) or b0
    }

    private fun writePcm24(output: ByteBuffer, sample: Int) {
        output.put((sample and 0xFF).toByte())
        output.put(((sample shr 8) and 0xFF).toByte())
        output.put(((sample shr 16) and 0xFF).toByte())
    }

    private fun replaceOutputBuffer(size: Int): ByteBuffer {
        if (buffer.capacity() < size) {
            buffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
        } else {
            buffer.clear()
        }
        outputBuffer = buffer
        return buffer
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
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
        framesSinceFlush = 0L
        meter.clearFilterHistory()
    }

    override fun reset() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
        inputAudioFormat = AudioFormat.NOT_SET
        bytesPerSample = 0
        configured = false
        buffer = AudioProcessor.EMPTY_BUFFER
        currentGain = 1f
        appliedGain = 1f
        generationStartGain = 1f
        appliedGeneration = UNSYNCED_GENERATION
        framesSinceFlush = 0L
        meter.reset()
    }

    private class LoudnessRequest(val fixedGain: Float?, val generation: Int) {
        companion object {
            val NONE = LoudnessRequest(fixedGain = null, generation = 0)
        }
    }

    companion object {
        internal const val STREAMING_REFERENCE_LUFS = -14f
        private const val MIN_GAIN = 0.25f
        private const val MIN_MEASURED_BLOCKS = 20
        private const val TRANSITION_RATE_DB_PER_SECOND = 60f
        private const val CUT_RATE_DB_PER_SECOND = 3f
        private const val RELEASE_RATE_DB_PER_SECOND = 1f
        private const val UNSYNCED_GENERATION = -1
        private const val PCM_24_MIN = -8_388_608
        private const val PCM_24_MAX = 8_388_607
        private const val PCM_16_SCALE = 1.0 / 32_768.0
        private const val PCM_24_SCALE = 1.0 / 8_388_608.0
        private const val PCM_32_SCALE = 1.0 / 2_147_483_648.0

        internal fun relativeLoudnessDb(loudnessDb: Float?, perceptualLoudnessDb: Float?): Float? =
            loudnessDb?.takeIf { it.isFinite() }
                ?: perceptualLoudnessDb?.takeIf { it.isFinite() }?.let { it - STREAMING_REFERENCE_LUFS }

        internal fun attenuationForRelativeLoudness(relativeLoudnessDb: Float): Float =
            10.0.pow(-relativeLoudnessDb.coerceAtLeast(0f) / 20.0).toFloat().coerceIn(MIN_GAIN, 1f)

        internal fun attenuationForMeasuredLoudness(integratedLufs: Double): Float? =
            integratedLufs.takeUnless { it.isNaN() }
                ?.let { attenuationForRelativeLoudness((it - STREAMING_REFERENCE_LUFS).toFloat()) }

        private fun slewGain(current: Float, target: Float, maxStepDb: Float): Float {
            val currentDb = 20.0 * log10(current.toDouble())
            val targetDb = 20.0 * log10(target.toDouble())
            val delta = targetDb - currentDb
            if (abs(delta) <= maxStepDb) return target
            val nextDb = currentDb + if (delta > 0) maxStepDb else -maxStepDb
            return 10.0.pow(nextDb / 20.0).toFloat()
        }
    }
}
