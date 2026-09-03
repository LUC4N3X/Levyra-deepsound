package com.luc4n3x.levyra.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.luc4n3x.levyra.ui.theme.LevyraHomeDesign

/**
 * Full-screen artwork-led Home backdrop.
 *
 * The palette follows the current editorial spotlight, while the lower half stays deliberately
 * quiet so shelves remain readable. There are no permanently running animations or bitmap effects.
 */
@Composable
internal fun LevyraHomeAtmosphere(
    accentStart: Color,
    accentEnd: Color,
    isLight: Boolean,
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val primary by animateColorAsState(
        targetValue = accentStart,
        animationSpec = if (animationsEnabled) tween(620) else snap(),
        label = "homeAuraPrimary"
    )
    val secondary by animateColorAsState(
        targetValue = accentEnd,
        animationSpec = if (animationsEnabled) tween(620) else snap(),
        label = "homeAuraSecondary"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .homeAtmosphereBackground(primary, secondary, isLight)
    )
}

private fun Modifier.homeAtmosphereBackground(
    primary: Color,
    secondary: Color,
    isLight: Boolean
): Modifier = drawWithCache {
    val width = size.width.coerceAtLeast(1f)
    val height = size.height.coerceAtLeast(1f)
    val visualPrimary = if (isLight) primary else softenHomeAccent(primary)
    val visualSecondary = if (isLight) secondary else softenHomeAccent(secondary)
    val blendedAccent = blendHomeAccents(visualPrimary, visualSecondary)
    val primaryCenter = if (isLight) Offset(width * 0.12f, -height * 0.10f) else Offset(width * 0.18f, height * 0.02f)
    val secondaryCenter = if (isLight) Offset(width * 0.98f, height * 0.26f) else Offset(width * 0.92f, height * 0.17f)
    val centreCenter = if (isLight) Offset(width * 0.52f, height * 0.44f) else Offset(width * 0.50f, height * 0.28f)
    val heroBridgeCenter = Offset(width * 0.50f, height * 0.24f)
    val primaryRadius = if (isLight) width * 1.34f else width * 1.10f
    val secondaryRadius = if (isLight) width * 1.02f else width * 0.92f
    val centreRadius = if (isLight) width * 1.16f else width * 1.00f
    val heroBridgeRadius = width * 0.86f
    val fadeTop = if (isLight) height * 0.34f else height * 0.25f

    val base = homeBaseBrush(isLight)
    val primaryHalo = homeHaloBrush(
        color = visualPrimary,
        center = primaryCenter,
        radius = primaryRadius,
        leadingAlpha = if (isLight) 0.18f else 0.10f,
        trailingAlpha = if (isLight) 0.055f else 0.016f
    )
    val secondaryHalo = homeHaloBrush(
        color = visualSecondary,
        center = secondaryCenter,
        radius = secondaryRadius,
        leadingAlpha = if (isLight) 0.12f else 0.07f,
        trailingAlpha = if (isLight) 0.035f else 0.012f
    )
    val centreWash = homeCentreWashBrush(
        color = blendedAccent,
        center = centreCenter,
        radius = centreRadius,
        alpha = if (isLight) 0.045f else 0.020f
    )
    val heroBridge = homeCentreWashBrush(
        color = blendedAccent,
        center = heroBridgeCenter,
        radius = heroBridgeRadius,
        alpha = if (isLight) 0f else 0.045f
    )
    val lowerFade = homeLowerFadeBrush(isLight, fadeTop, height)
    val persistentTint = homePersistentTintBrush(blendedAccent, height, isLight)
    val edgeVignette = homeEdgeVignetteBrush(isLight)

    onDrawBehind {
        drawRect(base)
        drawCircle(primaryHalo, radius = primaryRadius, center = primaryCenter)
        drawCircle(secondaryHalo, radius = secondaryRadius, center = secondaryCenter)
        drawCircle(centreWash, radius = centreRadius, center = centreCenter)
        drawCircle(heroBridge, radius = heroBridgeRadius, center = heroBridgeCenter)
        drawRect(edgeVignette)
        drawRect(
            brush = lowerFade,
            topLeft = Offset(0f, fadeTop),
            size = Size(width, height - fadeTop)
        )
        drawRect(persistentTint)
    }
}

private fun homeBaseBrush(isLight: Boolean): Brush = if (isLight) {
    Brush.verticalGradient(
        colorStops = arrayOf(
            0f to Color(0xFFF8FAFF),
            0.28f to Color(0xFFF4F6FB),
            0.58f to Color(0xFFF1F3F8),
            1f to Color(0xFFF1F3F8)
        )
    )
} else {
    Brush.verticalGradient(
        colorStops = arrayOf(
            0f to LevyraHomeDesign.CanvasMid,
            0.24f to LevyraHomeDesign.CanvasDark,
            0.50f to Color(0xFF05070C),
            0.72f to Color(0xFF03050A),
            1f to Color(0xFF020308)
        )
    )
}

private fun homeHaloBrush(
    color: Color,
    center: Offset,
    radius: Float,
    leadingAlpha: Float,
    trailingAlpha: Float
): Brush = Brush.radialGradient(
    colors = listOf(
        color.copy(alpha = leadingAlpha),
        color.copy(alpha = trailingAlpha),
        Color.Transparent
    ),
    center = center,
    radius = radius
)

private fun homeCentreWashBrush(
    color: Color,
    center: Offset,
    radius: Float,
    alpha: Float
): Brush = Brush.radialGradient(
    colors = listOf(color.copy(alpha = alpha), Color.Transparent),
    center = center,
    radius = radius
)

private fun homeLowerFadeBrush(isLight: Boolean, fadeTop: Float, height: Float): Brush {
    val colors = if (isLight) {
        listOf(
            Color.Transparent,
            Color(0xFFF1F3F8).copy(alpha = 0.78f),
            Color(0xFFF1F3F8)
        )
    } else {
        listOf(
            Color.Transparent,
            Color(0xFF05070C).copy(alpha = 0.66f),
            Color(0xFF03050A).copy(alpha = 0.93f),
            Color(0xFF020308)
        )
    }
    return Brush.verticalGradient(
        colors = colors,
        startY = fadeTop,
        endY = if (isLight) height * 0.88f else height * 0.64f
    )
}

private fun homePersistentTintBrush(color: Color, height: Float, isLight: Boolean): Brush {
    if (isLight) return Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
    return Brush.verticalGradient(
        colorStops = arrayOf(
            0f to Color.Transparent,
            0.26f to color.copy(alpha = 0.008f),
            0.48f to color.copy(alpha = 0.006f),
            0.72f to color.copy(alpha = 0.004f),
            1f to color.copy(alpha = 0.003f)
        ),
        startY = 0f,
        endY = height
    )
}

private fun homeEdgeVignetteBrush(isLight: Boolean): Brush {
    val colors = if (isLight) {
        listOf(
            Color(0xFFF1F3F8).copy(alpha = 0.22f),
            Color.Transparent,
            Color.Transparent,
            Color(0xFFF1F3F8).copy(alpha = 0.18f)
        )
    } else {
        listOf(
            Color.Black.copy(alpha = 0.10f),
            Color.Transparent,
            Color.Transparent,
            Color.Black.copy(alpha = 0.12f)
        )
    }
    return Brush.horizontalGradient(colors)
}

private fun softenHomeAccent(color: Color): Color {
    val grey = color.red * 0.299f + color.green * 0.587f + color.blue * 0.114f
    val desaturated = Color(
        red = color.red + (grey - color.red) * 0.42f,
        green = color.green + (grey - color.green) * 0.42f,
        blue = color.blue + (grey - color.blue) * 0.42f,
        alpha = 1f
    )
    val neutral = Color(0xFF0A0D14)
    return Color(
        red = desaturated.red * 0.34f + neutral.red * 0.66f,
        green = desaturated.green * 0.34f + neutral.green * 0.66f,
        blue = desaturated.blue * 0.34f + neutral.blue * 0.66f,
        alpha = 1f
    )
}

private fun blendHomeAccents(first: Color, second: Color): Color = Color(
    red = (first.red + second.red) / 2f,
    green = (first.green + second.green) / 2f,
    blue = (first.blue + second.blue) / 2f,
    alpha = 1f
)
