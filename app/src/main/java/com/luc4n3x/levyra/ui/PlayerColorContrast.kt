package com.luc4n3x.levyra.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

internal val PlayerDarkSurface = Color(0xFF0B0B10)
internal const val PlayerMinimumContrast = 4.5f
internal const val PlayerStrongContrast = 7f

private const val PlayerStrongSurfaceDarkening = 0.42f

internal data class PlayerContrastAdjustment(
    val color: Color,
    val amount: Float,
    val valid: Boolean
)

internal data class PlayerContrastGradient(
    val start: Color,
    val end: Color,
    val content: Color
)

internal fun Color.playerMix(other: Color, amount: Float): Color {
    val fraction = amount.coerceIn(0f, 1f)
    return Color(
        red = red + (other.red - red) * fraction,
        green = green + (other.green - green) * fraction,
        blue = blue + (other.blue - blue) * fraction,
        alpha = alpha + (other.alpha - alpha) * fraction
    )
}

internal fun Color.playerCompositeOver(background: Color): Color {
    val foregroundAlpha = alpha.coerceIn(0f, 1f)
    val backgroundAlpha = background.alpha.coerceIn(0f, 1f)
    val outputAlpha = foregroundAlpha + backgroundAlpha * (1f - foregroundAlpha)
    if (outputAlpha <= 0f) return Color.Transparent
    return Color(
        red = (red * foregroundAlpha + background.red * backgroundAlpha * (1f - foregroundAlpha)) / outputAlpha,
        green = (green * foregroundAlpha + background.green * backgroundAlpha * (1f - foregroundAlpha)) / outputAlpha,
        blue = (blue * foregroundAlpha + background.blue * backgroundAlpha * (1f - foregroundAlpha)) / outputAlpha,
        alpha = outputAlpha
    )
}

internal fun playerContrastRatio(foreground: Color, background: Color): Float {
    val opaqueBackground = background.playerCompositeOver(Color.Black).copy(alpha = 1f)
    val opaqueForeground = foreground.playerCompositeOver(opaqueBackground).copy(alpha = 1f)
    val foregroundLuminance = opaqueForeground.luminance()
    val backgroundLuminance = opaqueBackground.luminance()
    val lighter = maxOf(foregroundLuminance, backgroundLuminance)
    val darker = minOf(foregroundLuminance, backgroundLuminance)
    return (lighter + 0.05f) / (darker + 0.05f)
}

internal fun Color.playerAdjustForegroundToward(
    target: Color,
    backgrounds: List<Color>,
    minimumContrast: Float
): PlayerContrastAdjustment {
    val source = copy(alpha = 1f)
    val opaqueTarget = target.copy(alpha = 1f)
    if (backgrounds.all { playerContrastRatio(source, it) >= minimumContrast }) {
        return PlayerContrastAdjustment(source, 0f, true)
    }
    if (backgrounds.any { playerContrastRatio(opaqueTarget, it) < minimumContrast }) {
        return PlayerContrastAdjustment(opaqueTarget, 1f, false)
    }
    var low = 0f
    var high = 1f
    repeat(24) {
        val middle = (low + high) / 2f
        val candidate = source.playerMix(opaqueTarget, middle).copy(alpha = 1f)
        if (backgrounds.all { playerContrastRatio(candidate, it) >= minimumContrast }) {
            high = middle
        } else {
            low = middle
        }
    }
    return PlayerContrastAdjustment(source.playerMix(opaqueTarget, high).copy(alpha = 1f), high, true)
}

internal fun Color.playerContentColor(
    backgrounds: List<Color>,
    minimumContrast: Float = PlayerMinimumContrast
): Color {
    val white = playerAdjustForegroundToward(Color.White, backgrounds, minimumContrast)
    val black = playerAdjustForegroundToward(Color.Black, backgrounds, minimumContrast)
    return when {
        white.valid && black.valid -> if (white.amount <= black.amount) white.color else black.color
        white.valid -> white.color
        black.valid -> black.color
        else -> if (backgrounds.sumOf { it.luminance().toDouble() } / backgrounds.size.coerceAtLeast(1) < 0.5) Color.White else Color.Black
    }
}

internal fun Color.playerAdjustBackgroundFor(
    content: Color,
    minimumContrast: Float
): PlayerContrastAdjustment {
    val source = if (minimumContrast >= PlayerStrongContrast) {
        playerMix(PlayerDarkSurface, PlayerStrongSurfaceDarkening).copy(alpha = 1f)
    } else {
        copy(alpha = 1f)
    }
    if (playerContrastRatio(content, source) >= minimumContrast) {
        return PlayerContrastAdjustment(source, 0f, true)
    }
    val target = if (content.luminance() >= 0.5f) Color.Black else Color.White
    if (playerContrastRatio(content, target) < minimumContrast) {
        return PlayerContrastAdjustment(target, 1f, false)
    }
    var low = 0f
    var high = 1f
    repeat(24) {
        val middle = (low + high) / 2f
        val candidate = source.playerMix(target, middle).copy(alpha = 1f)
        if (playerContrastRatio(content, candidate) >= minimumContrast) {
            high = middle
        } else {
            low = middle
        }
    }
    return PlayerContrastAdjustment(source.playerMix(target, high).copy(alpha = 1f), high, true)
}

internal fun playerContrastGradient(
    start: Color,
    end: Color,
    minimumContrast: Float = PlayerMinimumContrast
): PlayerContrastGradient {
    fun candidate(content: Color): Pair<PlayerContrastGradient, Float>? {
        val safeStart = start.playerAdjustBackgroundFor(content, minimumContrast)
        val safeEnd = end.playerAdjustBackgroundFor(content, minimumContrast)
        if (!safeStart.valid || !safeEnd.valid) return null
        return PlayerContrastGradient(safeStart.color, safeEnd.color, content) to safeStart.amount + safeEnd.amount
    }
    val white = candidate(Color.White)
    val black = candidate(Color.Black)
    return when {
        white != null && black != null -> if (white.second <= black.second) white.first else black.first
        white != null -> white.first
        black != null -> black.first
        else -> PlayerContrastGradient(Color.Black, Color.Black, Color.White)
    }
}
