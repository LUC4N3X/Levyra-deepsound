package com.luc4n3x.levyra.ui.smartmusic

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.luc4n3x.levyra.domain.AdaptiveListeningCondition
import com.luc4n3x.levyra.domain.AdaptiveListeningLimits
import com.luc4n3x.levyra.domain.AdaptiveListeningSnapshot
import com.luc4n3x.levyra.domain.AdaptiveSensitivity
import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.feature.adaptive.AdaptiveListeningService
import com.luc4n3x.levyra.player.AdaptiveListeningBus
import com.luc4n3x.levyra.player.AdaptiveListeningConfig
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.SmartMusicCopy
import com.luc4n3x.levyra.ui.i18n.SmartMusicText
import com.luc4n3x.levyra.ui.i18n.smartMusicCopy
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import java.util.Locale
import kotlinx.coroutines.delay

private const val GRAPH_FLOOR_DB = -90f
private const val GRAPH_CEILING_DB = -10f

internal fun adaptiveConfigOf(settings: LevyraAudioSettings): AdaptiveListeningConfig = AdaptiveListeningConfig(
    enabled = settings.adaptiveListeningEnabled,
    sensitivity = settings.adaptiveListeningSensitivity,
    maxBoostDb = settings.adaptiveListeningMaxBoostDb
)

@Composable
internal fun AdaptiveListeningAutostart(settings: LevyraAudioSettings) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val config = adaptiveConfigOf(settings)
    val stopped by AdaptiveListeningBus.stoppedByUser.collectAsStateWithLifecycle()
    LaunchedEffect(config, stopped, lifecycleOwner) {
        if (!config.enabled || stopped) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            AdaptiveListeningService.start(context.applicationContext, config)
        }
    }
}

@Composable
internal fun AdaptiveListeningPanel(
    settings: LevyraAudioSettings,
    animated: Boolean,
    onEnabled: (Boolean) -> Unit,
    onSensitivity: (AdaptiveSensitivity) -> Unit,
    onMaxBoost: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val copy = smartMusicCopy(LocalLevyraStrings.current.code)
    val stopped by AdaptiveListeningBus.stoppedByUser.collectAsStateWithLifecycle()
    val enabled = settings.adaptiveListeningEnabled && !stopped
    val snapshot by AdaptiveListeningBus.snapshot.collectAsStateWithLifecycle()
    var consentOpen by rememberSaveable { mutableStateOf(false) }
    val enabler = rememberAdaptiveEnabler(settings, onEnabled)
    AdaptiveCard(enabled = enabled, animated = animated, modifier = modifier) {
        AdaptiveHeader(
            enabled = enabled,
            adapting = enabled && snapshot.condition == AdaptiveListeningCondition.Adapting,
            animated = animated,
            copy = copy,
            onToggle = { checked -> if (checked) consentOpen = true else onEnabled(false) }
        )
        AnimatedVisibility(visible = enabled) {
            AdaptiveDetails(
                settings = settings,
                snapshot = snapshot,
                animated = animated,
                copy = copy,
                onGrant = enabler.grant,
                onSensitivity = onSensitivity,
                onMaxBoost = onMaxBoost
            )
        }
    }
    if (consentOpen) {
        AdaptiveConsentDialog(
            copy = copy,
            onAllow = {
                consentOpen = false
                enabler.request()
            },
            onDismiss = { consentOpen = false }
        )
    }
}

private class AdaptiveEnabler(val request: () -> Unit, val grant: () -> Unit)

@Composable
private fun rememberAdaptiveEnabler(settings: LevyraAudioSettings, onEnabled: (Boolean) -> Unit): AdaptiveEnabler {
    val context = LocalContext.current
    val enable = {
        AdaptiveListeningBus.markStoppedByUser(false)
        onEnabled(true)
        AdaptiveListeningService.start(context.applicationContext, adaptiveConfigOf(settings).copy(enabled = true))
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) enable() else AdaptiveListeningBus.reset(AdaptiveListeningSnapshot(AdaptiveListeningCondition.PermissionMissing))
    }
    val grant = { launcher.launch(Manifest.permission.RECORD_AUDIO) }
    val request = {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted) enable() else grant()
    }
    return AdaptiveEnabler(request, grant)
}

@Composable
private fun AdaptiveCard(enabled: Boolean, animated: Boolean, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val corner by animateDpAsState(
        targetValue = if (enabled) 36.dp else 24.dp,
        animationSpec = if (animated) spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow) else snap(),
        label = "adaptive-card-corner"
    )
    Surface(
        color = if (enabled) scheme.surfaceContainerHigh else scheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = corner, bottomEnd = 18.dp, bottomStart = corner),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp).then(if (animated) Modifier.animateContentSize() else Modifier),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }
}

@Composable
private fun AdaptiveHeader(enabled: Boolean, adapting: Boolean, animated: Boolean, copy: SmartMusicCopy, onToggle: (Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        AmbientGlyph(active = adapting, animated = animated)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    copy[SmartMusicText.AdaptiveTitle],
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(color = scheme.tertiaryContainer, contentColor = scheme.onTertiaryContainer, shape = RoundedCornerShape(8.dp)) {
                    Text(copy[SmartMusicText.AdaptiveExperimental], style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Text(copy[SmartMusicText.AdaptiveSubtitle], style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun AdaptiveDetails(
    settings: LevyraAudioSettings,
    snapshot: AdaptiveListeningSnapshot,
    animated: Boolean,
    copy: SmartMusicCopy,
    onGrant: () -> Unit,
    onSensitivity: (AdaptiveSensitivity) -> Unit,
    onMaxBoost: (Float) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ConditionPill(snapshot.condition, copy, animated)
        if (snapshot.condition == AdaptiveListeningCondition.PermissionMissing) {
            FilledTonalButton(onClick = onGrant, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(copy[SmartMusicText.AdaptiveGrant])
            }
        }
        AmbientGraph(
            snapshot = snapshot,
            description = copy[SmartMusicText.AdaptiveHistory],
            modifier = Modifier.fillMaxWidth().aspectRatio(2.1f)
        )
        AdaptiveReadout(snapshot, copy)
        Text(copy[SmartMusicText.AdaptiveRelativeNote], style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        SensitivitySelector(settings.adaptiveListeningSensitivity, animated, copy, onSensitivity)
        MaxBoostControl(settings.adaptiveListeningMaxBoostDb, copy, onMaxBoost)
        Text(copy[SmartMusicText.AdaptiveHeadphonesNote], style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
    }
}

@Composable
private fun AdaptiveConsentDialog(copy: SmartMusicCopy, onAllow: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(copy[SmartMusicText.AdaptiveConsentTitle]) },
        text = { Text(copy[SmartMusicText.AdaptiveConsentBody]) },
        confirmButton = { TextButton(onClick = onAllow) { Text(copy[SmartMusicText.AdaptiveConsentAllow]) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(copy[SmartMusicText.AdaptiveConsentCancel]) } }
    )
}

@Composable
private fun AmbientGlyph(active: Boolean, animated: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val spin = if (active && animated) {
        rememberInfiniteTransition(label = "adaptive-glyph").animateFloat(
            0f, 360f, infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Restart), label = "adaptive-glyph-spin"
        ).value
    } else 0f
    Box(
        Modifier.size(44.dp).clip(rememberOrbitMorph(active, animated, spin)).background(if (active) scheme.primary else scheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(22.dp)) {
            val color = if (active) scheme.onPrimary else scheme.primary
            val bars = floatArrayOf(0.35f, 0.7f, 1f, 0.6f, 0.3f)
            val step = size.width / bars.size
            bars.forEachIndexed { index, value ->
                val x = step * (index + 0.5f)
                val half = size.height * value / 2f
                drawLine(color, Offset(x, center.y - half), Offset(x, center.y + half), strokeWidth = step * 0.5f, cap = StrokeCap.Round)
            }
        }
    }
}

private fun conditionText(condition: AdaptiveListeningCondition): SmartMusicText = when (condition) {
    AdaptiveListeningCondition.Off -> SmartMusicText.CondOff
    AdaptiveListeningCondition.Waiting -> SmartMusicText.CondWaiting
    AdaptiveListeningCondition.Calibrating -> SmartMusicText.CondCalibrating
    AdaptiveListeningCondition.Listening -> SmartMusicText.CondListening
    AdaptiveListeningCondition.Adapting -> SmartMusicText.CondAdapting
    AdaptiveListeningCondition.SpeakerOutput -> SmartMusicText.CondSpeaker
    AdaptiveListeningCondition.PlaybackBleed -> SmartMusicText.CondBleed
    AdaptiveListeningCondition.Unstable -> SmartMusicText.CondUnstable
    AdaptiveListeningCondition.Overload -> SmartMusicText.CondOverload
    AdaptiveListeningCondition.MicrophoneSilent -> SmartMusicText.CondSilent
    AdaptiveListeningCondition.PermissionMissing -> SmartMusicText.CondPermission
    AdaptiveListeningCondition.Unavailable -> SmartMusicText.CondUnavailable
}

@Composable
private fun ConditionPill(condition: AdaptiveListeningCondition, copy: SmartMusicCopy, animated: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val reliable = condition == AdaptiveListeningCondition.Listening || condition == AdaptiveListeningCondition.Adapting
    val blocked = condition in setOf(
        AdaptiveListeningCondition.SpeakerOutput, AdaptiveListeningCondition.PlaybackBleed, AdaptiveListeningCondition.Overload,
        AdaptiveListeningCondition.MicrophoneSilent, AdaptiveListeningCondition.PermissionMissing, AdaptiveListeningCondition.Unavailable
    )
    val dot = when {
        reliable -> scheme.primary
        blocked -> scheme.error
        else -> scheme.tertiary
    }
    Surface(color = scheme.surfaceContainerHighest, shape = RoundedCornerShape(18.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp).semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(dot))
            AnimatedContent(
                targetState = condition,
                transitionSpec = { if (animated) fadeIn() togetherWith fadeOut() else fadeIn(snap()) togetherWith fadeOut(snap()) },
                label = "adaptive-condition"
            ) { value ->
                Text(copy[conditionText(value)], style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
            }
        }
    }
}

@Composable
private fun AmbientGraph(snapshot: AdaptiveListeningSnapshot, description: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val history = snapshot.history
    val boosts = snapshot.boostHistory
    val reference = snapshot.referenceDb
    Canvas(modifier.clip(RoundedCornerShape(20.dp)).background(scheme.surfaceContainerLow).semantics { contentDescription = description }) {
        val range = GRAPH_CEILING_DB - GRAPH_FLOOR_DB
        fun y(db: Float) = size.height - (db.coerceIn(GRAPH_FLOOR_DB, GRAPH_CEILING_DB) - GRAPH_FLOOR_DB) / range * size.height
        val step = size.width / (60 - 1).toFloat()
        for (line in 1..3) {
            val gy = size.height * line / 4f
            drawLine(scheme.outlineVariant.copy(alpha = 0.3f), Offset(0f, gy), Offset(size.width, gy), 1.dp.toPx())
        }
        if (boosts.isNotEmpty()) {
            val area = Path()
            val offset = 60 - boosts.size
            area.moveTo(step * offset, size.height)
            boosts.forEachIndexed { index, boost ->
                val bx = step * (offset + index)
                area.lineTo(bx, size.height - boost / AdaptiveListeningLimits.MAX_BOOST_DB * size.height * 0.35f)
            }
            area.lineTo(step * (offset + boosts.lastIndex), size.height)
            area.close()
            drawPath(area, Brush.verticalGradient(listOf(scheme.tertiary.copy(alpha = 0.45f), scheme.tertiary.copy(alpha = 0.05f))))
        }
        if (reference.isFinite()) {
            drawLine(
                scheme.outline,
                Offset(0f, y(reference)),
                Offset(size.width, y(reference)),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
            )
        }
        if (history.size >= 2) {
            val line = Path()
            val offset = 60 - history.size
            history.forEachIndexed { index, value ->
                val px = step * (offset + index)
                if (index == 0) line.moveTo(px, y(value)) else line.lineTo(px, y(value))
            }
            drawPath(line, scheme.primary, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            drawCircle(scheme.primary, 5.dp.toPx(), Offset(step * 59, y(history.last())))
        }
    }
}

@Composable
private fun AdaptiveReadout(snapshot: AdaptiveListeningSnapshot, copy: SmartMusicCopy) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var applied by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                applied = AdaptiveListeningBus.appliedBoostDb
                delay(200L)
            }
        }
    }
    val lift by animateFloatAsState(applied, label = "adaptive-lift")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        ReadoutCell(copy[SmartMusicText.AdaptiveLevelNow], dbfs(snapshot.ambientDb), Modifier.weight(1f))
        ReadoutCell(copy[SmartMusicText.AdaptiveReference], dbfs(snapshot.referenceDb), Modifier.weight(1f))
        ReadoutCell(copy[SmartMusicText.AdaptiveBoostNow], String.format(Locale.ROOT, "+%.1f dB", lift), Modifier.weight(1f))
    }
}

private fun dbfs(value: Float): String = if (value.isFinite()) String.format(Locale.ROOT, "%.0f dBFS", value) else "—"

@Composable
private fun ReadoutCell(label: String, value: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(scheme.surfaceContainerHighest.copy(alpha = 0.6f)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = scheme.onSurface, maxLines = 1)
    }
}

private fun sensitivityLabel(value: AdaptiveSensitivity): SmartMusicText = when (value) {
    AdaptiveSensitivity.LOW -> SmartMusicText.AdaptiveLow
    AdaptiveSensitivity.BALANCED -> SmartMusicText.AdaptiveBalanced
    AdaptiveSensitivity.HIGH -> SmartMusicText.AdaptiveHigh
}

@Composable
private fun SensitivitySelector(
    selected: AdaptiveSensitivity,
    animated: Boolean,
    copy: SmartMusicCopy,
    onSensitivity: (AdaptiveSensitivity) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalLevyraHaptics.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(copy[SmartMusicText.AdaptiveSensitivity], style = MaterialTheme.typography.labelLarge, color = scheme.onSurface)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            AdaptiveSensitivity.entries.forEach { value ->
                val active = value == selected
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(if (active) 24.dp else 14.dp))
                        .background(if (active) scheme.secondaryContainer else scheme.surfaceContainerHighest.copy(alpha = 0.55f))
                        .selectable(selected = active, role = Role.RadioButton) {
                            haptics.perform(LevyraHapticAction.Confirm)
                            onSensitivity(value)
                        }
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Box(Modifier.size(14.dp).clip(rememberSelectionMorph(active, animated)).background(if (active) scheme.secondary else scheme.outlineVariant))
                    Text(
                        copy[sensitivityLabel(value)],
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun MaxBoostControl(maxBoostDb: Float, copy: SmartMusicCopy, onMaxBoost: (Float) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var value by remember(maxBoostDb) { mutableFloatStateOf(maxBoostDb) }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(copy[SmartMusicText.AdaptiveMaxBoost], style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface, modifier = Modifier.weight(1f))
            Text(String.format(Locale.ROOT, "+%.1f dB", value), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
        }
        Slider(
            value = value,
            onValueChange = { value = it },
            onValueChangeFinished = { onMaxBoost(value) },
            valueRange = AdaptiveListeningLimits.MIN_BOOST_DB..AdaptiveListeningLimits.MAX_BOOST_DB,
            steps = 9
        )
    }
}
