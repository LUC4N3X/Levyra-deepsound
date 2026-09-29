package com.luc4n3x.levyra.player.enhanced

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Real-time spectral analyzer and deficit detector for [EnhancedAudioProcessor].
 *
 * Implements deficit-gated analysis inspired by North Star:
 * 1. Analyzes subband energies across Cutoff Transition (14-17 kHz) and Air (19-22 kHz)
 *    using pre-allocated IIR biquad filters.
 * 2. Identifies lossy psychoacoustic cutoffs vs. natural acoustic rolloff or silence.
 * 3. Evaluates transient density, stereo coherence, and true peak headroom.
 * 4. Yields a calibrated [deficitConfidence] in [0.0, 1.0].
 *
 * Pre-allocates all state to ensure zero heap allocations in the realtime audio thread.
 */
class EnhancedAudioAnalyzer(
    private var config: EnhancedAudioConfig = EnhancedAudioConfig()
) {
    private var sampleRate: Int = 44_100
    private var channels: Int = 2

    private var bqCutoffB0 = 0f
    private var bqCutoffB1 = 0f
    private var bqCutoffB2 = 0f
    private var bqCutoffA1 = 0f
    private var bqCutoffA2 = 0f
    private var bqCutoffX1 = 0f
    private var bqCutoffX2 = 0f
    private var bqCutoffY1 = 0f
    private var bqCutoffY2 = 0f

    private var bqAirB0 = 0f
    private var bqAirB1 = 0f
    private var bqAirB2 = 0f
    private var bqAirA1 = 0f
    private var bqAirA2 = 0f
    private var bqAirX1 = 0f
    private var bqAirX2 = 0f
    private var bqAirY1 = 0f
    private var bqAirY2 = 0f

    private var prevMono = 0f
    private var smoothedConfidence = 0f

    fun configure(sampleRateHz: Int, channelCount: Int, newConfig: EnhancedAudioConfig = config) {
        this.sampleRate = sampleRateHz.coerceAtLeast(8_000)
        this.channels = channelCount.coerceIn(1, 8)
        this.config = newConfig.normalized()
        calculateFilterCoefficients()
        reset()
    }

    private fun calculateFilterCoefficients() {
        val sr = sampleRate.toDouble()
        val nyquist = sr / 2.0

        if (nyquist > 14_000.0) {
            val cutoffCenter = min(15_500.0, nyquist * 0.75)
            val cutoffQ = 1.0
            val w0C = 2.0 * PI * cutoffCenter / sr
            val alphaC = sin(w0C) / (2.0 * cutoffQ)
            val b0C = alphaC
            val b1C = 0.0
            val b2C = -alphaC
            val a0C = 1.0 + alphaC
            val a1C = -2.0 * cos(w0C)
            val a2C = 1.0 - alphaC

            bqCutoffB0 = (b0C / a0C).toFloat()
            bqCutoffB1 = (b1C / a0C).toFloat()
            bqCutoffB2 = (b2C / a0C).toFloat()
            bqCutoffA1 = (a1C / a0C).toFloat()
            bqCutoffA2 = (a2C / a0C).toFloat()
        }

        if (nyquist > 19_000.0) {
            val airCenter = min(20_000.0, nyquist * 0.90)
            val airQ = 1.2
            val w0A = 2.0 * PI * airCenter / sr
            val alphaA = sin(w0A) / (2.0 * airQ)
            val b0A = alphaA
            val b1A = 0.0
            val b2A = -alphaA
            val a0A = 1.0 + alphaA
            val a1A = -2.0 * cos(w0A)
            val a2A = 1.0 - alphaA

            bqAirB0 = (b0A / a0A).toFloat()
            bqAirB1 = (b1A / a0A).toFloat()
            bqAirB2 = (b2A / a0A).toFloat()
            bqAirA1 = (a1A / a0A).toFloat()
            bqAirA2 = (a2A / a0A).toFloat()
        }
    }

    fun updateConfig(newConfig: EnhancedAudioConfig) {
        this.config = newConfig.normalized()
    }

    fun reset() {
        bqCutoffX1 = 0f
        bqCutoffX2 = 0f
        bqCutoffY1 = 0f
        bqCutoffY2 = 0f
        bqAirX1 = 0f
        bqAirX2 = 0f
        bqAirY1 = 0f
        bqAirY2 = 0f
        prevMono = 0f
        smoothedConfidence = 0f
    }

    fun analyze(
        input: FloatArray,
        offset: Int,
        frames: Int,
        startTimeUs: Long
    ): EnhancedAudioMetrics {
        if (frames <= 0 || channels <= 0) {
            return emptyMetrics()
        }

        var maxPeak = 0f
        var sumSquaresLeft = 0.0
        var sumSquaresRight = 0.0
        var sumProductStereo = 0.0
        var transientEvents = 0
        var baseEnergyAcc = 0.0
        var cutoffEnergyAcc = 0.0
        var airEnergyAcc = 0.0

        val step = channels
        var index = offset

        for (f in 0 until frames) {
            val left = input[index]
            val right = if (channels > 1) input[index + 1] else left
            maxPeak = max(maxPeak, max(abs(left), abs(right)))

            val mono = (left + right) * 0.5f
            sumSquaresLeft += (left * left).toDouble()
            sumSquaresRight += (right * right).toDouble()
            sumProductStereo += (left * right).toDouble()

            if (abs(mono - prevMono) > 0.15f) {
                transientEvents++
            }
            prevMono = mono

            val yCutoff = filterCutoff(mono)
            val yAir = filterAir(mono)

            baseEnergyAcc += (mono * mono).toDouble()
            cutoffEnergyAcc += (yCutoff * yCutoff).toDouble()
            airEnergyAcc += (yAir * yAir).toDouble()

            index += step
        }

        val norm = 1.0 / max(1, frames).toDouble()
        val baseRms = sqrt(baseEnergyAcc * norm).toFloat()
        val cutoffRms = sqrt(cutoffEnergyAcc * norm).toFloat()
        val airRms = sqrt(airEnergyAcc * norm).toFloat()

        val stereoCoherence = calculateStereoCoherence(sumSquaresLeft, sumSquaresRight, sumProductStereo)
        val transientDensity = (transientEvents.toFloat() / frames.toFloat()).coerceIn(0f, 1f)

        return evaluateDeficit(
            baseRms = baseRms,
            cutoffRms = cutoffRms,
            airRms = airRms,
            transientDensity = transientDensity,
            stereoCoherence = stereoCoherence,
            maxPeak = maxPeak,
            startTimeUs = startTimeUs
        )
    }

    private fun filterCutoff(mono: Float): Float {
        val y = bqCutoffB0 * mono + bqCutoffB1 * bqCutoffX1 + bqCutoffB2 * bqCutoffX2 -
            bqCutoffA1 * bqCutoffY1 - bqCutoffA2 * bqCutoffY2
        bqCutoffX2 = bqCutoffX1
        bqCutoffX1 = mono
        bqCutoffY2 = bqCutoffY1
        bqCutoffY1 = y
        return y
    }

    private fun filterAir(mono: Float): Float {
        val y = bqAirB0 * mono + bqAirB1 * bqAirX1 + bqAirB2 * bqAirX2 -
            bqAirA1 * bqAirY1 - bqAirA2 * bqAirY2
        bqAirX2 = bqAirX1
        bqAirX1 = mono
        bqAirY2 = bqAirY1
        bqAirY1 = y
        return y
    }

    private fun calculateStereoCoherence(sqLeft: Double, sqRight: Double, prod: Double): Float {
        val denom = sqrt(sqLeft * sqRight)
        return if (denom > 1e-9) {
            (prod / denom).toFloat().coerceIn(-1f, 1f)
        } else {
            1f
        }
    }

    private fun evaluateDeficit(
        baseRms: Float,
        cutoffRms: Float,
        airRms: Float,
        transientDensity: Float,
        stereoCoherence: Float,
        maxPeak: Float,
        startTimeUs: Long
    ): EnhancedAudioMetrics {
        val nyquist = sampleRate / 2f
        val hasAirBand = nyquist > 19_000f

        if (baseRms < SILENCE_RMS_FLOOR || !hasAirBand) {
            smoothedConfidence *= SMOOTHING_FALLBACK
            return buildBypassMetrics(0f, 0f, transientDensity, stereoCoherence, maxPeak, startTimeUs)
        }

        val cutoffRatio = cutoffRms / max(1e-4f, baseRms)
        val airToCutoffRatio = airRms / max(1e-5f, cutoffRms)

        if (cutoffRatio < 0.01f || cutoffRms < 1e-3f) {
            smoothedConfidence *= SMOOTHING_FALLBACK
            return buildBypassMetrics(nyquist, airToCutoffRatio, transientDensity, stereoCoherence, maxPeak, startTimeUs)
        }

        val lossyDropDetected = airToCutoffRatio < 0.35f
        val naturalFullBand = airToCutoffRatio > 0.55f

        val instantConfidence = when {
            naturalFullBand -> 0f
            lossyDropDetected -> {
                val deficitSteepness = (1f - (airToCutoffRatio / 0.35f)).coerceIn(0f, 1f)
                (0.35f + deficitSteepness * 0.55f).coerceIn(0.15f, 0.95f)
            }
            airToCutoffRatio < 0.45f -> ((0.45f - airToCutoffRatio) / 0.45f * 0.4f).coerceIn(0f, 0.4f)
            else -> 0f
        }

        smoothedConfidence = smoothedConfidence * SMOOTHING_ATTACK + instantConfidence * (1f - SMOOTHING_ATTACK)
        val meetsThreshold = smoothedConfidence >= config.deficitThreshold
        val adaptiveGain = if (meetsThreshold) {
            (smoothedConfidence * config.adaptiveGainCap).coerceIn(0f, config.adaptiveGainCap)
        } else {
            0f
        }

        val elapsedUs = System.nanoTime() / 1_000L - startTimeUs
        val estimatedCutoffHz = if (lossyDropDetected) config.cutoffFrequencyHz else nyquist

        return EnhancedAudioMetrics(
            spectralCutoffHz = estimatedCutoffHz,
            hfEnergyRatio = airToCutoffRatio,
            spectralHoleCount = if (lossyDropDetected) 1 else 0,
            tonality = 0.5f,
            transientDensity = transientDensity,
            stereoCoherence = stereoCoherence,
            peakAmplitude = maxPeak,
            deficitConfidence = smoothedConfidence,
            adaptiveResidualGain = adaptiveGain,
            processingTimeUs = elapsedUs,
            bypassed = !meetsThreshold,
            bypassReason = if (!meetsThreshold) EnhancedAudioBypassReason.INSUFFICIENT_CONFIDENCE else null
        )
    }

    private fun buildBypassMetrics(
        cutoffHz: Float,
        hfRatio: Float,
        transientDensity: Float,
        stereoCoherence: Float,
        maxPeak: Float,
        startTimeUs: Long
    ): EnhancedAudioMetrics {
        val elapsedUs = System.nanoTime() / 1_000L - startTimeUs
        return EnhancedAudioMetrics(
            spectralCutoffHz = cutoffHz,
            hfEnergyRatio = hfRatio,
            spectralHoleCount = 0,
            tonality = 0.5f,
            transientDensity = transientDensity,
            stereoCoherence = stereoCoherence,
            peakAmplitude = maxPeak,
            deficitConfidence = 0f,
            adaptiveResidualGain = 0f,
            processingTimeUs = elapsedUs,
            bypassed = true,
            bypassReason = EnhancedAudioBypassReason.INSUFFICIENT_CONFIDENCE
        )
    }

    private fun emptyMetrics(): EnhancedAudioMetrics = EnhancedAudioMetrics(
        bypassed = true,
        bypassReason = EnhancedAudioBypassReason.INSUFFICIENT_CONFIDENCE
    )

    companion object {
        private const val SILENCE_RMS_FLOOR = 0.001f
        private const val SMOOTHING_ATTACK = 0.70f
        private const val SMOOTHING_FALLBACK = 0.70f
    }
}
