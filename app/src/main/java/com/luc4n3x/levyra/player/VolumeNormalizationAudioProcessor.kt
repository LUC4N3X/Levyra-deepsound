package com.luc4n3x.levyra.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow
import kotlin.math.sqrt

/*
 * Inspired by Metrolist's GPL-3.0 volume normalization processor.
 * Adapted to keep Levyra's existing loudness behaviour and RMS fallback.
 */
@UnstableApi
open class VolumeNormalizationAudioProcessor : AudioProcessor {

    @Volatile
    var enabled: Boolean = false

    @Volatile
    private var youtubeLoudnessDb: Float? = null

    @Volatile
    private var youtubePerceptualLoudnessDb: Float? = null

    @Volatile
    private var explicitGain: Float? = null

    private var inputAudioFormat = AudioFormat.NOT_SET
    private var bytesPerSample = 0
    private var configured = false
    private var buffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false
    private var currentGain = 1.0f
    private var targetGain = 1.0f

    fun setYoutubeLoudness(loudnessDb: Float?, perceptualLoudnessDb: Float?) {
        youtubeLoudnessDb = loudnessDb?.takeIf { it.isFinite() }
        youtubePerceptualLoudnessDb = perceptualLoudnessDb?.takeIf { it.isFinite() }
        explicitGain = null
    }

    fun setTargetGain(gainMb: Int) {
        explicitGain = 10.0.pow(gainMb / 2000.0)
            .toFloat()
            .coerceIn(MIN_METADATA_GAIN, MAX_DYNAMIC_GAIN)
    }

    internal fun metadataGain(): Float? {
        val loudness = youtubePerceptualLoudnessDb ?: youtubeLoudnessDb ?: return null
        val attenuationDb = loudness.coerceAtLeast(0.0f)
        return 10.0.pow(-attenuationDb / 20.0)
            .toFloat()
            .coerceIn(MIN_METADATA_GAIN, 1.0f)
    }

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.channelCount <= 0) {
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

        if (!enabled) {
            resetGain()
            output.put(inputBuffer)
            output.flip()
            return
        }

        val frameSize = bytesPerSample * inputAudioFormat.channelCount
        val completeBytes = if (frameSize > 0) inputSize - inputSize % frameSize else 0
        if (completeBytes <= 0) {
            output.put(inputBuffer)
            output.flip()
            return
        }

        val sampleCount = completeBytes / bytesPerSample
        val gain = updateGain(inputBuffer, sampleCount)
        processSamples(inputBuffer, output, sampleCount, gain)

        while (inputBuffer.hasRemaining()) {
            output.put(inputBuffer.get())
        }
        output.flip()
    }

    private fun updateGain(inputBuffer: ByteBuffer, sampleCount: Int): Float {
        val fixedGain = explicitGain ?: metadataGain()
        if (fixedGain != null) {
            targetGain = fixedGain
            currentGain += (targetGain - currentGain) * METADATA_SMOOTHING
            return currentGain
        }

        val probe = inputBuffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        var sumSquares = 0.0
        repeat(sampleCount) {
            val sample = readNormalizedSample(probe)
            sumSquares += sample * sample
        }

        val rms = sqrt(sumSquares / sampleCount).toFloat()
        targetGain = if (rms <= SILENCE_RMS) {
            1.0f
        } else {
            (TARGET_RMS / rms).coerceIn(MIN_DYNAMIC_GAIN, MAX_DYNAMIC_GAIN)
        }

        val smoothing = if (targetGain < currentGain) CUT_SMOOTHING else BOOST_SMOOTHING
        currentGain += (targetGain - currentGain) * smoothing
        return currentGain
    }

    private fun readNormalizedSample(input: ByteBuffer): Double = when (inputAudioFormat.encoding) {
        C.ENCODING_PCM_16BIT -> input.short.toDouble() / Short.MAX_VALUE.toDouble()
        C.ENCODING_PCM_24BIT -> readPcm24(input).toDouble() / PCM_24_MAX.toDouble()
        C.ENCODING_PCM_32BIT -> input.int.toDouble() / Int.MAX_VALUE.toDouble()
        C.ENCODING_PCM_FLOAT -> input.float
            .takeIf { it.isFinite() }
            ?.coerceIn(-1.0f, 1.0f)
            ?.toDouble()
            ?: 0.0
        else -> 0.0
    }

    private fun processSamples(
        input: ByteBuffer,
        output: ByteBuffer,
        sampleCount: Int,
        gain: Float,
    ) {
        when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT -> repeat(sampleCount) {
                val sample = input.short.toInt()
                val processed = (sample * gain)
                    .toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                output.putShort(processed.toShort())
            }

            C.ENCODING_PCM_24BIT -> repeat(sampleCount) {
                val sample = readPcm24(input)
                val processed = (sample * gain)
                    .toInt()
                    .coerceIn(PCM_24_MIN, PCM_24_MAX)
                writePcm24(output, processed)
            }

            C.ENCODING_PCM_32BIT -> repeat(sampleCount) {
                val sample = input.int
                val processed = (sample.toDouble() * gain)
                    .coerceIn(Int.MIN_VALUE.toDouble(), Int.MAX_VALUE.toDouble())
                    .toLong()
                    .toInt()
                output.putInt(processed)
            }

            C.ENCODING_PCM_FLOAT -> repeat(sampleCount) {
                val sample = input.float
                val processed = if (sample.isFinite()) {
                    (sample * gain).coerceIn(-1.0f, 1.0f)
                } else {
                    0.0f
                }
                output.putFloat(processed)
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
        clearBufferedState()
    }

    override fun reset() {
        clearBufferedState()
        inputAudioFormat = AudioFormat.NOT_SET
        bytesPerSample = 0
        configured = false
        buffer = AudioProcessor.EMPTY_BUFFER
    }

    private fun clearBufferedState() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
        resetGain()
    }

    private fun resetGain() {
        currentGain = 1.0f
        targetGain = 1.0f
    }

    companion object {
        private const val TARGET_RMS = 0.145f
        private const val SILENCE_RMS = 0.006f
        private const val MAX_DYNAMIC_GAIN = 1.65f
        private const val MIN_DYNAMIC_GAIN = 0.55f
        private const val MIN_METADATA_GAIN = 0.25f
        private const val BOOST_SMOOTHING = 0.012f
        private const val CUT_SMOOTHING = 0.18f
        private const val METADATA_SMOOTHING = 0.08f
        private const val PCM_24_MIN = -8_388_608
        private const val PCM_24_MAX = 8_388_607
    }
}
