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
import java.util.Locale

@Composable
fun PremiumSeekbar(
    positionMs: Long,
    durationMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
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

    val trackThicknessScale = remember { Animatable(1f) }
    val thumbScale = remember { Animatable(0.4f) }

    LaunchedEffect(isDragging) {
        trackThicknessScale.animateTo(
            targetValue = if (isDragging) 1.5f else 1f,
            animationSpec = spring(
                dampingRatio = 0.7f,
                stiffness = Spring.StiffnessMedium
            )
        )
    }
    
    LaunchedEffect(isDragging) {
        thumbScale.animateTo(
            targetValue = if (isDragging) 1f else 0.4f,
            animationSpec = spring(
                dampingRatio = 0.8f,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        val widthPx = with(density) { constraints.maxWidth.toDp().toPx() }
        val seekPosMs = (effectiveProgress * durationMs.coerceAtLeast(0L)).toLong()

        if (isDragging) {
            val tooltipHalfWidthPx = with(density) { 32.dp.toPx() }
                .coerceAtMost(widthPx / 2f)
            val tooltipMax = (widthPx - tooltipHalfWidthPx).coerceAtLeast(tooltipHalfWidthPx)
            val tooltipOffsetPx = (effectiveProgress * widthPx)
                .coerceIn(tooltipHalfWidthPx, tooltipMax)
            val tooltipOffsetDp = with(density) { (tooltipOffsetPx - tooltipHalfWidthPx).toDp() }

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
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
                    if (durationMs <= 0L) return@pointerInput
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
            val centerY = totalHeight / 2f
            
            val baseTrackHeight = 3.dp.toPx()
            val currentTrackHeight = baseTrackHeight * trackThicknessScale.value
            
            val activeWidth = totalWidth * effectiveProgress

            drawRoundRect(
                color = inactiveColor,
                topLeft = Offset(0f, centerY - currentTrackHeight / 2f),
                size = Size(totalWidth, currentTrackHeight),
                cornerRadius = CornerRadius(currentTrackHeight / 2f, currentTrackHeight / 2f)
            )

            val activeTrackHeight = currentTrackHeight * 1.15f
            
            drawRoundRect(
                color = activeColor.copy(alpha = 0.20f),
                topLeft = Offset(0f, centerY - activeTrackHeight * 1.5f),
                size = Size(activeWidth, activeTrackHeight * 3f),
                cornerRadius = CornerRadius(activeTrackHeight * 1.5f, activeTrackHeight * 1.5f)
            )

            drawRoundRect(
                color = activeColor,
                topLeft = Offset(0f, centerY - activeTrackHeight / 2f),
                size = Size(activeWidth, activeTrackHeight),
                cornerRadius = CornerRadius(activeTrackHeight / 2f, activeTrackHeight / 2f)
            )

            val thumbX = activeWidth.coerceIn(0f, totalWidth)
            val baseThumbRadius = 8.dp.toPx()
            val currentThumbRadius = baseThumbRadius * thumbScale.value

            if (currentThumbRadius > 0.5f) {
                drawCircle(
                    color = activeColor.copy(alpha = 0.35f),
                    radius = currentThumbRadius * 1.8f,
                    center = Offset(thumbX, centerY)
                )
                drawCircle(
                    color = thumbColor,
                    radius = currentThumbRadius,
                    center = Offset(thumbX, centerY)
                )
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}
