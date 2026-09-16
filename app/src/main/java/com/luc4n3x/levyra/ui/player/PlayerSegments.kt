package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.max
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LevyraPlayerShapes
import com.luc4n3x.levyra.ui.theme.LevyraSegment
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics

private const val DisabledSegmentAlpha = 0.42f

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
        targetValue = if (squished) weight * (1f + LevyraPlayerDesign.SegmentPressGrowth) else weight,
        animationSpec = if (animated) LevyraPlayerDesign.pressSpring() else snap(),
        label = "player-segment-weight"
    )
    val corner by animateDpAsState(
        targetValue = if (squished) max(innerCorner, LevyraPlayerDesign.SegmentPressedInnerCorner) else innerCorner,
        animationSpec = if (animated) LevyraPlayerDesign.pressSpring() else snap(),
        label = "player-segment-corner"
    )
    val fill by animateColorAsState(
        targetValue = container,
        animationSpec = if (animated) LevyraPlayerDesign.standardTween(200) else snap(),
        label = "player-segment-fill"
    )
    val shape = LevyraPlayerShapes.segment(position, corner)

    Box(
        modifier = modifier
            .weight(animatedWeight)
            .fillMaxHeight()
            .graphicsLayer { alpha = if (enabled) 1f else DisabledSegmentAlpha }
            .clip(shape)
            .background(fill)
            .then(
                if (outline.alpha > 0f) {
                    Modifier.border(LevyraPlayerDesign.Hairline, outline, shape)
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = interaction,
                indication = if (animated) null else LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    haptic?.let(haptics::perform)
                    onClick()
                }
            )
            .semantics {
                this.contentDescription = contentDescription
                toggleState?.let { this.toggleableState = it }
                stateDescription?.let { this.stateDescription = it }
            },
        contentAlignment = Alignment.Center,
        content = content
    )
}
