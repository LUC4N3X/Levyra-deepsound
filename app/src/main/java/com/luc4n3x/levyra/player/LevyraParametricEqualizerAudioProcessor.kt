package com.luc4n3x.levyra.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import com.luc4n3x.levyra.domain.ParametricBiquad
import com.luc4n3x.levyra.domain.ParametricEqBand
import com.luc4n3x.levyra.domain.ParametricEqProfile
import com.luc4n3x.levyra.domain.ParametricEqualizer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.exp
import kotlin.math.pow

class LevyraParametricEqualizerAudioProcessor : AudioProcessor {
    private data class RequestedConfiguration(
        val revision: Long,
        val enabled: Boolean,
        val profile: ParametricEqProfile?
    )

    private val revision = AtomicLong()

    @Volatile
    private var requested = RequestedConfiguration(0L, false, null)

    private var inputFormat = AudioFormat.NOT_SET
    private var outputFormat = AudioFormat.NOT_SET
    private var pendingInputFormat = AudioFormat.NOT_SET
    private var pendingOutputFormat = AudioFormat.NOT_SET
    private var reusableBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false
    private var configured = false
    private var channelIndex = 0
    private var appliedRevision = Long.MIN_VALUE
    private var activeBank = 0
    private var transitionBank = 1
    private var transitionFramesRemaining = 0
    private var transitionTotalFrames = 0
    private val banks = arrayOf(FilterBank(), FilterBank())

    fun setConfiguration(enabled: Boolean, profile: ParametricEqProfile?) {
        val normalized = profile?.normalized()
        requested = RequestedConfiguration(
            revision = revision.incrementAndGet(),
            enabled = enabled && normalized != null,
            profile = normalized
        )
    }

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if ((inputAudioFormat.encoding != C.ENCODING_PCM_16BIT &&
                inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) ||
            inputAudioFormat.sampleRate <= 0 || inputAudioFormat.channelCount <= 0
        ) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        pendingInputFormat = inputAudioFormat
        pendingOutputFormat = AudioFormat(
            inputAudioFormat.sampleRate,
            inputAudioFormat.channelCount,
            C.ENCODING_PCM_FLOAT
        )
        return pendingOutputFormat
    }

    override fun isActive(): Boolean = pendingOutputFormat != AudioFormat.NOT_SET

    override fun queueInput(inputBuffer: ByteBuffer) {
        val limit = inputBuffer.limit()
        val bytesPerSample = if (inputFormat.encoding == C.ENCODING_PCM_16BIT) 2 else 4
        val sampleCount = (limit - inputBuffer.position()) / bytesPerSample
        if (sampleCount <= 0) {
            outputBuffer = AudioProcessor.EMPTY_BUFFER
            inputBuffer.position(limit)
            return
        }

        applyRequestedConfiguration(force = false)
        val frameCount = (sampleCount / inputFormat.channelCount).coerceAtLeast(1)
        val smoothing = (1.0 - exp(-frameCount.toDouble() / (inputFormat.sampleRate * PARAMETER_SMOOTHING_SECONDS)))
            .toFloat()
            .coerceIn(0f, 1f)
        banks[activeBank].approachTargets(smoothing)

        val output = replaceOutputBuffer(sampleCount * 4)
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        output.order(ByteOrder.LITTLE_ENDIAN)
        if (inputFormat.encoding == C.ENCODING_PCM_FLOAT) {
            processFloat(inputBuffer, output, sampleCount)
        } else {
            processPcm16(inputBuffer, output, sampleCount)
        }
        output.flip()
        inputBuffer.position(limit)
    }

    private fun processPcm16(input: ByteBuffer, output: ByteBuffer, sampleCount: Int) {
        var remaining = sampleCount
        while (remaining > 0) {
            processAndWrite(input.short / 32768f, output)
            remaining -= 1
        }
    }

    private fun processFloat(input: ByteBuffer, output: ByteBuffer, sampleCount: Int) {
        var remaining = sampleCount
        while (remaining > 0) {
            processAndWrite(input.float, output)
            remaining -= 1
        }
    }

    private fun processAndWrite(input: Float, output: ByteBuffer) {
        val current = banks[activeBank].process(input, channelIndex)
        val processed = if (transitionFramesRemaining > 0) {
            val next = banks[transitionBank].process(input, channelIndex)
            val progress = 1f - transitionFramesRemaining.toFloat() / transitionTotalFrames
            current + (next - current) * progress
        } else {
            current
        }
        output.putFloat(if (processed.isFinite()) processed else input)
        channelIndex += 1
        if (channelIndex == inputFormat.channelCount) {
            channelIndex = 0
            if (transitionFramesRemaining > 0) {
                transitionFramesRemaining -= 1
                if (transitionFramesRemaining == 0) {
                    activeBank = transitionBank
                    transitionBank = 1 - activeBank
                }
            }
        }
    }

    private fun applyRequestedConfiguration(force: Boolean) {
        val configuration = requested
        if (!force && configuration.revision == appliedRevision) return
        val profile = configuration.profile.takeIf { configuration.enabled }
        if (!force && transitionFramesRemaining > 0) {
            val completedFrames = transitionTotalFrames - transitionFramesRemaining
            if (completedFrames * 2 >= transitionTotalFrames) activeBank = transitionBank
            transitionBank = 1 - activeBank
            transitionFramesRemaining = 0
            transitionTotalFrames = 0
        }
        val current = banks[activeBank]
        if (force) {
            current.configure(profile, inputFormat.sampleRate, clearState = true)
            banks[1 - activeBank].configure(null, inputFormat.sampleRate, clearState = true)
            transitionFramesRemaining = 0
            transitionTotalFrames = 0
        } else if (current.hasSameTopology(profile, inputFormat.sampleRate)) {
            current.setTargets(profile, inputFormat.sampleRate)
        } else {
            transitionBank = 1 - activeBank
            banks[transitionBank].configure(profile, inputFormat.sampleRate, clearState = true)
            transitionTotalFrames = (inputFormat.sampleRate * TOPOLOGY_CROSSFADE_SECONDS).toInt().coerceAtLeast(1)
            transitionFramesRemaining = transitionTotalFrames
        }
        appliedRevision = configuration.revision
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
        if (pendingInputFormat != AudioFormat.NOT_SET) {
            inputFormat = pendingInputFormat
            outputFormat = pendingOutputFormat
            banks[0].prepareChannels(inputFormat.channelCount)
            banks[1].prepareChannels(inputFormat.channelCount)
            configured = true
        }
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
        channelIndex = 0
        banks[0].clearStates()
        banks[1].clearStates()
        transitionFramesRemaining = 0
        appliedRevision = Long.MIN_VALUE
        if (configured) applyRequestedConfiguration(force = true)
    }

    override fun reset() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        reusableBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
        configured = false
        channelIndex = 0
        appliedRevision = Long.MIN_VALUE
        transitionFramesRemaining = 0
        inputFormat = AudioFormat.NOT_SET
        outputFormat = AudioFormat.NOT_SET
        pendingInputFormat = AudioFormat.NOT_SET
        pendingOutputFormat = AudioFormat.NOT_SET
        banks[0].releaseChannels()
        banks[1].releaseChannels()
    }

    private fun replaceOutputBuffer(size: Int): ByteBuffer {
        if (reusableBuffer.capacity() < size) {
            reusableBuffer = ByteBuffer.allocateDirect(size).order(ByteOrder.LITTLE_ENDIAN)
        } else {
            reusableBuffer.clear()
        }
        outputBuffer = reusableBuffer
        return reusableBuffer
    }

    private class FilterBank {
        private val coefficients = Array(ParametricEqualizer.MAX_BANDS) { BiquadCoefficients() }
        private val targets = Array(ParametricEqualizer.MAX_BANDS) { BiquadCoefficients() }
        private val types = IntArray(ParametricEqualizer.MAX_BANDS)
        private val frequencies = FloatArray(ParametricEqualizer.MAX_BANDS)
        private val qValues = FloatArray(ParametricEqualizer.MAX_BANDS)
        private var states = emptyArray<Array<FilterState>>()
        private var filterCount = 0
        private var preampLinear = 1f
        private var targetPreampLinear = 1f

        fun prepareChannels(channelCount: Int) {
            if (states.size != channelCount) {
                states = Array(channelCount) { Array(ParametricEqualizer.MAX_BANDS) { FilterState() } }
            } else {
                clearStates()
            }
        }

        fun releaseChannels() {
            states = emptyArray()
            filterCount = 0
            preampLinear = 1f
            targetPreampLinear = 1f
        }

        fun configure(profile: ParametricEqProfile?, sampleRate: Int, clearState: Boolean) {
            filterCount = writeTopology(profile, sampleRate, coefficients)
            var index = 0
            while (index < filterCount) {
                targets[index].copyFrom(coefficients[index])
                index += 1
            }
            targetPreampLinear = dbToLinear(profile?.preampDb ?: 0f)
            preampLinear = targetPreampLinear
            if (clearState) clearStates()
        }

        fun hasSameTopology(profile: ParametricEqProfile?, sampleRate: Int): Boolean {
            val bands = profile?.bands.orEmpty()
            val nyquistLimit = sampleRate * NYQUIST_FRACTION
            var expectedIndex = 0
            var index = 0
            while (index < bands.size) {
                val band = bands[index]
                if (band.enabled && band.frequencyHz < nyquistLimit) {
                    if (expectedIndex >= filterCount) return false
                    expectedIndex += 1
                }
                index += 1
            }
            return expectedIndex == filterCount
        }

        fun setTargets(profile: ParametricEqProfile?, sampleRate: Int) {
            val written = writeTopology(profile, sampleRate, targets)
            if (written != filterCount) return
            targetPreampLinear = dbToLinear(profile?.preampDb ?: 0f)
        }

        fun approachTargets(amount: Float) {
            var index = 0
            while (index < filterCount) {
                coefficients[index].approach(targets[index], amount)
                index += 1
            }
            preampLinear += (targetPreampLinear - preampLinear) * amount
        }

        fun process(input: Float, channel: Int): Float {
            var value = input
            var index = 0
            while (index < filterCount) {
                val coefficient = coefficients[index]
                val state = states[channel][index]
                val filtered = coefficient.b0 * value + state.z1
                state.z1 = coefficient.b1 * value - coefficient.a1 * filtered + state.z2
                state.z2 = coefficient.b2 * value - coefficient.a2 * filtered
                if (!filtered.isFinite() || !state.z1.isFinite() || !state.z2.isFinite()) {
                    state.clear()
                } else {
                    value = filtered
                }
                index += 1
            }
            return value * preampLinear
        }

        fun clearStates() {
            var channel = 0
            while (channel < states.size) {
                var band = 0
                while (band < states[channel].size) {
                    states[channel][band].clear()
                    band += 1
                }
                channel += 1
            }
        }

        private fun writeTopology(
            profile: ParametricEqProfile?,
            sampleRate: Int,
            destination: Array<BiquadCoefficients>
        ): Int {
            val bands = profile?.bands.orEmpty()
            val nyquistLimit = sampleRate * NYQUIST_FRACTION
            var count = 0
            var index = 0
            while (index < bands.size && count < ParametricEqualizer.MAX_BANDS) {
                val band = bands[index]
                if (band.enabled && band.frequencyHz < nyquistLimit && destination[count].set(band, sampleRate)) {
                    types[count] = band.filterType.ordinal
                    frequencies[count] = band.frequencyHz
                    qValues[count] = band.q
                    count += 1
                }
                index += 1
            }
            return count
        }

        private fun dbToLinear(db: Float): Float = 10.0.pow(db / 20.0).toFloat()
    }

    internal class BiquadCoefficients {
        var b0 = 1f
            private set
        var b1 = 0f
            private set
        var b2 = 0f
            private set
        var a1 = 0f
            private set
        var a2 = 0f
            private set

        private val design = DoubleArray(ParametricBiquad.COEFFICIENT_COUNT)

        fun set(band: ParametricEqBand, sampleRate: Int): Boolean {
            if (!ParametricBiquad.design(band, sampleRate, design)) return false
            return assign(design[0], design[1], design[2], design[3], design[4])
        }

        fun copyFrom(other: BiquadCoefficients) {
            b0 = other.b0
            b1 = other.b1
            b2 = other.b2
            a1 = other.a1
            a2 = other.a2
        }

        fun approach(target: BiquadCoefficients, amount: Float) {
            b0 += (target.b0 - b0) * amount
            b1 += (target.b1 - b1) * amount
            b2 += (target.b2 - b2) * amount
            a1 += (target.a1 - a1) * amount
            a2 += (target.a2 - a2) * amount
        }

        private fun assign(
            newB0: Double,
            newB1: Double,
            newB2: Double,
            newA1: Double,
            newA2: Double
        ): Boolean {
            if (!newB0.isFinite() || !newB1.isFinite() || !newB2.isFinite() ||
                !newA1.isFinite() || !newA2.isFinite() || !stable(newA1, newA2)
            ) return false
            b0 = newB0.toFloat()
            b1 = newB1.toFloat()
            b2 = newB2.toFloat()
            a1 = newA1.toFloat()
            a2 = newA2.toFloat()
            return b0.isFinite() && b1.isFinite() && b2.isFinite() && a1.isFinite() && a2.isFinite() &&
                stable(a1.toDouble(), a2.toDouble())
        }

        private fun stable(a1: Double, a2: Double): Boolean =
            1.0 + a1 + a2 > STABILITY_EPSILON &&
                1.0 - a1 + a2 > STABILITY_EPSILON &&
                1.0 - a2 > STABILITY_EPSILON
    }

    private class FilterState(var z1: Float = 0f, var z2: Float = 0f) {
        fun clear() {
            z1 = 0f
            z2 = 0f
        }
    }

    companion object {
        private const val PARAMETER_SMOOTHING_SECONDS = 0.035
        private const val TOPOLOGY_CROSSFADE_SECONDS = 0.02
        private const val NYQUIST_FRACTION = 0.49f
        private const val STABILITY_EPSILON = 1e-9
    }
}
