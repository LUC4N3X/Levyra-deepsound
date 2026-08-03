package com.luc4n3x.levyra.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

class TruePeakLimiterAudioProcessor : AudioProcessor {
    @Volatile var enabled: Boolean = true

    @Volatile var currentGain: Float = 1f
        private set

    private var format = AudioFormat.NOT_SET
    private var configured = false
    private var buffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false
    private var lookaheadFrames = 0
    private var ring = FloatArray(0)
    private var ringFrameCapacity = 0
    private var inputFrameIndex = 0L
    private var outputFrameIndex = 0L
    private var oversamplingHistory = FloatArray(0)
    private var oversamplingWriteIndex = 0
    private var dequeIndices = LongArray(0)
    private var dequePeaks = FloatArray(0)
    private var dequeHead = 0L
    private var dequeTail = 0L

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if ((inputAudioFormat.encoding != C.ENCODING_PCM_16BIT &&
                inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) ||
            inputAudioFormat.channelCount <= 0
        ) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        format = inputAudioFormat
        configured = true
        lookaheadFrames = (inputAudioFormat.sampleRate * LOOKAHEAD_MS / 1_000).coerceAtLeast(1)
        ringFrameCapacity = lookaheadFrames + 2
        ring = FloatArray(ringFrameCapacity * inputAudioFormat.channelCount)
        oversamplingHistory = FloatArray(inputAudioFormat.channelCount * TRUE_PEAK_TAPS)
        dequeIndices = LongArray(ringFrameCapacity + TRUE_PEAK_TAPS + 2)
        dequePeaks = FloatArray(ringFrameCapacity + TRUE_PEAK_TAPS + 2)
        clearState()
        return inputAudioFormat
    }

    override fun isActive(): Boolean = configured

    override fun queueInput(inputBuffer: ByteBuffer) {
        val inputLimit = inputBuffer.limit()
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        val frameSize = bytesPerSample() * format.channelCount
        val frames = inputBuffer.remaining() / frameSize
        if (frames <= 0) {
            inputBuffer.position(inputLimit)
            outputBuffer = AudioProcessor.EMPTY_BUFFER
            return
        }
        val outputFrames = (inputFrameIndex + frames - lookaheadFrames - outputFrameIndex).coerceAtLeast(0L).toInt()
        val output = replaceOutputBuffer(outputFrames * frameSize)
        repeat(frames) {
            val peak = readFrame(inputBuffer, inputFrameIndex)
            pushPeak((inputFrameIndex - TRUE_PEAK_GROUP_DELAY).coerceAtLeast(0L), peak)
            inputFrameIndex++
            if (inputFrameIndex - outputFrameIndex > lookaheadFrames) writeNextFrame(output)
        }
        inputBuffer.position(inputLimit)
        output.flip()
    }

    private fun readFrame(input: ByteBuffer, frameIndex: Long): Float {
        val ringOffset = (frameIndex % ringFrameCapacity).toInt() * format.channelCount
        repeat(format.channelCount) { channel ->
            val sample = if (format.encoding == C.ENCODING_PCM_FLOAT) input.float else input.short / 32768f
            ring[ringOffset + channel] = sample
            oversamplingHistory[channel * TRUE_PEAK_TAPS + oversamplingWriteIndex] = sample
        }
        val framePeak = if (enabled) oversampledFramePeak() else 0f
        oversamplingWriteIndex = (oversamplingWriteIndex + 1) % TRUE_PEAK_TAPS
        return framePeak
    }

    private fun pushZeroOversamplingFrame(detectorFrameIndex: Long) {
        repeat(format.channelCount) { channel ->
            oversamplingHistory[channel * TRUE_PEAK_TAPS + oversamplingWriteIndex] = 0f
        }
        val peak = oversampledFramePeak()
        oversamplingWriteIndex = (oversamplingWriteIndex + 1) % TRUE_PEAK_TAPS
        val lastInputFrame = inputFrameIndex - 1L
        if (lastInputFrame >= 0L) {
            val peakIndex = (detectorFrameIndex - TRUE_PEAK_GROUP_DELAY).coerceIn(0L, lastInputFrame)
            pushPeak(peakIndex, peak)
        }
    }

    private fun oversampledFramePeak(): Float {
        var peak = 0f
        repeat(format.channelCount) { channel ->
            val historyOffset = channel * TRUE_PEAK_TAPS
            repeat(OVERSAMPLE_FACTOR) { phase ->
                var reconstructed = 0f
                repeat(TRUE_PEAK_TAPS) { tap ->
                    val historyIndex = (oversamplingWriteIndex - tap + TRUE_PEAK_TAPS) % TRUE_PEAK_TAPS
                    reconstructed += oversamplingHistory[historyOffset + historyIndex] *
                        TRUE_PEAK_COEFFICIENTS[phase][tap]
                }
                peak = max(peak, abs(reconstructed))
            }
        }
        return peak
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
        val peakSlot = (dequeHead % dequePeaks.size).toInt()
        val peak = if (dequeTail > dequeHead) dequePeaks[peakSlot] else 0f
        val targetGain = if (enabled && peak > CEILING_LINEAR) CEILING_LINEAR / peak else 1f
        currentGain = if (targetGain < currentGain) {
            val peakIndex = dequeIndices[peakSlot]
            val attackFrames = (peakIndex - outputFrameIndex).coerceAtLeast(1L).toFloat()
            currentGain + (targetGain - currentGain) / attackFrames
        } else {
            currentGain + (targetGain - currentGain) * RELEASE_COEFFICIENT
        }
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
        repeat(TRUE_PEAK_GROUP_DELAY) { step ->
            pushZeroOversamplingFrame(inputFrameIndex + step)
        }
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
        oversamplingHistory = FloatArray(0)
        dequeIndices = LongArray(0)
        dequePeaks = FloatArray(0)
        buffer = AudioProcessor.EMPTY_BUFFER
    }

    private fun clearState() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
        inputFrameIndex = 0L
        outputFrameIndex = 0L
        currentGain = 1f
        oversamplingWriteIndex = 0
        dequeHead = 0L
        dequeTail = 0L
        ring.fill(0f)
        oversamplingHistory.fill(0f)
    }

    private fun bytesPerSample(): Int = if (format.encoding == C.ENCODING_PCM_FLOAT) 4 else 2

    private fun replaceOutputBuffer(size: Int): ByteBuffer {
        if (buffer.capacity() < size) {
            buffer = ByteBuffer.allocateDirect(size)
        } else {
            buffer.clear()
        }
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        outputBuffer = buffer
        return buffer
    }

    companion object {
        private const val LOOKAHEAD_MS = 5
        private const val OVERSAMPLE_FACTOR = 4
        private const val TRUE_PEAK_TAPS = 13
        private const val TRUE_PEAK_GROUP_DELAY = TRUE_PEAK_TAPS / 2
        private const val CEILING_DB = -1f
        private val CEILING_LINEAR = 10.0.pow(CEILING_DB / 20.0).toFloat()
        private const val RELEASE_COEFFICIENT = 0.0015f
        private val TRUE_PEAK_COEFFICIENTS = Array(OVERSAMPLE_FACTOR) { phase ->
            val fraction = phase.toDouble() / OVERSAMPLE_FACTOR
            FloatArray(TRUE_PEAK_TAPS) { tap ->
                val x = tap - TRUE_PEAK_GROUP_DELAY + fraction
                val sinc = if (abs(x) < 1e-12) 1.0 else sin(PI * x) / (PI * x)
                val window = 0.42 -
                    0.5 * cos(2.0 * PI * tap / (TRUE_PEAK_TAPS - 1)) +
                    0.08 * cos(4.0 * PI * tap / (TRUE_PEAK_TAPS - 1))
                (sinc * window).toFloat()
            }.also { coefficients ->
                val sum = coefficients.sum()
                if (sum != 0f) {
                    coefficients.indices.forEach { index -> coefficients[index] /= sum }
                }
            }
        }
    }
}