package com.luc4n3x.levyra.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.luc4n3x.levyra.data.apple.AppleCityChart
import com.luc4n3x.levyra.data.apple.AppleDailyChart
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.CityChartsCopy
import com.luc4n3x.levyra.ui.i18n.DailyTop100Copy
import com.luc4n3x.levyra.ui.i18n.LevyraQobuzSkinStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.QobuzDesign
import com.luc4n3x.levyra.viewmodel.DailyTop100ChartUiState

private val QobuzTop100Ink = Color(0xFF1A1A1A)

internal fun LazyListScope.qobuzChartRails(
    charts: List<AppleDailyChart>,
    cities: List<AppleCityChart>,
    chartCopy: DailyTop100Copy,
    cityCopy: CityChartsCopy,
    date: String,
    onOpenChart: (AppleDailyChart) -> Unit,
    onOpenCity: (AppleCityChart) -> Unit
) {
    if (charts.isNotEmpty()) {
        item(key = "qobuz-top100", contentType = "qobuz-chart-rail") {
            Column(Modifier.padding(top = 32.dp)) {
                QobuzSectionHeader(chartCopy.title, chartCopy.subtitle, "", null)
                Spacer(Modifier.height(QobuzDesign.headerToContent))
                LazyRow(contentPadding = QobuzRowPadding, horizontalArrangement = Arrangement.spacedBy(QobuzDesign.itemGap)) {
                    items(charts, key = { it.playlistId }, contentType = { "qobuz-top100-card" }) { chart ->
                        val label = if (chart.global) chartCopy.global else chart.label
                        QobuzTop100Card(
                            label, chartCopy.title, date,
                            Modifier
                                .semantics(mergeDescendants = true) { contentDescription = "${chartCopy.title}, $label" }
                                .clickable(role = Role.Button) { onOpenChart(chart) }
                        )
                    }
                }
            }
        }
    }
    if (cities.isNotEmpty()) {
        item(key = "qobuz-city-charts", contentType = "qobuz-chart-rail") {
            Column(Modifier.padding(top = 32.dp)) {
                QobuzSectionHeader(cityCopy.title, cityCopy.subtitle, "", null)
                Spacer(Modifier.height(QobuzDesign.headerToContent))
                LazyRow(contentPadding = QobuzRowPadding, horizontalArrangement = Arrangement.spacedBy(QobuzDesign.itemGap)) {
                    items(cities, key = { it.playlistId }, contentType = { "qobuz-city-card" }) { chart ->
                        QobuzCityCard(
                            chart.name, chart.badge, qobuzTagPalette(chart.playlistId),
                            Modifier
                                .semantics(mergeDescendants = true) { contentDescription = "${cityCopy.title}, ${chart.name}" }
                                .clickable(role = Role.Button) { onOpenCity(chart) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun QobuzTop100Card(label: String, title: String, date: String, modifier: Modifier = Modifier) {
    Column(modifier.width(QobuzDesign.gridCard)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(QobuzDesign.cardShape).background(QobuzTop100Ink)) {
            Text(
                date, color = QobuzDesign.hiResGold, fontSize = fixedSp(11f), fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = 14.dp, end = 14.dp)
            )
            Text(
                "100", color = QobuzDesign.hiResGold, fontSize = fixedSp(64f), lineHeight = fixedSp(64f),
                fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 14.dp, bottom = 12.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = QobuzText.cardTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(title, style = QobuzText.cardSubtitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun QobuzCityCard(name: String, badge: String, colors: Pair<Color, Color>, modifier: Modifier = Modifier) {
    Box(
        modifier.width(QobuzDesign.gridCard).aspectRatio(1f).clip(QobuzDesign.cardShape)
            .background(Brush.linearGradient(listOf(colors.first, colors.second)))
    ) {
        Text(
            badge, color = Color.White, fontSize = fixedSp(12f), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp).clip(QobuzDesign.buttonShape).background(Color(0x33000000))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
        Text(
            name, color = Color.White, fontSize = fixedSp(20f), lineHeight = fixedSp(24f), fontWeight = FontWeight.SemiBold,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
        )
    }
}

@Composable
internal fun QobuzChartScreen(
    open: DailyTop100ChartUiState,
    label: String,
    badge: String,
    copy: DailyTop100Copy,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPlay: (List<Track>, Track) -> Unit,
    onTrackActions: (Track) -> Unit
) {
    BackHandler(onBack = onBack)
    val strings = LocalLevyraStrings.current
    val qobuz = remember(strings.code) { LevyraQobuzSkinStrings.forCode(strings.code) }
    val view = LocalView.current
    val lightBars = QobuzDesign.surfacePrimary.luminance() > 0.5f
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = lightBars
            isAppearanceLightNavigationBars = lightBars
        }
    }
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val tracks = open.tracks
    Column(Modifier.fillMaxSize().background(QobuzDesign.surfacePrimary)) {
        QobuzBackBar(copy.title, statusTop, onBack)
        LazyColumn(contentPadding = PaddingValues(bottom = bottom + 24.dp)) {
            item(key = "qobuz-chart-hero", contentType = "qobuz-chart-hero") {
                Column(Modifier.fillMaxWidth().padding(horizontal = QobuzDesign.gutter, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            badge, color = QobuzDesign.textAccent, fontSize = fixedSp(56f), lineHeight = fixedSp(60f),
                            fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false
                        )
                        Text(
                            label, style = MaterialTheme.typography.headlineMedium, color = QobuzDesign.textPrimary,
                            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                        )
                    }
                    if (tracks.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            QobuzPlayCircle(qobuz.play, 56.dp, { onPlay(tracks, tracks.first()) })
                            QobuzIconAction(Icons.Rounded.Shuffle, strings.shuffle) {
                                val shuffled = tracks.shuffled()
                                onPlay(shuffled, shuffled.first())
                            }
                        }
                    }
                }
            }
            when {
                tracks.isNotEmpty() -> itemsIndexed(tracks, key = { _, track -> track.id }, contentType = { _, _ -> "qobuz-chart-row" }) { index, track ->
                    QobuzRankedRow(
                        index + 1, track, qobuz, false, { onPlay(tracks, track) }, { onTrackActions(track) },
                        Modifier.fillMaxWidth().padding(horizontal = QobuzDesign.gutter)
                    )
                }
                open.loading -> item(key = "qobuz-chart-loading", contentType = "qobuz-chart-status") {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = QobuzDesign.textAccent)
                    }
                }
                else -> item(key = "qobuz-chart-failed", contentType = "qobuz-chart-status") {
                    Column(Modifier.fillMaxWidth().padding(QobuzDesign.gutter), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(copy.failed, style = QobuzText.baseline)
                        Text(
                            copy.retry, style = MaterialTheme.typography.labelLarge, color = QobuzDesign.textPrimary,
                            modifier = Modifier.clip(QobuzDesign.buttonShape).background(QobuzDesign.surfaceSecondary)
                                .clickable(role = Role.Button, onClick = onRetry).padding(horizontal = 16.dp, vertical = 14.dp)
                        )
                    }
                }
            }
        }
    }
}
