package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.theme.LevyraIsMono
import com.luc4n3x.levyra.ui.theme.LocalLevyraVisualCapabilities
import kotlin.math.abs

private const val RailEdgeScale = 0.06f
private const val RailEdgeFade = 0.3f
private const val GlacierEdgeScale = 0.05f
private const val GlacierEdgeFade = 0.32f
private const val PagerScale = 0.05f
private const val PagerFade = 0.25f
private const val ParallaxZoom = 1.06f
private const val PagerTilt = 6f
private const val VideoTilt = 9f
private const val VideoEdgeScale = 0.06f
private const val VideoEdgeFade = 0.22f
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

internal fun Modifier.defaultVideoRailMotion(enabled: Boolean, state: LazyListState, key: Any): Modifier =
    if (!enabled) this else graphicsLayer {
        val info = state.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.key == key } ?: return@graphicsLayer
        if (item.size <= 0) return@graphicsLayer
        val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2f
        val half = ((info.viewportEndOffset - info.viewportStartOffset) / 2f).coerceAtLeast(1f)
        val offset = ((item.offset + item.size / 2f - viewportCenter) / half).coerceIn(-1f, 1f)
        val focus = RailEasing.transform(abs(offset))
        val scale = 1f - VideoEdgeScale * focus
        scaleX = scale
        scaleY = scale
        alpha = 1f - VideoEdgeFade * focus
        cameraDistance = 14f * density
        rotationY = -VideoTilt * offset
        transformOrigin = TransformOrigin(if (offset < 0f) 1f else 0f, 0.5f)
    }
