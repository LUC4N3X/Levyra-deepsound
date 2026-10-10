package com.luc4n3x.levyra.ui.player

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs

const val PlayerExpansionCollapsed: Float = 0f
const val PlayerExpansionExpanded: Float = 1f

/** Composes the full player invisibly for a frame so its first composition never eats the opening motion. */
const val PlayerPrewarmExpansion: Float = 0.001f

private const val OpenCommitFraction = 0.32f
private const val CloseCommitFraction = 0.72f
private const val CommitVelocity = 900f
private const val MorphStart = 0.01f
private const val MorphEnd = 0.96f
private const val ChromeFadeStart = 0.02f
private const val ChromeFadeEnd = 0.52f
private const val SurfaceFadeStart = 0f
private const val SurfaceFadeEnd = 0.2f
private const val BackgroundDepth = 0.028f
private const val PredictiveBackCollapseDepth = 0.14f
private const val ContainerFallbackHeightFraction = 0.12f

fun playerExpansionFromDrag(start: Float, dragPx: Float, travelPx: Float): Float {
    if (!start.isFinite()) return PlayerExpansionCollapsed
    val safeStart = start.coerceIn(PlayerExpansionCollapsed, PlayerExpansionExpanded)
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

fun playerContainerBounds(expansion: Float, origin: Rect?, width: Float, height: Float): Rect {
    val full = Rect(0f, 0f, width.coerceAtLeast(0f), height.coerceAtLeast(0f))
    val start = origin ?: Rect(0f, full.bottom * (1f - ContainerFallbackHeightFraction), full.right, full.bottom)
    return lerpRect(start, full, expansion.finiteOr(PlayerExpansionCollapsed))
}

fun playerContainerCornerPx(expansion: Float, origin: Rect?): Float {
    val startRadius = (origin?.height ?: 0f) / 2f
    return startRadius * (1f - expansion.finiteOr(PlayerExpansionCollapsed).coerceIn(0f, 1f))
}

fun Rect.placedInContainer(container: Rect, frameWidth: Float): Rect {
    if (!frameWidth.isFinite() || frameWidth <= 0f) return this
    val scale = container.width / frameWidth
    return Rect(
        left = container.left + left * scale,
        top = container.top + top * scale,
        right = container.left + right * scale,
        bottom = container.top + bottom * scale
    )
}

fun playerMorphActive(expansion: Float): Boolean {
    val safeExpansion = expansion.finiteOr(PlayerExpansionCollapsed)
    return safeExpansion > MorphStart && safeExpansion < PlayerExpansionExpanded
}

fun playerMorphFraction(expansion: Float): Float =
    playerMotionProgress(normalize(expansion, PlayerExpansionCollapsed, MorphEnd))

fun playerBackgroundScale(expansion: Float): Float =
    1f - playerMotionProgress(expansion) * BackgroundDepth

fun playerPredictiveBackExpansion(start: Float, progress: Float): Float {
    val safeStart = start.finiteOr(PlayerExpansionExpanded)
        .coerceIn(PlayerExpansionCollapsed, PlayerExpansionExpanded)
    val eased = playerMotionProgress(progress.finiteOr(0f).coerceIn(0f, 1f))
    return (safeStart * (1f - eased * PredictiveBackCollapseDepth))
        .coerceIn(PlayerExpansionCollapsed, PlayerExpansionExpanded)
}

private fun normalize(value: Float, start: Float, end: Float): Float {
    if (!value.isFinite() || !start.isFinite() || !end.isFinite() || end <= start) return 0f
    return ((value - start) / (end - start)).coerceIn(0f, 1f)
}

private fun Float.finiteOr(fallback: Float): Float = if (isFinite()) this else fallback

data class PlayerCollapseActions(
    val collapse: () -> Unit,
    val dragStart: () -> Unit,
    val drag: (Float) -> Unit,
    val dragEnd: (Float) -> Unit
)

data class PlayerContainerShape(val heightPx: Float, val cornerPx: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val height = heightPx.finiteOr(size.height).coerceIn(0f, size.height)
        val radius = cornerPx.finiteOr(0f).coerceIn(0f, minOf(size.width, height) / 2f)
        return Outline.Rounded(RoundRect(0f, 0f, size.width, height, CornerRadius(radius)))
    }
}
