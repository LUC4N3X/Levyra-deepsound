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

/** Resolves the brightness, centre and volume zones, mirroring their meaning in RTL layouts. */
fun playerGestureZone(xFraction: Float, rightToLeft: Boolean = false): PlayerGestureZone {
    val clamped = xFraction.finiteOr(0.5f).coerceIn(0f, 1f)
    val normalized = if (rightToLeft) 1f - clamped else clamped
    return when {
        normalized <= PlayerEdgeZoneFraction -> PlayerGestureZone.BrightnessEdge
        normalized >= 1f - PlayerEdgeZoneFraction -> PlayerGestureZone.VolumeEdge
        else -> PlayerGestureZone.Center
    }
}

/** Locks a drag to its dominant axis only after the configured touch slop has been crossed. */
fun resolvePlayerDragAxis(
    totalX: Float,
    totalY: Float,
    slopPx: Float = PlayerDragSlopPx
): PlayerDragAxis {
    val horizontal = abs(totalX.finiteOr(0f))
    val vertical = abs(totalY.finiteOr(0f))
    val safeSlop = slopPx.finiteOr(PlayerDragSlopPx).coerceAtLeast(0f)
    if (horizontal < safeSlop && vertical < safeSlop) return PlayerDragAxis.Undecided
    return if (horizontal >= vertical) PlayerDragAxis.Horizontal else PlayerDragAxis.Vertical
}

/**
 * Resolves a horizontal track swipe.
 *
 * Once the distance threshold has been crossed, displacement owns the direction. This prevents a
 * small counter-flick while releasing from selecting the opposite track. Velocity is used only for
 * short flings that have not already committed by distance.
 */
fun resolvePlayerSwipe(offsetPx: Float, velocityPx: Float, widthPx: Float): PlayerSwipeResult {
    val safeOffset = offsetPx.finiteOr(0f)
    val safeVelocity = velocityPx.finiteOr(0f)
    val safeWidth = widthPx.finiteOr(1f).coerceAtLeast(1f)
    val distanceThreshold = safeWidth * SwipeDistanceFraction
    val distanceCommitted = abs(safeOffset) >= distanceThreshold
    val velocityCommitted = abs(safeVelocity) >= SwipeVelocity
    if (!distanceCommitted && !velocityCommitted) return PlayerSwipeResult.Settle

    val direction = if (distanceCommitted) safeOffset else safeVelocity
    return if (direction < 0f) PlayerSwipeResult.Next else PlayerSwipeResult.Previous
}

/** Requires an intentional downward mini-player gesture before dismissing playback. */
fun resolveMiniPlayerDismiss(offsetPx: Float, velocityPx: Float, heightPx: Float): PlayerVerticalResult {
    val safeOffset = offsetPx.finiteOr(0f)
    if (safeOffset <= 0f) return PlayerVerticalResult.Settle
    val safeVelocity = velocityPx.finiteOr(0f)
    val safeHeight = heightPx.finiteOr(1f).coerceAtLeast(1f)
    val distanceThreshold = safeHeight * MiniDismissDistanceFraction
    val committed = safeOffset >= distanceThreshold || safeVelocity >= MiniDismissVelocity
    return if (committed) PlayerVerticalResult.Collapse else PlayerVerticalResult.Settle
}

/** Maps a tap to the logical leading or trailing half of the artwork. */
fun playerTapSide(xFraction: Float): PlayerTapSide =
    if (xFraction.finiteOr(0.5f).coerceIn(0f, 1f) < 0.5f) {
        PlayerTapSide.Leading
    } else {
        PlayerTapSide.Trailing
    }

/** Returns a bounded seek delta, with logical direction mirrored for RTL layouts. */
fun playerSeekDeltaMs(side: PlayerTapSide, seekSeconds: Int, rightToLeft: Boolean = false): Long {
    val magnitude = seekSeconds.coerceIn(5, 30).toLong() * 1_000L
    val forward = side == PlayerTapSide.Trailing
    val directed = if (rightToLeft) !forward else forward
    return if (directed) magnitude else -magnitude
}

/** Keeps swiped content attached to the mini player instead of allowing it to leave the surface. */
fun playerSwipeContentOffset(offsetPx: Float, widthPx: Float): Float {
    val safeWidth = widthPx.finiteOr(1f).coerceAtLeast(1f)
    val limit = safeWidth * 0.34f
    return offsetPx.finiteOr(0f).coerceIn(-limit, limit)
}

/** Fades swiped content without making the active track disappear completely. */
fun playerSwipeContentAlpha(offsetPx: Float, widthPx: Float): Float {
    val safeWidth = widthPx.finiteOr(1f).coerceAtLeast(1f)
    val limit = safeWidth * 0.34f
    val ratio = (abs(offsetPx.finiteOr(0f)) / limit).coerceIn(0f, 1f)
    return 1f - ratio * 0.45f
}

private fun Float.finiteOr(fallback: Float): Float = if (isFinite()) this else fallback

data class PlayerGestureEnvironment(
    val activity: android.app.Activity?,
    val audioManager: android.media.AudioManager?,
    val brightnessLabel: String,
    val volumeLabel: String,
    val rightToLeft: Boolean
)

data class PlayerGestureConfig(
    val trackId: String,
    val settings: com.luc4n3x.levyra.domain.LevyraInterfaceSettings,
    val playbackSpeed: Float,
    val environment: PlayerGestureEnvironment
)

data class PlayerGestureMediaActions(
    val seekBy: (Long) -> Unit,
    val next: () -> Unit,
    val previous: () -> Unit,
    val swipeOffset: (Float) -> Unit,
    val temporarySpeed: (Float) -> Unit
)

data class PlayerGestureUiActions(
    val feedback: (String) -> Unit,
    val haptic: () -> Unit,
    val collapse: PlayerCollapseActions,
    val artworkPreview: (() -> Unit)? = null
)
