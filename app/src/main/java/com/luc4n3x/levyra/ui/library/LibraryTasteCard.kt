package com.luc4n3x.levyra.ui.library

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.ListeningPulse
import com.luc4n3x.levyra.domain.PulsePeriod
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.TasteCardCopy
import com.luc4n3x.levyra.ui.i18n.formatLibraryDuration
import com.luc4n3x.levyra.ui.i18n.tasteCardCopy
import com.luc4n3x.levyra.ui.theme.LevyraCardDesign
import com.luc4n3x.levyra.ui.theme.LevyraType
import java.util.Locale

internal enum class TastePersona { NIGHT, MORNING, DAY, EVENING }

internal fun tastePersona(peakHour: Int): TastePersona? = when (peakHour) {
    in 0..4, in 22..23 -> TastePersona.NIGHT
    in 5..10 -> TastePersona.MORNING
    in 11..17 -> TastePersona.DAY
    in 18..21 -> TastePersona.EVENING
    else -> null
}

internal fun isExplorer(period: PulsePeriod): Boolean =
    period.plays > 0 && period.distinctArtists * 10 >= period.plays * 4

internal fun TasteCardCopy.title(persona: TastePersona): String = when (persona) {
    TastePersona.NIGHT -> nightOwl
    TastePersona.MORNING -> earlyRiser
    TastePersona.DAY -> daytime
    TastePersona.EVENING -> evening
}

@Composable
internal fun LibraryTasteCard(
    pulse: ListeningPulse,
    onOpenInsights: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val colors = MaterialTheme.colorScheme
    val copy = remember(strings.code) { tasteCardCopy(strings.code) }
    val locale = remember(strings.code) { Locale.forLanguageTag(strings.code) }
    val period = pulse.last7Days
    val persona = tastePersona(period.peakHour)
    val covers = remember(period.topTracks) {
        period.topTracks.map { it.thumbnailUrl }.filter(String::isNotBlank).distinct().take(3)
    }
    val artists = remember(period.topArtists) {
        period.topArtists.map { it.name }.filter(String::isNotBlank).take(3)
    }
    val accent = com.luc4n3x.levyra.ui.components.levyraUtilityColors(com.luc4n3x.levyra.ui.components.LevyraUtilitySeed).accent
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .levyraPressable(
                onClick = onOpenInsights,
                pressedScale = LevyraPressScale.Surface,
                role = Role.Button,
                onClickLabel = strings.listeningInsights
            ),
        color = colors.surfaceContainer,
        shape = LevyraCardDesign.SurfaceShape
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                ListeningClock(
                    hours = period.hourBuckets,
                    peakHour = period.peakHour,
                    accent = accent,
                    idle = colors.outlineVariant,
                    center = strings.formatLibraryDuration(period.totalListenMs)
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "${copy.yourSound} · ${strings.pulseWeek}".uppercase(locale),
                        color = colors.primary,
                        style = LevyraType.overline,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (persona == null || period.plays == 0) {
                        Text(strings.pulseEmpty, color = colors.onSurfaceVariant, style = LevyraType.metadata)
                    } else {
                        Text(
                            text = copy.title(persona),
                            color = colors.onSurface,
                            style = LevyraType.sectionTitle,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.semantics { heading() }
                        )
                        Text(
                            text = if (isExplorer(period)) copy.explorer else copy.loyal,
                            color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            if (artists.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LibraryCoverStack(covers = covers, ringColor = colors.surfaceContainer, round = true, size = 32.dp, step = 20.dp)
                    Text(
                        text = artists.joinToString(" · "),
                        color = colors.onSurface,
                        style = LevyraType.cardTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            ListeningWeekBars(pulse.week.map { it.listenedMs }, accent, colors.surfaceContainerHighest)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TasteStatPill(strings.formatTrackCount(period.distinctTracks))
                if (pulse.streakDays > 0) TasteStatPill("${pulse.streakDays} · ${strings.pulseStreak}")
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(colors.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = colors.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ListeningClock(hours: List<Long>, peakHour: Int, accent: Color, idle: Color, center: String) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(ListeningClockSize)
            .drawWithCache {
                val peak = hours.maxOrNull()?.takeIf { it > 0L } ?: 1L
                val inner = size.minDimension * 0.31f
                val reach = size.minDimension * 0.17f
                val stroke = 5.dp.toPx()
                val base = 3.dp.toPx()
                val c = Offset(size.width / 2f, size.height / 2f)
                onDrawBehind {
                    repeat(24) { hour ->
                        val value = hours.getOrNull(hour) ?: 0L
                        val ratio = value.toFloat() / peak
                        val angle = Math.toRadians(hour * 15.0 - 90.0)
                        val dx = kotlin.math.cos(angle).toFloat()
                        val dy = kotlin.math.sin(angle).toFloat()
                        val length = base + reach * ratio
                        val color = when {
                            hour == peakHour && value > 0L -> accent
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
            color = colors.onSurface,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.padding(horizontal = 30.dp)
        )
    }
}

@Composable
private fun ListeningWeekBars(days: List<Long>, accent: Color, track: Color) {
    if (days.isEmpty()) return
    val peak = days.maxOrNull()?.takeIf { it > 0L } ?: 1L
    val peakIndex = days.indexOf(days.max())
    Row(
        Modifier.fillMaxWidth().height(ListeningWeekHeight).clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEachIndexed { index, value ->
            val ratio = (value.toFloat() / peak).coerceIn(0.08f, 1f)
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(ratio)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (index == peakIndex && value > 0L) accent else if (value > 0L) accent.copy(alpha = 0.4f) else track)
            )
        }
    }
}

private val ListeningClockSize = 132.dp
private val ListeningWeekHeight = 44.dp

@Composable
private fun TasteStatPill(text: String) {
    val colors = MaterialTheme.colorScheme
    Text(
        text = text,
        color = colors.onSurface,
        style = LevyraType.caption,
        maxLines = 1,
        modifier = Modifier
            .clip(CircleShape)
            .background(colors.surfaceContainerHighest)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}
