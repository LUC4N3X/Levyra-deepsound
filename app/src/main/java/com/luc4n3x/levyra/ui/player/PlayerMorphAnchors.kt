package com.luc4n3x.levyra.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import kotlin.math.PI
import kotlin.math.sin

enum class PlayerMorphSlot {
    Mini,
    Full
}

@Stable
class PlayerMorphAnchors {
    var miniBounds by mutableStateOf<Rect?>(null)
        private set
    var fullBounds by mutableStateOf<Rect?>(null)
        private set

    /** Stores only finite, measurable bounds so a transient invalid layout cannot poison the morph. */
    fun update(slot: PlayerMorphSlot, bounds: Rect) {
        if (!bounds.isUsableMorphRect()) return
        when (slot) {
            PlayerMorphSlot.Mini -> if (miniBounds != bounds) miniBounds = bounds
            PlayerMorphSlot.Full -> if (fullBounds != bounds) fullBounds = bounds
        }
    }

    /**
     * Resolves a reversible flight path between the mini and full artwork.
     *
     * A small lift and bloom around the middle give the cover a physical hand-off between the two
     * surfaces. Both effects are zero at the endpoints, so dragging back down is exactly symmetric.
     */
    fun resolve(fraction: Float): Rect? {
        val start = miniBounds ?: return null
        val end = fullBounds ?: return null
        val t = fraction.finiteOr(0f).coerceIn(0f, 1f)
        val base = lerpRect(start, end, t)
        val pulse = sin(PI.toFloat() * t).coerceAtLeast(0f)
        val bloom = 1f + 0.035f * pulse
        val liftPx = -end.height.coerceAtMost(720f) * 0.035f * pulse
        return base.scaledAroundCenter(bloom, liftPx)
    }
}

@Composable
fun rememberPlayerMorphAnchors(): PlayerMorphAnchors = remember { PlayerMorphAnchors() }

fun Modifier.playerMorphAnchor(
    anchors: PlayerMorphAnchors,
    slot: PlayerMorphSlot
): Modifier = this.onGloballyPositioned { coordinates ->
    if (coordinates.isAttached) {
        anchors.update(slot, coordinates.boundsInRoot())
    }
}

fun lerpRect(start: Rect, end: Rect, fraction: Float): Rect {
    val t = fraction.finiteOr(0f).coerceIn(0f, 1f)
    return Rect(
        left = start.left + (end.left - start.left) * t,
        top = start.top + (end.top - start.top) * t,
        right = start.right + (end.right - start.right) * t,
        bottom = start.bottom + (end.bottom - start.bottom) * t
    )
}

fun morphCornerRadius(startRadius: Float, endRadius: Float, fraction: Float): Float {
    val safeStart = startRadius.finiteOr(0f).coerceAtLeast(0f)
    val safeEnd = endRadius.finiteOr(safeStart).coerceAtLeast(0f)
    val t = playerMotionProgress(fraction)
    return safeStart + (safeEnd - safeStart) * t
}

private fun Rect.scaledAroundCenter(scale: Float, offsetY: Float): Rect {
    val safeScale = scale.finiteOr(1f).coerceAtLeast(0f)
    val safeOffset = offsetY.finiteOr(0f)
    val halfWidth = width * safeScale / 2f
    val halfHeight = height * safeScale / 2f
    val centerX = (left + right) / 2f
    val centerY = (top + bottom) / 2f + safeOffset
    return Rect(
        left = centerX - halfWidth,
        top = centerY - halfHeight,
        right = centerX + halfWidth,
        bottom = centerY + halfHeight
    )
}

private fun Rect.isUsableMorphRect(): Boolean =
    left.isFinite() && top.isFinite() && right.isFinite() && bottom.isFinite() &&
        width > 0f && height > 0f

private fun Float.finiteOr(fallback: Float): Float = if (isFinite()) this else fallback
