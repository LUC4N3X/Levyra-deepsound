package com.luc4n3x.levyra.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import com.luc4n3x.levyra.domain.AdaptiveListeningCondition
import com.luc4n3x.levyra.domain.AdaptiveListeningLimits
import com.luc4n3x.levyra.domain.AdaptiveListeningSnapshot
import com.luc4n3x.levyra.domain.AdaptiveSensitivity
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AdaptiveListeningConfig(
    val enabled: Boolean = false,
    val sensitivity: AdaptiveSensitivity = AdaptiveSensitivity.BALANCED,
    val maxBoostDb: Float = AdaptiveListeningLimits.DEFAULT_BOOST_DB
)

object AdaptiveListeningBus {
    @Volatile
    var targetBoostDb: Float = 0f
        private set

    @Volatile
    var outputLevelDb: Float = AdaptiveListeningLimits.SILENCE_DB
        internal set

    @Volatile
    var appliedBoostDb: Float = 0f
        internal set

    @Volatile
    var monitoring: Boolean = false

    private val _snapshot = MutableStateFlow(AdaptiveListeningSnapshot())
    val snapshot: StateFlow<AdaptiveListeningSnapshot> = _snapshot.asStateFlow()

    private val _config = MutableStateFlow(AdaptiveListeningConfig())
    val config: StateFlow<AdaptiveListeningConfig> = _config.asStateFlow()

    private val _playbackActive = MutableStateFlow(false)
    val playbackActive: StateFlow<Boolean> = _playbackActive.asStateFlow()

    private val _stoppedByUser = MutableStateFlow(false)
    val stoppedByUser: StateFlow<Boolean> = _stoppedByUser.asStateFlow()

    fun markStoppedByUser(stopped: Boolean) {
        _stoppedByUser.value = stopped
    }

    fun configure(value: AdaptiveListeningConfig) {
        _config.value = value
        if (!value.enabled) reset(AdaptiveListeningSnapshot(AdaptiveListeningCondition.Off))
    }

    fun setPlaybackActive(active: Boolean) {
        _playbackActive.value = active
    }

    fun publish(value: AdaptiveListeningSnapshot) {
        val limit = _config.value.maxBoostDb.coerceIn(0f, AdaptiveListeningLimits.MAX_BOOST_DB)
        targetBoostDb = if (_config.value.enabled) value.boostDb.coerceIn(0f, limit) else 0f
        _snapshot.value = value
    }

    fun reset(value: AdaptiveListeningSnapshot = AdaptiveListeningSnapshot()) {
        targetBoostDb = 0f
        _snapshot.value = value
    }
}

class AdaptiveGainAudioProcessor(private val meterOutput: Boolean) : BaseAudioProcessor() {
    private var appliedDb = 0f
    private var riseStepDb = 0f
    private var fallStepDb = 0f
    private var channels = 0
    private var floatSamples = false
    private var meterSum = 0.0
    private var meterFrames = 0
    private var meterWindowFrames = 1

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if ((inputAudioFormat.encoding != C.ENCODING_PCM_16BIT && inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) ||
            inputAudioFormat.channelCount <= 0
        ) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        val sampleRate = inputAudioFormat.sampleRate.coerceAtLeast(1)
        riseStepDb = RISE_DB_PER_SECOND / sampleRate
        fallStepDb = FALL_DB_PER_SECOND / sampleRate
        meterWindowFrames = (sampleRate * METER_WINDOW_MS / 1000).coerceAtLeast(1)
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return
        val output = replaceOutputBuffer(remaining)
        val target = AdaptiveListeningBus.targetBoostDb
        channels = inputAudioFormat.channelCount
        floatSamples = inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        val metering = meterOutput && AdaptiveListeningBus.monitoring
        if (target == 0f && appliedDb == 0f && !metering) {
            output.put(inputBuffer)
            output.flip()
            return
        }
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        output.order(ByteOrder.LITTLE_ENDIAN)
        val sampleSize = if (floatSamples) 4 else 2
        val frames = remaining / (sampleSize * channels)
        val base = inputBuffer.position()
        val limit = if (floatSamples || (target == 0f && appliedDb == 0f)) target else minOf(target, headroomDb(inputBuffer, base, frames * channels))
        if (appliedDb > limit) appliedDb = limit.coerceAtLeast(0f)
        var gain = linear(appliedDb)
        for (frame in 0 until frames) {
            if (frame % GAIN_UPDATE_FRAMES == 0) {
                appliedDb = stepToward(appliedDb, limit, GAIN_UPDATE_FRAMES)
                gain = linear(appliedDb)
            }
            writeFrame(inputBuffer, output, base + frame * sampleSize * channels, sampleSize, gain, metering)
        }
        inputBuffer.position(inputBuffer.limit())
        output.flip()
        AdaptiveListeningBus.appliedBoostDb = appliedDb
    }

    private fun writeFrame(input: ByteBuffer, output: ByteBuffer, frameOffset: Int, sampleSize: Int, gain: Float, metering: Boolean) {
        for (channel in 0 until channels) {
            val offset = frameOffset + channel * sampleSize
            val sample = if (floatSamples) input.getFloat(offset) else input.getShort(offset) / 32768f
            if (metering && channel == 0) meterSum += (sample * sample).toDouble()
            val shaped = sample * gain
            if (floatSamples) output.putFloat(shaped) else output.putShort((shaped.coerceIn(-1f, MAX_16_BIT) * 32768f).roundToInt().toShort())
        }
        if (metering) {
            meterFrames++
            if (meterFrames >= meterWindowFrames) publishMeter()
        }
    }

    private fun headroomDb(input: ByteBuffer, base: Int, samples: Int): Float {
        var peak = 0
        for (index in 0 until samples) {
            val value = abs(input.getShort(base + index * 2).toInt())
            if (value > peak) peak = value
        }
        if (peak == 0) return AdaptiveListeningLimits.MAX_BOOST_DB
        return (20.0 * log10(HEADROOM_CEILING * 32768.0 / peak)).toFloat().coerceAtLeast(0f)
    }

    private fun stepToward(current: Float, target: Float, frames: Int): Float = when {
        current < target -> (current + riseStepDb * frames).coerceAtMost(target)
        current > target -> (current - fallStepDb * frames).coerceAtLeast(target)
        else -> current
    }

    private fun publishMeter() {
        val meanSquare = meterSum / meterFrames
        AdaptiveListeningBus.outputLevelDb = if (meanSquare <= 1e-12) AdaptiveListeningLimits.SILENCE_DB
        else (10.0 * log10(meanSquare)).toFloat()
        meterSum = 0.0
        meterFrames = 0
    }

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        meterSum = 0.0
        meterFrames = 0
    }

    override fun onReset() {
        appliedDb = 0f
        meterSum = 0.0
        meterFrames = 0
        if (meterOutput) {
            AdaptiveListeningBus.appliedBoostDb = 0f
            AdaptiveListeningBus.outputLevelDb = AdaptiveListeningLimits.SILENCE_DB
        }
    }

    private fun linear(db: Float): Float = if (db == 0f) 1f else 10f.pow(db / 20f)

    private companion object {
        const val RISE_DB_PER_SECOND = 0.5f
        const val FALL_DB_PER_SECOND = 2f
        const val GAIN_UPDATE_FRAMES = 64
        const val METER_WINDOW_MS = 250
        const val MAX_16_BIT = 0.9999695f
        const val HEADROOM_CEILING = 0.98
    }
}
