package com.luc4n3x.levyra.ui.smartmusic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.domain.SmartResumeCurve
import com.luc4n3x.levyra.domain.SmartResumeProfile
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.SmartMusicCopy
import com.luc4n3x.levyra.ui.i18n.SmartMusicText
import com.luc4n3x.levyra.ui.i18n.smartMusicCopy
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import kotlin.math.roundToInt

private val ExamplePausesMs = longArrayOf(30_000L, 5L * 60_000L, 60L * 60_000L, 8L * 60L * 60_000L)

@Composable
internal fun SmartResumeCard(
    settings: LevyraAudioSettings,
    animated: Boolean,
    onEnabled: (Boolean) -> Unit,
    onProfile: (SmartResumeProfile) -> Unit,
    onMinPause: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val copy = smartMusicCopy(LocalLevyraStrings.current.code)
    val scheme = MaterialTheme.colorScheme
    val enabled = settings.smartResumeEnabled
    val corner by animateDpAsState(
        targetValue = if (enabled) 36.dp else 24.dp,
        animationSpec = if (animated) spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow) else snap(),
        label = "resume-card-corner"
    )
    Surface(
        color = if (enabled) scheme.surfaceContainerHigh else scheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = corner, topEnd = 18.dp, bottomEnd = corner, bottomStart = 18.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp).then(if (animated) Modifier.animateContentSize() else Modifier),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RewindGlyph(
                    reach = if (enabled) settings.smartResumeProfile.scale / SmartResumeProfile.GENEROUS.scale else 0.15f,
                    animated = animated,
                    modifier = Modifier.size(44.dp)
                )
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text(copy[SmartMusicText.ResumeTitle], style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = scheme.onSurface)
                    Text(copy[SmartMusicText.ResumeSubtitle], style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
                Switch(checked = enabled, onCheckedChange = onEnabled)
            }
            AnimatedVisibility(visible = enabled) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ResumeProfileSelector(settings.smartResumeProfile, animated, copy, onProfile)
                    RewindLadder(settings, animated, copy)
                    MinPauseControl(settings.smartResumeMinPauseSeconds, copy, onMinPause)
                    Text(copy[SmartMusicText.ResumeNote], style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun RewindGlyph(reach: Float, animated: Boolean, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val sweep by animateFloatAsState(
        targetValue = 60f + 250f * reach.coerceIn(0f, 1f),
        animationSpec = if (animated) spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow) else snap(),
        label = "resume-glyph"
    )
    Canvas(modifier) {
        val stroke = size.minDimension * 0.12f
        val inset = stroke
        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
        drawCircle(scheme.surfaceContainerHighest, size.minDimension / 2f)
        drawArc(
            color = scheme.primary,
            startAngle = -90f,
            sweepAngle = -sweep,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        val radius = arcSize.width / 2f
        val angle = Math.toRadians(-90.0 - sweep)
        val tip = Offset(center.x + radius * kotlin.math.cos(angle).toFloat(), center.y + radius * kotlin.math.sin(angle).toFloat())
        drawCircle(scheme.primary, stroke * 0.9f, tip)
    }
}

private fun profileLabel(profile: SmartResumeProfile): SmartMusicText = when (profile) {
    SmartResumeProfile.GENTLE -> SmartMusicText.ResumeGentle
    SmartResumeProfile.BALANCED -> SmartMusicText.ResumeBalanced
    SmartResumeProfile.GENEROUS -> SmartMusicText.ResumeGenerous
}

@Composable
private fun ResumeProfileSelector(
    selected: SmartResumeProfile,
    animated: Boolean,
    copy: SmartMusicCopy,
    onProfile: (SmartResumeProfile) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalLevyraHaptics.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        SmartResumeProfile.entries.forEach { profile ->
            val active = profile == selected
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 88.dp)
                    .clip(RoundedCornerShape(if (active) 28.dp else 16.dp))
                    .background(if (active) scheme.primaryContainer else scheme.surfaceContainerHighest.copy(alpha = 0.55f))
                    .selectable(selected = active, role = Role.RadioButton) {
                        haptics.perform(LevyraHapticAction.Confirm)
                        onProfile(profile)
                    }
                    .padding(horizontal = 6.dp, vertical = 12.dp)
            ) {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(rememberSelectionMorph(active, animated))
                        .background(if (active) scheme.primary else scheme.outlineVariant.copy(alpha = 0.55f))
                )
                Text(
                    copy[profileLabel(profile)],
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
private fun RewindLadder(settings: LevyraAudioSettings, animated: Boolean, copy: SmartMusicCopy) {
    val scheme = MaterialTheme.colorScheme
    val maxRewind = SmartResumeCurve.rewindMs(Long.MAX_VALUE / 4, SmartResumeProfile.GENEROUS, SmartResumeCurve.MIN_PAUSE_SECONDS).coerceAtLeast(1L)
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.fillMaxWidth().height(132.dp)
    ) {
        ExamplePausesMs.forEach { pause ->
            val rewind = SmartResumeCurve.rewindMs(pause, settings.smartResumeProfile, settings.smartResumeMinPauseSeconds)
            val fraction by animateFloatAsState(
                targetValue = (rewind.toFloat() / maxRewind).coerceIn(0f, 1f),
                animationSpec = if (animated) spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow) else snap(),
                label = "resume-ladder"
            )
            val pauseLabel = copy[SmartMusicText.ResumeAfter].replace("{time}", pauseText(pause))
            val rewindLabel = if (rewind == 0L) "0 s" else copy[SmartMusicText.ResumeBack].replace("{n}", formatSeconds(rewind))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f).fillMaxHeight().semantics(mergeDescendants = true) {
                    contentDescription = "$pauseLabel, $rewindLabel"
                }
            ) {
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    val barHeight = (maxHeight - 22.dp) * (0.06f + 0.94f * fraction)
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(rewindLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = scheme.onSurface, maxLines = 1)
                        Box(
                            Modifier
                                .fillMaxWidth(0.62f)
                                .height(barHeight)
                                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 6.dp, bottomEnd = 6.dp))
                                .background(if (rewind == 0L) scheme.outlineVariant.copy(alpha = 0.5f) else scheme.tertiary)
                        )
                    }
                }
                Text(pauseLabel, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant, maxLines = 2, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun MinPauseControl(seconds: Int, copy: SmartMusicCopy, onMinPause: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var value by remember(seconds) { mutableFloatStateOf(seconds.toFloat()) }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            copy[SmartMusicText.ResumeMinPause].replace("{n}", value.roundToInt().toString()),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurface
        )
        Slider(
            value = value,
            onValueChange = { value = it },
            onValueChangeFinished = { onMinPause(value.roundToInt()) },
            valueRange = SmartResumeCurve.MIN_PAUSE_SECONDS.toFloat()..SmartResumeCurve.MAX_PAUSE_SECONDS.toFloat(),
            steps = (SmartResumeCurve.MAX_PAUSE_SECONDS - SmartResumeCurve.MIN_PAUSE_SECONDS) / 5 - 1
        )
    }
}

private fun pauseText(ms: Long): String = when {
    ms < 60_000L -> "${ms / 1000} s"
    ms < 3_600_000L -> "${ms / 60_000L} min"
    else -> "${ms / 3_600_000L} h"
}

private fun formatSeconds(ms: Long): String {
    val tenths = (ms / 100L).toInt()
    return if (tenths % 10 == 0) (tenths / 10).toString() else "${tenths / 10}.${tenths % 10}"
}
