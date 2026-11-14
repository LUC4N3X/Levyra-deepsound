package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import com.luc4n3x.levyra.ui.theme.AppleMusicDesign
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import com.luc4n3x.levyra.ui.components.levyraPressable
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
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
internal fun rememberProviderGenreZones(): List<ExploreZone> {
    val strings = LocalLevyraStrings.current
    return remember(strings) { exploreGenreChartZones(ExploreCatalog.getZones(strings), strings.code) }
}

@Composable
internal fun rememberProviderGenreChart(zone: ExploreZone): ProviderGenreChart {
    val kicker = rememberGenreHitsCopy().title
    val cover = rememberGenreChartCover(zone)
    return remember(zone, cover, kicker) {
        val artists = cover.artists.take(ProviderGenreChartArtists).joinToString(", ")
        ProviderGenreChart(zone, zone.label, artists, cover.artworkUrl, kicker, genreChartCoverTone(zone))
    }
}

@Composable
internal fun YouTubeMusicGenreHitsShelf(onOpen: (ExploreZone) -> Unit) {
    val zones = rememberProviderGenreZones()
    if (zones.isEmpty()) return
    val copy = rememberGenreHitsCopy()
    Column {
        YouTubeMusicSectionHeader(copy.shelfTitle, strapline = copy.shelfSubtitle)
        Spacer(Modifier.height(8.dp))
        androidx.compose.foundation.lazy.LazyRow(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = YouTubeMusicDesign.gutter),
            horizontalArrangement = Arrangement.spacedBy(YouTubeMusicDesign.itemGap)
        ) {
            items(zones, key = { "genre-hits-${it.id}" }, contentType = { "ytm-genre-hit" }) { zone ->
                val chart = rememberProviderGenreChart(zone)
                val model = remember(chart) {
                    YouTubeMusicCardModel("genre-hits-${zone.id}", chart.title, chart.subtitle, chart.artworkUrl, onClick = { onOpen(zone) }, cover = chart.cover)
                }
                YouTubeMusicCard(model)
            }
        }
    }
}

@Composable
internal fun AppleMusicGenreHitsShelf(onOpen: (ExploreZone) -> Unit) {
    val zones = rememberProviderGenreZones()
    if (zones.isEmpty()) return
    val copy = rememberGenreHitsCopy()
    val width = AppleMusicDesign.cardWidth(appleMusicViewportWidth())
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    Column {
        AppleMusicSectionHeader(copy.shelfTitle)
        androidx.compose.foundation.lazy.LazyRow(
            state = listState,
            flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(listState, androidx.compose.foundation.gestures.snapping.SnapPosition.Start),
            contentPadding = PaddingValues(horizontal = AppleMusicDesign.gutter),
            horizontalArrangement = Arrangement.spacedBy(AppleMusicDesign.itemGap)
        ) {
            items(zones, key = { "genre-hits-${it.id}" }, contentType = { "apple-genre-hit" }) { zone ->
                val chart = rememberProviderGenreChart(zone)
                val model = remember(chart) {
                    AppleMusicCardModel("genre-hits-${zone.id}", chart.title, chart.subtitle, chart.artworkUrl, cover = chart.cover) { onOpen(zone) }
                }
                AppleMusicSquareCard(model, "", Modifier.width(width))
            }
        }
    }
}

@Composable
internal fun SpotifyGenreHitsShelf(onOpen: (ExploreZone) -> Unit) {
    val zones = rememberProviderGenreZones()
    if (zones.isEmpty()) return
    val copy = rememberGenreHitsCopy()
    SpotifyHomeShelf(copy.shelfTitle) {
        items(zones, key = { "genre-hits-${it.id}" }, contentType = { "spotify-genre-hit" }) { zone ->
            val chart = rememberProviderGenreChart(zone)
            SpotifyHomeCoverCard(chart.title, chart.subtitle, chart.artworkUrl, cover = chart.cover) { onOpen(zone) }
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

private val YouTubeMusicChartTones = listOf(
    androidx.compose.ui.graphics.Color(0xFFB64CF0),
    androidx.compose.ui.graphics.Color(0xFF6E6EF2),
    androidx.compose.ui.graphics.Color(0xFF22B573),
    androidx.compose.ui.graphics.Color(0xFFF29A3A),
    androidx.compose.ui.graphics.Color(0xFFE8485A),
    androidx.compose.ui.graphics.Color(0xFF1FA6C9)
)
private val YouTubeMusicChartInk = androidx.compose.ui.graphics.Color(0xFF111111)

@Composable
private fun YouTubeMusicTop100Cover(
    label: String,
    date: String,
    tone: androidx.compose.ui.graphics.Color,
    shape: androidx.compose.ui.graphics.Shape,
    numeral: String? = null
) {
    Box(Modifier.fillMaxSize().clip(shape).background(tone).padding(14.dp)) {
        Text(
            date, color = YouTubeMusicChartInk, fontSize = 13.sp, letterSpacing = 1.2.sp, maxLines = 1,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        if (numeral == null) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TOP", color = YouTubeMusicChartInk, fontSize = 34.sp, lineHeight = 34.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, maxLines = 1)
                Text("100", color = YouTubeMusicChartInk, fontSize = 56.sp, lineHeight = 56.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, maxLines = 1)
            }
        } else {
            Text(
                label, color = YouTubeMusicChartInk, fontSize = 24.sp, lineHeight = 28.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.align(Alignment.Center)
            )
        }
        Text(
            if (numeral == null) label.uppercase() else "TOP $numeral", color = YouTubeMusicChartInk, fontSize = 15.sp, letterSpacing = 1.4.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
internal fun YouTubeMusicTop100Shelf(
    charts: List<com.luc4n3x.levyra.data.apple.AppleDailyChart>,
    slots: List<Int>,
    copy: com.luc4n3x.levyra.ui.i18n.DailyTop100Copy,
    date: String,
    modifier: Modifier,
    onOpen: (com.luc4n3x.levyra.data.apple.AppleDailyChart) -> Unit
) {
    val cards = remember(charts, slots, copy, date) {
        charts.mapIndexed { index, chart ->
            val label = if (chart.global) copy.global else chart.label
            val tone = youTubeMusicChartTone(slots.getOrNull(index) ?: index)
            YouTubeMusicCardModel(
                "ytm-top100-${chart.playlistId}", label, copy.title, "",
                onClick = { onOpen(chart) },
                cover = { shape -> YouTubeMusicTop100Cover(label, date, tone, shape) }
            )
        }
    }
    Column(modifier.fillMaxWidth()) {
        YouTubeMusicSectionHeader(copy.title, strapline = date)
        Spacer(Modifier.height(8.dp))
        YouTubeMusicCardCarousel(cards)
    }
}

@Composable
internal fun YouTubeMusicCityShelf(
    cities: List<com.luc4n3x.levyra.data.apple.AppleCityChart>,
    slots: List<Int>,
    copy: com.luc4n3x.levyra.ui.i18n.CityChartsCopy,
    date: String,
    modifier: Modifier,
    onOpen: (com.luc4n3x.levyra.data.apple.AppleCityChart) -> Unit
) {
    val cards = remember(cities, slots, copy, date) {
        cities.mapIndexed { index, chart ->
            val tone = youTubeMusicChartTone(slots.getOrNull(index) ?: index)
            YouTubeMusicCardModel(
                "ytm-city-${chart.playlistId}", chart.name, copy.title, "",
                onClick = { onOpen(chart) },
                cover = { shape -> YouTubeMusicTop100Cover(chart.name, date, tone, shape, chart.numeral) }
            )
        }
    }
    Column(modifier.fillMaxWidth()) {
        YouTubeMusicSectionHeader(copy.title, strapline = copy.subtitle)
        Spacer(Modifier.height(8.dp))
        YouTubeMusicCardCarousel(cards)
    }
}

@Composable
internal fun YouTubeMusicMostDiscussedShelf(
    tracks: List<com.luc4n3x.levyra.domain.Track>,
    comments: Map<String, com.luc4n3x.levyra.domain.ResonanceCommentSnippet>,
    explicitLabel: String,
    actions: AppleMusicHomeActions
) {
    val strings = LocalLevyraStrings.current
    val width = (youTubeMusicViewportWidth() - YouTubeMusicDesign.gutter * 2 - 24.dp).coerceIn(280.dp, 420.dp)
    Column {
        YouTubeMusicSectionHeader(strings.mostCommentedTracks)
        Spacer(Modifier.height(8.dp))
        androidx.compose.foundation.lazy.LazyRow(
            contentPadding = PaddingValues(horizontal = YouTubeMusicDesign.gutter),
            horizontalArrangement = Arrangement.spacedBy(YouTubeMusicDesign.itemGap)
        ) {
            items(tracks, key = { "ytm-discussed-${it.id}" }, contentType = { "ytm-discussed" }) { track ->
                YouTubeMusicDiscussedCard(track, comments[track.id], width, explicitLabel, strings, { actions.playFrom(tracks, track) }, { actions.onTrackActions(track) }) {
                    actions.openComments(track)
                }
            }
        }
    }
}

@Composable
private fun YouTubeMusicDiscussedCard(
    track: com.luc4n3x.levyra.domain.Track,
    comment: com.luc4n3x.levyra.domain.ResonanceCommentSnippet?,
    width: Dp,
    explicitLabel: String,
    strings: com.luc4n3x.levyra.ui.i18n.LevyraStrings,
    onPlay: () -> Unit,
    onMore: () -> Unit,
    onOpenComments: () -> Unit
) {
    val cardShape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
    val bubbleShape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
    Column(Modifier.width(width).clip(cardShape).background(YouTubeMusicDesign.surface).padding(20.dp)) {
        Row(
            Modifier.fillMaxWidth().levyraPressable(onClick = onPlay, onLongClick = onMore, role = androidx.compose.ui.semantics.Role.Button),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(60.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp)).background(YouTubeMusicDesign.placeholder)) {
                CoverImage(track, Modifier.fillMaxSize())
            }
            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                com.luc4n3x.levyra.ui.components.LevyraMediaTitle(
                    track.title, explicit = false, color = YouTubeMusicDesign.text, fontSize = 17.sp, lineHeight = 22.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (track.explicit) YouTubeMusicExplicitBadge(explicitLabel, Modifier.padding(end = 6.dp))
                    Text(track.artist, color = YouTubeMusicDesign.textSecondary, fontSize = 15.sp, lineHeight = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            YouTubeMusicIconAction(androidx.compose.material.icons.Icons.Rounded.MoreVert, strings.songOptions, onMore)
        }
        Column(
            Modifier.padding(top = 18.dp).fillMaxWidth().clip(bubbleShape)
                .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.08f))
                .levyraPressable(onClick = onOpenComments, role = androidx.compose.ui.semantics.Role.Button, onClickLabel = strings.tapToOpenComments)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(strings.commentsLabel, color = YouTubeMusicDesign.text, fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, maxLines = 1)
                comment?.countText?.takeIf(String::isNotBlank)?.let { count ->
                    Text(count, color = YouTubeMusicDesign.textSecondary, fontSize = 13.sp, maxLines = 1, modifier = Modifier.padding(start = 6.dp, bottom = 1.dp))
                }
            }
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(30.dp).clip(androidx.compose.foundation.shape.CircleShape).background(YouTubeMusicDesign.placeholder)) {
                    comment?.authorAvatarUrl?.takeIf(String::isNotBlank)?.let { url ->
                        StableRemoteArtwork(url, null, Modifier.fillMaxSize(), androidx.compose.ui.layout.ContentScale.Crop)
                    }
                }
                Text(
                    comment?.text?.takeIf(String::isNotBlank) ?: strings.tapToOpenComments,
                    color = YouTubeMusicDesign.text, fontSize = 15.sp, lineHeight = 20.sp, minLines = 2, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 14.dp, end = 8.dp)
                )
                androidx.compose.material3.Icon(
                    androidx.compose.material.icons.Icons.Rounded.KeyboardArrowDown, null,
                    tint = YouTubeMusicDesign.text, modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
internal fun youTubeMusicChartTone(slot: Int): androidx.compose.ui.graphics.Color = YouTubeMusicChartTones[slot.mod(YouTubeMusicChartTones.size)]

@Composable
internal fun YouTubeMusicTop100ChartScreen(
    open: com.luc4n3x.levyra.viewmodel.DailyTop100ChartUiState,
    label: String,
    date: String,
    slot: Int,
    copy: com.luc4n3x.levyra.ui.i18n.DailyTop100Copy,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPlay: (List<com.luc4n3x.levyra.domain.Track>, com.luc4n3x.levyra.domain.Track) -> Unit,
    onTrackActions: (com.luc4n3x.levyra.domain.Track) -> Unit,
    numeral: String? = null
) {
    androidx.activity.compose.BackHandler(onBack = onBack)
    val strings = LocalLevyraStrings.current
    val explicit = remember(strings.code) { com.luc4n3x.levyra.ui.i18n.LevyraYouTubeMusicSkinStrings.forCode(strings.code).explicit }
    val tone = youTubeMusicChartTone(slot)
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val tracks = open.tracks
    Box(Modifier.fillMaxSize().background(YouTubeMusicDesign.background)) {
        YouTubeMusicHomeBackdrop(tone, Modifier.fillMaxWidth(), height = statusTop + 520.dp)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = statusTop, bottom = 32.dp)) {
            item(key = "ytm-top100-bar", contentType = "ytm-top100-bar") {
                Row(Modifier.fillMaxWidth().height(YouTubeMusicDesign.topBarHeight).padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    YouTubeMusicIconAction(Icons.AutoMirrored.Rounded.ArrowBack, strings.back, onBack)
                }
            }
            item(key = "ytm-top100-hero", contentType = "ytm-top100-hero") {
                Column(Modifier.fillMaxWidth().padding(horizontal = YouTubeMusicDesign.gutter), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.padding(top = 8.dp).size(240.dp)) {
                        YouTubeMusicTop100Cover(label, date, tone, androidx.compose.foundation.shape.RoundedCornerShape(6.dp), numeral)
                    }
                    Text(
                        "${copy.title} · $label", color = YouTubeMusicDesign.text, fontSize = 28.sp, lineHeight = 34.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(top = 24.dp).semantics { heading() }
                    )
                    Text(
                        copy.subtitle, color = YouTubeMusicDesign.textSecondary, fontSize = 15.sp, maxLines = 2,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 8.dp)
                    )
                    if (tracks.isNotEmpty()) {
                        Text(
                            strings.formatTrackCount(tracks.size), color = YouTubeMusicDesign.textSecondary, fontSize = 14.sp,
                            maxLines = 1, modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Row(
                        Modifier.padding(top = 20.dp, bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        YouTubeMusicRoundAction(Icons.Rounded.Shuffle, strings.shuffle, filled = true, enabled = tracks.size > 1) {
                            tracks.shuffled().let { shuffled -> shuffled.firstOrNull()?.let { onPlay(shuffled, it) } }
                        }
                        Box(
                            Modifier.size(64.dp).clip(androidx.compose.foundation.shape.CircleShape).background(androidx.compose.ui.graphics.Color.White)
                                .levyraPressable(
                                    onClick = { tracks.firstOrNull()?.let { onPlay(tracks, it) } }, enabled = tracks.isNotEmpty(),
                                    role = androidx.compose.ui.semantics.Role.Button, onClickLabel = strings.play
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.Icon(Icons.Rounded.PlayArrow, strings.play, tint = androidx.compose.ui.graphics.Color.Black, modifier = Modifier.size(34.dp))
                        }
                    }
                }
            }
            if (open.loading && tracks.isEmpty()) {
                item(key = "ytm-top100-loading", contentType = "ytm-top100-state") {
                    Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                        com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator(Modifier.size(32.dp), color = YouTubeMusicDesign.text)
                    }
                }
            } else if (open.failed && tracks.isEmpty()) {
                item(key = "ytm-top100-failed", contentType = "ytm-top100-state") { YouTubeMusicNotice(copy.failed, copy.retry, onRetry) }
            }
            itemsIndexed(tracks, key = { _, track -> "ytm-top100-row-${track.id}" }, contentType = { _, _ -> "ytm-top100-row" }) { index, track ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${index + 1}", color = YouTubeMusicDesign.text, fontSize = 16.sp, maxLines = 1,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(start = YouTubeMusicDesign.gutter).width(28.dp)
                    )
                    YouTubeMusicSongRow(
                        track = track, subtitle = track.artist, current = false, explicitLabel = explicit,
                        onClick = { onPlay(tracks, track) }, onMore = { onTrackActions(track) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
