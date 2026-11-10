package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.ui.theme.LevyraIsIvory
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.theme.LevyraHomeDesign
import com.luc4n3x.levyra.ui.theme.LevyraIsMono
import com.luc4n3x.levyra.ui.theme.LocalLevyraVisualCapabilities
import kotlin.math.abs

private const val RailEdgeScale = 0.06f
private const val RailEdgeFade = 0.3f
private const val GlacierEdgeScale = 0.05f
private const val GlacierEdgeFade = 0.32f
private const val IvoryEdgeScale = 0.08f
private const val IvoryEdgeFade = 0.34f
private const val PagerScale = 0.05f
private const val PagerFade = 0.25f
private const val ParallaxZoom = 1.06f
private const val PagerTilt = 6f
private const val VideoEdgeScale = 0.06f
private const val VideoEdgeFade = 0.22f
private const val VideoFoldFade = 0.55f
private val RailSink = 6.dp
private val RailEasing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

@Composable
internal fun defaultRailMotionEnabled(): Boolean =
    defaultHomeOwnsCuratedPlaylists() && LocalAnimationsEnabled.current && LocalLevyraVisualCapabilities.current.decorativeMotion

internal fun Modifier.defaultRailItemMotion(enabled: Boolean, state: LazyListState, key: Any): Modifier =
    railEdgeMotion(enabled, state, key, RailEdgeScale, RailEdgeFade)

@Composable
internal fun glacierRailMotionEnabled(): Boolean =
    LevyraIsMono && LocalAnimationsEnabled.current && LocalLevyraVisualCapabilities.current.decorativeMotion

internal fun Modifier.glacierRailItemMotion(enabled: Boolean, state: LazyListState, key: Any): Modifier =
    railEdgeMotion(enabled, state, key, GlacierEdgeScale, GlacierEdgeFade)

@Composable
internal fun ivoryRailMotionEnabled(): Boolean =
    LevyraIsIvory && LocalAnimationsEnabled.current && LocalLevyraVisualCapabilities.current.decorativeMotion

internal fun Modifier.ivoryRailItemMotion(enabled: Boolean, state: LazyListState, key: Any): Modifier =
    railEdgeMotion(enabled, state, key, IvoryEdgeScale, IvoryEdgeFade)

private fun Modifier.railEdgeMotion(enabled: Boolean, state: LazyListState, key: Any, edgeScale: Float, edgeFade: Float): Modifier =
    if (!enabled) this else graphicsLayer {
        val info = state.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.key == key } ?: return@graphicsLayer
        if (item.size <= 0) return@graphicsLayer
        val start = info.viewportStartOffset
        val end = info.viewportEndOffset
        val visibleStart = maxOf(item.offset, start)
        val visibleEnd = minOf(item.offset + item.size, end)
        val visible = ((visibleEnd - visibleStart).toFloat() / item.size).coerceIn(0f, 1f)
        val hidden = RailEasing.transform(1f - visible)
        val scale = 1f - edgeScale * hidden
        scaleX = scale
        scaleY = scale
        alpha = 1f - edgeFade * hidden
        translationY = RailSink.toPx() * hidden
        transformOrigin = if (item.offset < start) TransformOrigin(1f, 1f) else TransformOrigin(0f, 1f)
    }

internal fun Modifier.defaultPagerPageMotion(enabled: Boolean, state: PagerState, page: Int): Modifier =
    if (!enabled) this else graphicsLayer {
        val offset = state.getOffsetDistanceInPages(page).coerceIn(-1f, 1f)
        val distance = RailEasing.transform(abs(offset))
        val scale = 1f - PagerScale * distance
        scaleX = scale
        scaleY = scale
        alpha = 1f - PagerFade * distance
        cameraDistance = 12f * density
        rotationY = -PagerTilt * offset
    }

internal fun Modifier.defaultRailParallax(enabled: Boolean, state: LazyListState, key: Any): Modifier =
    if (!enabled) this else graphicsLayer {
        val info = state.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.key == key } ?: return@graphicsLayer
        if (item.size <= 0) return@graphicsLayer
        val travel = (item.offset.toFloat() / item.size).coerceIn(-1f, 1f)
        scaleX = ParallaxZoom
        scaleY = ParallaxZoom
        translationX = -travel * size.width * (ParallaxZoom - 1f) / 2f
    }

internal fun Modifier.defaultVideoRailMotion(enabled: Boolean, state: LazyListState, key: Any, rtl: Boolean): Modifier =
    if (!enabled) this else graphicsLayer {
        val info = state.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.key == key } ?: return@graphicsLayer
        if (item.size <= 0) return@graphicsLayer
        val folded = videoFoldPixels(item.offset, item.size)
        if (folded > 0f) {
            val progress = RailEasing.transform(folded / item.size)
            val scale = 1f - VideoEdgeScale * progress
            translationX = if (rtl) -folded else folded
            scaleX = scale
            scaleY = scale
            alpha = 1f - VideoFoldFade * progress
            transformOrigin = TransformOrigin(if (rtl) 1f else 0f, 0.5f)
            clip = true
            shape = VideoFoldShape(folded, scale, LevyraHomeDesign.ArtworkCorner.toPx(), rtl)
        } else {
            val entering = item.offset + item.size - info.viewportEndOffset
            val hidden = RailEasing.transform((entering.toFloat() / item.size).coerceIn(0f, 1f))
            val scale = 1f - VideoEdgeScale * hidden
            scaleX = scale
            scaleY = scale
            alpha = 1f - VideoEdgeFade * hidden
            transformOrigin = TransformOrigin(if (rtl) 1f else 0f, 0.5f)
        }
    }

internal fun Modifier.defaultVideoArtworkMotion(enabled: Boolean, state: LazyListState, key: Any, rtl: Boolean): Modifier =
    if (!enabled) this else graphicsLayer {
        val info = state.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.key == key } ?: return@graphicsLayer
        val shift = videoFoldPixels(item.offset, item.size) / 2f
        translationX = if (rtl) shift else -shift
    }

internal fun videoFoldPixels(itemOffset: Int, itemSize: Int): Float =
    (-itemOffset).toFloat().coerceIn(0f, itemSize.toFloat())

private class VideoFoldShape(
    private val folded: Float,
    private val scale: Float,
    private val corner: Float,
    private val rtl: Boolean
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val visibleWidth = ((size.width - folded) / scale).coerceIn(0f, size.width)
        val left = if (rtl) size.width - visibleWidth else 0f
        return Outline.Rounded(RoundRect(left, 0f, left + visibleWidth, size.height, CornerRadius(corner)))
    }
}
