package com.luc4n3x.levyra.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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
    isPlaying: Boolean = true,
    animated: Boolean = true,
    contentDescription: String? = null
) {
    val density = LocalDensity.current
    val haptics = LocalLevyraHaptics.current

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

    val waveReveal = remember { Animatable(if (animated) 0f else 1f) }
    LaunchedEffect(animated) {
        if (animated) {
            waveReveal.snapTo(0f)
            waveReveal.animateTo(
                targetValue = 1f,
                animationSpec = LevyraPlayerDesign.emphasizedTween(420)
            )
        } else {
            waveReveal.snapTo(1f)
        }
    }

    val wavePhase = remember { Animatable(0f) }
    LaunchedEffect(animated, isPlaying, isDragging) {
        if (!animated || !isPlaying || isDragging) return@LaunchedEffect
        val fullPhase = 2f * PI.toFloat()
        while (true) {
            val remainingFraction = ((fullPhase - wavePhase.value) / fullPhase)
                .coerceIn(0.001f, 1f)
            wavePhase.animateTo(
                targetValue = fullPhase,
                animationSpec = tween(
                    durationMillis = (4_200f * remainingFraction).roundToInt().coerceAtLeast(1),
                    easing = LinearEasing
                )
            )
            wavePhase.snapTo(0f)
        }
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
                            haptics.perform(LevyraHapticAction.SeekSnap)
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
                            haptics.perform(LevyraHapticAction.SeekSnap)
                        },
                        onDragEnd = {
                            isDragging = false
                            haptics.perform(LevyraHapticAction.SeekSnap)
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

            drawRoundRect(
                color = inactiveColor,
                topLeft = Offset(trackStart, trackTop),
                size = Size(trackSpan, trackHeight),
                cornerRadius = radius
            )

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

            if (handleX > trackStart) {
                val activeSpan = handleX - trackStart
                clipRect(
                    left = trackStart,
                    top = 0f,
                    right = handleX,
                    bottom = size.height
                ) {
                    if (scrub > 0.001f) {
                        drawRoundRect(
                            color = activeColor.copy(alpha = activeColor.alpha * scrub),
                            topLeft = Offset(trackStart, trackTop),
                            size = Size(activeSpan, trackHeight),
                            cornerRadius = radius
                        )
                    }

                    val waveAlpha = (1f - scrub).coerceIn(0f, 1f)
                    if (waveAlpha > 0.001f) {
                        val targetWaveLength = 92.dp.toPx()
                        val waveCount = (activeSpan / targetWaveLength)
                            .roundToInt()
                            .coerceAtLeast(1)
                        val waveLength = activeSpan / waveCount
                        val minimumWaveSpan = 56.dp.toPx()
                        val amplitudeScale = (activeSpan / minimumWaveSpan).coerceIn(0f, 1f)
                        val waveHeight = 9.5.dp.toPx() * waveReveal.value * amplitudeScale
                        val baselineY = centerY + trackHeight / 2f
                        val topBaseY = centerY - trackHeight / 2f
                        val step = 2.dp.toPx().coerceAtLeast(1f)
                        val phaseOffset = if (animated && !isDragging) wavePhase.value else 0f
                        val edgeFeather = 18.dp.toPx().coerceAtMost(activeSpan / 2f)

                        val waveFill = Path().apply {
                            moveTo(trackStart, baselineY)
                            lineTo(trackStart, topBaseY)
                            var x = trackStart
                            while (x < handleX) {
                                val localX = x - trackStart
                                val phase = (localX / waveLength) * (2f * PI.toFloat()) + phaseOffset
                                val primaryCrest = (1f - cos(phase)) * 0.5f
                                val secondaryRipple = sin(phase * 2f) * 0.07f
                                val waterProfile = (primaryCrest + secondaryRipple).coerceIn(0f, 1f)
                                val edgeEnvelope = if (edgeFeather > 0f) {
                                    minOf(
                                        1f,
                                        localX / edgeFeather,
                                        (activeSpan - localX) / edgeFeather
                                    ).coerceIn(0f, 1f)
                                } else {
                                    1f
                                }
                                lineTo(
                                    x,
                                    topBaseY - waveHeight * waterProfile * edgeEnvelope
                                )
                                x += step
                            }
                            lineTo(handleX, topBaseY)
                            lineTo(handleX, baselineY)
                            close()
                        }

                        drawPath(
                            path = waveFill,
                            color = trailingColor.copy(alpha = trailingColor.alpha * waveAlpha * 0.14f)
                        )
                        drawPath(
                            path = waveFill,
                            color = activeColor.copy(alpha = activeColor.alpha * waveAlpha)
                        )
                    }
                }
            }

            if (scrub > 0.01f) {
                val glowRadius = thumbRadius * 1.8f
                drawCircle(
                    color = trailingColor.copy(alpha = 0.22f * scrub),
                    radius = glowRadius,
                    center = Offset(handleX, centerY)
                )
            }

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
