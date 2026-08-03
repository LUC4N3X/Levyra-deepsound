package com.luc4n3x.levyra.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

class TruePeakLimiterAudioProcessor : AudioProcessor {
    @Volatile var enabled: Boolean = true
    @Volatile var gainReductionDb: Float = 0f
        private set

    private var format = AudioFormat.NOT_SET
    private var configured = false
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false
    private var lookaheadFrames = 0
    private var ring = FloatArray(0)
    private var ringFrameCapacity = 0
    private var inputFrameIndex = 0L
    private var outputFrameIndex = 0L
    private var previousInput = FloatArray(0)
    private var currentGain = 1f
    private var dequeIndices = LongArray(0)
    private var dequePeaks = FloatArray(0)
    private var dequeHead = 0L
    private var dequeTail = 0L

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT && inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        format = inputAudioFormat
        configured = true
        lookaheadFrames = (inputAudioFormat.sampleRate * LOOKAHEAD_MS / 1_000).coerceAtLeast(1)
        ringFrameCapacity = lookaheadFrames + 2
        ring = FloatArray(ringFrameCapacity * inputAudioFormat.channelCount)
        previousInput = FloatArray(inputAudioFormat.channelCount)
        dequeIndices = LongArray(ringFrameCapacity + 2)
        dequePeaks = FloatArray(ringFrameCapacity + 2)
        clearState()
        return inputAudioFormat
    }

    override fun isActive(): Boolean = configured

    override fun queueInput(inputBuffer: ByteBuffer) {
        val frameSize = bytesPerSample() * format.channelCount
        val frames = inputBuffer.remaining() / frameSize
        if (frames <= 0) {
            outputBuffer = AudioProcessor.EMPTY_BUFFER
            return
        }
        val outputFrames = (inputFrameIndex + frames - lookaheadFrames - outputFrameIndex).coerceAtLeast(0L).toInt()
        val output = replaceOutputBuffer(outputFrames * frameSize)
        repeat(frames) {
            val peak = readFrame(inputBuffer, inputFrameIndex)
            pushPeak(inputFrameIndex, peak)
            inputFrameIndex++
            if (inputFrameIndex - outputFrameIndex > lookaheadFrames) writeNextFrame(output)
        }
        output.flip()
    }

    private fun readFrame(input: ByteBuffer, frameIndex: Long): Float {
        val ringOffset = (frameIndex % ringFrameCapacity).toInt() * format.channelCount
        var framePeak = 0f
        repeat(format.channelCount) { channel ->
            val sample = if (format.encoding == C.ENCODING_PCM_FLOAT) input.float else input.short / 32768f
            ring[ringOffset + channel] = sample
            val previous = previousInput[channel]
            repeat(OVERSAMPLE_FACTOR) { step ->
                val fraction = (step + 1f) / OVERSAMPLE_FACTOR
                framePeak = max(framePeak, abs(previous + (sample - previous) * fraction))
            }
            previousInput[channel] = sample
        }
        return framePeak
    }

    private fun pushPeak(index: Long, peak: Float) {
        while (dequeTail > dequeHead && dequePeaks[((dequeTail - 1L) % dequePeaks.size).toInt()] <= peak) dequeTail--
        val slot = (dequeTail % dequePeaks.size).toInt()
        dequeIndices[slot] = index
        dequePeaks[slot] = peak
        dequeTail++
    }

    private fun writeNextFrame(output: ByteBuffer) {
        while (dequeTail > dequeHead && dequeIndices[(dequeHead % dequeIndices.size).toInt()] < outputFrameIndex) dequeHead++
        val peak = if (dequeTail > dequeHead) dequePeaks[(dequeHead % dequePeaks.size).toInt()] else 0f
        val targetGain = if (enabled && peak > CEILING_LINEAR) CEILING_LINEAR / peak else 1f
        currentGain = if (targetGain < currentGain) targetGain else currentGain + (targetGain - currentGain) * RELEASE_COEFFICIENT
        gainReductionDb = if (currentGain >= 0.99999f) 0f else (-20.0 * kotlin.math.log10(currentGain.toDouble())).toFloat()
        val ringOffset = (outputFrameIndex % ringFrameCapacity).toInt() * format.channelCount
        repeat(format.channelCount) { channel ->
            val sample = ring[ringOffset + channel] * currentGain
            if (format.encoding == C.ENCODING_PCM_FLOAT) {
                output.putFloat(sample)
            } else {
                output.putShort((sample.coerceIn(-1f, 0.9999695f) * 32768f).roundToInt().toShort())
            }
        }
        outputFrameIndex++
    }

    override fun queueEndOfStream() {
        val remaining = (inputFrameIndex - outputFrameIndex).coerceAtLeast(0L).toInt()
        val output = replaceOutputBuffer(remaining * format.channelCount * bytesPerSample())
        repeat(remaining) { writeNextFrame(output) }
        output.flip()
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        val output = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return output
    }

    override fun isEnded(): Boolean = inputEnded && outputFrameIndex >= inputFrameIndex && !outputBuffer.hasRemaining()

    override fun flush(streamMetadata: AudioProcessor.StreamMetadata) = clearState()

    override fun reset() {
        clearState()
        configured = false
        format = AudioFormat.NOT_SET
        ring = FloatArray(0)
        previousInput = FloatArray(0)
        dequeIndices = LongArray(0)
        dequePeaks = FloatArray(0)
    }

    private fun clearState() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
        inputFrameIndex = 0L
        outputFrameIndex = 0L
        currentGain = 1f
        gainReductionDb = 0f
        dequeHead = 0L
        dequeTail = 0L
        ring.fill(0f)
        previousInput.fill(0f)
    }

    private fun bytesPerSample(): Int = if (format.encoding == C.ENCODING_PCM_FLOAT) 4 else 2

    private fun replaceOutputBuffer(size: Int): ByteBuffer {
        if (outputBuffer.capacity() < size) {
            outputBuffer = ByteBuffer.allocateDirect(size)
        } else {
            outputBuffer.clear()
        }
        outputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        return outputBuffer
    }

    companion object {
        private const val LOOKAHEAD_MS = 5
        private const val OVERSAMPLE_FACTOR = 4
        private const val CEILING_DB = -1f
        private val CEILING_LINEAR = 10.0.pow(CEILING_DB / 20.0).toFloat()
        private const val RELEASE_COEFFICIENT = 0.0015f
    }
}
