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
 * Uses pre-allocated IIR sub-band filters to distinguish an abrupt lossy
 * high-frequency cutoff from a natural, gradual top-end rolloff.
 */
class EnhancedAudioAnalyzer(
    private var config: EnhancedAudioConfig = EnhancedAudioConfig()
) {
    private var sampleRate: Int = 44_100
    private var channels: Int = 2

    private var bqShoulderB0 = 0f
    private var bqShoulderB1 = 0f
    private var bqShoulderB2 = 0f
    private var bqShoulderA1 = 0f
    private var bqShoulderA2 = 0f
    private var bqShoulderX1 = 0f
    private var bqShoulderX2 = 0f
    private var bqShoulderY1 = 0f
    private var bqShoulderY2 = 0f

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

        if (nyquist > 11_000.0) {
            val shoulderCenter = min(12_500.0, nyquist * 0.60)
            val w0 = 2.0 * PI * shoulderCenter / sr
            val alpha = sin(w0) / 2.0
            val a0 = 1.0 + alpha
            bqShoulderB0 = (alpha / a0).toFloat()
            bqShoulderB1 = 0f
            bqShoulderB2 = (-alpha / a0).toFloat()
            bqShoulderA1 = (-2.0 * cos(w0) / a0).toFloat()
            bqShoulderA2 = ((1.0 - alpha) / a0).toFloat()
        }

        if (nyquist > 14_000.0) {
            val cutoffCenter = min(15_500.0, nyquist * 0.75)
            val cutoffQ = 1.0
            val w0C = 2.0 * PI * cutoffCenter / sr
            val alphaC = sin(w0C) / (2.0 * cutoffQ)
            val a0C = 1.0 + alphaC

            bqCutoffB0 = (alphaC / a0C).toFloat()
            bqCutoffB1 = 0f
            bqCutoffB2 = (-alphaC / a0C).toFloat()
            bqCutoffA1 = (-2.0 * cos(w0C) / a0C).toFloat()
            bqCutoffA2 = ((1.0 - alphaC) / a0C).toFloat()
        }

        if (nyquist > 19_000.0) {
            val airCenter = min(20_000.0, nyquist * 0.90)
            val airQ = 1.2
            val w0A = 2.0 * PI * airCenter / sr
            val alphaA = sin(w0A) / (2.0 * airQ)
            val a0A = 1.0 + alphaA

            bqAirB0 = (alphaA / a0A).toFloat()
            bqAirB1 = 0f
            bqAirB2 = (-alphaA / a0A).toFloat()
            bqAirA1 = (-2.0 * cos(w0A) / a0A).toFloat()
            bqAirA2 = ((1.0 - alphaA) / a0A).toFloat()
        }
    }

    fun updateConfig(newConfig: EnhancedAudioConfig) {
        this.config = newConfig.normalized()
    }

    fun reset() {
        bqShoulderX1 = 0f
        bqShoulderX2 = 0f
        bqShoulderY1 = 0f
        bqShoulderY2 = 0f
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

    private val reusableMetrics = MutableEnhancedAudioMetrics()

    fun analyze(
        input: FloatArray,
        offset: Int,
        frames: Int,
        startTimeUs: Long,
        target: MutableEnhancedAudioMetrics = this.reusableMetrics
    ): MutableEnhancedAudioMetrics {
        if (frames <= 0 || channels <= 0) return emptyMetrics(target)

        var maxPeak = 0f
        var sumSquaresLeft = 0.0
        var sumSquaresRight = 0.0
        var sumProductStereo = 0.0
        var transientEvents = 0
        var baseEnergyAcc = 0.0
        var shoulderEnergyAcc = 0.0
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

            if (abs(mono - prevMono) > 0.15f) transientEvents++
            prevMono = mono

            val yShoulder = filterShoulder(mono)
            val yCutoff = filterCutoff(mono)
            val yAir = filterAir(mono)

            baseEnergyAcc += (mono * mono).toDouble()
            shoulderEnergyAcc += (yShoulder * yShoulder).toDouble()
            cutoffEnergyAcc += (yCutoff * yCutoff).toDouble()
            airEnergyAcc += (yAir * yAir).toDouble()

            index += step
        }

        val norm = 1.0 / max(1, frames).toDouble()
        val baseRms = sqrt(baseEnergyAcc * norm).toFloat()
        val shoulderRms = sqrt(shoulderEnergyAcc * norm).toFloat()
        val cutoffRms = sqrt(cutoffEnergyAcc * norm).toFloat()
        val airRms = sqrt(airEnergyAcc * norm).toFloat()

        return evaluateDeficit(
            baseRms = baseRms,
            shoulderRms = shoulderRms,
            cutoffRms = cutoffRms,
            airRms = airRms,
            transientDensity = (transientEvents.toFloat() / frames.toFloat()).coerceIn(0f, 1f),
            stereoCoherence = calculateStereoCoherence(sumSquaresLeft, sumSquaresRight, sumProductStereo),
            maxPeak = maxPeak,
            startTimeUs = startTimeUs,
            target = target
        )
    }

    private fun filterShoulder(x: Float): Float {
        val y = bqShoulderB0 * x + bqShoulderB1 * bqShoulderX1 + bqShoulderB2 * bqShoulderX2 -
            bqShoulderA1 * bqShoulderY1 - bqShoulderA2 * bqShoulderY2
        bqShoulderX2 = bqShoulderX1
        bqShoulderX1 = x
        bqShoulderY2 = bqShoulderY1
        bqShoulderY1 = y
        return y
    }

    private fun filterCutoff(x: Float): Float {
        val y = bqCutoffB0 * x + bqCutoffB1 * bqCutoffX1 + bqCutoffB2 * bqCutoffX2 -
            bqCutoffA1 * bqCutoffY1 - bqCutoffA2 * bqCutoffY2
        bqCutoffX2 = bqCutoffX1
        bqCutoffX1 = x
        bqCutoffY2 = bqCutoffY1
        bqCutoffY1 = y
        return y
    }

    private fun filterAir(x: Float): Float {
        val y = bqAirB0 * x + bqAirB1 * bqAirX1 + bqAirB2 * bqAirX2 -
            bqAirA1 * bqAirY1 - bqAirA2 * bqAirY2
        bqAirX2 = bqAirX1
        bqAirX1 = x
        bqAirY2 = bqAirY1
        bqAirY1 = y
        return y
    }

    private fun calculateStereoCoherence(sqLeft: Double, sqRight: Double, prod: Double): Float {
        val denom = sqrt(sqLeft * sqRight)
        return if (denom > 1e-9) (prod / denom).toFloat().coerceIn(-1f, 1f) else 1f
    }

    private fun evaluateDeficit(
        baseRms: Float,
        shoulderRms: Float,
        cutoffRms: Float,
        airRms: Float,
        transientDensity: Float,
        stereoCoherence: Float,
        maxPeak: Float,
        startTimeUs: Long,
        target: MutableEnhancedAudioMetrics
    ): MutableEnhancedAudioMetrics {
        val nyquist = sampleRate / 2f
        if (baseRms < SILENCE_RMS_FLOOR || nyquist <= 19_000f) {
            smoothedConfidence *= SMOOTHING_FALLBACK
            return buildBypassMetrics(0f, 0f, transientDensity, stereoCoherence, maxPeak, startTimeUs, target)
        }

        val cutoffRatio = cutoffRms / max(1e-4f, baseRms)
        val airToCutoffRatio = airRms / max(1e-5f, cutoffRms)
        val cutoffToShoulderRatio = cutoffRms / max(1e-5f, shoulderRms)

        if (cutoffRatio < MIN_CUTOFF_TO_BASE_RATIO || cutoffRms < MIN_CUTOFF_RMS) {
            smoothedConfidence *= SMOOTHING_FALLBACK
            return buildBypassMetrics(nyquist, airToCutoffRatio, transientDensity, stereoCoherence, maxPeak, startTimeUs, target)
        }

        val hasCutoffEdge = cutoffToShoulderRatio >= MIN_EDGE_RATIO
        val lossyDropDetected = hasCutoffEdge && airToCutoffRatio < LOSSY_AIR_RATIO
        val naturalFullBand = airToCutoffRatio > FULL_BAND_AIR_RATIO

        val instantConfidence = when {
            !hasCutoffEdge || naturalFullBand -> 0f
            lossyDropDetected -> {
                val deficitSteepness = (1f - airToCutoffRatio / LOSSY_AIR_RATIO).coerceIn(0f, 1f)
                (0.35f + deficitSteepness * 0.55f).coerceIn(0.15f, 0.95f)
            }
            airToCutoffRatio < PARTIAL_AIR_RATIO ->
                ((PARTIAL_AIR_RATIO - airToCutoffRatio) / PARTIAL_AIR_RATIO * 0.4f).coerceIn(0f, 0.4f)
            else -> 0f
        }

        smoothedConfidence = smoothedConfidence * SMOOTHING_ATTACK + instantConfidence * (1f - SMOOTHING_ATTACK)
        val meetsThreshold = smoothedConfidence >= config.deficitThreshold
        val adaptiveGain = if (meetsThreshold) {
            (smoothedConfidence * config.adaptiveGainCap).coerceIn(0f, config.adaptiveGainCap)
        } else {
            0f
        }

        target.spectralCutoffHz = if (lossyDropDetected) config.cutoffFrequencyHz else nyquist
        target.hfEnergyRatio = airToCutoffRatio
        target.spectralHoleCount = if (lossyDropDetected) 1 else 0
        target.tonality = cutoffToShoulderRatio.coerceIn(0f, 1f)
        target.transientDensity = transientDensity
        target.stereoCoherence = stereoCoherence
        target.peakAmplitude = maxPeak
        target.deficitConfidence = smoothedConfidence
        target.adaptiveResidualGain = adaptiveGain
        target.processingTimeUs = System.nanoTime() / 1_000L - startTimeUs
        target.bypassed = !meetsThreshold
        target.bypassReason = if (!meetsThreshold) EnhancedAudioBypassReason.INSUFFICIENT_CONFIDENCE else null
        return target
    }

    private fun buildBypassMetrics(
        cutoffHz: Float,
        hfRatio: Float,
        transientDensity: Float,
        stereoCoherence: Float,
        maxPeak: Float,
        startTimeUs: Long,
        target: MutableEnhancedAudioMetrics
    ): MutableEnhancedAudioMetrics {
        target.spectralCutoffHz = cutoffHz
        target.hfEnergyRatio = hfRatio
        target.spectralHoleCount = 0
        target.tonality = 0.5f
        target.transientDensity = transientDensity
        target.stereoCoherence = stereoCoherence
        target.peakAmplitude = maxPeak
        target.deficitConfidence = 0f
        target.adaptiveResidualGain = 0f
        target.processingTimeUs = System.nanoTime() / 1_000L - startTimeUs
        target.bypassed = true
        target.bypassReason = EnhancedAudioBypassReason.INSUFFICIENT_CONFIDENCE
        return target
    }

    private fun emptyMetrics(target: MutableEnhancedAudioMetrics): MutableEnhancedAudioMetrics {
        target.reset()
        target.bypassed = true
        target.bypassReason = EnhancedAudioBypassReason.INSUFFICIENT_CONFIDENCE
        return target
    }

    companion object {
        private const val SILENCE_RMS_FLOOR = 0.001f
        private const val MIN_CUTOFF_RMS = 0.001f
        private const val MIN_CUTOFF_TO_BASE_RATIO = 0.01f
        private const val MIN_EDGE_RATIO = 0.45f
        private const val LOSSY_AIR_RATIO = 0.35f
        private const val PARTIAL_AIR_RATIO = 0.45f
        private const val FULL_BAND_AIR_RATIO = 0.55f
        private const val SMOOTHING_ATTACK = 0.70f
        private const val SMOOTHING_FALLBACK = 0.70f
    }
}
