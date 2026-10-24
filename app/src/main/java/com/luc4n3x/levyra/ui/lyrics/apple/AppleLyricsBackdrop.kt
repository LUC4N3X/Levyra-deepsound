package com.luc4n3x.levyra.ui.lyrics.apple

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Size
import coil3.toBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

private const val BACKDROP_ARTWORK_PX = 96
private const val BACKDROP_OVERSCAN = 1.3f
private const val BACKDROP_SATURATION = 2.5f
private const val BACKDROP_FRAME_MS = 50L
private const val BACKDROP_BLUR_RADIUS = 8
private const val BACKDROP_BLUR_PASSES = 3
private const val BACKDROP_PERIOD_A_MS = 120_000L
private const val BACKDROP_PERIOD_B_MS = 90_000L
private const val BACKDROP_PERIOD_C_MS = 70_000L
private val BackdropFallback = Color(0xFF424242)
private val BackdropBlackScrim = android.graphics.Color.argb(77, 0, 0, 0)
private val BackdropWhiteScrim = android.graphics.Color.argb(26, 255, 255, 255)

internal class AppleLyricsBackdropRenderer(private val artwork: Bitmap, viewWidth: Int, viewHeight: Int, downsample: Float) {
    private val width = max(8, (viewWidth * BACKDROP_OVERSCAN / downsample).roundToInt())
    private val height = max(8, (viewHeight * BACKDROP_OVERSCAN / downsample).roundToInt())
    val bitmap: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    private val canvas = Canvas(bitmap)
    private val pixels = IntArray(width * height)
    private val scratch = IntArray(width * height)
    private val matrix = Matrix()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(BACKDROP_SATURATION) })
    }
    private val square = (max(width, height) * BACKDROP_OVERSCAN).roundToInt().toFloat()
    private val artworkScale = square / max(artwork.width, artwork.height).coerceAtLeast(1)

    fun render(elapsedMs: Long) {
        canvas.drawColor(android.graphics.Color.BLACK)
        val half = square / 2f
        val baseX = -(square - width) / 2f
        val baseY = -(square - height) / 2f
        layer(rotation(elapsedMs, BACKDROP_PERIOD_A_MS, -360f), half, baseX, baseY, 0f, 0f, 0f)
        layer(rotation(elapsedMs, BACKDROP_PERIOD_B_MS, 360f), half, baseX, baseY, -0.95f * width, -0.7f * height, 0f)
        val c = rotation(elapsedMs, BACKDROP_PERIOD_C_MS, 360f)
        layer(c, half, baseX, baseY, -0.5f * width, 0.7f * height, c)
        canvas.drawColor(BackdropBlackScrim)
        canvas.drawColor(BackdropWhiteScrim)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        repeat(BACKDROP_BLUR_PASSES) {
            boxBlurHorizontal(pixels, scratch, width, height, BACKDROP_BLUR_RADIUS)
            boxBlurVertical(scratch, pixels, width, height, BACKDROP_BLUR_RADIUS)
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }

    private fun layer(angle: Float, half: Float, baseX: Float, baseY: Float, dx: Float, dy: Float, orbit: Float) {
        matrix.setScale(artworkScale, artworkScale)
        matrix.postRotate(angle, half, half)
        matrix.postTranslate(baseX + dx, baseY + dy)
        if (orbit != 0f) matrix.postRotate(orbit, width / 2f, height / 2f)
        canvas.drawBitmap(artwork, matrix, paint)
    }

    fun recycle() {
        bitmap.recycle()
    }
}

private fun rotation(elapsedMs: Long, periodMs: Long, sweep: Float): Float =
    sweep * ((elapsedMs % periodMs).toFloat() / periodMs)

private fun boxBlurHorizontal(source: IntArray, target: IntArray, width: Int, height: Int, radius: Int) {
    val window = radius * 2 + 1
    for (y in 0 until height) {
        val row = y * width
        var a = 0; var r = 0; var g = 0; var b = 0
        for (i in -radius..radius) {
            val p = source[row + i.coerceIn(0, width - 1)]
            a += p ushr 24; r += (p shr 16) and 0xFF; g += (p shr 8) and 0xFF; b += p and 0xFF
        }
        for (x in 0 until width) {
            target[row + x] = (a / window shl 24) or (r / window shl 16) or (g / window shl 8) or b / window
            val outgoing = source[row + (x - radius).coerceIn(0, width - 1)]
            val incoming = source[row + (x + radius + 1).coerceIn(0, width - 1)]
            a += (incoming ushr 24) - (outgoing ushr 24)
            r += ((incoming shr 16) and 0xFF) - ((outgoing shr 16) and 0xFF)
            g += ((incoming shr 8) and 0xFF) - ((outgoing shr 8) and 0xFF)
            b += (incoming and 0xFF) - (outgoing and 0xFF)
        }
    }
}

private fun boxBlurVertical(source: IntArray, target: IntArray, width: Int, height: Int, radius: Int) {
    val window = radius * 2 + 1
    for (x in 0 until width) {
        var a = 0; var r = 0; var g = 0; var b = 0
        for (i in -radius..radius) {
            val p = source[i.coerceIn(0, height - 1) * width + x]
            a += p ushr 24; r += (p shr 16) and 0xFF; g += (p shr 8) and 0xFF; b += p and 0xFF
        }
        for (y in 0 until height) {
            target[y * width + x] = (a / window shl 24) or (r / window shl 16) or (g / window shl 8) or b / window
            val outgoing = source[(y - radius).coerceIn(0, height - 1) * width + x]
            val incoming = source[(y + radius + 1).coerceIn(0, height - 1) * width + x]
            a += (incoming ushr 24) - (outgoing ushr 24)
            r += ((incoming shr 16) and 0xFF) - ((outgoing shr 16) and 0xFF)
            g += ((incoming shr 8) and 0xFF) - ((outgoing shr 8) and 0xFF)
            b += (incoming and 0xFF) - (outgoing and 0xFF)
        }
    }
}

private class BackdropClock {
    var elapsedMs = 0L
}

@Composable
internal fun AppleLyricsBackdrop(artworkUrl: String?, animate: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val artwork by produceState<Bitmap?>(null, artworkUrl) {
        value = null
        if (artworkUrl.isNullOrBlank()) return@produceState
        value = withContext(Dispatchers.IO) {
            val request = ImageRequest.Builder(context)
                .data(artworkUrl)
                .size(Size(BACKDROP_ARTWORK_PX, BACKDROP_ARTWORK_PX))
                .allowHardware(false)
                .build()
            try {
                (SingletonImageLoader.get(context).execute(request) as? SuccessResult)?.image?.toBitmap()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
        }
    }
    val densityDpi = (LocalDensity.current.density * 160f).roundToInt()
    val downsample = if (densityDpi >= 420) 24f else 16f
    BoxWithConstraints(modifier.fillMaxSize().background(BackdropFallback)) {
        val widthPx = constraints.maxWidth
        val heightPx = constraints.maxHeight
        val source = artwork
        if (source != null && widthPx > 0 && heightPx > 0) {
            val renderer = remember(source, widthPx, heightPx, downsample) {
                AppleLyricsBackdropRenderer(source, widthPx, heightPx, downsample)
            }
            DisposableEffect(renderer) { onDispose { renderer.recycle() } }
            val clock = remember(source) { BackdropClock() }
            val frame = remember(renderer) { mutableIntStateOf(0) }
            LaunchedEffect(renderer, animate) {
                renderer.render(clock.elapsedMs)
                frame.intValue++
                if (!animate) return@LaunchedEffect
                var last = android.os.SystemClock.elapsedRealtime()
                while (true) {
                    delay(BACKDROP_FRAME_MS)
                    val now = android.os.SystemClock.elapsedRealtime()
                    clock.elapsedMs += now - last
                    last = now
                    renderer.render(clock.elapsedMs)
                    frame.intValue++
                }
            }
            val alpha by animateFloatAsState(if (frame.intValue > 0) 1f else 0f, tween(600), label = "apple-lyrics-backdrop")
            val target = remember { RectF() }
            val bitmapPaint = remember { Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG) }
            Box(
                Modifier.fillMaxSize().drawBehind {
                    frame.intValue
                    val overscanX = size.width * (BACKDROP_OVERSCAN - 1f) / 2f
                    val overscanY = size.height * (BACKDROP_OVERSCAN - 1f) / 2f
                    target.set(-overscanX, -overscanY, size.width + overscanX, size.height + overscanY)
                    bitmapPaint.alpha = (alpha * 255).roundToInt()
                    drawIntoCanvas { it.nativeCanvas.drawBitmap(renderer.bitmap, null, target, bitmapPaint) }
                }
            )
        }
    }
}
