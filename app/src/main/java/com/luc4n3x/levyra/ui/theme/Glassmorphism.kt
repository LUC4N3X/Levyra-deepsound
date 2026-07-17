package com.luc4n3x.levyra.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.glassmorphism(
    shape: Shape,
    overlayColor: Color = Color(0x1AFFFFFF),
    borderGradient: Brush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.2f),
            Color.White.copy(alpha = 0.05f)
        )
    ),
    borderWidth: Float = 1f
): Modifier = composed {
    this
        .clip(shape)
        .background(overlayColor)
        .border(
            width = borderWidth.dp,
            brush = borderGradient,
            shape = shape
        )
}

fun Modifier.glowShadow(
    color: Color,
    borderRadius: Dp = 18.dp,
    blurRadius: Dp = 12.dp,
    offsetY: Dp = 4.dp,
    offsetX: Dp = 0.dp,
    alpha: Float = 0.12f
): Modifier = composed {
    this.drawBehind {
        val blurRadiusPx = blurRadius.toPx()
        val borderRadiusPx = borderRadius.toPx()
        val offsetYPx = offsetY.toPx()
        val offsetXPx = offsetX.toPx()
        if (blurRadiusPx > 0f) {
            drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    val frameworkPaint = asFrameworkPaint()
                    frameworkPaint.color = Color.Transparent.toArgb()
                    frameworkPaint.setShadowLayer(
                        blurRadiusPx,
                        offsetXPx,
                        offsetYPx,
                        color.copy(alpha = alpha).toArgb()
                    )
                }
                canvas.drawRoundRect(
                    left = 0f,
                    top = 0f,
                    right = size.width,
                    bottom = size.height,
                    radiusX = borderRadiusPx,
                    radiusY = borderRadiusPx,
                    paint = paint
                )
            }
        }
    }
}

