package com.luc4n3x.levyra.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import kotlin.math.PI
import kotlin.math.sin

private const val StageRevealStart = 0.70f
private const val StageRevealEnd = 0.86f
private const val StageHandOffEnd = 0.98f

enum class PlayerMorphSlot {
    Mini,
    Full,
    Stage
}

@Stable
class PlayerMorphAnchors(private val expansion: () -> Float = { PlayerExpansionExpanded }) {
    var miniBounds by mutableStateOf<Rect?>(null)
        private set
    var fullBounds by mutableStateOf<Rect?>(null)
        private set
    var stageBounds by mutableStateOf<Rect?>(null)
        private set
    var stageActive by mutableStateOf(false)
        private set
    private var fullRestScale by mutableFloatStateOf(1f)
    private var fullRestOffsetY by mutableFloatStateOf(0f)
    internal var playerContainer: LayoutCoordinates? = null

    val targetsStage: Boolean
        get() = stageActive && stageBounds != null

    val targetBounds: Rect?
        get() = if (targetsStage) {
            stageBounds
        } else {
            fullBounds?.scaledAroundCenter(fullRestScale, fullRestOffsetY)
        }

    /** Stores only finite, measurable bounds so a transient invalid layout cannot poison the morph. */
    fun update(slot: PlayerMorphSlot, bounds: Rect) {
        if (!bounds.isUsableMorphRect()) return
        when (slot) {
            PlayerMorphSlot.Mini -> if (miniBounds != bounds) miniBounds = bounds
            PlayerMorphSlot.Full -> if (fullBounds != bounds) fullBounds = bounds
            PlayerMorphSlot.Stage -> if (stageBounds != bounds) stageBounds = bounds
        }
    }

    /** The paused hero rests slightly smaller and lower; the flight has to land exactly there. */
    fun updateFullRest(scale: Float, offsetY: Float) {
        if (scale.isFinite() && scale > 0f) fullRestScale = scale
        if (offsetY.isFinite()) fullRestOffsetY = offsetY
    }

    fun updateStageActive(active: Boolean) {
        if (stageActive != active) stageActive = active
    }

    /** Alpha of the immersive stage while the flying cover is still on top of it. */
    fun stageRevealAlpha(): Float =
        playerMotionProgress(normalizeFraction(expansion(), StageRevealStart, StageRevealEnd))

    /** Alpha of the flying cover itself; it only dissolves when it lands on the full-bleed stage. */
    fun flightAlpha(): Float =
        if (targetsStage) 1f - playerMotionProgress(normalizeFraction(expansion(), StageRevealEnd, StageHandOffEnd)) else 1f

    /**
     * Resolves a reversible flight path between the mini artwork and the player's resting artwork.
     *
     * A small lift and bloom around the middle give the cover a physical hand-off between the two
     * surfaces. Both effects are zero at the endpoints, so dragging back down is exactly symmetric.
     */
    fun resolve(fraction: Float): Rect? {
        val start = miniBounds ?: return null
        val end = targetBounds ?: return null
        val t = fraction.finiteOr(0f).coerceIn(0f, 1f)
        val base = lerpRect(start, end, t)
        val pulse = sin(PI.toFloat() * t).coerceAtLeast(0f)
        val bloom = 1f + 0.035f * pulse
        val liftPx = -end.height.coerceAtMost(720f) * 0.035f * pulse
        return base.scaledAroundCenter(bloom, liftPx)
    }
}

@Composable
fun rememberPlayerMorphAnchors(expansion: () -> Float): PlayerMorphAnchors {
    val latestExpansion by rememberUpdatedState(expansion)
    return remember { PlayerMorphAnchors { latestExpansion() } }
}

/** Marks the full player's untransformed frame, so anchors inside it ignore its opening scale. */
fun Modifier.playerMorphContainer(anchors: PlayerMorphAnchors): Modifier =
    this.onPlaced { coordinates -> anchors.playerContainer = coordinates }

fun Modifier.playerMorphAnchor(
    anchors: PlayerMorphAnchors,
    slot: PlayerMorphSlot
): Modifier = this.onGloballyPositioned { coordinates ->
    if (!coordinates.isAttached) return@onGloballyPositioned
    val container = anchors.playerContainer?.takeIf { slot != PlayerMorphSlot.Mini && it.isAttached }
    val bounds = container?.localBoundingBoxOf(coordinates, clipBounds = false) ?: coordinates.boundsInRoot()
    anchors.update(slot, bounds)
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

private fun normalizeFraction(value: Float, start: Float, end: Float): Float {
    if (!value.isFinite() || end <= start) return 0f
    return ((value - start) / (end - start)).coerceIn(0f, 1f)
}

private fun Rect.isUsableMorphRect(): Boolean =
    left.isFinite() && top.isFinite() && right.isFinite() && bottom.isFinite() &&
        width > 0f && height > 0f

private fun Float.finiteOr(fallback: Float): Float = if (isFinite()) this else fallback
