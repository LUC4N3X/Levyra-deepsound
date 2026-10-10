package com.luc4n3x.levyra.ui.player

import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

const val PLAYER_VIDEO_MIN_SCALE = 1f
const val PLAYER_VIDEO_MAX_SCALE = 5f
private const val PLAYER_VIDEO_SCALE_EPSILON = 0.02f

data class PlayerVideoTransform(
    val scale: Float = PLAYER_VIDEO_MIN_SCALE,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
) {
    val isZoomed: Boolean
        get() = scale > PLAYER_VIDEO_MIN_SCALE + PLAYER_VIDEO_SCALE_EPSILON

    companion object {
        val None = PlayerVideoTransform()
    }
}

fun boundedPlayerVideoScale(current: Float, zoomChange: Float): Float {
    if (!current.isFinite() || !zoomChange.isFinite() || zoomChange <= 0f) return PLAYER_VIDEO_MIN_SCALE
    return (current * zoomChange).coerceIn(PLAYER_VIDEO_MIN_SCALE, PLAYER_VIDEO_MAX_SCALE)
}

fun boundedPlayerVideoOffset(value: Float, dimensionPx: Float, scale: Float): Float {
    if (!value.isFinite() || !dimensionPx.isFinite() || dimensionPx <= 0f) return 0f
    if (scale <= PLAYER_VIDEO_MIN_SCALE) return 0f
    val maxOffset = dimensionPx * (scale - PLAYER_VIDEO_MIN_SCALE) / 2f
    return value.coerceIn(-maxOffset, maxOffset)
}

fun PlayerVideoTransform.applyPlayerVideoGesture(
    zoomChange: Float,
    pan: Offset,
    sizePx: Size
): PlayerVideoTransform {
    val nextScale = boundedPlayerVideoScale(scale, zoomChange)
    if (abs(nextScale - PLAYER_VIDEO_MIN_SCALE) <= PLAYER_VIDEO_SCALE_EPSILON) return PlayerVideoTransform.None
    val scaleRatio = if (scale > 0f) nextScale / scale else 1f
    return PlayerVideoTransform(
        scale = nextScale,
        offsetX = boundedPlayerVideoOffset(offsetX * scaleRatio + pan.x, sizePx.width, nextScale),
        offsetY = boundedPlayerVideoOffset(offsetY * scaleRatio + pan.y, sizePx.height, nextScale)
    )
}

fun Modifier.playerVideoZoomGestures(
    key: Any?,
    enabled: Boolean,
    transform: State<PlayerVideoTransform>,
    onTakeOver: () -> Unit,
    onTransform: (zoomChange: Float, pan: Offset, sizePx: Size) -> Unit
): Modifier = this.pointerInput(key, enabled) {
    if (!enabled) return@pointerInput
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var owning = transform.value.isZoomed
        if (owning) down.consume()
        var pastTouchSlop = owning
        var slopAccumulator = 0f
        val touchSlopPx = viewConfiguration.touchSlop
        val sizePx = Size(size.width.toFloat(), size.height.toFloat())
        do {
            val event = awaitPointerEvent()
            val pressed = event.changes.count { it.pressed }
            if (pressed == 0) break
            if (!owning && pressed > 1) {
                owning = true
                pastTouchSlop = false
                slopAccumulator = 0f
                onTakeOver()
            }
            if (!owning) continue

            val zoomChange = event.calculateZoom()
            val panChange = event.calculatePan()
            if (!pastTouchSlop) {
                val centroidSize = event.calculateCentroidSize(useCurrent = false)
                val zoomMotion = abs(1f - zoomChange) * centroidSize
                slopAccumulator += kotlin.math.hypot(panChange.x, panChange.y) + zoomMotion
                if (slopAccumulator > touchSlopPx) pastTouchSlop = true
            }
            if (pastTouchSlop && (zoomChange != 1f || panChange != Offset.Zero)) {
                onTransform(zoomChange, panChange, sizePx)
            }
            event.changes.forEach { change -> if (change.pressed) change.consume() }
        } while (true)
    }
}
