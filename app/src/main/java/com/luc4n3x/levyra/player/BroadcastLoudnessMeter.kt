package com.luc4n3x.levyra.player

import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.tan

internal class BroadcastLoudnessMeter {
    private var channels = 0
    private var framesPerHop = 0
    private var shelf = DoubleArray(COEFFICIENT_COUNT)
    private var highPass = DoubleArray(COEFFICIENT_COUNT)
    private var filterState = DoubleArray(0)
    private var channelWeights = DoubleArray(0)
    private val hopEnergies = DoubleArray(HOPS_PER_BLOCK)
    private var hopIndex = 0
    private var hopsFilled = 0
    private var hopEnergy = 0.0
    private var hopFrames = 0
    private val binEnergy = DoubleArray(HISTOGRAM_BINS)
    private val binBlocks = IntArray(HISTOGRAM_BINS)
    private var integratedLufs = Double.NaN
    private var integratedStale = false

    var measuredBlocks: Int = 0
        private set

    fun configure(sampleRate: Int, channelCount: Int) {
        require(sampleRate > 0 && channelCount > 0)
        channels = channelCount
        framesPerHop = (sampleRate / HOPS_PER_SECOND).coerceAtLeast(1)
        val stages = kWeightingStages(sampleRate)
        shelf = stages[0]
        highPass = stages[1]
        filterState = DoubleArray(channelCount * STATE_PER_CHANNEL)
        channelWeights = DoubleArray(channelCount) { channel -> channelWeight(channelCount, channel) }
        reset()
    }

    fun reset() {
        filterState.fill(0.0)
        hopEnergies.fill(0.0)
        hopIndex = 0
        hopsFilled = 0
        hopEnergy = 0.0
        hopFrames = 0
        binEnergy.fill(0.0)
        binBlocks.fill(0)
        measuredBlocks = 0
        integratedLufs = Double.NaN
        integratedStale = false
    }

    fun clearFilterHistory() {
        filterState.fill(0.0)
    }

    fun push(sample: Double, channel: Int) {
        val offset = channel * STATE_PER_CHANNEL
        val shelved = shelf[0] * sample + filterState[offset]
        filterState[offset] = shelf[1] * sample - shelf[3] * shelved + filterState[offset + 1]
        filterState[offset + 1] = shelf[2] * sample - shelf[4] * shelved
        val weighted = highPass[0] * shelved + filterState[offset + 2]
        filterState[offset + 2] = highPass[1] * shelved - highPass[3] * weighted + filterState[offset + 3]
        filterState[offset + 3] = highPass[2] * shelved - highPass[4] * weighted
        hopEnergy += channelWeights[channel] * weighted * weighted
        if (channel == channels - 1 && ++hopFrames == framesPerHop) completeHop()
    }

    fun integratedLoudnessLufs(): Double {
        if (!integratedStale) return integratedLufs
        integratedStale = false
        var totalEnergy = 0.0
        var totalBlocks = 0
        for (bin in 0 until HISTOGRAM_BINS) {
            totalEnergy += binEnergy[bin]
            totalBlocks += binBlocks[bin]
        }
        if (totalBlocks == 0) {
            integratedLufs = Double.NaN
            return integratedLufs
        }
        val relativeGateLufs = energyToLufs(totalEnergy / totalBlocks) + RELATIVE_GATE_LU
        var gatedEnergy = 0.0
        var gatedBlocks = 0
        for (bin in 0 until HISTOGRAM_BINS) {
            if (binCenterLufs(bin) < relativeGateLufs) continue
            gatedEnergy += binEnergy[bin]
            gatedBlocks += binBlocks[bin]
        }
        integratedLufs = if (gatedBlocks == 0) Double.NaN else energyToLufs(gatedEnergy / gatedBlocks)
        return integratedLufs
    }

    private fun completeHop() {
        hopEnergies[hopIndex] = hopEnergy / hopFrames
        hopIndex = (hopIndex + 1) % HOPS_PER_BLOCK
        if (hopsFilled < HOPS_PER_BLOCK) hopsFilled++
        hopEnergy = 0.0
        hopFrames = 0
        if (hopsFilled < HOPS_PER_BLOCK) return
        var blockEnergy = 0.0
        for (hop in 0 until HOPS_PER_BLOCK) blockEnergy += hopEnergies[hop]
        addBlock(blockEnergy / HOPS_PER_BLOCK)
    }

    private fun addBlock(energy: Double) {
        if (energy <= ABSOLUTE_GATE_ENERGY) return
        val bin = ((energyToLufs(energy) - HISTOGRAM_FLOOR_LUFS) * BINS_PER_LU)
            .toInt()
            .coerceIn(0, HISTOGRAM_BINS - 1)
        binEnergy[bin] += energy
        binBlocks[bin]++
        measuredBlocks++
        integratedStale = true
    }

    companion object {
        private const val COEFFICIENT_COUNT = 5
        private const val STATE_PER_CHANNEL = 4
        private const val HOPS_PER_SECOND = 10
        private const val HOPS_PER_BLOCK = 4
        private const val LOUDNESS_OFFSET_LU = -0.691
        private const val ABSOLUTE_GATE_LUFS = -70.0
        private const val RELATIVE_GATE_LU = -10.0
        private const val HISTOGRAM_FLOOR_LUFS = ABSOLUTE_GATE_LUFS
        private const val HISTOGRAM_CEILING_LUFS = 5.0
        private const val BINS_PER_LU = 10
        private val HISTOGRAM_BINS = ((HISTOGRAM_CEILING_LUFS - HISTOGRAM_FLOOR_LUFS) * BINS_PER_LU).toInt()
        private val ABSOLUTE_GATE_ENERGY = 10.0.pow((ABSOLUTE_GATE_LUFS - LOUDNESS_OFFSET_LU) / 10.0)
        private const val SURROUND_WEIGHT = 1.41
        private const val LFE_CHANNEL = 3
        private const val SHELF_FREQUENCY_HZ = 1681.974450955533
        private const val SHELF_GAIN_DB = 3.999843853973347
        private const val SHELF_Q = 0.7071752369554196
        private const val SHELF_BANDWIDTH_EXPONENT = 0.4996667741545416
        private const val HIGH_PASS_FREQUENCY_HZ = 38.13547087602444
        private const val HIGH_PASS_Q = 0.5003270373238773

        internal fun energyToLufs(energy: Double): Double = LOUDNESS_OFFSET_LU + 10.0 * log10(energy)

        internal fun kWeightingStages(sampleRate: Int): Array<DoubleArray> {
            val shelfK = tan(PI * SHELF_FREQUENCY_HZ / sampleRate)
            val shelfPeak = 10.0.pow(SHELF_GAIN_DB / 20.0)
            val shelfBand = shelfPeak.pow(SHELF_BANDWIDTH_EXPONENT)
            val shelfNorm = 1.0 + shelfK / SHELF_Q + shelfK * shelfK
            val shelf = doubleArrayOf(
                (shelfPeak + shelfBand * shelfK / SHELF_Q + shelfK * shelfK) / shelfNorm,
                2.0 * (shelfK * shelfK - shelfPeak) / shelfNorm,
                (shelfPeak - shelfBand * shelfK / SHELF_Q + shelfK * shelfK) / shelfNorm,
                2.0 * (shelfK * shelfK - 1.0) / shelfNorm,
                (1.0 - shelfK / SHELF_Q + shelfK * shelfK) / shelfNorm
            )
            val highPassK = tan(PI * HIGH_PASS_FREQUENCY_HZ / sampleRate)
            val highPassNorm = 1.0 + highPassK / HIGH_PASS_Q + highPassK * highPassK
            val highPass = doubleArrayOf(
                1.0,
                -2.0,
                1.0,
                2.0 * (highPassK * highPassK - 1.0) / highPassNorm,
                (1.0 - highPassK / HIGH_PASS_Q + highPassK * highPassK) / highPassNorm
            )
            return arrayOf(shelf, highPass)
        }

        internal fun channelWeight(channelCount: Int, channel: Int): Double = when {
            channelCount != 6 && channelCount != 8 -> 1.0
            channel == LFE_CHANNEL -> 0.0
            channel > LFE_CHANNEL -> SURROUND_WEIGHT
            else -> 1.0
        }

        private fun binCenterLufs(bin: Int): Double = HISTOGRAM_FLOOR_LUFS + (bin + 0.5) / BINS_PER_LU
    }
}
