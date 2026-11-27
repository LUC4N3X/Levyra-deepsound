package com.luc4n3x.levyra.ui.player

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luc4n3x.levyra.domain.SpectrumVisualizerStyle
import com.luc4n3x.levyra.player.visualizer.SpectrumEngine
import com.luc4n3x.levyra.player.visualizer.SpectrumFrame
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.SpectrumText
import com.luc4n3x.levyra.ui.i18n.spectrumCopy
import com.luc4n3x.levyra.ui.lyrics.rememberLyricsAudioOutputRoute
import com.luc4n3x.levyra.viewmodel.LevyraUiState

fun playerSpectrumVisible(state: LevyraUiState): Boolean =
    state.audioSettings.spectrumVisualizerStyle.enabled && state.currentTrack != null && !state.isVideoMode

@Composable
fun PlayerSpectrumStrip(state: LevyraUiState, modifier: Modifier = Modifier) {
    val style = state.audioSettings.spectrumVisualizerStyle
    val track = state.currentTrack ?: return
    val route = rememberLyricsAudioOutputRoute()
    val offsetMs = state.lyricsLatencyProfiles.resolve(route?.stableKey, route?.bluetooth == true)
    val tall = style == SpectrumVisualizerStyle.ANALYZER || style == SpectrumVisualizerStyle.SPECTROGRAM
    Box(
        modifier
            .fillMaxWidth()
            .background(SpectrumStripBackground)
            .navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 6.dp)
    ) {
        SpectrumVisualizer(
            style = style,
            outputLatencyOffsetMs = offsetMs,
            accent = Color(track.accentStart),
            description = LocalLevyraStrings.current.spectrumCopy[SpectrumText.Accessibility],
            modifier = Modifier
                .fillMaxWidth()
                .height(if (tall) 72.dp else 52.dp)
        )
    }
}

private val SpectrumStripBackground = Color(0xFF0B0B0D)

@Composable
fun SpectrumVisualizer(
    style: SpectrumVisualizerStyle,
    outputLatencyOffsetMs: Long,
    accent: Color,
    description: String,
    modifier: Modifier = Modifier
) {
    if (!style.enabled) return
    SideEffect { SpectrumEngine.outputLatencyOffsetMs = outputLatencyOffsetMs }
    val frameState = SpectrumEngine.frame.collectAsStateWithLifecycle()
    val palette = remember(accent) { SpectrumPalette(accent) }
    val path = remember { Path() }
    val spectrogram = remember(style, palette) {
        if (style == SpectrumVisualizerStyle.SPECTROGRAM) SpectrogramCanvas(palette) else null
    }
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = remember(palette) { TextStyle(color = palette.grid.copy(alpha = 0.9f), fontSize = 9.sp) }
    val analyzerLabels = remember(style, labelStyle) {
        if (style == SpectrumVisualizerStyle.ANALYZER) {
            AnalyzerLabels(
                levels = ANALYZER_GRID_DB.map { db -> textMeasurer.measure("$db", labelStyle) },
                frequencies = ANALYZER_FREQUENCY_LABELS.map { (_, label) -> textMeasurer.measure(label, labelStyle) }
            )
        } else {
            null
        }
    }
    Canvas(modifier.semantics { contentDescription = description }) {
        val frame = frameState.value ?: return@Canvas
        when (style) {
            SpectrumVisualizerStyle.OFF -> Unit
            SpectrumVisualizerStyle.BARS -> drawBars(frame, palette)
            SpectrumVisualizerStyle.MIRROR -> drawMirror(frame, palette)
            SpectrumVisualizerStyle.LED -> drawLed(frame, palette)
            SpectrumVisualizerStyle.CURVE -> drawCurve(frame, palette, path)
            SpectrumVisualizerStyle.ANALYZER -> drawAnalyzer(frame, palette, analyzerLabels)
            SpectrumVisualizerStyle.SPECTROGRAM -> spectrogram?.draw(this, frame)
            SpectrumVisualizerStyle.OSCILLOSCOPE -> drawScope(frame, palette, path)
        }
    }
}

private class SpectrumPalette(accent: Color) {
    val bright = lerp(accent, Color.White, 0.55f)
    val dim = accent.copy(alpha = 0.28f)
    val grid = Color.White.copy(alpha = 0.16f)
    val peak = Color.White.copy(alpha = 0.92f)
    val barBrush = Brush.verticalGradient(listOf(bright, accent, accent.copy(alpha = 0.55f)))
    val curveFill = Brush.verticalGradient(listOf(accent.copy(alpha = 0.55f), Color.Transparent))
    val curveStroke = Stroke(width = 2.2f, cap = StrokeCap.Round)
    val scopeGlow = Stroke(width = 6f, cap = StrokeCap.Round)
    val scopeLine = Stroke(width = 2f, cap = StrokeCap.Round)
    val ledLow = lerp(accent, Color(0xFF3DDC84), 0.35f)
    val ledMid = Color(0xFFFFD54F)
    val ledHigh = Color(0xFFFF5252)
    val heat = IntArray(256) { index ->
        val t = index / 255f
        val color = when {
            t < 0.35f -> lerp(Color.Transparent, accent.copy(alpha = 0.75f), t / 0.35f)
            t < 0.75f -> lerp(accent.copy(alpha = 0.75f), bright, (t - 0.35f) / 0.4f)
            else -> lerp(bright, Color.White, (t - 0.75f) / 0.25f)
        }
        color.toArgb()
    }
}

private class AnalyzerLabels(
    val levels: List<androidx.compose.ui.text.TextLayoutResult>,
    val frequencies: List<androidx.compose.ui.text.TextLayoutResult>
)

private fun DrawScope.drawBars(frame: SpectrumFrame, palette: SpectrumPalette) {
    val count = frame.bars.size
    val slot = size.width / count
    val barWidth = slot * 0.72f
    val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
    for (band in 0 until count) {
        val height = frame.bars[band] * size.height
        val left = band * slot + (slot - barWidth) / 2f
        if (height > 0.5f) {
            drawRoundRect(palette.barBrush, Offset(left, size.height - height), Size(barWidth, height), radius)
        }
        val peakY = size.height - frame.barPeaks[band] * size.height
        drawRect(palette.peak, Offset(left, peakY - 1.5f), Size(barWidth, 1.5f))
    }
}

private fun DrawScope.drawMirror(frame: SpectrumFrame, palette: SpectrumPalette) {
    val count = frame.bars.size
    val slot = size.width / (count * 2)
    val barWidth = slot * 0.7f
    val center = size.width / 2f
    val middle = size.height / 2f
    val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
    for (band in 0 until count) {
        val half = frame.bars[band] * middle
        if (half < 0.5f) continue
        val offset = band * slot + (slot - barWidth) / 2f
        val top = Offset(0f, middle - half)
        val barSize = Size(barWidth, half * 2f)
        drawRoundRect(palette.barBrush, top.copy(x = center + offset), barSize, radius)
        drawRoundRect(palette.barBrush, top.copy(x = center - offset - barWidth), barSize, radius)
    }
}

private fun DrawScope.drawLed(frame: SpectrumFrame, palette: SpectrumPalette) {
    val columns = LED_COLUMNS
    val bandsPerColumn = frame.bars.size / columns
    val slot = size.width / columns
    val cellWidth = slot * 0.78f
    val cellHeight = size.height / LED_ROWS
    val cellGap = cellHeight * 0.28f
    for (column in 0 until columns) {
        var level = 0f
        var peak = 0f
        for (offset in 0 until bandsPerColumn) {
            val band = column * bandsPerColumn + offset
            if (frame.bars[band] > level) level = frame.bars[band]
            if (frame.barPeaks[band] > peak) peak = frame.barPeaks[band]
        }
        val lit = (level * LED_ROWS).toInt()
        val peakRow = (peak * LED_ROWS).toInt().coerceAtMost(LED_ROWS - 1)
        val left = column * slot + (slot - cellWidth) / 2f
        for (row in 0 until LED_ROWS) {
            val top = size.height - (row + 1) * cellHeight + cellGap / 2f
            val color = when {
                row >= LED_ROWS - 2 -> palette.ledHigh
                row >= LED_ROWS * 2 / 3 -> palette.ledMid
                else -> palette.ledLow
            }
            val alpha = when {
                row < lit -> 1f
                row == peakRow && peak > 0.02f -> 0.95f
                else -> 0.09f
            }
            drawRect(color.copy(alpha = alpha), Offset(left, top), Size(cellWidth, cellHeight - cellGap))
        }
    }
}

private fun DrawScope.drawCurve(frame: SpectrumFrame, palette: SpectrumPalette, path: Path) {
    val count = frame.bars.size
    val step = size.width / (count - 1)
    path.reset()
    path.moveTo(0f, size.height)
    var previousX = 0f
    var previousY = size.height - frame.bars[0] * size.height
    path.lineTo(previousX, previousY)
    for (band in 1 until count) {
        val x = band * step
        val y = size.height - frame.bars[band] * size.height
        path.quadraticTo(previousX, previousY, (previousX + x) / 2f, (previousY + y) / 2f)
        previousX = x
        previousY = y
    }
    path.lineTo(size.width, previousY)
    path.lineTo(size.width, size.height)
    path.close()
    drawPath(path, palette.curveFill)
    drawPath(path, palette.bright, style = palette.curveStroke)
}

private fun DrawScope.drawAnalyzer(frame: SpectrumFrame, palette: SpectrumPalette, labels: AnalyzerLabels?) {
    val labelWidth = labels?.levels?.maxOf { it.size.width }?.plus(6f) ?: 0f
    val labelHeight = labels?.frequencies?.maxOf { it.size.height }?.toFloat() ?: 0f
    val plotLeft = labelWidth
    val plotWidth = size.width - plotLeft
    val plotHeight = size.height - labelHeight - 2f
    val range = -SpectrumFrame.RTA_FLOOR_DB
    ANALYZER_GRID_DB.forEachIndexed { index, db ->
        val y = plotHeight * (-db / range)
        drawLine(palette.grid, Offset(plotLeft, y), Offset(size.width, y), strokeWidth = 1f)
        labels?.levels?.get(index)?.let { layout ->
            val top = (y - layout.size.height / 2f).coerceIn(0f, plotHeight - layout.size.height)
            drawText(layout, topLeft = Offset(0f, top))
        }
    }
    val count = frame.rtaDb.size
    val slot = plotWidth / count
    val barWidth = slot * 0.8f
    for (band in 0 until count) {
        val level = ((frame.rtaDb[band] + range) / range).coerceIn(0f, 1f)
        val peak = ((frame.rtaPeakDb[band] + range) / range).coerceIn(0f, 1f)
        val left = plotLeft + band * slot + (slot - barWidth) / 2f
        val height = level * plotHeight
        if (height > 0.5f) drawRect(palette.barBrush, Offset(left, plotHeight - height), Size(barWidth, height))
        if (peak > 0f) drawRect(palette.peak, Offset(left, plotHeight - peak * plotHeight - 1.5f), Size(barWidth, 1.5f))
    }
    labels?.frequencies?.forEachIndexed { index, layout ->
        val band = ANALYZER_FREQUENCY_LABELS[index].first
        val center = plotLeft + band * slot + slot / 2f
        val left = (center - layout.size.width / 2f).coerceIn(plotLeft, size.width - layout.size.width)
        drawText(layout, topLeft = Offset(left, size.height - layout.size.height))
    }
}

private fun DrawScope.drawScope(frame: SpectrumFrame, palette: SpectrumPalette, path: Path) {
    val middle = size.height / 2f
    val points = frame.scope.size
    val step = size.width / (points - 1)
    drawLine(palette.grid, Offset(0f, middle), Offset(size.width, middle), strokeWidth = 1f)
    path.reset()
    for (index in 0 until points) {
        val y = middle - frame.scope[index].coerceIn(-1f, 1f) * middle * 0.95f
        if (index == 0) path.moveTo(0f, y) else path.lineTo(index * step, y)
    }
    drawPath(path, palette.dim, style = palette.scopeGlow)
    drawPath(path, palette.bright, style = palette.scopeLine)
}

private class SpectrogramCanvas(private val palette: SpectrumPalette) {
    private val rows = SpectrumFrame.SPECTROGRAM_ROWS
    private val bitmap: Bitmap = Bitmap.createBitmap(SPECTROGRAM_COLUMNS, rows, Bitmap.Config.ARGB_8888)
    private val image: ImageBitmap = bitmap.asImageBitmap()
    private val column = IntArray(rows)
    private var writeColumn = 0
    private var lastSequence = -1L

    fun draw(scope: DrawScope, frame: SpectrumFrame) {
        if (frame.spectrogramSequence != lastSequence) {
            lastSequence = frame.spectrogramSequence
            for (row in 0 until rows) {
                val value = frame.spectrogramColumn[row]
                column[rows - 1 - row] = palette.heat[(value * 255f).toInt().coerceIn(0, 255)]
            }
            bitmap.setPixels(column, 0, 1, writeColumn, 0, 1, rows)
            image.prepareToDraw()
            writeColumn = (writeColumn + 1) % SPECTROGRAM_COLUMNS
        }
        with(scope) {
            val height = size.height.toInt().coerceAtLeast(1)
            val columnWidth = size.width / SPECTROGRAM_COLUMNS
            val olderColumns = SPECTROGRAM_COLUMNS - writeColumn
            val olderWidth = (olderColumns * columnWidth).toInt()
            drawImage(
                image,
                srcOffset = IntOffset(writeColumn, 0),
                srcSize = IntSize(olderColumns, rows),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(olderWidth, height)
            )
            if (writeColumn > 0) {
                drawImage(
                    image,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(writeColumn, rows),
                    dstOffset = IntOffset(olderWidth, 0),
                    dstSize = IntSize(size.width.toInt() - olderWidth, height)
                )
            }
        }
    }
}

private const val LED_COLUMNS = 24
private const val LED_ROWS = 14
private const val SPECTROGRAM_COLUMNS = 240
private val ANALYZER_GRID_DB = listOf(0, -24, -48, -72)
private val ANALYZER_FREQUENCY_LABELS = listOf(1 to "25", 7 to "100", 13 to "400", 17 to "1k", 23 to "4k", 27 to "10k")
