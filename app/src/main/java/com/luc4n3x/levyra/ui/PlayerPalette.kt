package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.ui.theme.Nocturne78Palette

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.luc4n3x.levyra.domain.LevyraInterfaceSettings
import com.luc4n3x.levyra.domain.PlayerColorMode
import com.luc4n3x.levyra.ui.theme.LevyraActivePalette
import com.luc4n3x.levyra.ui.theme.LevyraPalette
import kotlin.math.abs

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

internal fun playerFixedAccents(mode: PlayerColorMode, theme: LevyraPalette, customAccent: Int): PlayerAccentPair? =
    if (theme.id == Nocturne78Palette.id) PlayerAccentPair(theme.cyan, theme.cyan) else when (mode) {
        PlayerColorMode.Dynamic -> null
        PlayerColorMode.Theme -> playerThemeAccents(theme.blue, theme.pink)
        PlayerColorMode.Custom -> playerAccentsFromSeed(Color(customAccent))
    }

@Composable
internal fun rememberPlayerFixedAccents(settings: LevyraInterfaceSettings): PlayerAccentPair? {
    val theme = LevyraActivePalette
    return remember(settings.playerColorMode, settings.playerColorAccent, theme) {
        playerFixedAccents(settings.playerColorMode, theme, settings.playerColorAccent)
    }
}

internal fun playerThemeAccents(primary: Color, secondary: Color): PlayerAccentPair {
    val first = primary.copy(alpha = 1f)
    val second = secondary.copy(alpha = 1f)
    val distance = abs(first.red - second.red) +
        abs(first.green - second.green) +
        abs(first.blue - second.blue)
    return if (distance < 0.12f) playerAccentsFromSeed(first) else harmonizePlayerAccents(first, second)
}

internal fun playerAccentsFromSeed(seed: Color): PlayerAccentPair {
    val primary = seed.copy(alpha = 1f)
    val (hue, saturation, value) = primary.hsvComponents()
    val secondary = Color.hsv(
        hue = (hue + 32f).mod(360f),
        saturation = (saturation * 0.82f).coerceIn(0f, 1f),
        value = (value * 0.86f).coerceIn(0.18f, 1f)
    )
    return PlayerAccentPair(primary, secondary)
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
