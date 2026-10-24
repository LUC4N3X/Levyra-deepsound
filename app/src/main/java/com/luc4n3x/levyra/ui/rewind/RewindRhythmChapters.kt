package com.luc4n3x.levyra.ui.rewind

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.RewindSummary
import com.luc4n3x.levyra.ui.experiences.MusicExperienceStyle
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import java.text.NumberFormat
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun ColumnScope.RewindRhythm(summary: RewindSummary, style: MusicExperienceStyle, locale: Locale, number: NumberFormat, reveal: Float) {
    val strings = LocalLevyraStrings.current
    val hours = summary.snapshot.hourBuckets
    val peak = hours.indices.maxByOrNull { hours[it] } ?: 0
    var selectedHour by rememberSaveable(summary.year) { mutableIntStateOf(peak) }
    RewindEyebrow(strings.insightsRhythm, style)
    Text(strings.insightsActiveAround.replace("%s", String.format(locale, "%02d:00", peak)), style = MaterialTheme.typography.headlineLarge, color = style.text)
    Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(280.dp).clearAndSetSemantics {}) {
            val stroke = 18.dp.toPx()
            val inset = stroke
            val diameter = size.minDimension - inset * 2
            val origin = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            val bounds = Size(diameter, diameter)
            val max = hours.maxOrNull()?.coerceAtLeast(1L) ?: 1L
            hours.forEachIndexed { index, value ->
                val alpha = if (value == 0L) 0.08f else 0.25f + 0.75f * (value.toDouble() / max).toFloat()
                drawArc(style.text.copy(alpha = 0.08f), index * 15f - 90f + 2f, 10f, false, origin, bounds, style = Stroke(stroke, cap = StrokeCap.Round))
                if (value > 0L) drawArc(
                    if (index == selectedHour) style.accent else style.companion.copy(alpha = alpha),
                    index * 15f - 90f + 2f, 10f * reveal.coerceIn(0f, 1f), false, origin, bounds,
                    style = Stroke(if (index == selectedHour) stroke * 1.25f else stroke, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(String.format(locale, "%02d:00", selectedHour), color = style.accent, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black)
            Text("${number.format(hours.getOrElse(selectedHour) { 0L } / 60_000L)} ${strings.recapUnitMinutes}", color = style.muted, style = MaterialTheme.typography.titleMedium)
        }
    }
    Slider(
        value = selectedHour.toFloat(), onValueChange = { selectedHour = it.roundToInt() }, valueRange = 0f..23f, steps = 22,
        colors = SliderDefaults.colors(thumbColor = style.accent, activeTrackColor = style.accent, inactiveTrackColor = style.surface),
        modifier = Modifier.semantics { contentDescription = strings.pulsePeakHour }
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(String.format(locale, "%02d:00", 0), color = style.muted)
        Text(String.format(locale, "%02d:00", 23), color = style.muted)
    }
    val metrics = summary.snapshot.metrics
    RewindStatLine(number.format(metrics.discoveryCount), strings.insightsNewTracks.replace("%s", "").trim(), style)
}

@Composable
internal fun ColumnScope.RewindTimeline(summary: RewindSummary, style: MusicExperienceStyle, locale: Locale, number: NumberFormat, reveal: Float) {
    val strings = LocalLevyraStrings.current
    val months = summary.months
    val peak = months.indices.maxByOrNull { months[it] } ?: 0
    var selectedMonth by rememberSaveable(summary.year) { mutableIntStateOf(peak) }
    RewindEyebrow(strings.insightsActivity, style)
    Text(Month.of(selectedMonth + 1).getDisplayName(TextStyle.FULL_STANDALONE, locale), style = MaterialTheme.typography.displayMedium, color = style.text)
    Text("${number.format(months[selectedMonth] / 60_000L)} ${strings.receiptMinutesListened}", color = style.accent, style = MaterialTheme.typography.headlineSmall)
    val max = months.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    Row(Modifier.fillMaxWidth().height(240.dp).clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
        months.forEachIndexed { index, value ->
            val month = Month.of(index + 1).getDisplayName(TextStyle.SHORT_STANDALONE, locale)
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Box(Modifier.fillMaxWidth().height((200f * (value.toDouble() / max).toFloat() * reveal.coerceIn(0f, 1f)).dp.coerceAtLeast(2.dp))
                    .clip(if (style.expressive) CircleShape else style.shape)
                    .background(if (selectedMonth == index) style.accent else style.companion.copy(alpha = 0.45f)))
                Spacer(Modifier.height(12.dp))
                Text(number.format(index + 1), style = MaterialTheme.typography.labelSmall, color = if (selectedMonth == index) style.accent else style.muted, fontSize = 10.sp)
            }
        }
    }
    Slider(value = selectedMonth.toFloat(), onValueChange = { selectedMonth = it.roundToInt() }, valueRange = 0f..11f, steps = 10,
        colors = SliderDefaults.colors(thumbColor = style.accent, activeTrackColor = style.accent, inactiveTrackColor = style.surface),
        modifier = Modifier.semantics { contentDescription = strings.insightsActivity })
    val peakDay = summary.snapshot.metrics.peakDayEpochMs
    if (peakDay > 0L) {
        val date = java.time.Instant.ofEpochMilli(peakDay).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        Text(strings.mostActiveDayHighlight, style = MaterialTheme.typography.titleMedium, color = style.muted)
        Text(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)), style = MaterialTheme.typography.headlineMedium, color = style.text)
    }
    RewindCoverage(summary, style)
}
