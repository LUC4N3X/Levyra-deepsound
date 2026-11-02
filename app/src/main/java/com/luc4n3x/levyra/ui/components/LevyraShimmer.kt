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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import kotlin.math.abs

private const val PulseDurationMs = 1_150
private const val HoldDurationMs = 620
private val PulseEasing = CubicBezierEasing(0.25f, 0f, 0.2f, 1f)

object LevyraShimmerDefaults {
    val Shape: Shape = LevyraPlayerDesign.ShapeXs
    val Base: Color = Color.White.copy(alpha = 0.055f)
    val Highlight: Color = Color.White.copy(alpha = 0.07f)
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
                durationMillis = PulseDurationMs + HoldDurationMs
                0f at 0 using PulseEasing
                1f at PulseDurationMs
                1f at PulseDurationMs + HoldDurationMs
            }
        ),
        label = "levyra-shimmer-progress"
    )
}

internal fun levyraPulseStrength(progress: Float): Float = 1f - abs(2f * progress.coerceIn(0f, 1f) - 1f)

fun Modifier.levyraShimmerSweep(
    progress: State<Float>?,
    accent: Color? = null
): Modifier {
    if (progress == null) return this
    val highlight = accent?.copy(alpha = 0.09f) ?: LevyraShimmerDefaults.Highlight
    return this.drawWithContent {
        drawContent()
        val strength = levyraPulseStrength(progress.value)
        if (strength > 0f) drawRect(color = highlight, alpha = strength)
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
