package com.luc4n3x.levyra.ui.atelier

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled

private class AtelierAmbientPaint {
    val glow: Bitmap = atelierBakeGlow(128)
    val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    val rect = RectF()
}

@Composable
internal fun AtelierAmbientBackdrop(coverUrl: String?, modifier: Modifier = Modifier, strength: Float = 1f) {
    val context = LocalContext.current
    val animated = LocalAnimationsEnabled.current
    val paint = remember { AtelierAmbientPaint() }
    val tones by produceState<Pair<Int, Int>?>(null, coverUrl) {
        value = coverUrl?.let { atelierLoadAmbient(context, it) } ?: value
    }
    val fallback = Color(0xFF2A2556)
    val primary by animateColorAsState(tones?.first?.let(::Color) ?: fallback, tween(if (animated) 520 else 0), label = "atelier-ambient-primary")
    val secondary by animateColorAsState(tones?.second?.let(::Color) ?: fallback, tween(if (animated) 520 else 0), label = "atelier-ambient-secondary")
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawIntoCanvas { composeCanvas ->
            val canvas = composeCanvas.nativeCanvas
            paint.paint.color = primary.toArgb()
            paint.paint.alpha = (150f * strength).toInt().coerceIn(0, 255)
            paint.rect.set(-w * 0.75f, -h * 0.25f, w * 1.1f, h * 0.75f)
            canvas.drawBitmap(paint.glow, null, paint.rect, paint.paint)
            paint.paint.color = secondary.toArgb()
            paint.paint.alpha = (120f * strength).toInt().coerceIn(0, 255)
            paint.rect.set(-w * 0.05f, h * 0.2f, w * 1.75f, h * 1.2f)
            canvas.drawBitmap(paint.glow, null, paint.rect, paint.paint)
        }
    }
}
