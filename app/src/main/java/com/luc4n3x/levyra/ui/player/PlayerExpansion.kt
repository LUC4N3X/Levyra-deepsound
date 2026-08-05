package com.luc4n3x.levyra.ui.player

import kotlin.math.abs

const val PlayerExpansionCollapsed: Float = 0f
const val PlayerExpansionExpanded: Float = 1f

private const val OpenCommitFraction = 0.32f
private const val CloseCommitFraction = 0.72f
private const val CommitVelocity = 900f
private const val MorphStart = 0.02f
private const val MorphEnd = 0.86f

fun playerExpansionFromDrag(start: Float, dragPx: Float, travelPx: Float): Float {
    if (travelPx <= 0f) return start.coerceIn(PlayerExpansionCollapsed, PlayerExpansionExpanded)
    val delta = -dragPx / travelPx
    return (start + delta).coerceIn(PlayerExpansionCollapsed, PlayerExpansionExpanded)
}

fun resolvePlayerExpansionTarget(
    expansion: Float,
    velocityPx: Float,
    wasExpanded: Boolean
): Float {
    val current = expansion.coerceIn(PlayerExpansionCollapsed, PlayerExpansionExpanded)
    if (abs(velocityPx) >= CommitVelocity) {
        return if (velocityPx < 0f) PlayerExpansionExpanded else PlayerExpansionCollapsed
    }
    val threshold = if (wasExpanded) CloseCommitFraction else OpenCommitFraction
    return if (current >= threshold) PlayerExpansionExpanded else PlayerExpansionCollapsed
}

fun playerChromeAlpha(expansion: Float): Float =
    (1f - expansion.coerceIn(0f, 1f) * 1.6f).coerceIn(0f, 1f)

fun playerSurfaceAlpha(expansion: Float): Float =
    ((expansion.coerceIn(0f, 1f) - 0.08f) / 0.42f).coerceIn(0f, 1f)

fun playerMorphActive(expansion: Float): Boolean =
    expansion > MorphStart && expansion < PlayerExpansionExpanded

fun playerMorphFraction(expansion: Float): Float =
    (expansion.coerceIn(0f, 1f) / MorphEnd).coerceIn(0f, 1f)

fun playerBackgroundScale(expansion: Float): Float =
    1f - expansion.coerceIn(0f, 1f) * 0.06f
