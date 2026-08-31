package com.luc4n3x.levyra.ui.player

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
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
): Modifier {
    if (!enabled) return this

    val transformModifier = if (edgeZonesEnabled) {
        Modifier.pointerInput(key, enabled) {
            PlayerVideoTransformController.bind(key)
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                do {
                    val event = awaitPointerEvent()
                    if (event.changes.count { it.pressed } >= 2) {
                        val zoomChange = event.calculateZoom()
                        val pan = event.calculatePan()
                        val centroid = event.calculateCentroid(useCurrent = true)
                        val hasTransform = zoomChange != 1f || pan != Offset.Zero
                        if (hasTransform && centroid.isSpecified) {
                            PlayerVideoTransformController.transform(
                                centroidX = centroid.x,
                                centroidY = centroid.y,
                                panX = pan.x,
                                panY = pan.y,
                                zoomChange = zoomChange
                            )
                            event.changes.forEach(PointerInputChange::consume)
                        }
                    }
                } while (event.changes.any { it.pressed })
            }
        }
    } else {
        Modifier
    }

    return this
        .then(transformModifier)
        .pointerInput(key, enabled, rightToLeft, edgeZonesEnabled) {
            val session = PlayerDragSession(
                rightToLeft = rightToLeft,
                edgeZonesEnabled = edgeZonesEnabled,
                onEvent = onEvent
            )
            var zoomPanning = false
            detectDragGestures(
                onDragStart = { offset ->
                    zoomPanning = edgeZonesEnabled && PlayerVideoTransformController.isZoomed
                    if (!zoomPanning) session.start(offset, size.width.toFloat())
                },
                onDrag = { change, dragAmount ->
                    if (edgeZonesEnabled && PlayerVideoTransformController.isZoomed) {
                        if (!zoomPanning) {
                            session.cancel()
                            zoomPanning = true
                        }
                        change.consume()
                        PlayerVideoTransformController.panBy(dragAmount.x, dragAmount.y)
                    } else {
                        session.drag(
                            change = change,
                            dragAmount = dragAmount,
                            widthPx = size.width.toFloat(),
                            heightPx = size.height.toFloat()
                        )
                    }
                },
                onDragEnd = {
                    if (!zoomPanning) {
                        session.end(
                            widthPx = size.width.toFloat(),
                            heightPx = size.height.toFloat()
                        )
                    }
                    zoomPanning = false
                },
                onDragCancel = {
                    if (!zoomPanning) session.cancel()
                    zoomPanning = false
                }
            )
        }
}

/** Owns one pointer gesture from press to settle so the Compose modifier stays declarative. */
private class PlayerDragSession(
    private val rightToLeft: Boolean,
    private val edgeZonesEnabled: Boolean,
    private val onEvent: (PlayerDragEvent) -> Unit
) {
    private var axis = PlayerDragAxis.Undecided
    private var zone = PlayerGestureZone.Center
    private var totalX = 0f
    private var totalY = 0f
    private var horizontalOffset = 0f
    private var peeked = false
    private val velocityTracker = VelocityTracker()

    fun start(offset: Offset, widthPx: Float) {
        resetMotion()
        velocityTracker.resetTracking()
        zone = resolveZone(offset.x, widthPx)
    }

    fun drag(
        change: PointerInputChange,
        dragAmount: Offset,
        widthPx: Float,
        heightPx: Float
    ) {
        change.consume()
        velocityTracker.addPosition(change.uptimeMillis, change.position)
        totalX += dragAmount.x
        totalY += dragAmount.y
        resolveAxisIfNeeded()
        dispatchDrag(dragAmount, widthPx, heightPx)
    }

    fun end(widthPx: Float, heightPx: Float) {
        val velocity = velocityTracker.calculateVelocity()
        when (axis) {
            PlayerDragAxis.Horizontal -> settleHorizontal(velocity.x, widthPx)
            PlayerDragAxis.Vertical -> settleVertical(velocity.y, heightPx)
            PlayerDragAxis.Undecided -> Unit
        }
        resetMotion()
    }

    fun cancel() {
        onEvent(PlayerDragEvent.Cancelled)
        resetMotion()
    }

    private fun resolveZone(offsetX: Float, widthPx: Float): PlayerGestureZone {
        if (!edgeZonesEnabled) return PlayerGestureZone.Center
        return playerGestureZone(offsetX / widthPx.coerceAtLeast(1f), rightToLeft)
    }

    private fun resolveAxisIfNeeded() {
        if (axis != PlayerDragAxis.Undecided) return
        axis = resolvePlayerDragAxis(totalX, totalY)
        if (axis == PlayerDragAxis.Vertical) {
            onEvent(PlayerDragEvent.VerticalStart(zone))
        }
    }

    private fun dispatchDrag(dragAmount: Offset, widthPx: Float, heightPx: Float) {
        when (axis) {
            PlayerDragAxis.Horizontal -> dispatchHorizontalDrag(dragAmount.x, widthPx)
            PlayerDragAxis.Vertical -> dispatchVerticalDrag(dragAmount.y, heightPx)
            PlayerDragAxis.Undecided -> Unit
        }
    }

    private fun dispatchHorizontalDrag(deltaX: Float, widthPx: Float) {
        horizontalOffset += deltaX
        onEvent(
            PlayerDragEvent.HorizontalOffset(
                playerSwipeContentOffset(horizontalOffset, widthPx.coerceAtLeast(1f))
            )
        )
    }

    private fun dispatchVerticalDrag(deltaY: Float, heightPx: Float) {
        if (totalY < 0f) peeked = true
        onEvent(
            PlayerDragEvent.VerticalDrag(
                zone = zone,
                deltaPx = deltaY,
                totalPx = totalY,
                heightPx = heightPx.coerceAtLeast(1f),
                peeked = peeked
            )
        )
    }

    private fun settleHorizontal(velocityX: Float, widthPx: Float) {
        val result = resolvePlayerSwipe(
            horizontalOffset,
            velocityX,
            widthPx.coerceAtLeast(1f)
        )
        onEvent(PlayerDragEvent.HorizontalSettled(mirrored(result, rightToLeft)))
    }

    private fun settleVertical(velocityY: Float, heightPx: Float) {
        onEvent(
            PlayerDragEvent.VerticalSettled(
                zone = zone,
                totalPx = totalY,
                velocityPx = velocityY,
                heightPx = heightPx.coerceAtLeast(1f),
                peeked = peeked
            )
        )
    }

    private fun resetMotion() {
        axis = PlayerDragAxis.Undecided
        zone = PlayerGestureZone.Center
        totalX = 0f
        totalY = 0f
        horizontalOffset = 0f
        peeked = false
    }
}

private fun mirrored(result: PlayerSwipeResult, rightToLeft: Boolean): PlayerSwipeResult {
    if (!rightToLeft) return result
    return when (result) {
        PlayerSwipeResult.Next -> PlayerSwipeResult.Previous
        PlayerSwipeResult.Previous -> PlayerSwipeResult.Next
        PlayerSwipeResult.Settle -> PlayerSwipeResult.Settle
    }
}
