package com.luc4n3x.levyra.ui.library

import android.text.format.DateFormat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.ListeningChartProjection
import com.luc4n3x.levyra.domain.ListeningPulse
import com.luc4n3x.levyra.domain.PulseDay
import com.luc4n3x.levyra.domain.PulseTrack
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraGroupedGridShape
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.formatLibraryDuration
import com.luc4n3x.levyra.ui.theme.LevyraCardDesign
import com.luc4n3x.levyra.ui.theme.LevyraType
import java.text.NumberFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val PulseChartHeight = 128.dp
private val PulseBarMaxWidth = 28.dp
private val PulseBarShape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
private const val PulseTopTrackLimit = 3
private const val PulseBarGrowMs = 520
private const val PulseBarEmphasisMs = 220

@Composable
internal fun LibraryListeningDashboard(
    pulse: ListeningPulse,
    onOpenInsights: (() -> Unit)? = null
) {
    val strings = LocalLevyraStrings.current
    val colors = MaterialTheme.colorScheme
    val locale = remember(strings.code) { Locale.forLanguageTag(strings.code) }
    val week = remember(pulse.week) { pulse.week.takeLast(ListeningChartProjection.WEEK_DAYS) }
    val weekMs = remember(week) { week.sumOf { it.listenedMs } }

    val openSurface = if (onOpenInsights != null) {
        Modifier.levyraPressable(
            onClick = onOpenInsights,
            pressedScale = LevyraPressScale.Surface,
            role = Role.Button,
            onClickLabel = strings.listeningInsights
        )
    } else {
        Modifier
    }

    Surface(
        modifier = Modifier.fillMaxWidth().then(openSurface),
        color = colors.surfaceContainer,
        shape = LevyraCardDesign.SurfaceShape
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            PulseHeader(
                weekMs = weekMs,
                previousMs = pulse.previous7DaysListenMs,
                showArrow = onOpenInsights != null,
                locale = locale
            )
            PulseWeekChart(week = week, locale = locale)
            val topTracks = pulse.last7Days.topTracks.take(PulseTopTrackLimit)
            if (topTracks.isNotEmpty()) {
                PulseTopTracks(tracks = topTracks)
            } else if (weekMs == 0L) {
                Text(strings.pulseEmpty, color = colors.onSurfaceVariant, style = LevyraType.metadata)
            }
            PulseStatTiles(pulse = pulse, locale = locale)
        }
    }
}

@Composable
private fun PulseHeader(weekMs: Long, previousMs: Long, showArrow: Boolean, locale: Locale) {
    val strings = LocalLevyraStrings.current
    val colors = MaterialTheme.colorScheme
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = strings.pulseWeek.uppercase(locale),
                color = colors.primary,
                style = LevyraType.overline,
                maxLines = 1
            )
            Text(
                text = strings.formatLibraryDuration(weekMs),
                color = colors.onSurface,
                style = LevyraType.heroTitle,
                maxLines = 1
            )
            pulseDeltaPercent(weekMs, previousMs)?.let { delta ->
                PulseDeltaLine(delta = delta, locale = locale)
            }
        }
        if (showArrow) {
            Surface(color = colors.primaryContainer, shape = CircleShape) {
                Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = colors.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

internal fun pulseDeltaPercent(currentMs: Long, previousMs: Long): Int? {
    if (previousMs <= 0L) return null
    val ratio = (currentMs - previousMs).toDouble() / previousMs.toDouble()
    return (ratio * 100.0).roundToInt().coerceIn(-999, 999)
}

@Composable
private fun PulseDeltaLine(delta: Int, locale: Locale) {
    val strings = LocalLevyraStrings.current
    val colors = MaterialTheme.colorScheme
    val number = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    val (icon, container, content) = when {
        delta > 0 -> Triple(Icons.AutoMirrored.Rounded.TrendingUp, colors.primaryContainer, colors.onPrimaryContainer)
        delta < 0 -> Triple(Icons.AutoMirrored.Rounded.TrendingDown, colors.surfaceContainerHighest, colors.onSurfaceVariant)
        else -> Triple(Icons.AutoMirrored.Rounded.TrendingFlat, colors.surfaceContainerHighest, colors.onSurfaceVariant)
    }
    val signed = when {
        delta > 0 -> "+" + number.format(delta)
        delta < 0 -> "−" + number.format(abs(delta))
        else -> number.format(0)
    } + "%"
    Row(
        modifier = Modifier
            .padding(top = 4.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(container)
                .padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
            Text(signed, color = content, style = LevyraType.cardTitle.copy(fontWeight = FontWeight.Bold), maxLines = 1)
        }
        Text(
            text = strings.insightsPrevious,
            color = colors.onSurfaceVariant,
            style = LevyraType.caption,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

internal fun pulseDefaultSelection(week: List<PulseDay>): Int {
    if (week.isEmpty()) return -1
    val today = week.lastIndex
    if (week[today].listenedMs > 0L) return today
    return ListeningChartProjection.peakDayIndex(week).takeIf { it >= 0 } ?: today
}

@Composable
private fun PulseWeekChart(week: List<PulseDay>, locale: Locale) {
    val strings = LocalLevyraStrings.current
    val colors = MaterialTheme.colorScheme
    val animationsEnabled = LocalAnimationsEnabled.current
    val fractions = remember(week) { ListeningChartProjection.weekFractions(week, 1L) }
    val peakMs = remember(week) { week.maxOfOrNull { it.listenedMs } ?: 0L }
    val averageMs = remember(week) { if (week.isEmpty()) 0L else week.sumOf { it.listenedMs } / week.size }
    val averageFraction = if (peakMs > 0L) (averageMs.toFloat() / peakMs.toFloat()).coerceIn(0f, 1f) else 0f
    var selected by rememberSaveable(week.lastOrNull()?.date?.toEpochDay()) {
        mutableIntStateOf(pulseDefaultSelection(week))
    }
    var revealed by rememberSaveable { mutableStateOf(!animationsEnabled) }
    val growth = remember { Animatable(if (revealed) 1f else 0f) }
    LaunchedEffect(animationsEnabled) {
        if (animationsEnabled && !revealed) growth.animateTo(1f, tween(PulseBarGrowMs)) else growth.snapTo(1f)
        revealed = true
    }
    val dayFormatter = remember(locale) {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEEdMMM"), locale)
    }
    val accessibleDateFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
    }
    val dashColor = colors.outline.copy(alpha = 0.55f)
    val baselineColor = colors.outlineVariant.copy(alpha = 0.45f)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val selectedDay = week.getOrNull(selected)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = selectedDay?.let { day ->
                    if (selected == week.lastIndex) strings.insightsToday
                    else day.date.format(dayFormatter).replaceFirstChar { it.titlecase(locale) }
                }.orEmpty(),
                color = colors.onSurfaceVariant,
                style = LevyraType.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = selectedDay?.let { strings.formatLibraryDuration(it.listenedMs) }.orEmpty(),
                color = colors.onSurface,
                style = LevyraType.cardTitle.copy(fontWeight = FontWeight.Bold),
                maxLines = 1
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(PulseChartHeight)
                .drawBehind {
                    drawLine(baselineColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
                    if (averageFraction > 0f) {
                        val y = size.height * (1f - averageFraction * growth.value)
                        drawLine(
                            color = dashColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))
                        )
                    }
                },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            week.forEachIndexed { index, day ->
                PulseDayBar(
                    day = day,
                    fraction = fractions.getOrElse(index) { 0f } * growth.value,
                    selected = index == selected,
                    label = day.date.format(accessibleDateFormatter) + ", " + strings.formatLibraryDuration(day.listenedMs),
                    onSelect = { selected = index },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            week.forEachIndexed { index, day ->
                val isSelected = index == selected
                Text(
                    text = day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                    color = if (isSelected) colors.primary else colors.onSurfaceVariant,
                    style = LevyraType.caption.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).clearAndSetSemantics {}
                )
            }
        }
        if (averageMs > 0L) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.semantics(mergeDescendants = true) {}
            ) {
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(2.dp)
                        .drawBehind {
                            drawLine(
                                color = dashColor,
                                start = Offset(0f, size.height / 2f),
                                end = Offset(size.width, size.height / 2f),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
                            )
                        }
                )
                Text(
                    text = strings.pulseProAverage + " · " + strings.formatLibraryDuration(averageMs),
                    color = colors.onSurfaceVariant,
                    style = LevyraType.caption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PulseDayBar(
    day: PulseDay,
    fraction: Float,
    selected: Boolean,
    label: String,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val animationsEnabled = LocalAnimationsEnabled.current
    val emphasis by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = if (animationsEnabled) tween(PulseBarEmphasisMs) else snap(),
        label = "pulse-bar-emphasis"
    )
    val barColor = lerp(colors.primary.copy(alpha = 0.30f), colors.primary, emphasis)
    Box(
        modifier = modifier
            .fillMaxHeight()
            .selectable(selected = selected, role = Role.Tab, onClick = onSelect)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.BottomCenter
    ) {
        if (day.listenedMs > 0L) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .widthIn(max = PulseBarMaxWidth)
                    .fillMaxHeight(fraction.coerceIn(0.02f, 1f))
                    .clip(PulseBarShape)
                    .background(barColor)
            )
        } else {
            Box(
                modifier = Modifier
                    .padding(bottom = 3.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (selected) colors.primary else colors.outlineVariant)
            )
        }
    }
}

@Composable
private fun PulseTopTracks(tracks: List<PulseTrack>) {
    val strings = LocalLevyraStrings.current
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(strings.topTracksTitle, color = colors.onSurface, style = LevyraType.cardTitle.copy(fontWeight = FontWeight.Bold))
        tracks.forEachIndexed { index, track ->
            Row(
                modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = (index + 1).toString(),
                    color = if (index == 0) colors.primary else colors.onSurfaceVariant,
                    style = LevyraType.cardTitle.copy(fontWeight = FontWeight.Black),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(16.dp)
                )
                PulseTrackCover(track.thumbnailUrl)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(track.title, color = colors.onSurface, style = LevyraType.cardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (track.artist.isNotBlank()) {
                        Text(track.artist, color = colors.onSurfaceVariant, style = LevyraType.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Text(
                    text = strings.formatLibraryDuration(track.listenedMs),
                    color = colors.onSurfaceVariant,
                    style = LevyraType.caption,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun PulseTrackCover(url: String) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(LevyraCardDesign.ThumbShape)
            .background(colors.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        if (url.isNotBlank()) {
            AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(44.dp))
        } else {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun PulseStatTiles(pulse: ListeningPulse, locale: Locale) {
    val strings = LocalLevyraStrings.current
    val number = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    val timeFormatter = remember(locale) { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale) }
    val peakHour = pulse.last7Days.peakHour
    val tiles = listOf(
        strings.streakHighlight to
            if (pulse.streakDays > 0) number.format(pulse.streakDays) + " " + strings.recapUnitDays else "—",
        strings.favoriteTimeHighlight to
            if (peakHour in 0..23) LocalTime.of(peakHour, 0).format(timeFormatter) else "—",
        strings.topArtistsTitle to pulse.last7Days.topArtists.firstOrNull()?.name.orEmpty().ifBlank { "—" }
    )
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        tiles.forEachIndexed { index, (label, value) ->
            PulseStatTile(
                label = label,
                value = value,
                modifier = Modifier
                    .weight(if (index == tiles.lastIndex) 1.3f else 1f)
                    .fillMaxHeight()
                    .clip(levyraGroupedGridShape(0, index, 1, tiles.size))
            )
        }
    }
}

@Composable
private fun PulseStatTile(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .heightIn(min = 76.dp)
            .background(colors.surfaceContainerHigh)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = colors.onSurfaceVariant, style = LevyraType.caption, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(
            value,
            color = colors.onSurface,
            style = LevyraType.cardTitle.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
