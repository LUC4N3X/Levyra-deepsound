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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
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
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import com.luc4n3x.levyra.ui.theme.LocalLevyraVisualCapabilities
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val WaveCrestLift = 0.18f
private val WaveCrestHeight = 9.5.dp
private const val ThumbHaloAlpha = 0.42f
private val ThumbHaloWidth = 1.5.dp

@Suppress("CognitiveComplexMethod")
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
    playbackSpeed: Float = 1f,
    animated: Boolean = true,
    motionActive: Boolean = true,
    contentDescription: String? = null,
    waveform: FloatArray? = null,
    interactionKey: Any? = null
) {
    val density = LocalDensity.current
    val haptics = LocalLevyraHaptics.current
    val decorativeMotion = LocalLevyraVisualCapabilities.current.decorativeMotion
    val measuredWaveform = waveform?.takeIf { values ->
        values.isNotEmpty() && values.all { it.isFinite() }
    }

    var isDragging by remember(interactionKey) { mutableStateOf(false) }
    var pendingSeekFraction by remember(interactionKey) { mutableStateOf<Float?>(null) }
    var dragProgressFraction by remember(interactionKey) {
        mutableFloatStateOf(seekbarProgressFraction(positionMs, durationMs))
    }
    var widthPx by remember { mutableFloatStateOf(0f) }

    val effectiveProgress = if (isDragging) {
        dragProgressFraction
    } else {
        seekbarProgressFraction(positionMs, durationMs)
    }
    val playbackProgress = seekbarProgressFraction(positionMs, durationMs)
    val animatedProgress = remember(interactionKey) { Animatable(playbackProgress) }
    LaunchedEffect(
        playbackProgress,
        durationMs,
        playbackSpeed,
        animated,
        isPlaying,
        isDragging,
        pendingSeekFraction,
        interactionKey
    ) {
        val pendingSeek = pendingSeekFraction
        if (
            isDragging ||
            !seekbarShouldSmoothProgress(
                currentFraction = animatedProgress.value,
                targetFraction = playbackProgress,
                isPlaying = isPlaying,
                animated = animated,
                seekOrDiscontinuity = pendingSeek != null
            )
        ) {
            animatedProgress.snapTo(playbackProgress)
            if (pendingSeek != null && abs(playbackProgress - pendingSeek) <= 0.001f) {
                pendingSeekFraction = null
            }
        } else {
            animatedProgress.animateTo(
                targetValue = playbackProgress,
                animationSpec = tween(
                    durationMillis = seekbarProgressAnimationDurationMs(
                        animatedProgress.value,
                        playbackProgress,
                        durationMs,
                        playbackSpeed
                    ),
                    easing = LinearEasing
                )
            )
        }
    }
    val bufferedProgress = remember(bufferedPositionMs, durationMs, effectiveProgress) {
        seekbarProgressFraction(bufferedPositionMs, durationMs).coerceAtLeast(effectiveProgress)
    }

    val scrubAmount = remember { Animatable(0f) }
    val waveIntensity = remember { Animatable(if (animated && isPlaying) 1f else 0f) }
    LaunchedEffect(animated, isPlaying, isDragging, interactionKey) {
        waveIntensity.animateTo(
            targetValue = if (animated && isPlaying && !isDragging) 1f else 0f,
            animationSpec = if (animated) tween(180) else snap()
        )
    }
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
    val waveMotion = measuredWaveform == null &&
        animated &&
        decorativeMotion &&
        motionActive &&
        isPlaying &&
        !isDragging
    LaunchedEffect(waveMotion, interactionKey) {
        if (!waveMotion) return@LaunchedEffect
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

    val waveCrestColor = remember(activeColor) { lerp(activeColor, Color.White, WaveCrestLift).copy(alpha = 1f) }
    val waveBrushCache = remember { WaveBrushCache() }

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
                    .offset { IntOffset(offsetX.roundToInt(), with(density) { -34.dp.roundToPx() }) }
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
                            val targetFraction = targetValue.coerceIn(0f, 1f)
                            pendingSeekFraction = targetFraction
                            onSeekTo(seekbarSeekMillis(targetFraction, durationMs))
                            true
                        } else {
                            false
                        }
                    }
                }
                .pointerInput(durationMs, interactionKey) {
                    detectTapGestures { offset ->
                        if (durationMs > 0L && size.width > 0) {
                            val fraction = seekbarFractionAt(offset.x, size.width.toFloat())
                            pendingSeekFraction = fraction
                            haptics.perform(LevyraHapticAction.SeekSnap)
                            onSeekTo(seekbarSeekMillis(fraction, durationMs))
                        }
                    }
                }
                .pointerInput(durationMs, interactionKey) {
                    if (durationMs <= 0L) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragProgressFraction = seekbarFractionAt(offset.x, size.width.toFloat())
                            haptics.perform(LevyraHapticAction.SeekSnap)
                        },
                        onDragEnd = {
                            pendingSeekFraction = dragProgressFraction
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
            val renderedProgress = if (isDragging) dragProgressFraction else animatedProgress.value
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

            val handleX = trackStart + seekbarHandleCenterX(renderedProgress, trackSpan, thumbRadius * 2f)

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

            if (measuredWaveform != null) {
                drawMeasuredWaveform(
                    waveform = measuredWaveform,
                    trackStart = trackStart,
                    trackSpan = trackSpan,
                    centerY = centerY,
                    trackHeight = trackHeight,
                    handleX = handleX,
                    inactiveColor = inactiveColor,
                    activeColor = activeColor,
                    waveIntensity = waveIntensity.value
                )
            } else if (handleX > trackStart) {
                drawAnimatedWaveform(
                    trackStart = trackStart,
                    handleX = handleX,
                    trackHeight = trackHeight,
                    scrub = scrub,
                    animated = animated,
                    isDragging = isDragging,
                    waveReveal = waveReveal.value,
                    wavePhase = wavePhase.value,
                    trailingColor = trailingColor,
                    activeColor = activeColor,
                    waveBrush = waveBrushCache.brush(
                        crest = waveCrestColor,
                        base = activeColor.copy(alpha = 1f),
                        top = centerY - trackHeight / 2f - WaveCrestHeight.toPx(),
                        bottom = centerY + trackHeight / 2f
                    ),
                    waveIntensity = waveIntensity.value
                )
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
                color = activeColor.copy(alpha = activeColor.alpha * ThumbHaloAlpha),
                radius = thumbRadius + ThumbHaloWidth.toPx(),
                center = Offset(handleX, centerY)
            )
            drawCircle(
                color = thumbColor,
                radius = thumbRadius,
                center = Offset(handleX, centerY)
            )
        }
    }
}

private class WaveBrushCache {
    private var crest = Color.Unspecified
    private var base = Color.Unspecified
    private var top = Float.NaN
    private var bottom = Float.NaN
    private var cached: Brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))

    fun brush(crest: Color, base: Color, top: Float, bottom: Float): Brush {
        if (crest != this.crest || base != this.base || top != this.top || bottom != this.bottom) {
            this.crest = crest
            this.base = base
            this.top = top
            this.bottom = bottom
            cached = Brush.verticalGradient(
                colors = listOf(crest, base),
                startY = top,
                endY = bottom
            )
        }
        return cached
    }
}

private fun DrawScope.drawMeasuredWaveform(
    waveform: FloatArray,
    trackStart: Float,
    trackSpan: Float,
    centerY: Float,
    trackHeight: Float,
    handleX: Float,
    inactiveColor: Color,
    activeColor: Color,
    waveIntensity: Float
) {
    val slotWidth = trackSpan / waveform.size
    val barWidth = minOf(2.dp.toPx(), slotWidth * 0.58f).coerceAtLeast(1f)
    val minimumHalfHeight = trackHeight * 0.72f
    val maximumHalfHeight = 10.dp.toPx()

    fun drawBars(color: Color) {
        waveform.forEachIndexed { index, rawAmplitude ->
            val amplitude = rawAmplitude.coerceIn(0f, 1f)
            val expandedHalfHeight = minimumHalfHeight +
                (maximumHalfHeight - minimumHalfHeight) * amplitude
            val halfHeight = trackHeight / 2f +
                (expandedHalfHeight - trackHeight / 2f) * waveIntensity.coerceIn(0f, 1f)
            val x = trackStart + (index + 0.5f) * slotWidth
            drawRoundRect(
                color = color,
                topLeft = Offset(x - barWidth / 2f, centerY - halfHeight),
                size = Size(barWidth, halfHeight * 2f),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }

    drawBars(inactiveColor.copy(alpha = maxOf(inactiveColor.alpha, 0.46f)))
    if (handleX > trackStart) {
        clipRect(
            left = trackStart,
            top = 0f,
            right = handleX,
            bottom = size.height
        ) {
            drawBars(activeColor)
        }
    }
}

private fun DrawScope.drawAnimatedWaveform(
    trackStart: Float,
    handleX: Float,
    trackHeight: Float,
    scrub: Float,
    animated: Boolean,
    isDragging: Boolean,
    waveReveal: Float,
    wavePhase: Float,
    trailingColor: Color,
    activeColor: Color,
    waveBrush: Brush,
    waveIntensity: Float
) {
    val centerY = size.height / 2f
    val radius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
    val activeSpan = handleX - trackStart
    if (waveIntensity <= 0.001f) {
        drawRoundRect(
            color = activeColor,
            topLeft = Offset(trackStart, centerY - trackHeight / 2f),
            size = Size(activeSpan, trackHeight),
            cornerRadius = radius
        )
        return
    }
    clipRect(
        left = trackStart,
        top = 0f,
        right = handleX,
        bottom = size.height
    ) {
        if (scrub > 0.001f) {
            drawRoundRect(
                color = activeColor.copy(alpha = activeColor.alpha * scrub),
                topLeft = Offset(trackStart, centerY - trackHeight / 2f),
                size = Size(activeSpan, trackHeight),
                cornerRadius = radius
            )
        }

        val waveAlpha = (1f - scrub).coerceIn(0f, 1f)
        if (waveAlpha <= 0.001f) return@clipRect

        val waveLength = 92.dp.toPx()
        val minimumWaveSpan = 56.dp.toPx()
        val amplitudeScale = (activeSpan / minimumWaveSpan).coerceIn(0f, 1f)
        val waveHeight = 9.5.dp.toPx() * waveReveal * amplitudeScale * waveIntensity.coerceIn(0f, 1f)
        val baselineY = centerY + trackHeight / 2f
        val topBaseY = centerY - trackHeight / 2f
        val step = 2.dp.toPx().coerceAtLeast(1f)
        val phaseOffset = if (animated && !isDragging) wavePhase else 0f
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
            brush = waveBrush,
            alpha = activeColor.alpha * waveAlpha
        )
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
