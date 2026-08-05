package com.luc4n3x.levyra.ui.player

import kotlin.math.abs

enum class PlayerGestureZone {
    BrightnessEdge,
    Center,
    VolumeEdge
}

enum class PlayerDragAxis {
    Undecided,
    Horizontal,
    Vertical
}

enum class PlayerSwipeResult {
    Previous,
    Next,
    Settle
}

enum class PlayerVerticalResult {
    Collapse,
    Settle
}

enum class PlayerTapSide {
    Leading,
    Trailing
}

const val PlayerEdgeZoneFraction: Float = 0.18f
const val PlayerDragSlopPx: Float = 12f

private const val SwipeDistanceFraction = 0.24f
private const val SwipeVelocity = 620f
private const val MiniDismissDistanceFraction = 0.62f
private const val MiniDismissVelocity = 1_100f

fun playerGestureZone(xFraction: Float, rightToLeft: Boolean = false): PlayerGestureZone {
    val clamped = xFraction.coerceIn(0f, 1f)
    val normalized = if (rightToLeft) 1f - clamped else clamped
    return when {
        normalized <= PlayerEdgeZoneFraction -> PlayerGestureZone.BrightnessEdge
        normalized >= 1f - PlayerEdgeZoneFraction -> PlayerGestureZone.VolumeEdge
        else -> PlayerGestureZone.Center
    }
}

fun resolvePlayerDragAxis(
    totalX: Float,
    totalY: Float,
    slopPx: Float = PlayerDragSlopPx
): PlayerDragAxis {
    val horizontal = abs(totalX)
    val vertical = abs(totalY)
    if (horizontal < slopPx && vertical < slopPx) return PlayerDragAxis.Undecided
    return if (horizontal >= vertical) PlayerDragAxis.Horizontal else PlayerDragAxis.Vertical
}

fun resolvePlayerSwipe(offsetPx: Float, velocityPx: Float, widthPx: Float): PlayerSwipeResult {
    val distanceThreshold = widthPx.coerceAtLeast(1f) * SwipeDistanceFraction
    val committed = abs(offsetPx) >= distanceThreshold || abs(velocityPx) >= SwipeVelocity
    if (!committed) return PlayerSwipeResult.Settle
    val direction = if (abs(velocityPx) >= SwipeVelocity) velocityPx else offsetPx
    return if (direction < 0f) PlayerSwipeResult.Next else PlayerSwipeResult.Previous
}

fun resolveMiniPlayerDismiss(offsetPx: Float, velocityPx: Float, heightPx: Float): PlayerVerticalResult {
    if (offsetPx <= 0f) return PlayerVerticalResult.Settle
    val distanceThreshold = heightPx.coerceAtLeast(1f) * MiniDismissDistanceFraction
    val committed = offsetPx >= distanceThreshold || velocityPx >= MiniDismissVelocity
    return if (committed) PlayerVerticalResult.Collapse else PlayerVerticalResult.Settle
}

fun playerTapSide(xFraction: Float): PlayerTapSide =
    if (xFraction.coerceIn(0f, 1f) < 0.5f) PlayerTapSide.Leading else PlayerTapSide.Trailing

fun playerSeekDeltaMs(side: PlayerTapSide, seekSeconds: Int, rightToLeft: Boolean = false): Long {
    val magnitude = seekSeconds.coerceIn(5, 30).toLong() * 1_000L
    val forward = side == PlayerTapSide.Trailing
    val directed = if (rightToLeft) !forward else forward
    return if (directed) magnitude else -magnitude
}

fun playerSwipeContentOffset(offsetPx: Float, widthPx: Float): Float {
    val limit = widthPx.coerceAtLeast(1f) * 0.34f
    return offsetPx.coerceIn(-limit, limit)
}

fun playerSwipeContentAlpha(offsetPx: Float, widthPx: Float): Float {
    val limit = widthPx.coerceAtLeast(1f) * 0.34f
    val ratio = (abs(offsetPx) / limit).coerceIn(0f, 1f)
    return 1f - ratio * 0.45f
}
