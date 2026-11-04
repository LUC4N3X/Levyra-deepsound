package com.luc4n3x.levyra.ui.moodjourney

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.MoodCurve
import com.luc4n3x.levyra.domain.MoodCurvePreset
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.atelierUppercase
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.SmartMusicCopy
import com.luc4n3x.levyra.ui.i18n.SmartMusicText
import com.luc4n3x.levyra.ui.i18n.smartMusicCopy
import com.luc4n3x.levyra.ui.theme.AtelierDesign
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraHomeDesign
import com.luc4n3x.levyra.ui.theme.LevyraIsAtelier
import com.luc4n3x.levyra.ui.theme.LevyraIsQobuz
import com.luc4n3x.levyra.ui.theme.LevyraSkinManagesLayout
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import com.luc4n3x.levyra.ui.theme.QobuzDesign

private val JourneyPresets = listOf(MoodCurvePreset.Sunrise, MoodCurvePreset.WindDown, MoodCurvePreset.Summit, MoodCurvePreset.Wave)

@Composable
internal fun MoodJourneyEntryCard(onOpen: (MoodCurvePreset?) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val copy = smartMusicCopy(LocalLevyraStrings.current.code)
    val haptics = LocalLevyraHaptics.current
    val open: (MoodCurvePreset?) -> Unit = { preset ->
        haptics.perform(LevyraHapticAction.Confirm)
        onOpen(preset)
    }
    if (LevyraIsAtelier) {
        AtelierMoodJourneyEntry(copy, open, modifier)
        return
    }
    Column(modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button) { open(null) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(copy[SmartMusicText.MoodTitle], style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = scheme.onSurface, modifier = Modifier.semantics { heading() })
                Text(copy[SmartMusicText.MoodSubtitle], style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            Box(Modifier.size(48.dp).clip(LevyraHomeDesign.HeroShape).background(scheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = scheme.onSurface, modifier = Modifier.size(22.dp))
            }
        }
        JourneyCurve()
        JourneyPresetControls(open)
    }
}

@Composable
private fun JourneyPresetControls(onOpen: (MoodCurvePreset) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val copy = smartMusicCopy(LocalLevyraStrings.current.code)
    val corner = when {
        LevyraIsAtelier -> AtelierDesign.cardCorner
        LevyraIsQobuz -> QobuzDesign.cardCorner
        LevyraSkinManagesLayout -> LevyraHomeDesign.HeroCorner.coerceAtMost(16.dp)
        else -> 20.dp
    }
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        JourneyPresets.forEachIndexed { index, preset ->
            val shape = RoundedCornerShape(
                topStart = if (index == 0) corner else 4.dp,
                bottomStart = if (index == 0) corner else 4.dp,
                topEnd = if (index == JourneyPresets.lastIndex) corner else 4.dp,
                bottomEnd = if (index == JourneyPresets.lastIndex) corner else 4.dp
            )
            Column(
                Modifier.weight(1f).fillMaxHeight().heightIn(min = 64.dp).clip(shape).background(scheme.surfaceContainerLow)
                    .clickable(role = Role.Button) { onOpen(preset) }.padding(horizontal = 4.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
            ) {
                JourneyPresetGlyph(preset)
                Text(copy[presetLabel(preset)], style = MaterialTheme.typography.labelMedium, color = scheme.onSurface, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun JourneyCurve() {
    val scheme = MaterialTheme.colorScheme
    val energies = remember { val curve = MoodCurve.preset(MoodCurvePreset.Wave); List(65) { curve.sample(it / 64f).energy } }
    val travel: State<Float>? = if (LocalAnimationsEnabled.current) {
        rememberInfiniteTransition(label = "mood-entry-travel").animateFloat(
            0f, 1f, infiniteRepeatable(tween(6_400, easing = LinearEasing)), label = "mood-entry-playhead"
        )
    } else null
    Box(Modifier.fillMaxWidth().height(64.dp).drawWithCache {
        val inset = 8.dp.toPx()
        val width = size.width - inset * 2
        fun point(index: Int) = Offset(inset + index / 64f * width, inset + (1f - energies[index]) * (size.height - inset * 2))
        val line = Path()
        val bars = Path()
        energies.indices.forEach { index ->
            val p = point(index)
            if (index == 0) line.moveTo(p.x, p.y) else line.lineTo(p.x, p.y)
            if (index % 2 == 0) {
                bars.moveTo(p.x, p.y + 7.dp.toPx())
                bars.lineTo(p.x, size.height - 2.dp.toPx())
            }
        }
        val lineStyle = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
        val barStyle = Stroke(1.dp.toPx(), cap = StrokeCap.Round)
        onDrawBehind {
            drawPath(bars, scheme.onSurface.copy(alpha = 0.06f), style = barStyle)
            drawPath(line, scheme.primary, style = lineStyle)
            val position = (travel?.value ?: 0.5f) * 64f
            val index = position.toInt().coerceIn(0, 63)
            val head = point(index) + (point(index + 1) - point(index)) * (position - index)
            drawCircle(scheme.surface, 6.dp.toPx(), head)
            drawCircle(scheme.primary, 3.5.dp.toPx(), head)
        }
    })
}

@Composable
private fun JourneyPresetGlyph(preset: MoodCurvePreset, tint: Color = Color.Unspecified) {
    val color = tint.takeOrElse { MaterialTheme.colorScheme.onSurfaceVariant }
    val energies = remember(preset) { val curve = MoodCurve.preset(preset); List(17) { curve.sample(it / 16f).energy } }
    Canvas(Modifier.size(width = 28.dp, height = 12.dp)) {
        val path = Path()
        energies.forEachIndexed { index, energy ->
            val x = index / 16f * size.width
            val y = (1f - energy) * size.height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun AtelierMoodJourneyEntry(copy: SmartMusicCopy, open: (MoodCurvePreset?) -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(AtelierDesign.hairline))
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button) { open(null) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                atelierUppercase(copy[SmartMusicText.MoodTitle]),
                style = MaterialTheme.typography.labelMedium,
                color = AtelierDesign.textSecondary,
                modifier = Modifier.weight(1f).semantics { heading() }
            )
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = AtelierDesign.textSecondary, modifier = Modifier.size(18.dp))
        }
        Text(
            copy[SmartMusicText.MoodSubtitle],
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Light),
            color = AtelierDesign.text,
            modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { open(null) }.padding(bottom = 18.dp)
        )
        AtelierJourneyTimeline(Modifier.fillMaxWidth().height(72.dp).clickable(role = Role.Button) { open(null) })
        Box(Modifier.padding(top = 18.dp).fillMaxWidth().height(1.dp).background(AtelierDesign.hairline))
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            JourneyPresets.forEachIndexed { index, preset ->
                if (index > 0) Box(Modifier.padding(vertical = 14.dp).width(1.dp).fillMaxHeight().background(AtelierDesign.hairline))
                Column(
                    Modifier.weight(1f).heightIn(min = 56.dp).clickable(role = Role.Button) { open(preset) }.padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)
                ) {
                    JourneyPresetGlyph(preset, AtelierDesign.textSecondary)
                    Text(copy[presetLabel(preset)], style = MaterialTheme.typography.labelLarge, color = AtelierDesign.text, textAlign = TextAlign.Center, maxLines = 1)
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(AtelierDesign.hairline))
    }
}

@Composable
private fun AtelierJourneyTimeline(modifier: Modifier) {
    val line = AtelierDesign.text
    val tick = AtelierDesign.hairlineStrong
    val accent = AtelierDesign.accent
    val energies = remember { val curve = MoodCurve.preset(MoodCurvePreset.Wave); List(65) { curve.sample(it / 64f).energy } }
    val travel: State<Float>? = if (LocalAnimationsEnabled.current) {
        rememberInfiniteTransition(label = "atelier-journey").animateFloat(
            0f, 1f, infiniteRepeatable(tween(9_000, easing = LinearEasing)), label = "atelier-journey-head"
        )
    } else null
    Box(modifier.drawWithCache {
        val baseline = size.height - 1.dp.toPx()
        val usable = size.height - 14.dp.toPx()
        fun point(index: Int) = Offset(index / 64f * size.width, baseline - 8.dp.toPx() - energies[index] * (usable - 8.dp.toPx()))
        val trace = Path().apply { energies.indices.forEach { index -> point(index).let { if (index == 0) moveTo(it.x, it.y) else lineTo(it.x, it.y) } } }
        val stroke = Stroke(1.25.dp.toPx(), cap = StrokeCap.Round)
        onDrawBehind {
            drawLine(tick, Offset(0f, baseline), Offset(size.width, baseline), 1.dp.toPx())
            for (index in 0..32) {
                val x = index / 32f * size.width
                val height = if (index % 8 == 0) 7.dp.toPx() else 3.dp.toPx()
                drawLine(tick, Offset(x, baseline), Offset(x, baseline - height), 1.dp.toPx())
            }
            drawPath(trace, line.copy(alpha = 0.9f), style = stroke)
            val position = (travel?.value ?: 0.5f) * 64f
            val index = position.toInt().coerceIn(0, 63)
            val head = point(index) + (point(index + 1) - point(index)) * (position - index)
            drawLine(accent.copy(alpha = 0.5f), Offset(head.x, head.y), Offset(head.x, baseline), 1.dp.toPx())
            drawCircle(accent, 3.dp.toPx(), head)
        }
    })
}
