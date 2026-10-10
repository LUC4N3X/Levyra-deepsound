package com.luc4n3x.levyra.ui.components

import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
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

@Composable
internal fun LevyraExpressiveIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        shapes = IconButtonDefaults.shapes(
            shape = MaterialTheme.shapes.extraLarge,
            pressedShape = MaterialTheme.shapes.medium
        ),
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        content = content
    )
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

internal val LevyraGroupedOuterCorner: Dp = 24.dp
internal val LevyraGroupedInnerCorner: Dp = 6.dp

internal fun levyraGroupedListShape(index: Int, count: Int): Shape {
    val first = index == 0
    val last = index == count - 1
    return RoundedCornerShape(
        topStart = if (first) LevyraGroupedOuterCorner else LevyraGroupedInnerCorner,
        topEnd = if (first) LevyraGroupedOuterCorner else LevyraGroupedInnerCorner,
        bottomEnd = if (last) LevyraGroupedOuterCorner else LevyraGroupedInnerCorner,
        bottomStart = if (last) LevyraGroupedOuterCorner else LevyraGroupedInnerCorner
    )
}

internal fun levyraGroupedGridShape(row: Int, column: Int, rows: Int, columns: Int): Shape {
    fun corner(isOuter: Boolean): Dp = if (isOuter) LevyraGroupedOuterCorner else LevyraGroupedInnerCorner
    val top = row == 0
    val bottom = row == rows - 1
    val start = column == 0
    val end = column == columns - 1
    return RoundedCornerShape(
        topStart = corner(top && start),
        topEnd = corner(top && end),
        bottomEnd = corner(bottom && end),
        bottomStart = corner(bottom && start)
    )
}
