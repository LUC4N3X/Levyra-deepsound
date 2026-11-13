package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.ui.components.LevyraMediaTitle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.GenreChartCopy
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.genreDiscoveryCopy
import com.luc4n3x.levyra.viewmodel.ExploreGenreDiscoveryState
import com.luc4n3x.levyra.ui.i18n.genreChartCopy
import com.luc4n3x.levyra.ui.theme.LevyraActivePalette
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraPanelSoft
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.IvoryDesign
import androidx.compose.foundation.shape.RoundedCornerShape

private val IvoryDestinationGutter = 20.dp

@Composable
internal fun IvoryCollectionDestination(
    title: String,
    subtitle: String?,
    tracks: List<Track>,
    editorial: ExploreGenreEditorial,
    isLoading: Boolean,
    currentTrackId: String?,
    strings: LevyraStrings,
    bottomInset: Dp,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onRequestTrackArtwork: (Track) -> Unit,
    discovery: ExploreGenreDiscoveryState,
    discoveryActions: GenreDiscoveryActions?
) {
    val discoveryCopy = remember(strings.code) { genreDiscoveryCopy(strings.code) }
    val chartCopy = remember(strings.code) { genreChartCopy(strings.code) }
    var chartExpanded by rememberSaveable(title) { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().background(LevyraBlack), contentPadding = PaddingValues(bottom = bottomInset + 32.dp)) {
        item(key = "ivory-destination-hero") {
            val lead = tracks.firstOrNull() ?: editorial.chart.firstOrNull()
            if (lead != null) LaunchedEffect(lead.id) { onRequestTrackArtwork(lead) }
            IvoryDestinationHero(
                title, subtitle?.takeIf(String::isNotBlank) ?: strings.explore, lead?.let(::atelierCoverUrl).orEmpty(),
                tracks.size, strings, onBack, onPlayAll.takeIf { tracks.isNotEmpty() || editorial.chart.isNotEmpty() }
            )
        }
        ivoryGenreChart(editorial, chartCopy, chartExpanded, currentTrackId, { chartExpanded = !chartExpanded }, onPlayTrack, onRequestTrackArtwork)
        when {
            isLoading && tracks.isEmpty() -> item(key = "ivory-destination-loading") {
                Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    LevyraLoadingIndicator(color = IvoryDesign.ink, modifier = Modifier.size(36.dp))
                }
            }
            tracks.isEmpty() -> if (editorial.chart.isEmpty() && !editorial.chartLoading && !discovery.hasVisibleContent && !discovery.isLoading) item(key = "ivory-destination-empty") {
                Text(strings.exploreEmpty, color = LevyraMuted, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(IvoryDestinationGutter))
            }
            else -> {
                ivoryCoverRail("featured", strings.popularTracks, editorial.featured.map { Triple(it.title, it.artist, it) }, onPlayTrack, onRequestTrackArtwork)
            }
        }
        genreArtistRowItems(genreArtistEntries(editorial, discovery), strings.artists, IvoryDiscoverySkin, onPlayTrack, onRequestTrackArtwork) { name ->
            discoveryActions?.openArtist?.invoke(name)
        }
        genreAlbumRowItems(genreAlbumEntries(editorial, discovery), strings.albumsPlain, IvoryDiscoverySkin, onPlayTrack, onRequestTrackArtwork) { album ->
            discoveryActions?.openAlbum?.invoke(album)
        }
        if (discoveryActions != null) genreDiscoveryItems(discovery, discoveryCopy, IvoryDiscoverySkin, discoveryActions)
        if (editorial.essentials.isNotEmpty()) {
            item(key = "ivory-destination-essentials") { Box(Modifier.padding(top = 18.dp)) { SkinSectionTitle(strings.songsPlain, IvoryDestinationGutter) } }
            itemsIndexed(editorial.essentials, key = { _, t -> "essential-${t.id}" }) { index, track ->
                LaunchedEffect(track.id) { onRequestTrackArtwork(track) }
                IvoryEssentialRow(index, track, track.id == currentTrackId) { onPlayTrack(track) }
            }
        }
    }
}

@Composable
private fun IvoryDestinationHero(
    title: String,
    eyebrow: String,
    artwork: String,
    count: Int,
    strings: LevyraStrings,
    onBack: () -> Unit,
    onPlay: (() -> Unit)?
) {
    Box(Modifier.fillMaxWidth().aspectRatio(0.9f).background(LevyraPanel)) {
        AsyncImage(artwork, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clearAndSetSemantics { })
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.5f), 0.3f to Color.Transparent, 0.6f to LevyraBlack.copy(alpha = 0.55f), 1f to LevyraBlack)
            )
        )
        IconButton(onClick = onBack, modifier = Modifier.statusBarsPadding().padding(8.dp).size(48.dp).clip(CircleShape).background(IvoryDesign.onInk)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, strings.back, tint = IvoryDesign.ink)
        }
        if (count > 0) Text(
            count.toString(), color = IvoryDesign.ink, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp).clip(CircleShape).background(IvoryDesign.onInk)
                .padding(horizontal = 10.dp, vertical = 6.dp).clearAndSetSemantics { }
        )
        Row(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = IvoryDestinationGutter, end = IvoryDestinationGutter, bottom = 12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    eyebrow, color = IvoryDesign.onInk, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1,
                    modifier = Modifier.clip(CircleShape).background(IvoryDesign.ink).padding(horizontal = 14.dp, vertical = 6.dp)
                )
                Text(
                    title, color = LevyraText, fontSize = 40.sp, lineHeight = 44.sp, fontWeight = FontWeight.Normal,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() }
                )
            }
            if (onPlay != null) Box(
                Modifier.padding(start = 12.dp).size(64.dp).clip(IvoryDesign.coverShape).background(IvoryDesign.ink)
                    .clickable(role = Role.Button, onClickLabel = strings.playAll, onClick = onPlay),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.PlayArrow, null, tint = IvoryDesign.onInk, modifier = Modifier.size(30.dp)) }
        }
    }
}

private fun LazyListScope.ivoryCoverRail(
    key: String,
    title: String,
    entries: List<Triple<String, String, Track>>,
    onPlayTrack: (Track) -> Unit,
    onRequestTrackArtwork: (Track) -> Unit
) {
    if (entries.isEmpty()) return
    item(key = "ivory-destination-$key") {
        Column(Modifier.padding(top = 18.dp)) {
            SkinSectionTitle(title, IvoryDestinationGutter)
            LazyRow(contentPadding = PaddingValues(horizontal = IvoryDestinationGutter), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(entries, key = { _, entry -> entry.third.id + key }) { index, (name, artist, track) ->
                    LaunchedEffect(track.id) { onRequestTrackArtwork(track) }
                    Column(
                        Modifier.width(156.dp)
                            .levyraPressable(onClick = { onPlayTrack(track) }, role = Role.Button, pressedScale = LevyraPressScale.Tile)
                            .semantics(mergeDescendants = true) { contentDescription = "$name, $artist" }
                    ) {
                        Box(Modifier.size(156.dp).clip(IvoryDesign.coverShape).background(LevyraPanel)) {
                            AsyncImage(atelierCoverUrl(track), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clearAndSetSemantics { })
                            IvoryCornerBadge(46.dp, Modifier.align(Alignment.BottomEnd)) { Icon(Icons.Rounded.PlayArrow, null, tint = IvoryDesign.onInk, modifier = Modifier.size(18.dp)) }
                        }
                        Column(Modifier.padding(top = 10.dp, start = 2.dp).clearAndSetSemantics { }) {
                            LevyraMediaTitle(name, explicit = track.explicit, color = LevyraText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(artist, color = LevyraMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.ivoryGenreChart(
    editorial: ExploreGenreEditorial,
    copy: GenreChartCopy,
    expanded: Boolean,
    currentTrackId: String?,
    onToggle: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onRequestTrackArtwork: (Track) -> Unit
) {
    val chart = editorial.chart
    if (chart.isEmpty() && !editorial.chartLoading) return
    item(key = "ivory-chart-header") {
        Column(Modifier.padding(top = 18.dp)) {
            SkinSectionTitle(copy.title, IvoryDestinationGutter)
            Text(
                copy.subtitle, color = LevyraMuted, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = IvoryDestinationGutter, end = IvoryDestinationGutter, bottom = 8.dp)
            )
        }
    }
    if (chart.isEmpty()) {
        item(key = "ivory-chart-loading") {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = IvoryDestinationGutter, vertical = 3.dp).heightIn(min = 72.dp)
                    .clip(RoundedCornerShape(IvoryDesign.tileCorner)).background(LevyraPanel).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LevyraLoadingIndicator(color = IvoryDesign.ink, modifier = Modifier.size(24.dp))
                Text(copy.loading, color = LevyraMuted, style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }
    itemsIndexed(chart.take(if (expanded) 50 else 10), key = { _, track -> "ivory-chart-${track.id}" }) { index, track ->
        LaunchedEffect(track.id) { onRequestTrackArtwork(track) }
        IvoryEssentialRow(index, track, track.id == currentTrackId) { onPlayTrack(track) }
    }
    item(key = "ivory-chart-footer") {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = IvoryDestinationGutter, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(copy.credit, color = LevyraMuted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            if (chart.size > 10) Box(
                Modifier.heightIn(min = 48.dp).clip(CircleShape).background(IvoryDesign.ink)
                    .clickable(role = Role.Button, onClick = onToggle).padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) { Text(if (expanded) copy.showLess else copy.showAll, color = IvoryDesign.onInk, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1) }
        }
    }
}

@Composable
private fun IvoryEssentialRow(index: Int, track: Track, current: Boolean, onPlay: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = IvoryDestinationGutter, vertical = 3.dp).heightIn(min = 72.dp).clip(RoundedCornerShape(IvoryDesign.tileCorner))
            .background(if (current) LevyraPanelSoft else LevyraPanel)
            .levyraPressable(onClick = onPlay, role = Role.Button, pressedScale = LevyraPressScale.Row)
            .padding(start = 10.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text((index + 1).toString(), color = if (index < 3) IvoryDesign.ink else LevyraMuted, style = MaterialTheme.typography.titleMedium, maxLines = 1, softWrap = false, modifier = Modifier.widthIn(min = 28.dp))
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(IvoryDesign.tileCorner))) { CoverImage(track, Modifier.fillMaxSize()) }
        Column(Modifier.weight(1f)) {
            LevyraMediaTitle(track.title, explicit = track.explicit,  color = LevyraText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = LevyraMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private val IvoryDiscoverySkin = GenreDiscoverySkin(
    keyPrefix = "ivory",
    rowModifier = Modifier,
    rowPadding = PaddingValues(horizontal = IvoryDestinationGutter),
    itemGap = 10.dp,
    header = { title -> Box(Modifier.padding(top = 18.dp)) { SkinSectionTitle(title, IvoryDestinationGutter) } },
    cover = { tile ->
        GenreDiscoveryCoverCard(tile, 156.dp, IvoryDesign.coverShape, LevyraPanel, LevyraText, LevyraMuted, titleWeight = FontWeight.Medium)
    },
    wide = { tile ->
        GenreDiscoveryCoverCard(
            tile, 280.dp, IvoryDesign.coverShape, LevyraPanel, LevyraText, LevyraMuted,
            titleWeight = FontWeight.Medium, aspectRatio = 16f / 9f
        )
    },
    artist = { tile ->
        GenreDiscoveryArtistCard(tile, 124.dp, RoundedCornerShape(IvoryDesign.coverCorner), LevyraPanel, LevyraText)
    },
    about = { text, copy ->
        GenreDiscoveryAboutText(text, copy, LevyraText, LevyraMuted, LevyraText, Modifier.padding(horizontal = IvoryDestinationGutter))
    },
    credit = { text -> GenreDiscoveryCredit(text, LevyraMuted, Modifier.padding(horizontal = IvoryDestinationGutter, vertical = 10.dp)) }
)
