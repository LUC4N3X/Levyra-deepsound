package com.luc4n3x.levyra.player.enhanced

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tanh

/**
 * High-fidelity, conservative DSP audio restoration engine for Levyra.
 *
 * Implements:
 * 1. Mid/Side stereo coherence protection (M = 0.5*(L+R), S = 0.5*(L-R)).
 * 2. Band-isolated harmonic extension with 2x oversampling on the residual branch
 *    to reduce ultrasonic aliasing foldback into the audible spectrum.
 * 3. Side excitation controlled by measured stereo coherence with complete
 *    anti-phase suppression and hard-panning isolation.
 * 4. Micro-transient envelope dynamics unmasking.
 * 5. Internal soft-knee headroom limiter preventing clipping before the output sink.
 *
 * Designed with zero allocations in the hot loop for minimal CPU and battery consumption.
 */
class DspRestorationEngine : EnhancedAudioEngine {
    override val name: String = "Levyra DSP Restoration"

    private var sampleRate: Int = 44_100
    private var channels: Int = 2
    private var config: EnhancedAudioConfig = EnhancedAudioConfig()

    // Biquad coefficients: Mid source bandpass at Fs (8-15 kHz)
    private var bqMidB0 = 1f
    private var bqMidB1 = 0f
    private var bqMidB2 = 0f
    private var bqMidA1 = 0f
    private var bqMidA2 = 0f

    // Biquad coefficients: 2x Interpolation Lowpass at 2*Fs (~15.5 kHz)
    private var bqInterpB0 = 1f
    private var bqInterpB1 = 0f
    private var bqInterpB2 = 0f
    private var bqInterpA1 = 0f
    private var bqInterpA2 = 0f

    // Biquad coefficients: 2x Air Bandpass at 2*Fs (~19 kHz)
    private var bqAirB0 = 1f
    private var bqAirB1 = 0f
    private var bqAirB2 = 0f
    private var bqAirA1 = 0f
    private var bqAirA2 = 0f

    // Biquad coefficients: 2x Anti-Aliasing Lowpass at 2*Fs (~21 kHz)
    private var bqAaB0 = 1f
    private var bqAaB1 = 0f
    private var bqAaB2 = 0f
    private var bqAaA1 = 0f
    private var bqAaA2 = 0f

    // Filter states for Mid channel (direct form I)
    private var midBqMidX1 = 0f
    private var midBqMidX2 = 0f
    private var midBqMidY1 = 0f
    private var midBqMidY2 = 0f

    private var midBqInterpX1 = 0f
    private var midBqInterpX2 = 0f
    private var midBqInterpY1 = 0f
    private var midBqInterpY2 = 0f

    private var midBqAirX1 = 0f
    private var midBqAirX2 = 0f
    private var midBqAirY1 = 0f
    private var midBqAirY2 = 0f

    private var midBqAaX1 = 0f
    private var midBqAaX2 = 0f
    private var midBqAaY1 = 0f
    private var midBqAaY2 = 0f

    // Filter states for Side channel (direct form I)
    private var sideBqMidX1 = 0f
    private var sideBqMidX2 = 0f
    private var sideBqMidY1 = 0f
    private var sideBqMidY2 = 0f

    private var sideBqInterpX1 = 0f
    private var sideBqInterpX2 = 0f
    private var sideBqInterpY1 = 0f
    private var sideBqInterpY2 = 0f

    private var sideBqAirX1 = 0f
    private var sideBqAirX2 = 0f
    private var sideBqAirY1 = 0f
    private var sideBqAirY2 = 0f

    private var sideBqAaX1 = 0f
    private var sideBqAaX2 = 0f
    private var sideBqAaY1 = 0f
    private var sideBqAaY2 = 0f

    // Transient envelope followers
    private var envFastM = 0f
    private var envSlowM = 0f

    override fun configure(sampleRateHz: Int, channelCount: Int, config: EnhancedAudioConfig) {
        this.sampleRate = sampleRateHz.coerceAtLeast(8_000)
        this.channels = channelCount.coerceIn(1, MAX_CHANNELS)
        this.config = config.normalized()
        calculateCoefficients()
        reset()
    }

    private fun calculateCoefficients() {
        val sr = sampleRate.toDouble()
        val nyquist = sr / 2.0

        // 1. Source bandpass at Fs (8-15 kHz)
        val midCenter = min(11_500.0, nyquist * 0.65)
        val midQ = 1.0
        val w0Mid = 2.0 * PI * midCenter / sr
        val alphaMid = sin(w0Mid) / (2.0 * midQ)
        val a0Mid = 1.0 + alphaMid

        bqMidB0 = (alphaMid / a0Mid).toFloat()
        bqMidB1 = 0f
        bqMidB2 = (-alphaMid / a0Mid).toFloat()
        bqMidA1 = (-2.0 * cos(w0Mid) / a0Mid).toFloat()
        bqMidA2 = ((1.0 - alphaMid) / a0Mid).toFloat()

        // 2x oversampling domain calculations
        val sr2x = sr * 2.0

        // 2. Interpolation lowpass at 2*Fs (~15.5 kHz)
        val interpCutoff = min(15_500.0, nyquist * 0.85)
        val w0Interp = 2.0 * PI * interpCutoff / sr2x
        val alphaInterp = sin(w0Interp) / (2.0 * 0.70710678)
        val a0Interp = 1.0 + alphaInterp
        val cosInterp = cos(w0Interp)

        bqInterpB0 = ((1.0 - cosInterp) * 0.5 / a0Interp).toFloat()
        bqInterpB1 = ((1.0 - cosInterp) / a0Interp).toFloat()
        bqInterpB2 = ((1.0 - cosInterp) * 0.5 / a0Interp).toFloat()
        bqInterpA1 = (-2.0 * cosInterp / a0Interp).toFloat()
        bqInterpA2 = ((1.0 - alphaInterp) / a0Interp).toFloat()

        // 3. Air band bandpass at 2*Fs (~19 kHz)
        val airCenter = min(19_000.0, nyquist * 0.92)
        val airQ = 1.2
        val w0Air = 2.0 * PI * airCenter / sr2x
        val alphaAir = sin(w0Air) / (2.0 * airQ)
        val a0Air = 1.0 + alphaAir

        bqAirB0 = (alphaAir / a0Air).toFloat()
        bqAirB1 = 0f
        bqAirB2 = (-alphaAir / a0Air).toFloat()
        bqAirA1 = (-2.0 * cos(w0Air) / a0Air).toFloat()
        bqAirA2 = ((1.0 - alphaAir) / a0Air).toFloat()

        // 4. Anti-aliasing lowpass at 2*Fs (~21 kHz)
        val aaCutoff = min(21_000.0, nyquist * 0.98)
        val w0Aa = 2.0 * PI * aaCutoff / sr2x
        val alphaAa = sin(w0Aa) / (2.0 * 0.70710678)
        val a0Aa = 1.0 + alphaAa
        val cosAa = cos(w0Aa)

        bqAaB0 = ((1.0 - cosAa) * 0.5 / a0Aa).toFloat()
        bqAaB1 = ((1.0 - cosAa) / a0Aa).toFloat()
        bqAaB2 = ((1.0 - cosAa) * 0.5 / a0Aa).toFloat()
        bqAaA1 = (-2.0 * cosAa / a0Aa).toFloat()
        bqAaA2 = ((1.0 - alphaAa) / a0Aa).toFloat()
    }

    override fun process(
        input: FloatArray,
        output: FloatArray,
        offset: Int,
        frames: Int,
        adaptiveResidualGain: Float,
        stereoCoherence: Float
    ): Boolean {
        if (frames <= 0) return true

        if (adaptiveResidualGain <= 0f) {
            val total = frames * channels
            System.arraycopy(input, offset, output, offset, total)
            return true
        }

        val harmonicGain = config.harmonicGain
        val transientSense = config.transientSensitivity
        val ceiling = config.truePeakCeilingLinear
        val ceilingMargin = (1f - ceiling).coerceAtLeast(0.001f)

        val ch = channels
        var idx = offset

        val allowSide = ch >= 2 && stereoCoherence > 0.05f
        val sideScale = if (allowSide) (stereoCoherence * 0.5f).coerceIn(0f, 0.5f) else 0f

        for (f in 0 until frames) {
            if (ch >= 2) {
                val origL = input[idx]
                val origR = input[idx + 1]

                val mid = (origL + origR) * 0.5f
                val side = (origL - origR) * 0.5f

                // Mid bandpass at Fs
                val midBand = filterMidSource(mid)

                // 2x oversampled harmonic generation on Mid
                val airMid = process2xHarmonicMid(midBand, harmonicGain)

                // Envelope follower on Mid
                val absM = abs(midBand)
                envFastM = envFastM * 0.85f + absM * 0.15f
                envSlowM = envSlowM * 0.98f + absM * 0.02f
                val transientDelta = max(0f, envFastM - envSlowM)
                val transientMultiplier = 1f + transientDelta * transientSense

                val resM = airMid * transientMultiplier

                // Always advance the Side filters so silence/coherence gating cannot freeze stale IIR history.
                val sideBand = filterSideSource(side)
                val airSide = process2xHarmonicSide(sideBand, harmonicGain)
                val resS = if (allowSide && abs(side) > 1e-6f) {
                    airSide * transientMultiplier * sideScale
                } else {
                    0f
                }

                val rawResL = resM + resS
                val rawResR = resM - resS

                // Hard pan protection: taper residual if channel is near silence
                val actL = (abs(origL) / 0.005f).coerceIn(0f, 1f)
                val actR = (abs(origR) / 0.005f).coerceIn(0f, 1f)

                val resL = rawResL * actL
                val resR = rawResR * actR

                val blendedL = origL + resL * adaptiveResidualGain
                val blendedR = origR + resR * adaptiveResidualGain

                output[idx] = applySoftKneeLimiter(blendedL, ceiling, ceilingMargin)
                output[idx + 1] = applySoftKneeLimiter(blendedR, ceiling, ceilingMargin)

                for (c in 2 until ch) {
                    output[idx + c] = input[idx + c]
                }
            } else {
                val orig = input[idx]
                val midBand = filterMidSource(orig)
                val airMid = process2xHarmonicMid(midBand, harmonicGain)

                val absM = abs(midBand)
                envFastM = envFastM * 0.85f + absM * 0.15f
                envSlowM = envSlowM * 0.98f + absM * 0.02f
                val transientDelta = max(0f, envFastM - envSlowM)
                val transientMultiplier = 1f + transientDelta * transientSense

                val res = airMid * transientMultiplier
                val blended = orig + res * adaptiveResidualGain
                output[idx] = applySoftKneeLimiter(blended, ceiling, ceilingMargin)
            }
            idx += ch
        }

        return true
    }

    private fun process2xHarmonicMid(midBand: Float, harmonicGain: Float): Float {
        val u0 = filterMidInterp(2.0f * midBand)
        val x0 = u0.coerceIn(-1.5f, 1.5f)
        val h2_0 = x0 * x0 * 0.5f
        val h3_0 = (4f * x0 * x0 * x0 - 3f * x0) * 0.15f
        val raw0 = (h2_0 + h3_0) * harmonicGain
        val air0 = filterMidAir(raw0)
        filterMidAa(air0)

        val u1 = filterMidInterp(0.0f)
        val x1 = u1.coerceIn(-1.5f, 1.5f)
        val h2_1 = x1 * x1 * 0.5f
        val h3_1 = (4f * x1 * x1 * x1 - 3f * x1) * 0.15f
        val raw1 = (h2_1 + h3_1) * harmonicGain
        val air1 = filterMidAir(raw1)
        return filterMidAa(air1)
    }

    private fun process2xHarmonicSide(sideBand: Float, harmonicGain: Float): Float {
        val u0 = filterSideInterp(2.0f * sideBand)
        val x0 = u0.coerceIn(-1.5f, 1.5f)
        val h2_0 = x0 * x0 * 0.5f
        val h3_0 = (4f * x0 * x0 * x0 - 3f * x0) * 0.15f
        val raw0 = (h2_0 + h3_0) * harmonicGain
        val air0 = filterSideAir(raw0)
        filterSideAa(air0)

        val u1 = filterSideInterp(0.0f)
        val x1 = u1.coerceIn(-1.5f, 1.5f)
        val h2_1 = x1 * x1 * 0.5f
        val h3_1 = (4f * x1 * x1 * x1 - 3f * x1) * 0.15f
        val raw1 = (h2_1 + h3_1) * harmonicGain
        val air1 = filterSideAir(raw1)
        return filterSideAa(air1)
    }

    private fun applySoftKneeLimiter(sample: Float, ceiling: Float, ceilingMargin: Float): Float {
        val absS = abs(sample)
        val limited = if (absS > ceiling) {
            val excess = (absS - ceiling) / ceilingMargin
            val sign = if (sample >= 0f) 1f else -1f
            sign * (ceiling + ceilingMargin * tanh(excess.toDouble()).toFloat())
        } else {
            sample
        }
        return limited.coerceIn(-1f, 1f)
    }

    private fun filterMidSource(x: Float): Float {
        val y = bqMidB0 * x + bqMidB1 * midBqMidX1 + bqMidB2 * midBqMidX2 -
            bqMidA1 * midBqMidY1 - bqMidA2 * midBqMidY2
        midBqMidX2 = midBqMidX1
        midBqMidX1 = x
        midBqMidY2 = midBqMidY1
        midBqMidY1 = y
        return y
    }

    private fun filterMidInterp(x: Float): Float {
        val y = bqInterpB0 * x + bqInterpB1 * midBqInterpX1 + bqInterpB2 * midBqInterpX2 -
            bqInterpA1 * midBqInterpY1 - bqInterpA2 * midBqInterpY2
        midBqInterpX2 = midBqInterpX1
        midBqInterpX1 = x
        midBqInterpY2 = midBqInterpY1
        midBqInterpY1 = y
        return y
    }

    private fun filterMidAir(x: Float): Float {
        val y = bqAirB0 * x + bqAirB1 * midBqAirX1 + bqAirB2 * midBqAirX2 -
            bqAirA1 * midBqAirY1 - bqAirA2 * midBqAirY2
        midBqAirX2 = midBqAirX1
        midBqAirX1 = x
        midBqAirY2 = midBqAirY1
        midBqAirY1 = y
        return y
    }

    private fun filterMidAa(x: Float): Float {
        val y = bqAaB0 * x + bqAaB1 * midBqAaX1 + bqAaB2 * midBqAaX2 -
            bqAaA1 * midBqAaY1 - bqAaA2 * midBqAaY2
        midBqAaX2 = midBqAaX1
        midBqAaX1 = x
        midBqAaY2 = midBqAaY1
        midBqAaY1 = y
        return y
    }

    private fun filterSideSource(x: Float): Float {
        val y = bqMidB0 * x + bqMidB1 * sideBqMidX1 + bqMidB2 * sideBqMidX2 -
            bqMidA1 * sideBqMidY1 - bqMidA2 * sideBqMidY2
        sideBqMidX2 = sideBqMidX1
        sideBqMidX1 = x
        sideBqMidY2 = sideBqMidY1
        sideBqMidY1 = y
        return y
    }

    private fun filterSideInterp(x: Float): Float {
        val y = bqInterpB0 * x + bqInterpB1 * sideBqInterpX1 + bqInterpB2 * sideBqInterpX2 -
            bqInterpA1 * sideBqInterpY1 - bqInterpA2 * sideBqInterpY2
        sideBqInterpX2 = sideBqInterpX1
        sideBqInterpX1 = x
        sideBqInterpY2 = sideBqInterpY1
        sideBqInterpY1 = y
        return y
    }

    private fun filterSideAir(x: Float): Float {
        val y = bqAirB0 * x + bqAirB1 * sideBqAirX1 + bqAirB2 * sideBqAirX2 -
            bqAirA1 * sideBqAirY1 - bqAirA2 * sideBqAirY2
        sideBqAirX2 = sideBqAirX1
        sideBqAirX1 = x
        sideBqAirY2 = sideBqAirY1
        sideBqAirY1 = y
        return y
    }

    private fun filterSideAa(x: Float): Float {
        val y = bqAaB0 * x + bqAaB1 * sideBqAaX1 + bqAaB2 * sideBqAaX2 -
            bqAaA1 * sideBqAaY1 - bqAaA2 * sideBqAaY2
        sideBqAaX2 = sideBqAaX1
        sideBqAaX1 = x
        sideBqAaY2 = sideBqAaY1
        sideBqAaY1 = y
        return y
    }

    override fun reset() {
        midBqMidX1 = 0f
        midBqMidX2 = 0f
        midBqMidY1 = 0f
        midBqMidY2 = 0f
        midBqInterpX1 = 0f
        midBqInterpX2 = 0f
        midBqInterpY1 = 0f
        midBqInterpY2 = 0f
        midBqAirX1 = 0f
        midBqAirX2 = 0f
        midBqAirY1 = 0f
        midBqAirY2 = 0f
        midBqAaX1 = 0f
        midBqAaX2 = 0f
        midBqAaY1 = 0f
        midBqAaY2 = 0f

        sideBqMidX1 = 0f
        sideBqMidX2 = 0f
        sideBqMidY1 = 0f
        sideBqMidY2 = 0f
        sideBqInterpX1 = 0f
        sideBqInterpX2 = 0f
        sideBqInterpY1 = 0f
        sideBqInterpY2 = 0f
        sideBqAirX1 = 0f
        sideBqAirX2 = 0f
        sideBqAirY1 = 0f
        sideBqAirY2 = 0f
        sideBqAaX1 = 0f
        sideBqAaX2 = 0f
        sideBqAaY1 = 0f
        sideBqAaY2 = 0f

        envFastM = 0f
        envSlowM = 0f
    }

    override fun release() {
        reset()
    }

    companion object {
        private const val MAX_CHANNELS = 8
    }
}
