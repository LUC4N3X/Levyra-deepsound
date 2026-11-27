package com.luc4n3x.levyra.player.visualizer

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

internal class SpectrumAnalyzer(val fftSize: Int = DEFAULT_FFT_SIZE) {
    init {
        require(fftSize >= 64 && fftSize and (fftSize - 1) == 0)
    }

    val binCount = fftSize / 2 + 1
    private val window = FloatArray(fftSize) { index -> (0.5 - 0.5 * cos(2.0 * PI * index / fftSize)).toFloat() }
    private val windowSum = window.sum().toDouble()
    private val windowSquareSum = window.sumOf { (it * it).toDouble() }
    private val amplitudeScale = (2.0 / windowSum).toFloat()
    private val powerScale = (2.0 / (fftSize * windowSquareSum)).toFloat()
    private val real = FloatArray(fftSize)
    private val imaginary = FloatArray(fftSize)
    private val bitReversed = IntArray(fftSize).also { table ->
        val bits = Integer.numberOfTrailingZeros(fftSize)
        for (index in 0 until fftSize) table[index] = Integer.reverse(index) ushr (32 - bits)
    }
    private val cosTable = FloatArray(fftSize / 2) { cos(2.0 * PI * it / fftSize).toFloat() }
    private val sinTable = FloatArray(fftSize / 2) { sin(2.0 * PI * it / fftSize).toFloat() }

    val amplitude = FloatArray(binCount)
    val power = FloatArray(binCount)
    var sampleRate = 0.0
        private set

    val binWidthHz: Double get() = sampleRate / fftSize

    fun analyze(samples: FloatArray, rate: Double) {
        require(samples.size >= fftSize)
        sampleRate = rate
        val base = samples.size - fftSize
        for (index in 0 until fftSize) {
            val target = bitReversed[index]
            real[target] = samples[base + index] * window[index]
            imaginary[target] = 0f
        }
        var size = 2
        while (size <= fftSize) {
            val half = size / 2
            val tableStep = fftSize / size
            var block = 0
            while (block < fftSize) {
                var k = 0
                while (k < half) {
                    val twiddleCos = cosTable[k * tableStep]
                    val twiddleSin = -sinTable[k * tableStep]
                    val even = block + k
                    val odd = even + half
                    val oddReal = real[odd] * twiddleCos - imaginary[odd] * twiddleSin
                    val oddImaginary = real[odd] * twiddleSin + imaginary[odd] * twiddleCos
                    real[odd] = real[even] - oddReal
                    imaginary[odd] = imaginary[even] - oddImaginary
                    real[even] += oddReal
                    imaginary[even] += oddImaginary
                    k++
                }
                block += size
            }
            size = size shl 1
        }
        for (bin in 0 until binCount) {
            val squared = real[bin] * real[bin] + imaginary[bin] * imaginary[bin]
            val edgeFactor = if (bin == 0 || bin == fftSize / 2) 0.5f else 1f
            amplitude[bin] = sqrt(squared) * amplitudeScale * edgeFactor
            power[bin] = squared * powerScale * edgeFactor
        }
    }

    fun peakBandsDb(edgesHz: FloatArray, out: FloatArray) {
        val width = binWidthHz
        val nyquistBin = binCount - 1
        for (band in out.indices) {
            val low = edgesHz[band]
            val high = edgesHz[band + 1]
            val firstBin = max(1, kotlin.math.ceil(low / width).toInt())
            val lastBin = min(nyquistBin, floor((high - 1e-9) / width).toInt())
            var peak = 0f
            if (firstBin <= lastBin) {
                for (bin in firstBin..lastBin) if (amplitude[bin] > peak) peak = amplitude[bin]
            } else {
                peak = interpolatedAmplitude(sqrt(low.toDouble() * high) / width)
            }
            out[band] = amplitudeToDb(peak)
        }
    }

    fun bandPowerDb(edgesHz: FloatArray, out: FloatArray) {
        val width = binWidthHz
        val nyquistBin = binCount - 1
        for (band in out.indices) {
            val low = edgesHz[band] / width
            val high = edgesHz[band + 1] / width
            var total = 0.0
            var bin = max(0, floor(low + 0.5).toInt())
            val last = min(nyquistBin, floor(high + 0.5).toInt())
            while (bin <= last) {
                val overlap = min(high, bin + 0.5) - max(low, bin - 0.5)
                if (overlap > 0.0) total += power[bin] * min(1.0, overlap)
                bin++
            }
            out[band] = powerToDb(total)
        }
    }

    private fun interpolatedAmplitude(position: Double): Float {
        val clamped = position.coerceIn(0.0, (binCount - 1).toDouble())
        val lower = floor(clamped).toInt()
        val upper = min(binCount - 1, lower + 1)
        val fraction = (clamped - lower).toFloat()
        return amplitude[lower] + (amplitude[upper] - amplitude[lower]) * fraction
    }

    companion object {
        const val DEFAULT_FFT_SIZE = 4096
        const val FLOOR_DB = -120f

        fun amplitudeToDb(amplitude: Float): Float =
            if (amplitude <= 1e-6f) FLOOR_DB else max(FLOOR_DB, (20.0 * log10(amplitude.toDouble())).toFloat())

        fun powerToDb(meanSquare: Double): Float =
            if (meanSquare <= 1e-12) FLOOR_DB else max(FLOOR_DB, (10.0 * log10(meanSquare / 0.5)).toFloat())

        fun logEdges(count: Int, minHz: Float, maxHz: Float): FloatArray {
            val ratio = (maxHz / minHz).toDouble()
            return FloatArray(count + 1) { (minHz * ratio.pow(it.toDouble() / count)).toFloat() }
        }

        val thirdOctaveCentersHz: FloatArray = FloatArray(31) { (1000.0 * 2.0.pow((it - 17) / 3.0)).toFloat() }

        val thirdOctaveEdgesHz: FloatArray = FloatArray(32) { index ->
            (1000.0 * 2.0.pow((index - 17) / 3.0 - 1.0 / 6.0)).toFloat()
        }
    }
}
