package com.luc4n3x.levyra.desktop.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.desktop.app.state.ExploreUiState
import com.luc4n3x.levyra.desktop.app.ui.components.Artwork
import com.luc4n3x.levyra.desktop.app.ui.components.ExpressiveSectionHeader
import com.luc4n3x.levyra.desktop.app.ui.components.ScrollableColumn
import com.luc4n3x.levyra.desktop.app.ui.components.SectionTints
import com.luc4n3x.levyra.desktop.app.ui.components.ShelfRow
import com.luc4n3x.levyra.desktop.app.ui.components.horizontalShelfBleed
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.app.ui.theme.LevyraBrand
import com.luc4n3x.levyra.desktop.core.model.Track
import com.luc4n3x.levyra.desktop.core.news.NewsStory
import com.luc4n3x.levyra.ui.i18n.dailyTop100Copy
import com.luc4n3x.levyra.ui.i18n.discoveryCopy
import com.luc4n3x.levyra.ui.i18n.exploreEditorialCopy
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import com.luc4n3x.levyra.ui.i18n.LevyraStrings as SharedStrings

@Composable
fun ExploreScreen(
    state: ExploreUiState,
    contentCountry: String,
    onLoad: (String, String) -> Unit,
    onPlay: (List<Track>, Int) -> Unit,
    onOpenStory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val code = strings.languageCode
    val locale = remember(code) { Locale.forLanguageTag(code) }
    val editorial = remember(code) { exploreEditorialCopy(code) }
    val top100 = remember(code) { dailyTop100Copy(code) }
    val newRelease = remember(code) { discoveryCopy(code).reasonNewRelease }
    val allLabel = remember(code) { SharedStrings.forCode(code).all }
    LaunchedEffect(contentCountry, code) { onLoad(contentCountry, code) }
    var worldReleases by remember { mutableStateOf(false) }
    var source by remember { mutableStateOf("") }
    val releases = if (worldReleases) state.worldReleases else state.localReleases
    val sources = remember(state.news) { state.news.map { it.source }.distinct() }
    val stories = remember(state.news, source) { state.news.filter { source.isBlank() || it.source == source } }
    val today = remember(locale) { LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)) }

    ScrollableColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        item {
            Column {
                Text(strings.navDiscover, style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold))
                Text(today.replaceFirstChar { it.titlecase(locale) }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (state.localReleases.isNotEmpty() || state.worldReleases.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ExpressiveSectionHeader(LevyraIcons.Disc, SectionTints.Lavender, editorial.releasesTitle)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ToggleChip(countryName(state.country, locale), !worldReleases) { worldReleases = false }
                        ToggleChip(top100.global, worldReleases) { worldReleases = true }
                    }
                    ShelfRow(
                        modifier = Modifier.horizontalShelfBleed(32.dp),
                        contentPadding = PaddingValues(horizontal = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(releases, key = { "rel-${it.id}" }) { track ->
                            Column(
                                Modifier.width(190.dp).clip(RoundedCornerShape(18.dp)).clickable(role = Role.Button) {
                                    onPlay(releases, releases.indexOf(track).coerceAtLeast(0))
                                },
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Artwork(track.artworkUrl, Modifier.size(190.dp), cornerRadius = 18.dp)
                                Text(newRelease.uppercase(locale), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
        if (state.charts.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ExpressiveSectionHeader(LevyraIcons.Chart, SectionTints.Amber, top100.title, top100.subtitle)
                    ShelfRow(
                        modifier = Modifier.horizontalShelfBleed(32.dp),
                        contentPadding = PaddingValues(horizontal = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(state.charts, key = { "chart-${it.country}" }) { chart ->
                            ChartCard(
                                count = chart.tracks.size,
                                label = countryName(chart.country, locale),
                                colors = ChartPalettes[state.charts.indexOf(chart) % ChartPalettes.size],
                                onClick = { onPlay(chart.tracks, 0) }
                            )
                        }
                    }
                }
            }
        }
        if (state.news.isNotEmpty()) {
            item {
                val lead = stories.firstOrNull()
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ExpressiveSectionHeader(LevyraIcons.Menu, SectionTints.Sky, editorial.newsTitle)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ToggleChip(allLabel, source.isBlank()) { source = "" }
                        sources.forEach { name -> ToggleChip(name, source == name) { source = name } }
                    }
                    if (lead != null) LeadStory(lead, editorial.minutesAgo, editorial.hoursAgo, editorial.daysAgo, editorial.justNow, locale) { onOpenStory(lead.url) }
                }
            }
            item {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val columns = if (maxWidth > 900.dp) 2 else 1
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        stories.drop(1).chunked(columns).forEach { chunk ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                chunk.forEach { story ->
                                    StoryRow(story, ago(story.publishedAtMs, editorial.minutesAgo, editorial.hoursAgo, editorial.daysAgo, editorial.justNow), locale,
                                        Modifier.weight(1f)) { onOpenStory(story.url) }
                                }
                                repeat(columns - chunk.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) LevyraBrand.onSoft else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) LevyraBrand.soft else MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 9.dp)
    )
}

private val ChartPalettes = listOf(
    listOf(Color(0xFF2E7D32), Color(0xFF66BB6A)),
    listOf(Color(0xFF00695C), Color(0xFF26A69A)),
    listOf(Color(0xFF283593), Color(0xFF5C6BC0)),
    listOf(Color(0xFF6A1B9A), Color(0xFFAB47BC)),
    listOf(Color(0xFFAD1457), Color(0xFFEC407A)),
    listOf(Color(0xFFE65100), Color(0xFFFFA726))
)

@Composable
private fun ChartCard(count: Int, label: String, colors: List<Color>, onClick: () -> Unit) {
    Box(
        Modifier
            .width(220.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(colors))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(20.dp)
    ) {
        Canvas(Modifier.align(Alignment.BottomEnd).size(width = 90.dp, height = 70.dp)) {
            val bars = listOf(0.35f, 0.6f, 0.45f, 0.85f, 0.55f, 1f)
            val barWidth = size.width / (bars.size * 1.6f)
            bars.forEachIndexed { index, height ->
                val h = size.height * height
                drawRoundRect(
                    Color.White.copy(alpha = 0.22f),
                    topLeft = Offset(index * barWidth * 1.6f, size.height - h),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(barWidth / 2f)
                )
            }
        }
        Column(Modifier.align(Alignment.TopStart)) {
            Text("$count", style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp, fontWeight = FontWeight.Bold), color = Color.White)
        }
        Text(label, style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.align(Alignment.BottomStart))
    }
}

@Composable
private fun LeadStory(story: NewsStory, minutes: String, hours: String, days: String, now: String, locale: Locale, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surfaceContainer).clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (story.imageUrl.isNotBlank()) {
            Artwork(story.imageUrl, Modifier.width(420.dp).aspectRatio(16f / 10f), cornerRadius = 0.dp)
        }
        Column(Modifier.weight(1f).padding(28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "${story.source.uppercase(locale)} · ${ago(story.publishedAtMs, minutes, hours, days, now)}",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                color = LevyraBrand.soft
            )
            Text(story.title, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), maxLines = 4, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun StoryRow(story: NewsStory, ago: String, locale: Locale, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surfaceContainer).clickable(role = Role.Button, onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${story.source.uppercase(locale)} · $ago", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp), color = LevyraBrand.soft)
            Text(story.title, style = MaterialTheme.typography.titleSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        if (story.imageUrl.isNotBlank()) Artwork(story.imageUrl, Modifier.size(84.dp), cornerRadius = 16.dp)
    }
}

private fun ago(epochMs: Long, minutes: String, hours: String, days: String, now: String): String {
    if (epochMs <= 0L) return ""
    val elapsed = (System.currentTimeMillis() - epochMs).coerceAtLeast(0L) / 60_000L
    return when {
        elapsed < 1 -> now
        elapsed < 60 -> minutes.replace("{count}", "$elapsed")
        elapsed < 60 * 24 -> hours.replace("{count}", "${elapsed / 60}")
        else -> days.replace("{count}", "${elapsed / (60 * 24)}")
    }
}

private fun countryName(code: String, locale: Locale): String =
    Locale("", code).getDisplayCountry(locale).ifBlank { code }
