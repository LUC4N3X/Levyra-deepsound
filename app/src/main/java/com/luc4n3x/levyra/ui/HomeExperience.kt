package com.luc4n3x.levyra.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.theme.LevyraHomeDesign

internal fun homeCanvasColor(isLight: Boolean): Color =
    if (isLight) LevyraHomeDesign.CanvasLight else LevyraHomeDesign.CanvasDark

@Composable
internal fun LevyraHomeAtmosphere(
    accentStart: Color,
    accentEnd: Color,
    isLight: Boolean,
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val primary = animateColorAsState(
        targetValue = accentStart,
        animationSpec = if (animationsEnabled) tween(620) else snap(),
        label = "homeAuraPrimary"
    )
    val secondary = animateColorAsState(
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
    primary: State<Color>,
    secondary: State<Color>,
    isLight: Boolean
): Modifier = drawWithCache {
    val canvas = homeCanvasColor(isLight)
    val tintHeight = HomeAtmosphereTintHeight.toPx().coerceAtMost(size.height)
    val accent = blendHomeAccents(primary.value, secondary.value)
    val tintAlpha = if (isLight) 0.16f else 0.34f
    val tint = Brush.verticalGradient(
        colorStops = arrayOf(
            0f to accent.copy(alpha = tintAlpha),
            0.45f to accent.copy(alpha = tintAlpha * 0.42f),
            1f to Color.Transparent
        ),
        startY = 0f,
        endY = tintHeight
    )
    val tintSize = Size(size.width, tintHeight)
    onDrawBehind {
        drawRect(canvas)
        drawRect(brush = tint, size = tintSize)
    }
}

private val HomeAtmosphereTintHeight = 260.dp

private fun blendHomeAccents(first: Color, second: Color): Color = Color(
    red = (first.red + second.red) / 2f,
    green = (first.green + second.green) / 2f,
    blue = (first.blue + second.blue) / 2f,
    alpha = 1f
)
