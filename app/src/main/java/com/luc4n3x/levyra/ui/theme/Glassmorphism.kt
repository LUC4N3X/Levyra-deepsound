package com.luc4n3x.levyra.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
        .shadow(
            elevation = 16.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.5f),
            spotColor = Color.Black.copy(alpha = 0.5f)
        )
        .clip(shape)
        .background(overlayColor)
        .border(
            width = borderWidth.dp,
            brush = borderGradient,
            shape = shape
        )
}
