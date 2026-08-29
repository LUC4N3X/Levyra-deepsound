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
 * quiet so shelves remain readable. [accentIntensity] is read in the draw phase so leaving the top
 * of the page returns Home to its neutral canvas without recomposing content. There are no
 * permanently running animations or bitmap effects.
 */
@Composable
internal fun LevyraHomeAtmosphere(
    accentStart: Color,
    accentEnd: Color,
    isLight: Boolean,
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier,
    accentIntensity: () -> Float = { 1f }
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
            .homeAtmosphereBackground(primary, secondary, isLight, accentIntensity)
    )
}

private fun Modifier.homeAtmosphereBackground(
    primary: Color,
    secondary: Color,
    isLight: Boolean,
    accentIntensity: () -> Float
): Modifier = drawWithCache {
    val width = size.width.coerceAtLeast(1f)
    val height = size.height.coerceAtLeast(1f)
    val primaryCenter = Offset(width * 0.12f, -height * 0.02f)
    val secondaryCenter = Offset(width * 0.98f, height * 0.16f)
    val centreCenter = Offset(width * 0.52f, height * 0.26f)
    val primaryRadius = width * 1.18f
    val secondaryRadius = width * 0.86f
    val centreRadius = width * 0.92f
    val fadeTop = height * 0.28f

    val base = homeBaseBrush(isLight)
    val primaryHalo = homeHaloBrush(
        color = primary,
        center = primaryCenter,
        radius = primaryRadius,
        leadingAlpha = if (isLight) 0.18f else 0.30f,
        trailingAlpha = if (isLight) 0.055f else 0.085f
    )
    val secondaryHalo = homeHaloBrush(
        color = secondary,
        center = secondaryCenter,
        radius = secondaryRadius,
        leadingAlpha = if (isLight) 0.12f else 0.22f,
        trailingAlpha = if (isLight) 0.035f else 0.06f
    )
    val centreWash = homeCentreWashBrush(
        color = blendHomeAccents(primary, secondary),
        center = centreCenter,
        radius = centreRadius,
        alpha = if (isLight) 0.045f else 0.075f
    )
    val lowerFade = homeLowerFadeBrush(isLight, fadeTop, height)
    val edgeVignette = homeEdgeVignetteBrush(isLight)

    onDrawBehind {
        val intensity = accentIntensity().coerceIn(0f, 1f)
        drawRect(base)
        if (intensity > 0f) {
            drawCircle(primaryHalo, radius = primaryRadius, center = primaryCenter, alpha = intensity)
            drawCircle(secondaryHalo, radius = secondaryRadius, center = secondaryCenter, alpha = intensity)
            drawCircle(centreWash, radius = centreRadius, center = centreCenter, alpha = intensity)
        }
        drawRect(edgeVignette)
        drawRect(
            brush = lowerFade,
            topLeft = Offset(0f, fadeTop),
            size = Size(width, height - fadeTop)
        )
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
            0.52f to Color(0xFF030407),
            1f to Color.Black
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
            Color(0xFFF1F3F8).copy(alpha = 0.42f),
            Color(0xFFF1F3F8).copy(alpha = 0.86f),
            Color(0xFFF1F3F8)
        )
    } else {
        listOf(
            Color.Transparent,
            Color.Black.copy(alpha = 0.42f),
            Color.Black.copy(alpha = 0.86f),
            Color.Black
        )
    }
    return Brush.verticalGradient(
        colors = colors,
        startY = fadeTop,
        endY = height * 0.68f
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
            Color.Black.copy(alpha = 0.22f),
            Color.Transparent,
            Color.Transparent,
            Color.Black.copy(alpha = 0.26f)
        )
    }
    return Brush.horizontalGradient(colors)
}

private fun blendHomeAccents(first: Color, second: Color): Color = Color(
    red = (first.red + second.red) / 2f,
    green = (first.green + second.green) / 2f,
    blue = (first.blue + second.blue) / 2f,
    alpha = 1f
)
