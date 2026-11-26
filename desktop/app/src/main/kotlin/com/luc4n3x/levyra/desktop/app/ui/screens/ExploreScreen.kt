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
import androidx.compose.material3.Icon
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
import com.luc4n3x.levyra.desktop.app.state.OpenChart
import com.luc4n3x.levyra.desktop.app.ui.components.TrackActions
import com.luc4n3x.levyra.ui.i18n.cityChartsCopy
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
    actions: TrackActions,
    onOpenChart: (OpenChart, String) -> Unit,
    onCloseChart: () -> Unit,
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
    val cityCopy = remember(code) { cityChartsCopy(code) }
    val shared = remember(code) { SharedStrings.forCode(code) }
    val chartDay = remember(locale) { chartDate(locale) }
    val dailySlots = remember(state.daily) { chartHueSlots(state.daily.map { it.playlistId }) }
    val citySlots = remember(state.cities) { chartHueSlots(state.cities.map { it.playlistId }) }
    val open = state.open
    if (open != null) {
        val storefront = if (open.city) state.cities.firstOrNull { it.playlistId == open.playlistId }?.storefront.orEmpty() else ""
        ChartDetailPage(
            chart = open,
            date = chartDay,
            playLabel = strings.playAll,
            errorLabel = top100.failed,
            retryLabel = top100.retry,
            backLabel = shared.back,
            actions = actions,
            onBack = onCloseChart,
            onRetry = { onOpenChart(open, storefront) },
            modifier = modifier
        )
        return
    }

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
                                Modifier.width(190.dp).clickable(role = Role.Button) {
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
        if (state.daily.isNotEmpty()) {
            item(key = "top100") {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ExpressiveSectionHeader(LevyraIcons.Chart, SectionTints.Amber, top100.title, top100.subtitle)
                    ShelfRow(
                        modifier = Modifier.horizontalShelfBleed(32.dp),
                        contentPadding = PaddingValues(horizontal = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(state.daily.size, key = { "top100-${state.daily[it].playlistId}" }) { index ->
                            val chart = state.daily[index]
                            val label = if (chart.global) top100.global else chart.label
                            Top100Card(label, chartDay, chart.global, dailySlots[index], onClick = {
                                onOpenChart(OpenChart(chart.playlistId, label, "Top 100", chart.global, city = false, slot = dailySlots[index]), "")
                            })
                        }
                    }
                }
            }
        }
        if (state.cities.isNotEmpty()) {
            item(key = "cities") {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ExpressiveSectionHeader(LevyraIcons.Radio, SectionTints.Sky, cityCopy.title, cityCopy.subtitle)
                    ShelfRow(
                        modifier = Modifier.horizontalShelfBleed(32.dp),
                        contentPadding = PaddingValues(horizontal = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(state.cities.size, key = { "city-${state.cities[it].playlistId}" }) { index ->
                            val city = state.cities[index]
                            CityChartCard(city.badge, city.name, citySlots[index], onClick = {
                                onOpenChart(OpenChart(city.playlistId, city.name, city.badge, global = false, city = true, slot = citySlots[index]), city.storefront)
                            })
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
                    val columns = when {
                        maxWidth > 1300.dp -> 4
                        maxWidth > 980.dp -> 3
                        maxWidth > 640.dp -> 2
                        else -> 1
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        stories.drop(1).chunked(columns).forEach { chunk ->
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
    Column(
        modifier.clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surfaceContainer).clickable(role = Role.Button, onClick = onClick)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
            if (story.imageUrl.isNotBlank()) {
                Artwork(story.imageUrl, Modifier.fillMaxSize(), cornerRadius = 0.dp)
            } else {
                Icon(LevyraIcons.Menu, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.Center).size(28.dp))
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${story.source.uppercase(locale)} · $ago", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp), color = LevyraBrand.soft, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(story.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
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
