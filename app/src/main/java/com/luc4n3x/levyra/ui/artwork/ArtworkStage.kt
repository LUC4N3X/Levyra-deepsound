package com.luc4n3x.levyra.ui.artwork

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import coil3.compose.AsyncImage
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.luc4n3x.levyra.data.LevyraArtworkCache
import kotlinx.coroutines.delay

internal enum class ArtworkDissolveEdge {
    Bottom,
    End
}

private const val ARTWORK_DISSOLVE_STEPS = 8

internal fun artworkDissolveStops(fadeFraction: Float): Array<Pair<Float, Color>> {
    val fade = fadeFraction.coerceIn(0.05f, 1f)
    val start = 1f - fade
    return Array(ARTWORK_DISSOLVE_STEPS + 2) { index ->
        if (index == 0) {
            0f to Color.Black
        } else {
            val step = (index - 1).toFloat() / ARTWORK_DISSOLVE_STEPS
            val eased = step * step * (3f - 2f * step)
            start + fade * step to Color.Black.copy(alpha = 1f - eased)
        }
    }
}

internal fun Modifier.artworkDissolve(
    edge: ArtworkDissolveEdge,
    fadeFraction: Float,
    sideFadeFraction: Float = 0f
): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithCache {
        val stops = artworkDissolveStops(fadeFraction)
        val rtl = layoutDirection == LayoutDirection.Rtl
        val edgeMask = when (edge) {
            ArtworkDissolveEdge.Bottom -> Brush.verticalGradient(
                colorStops = stops,
                startY = 0f,
                endY = size.height
            )
            ArtworkDissolveEdge.End -> Brush.horizontalGradient(
                colorStops = stops,
                startX = if (rtl) size.width else 0f,
                endX = if (rtl) 0f else size.width
            )
        }
        val sideMask = if (sideFadeFraction > 0f) {
            val side = sideFadeFraction.coerceIn(0.01f, 0.5f)
            Brush.horizontalGradient(
                0f to Color.Transparent,
                side to Color.Black,
                1f - side to Color.Black,
                1f to Color.Transparent
            )
        } else {
            null
        }
        onDrawWithContent {
            drawContent()
            drawRect(brush = edgeMask, blendMode = BlendMode.DstIn)
            if (sideMask != null) drawRect(brush = sideMask, blendMode = BlendMode.DstIn)
        }
    }

private class SeamlessArtworkMemory {
    var lastKey: MemoryCache.Key? = null
}

private enum class SeamlessArtworkPhase {
    Loading,
    Ready,
    Stalled,
    Failed
}

private const val SEAMLESS_ARTWORK_CROSSFADE_MS = 280
private const val SEAMLESS_ARTWORK_BRIDGE_MS = 900L
private const val SEAMLESS_ARTWORK_FALLBACK_FADE_MS = 220

@Composable
internal fun SeamlessArtworkImage(
    url: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    fallback: @Composable () -> Unit
) {
    if (url.isBlank()) {
        Box(modifier = modifier) { fallback() }
        return
    }
    val context = LocalContext.current
    val memory = remember { SeamlessArtworkMemory() }
    var phase by remember(url) { mutableStateOf(SeamlessArtworkPhase.Loading) }
    val request = remember(context, url) {
        ImageRequest.Builder(context)
            .data(LevyraArtworkCache.large(url))
            .placeholderMemoryCacheKey(memory.lastKey)
            .crossfade(SEAMLESS_ARTWORK_CROSSFADE_MS)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
    }
    LaunchedEffect(url) {
        delay(SEAMLESS_ARTWORK_BRIDGE_MS)
        if (phase == SeamlessArtworkPhase.Loading) phase = SeamlessArtworkPhase.Stalled
    }
    val fallbackVisible = phase == SeamlessArtworkPhase.Stalled || phase == SeamlessArtworkPhase.Failed
    val fallbackAlpha by animateFloatAsState(
        targetValue = if (fallbackVisible) 1f else 0f,
        animationSpec = tween(SEAMLESS_ARTWORK_FALLBACK_FADE_MS),
        label = "seamless-artwork-fallback"
    )
    Box(modifier = modifier) {
        AsyncImage(
            model = request,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            onSuccess = { success ->
                memory.lastKey = success.result.memoryCacheKey
                phase = SeamlessArtworkPhase.Ready
            },
            onError = {
                memory.lastKey = null
                phase = SeamlessArtworkPhase.Failed
            },
            modifier = Modifier.matchParentSize()
        )
        if (fallbackAlpha > 0.001f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = fallbackAlpha }
            ) {
                fallback()
            }
        }
    }
}
