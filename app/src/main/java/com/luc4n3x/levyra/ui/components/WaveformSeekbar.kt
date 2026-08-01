package com.luc4n3x.levyra.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import kotlin.math.abs
import kotlin.math.sin

/**
 * Interactive Waveform Seekbar component with real-time waveform bars,
 * full accessibility semantics (TalkBack adjustable seekbar), and drag preview time tooltip.
 */
@Composable
fun WaveformSeekbar(
    positionMs: Long,
    durationMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
    waveformAmplitudes: FloatArray? = null,
    activeColor: Color = LevyraCyan,
    inactiveColor: Color = LevyraMuted.copy(alpha = 0.35f),
    thumbColor: Color = Color.White
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgressFraction by remember { mutableFloatStateOf(0f) }

    val effectiveProgress = if (isDragging) {
        dragProgressFraction
    } else {
        if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    }

    val numBars = 75
    val barProfile = remember(durationMs, waveformAmplitudes) {
        if (waveformAmplitudes != null && waveformAmplitudes.size >= numBars) {
            FloatArray(numBars) { i ->
                val idx = (i * waveformAmplitudes.size) / numBars
                waveformAmplitudes.getOrElse(idx) { 0.5f }.coerceIn(0.15f, 1f)
            }
        } else {
            // Generate deterministic waveform pattern for tracks without precomputed waveform data
            FloatArray(numBars) { i ->
                val seed = (durationMs % 10000 + i * 17).toDouble()
                val val1 = abs(sin(i * 0.22 + seed))
                val val2 = abs(sin(i * 0.55 + seed * 0.3))
                val amplitude = ((val1 + val2) / 2.0).toFloat().coerceIn(0.18f, 0.95f)
                amplitude
            }
        }
    }

    val thumbScale = remember { Animatable(1f) }

    LaunchedEffect(isDragging) {
        thumbScale.animateTo(
            targetValue = if (isDragging) 1.4f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(vertical = 4.dp)
    ) {
        val widthPx = with(density) { constraints.maxWidth.toDp().toPx() }

        val seekPosMs = (effectiveProgress * durationMs.coerceAtLeast(0L)).toLong()

        // Floating Tooltip on Drag
        if (isDragging) {
            val tooltipOffsetPx = (effectiveProgress * widthPx).coerceIn(40f, widthPx - 40f)
            val tooltipOffsetDp = with(density) { (tooltipOffsetPx - 32.dp.toPx()).toDp() }

            Box(
                modifier = Modifier
                    .offset { IntOffset(with(density) { tooltipOffsetDp.toPx().toInt() }, -36.dp.roundToPx()) }
                    .shadow(8.dp, RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E1E24), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formatMs(seekPosMs),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = effectiveProgress,
                        range = 0f..1f,
                        steps = 0
                    )
                    setProgress { targetValue ->
                        if (durationMs > 0L) {
                            onSeekTo((targetValue.coerceIn(0f, 1f) * durationMs).toLong())
                            true
                        } else {
                            false
                        }
                    }
                }
                .pointerInput(durationMs) {
                    detectTapGestures { offset ->
                        if (durationMs > 0L && widthPx > 0f) {
                            val targetFrac = (offset.x / widthPx).coerceIn(0f, 1f)
                            onSeekTo((targetFrac * durationMs).toLong())
                        }
                    }
                }
                .pointerInput(durationMs) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            if (widthPx > 0f) {
                                dragProgressFraction = (offset.x / widthPx).coerceIn(0f, 1f)
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            if (durationMs > 0L) {
                                onSeekTo((dragProgressFraction * durationMs).toLong())
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            if (widthPx > 0f) {
                                dragProgressFraction = (change.position.x / widthPx).coerceIn(0f, 1f)
                            }
                        }
                    )
                }
        ) {
            val totalWidth = size.width
            val totalHeight = size.height
            val barGap = 2.dp.toPx()
            val totalGaps = (numBars - 1) * barGap
            val barWidth = ((totalWidth - totalGaps) / numBars).coerceAtLeast(1f)
            val centerY = totalHeight / 2f

            val activeWidth = totalWidth * effectiveProgress

            for (i in 0 until numBars) {
                val barLeft = i * (barWidth + barGap)
                val barHeight = (barProfile[i] * totalHeight * 0.85f).coerceAtLeast(4.dp.toPx())
                val barTop = centerY - barHeight / 2f

                val isPlayed = (barLeft + barWidth / 2f) <= activeWidth
                val color = if (isPlayed) activeColor else inactiveColor

                drawRoundRect(
                    color = color,
                    topLeft = Offset(barLeft, barTop),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }

            // Scrubbing Thumb Line / Handle
            val thumbX = activeWidth.coerceIn(0f, totalWidth)
            val currentThumbScale = thumbScale.value

            drawCircle(
                color = activeColor.copy(alpha = 0.35f),
                radius = 12.dp.toPx() * currentThumbScale,
                center = Offset(thumbX, centerY)
            )
            drawCircle(
                color = thumbColor,
                radius = 6.dp.toPx() * currentThumbScale,
                center = Offset(thumbX, centerY)
            )
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return String.format("%02d:%02d", mins, secs)
}
