package com.luc4n3x.levyra.desktop.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.desktop.core.model.EqualizerSettings

@Composable
fun EqualizerBars(
    amps: List<Float>,
    accent: Color,
    onChange: (Int, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val bands = EqualizerSettings.BAND_FREQUENCIES
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val guideColor = MaterialTheme.colorScheme.outlineVariant

    fun report(position: Offset, canvasSize: IntSize) {
        if (canvasSize.width <= 0 || canvasSize.height <= 0) return
        val slot = canvasSize.width.toFloat() / bands.size
        val index = (position.x / slot).toInt().coerceIn(0, bands.lastIndex)
        val ratio = 1f - (position.y / canvasSize.height).coerceIn(0f, 1f)
        onChange(index, (ratio * 2f - 1f) * EqualizerSettings.MAX_GAIN)
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .pointerInput(bands.size) {
                    detectTapGestures { offset -> report(offset, size) }
                }
                .pointerInput(bands.size) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        report(change.position, size)
                    }
                }
        ) {
            val slot = size.width / bands.size
            val barWidth = slot * 0.44f
            val center = size.height / 2f
            val radius = CornerRadius(barWidth / 2f, barWidth / 2f)

            drawRect(
                color = guideColor,
                topLeft = Offset(0f, center - 0.5f),
                size = Size(size.width, 1f)
            )

            bands.indices.forEach { index ->
                val x = slot * index + (slot - barWidth) / 2f
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = radius
                )
                val gain = amps.getOrElse(index) { 0f }
                    .coerceIn(EqualizerSettings.MIN_GAIN, EqualizerSettings.MAX_GAIN)
                val magnitude = (gain / EqualizerSettings.MAX_GAIN) * center
                val top = if (magnitude >= 0f) center - magnitude else center
                drawRoundRect(
                    color = accent,
                    topLeft = Offset(x, top),
                    size = Size(barWidth, kotlin.math.abs(magnitude).coerceAtLeast(2f)),
                    cornerRadius = radius
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            bands.forEach { frequency ->
                Text(
                    text = EqualizerSettings.bandLabel(frequency),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
