package com.luc4n3x.levyra.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics

object LevyraPressScale {
    const val Row: Float = 0.985f
    const val Tile: Float = 0.975f
    const val Surface: Float = 0.98f
    const val Control: Float = 0.96f
}

@Composable
fun Modifier.levyraPressable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    pressedScale: Float = LevyraPressScale.Surface,
    interactionSource: MutableInteractionSource? = null,
    role: Role? = null,
    onClickLabel: String? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    haptic: LevyraHapticAction? = null,
    longPressHaptic: LevyraHapticAction? = LevyraHapticAction.Confirm
): Modifier {
    val animationsEnabled = LocalAnimationsEnabled.current
    val haptics = LocalLevyraHaptics.current
    val interaction = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && animationsEnabled) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 900f),
        label = "levyra-press"
    )
    val indication = if (animationsEnabled) null else LocalIndication.current
    val click: () -> Unit = {
        haptic?.let(haptics::perform)
        onClick()
    }
    val longClick: (() -> Unit)? = onLongClick?.let { action ->
        {
            longPressHaptic?.let(haptics::perform)
            action()
        }
    }
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (longClick == null) {
                Modifier.clickable(
                    interactionSource = interaction,
                    indication = indication,
                    enabled = enabled,
                    onClickLabel = onClickLabel,
                    role = role,
                    onClick = click
                )
            } else {
                Modifier.combinedClickable(
                    interactionSource = interaction,
                    indication = indication,
                    enabled = enabled,
                    onClickLabel = onClickLabel,
                    role = role,
                    onLongClickLabel = onLongClickLabel,
                    onLongClick = longClick,
                    onClick = click
                )
            }
        )
}
