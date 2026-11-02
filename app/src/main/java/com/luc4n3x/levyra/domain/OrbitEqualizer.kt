package com.luc4n3x.levyra.domain

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

data class OrbitPosition(val tilt: Float, val contour: Float) {
    val radius: Float get() = hypot(tilt, contour)

    fun clamped(): OrbitPosition {
        val safeTilt = if (tilt.isFinite()) tilt else 0f
        val safeContour = if (contour.isFinite()) contour else 0f
        val length = hypot(safeTilt, safeContour)
        return if (length <= 1f) OrbitPosition(safeTilt, safeContour) else OrbitPosition(safeTilt / length, safeContour / length)
    }

    fun quantized(): OrbitPosition = OrbitPosition(
        (tilt * OrbitEqualizer.POSITION_STEPS).roundToInt() / OrbitEqualizer.POSITION_STEPS,
        (contour * OrbitEqualizer.POSITION_STEPS).roundToInt() / OrbitEqualizer.POSITION_STEPS
    )

    companion object {
        val Center = OrbitPosition(0f, 0f)
    }
}

data class OrbitShape(val profile: ParametricEqProfile, val peakBoostDb: Float, val loudnessOffsetDb: Float)

object OrbitEqualizer {
    const val PROFILE_ID = "parametric_orbit"
    const val DEPTH_DB = 7f
    const val SAFETY_MARGIN_DB = 0.3f
    const val POSITION_STEPS = 100f

    private val frequencies = floatArrayOf(105f, 260f, 1_150f, 3_300f, 9_500f)
    private val qValues = floatArrayOf(0.71f, 0.9f, 0.75f, 1.0f, 0.71f)
    private val types = arrayOf(
        ParametricFilterType.LOW_SHELF,
        ParametricFilterType.PEAK,
        ParametricFilterType.PEAK,
        ParametricFilterType.PEAK,
        ParametricFilterType.HIGH_SHELF
    )
    private val tiltWeights = floatArrayOf(-1f, -0.45f, 0f, 0.55f, 1f)
    private val contourWeights = floatArrayOf(-0.45f, -0.1f, 0.85f, 0.55f, -0.4f)
    private const val ANALYSIS_SAMPLE_RATE = 48_000
    private val analysisFrequencies = FloatArray(160) { index ->
        (20.0 * 10.0.pow(index / 159.0 * log10(20_000.0 / 20.0))).toFloat()
    }
    private val loudnessWindow = analysisFrequencies.indices.filter { analysisFrequencies[it] in 60f..12_000f }

    fun bandGains(position: OrbitPosition): FloatArray {
        val clamped = position.clamped()
        return FloatArray(frequencies.size) { index ->
            DEPTH_DB * (clamped.tilt * tiltWeights[index] + clamped.contour * contourWeights[index])
        }
    }

    fun shape(position: OrbitPosition, name: String): OrbitShape {
        val gains = bandGains(position)
        val bands = List(frequencies.size) { index ->
            ParametricEqBand(frequencies[index], gains[index], qValues[index], types[index])
        }
        val unity = ParametricEqProfile(PROFILE_ID, name, 0f, bands)
        val response = FloatArray(analysisFrequencies.size)
        ParametricBiquad.responseDb(unity, analysisFrequencies, ANALYSIS_SAMPLE_RATE, response)
        val peak = response.maxOrNull()?.coerceAtLeast(0f) ?: 0f
        val preamp = if (peak > 0f) {
            (-(peak + SAFETY_MARGIN_DB)).coerceAtLeast(ParametricEqualizer.MIN_PREAMP_DB)
        } else {
            0f
        }
        val meanDb = loudnessWindow.sumOf { response[it].toDouble() }.toFloat() / loudnessWindow.size.coerceAtLeast(1)
        return OrbitShape(
            profile = unity.copy(preampDb = preamp),
            peakBoostDb = peak,
            loudnessOffsetDb = (meanDb + preamp).coerceAtMost(0f)
        )
    }

    fun comparisonProfile(shape: OrbitShape): ParametricEqProfile =
        shape.profile.copy(
            preampDb = shape.loudnessOffsetDb.coerceIn(ParametricEqualizer.MIN_PREAMP_DB, 0f),
            bands = shape.profile.bands.map { it.copy(gainDb = 0f) }
        )

    fun positionOf(profile: ParametricEqProfile?): OrbitPosition? {
        val bands = profile?.bands ?: return null
        if (bands.size != frequencies.size) return null
        bands.forEachIndexed { index, band ->
            if (!band.enabled || band.filterType != types[index] ||
                abs(band.frequencyHz - frequencies[index]) > 0.5f || abs(band.q - qValues[index]) > 0.005f
            ) return null
        }
        var tt = 0f
        var tc = 0f
        var cc = 0f
        var tg = 0f
        var cg = 0f
        bands.forEachIndexed { index, band ->
            val gain = band.gainDb / DEPTH_DB
            tt += tiltWeights[index] * tiltWeights[index]
            tc += tiltWeights[index] * contourWeights[index]
            cc += contourWeights[index] * contourWeights[index]
            tg += tiltWeights[index] * gain
            cg += contourWeights[index] * gain
        }
        val determinant = tt * cc - tc * tc
        if (abs(determinant) < 1e-6f) return null
        val position = OrbitPosition((tg * cc - cg * tc) / determinant, (cg * tt - tg * tc) / determinant)
        val expected = bandGains(position)
        val fits = bands.indices.all { abs(expected[it] - bands[it].gainDb) < 0.05f }
        return if (fits) position.clamped().quantized() else null
    }
}
