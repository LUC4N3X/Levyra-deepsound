package com.luc4n3x.levyra.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.math.PI
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
    isPlaying: Boolean = false,
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

    val trackScale = remember { Animatable(1f) }
    val handleScale = remember { Animatable(0f) }
    val waveAmplitude = remember { Animatable(0f) }

    val scrubSpec: AnimationSpec<Float> =
        if (animated) LevyraPlayerDesign.expressiveSpring() else snap()
    LaunchedEffect(isDragging, animated) {
        trackScale.animateTo(
            targetValue = if (isDragging) 1.35f else 1f,
            animationSpec = scrubSpec
        )
    }
    LaunchedEffect(isDragging, animated) {
        handleScale.animateTo(
            targetValue = if (isDragging) 1f else 0f,
            animationSpec = scrubSpec
        )
    }

    val waveActive = animated && isPlaying && !isDragging && durationMs > 0L
    LaunchedEffect(waveActive, animated) {
        waveAmplitude.animateTo(
            targetValue = if (waveActive) 1f else 0f,
            animationSpec = if (animated) LevyraPlayerDesign.smoothSpring() else snap()
        )
    }

    val wavePath = remember { Path() }
    val wavePhase = remember { Animatable(0f) }
    LaunchedEffect(waveActive) {
        if (!waveActive) return@LaunchedEffect
        wavePhase.snapTo(0f)
        wavePhase.animateTo(
            targetValue = -2f * PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(LevyraPlayerDesign.WaveCycleMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
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
                    .offset { IntOffset(offsetX.roundToInt(), with(density) { (-35).dp.roundToPx() }) }
                    .background(
                        LevyraPlayerDesign.ScrubTooltipSurface.copy(alpha = 0.96f),
                        LevyraPlayerDesign.ShapePill
                    )
                    .border(
                        width = LevyraPlayerDesign.Hairline,
                        color = activeColor.copy(alpha = 0.52f),
                        shape = LevyraPlayerDesign.ShapePill
                    )
                    .padding(horizontal = 13.dp, vertical = 6.dp),
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

            val baseTrackHeight = LevyraPlayerDesign.TrackHeight.toPx()
            val trackHeight = baseTrackHeight * trackScale.value
            val handleWidth = LevyraPlayerDesign.HandleWidth.toPx() +
                (LevyraPlayerDesign.HandleWidthActive - LevyraPlayerDesign.HandleWidth).toPx() * handleScale.value
            val handleHeight = LevyraPlayerDesign.HandleHeight.toPx() +
                (LevyraPlayerDesign.HandleHeightActive - LevyraPlayerDesign.HandleHeight).toPx() * handleScale.value
            val handleDiameter = minOf(handleWidth, handleHeight)
            val capInset = handleDiameter / 2f
            val trackStart = capInset
            val trackEnd = (totalWidth - capInset).coerceAtLeast(trackStart)
            val trackSpan = trackEnd - trackStart
            val handleX = trackStart + seekbarHandleCenterX(effectiveProgress, trackSpan, handleDiameter)
            val bufferedEnd = (trackStart + bufferedProgress * trackSpan).coerceIn(trackStart, trackEnd)
            val activeEnd = handleX.coerceIn(trackStart, trackEnd)
            val trackRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)

            drawRoundRect(
                color = inactiveColor,
                topLeft = Offset(trackStart, centerY - trackHeight / 2f),
                size = Size(trackSpan, trackHeight),
                cornerRadius = trackRadius
            )

            if (bufferedEnd > trackStart) {
                drawRoundRect(
                    color = thumbColor.copy(alpha = 0.20f),
                    topLeft = Offset(trackStart, centerY - trackHeight / 2f),
                    size = Size(bufferedEnd - trackStart, trackHeight),
                    cornerRadius = trackRadius
                )
            }

            val activeSpan = activeEnd - trackStart
            if (activeSpan > 0f) {
                val amplitudePx = LevyraPlayerDesign.WaveAmplitude.toPx() * waveAmplitude.value
                if (amplitudePx > 0.35f) {
                    val wavelength = LevyraPlayerDesign.WavePeriodDp.dp.toPx()
                    val taper = wavelength * 0.8f
                    val samples = seekbarWaveSampleCount(activeSpan)
                    val step = activeSpan / samples
                    wavePath.rewind()
                    var offsetX = 0f
                    var index = 0
                    while (index <= samples) {
                        val local = seekbarWaveTaper(offsetX, activeSpan, taper)
                        val y = centerY + seekbarWaveOffset(
                            x = offsetX,
                            amplitudePx = amplitudePx * local,
                            wavelengthPx = wavelength,
                            phase = wavePhase.value
                        )
                        val pointX = trackStart + offsetX
                        if (index == 0) wavePath.moveTo(pointX, y) else wavePath.lineTo(pointX, y)
                        offsetX += step
                        index += 1
                    }
                    drawPath(
                        path = wavePath,
                        color = trailingColor.copy(alpha = 0.20f),
                        style = Stroke(
                            width = trackHeight + 5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                    drawPath(
                        path = wavePath,
                        color = activeColor,
                        style = Stroke(
                            width = trackHeight,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                } else {
                    drawRoundRect(
                        color = activeColor,
                        topLeft = Offset(trackStart, centerY - trackHeight / 2f),
                        size = Size(activeSpan, trackHeight),
                        cornerRadius = trackRadius
                    )
                }
            }

            drawCircle(
                color = trailingColor.copy(alpha = 0.18f),
                radius = handleDiameter / 2f + 3.dp.toPx(),
                center = Offset(handleX, centerY)
            )
            drawCircle(
                color = thumbColor,
                radius = handleDiameter / 2f,
                center = Offset(handleX, centerY)
            )
            drawCircle(
                color = activeColor.copy(alpha = 0.58f),
                radius = handleDiameter / 2f,
                center = Offset(handleX, centerY),
                style = Stroke(width = LevyraPlayerDesign.Hairline.toPx())
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
