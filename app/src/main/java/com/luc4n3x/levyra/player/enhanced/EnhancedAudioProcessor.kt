package com.luc4n3x.levyra.player.enhanced

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Media3 [AudioProcessor] integrating Levyra Enhanced Audio into the playback pipeline.
 *
 * Responsibilities:
 * 1. Safe, zero-allocation PCM processing (supports PCM 16-bit and PCM Float).
 * 2. Real-time deficit gating via [EnhancedAudioAnalyzer].
 * 3. Restoration execution via [EnhancedAudioEngine] (defaults to [DspRestorationEngine]).
 * 4. Comprehensive bypass management (Lossless source, remote playback, user disabled, low confidence, etc.).
 * 5. Headroom and clipping prevention.
 * 6. Thread-safe diagnostics exposure for Technical Audio Info.
 */
class EnhancedAudioProcessor(
    private var config: EnhancedAudioConfig = EnhancedAudioConfig(),
    private val dspEngine: EnhancedAudioEngine = DspRestorationEngine(),
    private val neuralEngine: NeuralRestorationEngine = NeuralRestorationEngine()
) : AudioProcessor {

    @Volatile
    var userEnabled: Boolean = true
        set(value) {
            field = value
            updateState()
        }

    @Volatile
    var isLosslessSource: Boolean = false
        set(value) {
            field = value
            updateState()
        }

    @Volatile
    var isRemotePlayback: Boolean = false
        set(value) {
            field = value
            updateState()
        }

    private var activeEngine: EnhancedAudioEngine = dspEngine

    val engineName: String
        get() = activeEngine.name

    private val analyzer = EnhancedAudioAnalyzer(config)

    private val _metricsState = MutableStateFlow(EnhancedAudioMetrics())
    val metricsState: StateFlow<EnhancedAudioMetrics> = _metricsState.asStateFlow()

    private var format: AudioFormat = AudioFormat.NOT_SET
    private var configured = false
    private var inputEnded = false

    private var buffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER

    // Reusable float buffers for zero-allocation processing
    private var floatInput = FloatArray(0)
    private var floatOutput = FloatArray(0)

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if ((inputAudioFormat.encoding != C.ENCODING_PCM_16BIT &&
                inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) ||
            inputAudioFormat.channelCount <= 0 ||
            inputAudioFormat.sampleRate <= 0
        ) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }

        format = inputAudioFormat
        configured = true

        // Select engine: Prefer neural if available, fallback to rock-solid DSP
        activeEngine = if (neuralEngine.isAvailable) neuralEngine else dspEngine
        activeEngine.configure(inputAudioFormat.sampleRate, inputAudioFormat.channelCount, config)
        analyzer.configure(inputAudioFormat.sampleRate, inputAudioFormat.channelCount, config)

        clearBuffers()
        updateState()
        return inputAudioFormat
    }

    override fun isActive(): Boolean = configured

    override fun queueInput(inputBuffer: ByteBuffer) {
        val inputLimit = inputBuffer.limit()
        val remaining = inputBuffer.remaining()
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)

        val bytesPerSample = bytesPerSample()
        val frameSize = bytesPerSample * format.channelCount
        val frames = remaining / frameSize

        if (frames <= 0) {
            inputBuffer.position(inputLimit)
            outputBuffer = AudioProcessor.EMPTY_BUFFER
            return
        }

        val bypassReason = currentBypassReason()
        if (bypassReason != null) {
            // Direct passthrough: copy directly to output buffer with zero overhead
            val out = replaceOutputBuffer(remaining)
            out.put(inputBuffer)
            inputBuffer.position(inputLimit)
            out.flip()

            val metrics = EnhancedAudioMetrics(
                bypassed = true,
                bypassReason = bypassReason
            )
            _metricsState.value = metrics
            return
        }

        val totalSamples = frames * format.channelCount
        ensureFloatCapacity(totalSamples)

        val startTime = System.nanoTime() / 1_000L

        try {
            if (format.encoding == C.ENCODING_PCM_FLOAT) {
                for (i in 0 until totalSamples) {
                    floatInput[i] = inputBuffer.float
                }
            } else {
                for (i in 0 until totalSamples) {
                    floatInput[i] = inputBuffer.short / 32768f
                }
            }

            val metrics = analyzer.analyze(floatInput, 0, frames, startTime)

            val success = if (metrics.isActive) {
                activeEngine.process(floatInput, floatOutput, 0, frames, metrics)
            } else {
                System.arraycopy(floatInput, 0, floatOutput, 0, totalSamples)
                true
            }

            if (!success) {
                System.arraycopy(floatInput, 0, floatOutput, 0, totalSamples)
            }

            val out = replaceOutputBuffer(frames * frameSize)
            if (format.encoding == C.ENCODING_PCM_FLOAT) {
                for (i in 0 until totalSamples) {
                    out.putFloat(floatOutput[i])
                }
            } else {
                for (i in 0 until totalSamples) {
                    val s = (floatOutput[i].coerceIn(-1f, 0.9999695f) * 32768f).roundToInt().toShort()
                    out.putShort(s)
                }
            }
            out.flip()

            _metricsState.value = metrics
        } catch (error: Throwable) {
            // Safe bypass on any error - playback never stops
            val out = replaceOutputBuffer(remaining)
            inputBuffer.position(inputLimit - remaining)
            out.put(inputBuffer)
            out.flip()

            _metricsState.value = EnhancedAudioMetrics(
                bypassed = true,
                bypassReason = EnhancedAudioBypassReason.INTERNAL_ERROR
            )
        } finally {
            inputBuffer.position(inputLimit)
        }
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        val out = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return out
    }

    override fun isEnded(): Boolean = inputEnded && !outputBuffer.hasRemaining()

    override fun flush(streamMetadata: AudioProcessor.StreamMetadata) {
        clearBuffers()
        activeEngine.reset()
        analyzer.reset()
    }

    override fun reset() {
        clearBuffers()
        configured = false
        format = AudioFormat.NOT_SET
        floatInput = FloatArray(0)
        floatOutput = FloatArray(0)
        activeEngine.release()
        analyzer.reset()
    }

    fun updateConfig(newConfig: EnhancedAudioConfig) {
        this.config = newConfig.normalized()
        activeEngine.configure(format.sampleRate.coerceAtLeast(8_000), format.channelCount.coerceAtLeast(1), this.config)
        analyzer.updateConfig(this.config)
    }

    private fun currentBypassReason(): EnhancedAudioBypassReason? = when {
        !userEnabled -> EnhancedAudioBypassReason.USER_DISABLED
        isLosslessSource -> EnhancedAudioBypassReason.ALREADY_LOSSLESS
        isRemotePlayback -> EnhancedAudioBypassReason.CAST_OR_REMOTE_PLAYBACK
        !configured -> EnhancedAudioBypassReason.ENGINE_NOT_READY
        else -> null
    }

    private fun updateState() {
        val reason = currentBypassReason()
        if (reason != null) {
            _metricsState.value = EnhancedAudioMetrics(
                bypassed = true,
                bypassReason = reason
            )
        }
    }

    private fun ensureFloatCapacity(required: Int) {
        if (floatInput.size < required) {
            floatInput = FloatArray(required)
            floatOutput = FloatArray(required)
        }
    }

    private fun clearBuffers() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
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
}
