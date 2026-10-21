package com.luc4n3x.levyra.ui.player

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ComposeShader
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Size
import coil3.toBitmap
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.CoverImage
import com.luc4n3x.levyra.ui.MotionArtworkLayer
import com.luc4n3x.levyra.ui.MotionArtworkPresentation
import com.luc4n3x.levyra.ui.preferredPlayerArtworkUrl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

private const val CanvasVideoAspect = 4f / 3f
private val CanvasBarrierOverlap = 150.dp
private const val CanvasLegibilityBase = 0xB3171717.toInt()
private const val CanvasLegibilityTop = 0x33FFFFFF
private const val CanvasBlackScrim = 0x59000000
private const val CanvasWhiteScrim = 0x0AFFFFFF
private const val CanvasStripBlurRadius = 25
private const val CanvasSaturation = 1.4f

internal class AppleCanvasReflection(val strip: Bitmap, val averageColor: Int)

@Composable
internal fun AppleMusicCanvasBackdrop(track: Track, canvas: AppleMusicCanvas, playing: Boolean, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val videoHeight = maxWidth * CanvasVideoAspect
        val barrierTop = (videoHeight - CanvasBarrierOverlap).coerceAtLeast(0.dp)
        val widthPx = with(density) { maxWidth.roundToPx() }
        val videoHeightPx = with(density) { videoHeight.roundToPx() }
        val reflection = rememberAppleCanvasReflection(track, widthPx, videoHeightPx)
        Box(Modifier.fillMaxWidth().height(videoHeight)) {
            key(track.id) {
                MotionArtworkLayer(
                    artwork = canvas.artwork,
                    enabled = canvas.enabled,
                    isPlaying = playing,
                    cornerRadius = 0.dp,
                    presentation = MotionArtworkPresentation.Immersive,
                    quality = canvas.quality,
                    livingArtwork = canvas.livingArtwork,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CoverImage(track, Modifier.fillMaxSize(), highRes = true)
                }
            }
        }
        Box(Modifier.fillMaxSize().drawWithCache { val paint = legibilityPaint(size.height); onDrawBehind { drawIntoCanvas { it.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint) } } })
        if (reflection != null) {
            val overlapPx = with(density) { CanvasBarrierOverlap.toPx() }
            Box(
                Modifier.fillMaxWidth().offset(y = barrierTop).height((maxHeight - barrierTop).coerceAtLeast(0.dp))
                    .graphicsLayer { scaleY = -1f }
                    .drawWithCache {
                        val paint = reflectionPaint(reflection.strip, size.width, size.height, overlapPx)
                        onDrawBehind { drawIntoCanvas { canvas -> canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint) } }
                    }
            )
            Box(
                Modifier.fillMaxWidth().offset(y = videoHeight).height((maxHeight - videoHeight).coerceAtLeast(0.dp))
                    .drawWithCache {
                        val paint = colorOverlayPaint(reflection.averageColor, size.height)
                        onDrawBehind { drawIntoCanvas { it.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint) } }
                    }
            )
        }
    }
}

@Composable
private fun rememberAppleCanvasReflection(track: Track, widthPx: Int, videoHeightPx: Int): AppleCanvasReflection? {
    val context = LocalContext.current
    val url = remember(track.id, track.thumbnailUrl, track.largeThumbnailUrl) { preferredPlayerArtworkUrl(track) }
    var reflection by remember(url, widthPx, videoHeightPx) { mutableStateOf<AppleCanvasReflection?>(null) }
    LaunchedEffect(url, widthPx, videoHeightPx) {
        if (url.isBlank() || widthPx <= 0 || videoHeightPx <= 0) return@LaunchedEffect
        val source = withContext(Dispatchers.IO) {
            val request = ImageRequest.Builder(context).data(url).size(Size(256, 256)).allowHardware(false).build()
            try {
                (SingletonImageLoader.get(context).execute(request) as? SuccessResult)?.image?.toBitmap()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "Canvas reflection source unavailable")
                null
            }
        } ?: return@LaunchedEffect
        reflection = withContext(Dispatchers.Default) { AppleCanvasReflections.build(source, widthPx, videoHeightPx) }
    }
    return reflection
}

internal object AppleCanvasReflections {
    fun build(source: Bitmap, viewWidth: Int, videoHeight: Int): AppleCanvasReflection {
        val sampleWidth = max(ceil(viewWidth / 8f / 10f).toInt() * 10, 2)
        val sampleHeight = max((videoHeight / 16f).roundToInt(), 2)
        val sample = Bitmap.createScaledBitmap(source, sampleWidth, sampleHeight, true)
        val stripHeight = max(sample.height / 8, 2)
        val strip = Bitmap.createBitmap(sample.width, stripHeight, Bitmap.Config.ARGB_8888)
        val saturation = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(CanvasSaturation) })
        }
        Canvas(strip).drawBitmap(
            sample,
            Rect(0, sample.height - stripHeight, sample.width, sample.height),
            Rect(0, 0, strip.width, strip.height),
            saturation
        )
        val blurred = boxBlur(strip, CanvasStripBlurRadius)
        Canvas(blurred).apply {
            drawColor(CanvasBlackScrim)
            drawColor(CanvasWhiteScrim)
        }
        val quarter = Bitmap.createScaledBitmap(blurred, 2, 2, true)
        var red = 0
        var green = 0
        var blue = 0
        for (x in 0 until 2) for (y in 0 until 2) {
            val pixel = quarter.getPixel(x, y)
            red += android.graphics.Color.red(pixel)
            green += android.graphics.Color.green(pixel)
            blue += android.graphics.Color.blue(pixel)
        }
        val average = android.graphics.Color.rgb((red / 4f).roundToInt(), (green / 4f).roundToInt(), (blue / 4f).roundToInt())
        return AppleCanvasReflection(blurred, average)
    }

    private fun boxBlur(source: Bitmap, radius: Int): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        repeat(3) {
            blurPass(pixels, width, height, radius.coerceAtMost(width - 1).coerceAtLeast(1), horizontal = true)
            blurPass(pixels, width, height, radius.coerceAtMost(height - 1).coerceAtLeast(1), horizontal = false)
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888).copy(Bitmap.Config.ARGB_8888, true)
    }

    private fun blurPass(pixels: IntArray, width: Int, height: Int, radius: Int, horizontal: Boolean) {
        val lines = if (horizontal) height else width
        val length = if (horizontal) width else height
        val buffer = IntArray(length)
        for (line in 0 until lines) {
            for (index in 0 until length) {
                var a = 0
                var r = 0
                var g = 0
                var b = 0
                for (offset in -radius..radius) {
                    val clamped = (index + offset).coerceIn(0, length - 1)
                    val pixel = if (horizontal) pixels[line * width + clamped] else pixels[clamped * width + line]
                    a += pixel ushr 24
                    r += (pixel shr 16) and 0xFF
                    g += (pixel shr 8) and 0xFF
                    b += pixel and 0xFF
                }
                val count = radius * 2 + 1
                buffer[index] = (a / count shl 24) or (r / count shl 16) or (g / count shl 8) or b / count
            }
            for (index in 0 until length) {
                if (horizontal) pixels[line * width + index] = buffer[index] else pixels[index * width + line] = buffer[index]
            }
        }
    }
}

private fun legibilityPaint(height: Float): Paint {
    val base = Bitmap.createBitmap(intArrayOf(CanvasLegibilityBase), 1, 1, Bitmap.Config.ARGB_8888)
    val mask = LinearGradient(0f, 0f, 0f, height, intArrayOf(CanvasLegibilityTop, 0, -1), floatArrayOf(0f, 0.2f, 1f), Shader.TileMode.CLAMP)
    return Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        shader = ComposeShader(mask, BitmapShader(base, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP), PorterDuff.Mode.SRC_IN)
    }
}

private fun reflectionPaint(strip: Bitmap, width: Float, height: Float, overlap: Float): Paint {
    val half = overlap * 0.5f
    val inner = width.pow(2) / 8f / half + half / 2f
    val radius = overlap - half + inner
    val mask = RadialGradient(width / 2f, height - half + inner, radius, intArrayOf(0, 0, -1), floatArrayOf(0f, inner / radius, 1f), Shader.TileMode.CLAMP)
    val bitmapShader = BitmapShader(strip, Shader.TileMode.CLAMP, Shader.TileMode.MIRROR).apply {
        if (Build.VERSION.SDK_INT >= 33) filterMode = BitmapShader.FILTER_MODE_LINEAR
        setLocalMatrix(Matrix().apply {
            setScale(1.25f * width / strip.width, height / strip.height)
            postTranslate(-0.25f * width / 2f, -overlap)
        })
    }
    return Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        shader = ComposeShader(mask, bitmapShader, PorterDuff.Mode.SRC_IN)
    }
}

private fun colorOverlayPaint(color: Int, height: Float): Paint {
    fun alpha(value: Int) = (color and 0x00FFFFFF) or (value shl 24)
    val colors = intArrayOf(0, alpha(204), alpha(242), color)
    return Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        shader = LinearGradient(0f, 0f, 0f, height * 0.4f, colors, floatArrayOf(0f, 0.7f, 0.9f, 1f), Shader.TileMode.CLAMP)
    }
}
