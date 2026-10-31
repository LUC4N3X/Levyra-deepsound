package com.luc4n3x.levyra.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import com.luc4n3x.levyra.domain.NightListeningCurve
import com.luc4n3x.levyra.domain.NightListeningPreset
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

class NightListeningAudioProcessor : BaseAudioProcessor() {
    private class Target(val enabled: Boolean, val preset: NightListeningPreset, val trimDb: Float)

    @Volatile private var target = Target(false, NightListeningPreset.BALANCED, 0f)

    @Volatile var currentGainReductionDb: Float = 0f
        private set

    @Volatile var currentInputLevelDb: Float = SILENCE_DB
        private set

    @Volatile var processing: Boolean = false
        private set

    private var envelope = 0f
    private var mix = 0f
    private var appliedGain = 1f
    private var makeupDb = 0f
    private var framesUntilUpdate = 0
    private var gainStep = 0f
    private var attackCoefficient = 0f
    private var releaseCoefficient = 0f
    private var preparedPreset: NightListeningPreset? = null
    private var mixStep = 0f
    private var makeupCoefficient = 0f
    private var meterReductionDb = 0f
    private var channels = 0
    private var sampleSize = 2
    private var floatSamples = false

    fun setConfiguration(enabled: Boolean, preset: NightListeningPreset, trimDb: Float) {
        target = Target(
            enabled,
            preset,
            trimDb.coerceIn(NightListeningCurve.MIN_TRIM_DB, NightListeningCurve.MAX_TRIM_DB)
        )
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if ((inputAudioFormat.encoding != C.ENCODING_PCM_16BIT &&
                inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) ||
            inputAudioFormat.channelCount <= 0
        ) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return
        val output = replaceOutputBuffer(remaining)
        val state = target
        if (!state.enabled && mix <= 0f) {
            processing = false
            currentGainReductionDb = 0f
            output.put(inputBuffer)
            output.flip()
            return
        }
        prepare(state.preset)
        processing = state.enabled
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        output.order(ByteOrder.LITTLE_ENDIAN)
        channels = inputAudioFormat.channelCount
        floatSamples = inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        sampleSize = if (floatSamples) 4 else 2
        val frames = remaining / (sampleSize * channels)
        val base = inputBuffer.position()
        val targetMakeupDb = state.preset.makeupDb + state.trimDb
        val targetMix = if (state.enabled) 1f else 0f
        for (frame in 0 until frames) {
            val frameOffset = base + frame * sampleSize * channels
            advanceEnvelope(framePeak(inputBuffer, frameOffset))
            if (framesUntilUpdate <= 0) {
                updateControl(state.preset, targetMakeupDb)
                framesUntilUpdate = CONTROL_INTERVAL_FRAMES
            }
            framesUntilUpdate--
            appliedGain += gainStep
            advanceMix(targetMix)
            writeFrame(inputBuffer, output, frameOffset, 1f + (appliedGain - 1f) * mix)
        }
        inputBuffer.position(inputBuffer.limit())
        output.flip()
    }

    private fun framePeak(input: ByteBuffer, frameOffset: Int): Float {
        var peak = 0f
        for (channel in 0 until channels) {
            peak = max(peak, abs(readSample(input, frameOffset + channel * sampleSize)))
        }
        return peak
    }

    private fun readSample(input: ByteBuffer, offset: Int): Float =
        if (floatSamples) input.getFloat(offset) else input.getShort(offset) / 32768f

    private fun advanceEnvelope(peak: Float) {
        val coefficient = if (peak > envelope) attackCoefficient else releaseCoefficient
        envelope = peak + (envelope - peak) * coefficient
    }

    private fun advanceMix(targetMix: Float) {
        mix = if (mix < targetMix) {
            (mix + mixStep).coerceAtMost(targetMix)
        } else {
            (mix - mixStep).coerceAtLeast(targetMix)
        }
    }

    private fun writeFrame(input: ByteBuffer, output: ByteBuffer, frameOffset: Int, gain: Float) {
        for (channel in 0 until channels) {
            val sample = readSample(input, frameOffset + channel * sampleSize) * gain
            if (floatSamples) {
                output.putFloat(sample)
            } else {
                output.putShort((sample.coerceIn(-1f, MAX_16_BIT) * 32768f).roundToInt().toShort())
            }
        }
    }

    private fun prepare(preset: NightListeningPreset) {
        if (preparedPreset === preset) return
        preparedPreset = preset
        val sampleRate = inputAudioFormat.sampleRate.coerceAtLeast(1).toFloat()
        attackCoefficient = exp(-1000f / (preset.attackMs * sampleRate))
        releaseCoefficient = exp(-1000f / (preset.releaseMs * sampleRate))
        mixStep = 1000f / (MIX_RAMP_MS * sampleRate)
        makeupCoefficient = exp(-1000f * CONTROL_INTERVAL_FRAMES / (MAKEUP_SMOOTHING_MS * sampleRate))
    }

    private fun updateControl(preset: NightListeningPreset, targetMakeupDb: Float) {
        val levelDb = if (envelope > SILENCE_LINEAR) 20f * log10(envelope) else SILENCE_DB
        val reductionDb = NightListeningCurve.gainReductionDb(levelDb, preset)
        makeupDb = targetMakeupDb + (makeupDb - targetMakeupDb) * makeupCoefficient
        val nextGain = 10f.pow((makeupDb - reductionDb) / 20f)
        gainStep = (nextGain - appliedGain) / CONTROL_INTERVAL_FRAMES
        meterReductionDb = reductionDb
        currentGainReductionDb = reductionDb * mix
        currentInputLevelDb = levelDb
    }

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        envelope = 0f
        framesUntilUpdate = 0
        gainStep = 0f
        meterReductionDb = 0f
        currentGainReductionDb = 0f
        currentInputLevelDb = SILENCE_DB
        preparedPreset = null
    }

    override fun onReset() {
        mix = 0f
        appliedGain = 1f
        makeupDb = 0f
        processing = false
    }

    private companion object {
        const val CONTROL_INTERVAL_FRAMES = 32
        const val MIX_RAMP_MS = 180f
        const val MAKEUP_SMOOTHING_MS = 120f
        const val SILENCE_DB = -90f
        const val SILENCE_LINEAR = 3.1622776E-5f
        const val MAX_16_BIT = 0.9999695f
    }
}
