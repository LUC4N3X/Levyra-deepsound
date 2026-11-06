package com.luc4n3x.levyra.ui.library

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.ListeningChartProjection
import com.luc4n3x.levyra.domain.ListeningPulse
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.MonoArtwork
import com.luc4n3x.levyra.ui.MonoOverline
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.formatLibraryDuration
import com.luc4n3x.levyra.ui.i18n.tasteCardCopy
import com.luc4n3x.levyra.ui.theme.MonoCodeFont
import com.luc4n3x.levyra.ui.theme.MonoDesign
import java.time.format.TextStyle
import java.util.Locale

@Suppress("ComplexMethod", "CognitiveComplexMethod")
@Composable
internal fun MonoPulseCard(pulse: ListeningPulse, onOpen: () -> Unit) {
    val strings = LocalLevyraStrings.current
    val locale = remember(strings.code) { Locale.forLanguageTag(strings.code) }
    val week = remember(pulse.week) { pulse.week.takeLast(ListeningChartProjection.WEEK_DAYS) }
    val weekMs = remember(week) { week.sumOf { it.listenedMs } }
    val delta = pulseDeltaPercent(weekMs, pulse.previous7DaysListenMs)
    val animated = LocalAnimationsEnabled.current
    val grow = remember(week) { Animatable(if (animated) 0f else 1f) }
    LaunchedEffect(week, animated) { if (animated) grow.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) else grow.snapTo(1f) }
    Column(
        Modifier.fillMaxWidth().padding(start = MonoDesign.gutter, end = MonoDesign.gutter, top = 12.dp)
            .clip(MonoDesign.panelShape)
            .drawBehind {
                drawRect(Brush.linearGradient(listOf(MonoDesign.raised, MonoDesign.surface), Offset.Zero, Offset(size.width, size.height)))
                drawRect(Brush.radialGradient(listOf(MonoDesign.accent.copy(alpha = 0.14f), Color.Transparent), center = Offset(size.width, 0f), radius = size.width * 0.95f))
                drawRect(Brush.radialGradient(listOf(MonoDesign.accent.copy(alpha = 0.06f), Color.Transparent), center = Offset(0f, size.height), radius = size.width * 0.8f))
                drawRect(Brush.horizontalGradient(0f to Color.Transparent, 0.5f to Color.White.copy(alpha = 0.08f), 1f to Color.Transparent), size = androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx()))
            }
            .border(1.dp, MonoDesign.hairline, MonoDesign.panelShape)
            .levyraPressable(onClick = onOpen, role = Role.Button, pressedScale = LevyraPressScale.Surface, onClickLabel = strings.listeningInsights)
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        val period = pulse.last7Days
        val taste = remember(strings.code) { tasteCardCopy(strings.code) }
        val persona = tastePersona(period.peakHour)
        val covers = remember(period.topTracks) { period.topTracks.map { it.thumbnailUrl }.filter(String::isNotBlank).distinct().take(4) }
        val artists = remember(period.topArtists) { period.topArtists.map { it.name }.filter(String::isNotBlank).take(3) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.height(56.dp).width((56 + 36 * (covers.size - 1).coerceAtLeast(0)).dp)) {
                covers.forEachIndexed { i, url ->
                    Box(
                        Modifier.padding(start = (i * 36).dp).size(56.dp)
                            .graphicsLayer { rotationZ = (i - (covers.size - 1) / 2f) * 4f }
                            .clip(MonoDesign.cardShape).border(2.dp, MonoDesign.raised, MonoDesign.cardShape)
                    ) { MonoArtwork(url, Modifier.size(56.dp)) }
                }
            }
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            Box(Modifier.size(40.dp).clip(CircleShape).border(1.dp, MonoDesign.hairlineStrong, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = MonoDesign.text, modifier = Modifier.size(18.dp))
            }
        }
        MonoOverline("${taste.yourSound} \u00B7 ${strings.pulseWeek}", color = MonoDesign.accent)
        if (persona == null || period.plays == 0) {
            Text(strings.pulseEmpty, style = MaterialTheme.typography.bodyMedium, color = MonoDesign.textSecondary)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(taste.title(persona), style = MaterialTheme.typography.headlineMedium, color = MonoDesign.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(if (isExplorer(period)) taste.explorer else taste.loyal, style = MaterialTheme.typography.bodyMedium, color = MonoDesign.textSecondary)
                if (artists.isNotEmpty()) {
                    Text(artists.joinToString(" \u00B7 "), style = MaterialTheme.typography.titleSmall, color = MonoDesign.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        val peak = week.maxOfOrNull { it.listenedMs }?.coerceAtLeast(1L) ?: 1L
        val values = remember(week) { week.map { (it.listenedMs.toFloat() / peak).coerceIn(0f, 1f) } }
        val trace = remember { Path() }
        val live = remember { Path() }
        val measure = remember { androidx.compose.ui.graphics.PathMeasure() }
        val sweep = if (animated && weekMs > 0) {
            rememberInfiniteTransition(label = "mono-ecg").animateFloat(
                0f, 1f, infiniteRepeatable(tween(5200, easing = androidx.compose.animation.core.LinearEasing), RepeatMode.Restart), label = "mono-ecg-sweep"
            )
        } else remember { mutableFloatStateOf(1f) }
        Canvas(Modifier.fillMaxWidth().height(112.dp).semantics { contentDescription = strings.formatLibraryDuration(weekMs) }) {
            if (values.isEmpty()) return@Canvas
            val segment = size.width / values.size
            val base = size.height * 0.62f
            val reach = size.height * 0.56f
            val grid = MonoDesign.hairline
            for (i in 1 until values.size) drawLine(grid, Offset(segment * i, 0f), Offset(segment * i, size.height), 1.dp.toPx())
            drawLine(grid, Offset(0f, base), Offset(size.width, base), 1.dp.toPx())
            trace.reset()
            trace.moveTo(0f, base)
            values.forEachIndexed { i, value ->
                val x = segment * i
                val amp = reach * (if (value > 0f) 0.12f + 0.88f * value else 0f) * grow.value
                trace.lineTo(x + segment * 0.30f, base)
                trace.lineTo(x + segment * 0.36f, base + amp * 0.12f)
                trace.lineTo(x + segment * 0.45f, base - amp)
                trace.lineTo(x + segment * 0.53f, base + amp * 0.32f)
                trace.lineTo(x + segment * 0.60f, base)
                trace.cubicTo(x + segment * 0.68f, base, x + segment * 0.72f, base - amp * 0.2f, x + segment * 0.80f, base)
                trace.lineTo(x + segment, base)
            }
            val stroke = 1.5.dp.toPx()
            drawPath(trace, MonoDesign.accent.copy(alpha = 0.16f), style = Stroke(stroke, cap = StrokeCap.Round))
            measure.setPath(trace, false)
            val length = measure.length
            if (length <= 0f) return@Canvas
            val head = length * sweep.value
            val tail = length * 0.32f
            val chunks = 12
            for (c in 0 until chunks) {
                val from = (head - tail + tail * c / chunks).coerceAtLeast(0f)
                val to = (head - tail + tail * (c + 1) / chunks).coerceAtLeast(0f)
                if (to <= from) continue
                live.reset()
                measure.getSegment(from, to, live, true)
                drawPath(live, MonoDesign.accent.copy(alpha = (c + 1f) / chunks), style = Stroke(stroke + 0.5.dp.toPx(), cap = StrokeCap.Round))
            }
            val tip = measure.getPosition(head.coerceIn(0f, length))
            drawCircle(MonoDesign.accent.copy(alpha = 0.25f), 9.dp.toPx(), tip)
            drawCircle(MonoDesign.accent, 3.5.dp.toPx(), tip)
        }
        Row(Modifier.fillMaxWidth().clearAndSetSemantics { }) {
            week.forEachIndexed { i, day ->
                Text(
                    day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale), style = MaterialTheme.typography.labelSmall,
                    color = if (i == week.lastIndex) MonoDesign.accent else MonoDesign.textFaint,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.weight(1f)
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(strings.formatLibraryDuration(period.totalListenMs), strings.formatTrackCount(period.distinctTracks)).forEach { label ->
                Text(
                    label, style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoCodeFont), color = MonoDesign.text,
                    modifier = Modifier.clip(MonoDesign.pillShape).border(1.dp, MonoDesign.hairlineStrong, MonoDesign.pillShape).padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
internal fun MonoShelfStrip(tiles: List<MonoShelfTile>) {
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = MonoDesign.gutter),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(top = 12.dp)
    ) {
        items(tiles, key = { it.title }) { tile ->
            Row(
                Modifier.height(72.dp).clip(MonoDesign.cardShape)
                    .drawBehind { drawRect(MonoDesign.surface) }
                    .border(1.dp, MonoDesign.hairline, MonoDesign.cardShape)
                    .levyraPressable(onClick = tile.onClick, role = Role.Button, pressedScale = LevyraPressScale.Tile, onClickLabel = tile.title)
                    .padding(start = 10.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.size(width = 58.dp, height = 52.dp).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
                    val shown = tile.covers.filter(String::isNotBlank).distinct().take(3)
                    val shape = if (tile.round) CircleShape else MonoDesign.artShape
                    if (shown.isEmpty()) {
                        Box(Modifier.size(40.dp).clip(shape).border(1.dp, MonoDesign.hairlineStrong, shape), contentAlignment = Alignment.Center) {
                            Icon(tile.icon, null, tint = MonoDesign.accent, modifier = Modifier.size(18.dp))
                        }
                    } else {
                        shown.forEachIndexed { index, url ->
                            val offset = index - (shown.size - 1) / 2f
                            Box(
                                Modifier.size(40.dp).graphicsLayer {
                                    rotationZ = offset * 10f
                                    translationX = offset * 9.dp.toPx()
                                    this.shape = shape
                                    clip = true
                                    shadowElevation = 4.dp.toPx()
                                }
                            ) { MonoArtwork(url, Modifier.size(40.dp)) }
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(tile.title, style = MaterialTheme.typography.titleSmall, color = MonoDesign.text, maxLines = 1)
                    Text(tile.detail, style = MaterialTheme.typography.labelSmall, color = MonoDesign.textSecondary, maxLines = 1)
                }
            }
        }
    }
}
