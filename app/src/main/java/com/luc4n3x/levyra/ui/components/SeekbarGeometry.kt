package com.luc4n3x.levyra.ui.components

internal fun seekbarFractionAt(x: Float, widthPx: Float): Float {
    if (!x.isFinite() || !widthPx.isFinite() || widthPx <= 0f) return 0f
    return (x / widthPx).coerceIn(0f, 1f)
}

internal fun seekbarProgressFraction(positionMs: Long, durationMs: Long): Float {
    if (durationMs <= 0L) return 0f
    return (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
}

internal fun seekbarHandleCenterX(fraction: Float, widthPx: Float, handleWidthPx: Float): Float {
    if (!fraction.isFinite() || !widthPx.isFinite() || !handleWidthPx.isFinite() || widthPx <= 0f) return 0f
    val half = (handleWidthPx.coerceAtLeast(0f) / 2f).coerceAtMost(widthPx / 2f)
    return (fraction.coerceIn(0f, 1f) * widthPx).coerceIn(half, widthPx - half)
}

internal fun seekbarTooltipOffsetX(fraction: Float, widthPx: Float, tooltipWidthPx: Float): Float {
    if (!fraction.isFinite() || !widthPx.isFinite() || !tooltipWidthPx.isFinite() || widthPx <= 0f) return 0f
    val safeTooltipWidth = tooltipWidthPx.coerceAtLeast(0f)
    if (safeTooltipWidth >= widthPx) return 0f
    val centered = fraction.coerceIn(0f, 1f) * widthPx - safeTooltipWidth / 2f
    return centered.coerceIn(0f, widthPx - safeTooltipWidth)
}

internal fun seekbarSeekMillis(fraction: Float, durationMs: Long): Long {
    if (!fraction.isFinite() || durationMs <= 0L) return 0L
    return (fraction.coerceIn(0f, 1f) * durationMs).toLong().coerceIn(0L, durationMs)
}
