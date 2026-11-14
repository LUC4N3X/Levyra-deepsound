package com.luc4n3x.levyra.domain

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

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

data class OrbitLayout(
    val poleAngles: List<Float> = OrbitEqualizer.DEFAULT_POLE_ANGLES,
    val puck: OrbitPosition = OrbitPosition.Center
) {
    fun encoded(): String = poleAngles.joinToString(",") + ";" + puck.tilt + "," + puck.contour

    companion object {
        fun decode(text: String?): OrbitLayout? {
            val parts = text?.split(';')?.takeIf { it.size == 2 } ?: return null
            val angles = parts[0].split(',').map { it.toFloatOrNull()?.takeIf(Float::isFinite) ?: return null }
            val puck = parts[1].split(',').map { it.toFloatOrNull()?.takeIf(Float::isFinite) ?: return null }
            if (angles.size != OrbitEqualizer.DEFAULT_POLE_ANGLES.size || puck.size != 2) return null
            return OrbitLayout(angles.map(OrbitEqualizer::normalizedAngle), OrbitPosition(puck[0], puck[1]).clamped())
        }
    }
}

data class OrbitShape(val profile: ParametricEqProfile, val peakBoostDb: Float, val loudnessOffsetDb: Float)

object OrbitEqualizer {
    const val PROFILE_ID = "parametric_orbit"
    const val DEPTH_DB = 7f
    const val SAFETY_MARGIN_DB = 0.3f
    const val POSITION_STEPS = 100f
    val DEFAULT_POLE_ANGLES = listOf(180f, 0f, 90f, 270f)

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
    private val poleVectors = listOf(
        FloatArray(tiltWeights.size) { -tiltWeights[it] },
        tiltWeights,
        contourWeights,
        FloatArray(contourWeights.size) { -contourWeights[it] }
    )
    private const val ANALYSIS_SAMPLE_RATE = 48_000
    private val analysisFrequencies = FloatArray(160) { index ->
        (20.0 * 10.0.pow(index / 159.0 * log10(20_000.0 / 20.0))).toFloat()
    }
    private val loudnessWindow = analysisFrequencies.indices.filter { analysisFrequencies[it] in 60f..12_000f }

    fun normalizedAngle(degrees: Float): Float = ((degrees % 360f) + 360f) % 360f

    fun poleDirection(degrees: Float): OrbitPosition {
        val radians = Math.toRadians(degrees.toDouble())
        return OrbitPosition(cos(radians).toFloat(), sin(radians).toFloat())
    }

    fun poleWeights(position: OrbitPosition, poleAngles: List<Float> = DEFAULT_POLE_ANGLES): FloatArray {
        val clamped = position.clamped()
        return FloatArray(poleVectors.size) { pole ->
            val direction = poleDirection(poleAngles[pole])
            (clamped.tilt * direction.tilt + clamped.contour * direction.contour).coerceAtLeast(0f)
        }
    }

    fun bandGains(position: OrbitPosition, poleAngles: List<Float> = DEFAULT_POLE_ANGLES): FloatArray {
        val weights = poleWeights(position, poleAngles)
        return FloatArray(frequencies.size) { index ->
            DEPTH_DB * poleVectors.indices.sumOf { pole -> (weights[pole] * poleVectors[pole][index]).toDouble() }.toFloat()
        }
    }

    fun shape(position: OrbitPosition, name: String, poleAngles: List<Float> = DEFAULT_POLE_ANGLES): OrbitShape {
        val gains = bandGains(position, poleAngles)
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

    fun restore(profile: ParametricEqProfile?, layout: OrbitLayout?): OrbitLayout? {
        if (layout != null && profile != null && isOrbitShape(profile.bands)) {
            val expected = bandGains(layout.puck, layout.poleAngles)
            if (profile.bands.indices.all { abs(expected[it] - profile.bands[it].gainDb) < 0.05f }) return layout
        }
        return positionOf(profile)?.let { OrbitLayout(puck = it) }
    }

    private fun isOrbitShape(bands: List<ParametricEqBand>): Boolean =
        bands.size == frequencies.size && bands.indices.all { index ->
            val band = bands[index]
            band.enabled && band.filterType == types[index] &&
                abs(band.frequencyHz - frequencies[index]) <= 0.5f && abs(band.q - qValues[index]) <= 0.005f
        }

    fun positionOf(profile: ParametricEqProfile?): OrbitPosition? {
        val bands = profile?.bands ?: return null
        if (!isOrbitShape(bands)) return null
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
