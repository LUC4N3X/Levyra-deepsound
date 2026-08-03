package com.luc4n3x.levyra.desktop.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

fun Modifier.hoverScale(target: Float = 1.03f): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(targetValue = if (hovered) target else 1f)
    hoverable(interactionSource).graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun rememberHoverState(key: Any? = null): Pair<MutableInteractionSource, Boolean> {
    val interactionSource = remember(key) { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    return interactionSource to hovered
}
