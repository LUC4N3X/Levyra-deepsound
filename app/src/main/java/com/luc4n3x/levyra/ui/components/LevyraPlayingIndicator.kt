package com.luc4n3x.levyra.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled

private const val BarCount = 4
private val BarDurations = intArrayOf(620, 480, 720, 540)
private val BarIdleHeights = floatArrayOf(0.42f, 0.72f, 0.30f, 0.58f)
private val BarMinimums = floatArrayOf(0.24f, 0.32f, 0.20f, 0.28f)

@Composable
fun LevyraPlayingIndicator(
    playing: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = 16.dp,
    barWidth: Dp = 2.5.dp,
    contentDescription: String? = null
) {
    val animationsEnabled = LocalAnimationsEnabled.current
    val bars = rememberPlayingIndicatorBars(playing && animationsEnabled)
    val semanticsModifier = if (contentDescription != null) {
        Modifier.clearAndSetSemantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }
    Canvas(modifier = modifier.size(size).then(semanticsModifier)) {
        val width = this.size.width
        val height = this.size.height
        if (width <= 0f || height <= 0f) return@Canvas
        val stroke = barWidth.toPx().coerceAtMost(width / BarCount)
        val gap = ((width - stroke * BarCount) / (BarCount - 1)).coerceAtLeast(0f)
        val radius = CornerRadius(stroke / 2f, stroke / 2f)
        for (index in 0 until BarCount) {
            val fraction = bars[index].value.coerceIn(0.08f, 1f)
            val barHeight = height * fraction
            drawRoundRect(
                color = color,
                topLeft = Offset(index * (stroke + gap), height - barHeight),
                size = Size(stroke, barHeight),
                cornerRadius = radius
            )
        }
    }
}

@Composable
private fun rememberPlayingIndicatorBars(animate: Boolean): List<State<Float>> {
    if (!animate) {
        return remember { List(BarCount) { index -> mutableFloatStateOf(BarIdleHeights[index]) } }
    }
    val transition = rememberInfiniteTransition(label = "levyra-playing")
    return List(BarCount) { index ->
        transition.animateFloat(
            initialValue = BarMinimums[index],
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = BarDurations[index]),
                repeatMode = RepeatMode.Reverse
            ),
            label = "levyra-playing-bar-$index"
        )
    }
}
