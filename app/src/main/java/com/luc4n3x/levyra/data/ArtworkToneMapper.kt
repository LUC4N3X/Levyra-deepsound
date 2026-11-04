package com.luc4n3x.levyra.data

import com.materialkolor.hct.Hct

internal object ArtworkToneMapper {
    const val ChromaCeiling = 62.0
    const val NeutralChromaCeiling = 7.0
    const val MonochromeChromaticShare = 0.06f

    fun withToneRange(
        argb: Int,
        toneFloor: Double,
        toneCeiling: Double,
        chromaCeiling: Double = ChromaCeiling
    ): Int {
        require(toneFloor in 0.0..100.0)
        require(toneCeiling in 0.0..100.0)
        require(toneFloor <= toneCeiling)
        val source = Hct.fromInt(argb)
        val tone = source.tone.coerceIn(toneFloor, toneCeiling)
        return Hct.from(source.hue, source.chroma.coerceAtMost(chromaCeiling), tone).toInt()
    }

    fun isChromatic(saturation: Float, value: Float): Boolean = saturation >= 0.13f && value >= 0.11f

    fun isMonochrome(chromaticWeight: Float, totalWeight: Float): Boolean =
        totalWeight > 0f && chromaticWeight / totalWeight < MonochromeChromaticShare

    fun neutral(averageArgb: Int): Pair<Int, Int> {
        val source = Hct.fromInt(averageArgb)
        val chroma = source.chroma.coerceAtMost(NeutralChromaCeiling)
        val tone = source.tone.coerceIn(46.0, 64.0)
        val start = Hct.from(source.hue, chroma, tone).toInt()
        val end = Hct.from(source.hue, chroma * 0.7, (tone - 14.0).coerceAtLeast(34.0)).toInt()
        return start to end
    }

    fun companion(argb: Int): Int {
        val source = Hct.fromInt(argb)
        val shift = when {
            source.hue < 55.0 -> 42.0
            source.hue > 305.0 -> -42.0
            source.hue < 180.0 -> 52.0
            else -> -52.0
        }
        val hue = normalizeHue(source.hue + shift)
        val chroma = (source.chroma * 0.88).coerceAtMost(56.0)
        val tone = (source.tone * 0.82).coerceIn(40.0, 60.0)
        return Hct.from(hue, chroma, tone).toInt()
    }

    private fun normalizeHue(hue: Double): Double {
        val normalized = hue % 360.0
        return if (normalized < 0.0) normalized + 360.0 else normalized
    }
}
