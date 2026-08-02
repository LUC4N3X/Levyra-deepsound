package com.luc4n3x.levyra.desktop.app.ui.components

import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/**
 * Kept for source compatibility with older desktop cards.
 * Scaling raster artwork on pointer enter/exit caused visible flashes on Windows,
 * so hover feedback is now handled by lightweight surface changes in each card.
 */
@Suppress("UNUSED_PARAMETER")
fun Modifier.hoverScale(target: Float = 1.03f): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    hoverable(interactionSource)
}

@Composable
fun rememberHoverState(key: Any? = null): Pair<MutableInteractionSource, Boolean> {
    val interactionSource = remember(key) { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    return interactionSource to hovered
}
