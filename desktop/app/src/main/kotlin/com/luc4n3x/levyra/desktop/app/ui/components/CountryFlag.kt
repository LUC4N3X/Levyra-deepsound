package com.luc4n3x.levyra.desktop.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CountryFlag(
    countryCode: String,
    modifier: Modifier = Modifier.width(38.dp).height(25.dp)
) {
    val shape = RoundedCornerShape(5.dp)
    Canvas(
        modifier = modifier
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.18f), shape)
    ) {
        val w = size.width
        val h = size.height
        val code = countryCode.trim().uppercase()

        fun horizontal(colors: List<Color>) {
            val stripe = h / colors.size
            colors.forEachIndexed { index, color ->
                drawRect(color, Offset(0f, stripe * index), Size(w, stripe + 1f))
            }
        }

        fun vertical(colors: List<Color>) {
            val stripe = w / colors.size
            colors.forEachIndexed { index, color ->
                drawRect(color, Offset(stripe * index, 0f), Size(stripe + 1f, h))
            }
        }

        fun star(
            centerX: Float,
            centerY: Float,
            outerRadius: Float,
            innerRadius: Float,
            color: Color,
            points: Int = 5
        ) {
            val path = Path()
            repeat(points * 2) { index ->
                val radius = if (index % 2 == 0) outerRadius else innerRadius
                val angle = -PI / 2 + index * PI / points
                val x = centerX + cos(angle).toFloat() * radius
                val y = centerY + sin(angle).toFloat() * radius
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, color)
        }

        when (code) {
            "IT" -> vertical(listOf(Color(0xFF009246), Color.White, Color(0xFFCE2B37)))
            "FR" -> vertical(listOf(Color(0xFF0055A4), Color.White, Color(0xFFEF4135)))
            "RO" -> vertical(listOf(Color(0xFF002B7F), Color(0xFFFCD116), Color(0xFFCE1126)))
            "DE" -> horizontal(listOf(Color(0xFF151515), Color(0xFFDD0000), Color(0xFFFFCE00)))
            "NL" -> horizontal(listOf(Color(0xFFAE1C28), Color.White, Color(0xFF21468B)))
            "RU" -> horizontal(listOf(Color.White, Color(0xFF0039A6), Color(0xFFD52B1E)))
            "UA" -> horizontal(listOf(Color(0xFF0057B7), Color(0xFFFFD700)))
            "PL" -> horizontal(listOf(Color.White, Color(0xFFDC143C)))
            "ID" -> horizontal(listOf(Color(0xFFFF0000), Color.White))
            "ES" -> {
                drawRect(Color(0xFFAA151B))
                drawRect(Color(0xFFF1BF00), Offset(0f, h * 0.25f), Size(w, h * 0.5f))
            }
            "PT" -> {
                drawRect(Color(0xFFFF0000))
                drawRect(Color(0xFF046A38), size = Size(w * 0.4f, h))
                drawCircle(Color(0xFFFFD700), h * 0.16f, Offset(w * 0.4f, h * 0.5f))
            }
            "SE" -> {
                drawRect(Color(0xFF006AA7))
                drawRect(Color(0xFFFECC00), Offset(w * 0.31f, 0f), Size(w * 0.13f, h))
                drawRect(Color(0xFFFECC00), Offset(0f, h * 0.42f), Size(w, h * 0.16f))
            }
            "DK" -> {
                drawRect(Color(0xFFC8102E))
                drawRect(Color.White, Offset(w * 0.31f, 0f), Size(w * 0.11f, h))
                drawRect(Color.White, Offset(0f, h * 0.43f), Size(w, h * 0.14f))
            }
            "GR" -> {
                repeat(9) { index ->
                    drawRect(
                        if (index % 2 == 0) Color(0xFF0D5EAF) else Color.White,
                        Offset(0f, index * h / 9f),
                        Size(w, h / 9f + 1f)
                    )
                }
                drawRect(Color(0xFF0D5EAF), size = Size(w * 0.42f, h * 5f / 9f))
                drawRect(Color.White, Offset(w * 0.17f, 0f), Size(w * 0.08f, h * 5f / 9f))
                drawRect(Color.White, Offset(0f, h * 2f / 9f), Size(w * 0.42f, h / 9f))
            }
            "CZ" -> {
                horizontal(listOf(Color.White, Color(0xFFD7141A)))
                val triangle = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(w * 0.48f, h * 0.5f)
                    lineTo(0f, h)
                    close()
                }
                drawPath(triangle, Color(0xFF11457E))
            }
            "GB" -> {
                drawRect(Color(0xFF012169))
                drawLine(Color.White, Offset(0f, 0f), Offset(w, h), h * 0.28f)
                drawLine(Color.White, Offset(w, 0f), Offset(0f, h), h * 0.28f)
                drawLine(Color(0xFFC8102E), Offset(0f, 0f), Offset(w, h), h * 0.11f)
                drawLine(Color(0xFFC8102E), Offset(w, 0f), Offset(0f, h), h * 0.11f)
                drawRect(Color.White, Offset(w * 0.39f, 0f), Size(w * 0.22f, h))
                drawRect(Color.White, Offset(0f, h * 0.34f), Size(w, h * 0.32f))
                drawRect(Color(0xFFC8102E), Offset(w * 0.44f, 0f), Size(w * 0.12f, h))
                drawRect(Color(0xFFC8102E), Offset(0f, h * 0.41f), Size(w, h * 0.18f))
            }
            "US" -> {
                repeat(13) { index ->
                    drawRect(
                        if (index % 2 == 0) Color(0xFFB22234) else Color.White,
                        Offset(0f, index * h / 13f),
                        Size(w, h / 13f + 1f)
                    )
                }
                drawRect(Color(0xFF3C3B6E), size = Size(w * 0.43f, h * 7f / 13f))
                repeat(3) { row ->
                    repeat(4) { column ->
                        drawCircle(
                            Color.White,
                            h * 0.022f,
                            Offset(w * (0.055f + column * 0.09f), h * (0.07f + row * 0.13f))
                        )
                    }
                }
            }
            "BR" -> {
                drawRect(Color(0xFF009C3B))
                val diamond = Path().apply {
                    moveTo(w * 0.5f, h * 0.1f)
                    lineTo(w * 0.9f, h * 0.5f)
                    lineTo(w * 0.5f, h * 0.9f)
                    lineTo(w * 0.1f, h * 0.5f)
                    close()
                }
                drawPath(diamond, Color(0xFFFFDF00))
                drawCircle(Color(0xFF002776), h * 0.22f, Offset(w * 0.5f, h * 0.5f))
            }
            "TR" -> {
                drawRect(Color(0xFFE30A17))
                drawCircle(Color.White, h * 0.23f, Offset(w * 0.43f, h * 0.5f))
                drawCircle(Color(0xFFE30A17), h * 0.19f, Offset(w * 0.49f, h * 0.47f))
                star(w * 0.63f, h * 0.5f, h * 0.11f, h * 0.045f, Color.White)
            }
            "SA" -> {
                drawRect(Color(0xFF006C35))
                drawLine(Color.White, Offset(w * 0.28f, h * 0.67f), Offset(w * 0.75f, h * 0.67f), h * 0.055f)
                drawLine(Color.White, Offset(w * 0.68f, h * 0.61f), Offset(w * 0.77f, h * 0.67f), h * 0.04f)
            }
            "CN" -> {
                drawRect(Color(0xFFDE2910))
                star(w * 0.23f, h * 0.29f, h * 0.17f, h * 0.068f, Color(0xFFFFDE00))
                repeat(4) { index ->
                    val x = w * (0.42f + (index % 2) * 0.09f)
                    val y = h * (0.16f + index * 0.13f)
                    star(x, y, h * 0.045f, h * 0.018f, Color(0xFFFFDE00))
                }
            }
            "JP" -> {
                drawRect(Color.White)
                drawCircle(Color(0xFFBC002D), h * 0.28f, Offset(w * 0.5f, h * 0.5f))
            }
            "KR" -> {
                drawRect(Color.White)
                drawCircle(Color(0xFFCD2E3A), h * 0.22f, Offset(w * 0.5f, h * 0.45f))
                drawCircle(Color(0xFF0047A0), h * 0.22f, Offset(w * 0.5f, h * 0.57f))
                drawCircle(Color(0xFFCD2E3A), h * 0.11f, Offset(w * 0.5f, h * 0.57f))
                drawCircle(Color(0xFF0047A0), h * 0.11f, Offset(w * 0.5f, h * 0.45f))
                drawLine(Color.Black, Offset(w * 0.16f, h * 0.22f), Offset(w * 0.28f, h * 0.34f), h * 0.035f)
                drawLine(Color.Black, Offset(w * 0.72f, h * 0.66f), Offset(w * 0.84f, h * 0.78f), h * 0.035f)
            }
            "IN" -> {
                horizontal(listOf(Color(0xFFFF9933), Color.White, Color(0xFF138808)))
                drawCircle(Color(0xFF000080), h * 0.1f, Offset(w * 0.5f, h * 0.5f), style = androidx.compose.ui.graphics.drawscope.Stroke(h * 0.025f))
                repeat(8) { index ->
                    val angle = index * PI / 4
                    drawLine(
                        Color(0xFF000080),
                        Offset(w * 0.5f, h * 0.5f),
                        Offset(w * 0.5f + cos(angle).toFloat() * h * 0.09f, h * 0.5f + sin(angle).toFloat() * h * 0.09f),
                        h * 0.012f
                    )
                }
            }
            "VN" -> {
                drawRect(Color(0xFFDA251D))
                star(w * 0.5f, h * 0.5f, h * 0.25f, h * 0.1f, Color(0xFFFFFF00))
            }
            "TH" -> {
                val colors = listOf(
                    Color(0xFFA51931),
                    Color.White,
                    Color(0xFF2D2A4A),
                    Color(0xFF2D2A4A),
                    Color.White,
                    Color(0xFFA51931)
                )
                horizontal(colors)
            }
            "PH" -> {
                drawRect(Color(0xFF0038A8), size = Size(w, h * 0.5f))
                drawRect(Color(0xFFCE1126), Offset(0f, h * 0.5f), Size(w, h * 0.5f))
                val triangle = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(w * 0.43f, h * 0.5f)
                    lineTo(0f, h)
                    close()
                }
                drawPath(triangle, Color.White)
                drawCircle(Color(0xFFFCD116), h * 0.085f, Offset(w * 0.14f, h * 0.5f))
            }
            "IL" -> {
                drawRect(Color.White)
                drawRect(Color(0xFF0038B8), Offset(0f, h * 0.14f), Size(w, h * 0.11f))
                drawRect(Color(0xFF0038B8), Offset(0f, h * 0.75f), Size(w, h * 0.11f))
                val cx = w * 0.5f
                val cy = h * 0.5f
                val r = h * 0.19f
                val up = Path().apply {
                    moveTo(cx, cy - r)
                    lineTo(cx - r * 0.86f, cy + r * 0.5f)
                    lineTo(cx + r * 0.86f, cy + r * 0.5f)
                    close()
                }
                val down = Path().apply {
                    moveTo(cx, cy + r)
                    lineTo(cx - r * 0.86f, cy - r * 0.5f)
                    lineTo(cx + r * 0.86f, cy - r * 0.5f)
                    close()
                }
                drawPath(up, Color(0xFF0038B8), style = androidx.compose.ui.graphics.drawscope.Stroke(h * 0.04f))
                drawPath(down, Color(0xFF0038B8), style = androidx.compose.ui.graphics.drawscope.Stroke(h * 0.04f))
            }
            "MX" -> {
                vertical(listOf(Color(0xFF006847), Color.White, Color(0xFFCE1126)))
                drawCircle(Color(0xFF8A6D3B), h * 0.08f, Offset(w * 0.5f, h * 0.5f))
            }
            "CA" -> {
                vertical(listOf(Color(0xFFFF0000), Color.White, Color.White, Color(0xFFFF0000)))
                star(w * 0.5f, h * 0.5f, h * 0.17f, h * 0.07f, Color(0xFFFF0000), 7)
            }
            "AU" -> {
                drawRect(Color(0xFF012169))
                drawCircle(Color.White, h * 0.07f, Offset(w * 0.72f, h * 0.34f))
                drawCircle(Color.White, h * 0.05f, Offset(w * 0.82f, h * 0.62f))
                drawCircle(Color.White, h * 0.045f, Offset(w * 0.61f, h * 0.72f))
            }
            else -> {
                drawRect(Color(0xFF1F2937))
                drawCircle(Color(0xFF22D3EE), h * 0.18f, Offset(w * 0.5f, h * 0.5f))
            }
        }
    }
}
