package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.YoutubeMusicHomeShelf
import com.luc4n3x.levyra.ui.i18n.LevyraMonoHomeStrings
import com.luc4n3x.levyra.ui.theme.YouTubeMusicDesign
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.ExploreCatalog
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.ui.i18n.GenreChartCopy
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.genreChartCopy

private const val ProviderGenreChartArtists = 2

@Immutable
internal class ProviderGenreChart(
    val zone: ExploreZone,
    val title: String,
    val subtitle: String,
    val artworkUrl: String,
    val kicker: String,
    val tone: androidx.compose.ui.graphics.Color
) {
    val cover: @Composable (androidx.compose.ui.graphics.Shape) -> Unit = { shape ->
        GenreChartCover(zone.id, title, tone, artworkUrl.ifBlank { null }, kicker, Modifier.fillMaxSize(), shape)
    }
}

@Composable
internal fun rememberGenreHitsCopy(): GenreChartCopy {
    val code = LocalLevyraStrings.current.code
    return remember(code) { genreChartCopy(code) }
}

@Composable
internal fun rememberProviderGenreCharts(): List<ProviderGenreChart> {
    val strings = LocalLevyraStrings.current
    val zones = remember(strings) { exploreGenreChartZones(ExploreCatalog.getZones(strings), strings.code) }
    val kicker = rememberGenreHitsCopy().title
    return zones.map { zone ->
        key(zone.id) {
            val cover = rememberGenreChartCover(zone)
            remember(zone, cover, kicker) {
                val artists = cover.artists.take(ProviderGenreChartArtists).joinToString(", ")
                ProviderGenreChart(zone, zone.label, artists, cover.artworkUrl, kicker, genreChartCoverTone(zone))
            }
        }
    }
}

@Composable
internal fun YouTubeMusicGenreHitsShelf(onOpen: (ExploreZone) -> Unit) {
    val charts = rememberProviderGenreCharts()
    if (charts.isEmpty()) return
    val copy = rememberGenreHitsCopy()
    Column {
        YouTubeMusicSectionHeader(copy.shelfTitle, strapline = copy.shelfSubtitle)
        Spacer(Modifier.height(8.dp))
        YouTubeMusicCardCarousel(
            charts.map { chart ->
                YouTubeMusicCardModel("genre-hits-${chart.zone.id}", chart.title, chart.subtitle, chart.artworkUrl, onClick = { onOpen(chart.zone) }, cover = chart.cover)
            }
        )
    }
}

@Composable
internal fun AppleMusicGenreHitsShelf(onOpen: (ExploreZone) -> Unit) {
    val charts = rememberProviderGenreCharts()
    if (charts.isEmpty()) return
    val copy = rememberGenreHitsCopy()
    Column {
        AppleMusicSectionHeader(copy.shelfTitle)
        AppleMusicCardCarousel(
            charts.map { chart ->
                AppleMusicCardModel("genre-hits-${chart.zone.id}", chart.title, chart.subtitle, chart.artworkUrl, cover = chart.cover) { onOpen(chart.zone) }
            }
        )
    }
}

@Composable
internal fun SpotifyGenreHitsShelf(onOpen: (ExploreZone) -> Unit) {
    val charts = rememberProviderGenreCharts()
    if (charts.isEmpty()) return
    val copy = rememberGenreHitsCopy()
    SpotifyHomeShelf(copy.shelfTitle) {
        items(charts, key = { "genre-hits-${it.zone.id}" }) { chart ->
            SpotifyHomeCoverCard(chart.title, chart.subtitle, chart.artworkUrl, cover = chart.cover) { onOpen(chart.zone) }
        }
    }
}

private fun youTubeMusicCuratedCards(shelf: YoutubeMusicHomeShelf, onOpen: (PlaylistHit) -> Unit): List<YouTubeMusicCardModel> =
    shelf.items.distinctBy { it.key }.mapNotNull { item ->
        val playlist = item.playlist ?: return@mapNotNull null
        YouTubeMusicCardModel("curated-${shelf.key}-${item.key}", item.title, monoWithoutYoutube(item.subtitle), item.thumbnailUrl, onClick = { onOpen(playlist) })
    }

@Composable
private fun YouTubeMusicCuratedRow(shelf: YoutubeMusicHomeShelf, title: String, onOpen: (PlaylistHit) -> Unit, onShowAll: (() -> Unit)?) {
    val cards = remember(shelf, onOpen) { youTubeMusicCuratedCards(shelf, onOpen) }
    if (cards.isEmpty()) return
    Column {
        YouTubeMusicSectionHeader(title, strapline = shelf.strapline, onOpen = onShowAll)
        Spacer(Modifier.height(8.dp))
        YouTubeMusicCardCarousel(cards)
    }
}

@Composable
internal fun YouTubeMusicCuratedShelves(onOpen: (PlaylistHit) -> Unit) {
    val code = LocalLevyraStrings.current.code
    val shelves = rememberCuratedPlaylistShelves(code)
    val leadTitle = remember(code) { LevyraMonoHomeStrings.forCode(code).mixedForYou }
    val showAll = LocalCuratedPlaylistsOpener.current
    Column(verticalArrangement = Arrangement.spacedBy(YouTubeMusicDesign.sectionGap)) {
        shelves.forEachIndexed { index, shelf ->
            key(shelf.key) {
                YouTubeMusicCuratedRow(shelf, if (index == 0) leadTitle else shelf.title, onOpen, showAll.takeIf { index == 0 })
            }
        }
    }
}

@Composable
internal fun YouTubeMusicCuratedPlaylistsPage(page: CuratedPageData, onOpen: (PlaylistHit) -> Unit, onDismiss: () -> Unit, bottomInset: Dp) {
    val strings = LocalLevyraStrings.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    LazyColumn(
        Modifier.fillMaxSize().background(YouTubeMusicDesign.background).pointerInput(Unit) { detectTapGestures { } },
        contentPadding = PaddingValues(top = statusTop, bottom = bottomInset + 16.dp),
        verticalArrangement = Arrangement.spacedBy(YouTubeMusicDesign.sectionGap)
    ) {
        item(key = "ytm-curated-bar", contentType = "ytm-curated-bar") {
            Row(Modifier.fillMaxWidth().height(YouTubeMusicDesign.topBarHeight).padding(start = 4.dp, end = YouTubeMusicDesign.gutter), verticalAlignment = Alignment.CenterVertically) {
                YouTubeMusicIconAction(Icons.AutoMirrored.Rounded.ArrowBack, strings.back, onDismiss)
                Text(
                    page.title, color = YouTubeMusicDesign.text, style = MaterialTheme.typography.titleLarge, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp).semantics { heading() }
                )
            }
        }
        items(page.groups, key = { "ytm-curated-${it.key}" }, contentType = { "ytm-curated-group" }) { shelf ->
            YouTubeMusicCuratedRow(shelf, shelf.title, onOpen, null)
        }
    }
}
