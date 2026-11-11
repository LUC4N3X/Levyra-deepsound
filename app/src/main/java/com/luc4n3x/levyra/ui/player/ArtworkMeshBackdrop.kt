package com.luc4n3x.levyra.ui.player

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.bitmapConfig
import coil3.toBitmap
import com.luc4n3x.levyra.data.ArtworkPaletteCache
import com.luc4n3x.levyra.data.LevyraArtworkCache
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.preferredPlayerArtworkUrl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.roundToInt

private const val MeshFadeMs = 900
private const val FadeEdgeSteps = 12
private val MeshBlur = 44.dp

@Immutable
internal class ArtworkMesh(val image: ImageBitmap, val topEdge: ImageBitmap, val topLuminance: Float)

private object ArtworkMeshMemory {
    val entries = LruCache<String, ArtworkMesh>(12)
}

@Composable
internal fun rememberArtworkMesh(track: Track): ArtworkMesh? {
    val context = LocalContext.current
    val key = remember(track.id, track.thumbnailUrl, track.largeThumbnailUrl) {
        ArtworkPaletteCache.key(track.id, track.thumbnailUrl, track.largeThumbnailUrl)
    }
    val url = preferredPlayerArtworkUrl(track)
    var mesh by remember { mutableStateOf<ArtworkMesh?>(null) }
    LaunchedEffect(key, url) {
        val cached = ArtworkMeshMemory.entries.get(key)
        if (cached != null) {
            mesh = cached
            return@LaunchedEffect
        }
        if (url.isBlank()) return@LaunchedEffect
        val loaded = loadArtworkMesh(context, url, key.hashCode()) ?: return@LaunchedEffect
        ArtworkMeshMemory.entries.put(key, loaded)
        mesh = loaded
    }
    return mesh
}

private suspend fun loadArtworkMesh(context: Context, url: String, seed: Int): ArtworkMesh? = try {
    val request = ImageRequest.Builder(context)
        .data(LevyraArtworkCache.small(url))
        .size(ArtworkMeshSourcePx, ArtworkMeshSourcePx)
        .allowHardware(false)
        .bitmapConfig(Bitmap.Config.ARGB_8888)
        .diskCachePolicy(CachePolicy.ENABLED)
        .memoryCachePolicy(CachePolicy.DISABLED)
        .build()
    val bitmap = withContext(Dispatchers.IO) { SingletonImageLoader.get(context).execute(request).image?.toBitmap() }
    if (bitmap == null) null else withContext(Dispatchers.Default) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        buildArtworkMesh(pixels, width, height, seed)?.let { built ->
            ArtworkMesh(
                image = Bitmap.createBitmap(built.texels, ArtworkMeshTexture, ArtworkMeshTexture, Bitmap.Config.ARGB_8888).asImageBitmap(),
                topEdge = Bitmap.createBitmap(built.topEdge, ArtworkMeshTexture, 1, Bitmap.Config.ARGB_8888).asImageBitmap(),
                topLuminance = built.topLuminance
            )
        }
    }
} catch (error: CancellationException) {
    throw error
} catch (error: Exception) {
    Timber.d(error, "Artwork mesh build failed")
    null
}

@Composable
internal fun ArtworkMeshBackdrop(mesh: ArtworkMesh?, seam: Dp, topHold: Dp, fallback: Color, modifier: Modifier = Modifier) {
    val animations = LocalAnimationsEnabled.current
    var shown by remember { mutableStateOf(mesh) }
    var incoming by remember { mutableStateOf<ArtworkMesh?>(null) }
    val fade = remember { Animatable(0f) }
    LaunchedEffect(mesh) {
        val next = mesh ?: return@LaunchedEffect
        incoming?.let { shown = it }
        incoming = null
        val current = shown
        if (next === current) return@LaunchedEffect
        if (current == null || !animations) {
            shown = next
            return@LaunchedEffect
        }
        incoming = next
        fade.snapTo(0f)
        fade.animateTo(1f, tween(MeshFadeMs, easing = FastOutSlowInEasing))
        shown = next
        incoming = null
    }
    Canvas(modifier.fillMaxSize().background(fallback).blur(MeshBlur)) {
        val seamY = seam.toPx().coerceIn(0f, size.height)
        val topHoldY = topHold.toPx().coerceIn(0f, seamY)
        shown?.let { drawArtworkMesh(it, seamY, topHoldY, 1f) }
        incoming?.let { drawArtworkMesh(it, seamY, topHoldY, fade.value) }
        drawRect(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.06f), 1f to Color.Black.copy(alpha = 0.30f)))
    }
}

private fun DrawScope.drawArtworkMesh(mesh: ArtworkMesh, seamY: Float, topHoldY: Float, alpha: Float) {
    if (alpha <= 0.001f) return
    val width = size.width.roundToInt()
    val seam = seamY.roundToInt()
    val topHold = topHoldY.roundToInt().coerceIn(0, seam)
    if (topHold > 0) {
        drawImage(
            mesh.topEdge, IntOffset.Zero, IntSize(ArtworkMeshTexture, 1), IntOffset.Zero, IntSize(width, topHold),
            alpha = alpha, filterQuality = FilterQuality.Low
        )
    }
    if (seam > topHold) {
        drawImage(
            mesh.image, IntOffset.Zero, IntSize(ArtworkMeshTexture, 1), IntOffset(0, topHold), IntSize(width, seam - topHold),
            alpha = alpha, filterQuality = FilterQuality.Low
        )
    }
    drawImage(
        mesh.image, IntOffset.Zero, IntSize(ArtworkMeshTexture, ArtworkMeshTexture), IntOffset(0, seam),
        IntSize(width, (size.height.roundToInt() - seam).coerceAtLeast(1)),
        alpha = alpha, filterQuality = FilterQuality.Low
    )
}

internal fun Modifier.verticalFadeEdges(topFade: Dp, bottomFade: Dp): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            if (size.height <= 0f) return@drawWithContent
            val topStop = topFade.toPx().coerceIn(0f, size.height / 2f) / size.height
            val bottomStop = 1f - bottomFade.toPx().coerceIn(0f, size.height / 2f) / size.height
            drawRect(Brush.verticalGradient(colorStops = fadeEdgeStops(topStop, bottomStop)), blendMode = BlendMode.DstIn)
        }

internal fun Modifier.horizontalFadeEdges(fadeBackward: Boolean, fadeForward: Boolean, fade: Dp): Modifier =
    if (!fadeBackward && !fadeForward) this
    else graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            if (size.width <= 0f) return@drawWithContent
            val edge = fade.toPx().coerceIn(0f, size.width / 2f) / size.width
            val ltr = layoutDirection == LayoutDirection.Ltr
            val fadeLeft = if (ltr) fadeBackward else fadeForward
            val fadeRight = if (ltr) fadeForward else fadeBackward
            val stops = fadeEdgeStops(if (fadeLeft) edge else 0f, if (fadeRight) 1f - edge else 1f)
            drawRect(Brush.horizontalGradient(colorStops = stops), blendMode = BlendMode.DstIn)
        }

private fun fadeEdgeStops(topStop: Float, bottomStop: Float): Array<Pair<Float, Color>> = buildList {
    if (topStop > 0f) {
        for (i in 0..FadeEdgeSteps) {
            val t = i / FadeEdgeSteps.toFloat()
            add(topStop * t to Color.Black.copy(alpha = smoothstep(t)))
        }
    } else {
        add(0f to Color.Black)
    }
    if (bottomStop < 1f) {
        for (i in 0..FadeEdgeSteps) {
            val t = i / FadeEdgeSteps.toFloat()
            add(bottomStop + (1f - bottomStop) * t to Color.Black.copy(alpha = 1f - smoothstep(t)))
        }
    } else {
        add(1f to Color.Black)
    }
}.toTypedArray()

private fun smoothstep(t: Float): Float = t * t * (3f - 2f * t)
