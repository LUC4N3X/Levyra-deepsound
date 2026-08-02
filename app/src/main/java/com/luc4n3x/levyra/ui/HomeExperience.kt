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

    val base = if (isLight) {
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

    val primaryCenter = Offset(width * 0.12f, -height * 0.02f)
    val primaryRadius = width * 1.18f
    val primaryHalo = Brush.radialGradient(
        colors = listOf(
            primary.copy(alpha = if (isLight) 0.18f else 0.30f),
            primary.copy(alpha = if (isLight) 0.055f else 0.085f),
            Color.Transparent
        ),
        center = primaryCenter,
        radius = primaryRadius
    )

    val secondaryCenter = Offset(width * 0.98f, height * 0.16f)
    val secondaryRadius = width * 0.86f
    val secondaryHalo = Brush.radialGradient(
        colors = listOf(
            secondary.copy(alpha = if (isLight) 0.12f else 0.22f),
            secondary.copy(alpha = if (isLight) 0.035f else 0.06f),
            Color.Transparent
        ),
        center = secondaryCenter,
        radius = secondaryRadius
    )

    val centreCenter = Offset(width * 0.52f, height * 0.26f)
    val centreRadius = width * 0.92f
    val centreWash = Brush.radialGradient(
        colors = listOf(
            blendHomeAccents(primary, secondary).copy(alpha = if (isLight) 0.045f else 0.075f),
            Color.Transparent
        ),
        center = centreCenter,
        radius = centreRadius
    )

    val fadeTop = height * 0.25f
    val lowerFade = Brush.verticalGradient(
        colors = if (isLight) {
            listOf(
                Color.Transparent,
                Color(0xFFF1F3F8).copy(alpha = 0.78f),
                Color(0xFFF1F3F8)
            )
        } else {
            listOf(
                Color.Transparent,
                Color.Black.copy(alpha = 0.78f),
                Color.Black
            )
        },
        startY = fadeTop,
        endY = height * 0.62f
    )

    val edgeVignette = if (isLight) {
        Brush.horizontalGradient(
            listOf(
                Color(0xFFF1F3F8).copy(alpha = 0.22f),
                Color.Transparent,
                Color.Transparent,
                Color(0xFFF1F3F8).copy(alpha = 0.18f)
            )
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                Color.Black.copy(alpha = 0.22f),
                Color.Transparent,
                Color.Transparent,
                Color.Black.copy(alpha = 0.26f)
            )
        )
    }

    onDrawBehind {
        drawRect(base)
        drawCircle(primaryHalo, radius = primaryRadius, center = primaryCenter)
        drawCircle(secondaryHalo, radius = secondaryRadius, center = secondaryCenter)
        drawCircle(centreWash, radius = centreRadius, center = centreCenter)
        drawRect(edgeVignette)
        drawRect(
            brush = lowerFade,
            topLeft = Offset(0f, fadeTop),
            size = Size(width, height - fadeTop)
        )
    }
}

private fun blendHomeAccents(first: Color, second: Color): Color = Color(
    red = (first.red + second.red) / 2f,
    green = (first.green + second.green) / 2f,
    blue = (first.blue + second.blue) / 2f,
    alpha = 1f
)
