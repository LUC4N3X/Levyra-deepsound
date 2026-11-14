package com.luc4n3x.levyra.ui.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.ListeningPulse
import com.luc4n3x.levyra.domain.PulseDay
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.formatLibraryDuration
import com.luc4n3x.levyra.ui.theme.AtelierDesign
import com.luc4n3x.levyra.ui.theme.QobuzDesign
import com.luc4n3x.levyra.ui.theme.SpotifyDesign
import java.util.Locale

private val SpotifyPulseTileTop = Color(0xFF1ED760)
private val SpotifyPulseTileBottom = Color(0xFF0B3D20)
internal val YouTubeMusicPulseColors = listOf(Color(0xFF8E0E2B), Color(0xFFFF4E45))

internal fun pulseWeekSummary(pulse: ListeningPulse, strings: LevyraStrings): String =
    if (pulse.last7Days.totalListenMs <= 0L) strings.pulseEmpty
    else "${strings.formatLibraryDuration(pulse.last7Days.totalListenMs)} · ${strings.pulseWeek}"

@Composable
private fun PulseWeekBars(
    week: List<PulseDay>,
    today: Color,
    past: Color,
    idle: Color,
    modifier: Modifier,
    gap: Dp = 3.dp,
    rounded: Boolean = true
) {
    val peak = remember(week) { week.maxOfOrNull { it.listenedMs }?.coerceAtLeast(1L) ?: 1L }
    Canvas(modifier.clearAndSetSemantics { }) {
        if (week.isEmpty()) return@Canvas
        val gapPx = gap.toPx()
        val barWidth = (size.width - gapPx * (week.size - 1)) / week.size
        val floor = 2.dp.toPx()
        val radius = if (rounded) CornerRadius(barWidth / 2f) else CornerRadius(1.dp.toPx())
        week.forEachIndexed { index, day ->
            val height = if (day.listenedMs > 0L) (size.height * day.listenedMs / peak).coerceAtLeast(floor) else floor
            val color = when {
                day.listenedMs <= 0L -> idle
                index == week.lastIndex -> today
                else -> past
            }
            drawRoundRect(color, Offset(index * (barWidth + gapPx), size.height - height), Size(barWidth, height), radius)
        }
    }
}

@Composable
internal fun AtelierLibraryPulseStrip(pulse: ListeningPulse, onOpen: () -> Unit) {
    val strings = LocalLevyraStrings.current
    val locale = remember(strings.code) { Locale.forLanguageTag(strings.code) }
    val active = pulse.last7Days.totalListenMs > 0L
    val duration = strings.formatLibraryDuration(pulse.last7Days.totalListenMs)
    val summary = pulseWeekSummary(pulse, strings)
    Column(Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp)
                .clickable(role = Role.Button, onClick = onOpen)
                .semantics(mergeDescendants = true) { contentDescription = "${strings.pulseTitle}, $summary" }
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PulseWeekBars(
                week = pulse.week,
                today = AtelierDesign.accentBright,
                past = AtelierDesign.accent.copy(alpha = 0.6f),
                idle = AtelierDesign.hairlineStrong,
                modifier = Modifier.width(28.dp).height(24.dp),
                gap = 2.dp,
                rounded = false
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f).clearAndSetSemantics { }) {
                Text(
                    strings.pulseTitle.uppercase(locale),
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.2.sp),
                    color = AtelierDesign.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    buildAnnotatedString {
                        if (active) {
                            withStyle(SpanStyle(color = AtelierDesign.accentBright)) { append(duration.uppercase(locale)) }
                            append("  ·  ")
                            append(strings.pulseWeek.uppercase(locale))
                        } else {
                            append(strings.pulseEmpty.uppercase(locale))
                        }
                    },
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.6.sp),
                    color = AtelierDesign.textFaint,
                    maxLines = if (active) 1 else 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = AtelierDesign.accent, modifier = Modifier.padding(start = 4.dp).size(22.dp))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(AtelierDesign.hairline))
    }
}

@Composable
internal fun QobuzLibraryPulseCard(pulse: ListeningPulse, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val strings = LocalLevyraStrings.current
    val locale = remember(strings.code) { Locale.forLanguageTag(strings.code) }
    val active = pulse.last7Days.totalListenMs > 0L
    val shape = RoundedCornerShape(4.dp)
    Column(
        modifier.fillMaxWidth()
            .clip(shape)
            .background(QobuzDesign.surfaceSecondary)
            .clickable(role = Role.Button, onClick = onOpen)
            .semantics(mergeDescendants = true) { contentDescription = "${strings.pulseTitle}, ${pulseWeekSummary(pulse, strings)}" }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clearAndSetSemantics { }) {
            Text(
                strings.pulseTitle.uppercase(locale),
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold),
                color = QobuzDesign.textAccent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = QobuzDesign.iconSecondary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.clearAndSetSemantics { }) {
            Column(Modifier.weight(1f)) {
                if (active) {
                    Text(
                        strings.formatLibraryDuration(pulse.last7Days.totalListenMs),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = QobuzDesign.textPrimary,
                        maxLines = 1
                    )
                }
                Text(
                    if (active) strings.pulseWeek else strings.pulseEmpty,
                    style = MaterialTheme.typography.bodyMedium,
                    color = QobuzDesign.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            PulseWeekBars(
                week = pulse.week,
                today = QobuzDesign.textAccent,
                past = QobuzDesign.textSecondary,
                idle = QobuzDesign.separatorStrong,
                modifier = Modifier.padding(start = 16.dp).width(92.dp).height(40.dp),
                gap = 5.dp
            )
        }
    }
}

@Composable
internal fun SpotifyLibraryPulseRow(pulse: ListeningPulse, onOpen: () -> Unit) {
    val strings = LocalLevyraStrings.current
    val summary = pulseWeekSummary(pulse, strings)
    Row(
        Modifier.fillMaxWidth()
            .levyraPressable(onClick = onOpen, pressedScale = LevyraPressScale.Control, role = Role.Button, onClickLabel = strings.pulseTitle)
            .semantics(mergeDescendants = true) { contentDescription = "${strings.pulseTitle}, $summary" }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(SpotifyDesign.cornerSmall))
                .background(Brush.linearGradient(listOf(SpotifyPulseTileTop, SpotifyPulseTileBottom))),
            contentAlignment = Alignment.Center
        ) {
            PulseWeekBars(
                week = pulse.week,
                today = Color.White,
                past = Color.White.copy(alpha = 0.72f),
                idle = Color.White.copy(alpha = 0.28f),
                modifier = Modifier.width(36.dp).height(28.dp),
                gap = 2.dp
            )
        }
        Column(Modifier.weight(1f).clearAndSetSemantics { }) {
            Text(strings.pulseTitle, color = SpotifyDesign.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Insights, null, tint = SpotifyDesign.greenText, modifier = Modifier.size(14.dp))
                Text(
                    summary, color = SpotifyDesign.subdued, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}
