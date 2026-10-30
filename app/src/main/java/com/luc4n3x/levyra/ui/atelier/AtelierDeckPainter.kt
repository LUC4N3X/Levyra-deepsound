package com.luc4n3x.levyra.ui.atelier

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

internal const val AtelierDeckPlinth = 0.038f

internal fun atelierBakeDeck(widthPx: Int): Bitmap {
    val w = widthPx.toFloat()
    val deckHeight = w * AtelierDeckGeometry.Aspect
    val bitmap = Bitmap.createBitmap(widthPx, (deckHeight + w * AtelierDeckPlinth).toInt() + 2, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    val random = Random(1978)
    drawWalnut(canvas, paint, random, w, deckHeight)
    drawPlate(canvas, paint, random, w, deckHeight)
    drawPlatter(canvas, paint, w)
    drawControls(canvas, paint, w, deckHeight)
    drawArmBase(canvas, paint, w)
    return bitmap
}

private fun drawWalnut(canvas: Canvas, paint: Paint, random: Random, w: Float, deckHeight: Float) {
    val total = deckHeight + w * AtelierDeckPlinth
    paint.shader = LinearGradient(0f, 0f, 0f, deckHeight, intArrayOf(0xFF5B3820.toInt(), 0xFF4A2C18.toInt(), 0xFF3A2212.toInt()), null, Shader.TileMode.CLAMP)
    canvas.drawRect(0f, 0f, w, deckHeight, paint)
    paint.shader = LinearGradient(0f, deckHeight, 0f, total, intArrayOf(0xFF2E1A0D.toInt(), 0xFF170D07.toInt()), null, Shader.TileMode.CLAMP)
    canvas.drawRect(0f, deckHeight, w, total, paint)
    paint.shader = null
    paint.style = Paint.Style.STROKE
    val grain = Path()
    repeat(260) {
        val y = random.nextFloat() * total
        val amplitude = random.nextFloat() * w * 0.006f
        val phase = random.nextFloat() * 6.28f
        grain.reset()
        grain.moveTo(0f, y)
        var x = 0f
        while (x < w) {
            x += w * 0.06f
            grain.lineTo(x, y + sin(x / w * 11f + phase) * amplitude)
        }
        paint.strokeWidth = 0.6f + random.nextFloat() * 1.4f
        paint.color = if (random.nextFloat() < 0.72f) 20 + random.nextInt(50) shl 24 or 0x120804 else 10 + random.nextInt(18) shl 24 or 0xC98E55
        canvas.drawPath(grain, paint)
    }
    paint.style = Paint.Style.FILL
    paint.color = 0x38FFD9AE
    canvas.drawRect(0f, deckHeight - 1.5f, w, deckHeight, paint)
}

private fun drawPlate(canvas: Canvas, paint: Paint, random: Random, w: Float, deckHeight: Float) {
    val margin = w * 0.028f
    val plate = RectF(margin, margin, w - margin, deckHeight - margin)
    val corner = w * 0.006f
    paint.color = 0x99000000.toInt()
    canvas.drawRoundRect(RectF(plate.left - 2f, plate.top - 2f, plate.right + 3f, plate.bottom + 4f), corner, corner, paint)
    paint.shader = LinearGradient(plate.left, plate.top, plate.right, plate.bottom, intArrayOf(0xFF4D463C.toInt(), 0xFF39332B.toInt(), 0xFF2B2620.toInt()), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
    canvas.drawRoundRect(plate, corner, corner, paint)
    paint.shader = null
    val checkpoint = canvas.save()
    canvas.clipRect(plate)
    paint.strokeWidth = 1f
    var y = plate.top
    while (y < plate.bottom) {
        paint.color = if (random.nextBoolean()) 6 + random.nextInt(14) shl 24 or 0xFFFFFF else 8 + random.nextInt(18) shl 24
        canvas.drawLine(plate.left, y, plate.right, y, paint)
        y += 1.6f
    }
    paint.shader = RadialGradient(w * 0.24f, deckHeight * 0.16f, w * 0.85f, intArrayOf(0x24FFEFD6, 0x00FFEFD6), null, Shader.TileMode.CLAMP)
    canvas.drawRect(plate, paint)
    paint.shader = null
    canvas.restoreToCount(checkpoint)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1.2f
    paint.color = 0x26FFFFFF
    canvas.drawRoundRect(plate, corner, corner, paint)
    paint.style = Paint.Style.FILL
}

private fun drawPlatter(canvas: Canvas, paint: Paint, w: Float) {
    val cx = w * AtelierDeckGeometry.PlatterX
    val cy = w * AtelierDeckGeometry.PlatterY
    val r = w * AtelierDeckGeometry.PlatterRadius
    paint.shader = RadialGradient(cx, cy + r * 0.05f, r * 1.14f, intArrayOf(0xB0000000.toInt(), 0xB0000000.toInt(), 0x00000000), floatArrayOf(0f, 0.88f, 1f), Shader.TileMode.CLAMP)
    canvas.drawCircle(cx, cy + r * 0.05f, r * 1.14f, paint)
    paint.shader = SweepGradient(
        cx, cy,
        intArrayOf(0xFF8F8A80.toInt(), 0xFFE8E3D8.toInt(), 0xFF76716A.toInt(), 0xFFD2CDC2.toInt(), 0xFF6C6760.toInt(), 0xFFEDE8DD.toInt(), 0xFF8F8A80.toInt()), null
    )
    canvas.drawCircle(cx, cy, r, paint)
    paint.shader = null
    paint.color = 0x88000000.toInt()
    paint.strokeWidth = r * 0.012f
    var tick = 0
    while (tick < 180) {
        val a = tick * 2.0 * Math.PI / 180.0
        val inner = if (tick % 5 == 0) r * 0.962f else r * 0.975f
        canvas.drawLine(cx + cos(a).toFloat() * inner, cy + sin(a).toFloat() * inner, cx + cos(a).toFloat() * r * 0.992f, cy + sin(a).toFloat() * r * 0.992f, paint)
        tick++
    }
    paint.shader = RadialGradient(cx, cy, r * 0.95f, intArrayOf(0xFF1E1C1A.toInt(), 0xFF171514.toInt(), 0xFF100F0E.toInt()), floatArrayOf(0f, 0.8f, 1f), Shader.TileMode.CLAMP)
    canvas.drawCircle(cx, cy, r * 0.95f, paint)
    paint.shader = null
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = r * 0.012f
    listOf(0.42f, 0.56f, 0.70f, 0.84f).forEach { ring ->
        paint.color = 0x14FFFFFF
        canvas.drawCircle(cx, cy, r * ring, paint)
    }
    paint.style = Paint.Style.FILL
    paint.color = 0xFFC4BFB6.toInt()
    canvas.drawCircle(cx, cy, r * 0.016f, paint)
}

private fun knob(canvas: Canvas, paint: Paint, x: Float, y: Float, radius: Float, index: Float) {
    paint.color = 0x88000000.toInt()
    canvas.drawCircle(x + radius * 0.08f, y + radius * 0.16f, radius * 1.06f, paint)
    paint.shader = SweepGradient(x, y, intArrayOf(0xFF9C968B.toInt(), 0xFFF1ECE1.toInt(), 0xFF87827A.toInt(), 0xFFE2DDD2.toInt(), 0xFF7E7971.toInt(), 0xFF9C968B.toInt()), null)
    canvas.drawCircle(x, y, radius, paint)
    paint.shader = null
    paint.color = 0xFFC8A26A.toInt()
    paint.strokeWidth = radius * 0.13f
    val a = Math.toRadians(index.toDouble())
    canvas.drawLine(x + sin(a).toFloat() * radius * 0.35f, y - cos(a).toFloat() * radius * 0.35f, x + sin(a).toFloat() * radius * 0.85f, y - cos(a).toFloat() * radius * 0.85f, paint)
}

private fun drawControls(canvas: Canvas, paint: Paint, w: Float, deckHeight: Float) {
    val y = deckHeight - w * 0.068f
    knob(canvas, paint, w * 0.075f, y, w * 0.024f, -35f)
    knob(canvas, paint, w * 0.145f, y, w * 0.019f, 40f)
    paint.color = 0x55000000
    canvas.drawRoundRect(RectF(w * 0.69f, y - w * 0.012f, w * 0.80f, y + w * 0.012f), w * 0.012f, w * 0.012f, paint)
    paint.color = 0xFFD6D1C6.toInt()
    canvas.drawRoundRect(RectF(w * 0.70f, y - w * 0.008f, w * 0.745f, y + w * 0.008f), w * 0.008f, w * 0.008f, paint)
    paint.shader = RadialGradient(w * 0.075f, w * 0.066f, w * 0.022f, intArrayOf(0xFFFFC98A.toInt(), 0x55FF9A4A, 0x00FF9A4A), floatArrayOf(0f, 0.3f, 1f), Shader.TileMode.CLAMP)
    canvas.drawCircle(w * 0.075f, w * 0.066f, w * 0.022f, paint)
    paint.shader = null
}

private fun drawArmBase(canvas: Canvas, paint: Paint, w: Float) {
    val x = w * AtelierDeckGeometry.PivotX
    val y = w * AtelierDeckGeometry.PivotY
    val r = w * 0.052f
    paint.color = 0x99000000.toInt()
    canvas.drawCircle(x + r * 0.08f, y + r * 0.22f, r * 1.08f, paint)
    paint.shader = SweepGradient(x, y, intArrayOf(0xFF4A453E.toInt(), 0xFFA69E90.toInt(), 0xFF3A362F.toInt(), 0xFF8D867A.toInt(), 0xFF4A453E.toInt()), null)
    canvas.drawCircle(x, y, r, paint)
    paint.shader = null
    paint.color = 0xFFC8A26A.toInt()
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = r * 0.07f
    canvas.drawCircle(x, y, r * 0.72f, paint)
    paint.style = Paint.Style.FILL
    val restX = x + w * 0.033f
    val restY = y + w * AtelierDeckGeometry.ArmLength * 0.62f
    paint.color = 0x88000000.toInt()
    canvas.drawCircle(restX + 2f, restY + 4f, w * 0.012f, paint)
    paint.color = 0xFF8D867A.toInt()
    canvas.drawCircle(restX, restY, w * 0.010f, paint)
}

internal class AtelierArmSprite(val bitmap: Bitmap, val shadow: Bitmap, val pivotX: Float, val pivotY: Float)

internal fun atelierBakeArm(deckWidthPx: Int): AtelierArmSprite {
    val w = deckWidthPx.toFloat()
    val length = w * AtelierDeckGeometry.ArmLength
    val sideRoom = w * 0.10f
    val topRoom = w * 0.10f
    val bottomRoom = w * 0.05f
    val bitmap = Bitmap.createBitmap((sideRoom * 2).toInt(), (topRoom + length + bottomRoom).toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val px = sideRoom
    val py = topRoom
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val stylusY = py + length

    paint.shader = LinearGradient(px - w * 0.02f, 0f, px + w * 0.02f, 0f, intArrayOf(0xFF3B3731.toInt(), 0xFFCFC9BD.toInt(), 0xFF57524A.toInt()), floatArrayOf(0f, 0.42f, 1f), Shader.TileMode.CLAMP)
    canvas.drawRoundRect(RectF(px - w * 0.019f, py - w * 0.088f, px + w * 0.019f, py - w * 0.036f), w * 0.005f, w * 0.005f, paint)
    paint.shader = null
    paint.style = Paint.Style.STROKE
    paint.strokeCap = Paint.Cap.ROUND
    val tube = Path().apply {
        moveTo(px, py - w * 0.036f)
        lineTo(px, py + length * 0.20f)
        cubicTo(px, py + length * 0.46f, px + w * 0.058f, py + length * 0.52f, px + w * 0.050f, py + length * 0.72f)
        cubicTo(px + w * 0.045f, py + length * 0.84f, px + w * 0.010f, py + length * 0.88f, px, py + length * 0.925f)
    }
    paint.strokeWidth = w * 0.0125f
    paint.color = 0xFF2C2925.toInt()
    canvas.drawPath(tube, paint)
    paint.strokeWidth = w * 0.0095f
    paint.color = 0xFFA9A398.toInt()
    canvas.drawPath(tube, paint)
    paint.strokeWidth = w * 0.0032f
    paint.color = 0xFFF3EEE3.toInt()
    canvas.save()
    canvas.translate(-w * 0.0018f, 0f)
    canvas.drawPath(tube, paint)
    canvas.restore()
    paint.style = Paint.Style.FILL
    paint.strokeCap = Paint.Cap.BUTT
    paint.shader = LinearGradient(px - w * 0.02f, 0f, px + w * 0.02f, 0f, intArrayOf(0xFF7E6A44.toInt(), 0xFFE7C993.toInt(), 0xFF8F7648.toInt()), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
    canvas.drawRoundRect(RectF(px - w * 0.0165f, stylusY - w * 0.056f, px + w * 0.0165f, stylusY - w * 0.006f), w * 0.004f, w * 0.004f, paint)
    paint.shader = null
    paint.color = 0xFF17141A.toInt()
    canvas.drawRect(px - w * 0.0095f, stylusY - w * 0.033f, px + w * 0.0095f, stylusY - w * 0.004f, paint)
    paint.color = 0xFFE7C993.toInt()
    canvas.drawRect(px - w * 0.0015f, stylusY - w * 0.006f, px + w * 0.0015f, stylusY, paint)
    paint.color = 0xFFCFC9BD.toInt()
    paint.strokeWidth = w * 0.0028f
    canvas.drawLine(px + w * 0.0155f, stylusY - w * 0.046f, px + w * 0.036f, stylusY - w * 0.030f, paint)
    paint.color = 0xFF7C766C.toInt()
    canvas.drawCircle(px, py, w * 0.021f, paint)
    paint.color = 0xFFDAD4C8.toInt()
    canvas.drawCircle(px, py, w * 0.012f, paint)
    return AtelierArmSprite(bitmap, bitmap.extractAlpha(), px, py)
}
