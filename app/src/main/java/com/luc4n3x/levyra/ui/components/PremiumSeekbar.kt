package com.luc4n3x.levyra.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun PremiumSeekbar(
    positionMs: Long,
    durationMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
    bufferedPositionMs: Long = 0L,
    activeColor: Color = LevyraCyan,
    trailingColor: Color = activeColor,
    inactiveColor: Color = LevyraMuted.copy(alpha = 0.35f),
    thumbColor: Color = Color.White,
    animated: Boolean = true,
    contentDescription: String? = null
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current

    var isDragging by remember { mutableStateOf(false) }
    var dragProgressFraction by remember { mutableFloatStateOf(0f) }
    var widthPx by remember { mutableFloatStateOf(0f) }

    val effectiveProgress = if (isDragging) {
        dragProgressFraction
    } else {
        seekbarProgressFraction(positionMs, durationMs)
    }
    val bufferedProgress = remember(bufferedPositionMs, durationMs, effectiveProgress) {
        seekbarProgressFraction(bufferedPositionMs, durationMs).coerceAtLeast(effectiveProgress)
    }

    val scrubAmount = remember { Animatable(0f) }
    val scrubSpec: AnimationSpec<Float> =
        if (animated) LevyraPlayerDesign.expressiveSpring() else snap()
    LaunchedEffect(isDragging, animated) {
        scrubAmount.animateTo(
            targetValue = if (isDragging) 1f else 0f,
            animationSpec = scrubSpec
        )
    }

    val scrubMillis by remember(durationMs) {
        derivedStateOf { seekbarSeekMillis(dragProgressFraction, durationMs) }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(LevyraPlayerDesign.MinimumTouchTarget)
            .onSizeChanged { widthPx = it.width.toFloat() },
        contentAlignment = Alignment.CenterStart
    ) {
        if (isDragging && widthPx > 0f) {
            val tooltipWidthPx = with(density) { 74.dp.toPx() }
            val offsetX = seekbarTooltipOffsetX(effectiveProgress, widthPx, tooltipWidthPx)
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), with(density) { (-34).dp.roundToPx() }) }
                    .background(Color(0xFF101014).copy(alpha = 0.94f), LevyraPlayerDesign.ShapeXs)
                    .border(
                        width = LevyraPlayerDesign.Hairline,
                        color = activeColor.copy(alpha = 0.55f),
                        shape = LevyraPlayerDesign.ShapeXs
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formatSeekbarMillis(scrubMillis),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(LevyraPlayerDesign.MinimumTouchTarget)
                .clipToBounds()
                .semantics {
                    contentDescription?.let { this.contentDescription = it }
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = effectiveProgress,
                        range = 0f..1f,
                        steps = 0
                    )
                    setProgress { targetValue ->
                        if (durationMs > 0L) {
                            onSeekTo(seekbarSeekMillis(targetValue, durationMs))
                            true
                        } else {
                            false
                        }
                    }
                }
                .pointerInput(durationMs) {
                    detectTapGestures { offset ->
                        if (durationMs > 0L && size.width > 0) {
                            val fraction = seekbarFractionAt(offset.x, size.width.toFloat())
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSeekTo(seekbarSeekMillis(fraction, durationMs))
                        }
                    }
                }
                .pointerInput(durationMs) {
                    if (durationMs <= 0L) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragProgressFraction = seekbarFractionAt(offset.x, size.width.toFloat())
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragEnd = {
                            isDragging = false
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSeekTo(seekbarSeekMillis(dragProgressFraction, durationMs))
                        },
                        onDragCancel = { isDragging = false },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            dragProgressFraction = seekbarFractionAt(change.position.x, size.width.toFloat())
                        }
                    )
                }
        ) {
            val totalWidth = size.width
            if (totalWidth <= 0f) return@Canvas
            val centerY = size.height / 2f
            val scrub = scrubAmount.value

            val trackHeight = LevyraPlayerDesign.TrackHeight.toPx() +
                (LevyraPlayerDesign.TrackHeightActive - LevyraPlayerDesign.TrackHeight).toPx() * scrub
            val thumbRadius = LevyraPlayerDesign.ThumbRadius.toPx() +
                (LevyraPlayerDesign.ThumbRadiusActive - LevyraPlayerDesign.ThumbRadius).toPx() * scrub
            val capInset = trackHeight / 2f
            val trackStart = capInset
            val trackEnd = (totalWidth - capInset).coerceAtLeast(trackStart)
            val trackSpan = trackEnd - trackStart
            val radius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
            val trackTop = centerY - trackHeight / 2f

            val handleX = trackStart + seekbarHandleCenterX(effectiveProgress, trackSpan, thumbRadius * 2f)

            // 1. Inactive continuous background track
            drawRoundRect(
                color = inactiveColor,
                topLeft = Offset(trackStart, trackTop),
                size = Size(trackSpan, trackHeight),
                cornerRadius = radius
            )

            // 2. Buffered progress span
            val bufferedEnd = (trackStart + bufferedProgress * trackSpan).coerceIn(trackStart, trackEnd)
            val bufferedSpan = bufferedEnd - trackStart
            if (bufferedSpan > 0f) {
                drawRoundRect(
                    color = LevyraPlayerDesign.TrackBuffered,
                    topLeft = Offset(trackStart, trackTop),
                    size = Size(bufferedSpan, trackHeight),
                    cornerRadius = radius
                )
            }

            // 3. Active continuous progress track
            val activeSpan = handleX - trackStart
            if (activeSpan > 0f) {
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(trackStart, trackTop),
                    size = Size(activeSpan, trackHeight),
                    cornerRadius = radius
                )
            }

            // 4. Thumb glow when scrubbing
            if (scrub > 0.01f) {
                val glowRadius = thumbRadius * 1.8f
                drawCircle(
                    color = trailingColor.copy(alpha = 0.22f * scrub),
                    radius = glowRadius,
                    center = Offset(handleX, centerY)
                )
            }

            // 5. Thumb circle with soft shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.20f),
                radius = thumbRadius + 1f,
                center = Offset(handleX, centerY + 1f)
            )
            drawCircle(
                color = thumbColor,
                radius = thumbRadius,
                center = Offset(handleX, centerY)
            )
        }
    }
}

internal fun formatSeekbarMillis(ms: Long): String {
    val totalSeconds = (ms / 1_000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}
