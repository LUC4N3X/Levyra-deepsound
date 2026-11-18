package com.luc4n3x.levyra.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.abs

private const val ReleaseTiltDegrees = 12f
private const val ReleaseTiltShrink = 0.07f
private const val ReleaseTiltCamera = 16f

internal fun Modifier.releaseShelfTilt(state: LazyListState, key: Any): Modifier = graphicsLayer {
    val info = state.layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.key == key } ?: return@graphicsLayer
    val viewport = (info.viewportEndOffset - info.viewportStartOffset).coerceAtLeast(1)
    val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2f
    val fraction = ((item.offset + item.size / 2f - viewportCenter) / viewport).coerceIn(-1f, 1f)
    val scale = 1f - abs(fraction) * ReleaseTiltShrink
    rotationY = -fraction * ReleaseTiltDegrees
    scaleX = scale
    scaleY = scale
    transformOrigin = TransformOrigin(if (fraction > 0f) 0f else 1f, 0.5f)
    cameraDistance = ReleaseTiltCamera * density
}
