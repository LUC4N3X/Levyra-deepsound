package com.luc4n3x.levyra.widget.studio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.luc4n3x.levyra.R
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

internal object WidgetStudioPainter {
    private const val MAX_FULL_SIDE = 900
    private const val MAX_ART_SIDE = 720

    data class Scrim(val fadeStart: Float, val solidAt: Float, val solidAlpha: Float, val topAlpha: Float, val horizontal: Boolean = false)

    fun fullBleed(
        context: Context,
        source: Bitmap?,
        widthPx: Int,
        heightPx: Int,
        radiusPx: Float,
        scheme: StudioColorScheme,
        scrim: Scrim?,
        circularInset: Boolean = false
    ): Bitmap {
        val (w, h) = capped(widthPx, heightPx, MAX_FULL_SIDE)
        val scale = w.toFloat() / widthPx.coerceAtLeast(1)
        val radius = radiusPx * scale
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bounds = RectF(0f, 0f, w.toFloat(), h.toFloat())
        val clip = Path().apply { addRoundRect(bounds, radius, radius, Path.Direction.CW) }
        canvas.clipPath(clip)
        canvas.drawColor(scheme.surface)
        if (circularInset) {
            val side = min(w, h) * 0.78f
            val left = (w - side) / 2f
            val top = (h - side) / 2f * 0.82f
            val circle = RectF(left, top, left + side, top + side)
            drawHalo(canvas, circle, scheme)
            drawArtwork(context, canvas, source, circle, side / 2f, scheme)
        } else {
            drawArtwork(context, canvas, source, bounds, 0f, scheme)
        }
        if (scrim != null) drawScrim(canvas, bounds, scheme, scrim)
        return bitmap
    }

    fun rounded(
        context: Context,
        source: Bitmap?,
        widthPx: Int,
        heightPx: Int,
        radiusPx: Float,
        circular: Boolean,
        scheme: StudioColorScheme,
        outputScale: Float = 1f
    ): Bitmap {
        val (w, h) = capped(widthPx, heightPx, MAX_ART_SIDE)
        val scale = w.toFloat() / widthPx.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bounds = RectF(0f, 0f, w.toFloat(), h.toFloat())
        val radius = if (circular) min(w, h) / 2f else radiusPx * scale
        drawArtwork(context, canvas, source, bounds, radius, scheme)
        val densityScale = scale * outputScale
        if (densityScale != 1f) bitmap.density = (bitmap.density * densityScale).roundToInt().coerceAtLeast(1)
        return bitmap
    }

    fun vinyl(context: Context, source: Bitmap?, sizePx: Int, scheme: StudioColorScheme): Bitmap {
        val side = sizePx.coerceIn(64, MAX_FULL_SIDE)
        val bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val c = side / 2f
        val r = side / 2f * 0.97f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.argb(70, 0, 0, 0)
        canvas.drawCircle(c, c + side * 0.008f, r, paint)
        val discColor = ColorUtils.blendARGB(0xFF0D0D10.toInt(), scheme.primary, 0.07f)
        paint.color = discColor
        canvas.drawCircle(c, c, r, paint)
        paint.style = Paint.Style.STROKE
        val labelRadius = r * 0.56f
        var groove = r * 0.97f
        var index = 0
        while (groove > labelRadius + r * 0.05f) {
            paint.strokeWidth = r * if (index % 3 == 0) 0.006f else 0.003f
            paint.color = Color.argb(if (index % 3 == 0) 34 else 18, 255, 255, 255)
            canvas.drawCircle(c, c, groove, paint)
            groove -= r * 0.027f
            index++
        }
        paint.style = Paint.Style.FILL
        val sheen = SweepGradient(
            c, c,
            intArrayOf(
                Color.TRANSPARENT, Color.argb(46, 255, 255, 255), Color.TRANSPARENT, Color.TRANSPARENT,
                Color.argb(30, 255, 255, 255), Color.TRANSPARENT, Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.09f, 0.2f, 0.5f, 0.59f, 0.7f, 1f)
        )
        paint.shader = sheen
        canvas.drawCircle(c, c, r, paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = r * 0.022f
        paint.color = scheme.primary
        canvas.drawCircle(c, c, labelRadius + r * 0.022f, paint)
        paint.style = Paint.Style.FILL
        val label = RectF(c - labelRadius, c - labelRadius, c + labelRadius, c + labelRadius)
        drawArtwork(context, canvas, source, label, labelRadius, scheme)
        paint.color = Color.argb(60, 0, 0, 0)
        canvas.drawCircle(c, c, r * 0.06f, paint)
        paint.color = scheme.surface
        canvas.drawCircle(c, c, r * 0.032f, paint)
        return bitmap
    }

    fun progress(
        widthPx: Int,
        heightPx: Int,
        fraction: Float,
        active: Int,
        track: Int,
        wavy: Boolean,
        density: Float,
        rtl: Boolean = false
    ): Bitmap {
        val w = widthPx.coerceIn(32, 1200)
        val h = heightPx.coerceIn(8, 64)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (rtl) canvas.scale(-1f, 1f, w / 2f, 0f)
        val stroke = 4f * density
        val cy = h / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = stroke
        }
        val start = stroke / 2f
        val end = w - stroke / 2f
        val clamped = fraction.coerceIn(0f, 1f)
        val activeEnd = start + (end - start) * clamped
        val gap = 4f * density
        val trackStart = activeEnd + gap + stroke
        if (trackStart < end) {
            paint.color = track
            canvas.drawLine(trackStart, cy, end, cy, paint)
        }
        paint.style = Paint.Style.FILL
        paint.color = active
        canvas.drawCircle(end, cy, stroke / 2f, paint)
        paint.style = Paint.Style.STROKE
        if (activeEnd > start + 0.5f) {
            paint.color = active
            if (wavy) {
                val amplitude = min(h / 2f - stroke / 2f, 2.6f * density).coerceAtLeast(0f)
                val wavelength = 26f * density
                val path = Path()
                var x = start
                path.moveTo(x, cy)
                while (x < activeEnd) {
                    x = min(x + 1.5f, activeEnd)
                    path.lineTo(x, cy + amplitude * sin(2.0 * PI * (x - start) / wavelength).toFloat())
                }
                canvas.drawPath(path, paint)
            } else {
                canvas.drawLine(start, cy, activeEnd, cy, paint)
            }
        }
        return bitmap
    }

    private fun drawHalo(canvas: Canvas, circle: RectF, scheme: StudioColorScheme) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val radius = circle.width() / 2f * 1.18f
        paint.shader = RadialGradient(
            circle.centerX(), circle.centerY(), radius,
            intArrayOf(WidgetStudioColors.withAlpha(scheme.primary, 0.35f), Color.TRANSPARENT),
            floatArrayOf(0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(circle.centerX(), circle.centerY(), radius, paint)
    }

    private fun drawArtwork(
        context: Context,
        canvas: Canvas,
        source: Bitmap?,
        target: RectF,
        radius: Float,
        scheme: StudioColorScheme
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        if (source != null && !source.isRecycled) {
            val shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            val scale = max(target.width() / source.width, target.height() / source.height)
            val dx = target.left + (target.width() - source.width * scale) / 2f
            val dy = target.top + (target.height() - source.height * scale) / 2f
            shader.setLocalMatrix(Matrix().apply {
                setScale(scale, scale)
                postTranslate(dx, dy)
            })
            paint.shader = shader
            canvas.drawRoundRect(target, radius, radius, paint)
            return
        }
        paint.shader = LinearGradient(
            target.left, target.top, target.right, target.bottom,
            scheme.primaryContainer, ColorUtils.blendARGB(scheme.primaryContainer, scheme.surface, 0.55f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(target, radius, radius, paint)
        val icon = ContextCompat.getDrawable(context, R.drawable.ic_studio_note)?.mutate() ?: return
        val side = (min(target.width(), target.height()) * 0.34f).roundToInt()
        val left = (target.centerX() - side / 2f).roundToInt()
        val top = (target.centerY() - side / 2f).roundToInt()
        icon.setBounds(left, top, left + side, top + side)
        icon.setTint(WidgetStudioColors.withAlpha(scheme.onPrimaryContainer, 0.7f))
        icon.draw(canvas)
    }

    private fun drawScrim(canvas: Canvas, bounds: RectF, scheme: StudioColorScheme, scrim: Scrim) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val base = scheme.scrim
        if (scrim.horizontal) {
            paint.shader = LinearGradient(
                bounds.left, 0f, bounds.right, 0f,
                intArrayOf(
                    WidgetStudioColors.withAlpha(base, 0f),
                    WidgetStudioColors.withAlpha(base, scrim.solidAlpha),
                    WidgetStudioColors.withAlpha(base, (scrim.solidAlpha + 0.06f).coerceAtMost(0.97f))
                ),
                floatArrayOf(scrim.fadeStart, scrim.solidAt, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(bounds, paint)
            return
        }
        paint.shader = LinearGradient(
            0f, bounds.top, 0f, bounds.bottom,
            intArrayOf(
                WidgetStudioColors.withAlpha(base, 0f),
                WidgetStudioColors.withAlpha(base, scrim.solidAlpha * 0.55f),
                WidgetStudioColors.withAlpha(base, scrim.solidAlpha),
                WidgetStudioColors.withAlpha(base, (scrim.solidAlpha + 0.06f).coerceAtMost(0.97f))
            ),
            floatArrayOf(scrim.fadeStart, (scrim.fadeStart + scrim.solidAt) / 2f, scrim.solidAt, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(bounds, paint)
        if (scrim.topAlpha > 0f) {
            paint.shader = LinearGradient(
                0f, bounds.top, 0f, bounds.top + bounds.height() * 0.24f,
                WidgetStudioColors.withAlpha(base, scrim.topAlpha), WidgetStudioColors.withAlpha(base, 0f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.top + bounds.height() * 0.24f, paint)
        }
    }

    private fun capped(widthPx: Int, heightPx: Int, maxSide: Int): Pair<Int, Int> {
        val w = widthPx.coerceAtLeast(16)
        val h = heightPx.coerceAtLeast(16)
        val longest = max(w, h)
        if (longest <= maxSide) return w to h
        val ratio = maxSide.toFloat() / longest
        return (w * ratio).roundToInt().coerceAtLeast(16) to (h * ratio).roundToInt().coerceAtLeast(16)
    }
}
