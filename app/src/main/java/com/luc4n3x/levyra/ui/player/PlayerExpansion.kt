package com.luc4n3x.levyra.ui.player

import kotlin.math.abs

const val PlayerExpansionCollapsed: Float = 0f
const val PlayerExpansionExpanded: Float = 1f

private const val OpenCommitFraction = 0.32f
private const val CloseCommitFraction = 0.72f
private const val CommitVelocity = 900f
private const val MorphStart = 0.01f
private const val MorphEnd = 0.96f
private const val ChromeFadeStart = 0.05f
private const val ChromeFadeEnd = 0.72f
private const val SurfaceFadeStart = 0.015f
private const val SurfaceFadeEnd = 0.50f
private const val BackgroundDepth = 0.028f
private const val SurfaceScaleStart = 0.94f
private const val SurfaceLiftStart = 0.055f

fun playerExpansionFromDrag(start: Float, dragPx: Float, travelPx: Float): Float {
    val safeStart = start.finiteOr(PlayerExpansionCollapsed)
        .coerceIn(PlayerExpansionCollapsed, PlayerExpansionExpanded)
    if (!dragPx.isFinite() || !travelPx.isFinite() || travelPx <= 0f) return safeStart
    val delta = -dragPx / travelPx
    return (safeStart + delta).coerceIn(PlayerExpansionCollapsed, PlayerExpansionExpanded)
}

fun resolvePlayerExpansionTarget(
    expansion: Float,
    velocityPx: Float,
    wasExpanded: Boolean
): Float {
    val current = expansion.finiteOr(if (wasExpanded) PlayerExpansionExpanded else PlayerExpansionCollapsed)
        .coerceIn(PlayerExpansionCollapsed, PlayerExpansionExpanded)
    val safeVelocity = velocityPx.finiteOr(0f)
    if (abs(safeVelocity) >= CommitVelocity) {
        return if (safeVelocity < 0f) PlayerExpansionExpanded else PlayerExpansionCollapsed
    }
    val threshold = if (wasExpanded) CloseCommitFraction else OpenCommitFraction
    return if (current >= threshold) PlayerExpansionExpanded else PlayerExpansionCollapsed
}

/**
 * A symmetric smooth-step shared by every visible player transition layer.
 *
 * The drag remains reversible and tied to the finger, while the rendered layers accelerate and
 * settle together instead of looking like unrelated fades.
 */
fun playerMotionProgress(fraction: Float): Float {
    val t = fraction.finiteOr(0f).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

fun playerChromeAlpha(expansion: Float): Float =
    1f - playerMotionProgress(normalize(expansion, ChromeFadeStart, ChromeFadeEnd))

fun playerSurfaceAlpha(expansion: Float): Float =
    playerMotionProgress(normalize(expansion, SurfaceFadeStart, SurfaceFadeEnd))

/** Makes the full player grow out of the mini player rather than simply fading over it. */
fun playerSurfaceScale(expansion: Float): Float =
    SurfaceScaleStart + (1f - SurfaceScaleStart) * playerMotionProgress(expansion)

/** Downward offset, expressed as a fraction of the available travel, removed as the player opens. */
fun playerSurfaceLiftFraction(expansion: Float): Float {
    val remaining = 1f - playerMotionProgress(expansion)
    return remaining * remaining * SurfaceLiftStart
}

fun playerMorphActive(expansion: Float): Boolean {
    val safeExpansion = expansion.finiteOr(PlayerExpansionCollapsed)
    return safeExpansion > MorphStart && safeExpansion < PlayerExpansionExpanded
}

fun playerMorphFraction(expansion: Float): Float =
    playerMotionProgress(normalize(expansion, PlayerExpansionCollapsed, MorphEnd))

fun playerBackgroundScale(expansion: Float): Float =
    1f - playerMotionProgress(expansion) * BackgroundDepth

private fun normalize(value: Float, start: Float, end: Float): Float {
    if (!value.isFinite() || !start.isFinite() || !end.isFinite() || end <= start) return 0f
    return ((value - start) / (end - start)).coerceIn(0f, 1f)
}

private fun Float.finiteOr(fallback: Float): Float = if (isFinite()) this else fallback
