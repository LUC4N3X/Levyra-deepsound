package com.luc4n3x.levyra.ui.player

import kotlin.math.abs

internal class PlayerHorizontalDragSession(
    private val rightToLeft: Boolean,
    private val minFlingDistancePx: Float,
    private val onEvent: (PlayerDragEvent) -> Unit
) {
    private var horizontalOffset = 0f

    fun drag(deltaX: Float, widthPx: Float) {
        horizontalOffset += deltaX
        onEvent(
            PlayerDragEvent.HorizontalOffset(
                playerSwipeContentOffset(horizontalOffset, widthPx.coerceAtLeast(1f))
            )
        )
    }

    fun settle(velocityX: Float, widthPx: Float) {
        val releaseVelocity = playerSwipeReleaseVelocity(
            offsetPx = horizontalOffset,
            velocityPx = velocityX,
            minFlingDistancePx = minFlingDistancePx
        )
        val result = resolvePlayerSwipe(
            horizontalOffset,
            releaseVelocity,
            widthPx.coerceAtLeast(1f)
        )
        onEvent(PlayerDragEvent.HorizontalSettled(mirroredPlayerSwipeResult(result, rightToLeft)))
    }

    fun reset() {
        horizontalOffset = 0f
    }
}

internal fun playerSwipeReleaseVelocity(
    offsetPx: Float,
    velocityPx: Float,
    minFlingDistancePx: Float
): Float = velocityPx
    .takeIf { offsetPx.isFinite() }
    ?.takeIf { it.isFinite() }
    ?.takeIf { minFlingDistancePx.isFinite() }
    ?.takeIf { abs(offsetPx) >= minFlingDistancePx.coerceAtLeast(0f) }
    ?.takeIf { (it < 0f) == (offsetPx < 0f) }
    ?: 0f

private fun mirroredPlayerSwipeResult(
    result: PlayerSwipeResult,
    rightToLeft: Boolean
): PlayerSwipeResult = if (!rightToLeft) {
    result
} else {
    when (result) {
        PlayerSwipeResult.Next -> PlayerSwipeResult.Previous
        PlayerSwipeResult.Previous -> PlayerSwipeResult.Next
        PlayerSwipeResult.Settle -> PlayerSwipeResult.Settle
    }
}
