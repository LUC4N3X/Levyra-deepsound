package com.luc4n3x.levyra.ui.artwork

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

data class LivingArtworkColors(
    val tones: List<Color>,
    val base: Color
)

private const val TONE_COUNT = 5

internal fun livingArtworkColors(primary: Color, secondary: Color): LivingArtworkColors {
    val start = primary.normalizedForLivingArtwork()
    val end = secondary.normalizedForLivingArtwork()
    val mid = livingArtworkMix(start, end, 0.5f)
    val tones = listOf(
        start,
        end,
        livingArtworkMix(mid, Color.White, 0.22f),
        livingArtworkMix(start, Color.Black, 0.34f),
        livingArtworkMix(end, Color.White, 0.12f)
    )
    val base = livingArtworkMix(mid, Color.Black, 0.72f)
    return LivingArtworkColors(tones = tones, base = base)
}

internal fun livingArtworkMix(first: Color, second: Color, amount: Float): Color {
    val ratio = amount.coerceIn(0f, 1f)
    return Color(
        red = first.red + (second.red - first.red) * ratio,
        green = first.green + (second.green - first.green) * ratio,
        blue = first.blue + (second.blue - first.blue) * ratio,
        alpha = 1f
    )
}

private fun Color.normalizedForLivingArtwork(): Color {
    val opaque = copy(alpha = 1f)
    val luminance = opaque.luminance()
    return when {
        luminance < 0.06f -> livingArtworkMix(opaque, Color.White, 0.26f)
        luminance > 0.86f -> livingArtworkMix(opaque, Color.Black, 0.22f)
        else -> opaque
    }
}

internal fun livingArtworkToneCount(): Int = TONE_COUNT
