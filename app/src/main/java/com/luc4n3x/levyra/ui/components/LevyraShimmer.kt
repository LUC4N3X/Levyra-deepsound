package com.luc4n3x.levyra.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign

private const val SweepDurationMs = 1_150
private const val HoldDurationMs = 620
private val SweepEasing = CubicBezierEasing(0.25f, 0f, 0.2f, 1f)
private const val SweepWidthFraction = 0.55f
private const val TiltPx = 220f

object LevyraShimmerDefaults {
    val Shape: Shape = LevyraPlayerDesign.ShapeXs
    val Base: Color = Color.White.copy(alpha = 0.055f)
    val Highlight: Color = Color.White.copy(alpha = 0.13f)
}

@Composable
fun rememberLevyraShimmerProgress(active: Boolean = true): State<Float>? {
    if (!active || !LocalAnimationsEnabled.current) return null
    val transition = rememberInfiniteTransition(label = "levyra-shimmer")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = SweepDurationMs + HoldDurationMs
                0f at 0 using SweepEasing
                1f at SweepDurationMs
                1f at SweepDurationMs + HoldDurationMs
            }
        ),
        label = "levyra-shimmer-progress"
    )
}

fun Modifier.levyraShimmerSweep(
    progress: State<Float>?,
    accent: Color? = null
): Modifier {
    if (progress == null) return this
    return this.drawWithCache {
        val highlight = accent?.copy(alpha = 0.16f) ?: LevyraShimmerDefaults.Highlight
        val sweepWidth = (size.width * SweepWidthFraction).coerceAtLeast(1f)
        val brush = Brush.linearGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                0.5f to highlight,
                1f to Color.Transparent
            ),
            start = Offset(0f, 0f),
            end = Offset(sweepWidth, TiltPx)
        )
        val travel = size.width + sweepWidth
        onDrawWithContent {
            drawContent()
            translate(left = -sweepWidth + travel * progress.value) {
                drawRect(brush = brush, topLeft = Offset.Zero, size = size)
            }
        }
    }
}

@Composable
fun Modifier.levyraShimmer(
    active: Boolean = true,
    accent: Color? = null
): Modifier = levyraShimmerSweep(rememberLevyraShimmerProgress(active), accent)

@Composable
fun LevyraSkeletonBlock(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = LevyraShimmerDefaults.Shape,
    accent: Color? = null,
    progress: State<Float>? = rememberLevyraShimmerProgress()
) {
    val base = remember(accent) { accent?.copy(alpha = 0.07f) ?: LevyraShimmerDefaults.Base }
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(shape)
            .background(base)
            .levyraShimmerSweep(progress, accent)
    )
}
