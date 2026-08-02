package com.luc4n3x.levyra.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.theme.LevyraHomeDesign

/**
 * Compatibility models kept only until the obsolete quick-access item is removed from LevyraApp.
 * The composable intentionally renders nothing: the shortcut dashboard is no longer part of Home.
 */
internal data class LevyraHomeQuickAccessTracks(
    val current: Track?,
    val mix: Track?,
    val favorite: Track?,
    val release: Track?,
    val chart: Track?
)

internal data class LevyraHomeQuickAccessAvailability(
    val hasMix: Boolean,
    val hasFavorites: Boolean,
    val hasNewReleases: Boolean,
    val hasCharts: Boolean
)

internal data class LevyraHomeQuickAccessPlayback(
    val isPlaying: Boolean,
    val isResolving: Boolean
)

internal data class LevyraHomeQuickAccessState(
    val tracks: LevyraHomeQuickAccessTracks,
    val availability: LevyraHomeQuickAccessAvailability,
    val playback: LevyraHomeQuickAccessPlayback,
    val isLight: Boolean
)

internal data class LevyraHomeQuickAccessActions(
    val onContinue: () -> Unit,
    val onMix: () -> Unit,
    val onFavorites: () -> Unit,
    val onNewReleases: () -> Unit,
    val onCharts: () -> Unit,
    val onSearch: () -> Unit
)

@Suppress("UNUSED_PARAMETER")
@Composable
internal fun LevyraHomeQuickAccessGrid(
    state: LevyraHomeQuickAccessState,
    actions: LevyraHomeQuickAccessActions
) = Unit

/**
 * Artwork-led Home backdrop with cached drawing primitives and no permanently running animation.
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
            .height(LevyraHomeDesign.AtmosphereHeight)
            .homeAtmosphereBackground(primary, secondary, isLight)
    )
}

private fun Modifier.homeAtmosphereBackground(
    primary: Color,
    secondary: Color,
    isLight: Boolean
): Modifier = drawWithCache {
    val width = size.width
    val height = size.height
    val safeRadius = width.coerceAtLeast(1f)
    val leftCenter = Offset(width * 0.12f, height * 0.06f)
    val rightCenter = Offset(width * 0.96f, height * 0.22f)
    val leftRadius = safeRadius * 0.94f
    val rightRadius = safeRadius * 0.78f
    val fadeTop = height * 0.46f

    val base = homeBaseBrush(isLight)
    val leftHalo = homeHaloBrush(primary, isLight, leftCenter, leftRadius, prominent = true)
    val rightHalo = homeHaloBrush(secondary, isLight, rightCenter, rightRadius, prominent = false)
    val wave = homeWavePath(width, height)
    val echo = homeEchoPath(width, height)
    val waveBrush = homeWaveBrush(primary, secondary, isLight)
    val bottomFade = homeBottomFadeBrush(isLight, fadeTop, height)

    onDrawBehind {
        drawRect(base)
        drawCircle(leftHalo, radius = leftRadius, center = leftCenter)
        drawCircle(rightHalo, radius = rightRadius, center = rightCenter)
        drawPath(wave, brush = waveBrush, style = Stroke(width = 1.25.dp.toPx()))
        drawPath(
            echo,
            color = homeEchoColor(primary, isLight),
            style = Stroke(width = 0.75.dp.toPx())
        )
        drawRect(
            brush = bottomFade,
            topLeft = Offset(0f, fadeTop),
            size = Size(width, height - fadeTop)
        )
    }
}

private fun homeBaseBrush(isLight: Boolean): Brush = if (isLight) {
    Brush.verticalGradient(
        listOf(
            Color(0xFFF9FAFF),
            Color(0xFFF4F6FC),
            Color(0xFFF1F3F8)
        )
    )
} else {
    Brush.verticalGradient(
        colorStops = arrayOf(
            0f to LevyraHomeDesign.CanvasMid,
            0.34f to LevyraHomeDesign.CanvasDark,
            1f to Color.Black
        )
    )
}

private fun homeHaloBrush(
    color: Color,
    isLight: Boolean,
    center: Offset,
    radius: Float,
    prominent: Boolean
): Brush = Brush.radialGradient(
    colors = listOf(
        color.copy(alpha = homeHaloAlpha(isLight, prominent, leading = true)),
        color.copy(alpha = homeHaloAlpha(isLight, prominent, leading = false)),
        Color.Transparent
    ),
    center = center,
    radius = radius
)

private fun homeHaloAlpha(isLight: Boolean, prominent: Boolean, leading: Boolean): Float = when {
    isLight && prominent && leading -> 0.12f
    isLight && prominent -> 0.035f
    isLight && leading -> 0.08f
    isLight -> 0.02f
    prominent && leading -> 0.14f
    prominent -> 0.04f
    leading -> 0.10f
    else -> 0.03f
}

private fun homeWaveBrush(primary: Color, secondary: Color, isLight: Boolean): Brush =
    Brush.horizontalGradient(
        listOf(
            Color.Transparent,
            primary.copy(alpha = if (isLight) 0.08f else 0.16f),
            secondary.copy(alpha = if (isLight) 0.06f else 0.12f),
            Color.Transparent
        )
    )

private fun homeBottomFadeBrush(isLight: Boolean, fadeTop: Float, height: Float): Brush =
    Brush.verticalGradient(
        colors = if (isLight) {
            listOf(
                Color.Transparent,
                Color(0xFFF1F3F8).copy(alpha = 0.86f),
                Color(0xFFF1F3F8)
            )
        } else {
            listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f), Color.Black)
        },
        startY = fadeTop,
        endY = height
    )

private fun homeEchoColor(primary: Color, isLight: Boolean): Color =
    if (isLight) primary.copy(alpha = 0.035f) else Color.White.copy(alpha = 0.035f)

private fun homeWavePath(width: Float, height: Float): Path = Path().apply {
    moveTo(-width * 0.08f, height * 0.29f)
    cubicTo(
        width * 0.18f,
        height * 0.19f,
        width * 0.33f,
        height * 0.37f,
        width * 0.54f,
        height * 0.25f
    )
    cubicTo(
        width * 0.72f,
        height * 0.15f,
        width * 0.89f,
        height * 0.31f,
        width * 1.08f,
        height * 0.21f
    )
}

private fun homeEchoPath(width: Float, height: Float): Path = Path().apply {
    moveTo(-width * 0.06f, height * 0.32f)
    cubicTo(
        width * 0.19f,
        height * 0.23f,
        width * 0.36f,
        height * 0.41f,
        width * 0.56f,
        height * 0.29f
    )
    cubicTo(
        width * 0.74f,
        height * 0.19f,
        width * 0.91f,
        height * 0.34f,
        width * 1.07f,
        height * 0.25f
    )
}
