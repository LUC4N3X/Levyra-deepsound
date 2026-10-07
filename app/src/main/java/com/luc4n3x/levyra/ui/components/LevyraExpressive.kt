package com.luc4n3x.levyra.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LevyraLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    LoadingIndicator(modifier = modifier, color = color)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun levyraExpressiveCorner(
    interactionSource: InteractionSource,
    rest: Dp,
    pressed: Dp,
    label: String
): Dp {
    val isPressed by interactionSource.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = if (isPressed) pressed else rest,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = label
    )
    return corner
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun levyraExpressiveToggleCorner(
    checked: Boolean,
    unchecked: Dp,
    checkedCorner: Dp,
    label: String
): Dp {
    val corner by animateDpAsState(
        targetValue = if (checked) checkedCorner else unchecked,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = label
    )
    return corner
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LevyraWavyProgress(
    progress: Float,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier
) {
    LinearWavyProgressIndicator(
        progress = { progress },
        modifier = modifier,
        color = color,
        trackColor = trackColor
    )
}
