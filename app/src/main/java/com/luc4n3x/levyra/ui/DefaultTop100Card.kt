package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.components.LevyraUtilitySeed
import com.materialkolor.hct.Hct

private val DefaultTop100Corner = 20.dp
private const val DefaultTop100Chroma = 46.0
private const val DefaultTop100HueStep = 30.0
private val DefaultTop100Bars = floatArrayOf(0.34f, 0.52f, 0.42f, 0.7f, 0.58f, 0.86f, 0.66f, 1f)

@Immutable
private data class DefaultTop100Tones(val light: Color, val deep: Color)

private fun defaultTop100Tones(global: Boolean, slot: Int): DefaultTop100Tones {
    val seedHue = Hct.fromInt(LevyraUtilitySeed.toArgb()).hue
    val hue = if (global) seedHue else ((seedHue + (slot + 1) * DefaultTop100HueStep) % 360.0 + 360.0) % 360.0
    return DefaultTop100Tones(
        light = Color(Hct.from(hue, DefaultTop100Chroma, 46.0).toInt()),
        deep = Color(Hct.from(hue, DefaultTop100Chroma, 22.0).toInt())
    )
}

@Composable
internal fun DefaultTop100Card(label: String, date: String, global: Boolean, slot: Int, modifier: Modifier = Modifier) {
    val tones = remember(global, slot) { defaultTop100Tones(global, slot) }
    val bars = remember(slot) { FloatArray(DefaultTop100Bars.size) { DefaultTop100Bars[(it + slot) % DefaultTop100Bars.size] } }
    Box(
        modifier
            .clip(RoundedCornerShape(DefaultTop100Corner))
            .drawWithCache {
                val fill = Brush.linearGradient(listOf(tones.light, tones.deep), start = Offset.Zero, end = Offset(size.width, size.height))
                val barWidth = 7.dp.toPx()
                val gap = 5.dp.toPx()
                val maxHeight = size.height * 0.46f
                val radius = CornerRadius(barWidth / 2f)
                val right = size.width - 14.dp.toPx()
                val bottom = size.height
                onDrawBehind {
                    drawRect(fill)
                    bars.forEachIndexed { index, level ->
                        val height = maxHeight * level
                        val left = right - (bars.size - index) * (barWidth + gap) + gap
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.10f + 0.06f * level),
                            topLeft = Offset(left, bottom - height),
                            size = Size(barWidth, height + radius.x),
                            cornerRadius = radius
                        )
                    }
                }
            }
    ) {
        Text(
            date,
            color = Color.White.copy(alpha = 0.86f),
            fontSize = fixedSp(11f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            maxLines = 1,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = 15.dp)
        )
        Box(
            Modifier.align(Alignment.TopEnd).padding(10.dp).size(30.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Equalizer, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
        }
        Column(Modifier.align(Alignment.BottomStart).padding(start = 14.dp, end = 14.dp, bottom = 13.dp)) {
            Text(
                "100",
                color = Color.White,
                fontSize = fixedSp(56f),
                lineHeight = fixedSp(56f),
                fontWeight = FontWeight.Black,
                letterSpacing = (-2.5).sp,
                maxLines = 1
            )
            Text(
                label,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
