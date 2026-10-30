package com.luc4n3x.levyra.ui.atelier

import android.app.ActivityManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Size
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

internal object AtelierStage {
    var expansion: () -> Float = { 1f }
    var miniRect: () -> androidx.compose.ui.geometry.Rect? = { null }
    var shelfCover: Bitmap? = null
    var shelfHandoff = false
    var miniAttached by androidx.compose.runtime.mutableStateOf(false)
    val shelfQuad = FloatArray(8)
    var shelfQuadValid = false

    fun reset() {
        expansion = { 1f }
        miniRect = { null }
        shelfCover = null
        shelfHandoff = false
        shelfQuadValid = false
    }
}

internal enum class AtelierQuality { Standard, High }

internal fun atelierQualityOf(context: Context): AtelierQuality {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val constrained = manager == null || manager.isLowRamDevice || manager.memoryClass < 192
    return if (constrained) AtelierQuality.Standard else AtelierQuality.High
}

internal suspend fun atelierLoadCover(context: Context, url: String, sizePx: Int, hardware: Boolean): Bitmap? {
    if (url.isBlank()) return null
    val request = ImageRequest.Builder(context).data(url).size(Size(sizePx, sizePx)).allowHardware(hardware).build()
    val result = SingletonImageLoader.get(context).execute(request) as? SuccessResult ?: return null
    return withContext(Dispatchers.Default) { result.image.toBitmap() }
}

internal suspend fun atelierLoadAmbient(context: Context, url: String): Pair<Int, Int>? {
    val bitmap = atelierLoadCover(context, url, 24, hardware = false) ?: return null
    return withContext(Dispatchers.Default) {
        val readable = if (bitmap.config == Bitmap.Config.HARDWARE) bitmap.copy(Bitmap.Config.ARGB_8888, false) else bitmap
        val pixels = IntArray(readable.width * readable.height)
        readable.getPixels(pixels, 0, readable.width, 0, 0, readable.width, readable.height)
        atelierAmbientFrom(pixels)
    }
}

internal fun atelierBakeDisc(sizePx: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val c = sizePx * 0.5f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.shader = RadialGradient(c, c, c, intArrayOf(0xFF17151A.toInt(), 0xFF0E0D10.toInt(), 0xFF09080A.toInt()), floatArrayOf(0f, 0.7f, 1f), Shader.TileMode.CLAMP)
    canvas.drawCircle(c, c, c, paint)
    paint.shader = null
    paint.style = Paint.Style.STROKE
    val outer = 0.965f
    val inner = 0.36f
    val pitch = (2f / sizePx).coerceAtLeast(0.002f) * 1.4f
    var ratio = inner
    var seed = 7
    while (ratio < outer) {
        seed = seed * 31 + 11
        val wobble = (seed and 0xFF) / 255f
        val gap = BandGaps.any { ratio > it && ratio < it + 0.008f }
        paint.color = if (gap) 0x00FFFFFF else AndroidColor.argb((10 + wobble * 22).toInt(), 255, 255, 255)
        paint.strokeWidth = 1f
        canvas.drawCircle(c, c, ratio * c, paint)
        ratio += pitch
    }
    paint.color = 0x24FFFFFF
    paint.strokeWidth = 1.6f
    canvas.drawCircle(c, c, c * 0.992f, paint)
    paint.style = Paint.Style.FILL
    return bitmap
}

private val BandGaps = floatArrayOf(0.48f, 0.60f, 0.72f, 0.84f)

internal fun atelierBakeDiscLight(sizePx: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val c = sizePx * 0.5f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.shader = SweepGradient(
        c, c,
        intArrayOf(0x00FFFFFF, 0x2CFFF4E2, 0x08FFFFFF, 0x00FFFFFF, 0x00FFFFFF, 0x1EDCE6FF, 0x06FFFFFF, 0x00FFFFFF, 0x00FFFFFF),
        floatArrayOf(0f, 0.07f, 0.14f, 0.22f, 0.47f, 0.56f, 0.64f, 0.72f, 1f)
    )
    canvas.drawCircle(c, c, c * 0.985f, paint)
    paint.shader = null
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    canvas.drawCircle(c, c, c * 0.345f, paint)
    return bitmap
}

internal fun atelierBakeLabel(cover: Bitmap?, sizePx: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val c = sizePx * 0.5f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    paint.color = 0xFF2A241E.toInt()
    canvas.drawCircle(c, c, c, paint)
    if (cover != null) {
        val layer = canvas.saveLayer(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), null)
        canvas.drawCircle(c, c, c, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        val readable = if (cover.config == Bitmap.Config.HARDWARE) cover.copy(Bitmap.Config.ARGB_8888, false) else cover
        val edge = min(readable.width, readable.height)
        val src = Rect((readable.width - edge) / 2, (readable.height - edge) / 2, (readable.width + edge) / 2, (readable.height + edge) / 2)
        canvas.drawBitmap(readable, src, RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()), paint)
        paint.xfermode = null
        canvas.restoreToCount(layer)
    }
    paint.shader = RadialGradient(c, c, c, intArrayOf(0x00000000, 0x00000000, 0x55000000), floatArrayOf(0f, 0.86f, 1f), Shader.TileMode.CLAMP)
    canvas.drawCircle(c, c, c, paint)
    paint.shader = null
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = sizePx * 0.006f
    paint.color = 0x30FFFFFF
    canvas.drawCircle(c, c, c * 0.992f, paint)
    paint.style = Paint.Style.FILL
    paint.color = 0xFF0B0A09.toInt()
    canvas.drawCircle(c, c, sizePx * 0.042f, paint)
    paint.color = 0xFFB9B4AC.toInt()
    canvas.drawCircle(c, c, sizePx * 0.026f, paint)
    return bitmap
}

internal fun atelierBakeSoftMask(sizePx: Int, inset: Float, blur: Float): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ALPHA_8)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = AndroidColor.BLACK
    paint.maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL)
    canvas.drawRect(inset, inset, sizePx - inset, sizePx - inset, paint)
    return bitmap
}

internal fun atelierBakeGlow(sizePx: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ALPHA_8)
    val canvas = Canvas(bitmap)
    val c = sizePx * 0.5f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.shader = RadialGradient(
        c, c, c,
        intArrayOf(0xFF000000.toInt(), 0xB0000000.toInt(), 0x50000000, 0x14000000, 0x00000000),
        floatArrayOf(0f, 0.28f, 0.55f, 0.8f, 1f), Shader.TileMode.CLAMP
    )
    canvas.drawCircle(c, c, c, paint)
    return bitmap
}

internal class AtelierSleevePainter {
    private val unit = floatArrayOf(0f, 0f, UnitSize, 0f, UnitSize, UnitSize, 0f, UnitSize)
    private val matrix = Matrix()
    private val localMatrix = Matrix()
    private val src = Rect()
    private val dst = RectF()
    private val coverPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val maskPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    private val spinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, 52f, 0f, intArrayOf(0x66000000, 0x10000000, 0x00000000), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
    }
    private val foldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, UnitSize, UnitSize, intArrayOf(0x14FFFFFF, 0x00FFFFFF, 0x1A000000), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
    }
    private val sheenShader = LinearGradient(-260f, -160f, 260f, 160f, intArrayOf(0x00FFFFFF, 0x24FFFFFF, 0x00FFFFFF), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
    private val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = sheenShader }
    private val castPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(UnitSize, 0f, UnitSize + 120f, 0f, intArrayOf(0x8C000000.toInt(), 0x00000000), null, Shader.TileMode.CLAMP)
    }

    fun draw(
        canvas: Canvas,
        quad: FloatArray,
        cover: Bitmap?,
        disc: Bitmap?,
        discOut: Float,
        shadowMask: Bitmap?,
        shadowStrength: Float,
        sheen: Float,
        dim: Float,
        alpha: Float,
        rich: Boolean
    ) {
        val byteAlpha = (alpha.coerceIn(0f, 1f) * 255f).toInt()
        if (byteAlpha <= 0 || !matrix.setPolyToPoly(unit, 0, quad, 0, 4)) return
        val checkpoint = canvas.save()
        canvas.concat(matrix)
        if (shadowMask != null && shadowStrength > 0f) {
            maskPaint.color = 0xFF000000.toInt()
            maskPaint.alpha = (byteAlpha * shadowStrength).toInt()
            dst.set(-150f, -90f, UnitSize + 210f, UnitSize + 240f)
            canvas.drawBitmap(shadowMask, null, dst, maskPaint)
        }
        if (disc != null && discOut > 0.001f) {
            val centerX = HalfUnit + discOut * DiscTravel
            dst.set(centerX - DiscRadius, HalfUnit - DiscRadius, centerX + DiscRadius, HalfUnit + DiscRadius)
            coverPaint.alpha = byteAlpha
            canvas.drawBitmap(disc, null, dst, coverPaint)
            castPaint.alpha = byteAlpha
            canvas.drawRect(UnitSize, HalfUnit - DiscRadius, UnitSize + 120f, HalfUnit + DiscRadius, castPaint)
        }
        fillPaint.color = 0xFF2A241E.toInt()
        fillPaint.alpha = byteAlpha
        if (rich) canvas.drawRect(UnitSize - 2f, 4f, UnitSize + 9f, UnitSize - 2f, fillPaint)
        fillPaint.color = 0xFF1C1815.toInt()
        fillPaint.alpha = byteAlpha
        canvas.drawRect(0f, 0f, UnitSize, UnitSize, fillPaint)
        if (cover != null && !cover.isRecycled) {
            val edge = min(cover.width, cover.height)
            src.set((cover.width - edge) / 2, (cover.height - edge) / 2, (cover.width + edge) / 2, (cover.height + edge) / 2)
            dst.set(0f, 0f, UnitSize, UnitSize)
            coverPaint.alpha = byteAlpha
            canvas.drawBitmap(cover, src, dst, coverPaint)
        }
        spinePaint.alpha = byteAlpha
        canvas.drawRect(0f, 0f, 52f, UnitSize, spinePaint)
        foldPaint.alpha = byteAlpha
        canvas.drawRect(0f, 0f, UnitSize, UnitSize, foldPaint)
        if (rich) {
            localMatrix.setTranslate(sheen * UnitSize, sheen * UnitSize * 0.6f)
            sheenShader.setLocalMatrix(localMatrix)
            sheenPaint.alpha = byteAlpha
            canvas.drawRect(0f, 0f, UnitSize, UnitSize, sheenPaint)
        }
        fillPaint.color = 0x2EFFFFFF
        fillPaint.alpha = (byteAlpha * 0.18f).toInt()
        canvas.drawRect(0f, 0f, UnitSize, 5f, fillPaint)
        canvas.drawRect(0f, 5f, 5f, UnitSize, fillPaint)
        if (dim > 0.004f) {
            fillPaint.color = 0xFF000000.toInt()
            fillPaint.alpha = (byteAlpha * dim.coerceIn(0f, 1f)).toInt()
            canvas.drawRect(0f, 0f, UnitSize, UnitSize, fillPaint)
        }
        canvas.restoreToCount(checkpoint)
    }

    private companion object {
        const val UnitSize = 1000f
        const val HalfUnit = 500f
        const val DiscRadius = 478f
        const val DiscTravel = 400f
    }
}
