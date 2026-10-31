package com.luc4n3x.levyra.ui.smartaudio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.domain.NightListeningCurve
import com.luc4n3x.levyra.domain.NightListeningPreset
import com.luc4n3x.levyra.player.NightListeningTelemetry
import com.luc4n3x.levyra.player.PlaybackService
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.SmartAudioText
import com.luc4n3x.levyra.ui.i18n.smartAudioCopy
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.delay

private const val CURVE_FLOOR_DB = -60f
private const val CURVE_CEILING_DB = 0f
private const val TELEMETRY_INTERVAL_MS = 120L
private const val MAX_METER_REDUCTION_DB = 20f

internal fun nightPresetLabelKey(preset: NightListeningPreset): SmartAudioText = when (preset) {
    NightListeningPreset.LIGHT -> SmartAudioText.NightLight
    NightListeningPreset.BALANCED -> SmartAudioText.NightBalanced
    NightListeningPreset.INTENSE -> SmartAudioText.NightIntense
    NightListeningPreset.NIGHT -> SmartAudioText.NightDeep
}

@Composable
internal fun NightListeningCard(
    settings: LevyraAudioSettings,
    animated: Boolean,
    onEnabled: (Boolean) -> Unit,
    onPreset: (NightListeningPreset) -> Unit,
    onTrim: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val copy = smartAudioCopy(LocalLevyraStrings.current.code)
    val scheme = MaterialTheme.colorScheme
    var advanced by rememberSaveable { mutableStateOf(false) }
    val telemetry = rememberNightListeningTelemetry(settings.nightListeningEnabled)
    val corner by animateDpAsState(
        targetValue = if (settings.nightListeningEnabled) 36.dp else 24.dp,
        animationSpec = if (animated) spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow) else snap(),
        label = "night-card-corner"
    )
    Surface(
        color = if (settings.nightListeningEnabled) scheme.surfaceContainerHigh else scheme.surfaceContainer,
        shape = RoundedCornerShape(corner),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .then(if (animated) Modifier.animateContentSize() else Modifier),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MoonGlyph(
                    fullness = if (settings.nightListeningEnabled) moonFullness(settings.nightListeningPreset) else 0.12f,
                    animated = animated,
                    color = scheme.primary,
                    track = scheme.surfaceContainerHighest,
                    modifier = Modifier.size(44.dp)
                )
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text(copy[SmartAudioText.NightTitle], style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = scheme.onSurface)
                    Text(copy[SmartAudioText.NightSubtitle], style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
                Switch(checked = settings.nightListeningEnabled, onCheckedChange = onEnabled)
            }
            AnimatedVisibility(visible = settings.nightListeningEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    MoonPresetSelector(settings.nightListeningPreset, animated, onPreset)
                    CompressionCurve(
                        preset = settings.nightListeningPreset,
                        trimDb = settings.nightListeningTrimDb,
                        inputLevelDb = telemetry.inputLevelDb.takeIf { telemetry.processing },
                        animated = animated,
                        contentDescription = copy[SmartAudioText.NightCurve],
                        modifier = Modifier.fillMaxWidth().aspectRatio(1.9f)
                    )
                    CompressionMeter(telemetry, animated)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { advanced = !advanced }
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(copy[SmartAudioText.NightAdvanced], style = MaterialTheme.typography.labelLarge, color = scheme.onSurface, modifier = Modifier.weight(1f))
                        val rotation by animateFloatAsState(if (advanced) 180f else 0f, label = "night-advanced")
                        Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.rotate(rotation))
                    }
                    AnimatedVisibility(visible = advanced) {
                        NightAdvancedControls(settings, onTrim)
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberNightListeningTelemetry(enabled: Boolean): NightListeningTelemetry {
    var telemetry by remember { mutableStateOf(NightListeningTelemetry()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(enabled, lifecycleOwner) {
        if (!enabled) {
            telemetry = NightListeningTelemetry()
            return@LaunchedEffect
        }
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                telemetry = PlaybackService.nightListeningTelemetry
                delay(TELEMETRY_INTERVAL_MS)
            }
        }
    }
    return telemetry
}

private fun moonFullness(preset: NightListeningPreset): Float = when (preset) {
    NightListeningPreset.LIGHT -> 0.28f
    NightListeningPreset.BALANCED -> 0.5f
    NightListeningPreset.INTENSE -> 0.74f
    NightListeningPreset.NIGHT -> 1f
}

@Composable
private fun MoonPresetSelector(
    selected: NightListeningPreset,
    animated: Boolean,
    onPreset: (NightListeningPreset) -> Unit
) {
    val copy = smartAudioCopy(LocalLevyraStrings.current.code)
    val scheme = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        NightListeningPreset.entries.forEach { preset ->
            val active = preset == selected
            val corner by animateDpAsState(
                targetValue = if (active) 30.dp else 16.dp,
                animationSpec = if (animated) spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow) else snap(),
                label = "night-preset-corner"
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 88.dp)
                    .clip(RoundedCornerShape(corner))
                    .background(if (active) scheme.primaryContainer else scheme.surfaceContainerHighest.copy(alpha = 0.55f))
                    .selectable(selected = active, role = Role.RadioButton) { onPreset(preset) }
                    .padding(horizontal = 4.dp, vertical = 12.dp)
            ) {
                MoonGlyph(
                    fullness = moonFullness(preset),
                    animated = animated,
                    color = if (active) scheme.onPrimaryContainer else scheme.primary,
                    track = if (active) scheme.primary.copy(alpha = 0.25f) else scheme.outlineVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(30.dp)
                )
                Text(
                    copy[nightPresetLabelKey(preset)],
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = if (active) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MoonGlyph(
    fullness: Float,
    animated: Boolean,
    color: Color,
    track: Color,
    modifier: Modifier = Modifier
) {
    val phase by animateFloatAsState(
        targetValue = fullness,
        animationSpec = if (animated) spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow) else snap(),
        label = "night-moon-phase"
    )
    Canvas(modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(track, radius, center)
        val terminator = radius * (1f - 2f * phase.coerceIn(0f, 1f))
        val path = Path().apply {
            moveTo(center.x, center.y - radius)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius),
                startAngleDegrees = -90f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            val width = abs(terminator)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(center.x - width, center.y - radius, center.x + width, center.y + radius),
                startAngleDegrees = 90f,
                sweepAngleDegrees = if (terminator >= 0f) -180f else 180f,
                forceMoveTo = false
            )
            close()
        }
        drawPath(path, color)
    }
}

@Composable
private fun CompressionCurve(
    preset: NightListeningPreset,
    trimDb: Float,
    inputLevelDb: Float?,
    animated: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val spec = if (animated) spring<Float>(Spring.DampingRatioNoBouncy, Spring.StiffnessLow) else snap()
    val threshold by animateFloatAsState(preset.thresholdDb, spec, label = "night-threshold")
    val ratio by animateFloatAsState(preset.ratio, spec, label = "night-ratio")
    val knee by animateFloatAsState(preset.kneeDb, spec, label = "night-knee")
    val makeup by animateFloatAsState(preset.makeupDb + trimDb, spec, label = "night-makeup")
    val level by animateFloatAsState(
        targetValue = inputLevelDb?.coerceIn(CURVE_FLOOR_DB, CURVE_CEILING_DB) ?: CURVE_FLOOR_DB,
        animationSpec = if (animated) spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium) else snap(),
        label = "night-level"
    )
    Canvas(modifier.semantics { this.contentDescription = contentDescription }) {
        val range = CURVE_CEILING_DB - CURVE_FLOOR_DB
        fun x(db: Float) = (db - CURVE_FLOOR_DB) / range * size.width
        fun y(db: Float) = size.height - (db - CURVE_FLOOR_DB) / range * size.height
        drawCurveGrid(scheme.outlineVariant.copy(alpha = 0.32f))
        drawLine(scheme.outline.copy(alpha = 0.45f), Offset(0f, size.height), Offset(size.width, 0f), strokeWidth = 1.5.dp.toPx())
        val curve = Path()
        var db = CURVE_FLOOR_DB
        var first = true
        while (db <= CURVE_CEILING_DB) {
            val output = db - NightListeningCurve.gainReductionDb(db, threshold, ratio, knee) + makeup
            if (first) curve.moveTo(x(db), y(output)) else curve.lineTo(x(db), y(output))
            first = false
            db += 1f
        }
        drawPath(curve, scheme.primary, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        if (inputLevelDb != null) {
            val output = level - NightListeningCurve.gainReductionDb(level, threshold, ratio, knee) + makeup
            drawCircle(scheme.primary.copy(alpha = 0.22f), 11.dp.toPx(), Offset(x(level), y(output)))
            drawCircle(scheme.primary, 5.dp.toPx(), Offset(x(level), y(output)))
        }
    }
}

private fun DrawScope.drawCurveGrid(color: Color) {
    val stroke = 1.dp.toPx()
    for (step in 1..3) {
        val fraction = step / 4f
        drawLine(color, Offset(size.width * fraction, 0f), Offset(size.width * fraction, size.height), stroke)
        drawLine(color, Offset(0f, size.height * fraction), Offset(size.width, size.height * fraction), stroke)
    }
}

@Composable
private fun CompressionMeter(telemetry: NightListeningTelemetry, animated: Boolean) {
    val copy = smartAudioCopy(LocalLevyraStrings.current.code)
    val scheme = MaterialTheme.colorScheme
    val fraction by animateFloatAsState(
        targetValue = (telemetry.gainReductionDb / MAX_METER_REDUCTION_DB).coerceIn(0f, 1f),
        animationSpec = if (animated) spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow) else snap(),
        label = "night-reduction"
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(copy[SmartAudioText.NightCompressionNow], style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(
                if (telemetry.processing) String.format(Locale.ROOT, "-%.1f dB", telemetry.gainReductionDb) else copy[SmartAudioText.NightIdle],
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(scheme.surfaceContainerHighest)
        ) {
            Canvas(Modifier.fillMaxWidth().height(8.dp)) {
                drawRoundRect(
                    color = scheme.tertiary,
                    size = Size(size.width * fraction, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun NightAdvancedControls(settings: LevyraAudioSettings, onTrim: (Float) -> Unit) {
    val copy = smartAudioCopy(LocalLevyraStrings.current.code)
    val scheme = MaterialTheme.colorScheme
    val preset = settings.nightListeningPreset
    var trim by remember(settings.nightListeningTrimDb) { mutableFloatStateOf(settings.nightListeningTrimDb) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        NightParameter(copy[SmartAudioText.NightThreshold], String.format(Locale.ROOT, "%.0f dB", preset.thresholdDb))
        NightParameter(copy[SmartAudioText.NightRatio], String.format(Locale.ROOT, "%.1f:1", preset.ratio))
        NightParameter(copy[SmartAudioText.NightAttack], String.format(Locale.ROOT, "%.0f ms", preset.attackMs))
        NightParameter(copy[SmartAudioText.NightRelease], String.format(Locale.ROOT, "%.0f ms", preset.releaseMs))
        NightParameter(copy[SmartAudioText.NightMakeup], String.format(Locale.ROOT, "+%.1f dB", preset.makeupDb))
        Spacer(Modifier.height(4.dp))
        NightParameter(copy[SmartAudioText.NightTrim], String.format(Locale.ROOT, "%+.1f dB", trim))
        Slider(
            value = trim,
            onValueChange = { trim = it },
            onValueChangeFinished = { onTrim(trim) },
            valueRange = NightListeningCurve.MIN_TRIM_DB..NightListeningCurve.MAX_TRIM_DB,
            steps = 17
        )
        Text(copy[SmartAudioText.NightCastNote], style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
    }
}

@Composable
private fun NightParameter(label: String, value: String) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = scheme.onSurface, modifier = Modifier.widthIn(min = 64.dp), textAlign = TextAlign.End)
    }
}
