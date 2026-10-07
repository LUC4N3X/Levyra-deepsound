package com.luc4n3x.levyra.ui.components

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp

private val MarqueeEdgeFade = 12.dp

fun Modifier.levyraMarquee(
    enabled: Boolean,
    repeatDelayMillis: Int,
    edgeFade: Dp = MarqueeEdgeFade
): Modifier {
    if (!enabled) return this
    return this
        .layout { measurable, constraints ->
            val edgePx = edgeFade.roundToPx()
            val placeable = measurable.measure(
                constraints.copy(
                    minWidth = if (constraints.minWidth > 0) constraints.minWidth + edgePx * 2 else 0,
                    maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth + edgePx * 2 else constraints.maxWidth
                )
            )
            val width = constraints.constrainWidth((placeable.width - edgePx * 2).coerceAtLeast(0))
            layout(width, placeable.height) {
                placeable.place(-edgePx, 0)
            }
        }
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithCache {
            val edgeFraction = if (size.width > 0f) (edgeFade.toPx() / size.width).coerceIn(0f, 0.5f) else 0f
            val mask = Brush.horizontalGradient(
                0f to Color.Transparent,
                edgeFraction to Color.Black,
                1f - edgeFraction to Color.Black,
                1f to Color.Transparent
            )
            onDrawWithContent {
                drawContent()
                drawRect(brush = mask, blendMode = BlendMode.DstIn)
            }
        }
        .basicMarquee(iterations = Int.MAX_VALUE, repeatDelayMillis = repeatDelayMillis)
        .padding(horizontal = edgeFade)
}
