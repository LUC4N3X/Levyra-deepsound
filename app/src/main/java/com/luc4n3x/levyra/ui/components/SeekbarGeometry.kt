package com.luc4n3x.levyra.ui.components

import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.sin

internal const val SeekbarWaveSampleStepPx = 3f

internal fun seekbarFractionAt(x: Float, widthPx: Float): Float {
    if (widthPx <= 0f) return 0f
    return (x / widthPx).coerceIn(0f, 1f)
}

internal fun seekbarProgressFraction(positionMs: Long, durationMs: Long): Float {
    if (durationMs <= 0L) return 0f
    return (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
}

internal fun seekbarHandleCenterX(fraction: Float, widthPx: Float, handleWidthPx: Float): Float {
    if (widthPx <= 0f) return 0f
    val half = (handleWidthPx / 2f).coerceAtMost(widthPx / 2f)
    return (fraction.coerceIn(0f, 1f) * widthPx).coerceIn(half, widthPx - half)
}

internal fun seekbarTooltipOffsetX(fraction: Float, widthPx: Float, tooltipWidthPx: Float): Float {
    if (widthPx <= 0f) return 0f
    if (tooltipWidthPx >= widthPx) return 0f
    val centered = fraction.coerceIn(0f, 1f) * widthPx - tooltipWidthPx / 2f
    return centered.coerceIn(0f, widthPx - tooltipWidthPx)
}

internal fun seekbarWaveSampleCount(widthPx: Float): Int {
    if (widthPx <= 0f) return 0
    return ceil(widthPx / SeekbarWaveSampleStepPx).toInt().coerceIn(1, 900)
}

internal fun seekbarWaveTaper(x: Float, activeWidthPx: Float, taperPx: Float): Float {
    if (activeWidthPx <= 0f || taperPx <= 0f) return 0f
    val fromStart = (x / taperPx).coerceIn(0f, 1f)
    val fromEnd = ((activeWidthPx - x) / taperPx).coerceIn(0f, 1f)
    return minOf(fromStart, fromEnd)
}

internal fun seekbarWaveOffset(
    x: Float,
    amplitudePx: Float,
    wavelengthPx: Float,
    phase: Float
): Float {
    if (amplitudePx <= 0f || wavelengthPx <= 0f) return 0f
    return amplitudePx * sin((2f * PI.toFloat() * x / wavelengthPx) + phase)
}

internal fun seekbarSeekMillis(fraction: Float, durationMs: Long): Long {
    if (durationMs <= 0L) return 0L
    return (fraction.coerceIn(0f, 1f) * durationMs).toLong().coerceIn(0L, durationMs)
}
