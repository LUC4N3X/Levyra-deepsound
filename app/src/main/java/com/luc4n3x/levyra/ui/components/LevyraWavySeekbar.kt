package com.luc4n3x.levyra.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

private val WavySeekbarHeight = 36.dp
private val WavyStroke = 4.dp
private val WavyAmplitude = 3.5.dp
private val WavyLength = 26.dp
private val WavyThumbWidth = 5.dp
private val WavyThumbHeight = 22.dp
private val WavyThumbGap = 6.dp
private const val WavyPhaseMillis = 1_400

@Composable
fun LevyraWavySeekbar(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    animated: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val latestOnSeek by rememberUpdatedState(onSeek)
    val playedFraction = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val fraction = dragFraction ?: playedFraction
    val waving = isPlaying && animated && dragFraction == null
    val amplitude by animateDpAsState(
        targetValue = if (waving) WavyAmplitude else 0.dp,
        animationSpec = spring(stiffness = 300f),
        label = "wavy-seekbar-amplitude"
    )
    val phaseState = if (waving) {
        rememberInfiniteTransition(label = "wavy-seekbar-phase").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(WavyPhaseMillis, easing = LinearEasing), RepeatMode.Restart),
            label = "wavy-seekbar-phase-value"
        )
    } else {
        null
    }
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(WavySeekbarHeight)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                setProgress { target ->
                    latestOnSeek(target.coerceIn(0f, 1f))
                    true
                }
            }
            .pointerInput(durationMs) {
                detectTapGestures { offset ->
                    if (durationMs > 0L) latestOnSeek((offset.x / size.width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(durationMs) {
                detectHorizontalDragGestures(
                    onDragStart = { offset -> dragFraction = (offset.x / size.width).coerceIn(0f, 1f) },
                    onDragEnd = {
                        dragFraction?.let { if (durationMs > 0L) latestOnSeek(it) }
                        dragFraction = null
                    },
                    onDragCancel = { dragFraction = null },
                    onHorizontalDrag = { change, _ ->
                        dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                    }
                )
            }
    ) {
        val centerY = size.height / 2f
        val stroke = WavyStroke.toPx()
        val thumbX = (size.width * fraction).coerceIn(0f, size.width)
        val gap = WavyThumbGap.toPx()
        val waveEnd = (thumbX - gap).coerceAtLeast(0f)
        val inactiveStart = (thumbX + gap).coerceAtMost(size.width)
        if (inactiveStart < size.width) {
            drawLine(
                color = inactiveColor,
                start = Offset(inactiveStart, centerY),
                end = Offset(size.width - stroke / 2f, centerY),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
        if (waveEnd > 0f) {
            val amp = amplitude.toPx()
            val wavelength = WavyLength.toPx()
            val shift = (phaseState?.value ?: 0f) * wavelength
            val path = Path()
            var x = stroke / 2f
            path.moveTo(x, centerY + amp * sin(((x + shift) / wavelength) * 2f * PI.toFloat()))
            while (x < waveEnd) {
                x = (x + 2f).coerceAtMost(waveEnd)
                path.lineTo(x, centerY + amp * sin(((x + shift) / wavelength) * 2f * PI.toFloat()))
            }
            drawPath(path, activeColor, style = Stroke(width = stroke, cap = StrokeCap.Round))
        }
        val thumbWidth = WavyThumbWidth.toPx()
        val thumbHeight = WavyThumbHeight.toPx()
        drawRoundRect(
            color = activeColor,
            topLeft = Offset(thumbX - thumbWidth / 2f, centerY - thumbHeight / 2f),
            size = Size(thumbWidth, thumbHeight),
            cornerRadius = CornerRadius(thumbWidth / 2f)
        )
    }
}
