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
    isLight: Boolean // Ignored, always dark theme for Home
): Modifier = drawWithCache {
    val width = size.width
    val height = size.height
    val fadeTop = height * 0.4f

    // Cool deep cosmic gradient
    val topGradient = Brush.radialGradient(
        colors = listOf(
            primary.copy(alpha = 0.25f),
            secondary.copy(alpha = 0.15f),
            Color.Transparent
        ),
        center = Offset(width / 2f, 0f),
        radius = width.coerceAtLeast(height) * 0.9f
    )

    val backgroundDark = Color(0xFF0C0A15) // Deep cosmic purple-blue, not total black

    val bottomFade = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            backgroundDark.copy(alpha = 0.8f),
            backgroundDark
        ),
        startY = fadeTop,
        endY = height
    )

    onDrawBehind {
        drawRect(backgroundDark)
        drawRect(topGradient)
        // Draw the fade to ensure it completely blends to the cosmic color at the bottom
        drawRect(
            brush = bottomFade,
            topLeft = Offset(0f, 0f),
            size = Size(width, height)
        )
    }
}
