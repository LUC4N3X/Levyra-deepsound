package com.luc4n3x.levyra.player.enhanced

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

class DspRestorationEngine : EnhancedAudioEngine {
    override val name: String = "Levyra Band Replication"

    private var sampleRate = 44_100
    private var channels = 2
    private var config = EnhancedAudioConfig()

    private var cutoffHz = 0f
    private var shiftHz = 0f
    private var currentGain = 0f

    private var sourceHighPass = Array(0) { Biquad() }
    private var sourceLowPass = Array(0) { Biquad() }
    private var outputHighPass = Array(0) { Biquad() }
    private var hilbertA = Array(0) { AllpassChain(HILBERT_A) }
    private var hilbertB = Array(0) { AllpassChain(HILBERT_B) }
    private var delayedB = FloatArray(0)

    private var oscCos = 1.0
    private var oscSin = 0.0
    private var stepCos = 1.0
    private var stepSin = 0.0
    private var oscSamples = 0

    override fun configure(sampleRateHz: Int, channelCount: Int, config: EnhancedAudioConfig) {
        sampleRate = sampleRateHz.coerceAtLeast(8_000)
        channels = channelCount.coerceIn(1, 8)
        this.config = config.normalized()
        val filterCount = channels * 2
        sourceHighPass = Array(filterCount) { Biquad() }
        sourceLowPass = Array(filterCount) { Biquad() }
        outputHighPass = Array(filterCount) { Biquad() }
        hilbertA = Array(channels) { AllpassChain(HILBERT_A) }
        hilbertB = Array(channels) { AllpassChain(HILBERT_B) }
        delayedB = FloatArray(channels)
        cutoffHz = 0f
        shiftHz = 0f
        reset()
    }

    override fun process(
        input: FloatArray,
        output: FloatArray,
        offset: Int,
        frames: Int,
        adaptiveResidualGain: Float,
        stereoCoherence: Float
    ): Boolean = render(input, output, offset, frames, cutoffHz, adaptiveResidualGain)

    override fun process(
        input: FloatArray,
        output: FloatArray,
        offset: Int,
        frames: Int,
        metrics: MutableEnhancedAudioMetrics
    ): Boolean = render(input, output, offset, frames, metrics.spectralCutoffHz, metrics.adaptiveResidualGain)

    override fun process(
        input: FloatArray,
        output: FloatArray,
        offset: Int,
        frames: Int,
        metrics: EnhancedAudioMetrics
    ): Boolean = render(input, output, offset, frames, metrics.spectralCutoffHz, metrics.adaptiveResidualGain)

    private fun render(
        input: FloatArray,
        output: FloatArray,
        offset: Int,
        frames: Int,
        requestedCutoffHz: Float,
        targetGain: Float
    ): Boolean {
        if (frames <= 0) return true
        val total = frames * channels
        val nyquist = sampleRate / 2f
        val usable = requestedCutoffHz > 0f && requestedCutoffHz < nyquist - MIN_PATCH_HZ
        if (usable && abs(requestedCutoffHz - cutoffHz) > RETUNE_TOLERANCE_HZ) retune(requestedCutoffHz)
        val endGain = if (usable) targetGain.coerceIn(0f, 1f) else 0f
        if (cutoffHz <= 0f || (endGain == 0f && currentGain == 0f)) {
            System.arraycopy(input, offset, output, offset, total)
            currentGain = 0f
            return true
        }
        val ceiling = config.truePeakCeilingLinear
        val margin = (1f - ceiling).coerceAtLeast(0.001f)
        val gainStep = (endGain - currentGain) / frames
        var gain = currentGain
        var index = offset
        for (frame in 0 until frames) {
            gain += gainStep
            val c = oscCos.toFloat()
            val s = oscSin.toFloat()
            for (channel in 0 until channels) {
                val dry = input[index + channel]
                val filterIndex = channel * 2
                var band = sourceHighPass[filterIndex].process(dry)
                band = sourceHighPass[filterIndex + 1].process(band)
                band = sourceLowPass[filterIndex].process(band)
                band = sourceLowPass[filterIndex + 1].process(band)
                val inPhase = hilbertA[channel].process(band)
                val quadrature = delayedB[channel]
                delayedB[channel] = hilbertB[channel].process(band)
                var patch = inPhase * c - quadrature * s
                patch = outputHighPass[filterIndex].process(patch)
                patch = outputHighPass[filterIndex + 1].process(patch)
                output[index + channel] = softLimit(dry + patch * gain, ceiling, margin)
            }
            advanceOscillator()
            index += channels
        }
        currentGain = endGain
        return true
    }

    private fun retune(newCutoffHz: Float) {
        val nyquist = sampleRate / 2f
        cutoffHz = newCutoffHz
        shiftHz = min(newCutoffHz * MAX_PATCH_RATIO, nyquist - newCutoffHz - GUARD_HZ).coerceAtLeast(MIN_PATCH_HZ)
        val sourceLow = newCutoffHz - shiftHz
        for (filter in sourceHighPass) filter.setHighPass(sourceLow, sampleRate)
        for (filter in sourceLowPass) filter.setLowPass(newCutoffHz - GUARD_HZ, sampleRate)
        for (filter in outputHighPass) filter.setHighPass(newCutoffHz, sampleRate)
        val w = 2.0 * PI * shiftHz / sampleRate
        stepCos = cos(w)
        stepSin = sin(w)
    }

    private fun advanceOscillator() {
        val nextCos = oscCos * stepCos - oscSin * stepSin
        val nextSin = oscSin * stepCos + oscCos * stepSin
        oscCos = nextCos
        oscSin = nextSin
        oscSamples++
        if (oscSamples >= RENORMALIZE_INTERVAL) {
            val magnitude = sqrt(oscCos * oscCos + oscSin * oscSin)
            oscCos /= magnitude
            oscSin /= magnitude
            oscSamples = 0
        }
    }

    private fun softLimit(sample: Float, ceiling: Float, margin: Float): Float {
        val magnitude = abs(sample)
        if (magnitude <= ceiling) return sample
        val sign = if (sample >= 0f) 1f else -1f
        return (sign * (ceiling + margin * tanh(((magnitude - ceiling) / margin).toDouble()).toFloat())).coerceIn(-1f, 1f)
    }

    override fun reset() {
        sourceHighPass.forEach(Biquad::clear)
        sourceLowPass.forEach(Biquad::clear)
        outputHighPass.forEach(Biquad::clear)
        hilbertA.forEach(AllpassChain::clear)
        hilbertB.forEach(AllpassChain::clear)
        delayedB.fill(0f)
        oscCos = 1.0
        oscSin = 0.0
        oscSamples = 0
        currentGain = 0f
    }

    override fun release() {
        reset()
    }

    private class Biquad {
        private var b0 = 1f
        private var b1 = 0f
        private var b2 = 0f
        private var a1 = 0f
        private var a2 = 0f
        private var x1 = 0f
        private var x2 = 0f
        private var y1 = 0f
        private var y2 = 0f

        fun setLowPass(frequencyHz: Float, sampleRate: Int) = design(frequencyHz, sampleRate, lowPass = true)

        fun setHighPass(frequencyHz: Float, sampleRate: Int) = design(frequencyHz, sampleRate, lowPass = false)

        private fun design(frequencyHz: Float, sampleRate: Int, lowPass: Boolean) {
            val w0 = 2.0 * PI * frequencyHz.coerceIn(20f, sampleRate * 0.49f) / sampleRate
            val alpha = sin(w0) / (2.0 * BUTTERWORTH_Q)
            val cosW = cos(w0)
            val a0 = 1.0 + alpha
            val numerator = if (lowPass) (1.0 - cosW) / 2.0 else (1.0 + cosW) / 2.0
            b0 = (numerator / a0).toFloat()
            b1 = ((if (lowPass) 1.0 - cosW else -(1.0 + cosW)) / a0).toFloat()
            b2 = b0
            a1 = (-2.0 * cosW / a0).toFloat()
            a2 = ((1.0 - alpha) / a0).toFloat()
        }

        fun process(x: Float): Float {
            val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1
            x1 = x
            y2 = y1
            y1 = y
            return y
        }

        fun clear() {
            x1 = 0f
            x2 = 0f
            y1 = 0f
            y2 = 0f
        }
    }

    private class AllpassChain(coefficients: FloatArray) {
        private val squared = FloatArray(coefficients.size) { coefficients[it] * coefficients[it] }
        private val x1 = FloatArray(coefficients.size)
        private val x2 = FloatArray(coefficients.size)
        private val y1 = FloatArray(coefficients.size)
        private val y2 = FloatArray(coefficients.size)

        fun process(input: Float): Float {
            var value = input
            for (i in squared.indices) {
                val y = squared[i] * (value + y2[i]) - x2[i]
                x2[i] = x1[i]
                x1[i] = value
                y2[i] = y1[i]
                y1[i] = y
                value = y
            }
            return value
        }

        fun clear() {
            x1.fill(0f)
            x2.fill(0f)
            y1.fill(0f)
            y2.fill(0f)
        }
    }

    private companion object {
        const val BUTTERWORTH_Q = 0.7071067811865476
        const val MAX_PATCH_RATIO = 0.35f
        const val MIN_PATCH_HZ = 1_000f
        const val GUARD_HZ = 150f
        const val RETUNE_TOLERANCE_HZ = 60f
        const val RENORMALIZE_INTERVAL = 4_096
        val HILBERT_A = floatArrayOf(0.6923878f, 0.9360654f, 0.9882295f, 0.9987488f)
        val HILBERT_B = floatArrayOf(0.4021921f, 0.8561711f, 0.9722910f, 0.9952885f)
    }
}
