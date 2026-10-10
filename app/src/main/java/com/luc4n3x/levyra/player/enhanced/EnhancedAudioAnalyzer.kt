package com.luc4n3x.levyra.player.enhanced

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class EnhancedAudioAnalyzer(
    private var config: EnhancedAudioConfig = EnhancedAudioConfig()
) {
    private var sampleRate: Int = 44_100
    private var channels: Int = 2

    private val ring = FloatArray(FFT_SIZE)
    private var ringPos = 0
    private var samplesSinceFrame = 0
    private val window = FloatArray(FFT_SIZE) { i -> (0.5 - 0.5 * cos(2.0 * PI * i / (FFT_SIZE - 1))).toFloat() }
    private val re = FloatArray(FFT_SIZE)
    private val im = FloatArray(FFT_SIZE)
    private val averagePower = FloatArray(FFT_SIZE / 2 + 1)
    private val levelDb = FloatArray(FFT_SIZE / 2 + 1)
    private val cosTable = FloatArray(FFT_SIZE / 2) { i -> cos(2.0 * PI * i / FFT_SIZE).toFloat() }
    private val sinTable = FloatArray(FFT_SIZE / 2) { i -> sin(2.0 * PI * i / FFT_SIZE).toFloat() }
    private var analyzedFrames = 0

    private var detectedCutoffHz = 0f
    private var detectedDropDb = 0f
    private var patchGain = 0f
    private var smoothedConfidence = 0f

    fun configure(sampleRateHz: Int, channelCount: Int, newConfig: EnhancedAudioConfig = config) {
        sampleRate = sampleRateHz.coerceAtLeast(8_000)
        channels = channelCount.coerceIn(1, 8)
        config = newConfig.normalized()
        reset()
    }

    fun updateConfig(newConfig: EnhancedAudioConfig) {
        config = newConfig.normalized()
    }

    fun reset() {
        ring.fill(0f)
        ringPos = 0
        samplesSinceFrame = 0
        averagePower.fill(0f)
        analyzedFrames = 0
        detectedCutoffHz = 0f
        detectedDropDb = 0f
        patchGain = 0f
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
        if (frames <= 0 || channels <= 0) return bypass(target, startTimeUs, 0f, 1f)
        var maxPeak = 0f
        var sumLeft = 0.0
        var sumRight = 0.0
        var sumProduct = 0.0
        var index = offset
        for (f in 0 until frames) {
            val left = input[index]
            val right = if (channels > 1) input[index + 1] else left
            maxPeak = max(maxPeak, max(abs(left), abs(right)))
            sumLeft += (left * left).toDouble()
            sumRight += (right * right).toDouble()
            sumProduct += (left * right).toDouble()
            ring[ringPos] = (left + right) * 0.5f
            ringPos = ringPos + 1 and FFT_SIZE - 1
            samplesSinceFrame++
            if (samplesSinceFrame >= analysisHop()) {
                samplesSinceFrame = 0
                analyzeFrame()
            }
            index += channels
        }
        val denom = sqrt(sumLeft * sumRight)
        val coherence = if (denom > 1e-9) (sumProduct / denom).toFloat().coerceIn(-1f, 1f) else 1f

        val instant = if (detectedCutoffHz > 0f) {
            ((detectedDropDb - MIN_DROP_DB) / DROP_RANGE_DB).coerceIn(0f, 1f) * 0.5f + 0.5f
        } else {
            0f
        }
        smoothedConfidence = smoothedConfidence * CONFIDENCE_SMOOTHING + instant * (1f - CONFIDENCE_SMOOTHING)
        if (detectedCutoffHz <= 0f || smoothedConfidence < config.deficitThreshold) {
            return bypass(target, startTimeUs, maxPeak, coherence)
        }
        target.spectralCutoffHz = detectedCutoffHz
        target.hfEnergyRatio = patchGain
        target.spectralHoleCount = 1
        target.tonality = 0f
        target.transientDensity = 0f
        target.stereoCoherence = coherence
        target.peakAmplitude = maxPeak
        target.deficitConfidence = smoothedConfidence
        target.adaptiveResidualGain = (patchGain * smoothedConfidence).coerceIn(0f, 1f)
        target.processingTimeUs = System.nanoTime() / 1_000L - startTimeUs
        target.bypassed = false
        target.bypassReason = null
        return target
    }

    private fun analysisHop(): Int = max(FFT_SIZE, sampleRate / FRAMES_PER_SECOND)

    private fun analyzeFrame() {
        var energy = 0.0
        for (i in 0 until FFT_SIZE) {
            val sample = ring[ringPos + i and FFT_SIZE - 1]
            energy += (sample * sample).toDouble()
            re[i] = sample * window[i]
            im[i] = 0f
        }
        if (sqrt(energy / FFT_SIZE) < SILENCE_RMS_FLOOR) return
        fft()
        val weight = if (analyzedFrames < WARMUP_FRAMES) 1f / (analyzedFrames + 1) else AVERAGE_WEIGHT
        for (k in averagePower.indices) {
            val power = re[k] * re[k] + im[k] * im[k]
            averagePower[k] += (power - averagePower[k]) * weight
        }
        analyzedFrames++
        if (analyzedFrames >= WARMUP_FRAMES) detectCutoff()
    }

    private fun detectCutoff() {
        for (k in averagePower.indices) levelDb[k] = (10.0 * log10(averagePower[k].toDouble() + 1e-20)).toFloat()
        val binHz = sampleRate.toFloat() / FFT_SIZE
        val nyquist = sampleRate / 2f
        val searchLow = (MIN_CUTOFF_HZ / binHz).toInt()
        val searchHigh = (min(MAX_CUTOFF_HZ, nyquist - MIN_HEADROOM_HZ) / binHz).toInt()
        val nearBins = (NEAR_BAND_HZ / binHz).toInt().coerceAtLeast(1)
        val gapBins = (GAP_HZ / binHz).toInt().coerceAtLeast(1)
        val referenceLevel = meanDb((REFERENCE_LOW_HZ / binHz).toInt(), (REFERENCE_HIGH_HZ / binHz).toInt())
        var bestBin = -1
        var bestDrop = 0f
        var bestBelow = 0f
        var k = searchLow
        while (k <= searchHigh) {
            val below = meanDb(k - gapBins - nearBins, k - gapBins)
            val above = meanDb(k + gapBins, min(k + gapBins + nearBins, averagePower.size - 1))
            val drop = below - above
            if (drop > bestDrop) {
                bestDrop = drop
                bestBin = k
                bestBelow = below
            }
            k += 2
        }
        val emptyAbove = bestBin > 0 &&
            meanDb(bestBin + gapBins, averagePower.size - 1) <= referenceLevel - EMPTY_BAND_DB
        val contentBelow = bestBelow >= referenceLevel - MAX_BELOW_REFERENCE_DB
        if (bestBin < 0 || bestDrop < MIN_DROP_DB || !emptyAbove || !contentBelow) {
            detectedCutoffHz = 0f
            detectedDropDb = 0f
            patchGain = 0f
            return
        }
        val cutoffHz = bestBin * binHz
        val width = min(cutoffHz * MAX_PATCH_RATIO, nyquist - cutoffHz)
        val sourceLevel = meanDb(((cutoffHz - width) / binHz).toInt(), bestBin - gapBins)
        val lowerLevel = meanDb(((cutoffHz - 2f * width) / binHz).toInt(), ((cutoffHz - width) / binHz).toInt())
        val slopeDb = (sourceLevel - lowerLevel).coerceIn(-MAX_SLOPE_DB, 0f) - PATCH_MARGIN_DB
        detectedCutoffHz = cutoffHz
        detectedDropDb = bestDrop
        patchGain = 10f.pow(slopeDb / 20f).coerceIn(0f, 1f)
    }

    private fun meanDb(from: Int, to: Int): Float {
        val start = from.coerceIn(0, levelDb.size - 1)
        val end = to.coerceIn(start, levelDb.size - 1)
        var sum = 0f
        for (i in start..end) sum += levelDb[i]
        return sum / (end - start + 1)
    }

    private fun fft() {
        var j = 0
        for (i in 1 until FFT_SIZE) {
            var bit = FFT_SIZE shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tr = re[i]
                re[i] = re[j]
                re[j] = tr
                val ti = im[i]
                im[i] = im[j]
                im[j] = ti
            }
        }
        var length = 2
        while (length <= FFT_SIZE) {
            val half = length shr 1
            val step = FFT_SIZE / length
            var start = 0
            while (start < FFT_SIZE) {
                for (m in 0 until half) {
                    val wr = cosTable[m * step]
                    val wi = -sinTable[m * step]
                    val a = start + m
                    val b = a + half
                    val xr = re[b] * wr - im[b] * wi
                    val xi = re[b] * wi + im[b] * wr
                    re[b] = re[a] - xr
                    im[b] = im[a] - xi
                    re[a] += xr
                    im[a] += xi
                }
                start += length
            }
            length = length shl 1
        }
    }

    private fun bypass(
        target: MutableEnhancedAudioMetrics,
        startTimeUs: Long,
        maxPeak: Float,
        coherence: Float
    ): MutableEnhancedAudioMetrics {
        target.spectralCutoffHz = if (detectedCutoffHz > 0f) detectedCutoffHz else sampleRate / 2f
        target.hfEnergyRatio = 0f
        target.spectralHoleCount = 0
        target.tonality = 0f
        target.transientDensity = 0f
        target.stereoCoherence = coherence
        target.peakAmplitude = maxPeak
        target.deficitConfidence = smoothedConfidence
        target.adaptiveResidualGain = 0f
        target.processingTimeUs = System.nanoTime() / 1_000L - startTimeUs
        target.bypassed = true
        target.bypassReason = EnhancedAudioBypassReason.INSUFFICIENT_CONFIDENCE
        return target
    }

    companion object {
        const val FFT_SIZE = 2048
        const val MIN_CUTOFF_HZ = 11_000f
        const val MAX_CUTOFF_HZ = 19_500f
        const val MIN_DROP_DB = 30f
        private const val DROP_RANGE_DB = 20f
        private const val EMPTY_BAND_DB = 50f
        private const val MAX_BELOW_REFERENCE_DB = 45f
        private const val MIN_HEADROOM_HZ = 1_500f
        private const val NEAR_BAND_HZ = 700f
        private const val GAP_HZ = 170f
        private const val REFERENCE_LOW_HZ = 1_000f
        private const val REFERENCE_HIGH_HZ = 6_000f
        private const val MAX_PATCH_RATIO = 0.35f
        private const val MAX_SLOPE_DB = 18f
        private const val PATCH_MARGIN_DB = 3f
        private const val FRAMES_PER_SECOND = 10
        private const val WARMUP_FRAMES = 8
        private const val AVERAGE_WEIGHT = 0.08f
        private const val CONFIDENCE_SMOOTHING = 0.9f
        private const val SILENCE_RMS_FLOOR = 0.001f
    }
}
