package com.luc4n3x.levyra.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.player.VisualizerAudioProcessor

@Composable
fun WaveformVisualizer(
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    val waveform by VisualizerAudioProcessor.waveformState.collectAsState()

    Canvas(modifier = modifier.fillMaxWidth().height(48.dp)) {
        if (waveform.isEmpty()) return@Canvas
        
        val width = size.width
        val height = size.height
        val barWidth = width / waveform.size
        
        val path = Path()
        val centerY = height / 2f
        
        // Draw fluid wave
        var startX = 0f
        path.moveTo(startX, centerY)
        
        for (i in waveform.indices) {
            val magnitude = (waveform[i] * height * 0.8f).coerceAtMost(height)
            val nextX = startX + barWidth
            val controlX = startX + barWidth / 2f
            
            // Draw upper curve
            val targetY = centerY - magnitude / 2f
            path.quadraticBezierTo(controlX, targetY, nextX, targetY)
            
            startX = nextX
        }
        
        // Draw lower curve back to start
        for (i in waveform.indices.reversed()) {
            val magnitude = (waveform[i] * height * 0.8f).coerceAtMost(height)
            val nextX = startX - barWidth
            val controlX = startX - barWidth / 2f
            
            val targetY = centerY + magnitude / 2f
            path.quadraticBezierTo(controlX, targetY, nextX, targetY)
            
            startX = nextX
        }
        
        path.close()
        drawPath(
            path = path,
            color = color.copy(alpha = 0.6f)
        )
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
