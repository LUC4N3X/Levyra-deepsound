package com.luc4n3x.levyra.widget.studio

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import androidx.core.graphics.ColorUtils
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

data class StudioColorScheme(
    val dark: Boolean,
    val surface: Int,
    val surfaceContainer: Int,
    val surfaceContainerHigh: Int,
    val onSurface: Int,
    val onSurfaceVariant: Int,
    val primary: Int,
    val onPrimary: Int,
    val primaryContainer: Int,
    val onPrimaryContainer: Int,
    val secondaryContainer: Int,
    val onSecondaryContainer: Int,
    val tertiaryContainer: Int,
    val onTertiaryContainer: Int,
    val outline: Int,
    val scrim: Int
)

object WidgetStudioColors {
    const val LEVYRA_SEED = 0xFF6EE7FF.toInt()
    private const val NEUTRAL_CHROMA = 6.0

    fun scheme(
        context: Context,
        palette: StudioPalette,
        customColor: Int,
        artworkSeed: Int?
    ): StudioColorScheme {
        val systemDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        return when (palette) {
            StudioPalette.Artwork -> fromSeed(artworkSeed ?: LEVYRA_SEED, dark = true, chromaBoost = 1.0)
            StudioPalette.MaterialYou -> fromSeed(materialYouSeed(context), dark = systemDark, chromaBoost = 1.0)
            StudioPalette.Levyra -> levyra()
            StudioPalette.Light -> fromSeed(artworkSeed ?: LEVYRA_SEED, dark = false, chromaBoost = 0.55)
            StudioPalette.Dark -> fromSeed(artworkSeed ?: LEVYRA_SEED, dark = true, chromaBoost = 0.45)
            StudioPalette.Custom -> fromSeed(customColor, dark = systemDark, chromaBoost = 1.0)
        }
    }

    private fun materialYouSeed(context: Context): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getColor(android.R.color.system_accent1_500)
        } else {
            LEVYRA_SEED
        }

    private fun levyra(): StudioColorScheme {
        val base = fromSeed(LEVYRA_SEED, dark = true, chromaBoost = 1.0)
        return base.copy(
            surface = 0xFF0A0914.toInt(),
            surfaceContainer = 0xFF15132A.toInt(),
            surfaceContainerHigh = 0xFF1F1C3A.toInt(),
            onSurface = 0xFFF8F7FF.toInt(),
            onSurfaceVariant = 0xFFB9B2CC.toInt(),
            primary = LEVYRA_SEED,
            onPrimary = 0xFF00363F.toInt(),
            scrim = 0xFF0A0914.toInt()
        )
    }

    fun fromSeed(seed: Int, dark: Boolean, chromaBoost: Double): StudioColorScheme {
        val lab = DoubleArray(3)
        ColorUtils.colorToLAB(seed, lab)
        val chroma = hypot(lab[1], lab[2])
        val hue = atan2(lab[2], lab[1])
        val neutral = chroma < NEUTRAL_CHROMA
        fun tone(lightness: Double, chromaTarget: Double): Int {
            val c = if (neutral) chroma.coerceAtMost(4.0) else (chromaTarget * chromaBoost).coerceAtMost(chroma.coerceAtLeast(18.0) * 1.25)
            return ColorUtils.LABToColor(lightness, c * kotlin.math.cos(hue), c * kotlin.math.sin(hue))
        }
        return if (dark) {
            StudioColorScheme(
                dark = true,
                surface = tone(8.0, 10.0),
                surfaceContainer = tone(13.0, 12.0),
                surfaceContainerHigh = tone(19.0, 14.0),
                onSurface = tone(95.0, 4.0),
                onSurfaceVariant = tone(78.0, 10.0),
                primary = tone(82.0, 42.0),
                onPrimary = tone(18.0, 30.0),
                primaryContainer = tone(32.0, 38.0),
                onPrimaryContainer = tone(92.0, 20.0),
                secondaryContainer = tone(26.0, 14.0),
                onSecondaryContainer = tone(90.0, 10.0),
                tertiaryContainer = tertiary(seed, 30.0, 30.0, neutral),
                onTertiaryContainer = tertiary(seed, 92.0, 16.0, neutral),
                outline = tone(42.0, 8.0),
                scrim = tone(5.0, 12.0)
            )
        } else {
            StudioColorScheme(
                dark = false,
                surface = tone(97.0, 4.0),
                surfaceContainer = tone(93.0, 7.0),
                surfaceContainerHigh = tone(89.0, 9.0),
                onSurface = tone(10.0, 6.0),
                onSurfaceVariant = tone(32.0, 10.0),
                primary = tone(40.0, 48.0),
                onPrimary = tone(99.0, 2.0),
                primaryContainer = tone(88.0, 26.0),
                onPrimaryContainer = tone(14.0, 30.0),
                secondaryContainer = tone(88.0, 12.0),
                onSecondaryContainer = tone(16.0, 12.0),
                tertiaryContainer = tertiary(seed, 88.0, 24.0, neutral),
                onTertiaryContainer = tertiary(seed, 14.0, 26.0, neutral),
                outline = tone(72.0, 8.0),
                scrim = tone(97.0, 4.0)
            )
        }
    }

    private fun tertiary(seed: Int, lightness: Double, chroma: Double, neutral: Boolean): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(seed, hsl)
        hsl[0] = (hsl[0] + 60f) % 360f
        val rotated = ColorUtils.HSLToColor(hsl)
        val lab = DoubleArray(3)
        ColorUtils.colorToLAB(rotated, lab)
        val hue = atan2(lab[2], lab[1])
        val c = if (neutral) 4.0 else chroma
        return ColorUtils.LABToColor(lightness, c * kotlin.math.cos(hue), c * kotlin.math.sin(hue))
    }

    fun artworkSeed(bitmap: Bitmap): Int? {
        val sample = runCatching { Bitmap.createScaledBitmap(bitmap, 32, 32, true) }.getOrNull() ?: return null
        val histogram = HueHistogram()
        val lab = DoubleArray(3)
        for (y in 0 until sample.height) {
            for (x in 0 until sample.width) {
                val pixel = sample.getPixel(x, y)
                if (Color.alpha(pixel) >= 128) {
                    ColorUtils.colorToLAB(pixel, lab)
                    histogram.add(lab)
                }
            }
        }
        if (sample !== bitmap) sample.recycle()
        return histogram.dominant()
    }

    private class HueHistogram {
        private val buckets = DoubleArray(BUCKETS)
        private val sums = Array(BUCKETS) { DoubleArray(3) }
        private var neutralWeight = 0.0
        private val neutralSum = DoubleArray(3)

        fun add(lab: DoubleArray) {
            val lightnessWeight = 1.0 - abs(lab[0] - 55.0) / 70.0
            if (lightnessWeight <= 0.0) return
            val chroma = hypot(lab[1], lab[2])
            if (chroma < 10.0) {
                neutralWeight += lightnessWeight
                for (i in 0..2) neutralSum[i] += lab[i] * lightnessWeight
                return
            }
            val weight = chroma * lightnessWeight
            val angle = (Math.toDegrees(atan2(lab[2], lab[1])) + 360.0) % 360.0
            val bucket = (angle / (360.0 / BUCKETS)).toInt().coerceIn(0, BUCKETS - 1)
            buckets[bucket] += weight
            for (i in 0..2) sums[bucket][i] += lab[i] * weight
        }

        fun dominant(): Int? {
            val best = buckets.indices.maxByOrNull { buckets[it] } ?: return null
            val bestWeight = buckets[best]
            if (bestWeight >= 40.0) {
                return ColorUtils.LABToColor(sums[best][0] / bestWeight, sums[best][1] / bestWeight, sums[best][2] / bestWeight)
            }
            if (neutralWeight <= 0.0) return null
            return ColorUtils.LABToColor(neutralSum[0] / neutralWeight, neutralSum[1] / neutralWeight, neutralSum[2] / neutralWeight)
        }
    }

    fun withAlpha(color: Int, alpha: Float): Int =
        ColorUtils.setAlphaComponent(color, (alpha.coerceIn(0f, 1f) * 255f).toInt())

    private const val BUCKETS = 12
}
