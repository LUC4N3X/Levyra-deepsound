package com.luc4n3x.levyra.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import com.luc4n3x.levyra.ui.theme.LocalLevyraVisualCapabilities
import kotlin.math.abs

private const val RailEdgeScale = 0.06f
private const val RailEdgeFade = 0.3f
private const val PagerScale = 0.05f
private const val PagerFade = 0.25f
private const val ParallaxZoom = 1.06f

@Composable
internal fun defaultRailMotionEnabled(): Boolean =
    defaultHomeOwnsCuratedPlaylists() && LocalAnimationsEnabled.current && LocalLevyraVisualCapabilities.current.decorativeMotion

internal fun Modifier.defaultRailItemMotion(enabled: Boolean, state: LazyListState, key: Any): Modifier =
    if (!enabled) this else graphicsLayer {
        val info = state.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.key == key } ?: return@graphicsLayer
        if (item.size <= 0) return@graphicsLayer
        val start = info.viewportStartOffset
        val end = info.viewportEndOffset
        val visibleStart = maxOf(item.offset, start)
        val visibleEnd = minOf(item.offset + item.size, end)
        val visible = ((visibleEnd - visibleStart).toFloat() / item.size).coerceIn(0f, 1f)
        val hidden = 1f - visible
        val scale = 1f - RailEdgeScale * hidden
        scaleX = scale
        scaleY = scale
        alpha = 1f - RailEdgeFade * hidden
        transformOrigin = if (item.offset < start) TransformOrigin(1f, 0.5f) else TransformOrigin(0f, 0.5f)
    }

internal fun Modifier.defaultPagerPageMotion(enabled: Boolean, state: PagerState, page: Int): Modifier =
    if (!enabled) this else graphicsLayer {
        val distance = abs(state.getOffsetDistanceInPages(page)).coerceIn(0f, 1f)
        val scale = 1f - PagerScale * distance
        scaleX = scale
        scaleY = scale
        alpha = 1f - PagerFade * distance
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
