package com.luc4n3x.levyra.player.enhanced

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

fun interface TimeProvider {
    fun nanoTime(): Long
}

class EnhancedAudioProcessor(
    private var config: EnhancedAudioConfig = EnhancedAudioConfig(),
    private val dspEngine: EnhancedAudioEngine = DspRestorationEngine(),
    private val timeProvider: TimeProvider = TimeProvider { System.nanoTime() }
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

    private val activeEngine: EnhancedAudioEngine = dspEngine

    val engineName: String
        get() = activeEngine.name

    private val analyzer = EnhancedAudioAnalyzer(config)

    private val _metricsState = MutableStateFlow(EnhancedAudioMetrics())
    val metricsState: StateFlow<EnhancedAudioMetrics> = _metricsState.asStateFlow()

    private val mutableMetrics = MutableEnhancedAudioMetrics()
    private var lastEmitTimeNs = 0L
    private var lastEmittedBypassReason: EnhancedAudioBypassReason? = null
    private var lastEmittedBypassed: Boolean? = null

    private var consecutiveOverruns = 0
    private var isOverloadBypassed = false
    private var cooldownRemainingBlocks = 0

    private var format: AudioFormat = AudioFormat.NOT_SET
    private var configured = false
    private var inputEnded = false

    private var buffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER

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

        val frameSize = bytesPerSample() * format.channelCount
        val frames = remaining / frameSize
        if (frames <= 0) {
            inputBuffer.position(inputLimit)
            outputBuffer = AudioProcessor.EMPTY_BUFFER
            return
        }

        val bypassReason = currentBypassReason()
        if (bypassReason != null) {
            handleBypassPassthrough(inputBuffer, remaining, inputLimit, bypassReason)
            return
        }

        processEnhancedAudio(inputBuffer, remaining, inputLimit, frames, frameSize)
    }

    private fun handleBypassPassthrough(
        inputBuffer: ByteBuffer,
        remaining: Int,
        inputLimit: Int,
        bypassReason: EnhancedAudioBypassReason
    ) {
        if (isOverloadBypassed) {
            cooldownRemainingBlocks--
            if (cooldownRemainingBlocks <= 0) {
                isOverloadBypassed = false
                consecutiveOverruns = 0
            }
        }

        val out = replaceOutputBuffer(remaining)
        out.put(inputBuffer)
        inputBuffer.position(inputLimit)
        out.flip()

        mutableMetrics.reset()
        mutableMetrics.setBypass(bypassReason)
        maybeEmitMetrics(mutableMetrics)
    }

    private fun processEnhancedAudio(
        inputBuffer: ByteBuffer,
        remaining: Int,
        inputLimit: Int,
        frames: Int,
        frameSize: Int
    ) {
        val totalSamples = frames * format.channelCount
        ensureFloatCapacity(totalSamples)

        val blockStartNs = timeProvider.nanoTime()
        val startTimeUs = blockStartNs / 1_000L

        try {
            readInputToFloats(inputBuffer, totalSamples)
            analyzer.analyze(floatInput, 0, frames, startTimeUs, mutableMetrics)

            val success = activeEngine.process(floatInput, floatOutput, 0, frames, mutableMetrics)

            if (!success) {
                System.arraycopy(floatInput, 0, floatOutput, 0, totalSamples)
            }

            writeFloatsToOutput(totalSamples, frames, frameSize)

            val blockEndNs = timeProvider.nanoTime()
            val elapsedUs = (blockEndNs - blockStartNs) / 1_000L
            mutableMetrics.processingTimeUs = elapsedUs
            evaluateWatchdogOverrun(elapsedUs)

            maybeEmitMetrics(mutableMetrics)
        } catch (error: Throwable) {
            val out = replaceOutputBuffer(remaining)
            inputBuffer.position(inputLimit - remaining)
            out.put(inputBuffer)
            out.flip()

            mutableMetrics.reset()
            mutableMetrics.setBypass(EnhancedAudioBypassReason.INTERNAL_ERROR)
            maybeEmitMetrics(mutableMetrics, force = true)
        } finally {
            inputBuffer.position(inputLimit)
        }
    }

    private fun readInputToFloats(inputBuffer: ByteBuffer, totalSamples: Int) {
        if (format.encoding == C.ENCODING_PCM_FLOAT) {
            for (i in 0 until totalSamples) {
                floatInput[i] = inputBuffer.float
            }
        } else {
            for (i in 0 until totalSamples) {
                floatInput[i] = inputBuffer.short / 32768f
            }
        }
    }

    private fun writeFloatsToOutput(totalSamples: Int, frames: Int, frameSize: Int) {
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
    }

    private fun evaluateWatchdogOverrun(elapsedUs: Long) {
        if (elapsedUs > config.maxAllowedProcessingTimeUs) {
            consecutiveOverruns++
            if (consecutiveOverruns >= OVERRUN_HYSTERESIS_LIMIT) {
                isOverloadBypassed = true
                cooldownRemainingBlocks = RECOVERY_COOLDOWN_BLOCKS
                mutableMetrics.setBypass(EnhancedAudioBypassReason.CPU_OVERLOAD)
            }
        } else {
            consecutiveOverruns = 0
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
        consecutiveOverruns = 0
        isOverloadBypassed = false
        cooldownRemainingBlocks = 0
        activeEngine.reset()
        analyzer.reset()
        mutableMetrics.reset()
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
        isOverloadBypassed -> EnhancedAudioBypassReason.CPU_OVERLOAD
        else -> null
    }

    private fun updateState() {
        val reason = currentBypassReason() ?: return
        _metricsState.value = EnhancedAudioMetrics(
            bypassed = true,
            bypassReason = reason
        )
    }

    private fun maybeEmitMetrics(metrics: MutableEnhancedAudioMetrics, force: Boolean = false) {
        val nowNs = timeProvider.nanoTime()
        val stateChanged = metrics.bypassed != lastEmittedBypassed || metrics.bypassReason != lastEmittedBypassReason
        if (force || stateChanged || nowNs - lastEmitTimeNs >= EMIT_INTERVAL_NS) {
            lastEmitTimeNs = nowNs
            lastEmittedBypassed = metrics.bypassed
            lastEmittedBypassReason = metrics.bypassReason
            _metricsState.value = metrics.toSnapshot()
        }
    }

    private fun ensureFloatCapacity(required: Int) {
        if (floatInput.size < required) {
            val newCapacity = max(required, floatInput.size * 3 / 2).coerceAtLeast(4096)
            floatInput = FloatArray(newCapacity)
            floatOutput = FloatArray(newCapacity)
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

    companion object {
        private const val EMIT_INTERVAL_NS = 200_000_000L
        private const val OVERRUN_HYSTERESIS_LIMIT = 5
        private const val RECOVERY_COOLDOWN_BLOCKS = 50
    }
}
