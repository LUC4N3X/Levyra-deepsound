package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.theme.MonoDesign
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

internal fun DrawScope.drawMonoWave(
    color: Color,
    phase: Float,
    amplitude: Float,
    cycles: Float = 9f,
    strokeWidth: Dp = 2.dp,
    from: Float = 0f,
    to: Float = 1f,
    path: Path = Path()
) {
    if (to <= from || size.width <= 0f) return
    val mid = size.height / 2f
    val reach = size.height / 2f - strokeWidth.toPx()
    path.reset()
    val steps = (size.width * (to - from) / 3f).toInt().coerceIn(8, 240)
    for (i in 0..steps) {
        val t = from + (to - from) * i / steps
        val envelope = 0.3f + 0.7f * abs(sin(t * PI.toFloat() * 2f + phase * 0.35f))
        val y = mid + sin(t * PI.toFloat() * 2f * cycles + phase) * reach * amplitude * envelope
        val x = t * size.width
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, color, style = Stroke(strokeWidth.toPx(), cap = StrokeCap.Round))
}

@Composable
internal fun monoWavePhase(active: Boolean, periodMillis: Int = 2400): Float = monoWavePhaseState(active, periodMillis).value

@Composable
internal fun monoWavePhaseState(active: Boolean, periodMillis: Int = 2400): State<Float> {
    if (!active || !LocalAnimationsEnabled.current) return remember { mutableFloatStateOf(0f) }
    val transition = rememberInfiniteTransition(label = "mono-wave")
    return transition.animateFloat(
        0f, 2f * PI.toFloat(),
        infiniteRepeatable(tween(periodMillis, easing = LinearEasing), RepeatMode.Restart),
        label = "mono-wave-phase"
    )
}

@Composable
internal fun MonoWaveLine(
    playing: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MonoDesign.text,
    cycles: Float = 9f,
    strokeWidth: Dp = 2.dp
) {
    val phase = monoWavePhaseState(playing)
    val amplitude = animateFloatAsState(if (playing) 1f else 0.08f, tween(520), label = "mono-wave-amp")
    val path = remember { Path() }
    Canvas(modifier) { drawMonoWave(color, phase.value, amplitude.value, cycles, strokeWidth, path = path) }
}

@Composable
internal fun MonoOverline(text: String, modifier: Modifier = Modifier, color: Color = MonoDesign.textSecondary) {
    Text(text.uppercase(), modifier = modifier, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
internal fun MonoSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    overline: String? = null,
    action: String? = null,
    overlineColor: Color = MonoDesign.textSecondary,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = MonoDesign.gutter),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (overline != null) MonoOverline(overline, color = overlineColor)
            Text(
                title, style = MaterialTheme.typography.titleLarge, color = MonoDesign.text, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() }
            )
        }
        if (action != null && onAction != null) {
            Text(
                "$action ›", style = MaterialTheme.typography.labelLarge, color = MonoDesign.textSecondary,
                modifier = Modifier.clip(MonoDesign.pillShape).clickable(onClick = onAction).heightIn(min = 32.dp).padding(horizontal = 4.dp, vertical = 7.dp)
            )
        }
    }
}

@Composable
internal fun MonoScreenTitle(title: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(horizontal = MonoDesign.gutter), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title, style = MaterialTheme.typography.headlineLarge, color = MonoDesign.text, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).semantics { heading() }
        )
        trailing?.invoke()
    }
}

@Composable
internal fun MonoPill(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = MonoDesign.pillShape
    Box(
        modifier
            .clip(shape)
            .background(if (selected) MonoDesign.accent else Color.Transparent)
            .border(1.dp, if (selected) MonoDesign.accent else MonoDesign.hairlineStrong, shape)
            .clickable(onClick = onClick)
            .heightIn(min = 36.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) MonoDesign.onAccent else MonoDesign.text, maxLines = 1)
    }
}

internal fun Modifier.monoPanel(shape: androidx.compose.ui.graphics.Shape = MonoDesign.cardShape, color: Color = MonoDesign.surface): Modifier =
    clip(shape).background(color).border(1.dp, MonoDesign.hairline, shape)

internal fun DrawScope.drawMonoDiamond(color: Color, inset: Float = 0.62f) {
    val side = size.minDimension * 0.62f
    val cx = size.width - side * (1f - inset) * 0.9f
    val cy = size.height - side * (1f - inset) * 0.9f
    val path = Path().apply {
        moveTo(cx, cy - side / 2f)
        lineTo(cx + side / 2f, cy)
        lineTo(cx, cy + side / 2f)
        lineTo(cx - side / 2f, cy)
        close()
    }
    drawPath(path, color, style = Stroke(side * 0.09f))
}
