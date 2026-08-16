package com.luc4n3x.levyra.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
internal data class PlayerAmbience(
    val tint: Color,
    val elevated: Color,
    val control: Color,
    val base: Color
)

private const val AmbienceTintLevel = 0.30f
private const val AmbienceElevatedLevel = 0.085f
private const val AmbienceControlLevel = 0.090f
private const val AmbienceBaseLevel = 0.042f

internal fun playerAmbienceOf(primary: Color, secondary: Color): PlayerAmbience {
    val blended = primary.playerAmbienceMix(secondary, 0.5f)
    return PlayerAmbience(
        tint = blended.playerAmbienceDesaturate(0.28f).playerAmbienceTone(AmbienceTintLevel),
        elevated = blended.playerAmbienceDesaturate(0.42f).playerAmbienceTone(AmbienceElevatedLevel),
        control = blended.playerAmbienceDesaturate(0.20f).playerAmbienceTone(AmbienceControlLevel),
        base = blended.playerAmbienceDesaturate(0.34f).playerAmbienceTone(AmbienceBaseLevel)
    )
}

internal fun Color.playerAmbienceMix(other: Color, amount: Float): Color {
    val fraction = amount.coerceIn(0f, 1f)
    return Color(
        red = red + (other.red - red) * fraction,
        green = green + (other.green - green) * fraction,
        blue = blue + (other.blue - blue) * fraction,
        alpha = 1f
    )
}

private fun Color.playerAmbienceDesaturate(amount: Float): Color {
    val fraction = amount.coerceIn(0f, 1f)
    val grey = playerAmbienceLevel()
    return Color(
        red = red + (grey - red) * fraction,
        green = green + (grey - green) * fraction,
        blue = blue + (grey - blue) * fraction,
        alpha = 1f
    )
}

private fun Color.playerAmbienceTone(target: Float): Color {
    val level = playerAmbienceLevel()
    if (level <= 0.004f) return Color(target, target, target, 1f)
    val factor = target / level
    return Color(
        red = (red * factor).coerceIn(0f, 1f),
        green = (green * factor).coerceIn(0f, 1f),
        blue = (blue * factor).coerceIn(0f, 1f),
        alpha = 1f
    )
}

private fun Color.playerAmbienceLevel(): Float = 0.299f * red + 0.587f * green + 0.114f * blue
