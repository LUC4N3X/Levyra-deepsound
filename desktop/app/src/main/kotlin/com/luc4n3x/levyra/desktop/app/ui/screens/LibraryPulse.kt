package com.luc4n3x.levyra.desktop.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.luc4n3x.levyra.desktop.app.ui.components.Artwork
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.core.storage.HistoryEntry
import com.luc4n3x.levyra.ui.i18n.formatLibraryDuration
import com.luc4n3x.levyra.ui.i18n.tasteCardCopy
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import com.luc4n3x.levyra.ui.i18n.LevyraStrings as SharedStrings

internal class LibraryPulse(
    val listenedMs: Long,
    val hours: List<Long>,
    val peakHour: Int,
    val plays: Int,
    val distinctTracks: Int,
    val distinctArtists: Int,
    val topArtists: List<String>,
    val covers: List<String>,
    val week: List<Long>,
    val streakDays: Int
)

internal fun libraryPulse(
    history: List<HistoryEntry>,
    now: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault()
): LibraryPulse {
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val start = today.minusDays(6)
    val recent = history.filter {
        val day = Instant.ofEpochMilli(it.playedAt).atZone(zone).toLocalDate()
        !day.isBefore(start) && !day.isAfter(today)
    }
    val hours = LongArray(24)
    val week = LongArray(7)
    recent.forEach { entry ->
        val time = Instant.ofEpochMilli(entry.playedAt).atZone(zone)
        val weight = entry.track.durationMs.coerceAtLeast(1L)
        hours[time.hour] += weight
        week[ChronoUnit.DAYS.between(start, time.toLocalDate()).toInt()] += weight
    }
    val artists = recent.map { it.track.artist.substringBefore(",").trim() }.filter(String::isNotBlank)
    val days = history.map { Instant.ofEpochMilli(it.playedAt).atZone(zone).toLocalDate() }.toSet()
    var cursor = if (today in days) today else today.minusDays(1)
    var streak = 0
    while (cursor in days) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return LibraryPulse(
        listenedMs = recent.sumOf { it.track.durationMs.coerceAtLeast(0L) },
        hours = hours.toList(),
        peakHour = if (recent.isEmpty()) -1 else hours.indices.maxBy { hours[it] },
        plays = recent.size,
        distinctTracks = recent.distinctBy { it.track.id }.size,
        distinctArtists = artists.distinct().size,
        topArtists = artists.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(3).map { it.key },
        covers = recent.groupingBy { it.track.artworkUrl }.eachCount().entries
            .filter { it.key.isNotBlank() }
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key },
        week = week.toList(),
        streakDays = streak
    )
}

@Composable
internal fun LibraryPulseCard(pulse: LibraryPulse, shared: SharedStrings, onOpen: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val copy = remember(shared.code) { tasteCardCopy(shared.code) }
    val locale = remember(shared.code) { Locale.forLanguageTag(shared.code) }
    val accent = LibraryTones.Sky.accent
    val persona = when (pulse.peakHour) {
        in 0..4, in 22..23 -> copy.nightOwl
        in 5..10 -> copy.earlyRiser
        in 11..17 -> copy.daytime
        in 18..21 -> copy.evening
        else -> null
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LibraryShapes.Surface)
            .background(colors.surfaceContainer)
            .clickable(role = Role.Button, onClickLabel = shared.listeningInsights, onClick = onOpen)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            PulseClock(pulse, accent, colors.outlineVariant, shared.formatLibraryDuration(pulse.listenedMs))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "${copy.yourSound} · ${shared.pulseWeek}".uppercase(locale),
                    color = colors.primary,
                    style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.4.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (persona == null || pulse.plays == 0) {
                    Text(shared.pulseEmpty, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(
                        persona,
                        color = colors.onSurface,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        if (pulse.distinctArtists * 10 >= pulse.plays * 4) copy.explorer else copy.loyal,
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (pulse.topArtists.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(width = 36.dp + 22.dp * (pulse.covers.size - 1).coerceAtLeast(0), height = 36.dp)) {
                    pulse.covers.forEachIndexed { index, cover ->
                        Artwork(
                            url = cover,
                            modifier = Modifier
                                .offset(x = 22.dp * index)
                                .zIndex(index.toFloat())
                                .size(36.dp)
                                .border(2.dp, colors.surfaceContainer, CircleShape)
                                .clip(CircleShape),
                            cornerRadius = 18.dp,
                            iconSize = 14.dp
                        )
                    }
                }
                Text(
                    pulse.topArtists.joinToString(" · "),
                    color = colors.onSurface,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        PulseWeekBars(pulse.week, accent, colors.surfaceContainerHighest)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PulsePill(shared.formatTrackCount(pulse.distinctTracks))
            if (pulse.streakDays > 0) PulsePill("${pulse.streakDays} · ${shared.pulseStreak}")
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(colors.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    LevyraIcons.ChevronLeft,
                    contentDescription = null,
                    tint = colors.onSurface,
                    modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = 180f }
                )
            }
        }
    }
}

@Composable
private fun PulseClock(pulse: LibraryPulse, accent: Color, idle: Color, center: String) {
    Box(
        Modifier
            .size(148.dp)
            .drawWithCache {
                val peak = pulse.hours.maxOrNull()?.takeIf { it > 0L } ?: 1L
                val inner = size.minDimension * 0.31f
                val reach = size.minDimension * 0.17f
                val stroke = 5.dp.toPx()
                val base = 3.dp.toPx()
                val c = Offset(size.width / 2f, size.height / 2f)
                onDrawBehind {
                    repeat(24) { hour ->
                        val value = pulse.hours[hour]
                        val angle = Math.toRadians(hour * 15.0 - 90.0)
                        val dx = cos(angle).toFloat()
                        val dy = sin(angle).toFloat()
                        val length = base + reach * value.toFloat() / peak
                        val color = when {
                            hour == pulse.peakHour && value > 0L -> accent
                            value > 0L -> accent.copy(alpha = 0.45f)
                            else -> idle.copy(alpha = 0.55f)
                        }
                        drawLine(
                            color,
                            Offset(c.x + dx * inner, c.y + dy * inner),
                            Offset(c.x + dx * (inner + length), c.y + dy * (inner + length)),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            center,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.padding(horizontal = 34.dp)
        )
    }
}

@Composable
private fun PulseWeekBars(days: List<Long>, accent: Color, track: Color) {
    val peak = days.maxOrNull()?.takeIf { it > 0L } ?: 1L
    val peakIndex = days.indexOf(days.max())
    Row(
        Modifier.fillMaxWidth().height(52.dp).clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEachIndexed { index, value ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight((value.toFloat() / peak).coerceIn(0.08f, 1f))
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when {
                            index == peakIndex && value > 0L -> accent
                            value > 0L -> accent.copy(alpha = 0.4f)
                            else -> track
                        }
                    )
            )
        }
    }
}

@Composable
private fun PulsePill(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.labelLarge,
        maxLines = 1,
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}
