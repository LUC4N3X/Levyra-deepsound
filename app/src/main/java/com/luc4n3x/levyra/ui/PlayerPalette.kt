package com.luc4n3x.levyra.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

internal data class PlayerAccentPair(
    val primary: Color,
    val secondary: Color
)

/** Keeps clashing red/green artwork accents within one visually coherent color family. */
internal fun harmonizePlayerAccents(primary: Color, secondary: Color): PlayerAccentPair {
    val normalizedPrimary = primary.copy(alpha = 1f)
    val normalizedSecondary = secondary.copy(alpha = 1f)
    val primaryIsRed = normalizedPrimary.isPlayerRedDominant()
    val secondaryIsRed = normalizedSecondary.isPlayerRedDominant()
    val primaryIsGreen = normalizedPrimary.isPlayerGreenDominant()
    val secondaryIsGreen = normalizedSecondary.isPlayerGreenDominant()
    val redGreenConflict =
        (primaryIsRed && secondaryIsGreen) || (primaryIsGreen && secondaryIsRed)

    if (!redGreenConflict) {
        return PlayerAccentPair(normalizedPrimary, normalizedSecondary)
    }

    val redAnchor = if (primaryIsRed) normalizedPrimary else normalizedSecondary
    val redAnchorLuminance = redAnchor.luminance()
    val companionTarget = if (redAnchorLuminance > 0.35f) Color.Black else Color.White
    val companionAmount = if (redAnchorLuminance > 0.35f) 0.28f else 0.18f
    return PlayerAccentPair(
        primary = redAnchor,
        secondary = redAnchor.playerPaletteMix(companionTarget, companionAmount)
    )
}

private fun Color.isPlayerRedDominant(): Boolean {
    return red >= 0.38f && red > green * 1.18f && red > blue * 1.08f
}

private fun Color.isPlayerGreenDominant(): Boolean {
    return green >= 0.34f && green > red * 1.14f && green > blue * 1.08f
}

private fun Color.playerPaletteMix(other: Color, amount: Float): Color {
    val fraction = amount.coerceIn(0f, 1f)
    return Color(
        red = red + (other.red - red) * fraction,
        green = green + (other.green - green) * fraction,
        blue = blue + (other.blue - blue) * fraction,
        alpha = 1f
    )
}
