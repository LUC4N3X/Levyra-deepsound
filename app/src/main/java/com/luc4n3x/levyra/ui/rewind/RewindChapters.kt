@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.luc4n3x.levyra.ui.rewind

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.RewindChapter
import com.luc4n3x.levyra.domain.RewindSummary
import com.luc4n3x.levyra.ui.components.LevyraMorphShape
import com.luc4n3x.levyra.ui.experiences.ExperienceArtwork
import com.luc4n3x.levyra.ui.experiences.MusicExperienceStyle
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.MusicExperienceText
import com.luc4n3x.levyra.ui.i18n.musicExperience
import com.luc4n3x.levyra.ui.insights.ListeningReceiptComposer
import com.luc4n3x.levyra.ui.insights.ListeningReceiptModel
import com.luc4n3x.levyra.ui.insights.receiptLabels
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.theme.LocalLevyraVisualCapabilities
import androidx.graphics.shapes.Morph
import java.text.NumberFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import com.luc4n3x.levyra.domain.ListeningInsightsRanges

@Composable
internal fun RewindChapterContent(chapter: RewindChapter, summary: RewindSummary, style: MusicExperienceStyle, active: Boolean) {
    val strings = LocalLevyraStrings.current
    val locale = remember(strings.code) { Locale.forLanguageTag(strings.code) }
    val number = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    val animated = LocalAnimationsEnabled.current && LocalLevyraVisualCapabilities.current.decorativeMotion
    val reveal = remember(chapter, summary.year) { Animatable(if (animated) 0f else 1f) }
    val motion = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    LaunchedEffect(active, animated) {
        if (active) reveal.animateTo(1f, if (animated) motion else snap())
    }
    val scroll = rememberScrollState()
    Column(
        Modifier.widthIn(max = 640.dp).fillMaxHeight().fillMaxWidth().verticalScroll(scroll).padding(horizontal = 24.dp).padding(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        when (chapter) {
            RewindChapter.Overture -> {
                Text(summary.year.toString(), style = MaterialTheme.typography.displayLargeEmphasized, color = style.accent, fontSize = 80.sp, lineHeight = 80.sp)
                RewindOvertureArt(summary, style, reveal.value)
                Text(strings.musicExperience(MusicExperienceText.RewindSubtitle), style = MaterialTheme.typography.displaySmallEmphasized, modifier = Modifier.semantics { heading() })
                RewindCoverage(summary, style)
                Text(strings.musicExperience(MusicExperienceText.Private), style = MaterialTheme.typography.labelLarge, color = style.muted)
            }
            RewindChapter.Time -> {
                RewindEyebrow(strings.receiptMinutesListened, style)
                val minutes = summary.snapshot.metrics.listenedMs / 60_000L
                Box(Modifier.fillMaxWidth().clip(if (style.expressive) MaterialShapes.Puffy.toShape() else style.shape).background(style.accent).padding(horizontal = 32.dp, vertical = 48.dp)) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            number.format((minutes * reveal.value.coerceIn(0f, 1f)).toLong()),
                            color = style.onAccent, style = MaterialTheme.typography.displayLargeEmphasized,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontWeight = FontWeight.Black, maxLines = 1,
                            autoSize = TextAutoSize.StepBased(minFontSize = 40.sp, maxFontSize = 112.sp),
                            modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = "${number.format(minutes)} ${strings.receiptMinutesListened}" }
                        )
                        Text(strings.recapUnitMinutes, color = style.onAccent, style = MaterialTheme.typography.headlineLarge)
                    }
                }
                Text("${number.format(minutes / 60)} ${strings.recapUnitHours}", style = MaterialTheme.typography.displaySmall, color = style.text)
                RewindStatLine(number.format(summary.snapshot.metrics.countedPlays), strings.statPlays, style)
                RewindStatLine(number.format(summary.activeDays), strings.musicExperience(MusicExperienceText.ActiveDays), style)
                RewindStatLine(number.format(summary.snapshot.metrics.distinctTracks), strings.statTracks, style)
            }
            RewindChapter.Artists -> RewindArtists(summary, style, number, reveal.value)
            RewindChapter.Tracks -> RewindTracks(summary, style, number, reveal.value)
            RewindChapter.Albums -> RewindAlbums(summary, style, number)
            RewindChapter.Rhythm -> RewindRhythm(summary, style, locale, number, reveal.value)
            RewindChapter.Timeline -> RewindTimeline(summary, style, locale, number, reveal.value)
            RewindChapter.Poster -> {
                RewindEyebrow("Levyra Rewind / ${summary.year}", style)
                Text(strings.wrappedYearInMusic, style = MaterialTheme.typography.displaySmallEmphasized)
                val content = remember(summary, strings) {
                    val now = System.currentTimeMillis()
                    val zone = ZoneId.systemDefault()
                    ListeningReceiptModel.content(
                        summary.snapshot, receiptLabels(strings, "Levyra Rewind", summary.year.toString(), locale),
                        locale, now, zone, ListeningInsightsRanges.year(summary.year, now, zone)
                    )
                }
                ListeningReceiptComposer(content, false, style.accent, style.text, style.muted, previewMaxHeight = 440.dp)
                RewindCoverage(summary, style)
            }
        }
    }
}

@Composable
private fun ColumnScope.RewindOvertureArt(summary: RewindSummary, style: MusicExperienceStyle, reveal: Float) {
    val tracks = summary.snapshot.topTracks.take(3)
    if (tracks.isEmpty()) return
    val morph = remember { Morph(MaterialShapes.Square.normalized(), MaterialShapes.Puffy.normalized()) }
    BoxWithConstraints(Modifier.widthIn(max = 400.dp).fillMaxWidth().align(Alignment.CenterHorizontally).aspectRatio(1.15f), contentAlignment = Alignment.Center) {
        val edge = maxWidth * 0.65f
        tracks.drop(1).forEachIndexed { index, track ->
            ExperienceArtwork(track.artworkUrl, track.title, style,
                Modifier.size(maxWidth * 0.45f).align(if (index == 0) Alignment.BottomStart else Alignment.TopEnd)
                    .graphicsLayer { rotationZ = if (style.expressive) (if (index == 0) -16f else 14f) * reveal else 0f },
                shape = if (style.expressive) MaterialShapes.Cookie9Sided.toShape() else style.shape)
        }
        val lead = tracks.first()
        ExperienceArtwork(lead.artworkUrl, lead.title, style,
            Modifier.size(edge).graphicsLayer { rotationZ = if (style.expressive) -7f + 7f * reveal else 0f },
            shape = if (style.expressive) LevyraMorphShape(morph, reveal.coerceIn(0f, 1f)) else style.shape)
        Surface(color = style.accent, contentColor = style.onAccent, shape = if (style.expressive) MaterialShapes.SoftBurst.toShape() else style.shape,
            modifier = Modifier.align(Alignment.BottomEnd).size(88.dp)) {
            Box(contentAlignment = Alignment.Center) { Text("↻", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
internal fun RewindEyebrow(text: String, style: MusicExperienceStyle) {
    Text(text, color = style.accent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
}

@Composable
internal fun RewindStatLine(value: String, label: String, style: MusicExperienceStyle) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(value, color = style.accent, style = MaterialTheme.typography.headlineLargeEmphasized, modifier = Modifier.weight(1f))
        Text(label, color = style.muted, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
    HorizontalDivider(color = style.text.copy(alpha = 0.12f))
}

@Composable
internal fun RewindCoverage(summary: RewindSummary, style: MusicExperienceStyle) {
    val strings = LocalLevyraStrings.current
    val formatter = remember(strings.code) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.forLanguageTag(strings.code)) }
    val range = listOfNotNull(summary.firstDay, summary.lastDay).distinct().joinToString(" – ") { it.format(formatter) }
    if (range.isNotEmpty()) Text(strings.musicExperience(MusicExperienceText.Coverage).replace("%s", range), color = style.muted, style = MaterialTheme.typography.bodySmall)
}
