package com.luc4n3x.levyra.ui.player

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker

sealed interface PlayerDragEvent {

    data class HorizontalOffset(val offsetPx: Float) : PlayerDragEvent

    data class HorizontalSettled(val result: PlayerSwipeResult) : PlayerDragEvent

    data class VerticalStart(val zone: PlayerGestureZone) : PlayerDragEvent

    data class VerticalDrag(
        val zone: PlayerGestureZone,
        val deltaPx: Float,
        val totalPx: Float,
        val heightPx: Float,
        val peeked: Boolean
    ) : PlayerDragEvent

    data class VerticalSettled(
        val zone: PlayerGestureZone,
        val totalPx: Float,
        val velocityPx: Float,
        val heightPx: Float,
        val peeked: Boolean
    ) : PlayerDragEvent

    data object Cancelled : PlayerDragEvent
}

fun Modifier.playerAxisDragGestures(
    key: Any?,
    enabled: Boolean,
    rightToLeft: Boolean,
    edgeZonesEnabled: Boolean,
    onEvent: (PlayerDragEvent) -> Unit
): Modifier = this.pointerInput(key, enabled, rightToLeft, edgeZonesEnabled) {
    if (!enabled) return@pointerInput
    var axis = PlayerDragAxis.Undecided
    var zone = PlayerGestureZone.Center
    var totalX = 0f
    var totalY = 0f
    var horizontalOffset = 0f
    var peeked = false
    val velocityTracker = VelocityTracker()
    detectDragGestures(
        onDragStart = { offset ->
            axis = PlayerDragAxis.Undecided
            totalX = 0f
            totalY = 0f
            horizontalOffset = 0f
            peeked = false
            velocityTracker.resetTracking()
            zone = if (edgeZonesEnabled) {
                playerGestureZone(offset.x / size.width.coerceAtLeast(1).toFloat(), rightToLeft)
            } else {
                PlayerGestureZone.Center
            }
        },
        onDrag = { change, dragAmount ->
            change.consume()
            velocityTracker.addPosition(change.uptimeMillis, change.position)
            totalX += dragAmount.x
            totalY += dragAmount.y
            if (axis == PlayerDragAxis.Undecided) {
                axis = resolvePlayerDragAxis(totalX, totalY)
                if (axis == PlayerDragAxis.Vertical) onEvent(PlayerDragEvent.VerticalStart(zone))
            }
            when (axis) {
                PlayerDragAxis.Horizontal -> {
                    horizontalOffset += dragAmount.x
                    onEvent(
                        PlayerDragEvent.HorizontalOffset(
                            playerSwipeContentOffset(
                                horizontalOffset,
                                size.width.coerceAtLeast(1).toFloat()
                            )
                        )
                    )
                }
                PlayerDragAxis.Vertical -> {
                    if (totalY < 0f) peeked = true
                    onEvent(
                        PlayerDragEvent.VerticalDrag(
                            zone = zone,
                            deltaPx = dragAmount.y,
                            totalPx = totalY,
                            heightPx = size.height.coerceAtLeast(1).toFloat(),
                            peeked = peeked
                        )
                    )
                }
                PlayerDragAxis.Undecided -> Unit
            }
        },
        onDragEnd = {
            val velocity = velocityTracker.calculateVelocity()
            when (axis) {
                PlayerDragAxis.Horizontal -> {
                    val result = resolvePlayerSwipe(
                        horizontalOffset,
                        velocity.x,
                        size.width.coerceAtLeast(1).toFloat()
                    )
                    onEvent(PlayerDragEvent.HorizontalSettled(mirrored(result, rightToLeft)))
                }
                PlayerDragAxis.Vertical -> onEvent(
                    PlayerDragEvent.VerticalSettled(
                        zone = zone,
                        totalPx = totalY,
                        velocityPx = velocity.y,
                        heightPx = size.height.coerceAtLeast(1).toFloat(),
                        peeked = peeked
                    )
                )
                PlayerDragAxis.Undecided -> Unit
            }
            axis = PlayerDragAxis.Undecided
        },
        onDragCancel = {
            onEvent(PlayerDragEvent.Cancelled)
            axis = PlayerDragAxis.Undecided
        }
    )
}

private fun mirrored(result: PlayerSwipeResult, rightToLeft: Boolean): PlayerSwipeResult {
    if (!rightToLeft) return result
    return when (result) {
        PlayerSwipeResult.Next -> PlayerSwipeResult.Previous
        PlayerSwipeResult.Previous -> PlayerSwipeResult.Next
        PlayerSwipeResult.Settle -> PlayerSwipeResult.Settle
    }
}
