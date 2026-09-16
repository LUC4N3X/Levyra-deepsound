package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.max
import com.luc4n3x.levyra.ui.components.PlayerIcon
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraHaptics
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LevyraPlayerShapes
import com.luc4n3x.levyra.ui.theme.LevyraSegment
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics

private const val DisabledSegmentAlpha = 0.42f
private const val BusyGlyphRatio = 0.82f

@Composable
internal fun RowScope.PlayerSegmentButton(
    position: LevyraSegment,
    weight: Float,
    container: Color,
    innerCorner: Dp,
    contentDescription: String,
    animated: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    toggleState: ToggleableState? = null,
    stateDescription: String? = null,
    outline: Color = Color.Transparent,
    haptic: LevyraHapticAction? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val haptics = LocalLevyraHaptics.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val squished = pressed && enabled && animated
    val animatedWeight by animateFloatAsState(
        targetValue = segmentWeight(weight, squished),
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.pressSpring()),
        label = "player-segment-weight"
    )
    val corner by animateDpAsState(
        targetValue = segmentCorner(innerCorner, squished),
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.pressSpring()),
        label = "player-segment-corner"
    )
    val fill by animateColorAsState(
        targetValue = container,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.standardTween(200)),
        label = "player-segment-fill"
    )
    val shape = LevyraPlayerShapes.segment(position, corner)

    Box(
        modifier = modifier
            .weight(animatedWeight)
            .fillMaxHeight()
            .graphicsLayer { alpha = segmentAlpha(enabled) }
            .clip(shape)
            .background(fill)
            .segmentOutline(outline, shape)
            .clickable(
                interactionSource = interaction,
                indication = segmentIndication(animated),
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    haptics.performOptional(haptic)
                    onClick()
                }
            )
            .segmentSemantics(contentDescription, toggleState, stateDescription),
        contentAlignment = Alignment.Center,
        content = content
    )
}

private fun segmentWeight(weight: Float, squished: Boolean): Float =
    if (squished) weight * (1f + LevyraPlayerDesign.SegmentPressGrowth) else weight

private fun segmentCorner(innerCorner: Dp, squished: Boolean): Dp =
    if (squished) max(innerCorner, LevyraPlayerDesign.SegmentPressedInnerCorner) else innerCorner

private fun segmentAlpha(enabled: Boolean): Float = if (enabled) 1f else DisabledSegmentAlpha

@Composable
private fun segmentIndication(animated: Boolean): Indication? =
    if (animated) null else LocalIndication.current

private fun LevyraHaptics.performOptional(action: LevyraHapticAction?) {
    if (action != null) perform(action)
}

private fun Modifier.segmentOutline(outline: Color, shape: Shape): Modifier =
    if (outline.alpha > 0f) border(LevyraPlayerDesign.Hairline, outline, shape) else this

private fun Modifier.segmentSemantics(
    description: String,
    toggleState: ToggleableState?,
    stateLabel: String?
): Modifier = semantics {
    contentDescription = description
    if (toggleState != null) toggleableState = toggleState
    if (stateLabel != null) stateDescription = stateLabel
}

internal fun segmentPosition(index: Int, count: Int): LevyraSegment = when {
    count == 1 -> LevyraSegment.Single
    index == 0 -> LevyraSegment.Leading
    index == count - 1 -> LevyraSegment.Trailing
    else -> LevyraSegment.Middle
}

internal fun segmentToggleState(toggle: Boolean, active: Boolean): ToggleableState? =
    if (toggle) ToggleableState(active) else null

@Composable
internal fun PlayerSegmentGlyph(icon: ImageVector, tint: Color, busy: Boolean, size: Dp) {
    if (busy) {
        CircularProgressIndicator(
            modifier = Modifier.size(size * BusyGlyphRatio),
            strokeWidth = LevyraPlayerDesign.SpaceXxs,
            color = tint
        )
    } else {
        PlayerIcon(icon, tint, Modifier.size(size))
    }
}
