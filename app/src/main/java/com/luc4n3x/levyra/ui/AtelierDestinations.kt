package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraAtelierStrings
import com.luc4n3x.levyra.ui.i18n.GenreChartCopy
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.genreDiscoveryCopy
import com.luc4n3x.levyra.viewmodel.ExploreGenreDiscoveryState
import com.luc4n3x.levyra.ui.i18n.genreChartCopy
import com.luc4n3x.levyra.ui.theme.AtelierDesign

private val AtelierDestinationSleeveSide = 148.dp
private val AtelierDestinationPortrait = 112.dp

@Composable
internal fun AtelierDestinationTopBar(title: String?, backLabel: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 4.dp, end = AtelierDesign.gutter, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AtelierIconAction(Icons.AutoMirrored.Rounded.ArrowBack, backLabel, onBack, AtelierDesign.text)
        if (title != null) {
            Text(
                title, fontFamily = AtelierSerif, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = AtelierDesign.text,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 6.dp).semantics { heading() }
            )
        }
    }
}

@Composable
internal fun AtelierCollectionDestination(
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
    val copy = remember(strings.code) { LevyraAtelierStrings.forCode(strings.code) }
    val chartCopy = remember(strings.code) { genreChartCopy(strings.code) }
    var chartExpanded by rememberSaveable(title) { mutableStateOf(false) }
    LazyColumn(
        Modifier.fillMaxSize().background(AtelierDesign.room),
        contentPadding = PaddingValues(bottom = bottomInset + AtelierDesign.sectionGap)
    ) {
        item(key = "atelier-destination-bar", contentType = "atelier-destination-bar") {
            AtelierDestinationTopBar(null, strings.back, onBack)
        }
        item(key = "atelier-destination-poster", contentType = "atelier-destination-poster") {
            val lead = tracks.firstOrNull() ?: editorial.chart.firstOrNull()
            if (lead != null) LaunchedEffect(lead.id) { onRequestTrackArtwork(lead) }
            AtelierDestinationPoster(
                title = title,
                stamp = subtitle?.takeIf(String::isNotBlank) ?: strings.explore,
                artwork = lead?.let(::atelierCoverUrl).orEmpty(),
                meta = if (tracks.isNotEmpty()) copy.tracksCount.format(tracks.size) else "",
                playLabel = strings.playAll,
                onPlayAll = onPlayAll.takeIf { tracks.isNotEmpty() || editorial.chart.isNotEmpty() }
            )
        }
        atelierGenreChart(editorial, chartCopy, chartExpanded, currentTrackId, strings.play, { chartExpanded = !chartExpanded }, onPlayTrack, onRequestTrackArtwork)
        when {
            isLoading && tracks.isEmpty() -> item(key = "atelier-destination-loading", contentType = "atelier-destination-status") {
                Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    LevyraLoadingIndicator(color = AtelierDesign.accentBright, modifier = Modifier.size(36.dp))
                }
            }
            tracks.isEmpty() -> if (editorial.chart.isEmpty() && !editorial.chartLoading && !discovery.hasVisibleContent && !discovery.isLoading) item(key = "atelier-destination-empty", contentType = "atelier-destination-status") {
                Text(
                    strings.exploreEmpty, fontFamily = AtelierSerif, fontStyle = FontStyle.Italic, fontSize = 16.sp,
                    color = AtelierDesign.textSecondary, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(28.dp)
                )
            }
            else -> {
                atelierDestinationSleeves("featured", strings.popularTracks, editorial.featured, currentTrackId, onPlayTrack, onRequestTrackArtwork)
            }
        }
        genreArtistRowItems(genreArtistEntries(editorial, discovery), strings.artists, AtelierDiscoverySkin, onPlayTrack, onRequestTrackArtwork) { name ->
            discoveryActions?.openArtist?.invoke(name)
        }
        genreAlbumRowItems(genreAlbumEntries(editorial, discovery), strings.albumsPlain, AtelierDiscoverySkin, onPlayTrack, onRequestTrackArtwork) { album ->
            discoveryActions?.openAlbum?.invoke(album)
        }
        if (discoveryActions != null) genreDiscoveryItems(discovery, discoveryCopy, AtelierDiscoverySkin, discoveryActions)
        if (editorial.essentials.isNotEmpty()) {
            item(key = "atelier-destination-essentials-label", contentType = "atelier-label") { AtelierSectionLabel(strings.songsPlain) }
            itemsIndexed(editorial.essentials, key = { _, track -> "essential-${track.id}" }, contentType = { _, _ -> "atelier-track-line" }) { index, track ->
                LaunchedEffect(track.id) { onRequestTrackArtwork(track) }
                AtelierTrackLine(
                    track, index + 1, track.id == currentTrackId, Icons.Rounded.PlayArrow, strings.play,
                    onPlay = { onPlayTrack(track) }, onAction = { onPlayTrack(track) }
                )
            }
        }
    }
}

private fun LazyListScope.atelierGenreChart(
    editorial: ExploreGenreEditorial,
    copy: GenreChartCopy,
    expanded: Boolean,
    currentTrackId: String?,
    playLabel: String,
    onToggle: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onRequestTrackArtwork: (Track) -> Unit
) {
    val chart = editorial.chart
    if (chart.isEmpty() && !editorial.chartLoading) return
    item(key = "atelier-chart-header", contentType = "atelier-chart-header") {
        Column {
            AtelierSectionLabel(copy.title)
            Text(
                copy.subtitle, fontFamily = AtelierSerif, fontStyle = FontStyle.Italic, fontSize = 15.sp, color = AtelierDesign.textSecondary,
                modifier = Modifier.padding(start = AtelierDesign.gutter, end = AtelierDesign.gutter, bottom = 6.dp)
            )
        }
    }
    if (chart.isEmpty()) {
        item(key = "atelier-chart-loading", contentType = "atelier-destination-status") {
            Row(
                Modifier.fillMaxWidth().heightIn(min = AtelierDesign.rowHeight).padding(horizontal = AtelierDesign.gutter),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                LevyraLoadingIndicator(color = AtelierDesign.accentBright, modifier = Modifier.size(24.dp))
                Text(copy.loading, fontFamily = AtelierSerif, fontStyle = FontStyle.Italic, fontSize = 15.sp, color = AtelierDesign.textSecondary)
            }
        }
        return
    }
    itemsIndexed(chart.take(if (expanded) 50 else 10), key = { _, track -> "atelier-chart-${track.id}" }, contentType = { _, _ -> "atelier-track-line" }) { index, track ->
        LaunchedEffect(track.id) { onRequestTrackArtwork(track) }
        AtelierTrackLine(
            track, index + 1, track.id == currentTrackId, Icons.Rounded.PlayArrow, playLabel,
            onPlay = { onPlayTrack(track) }, onAction = { onPlayTrack(track) }
        )
    }
    item(key = "atelier-chart-footer", contentType = "atelier-chart-footer") {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                copy.credit, fontFamily = AtelierSerif, fontStyle = FontStyle.Italic, fontSize = 13.sp, color = AtelierDesign.textSecondary,
                modifier = Modifier.weight(1f)
            )
            if (chart.size > 10) {
                val label = if (expanded) copy.showLess else copy.showAll
                AtelierHardwareKey(label = label, lit = false, onClick = onToggle) {
                    Text(label, style = MaterialTheme.typography.labelLarge, color = AtelierDesign.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

private fun LazyListScope.atelierDestinationSleeves(
    key: String,
    title: String,
    tracks: List<Track>,
    currentTrackId: String?,
    onPlayTrack: (Track) -> Unit,
    onRequestTrackArtwork: (Track) -> Unit
) {
    if (tracks.isEmpty()) return
    item(key = "atelier-destination-$key", contentType = "atelier-destination-sleeves") {
        Column {
            AtelierSectionLabel(title)
            LazyRow(contentPadding = AtelierRowPadding, horizontalArrangement = Arrangement.spacedBy(AtelierDesign.itemGap)) {
                items(tracks, key = { it.id }, contentType = { "atelier-destination-sleeve" }) { track ->
                    LaunchedEffect(track.id) { onRequestTrackArtwork(track) }
                    AtelierDestinationSleeve(track.title, track.artist, atelierCoverUrl(track), track.id == currentTrackId) { onPlayTrack(track) }
                }
            }
        }
    }
}

@Composable
private fun AtelierDestinationPoster(title: String, stamp: String, artwork: String, meta: String, playLabel: String, onPlayAll: (() -> Unit)?) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter).padding(top = 6.dp)) {
        val posterWidth = maxWidth * 0.9f
        Column(Modifier.fillMaxWidth()) {
            Box(
                Modifier.width(posterWidth).height(posterWidth / 0.82f)
                    .graphicsLayer {
                        rotationZ = -1.2f
                        shadowElevation = 24.dp.toPx()
                        shape = RectangleShape
                        clip = true
                    }
                    .background(AtelierPaper)
                    .padding(9.dp)
                    .border(1.dp, AtelierInk.copy(alpha = 0.5f))
            ) {
                AtelierPrintImage(artwork, AtelierPosterFilter, title.take(1).uppercase(), Modifier.fillMaxSize().atelierAgedPrint())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to AtelierInk.copy(alpha = 0.93f))))
                AtelierPosterStamp(stamp, Modifier.align(Alignment.TopStart).padding(14.dp))
                Text(
                    title, fontFamily = AtelierSerif, fontWeight = FontWeight.Bold, fontSize = 42.sp, lineHeight = 44.sp,
                    color = AtelierPaper, maxLines = 3, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.BottomStart).padding(18.dp).semantics { heading() }
                )
            }
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onPlayAll != null) {
                    AtelierHardwareKey(playLabel, lit = false, onClick = onPlayAll, modifier = Modifier.width(92.dp).height(56.dp)) {
                        AtelierPlayGlyph(false, 28.dp)
                    }
                    Spacer(Modifier.width(14.dp))
                }
                if (meta.isNotBlank()) {
                    Text(meta, fontFamily = AtelierSerif, fontStyle = FontStyle.Italic, fontSize = 15.sp, color = AtelierDesign.textSecondary, maxLines = 1)
                }
            }
        }
    }
}

@Composable
internal fun AtelierDestinationSleeve(
    title: String,
    subtitle: String,
    artwork: String,
    current: Boolean,
    modifier: Modifier = Modifier.width(AtelierDestinationSleeveSide),
    onClick: () -> Unit
) {
    Column(
        modifier
            .levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Tile)
            .semantics(mergeDescendants = true) { contentDescription = "$title, $subtitle" }
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f)
                .graphicsLayer {
                    shadowElevation = 10.dp.toPx()
                    shape = AtelierDesign.sleeveShape
                    clip = true
                }
                .background(AtelierPaper).padding(5.dp)
        ) { AtelierArtwork(artwork, Modifier.clearAndSetSemantics { }) }
        Text(
            title, fontFamily = AtelierSerif, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 19.sp,
            color = if (current) AtelierDesign.accentBright else AtelierDesign.text, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp).clearAndSetSemantics { }
        )
        Text(
            subtitle, style = MaterialTheme.typography.bodySmall, color = AtelierDesign.textSecondary, minLines = 1, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.clearAndSetSemantics { }
        )
    }
}

@Composable
private fun AtelierDestinationArtist(name: String, artwork: String, onClick: () -> Unit) {
    Column(
        Modifier.width(AtelierDestinationPortrait + 8.dp)
            .levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Tile)
            .semantics(mergeDescendants = true) { contentDescription = name },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(AtelierDestinationPortrait).clip(CircleShape).background(AtelierPaper).padding(4.dp).clip(CircleShape)) {
            AtelierArtwork(artwork, Modifier.clearAndSetSemantics { })
        }
        Text(
            name, fontFamily = AtelierSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = AtelierDesign.text,
            textAlign = TextAlign.Center, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp).clearAndSetSemantics { }
        )
    }
}

@Composable
internal fun AtelierNewReleasesDestination(
    releases: List<AlbumHit>,
    isLoading: Boolean,
    strings: LevyraStrings,
    bottomInset: Dp,
    onBack: () -> Unit,
    onOpenRelease: (AlbumHit) -> Unit
) {
    val rows = remember(releases) { releases.distinctBy(::atelierAlbumKey).chunked(2) }
    LazyColumn(
        Modifier.fillMaxSize().background(AtelierDesign.room),
        contentPadding = PaddingValues(bottom = bottomInset + AtelierDesign.sectionGap)
    ) {
        item(key = "atelier-releases-bar", contentType = "atelier-destination-bar") {
            Column {
                AtelierDestinationTopBar(strings.exploreNewReleases, strings.back, onBack)
                Row(Modifier.padding(horizontal = AtelierDesign.gutter, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(36.dp).height(1.dp).background(AtelierDesign.accent))
                    Text(" ✦ ", fontFamily = AtelierSerif, fontSize = 12.sp, color = AtelierDesign.accent, modifier = Modifier.clearAndSetSemantics { })
                    Box(Modifier.width(36.dp).height(1.dp).background(AtelierDesign.accent))
                }
            }
        }
        when {
            isLoading && releases.isEmpty() -> item(key = "atelier-releases-loading", contentType = "atelier-destination-status") {
                Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    LevyraLoadingIndicator(color = AtelierDesign.accentBright, modifier = Modifier.size(36.dp))
                }
            }
            releases.isEmpty() -> item(key = "atelier-releases-empty", contentType = "atelier-destination-status") {
                Text(
                    strings.exploreEmpty, fontFamily = AtelierSerif, fontStyle = FontStyle.Italic, fontSize = 16.sp,
                    color = AtelierDesign.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(28.dp)
                )
            }
            else -> items(rows, key = { row -> "atelier-release-${atelierAlbumKey(row.first())}" }, contentType = { "atelier-release-row" }) { row ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(AtelierDesign.itemGap)
                ) {
                    row.forEach { release -> AtelierAlbumTile(release, Modifier.weight(1f)) { onOpenRelease(release) } }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private val AtelierDiscoverySkin = GenreDiscoverySkin(
    keyPrefix = "atelier",
    rowModifier = Modifier,
    rowPadding = AtelierRowPadding,
    itemGap = AtelierDesign.itemGap,
    header = { title -> AtelierSectionLabel(title) },
    cover = { tile -> AtelierDestinationSleeve(tile.title, tile.subtitle, tile.artworkUrl, false, onClick = tile.onClick) },
    wide = { tile ->
        GenreDiscoveryCoverCard(
            tile, 280.dp, AtelierDesign.sleeveShape, AtelierPaper, AtelierDesign.text, AtelierDesign.textSecondary,
            fontFamily = AtelierSerif, aspectRatio = 16f / 9f
        )
    },
    artist = { tile -> AtelierDestinationArtist(tile.title, tile.artworkUrl, tile.onClick) },
    about = { text, copy ->
        GenreDiscoveryAboutText(
            text, copy, AtelierDesign.text, AtelierDesign.textSecondary, AtelierDesign.accentBright,
            Modifier.padding(horizontal = AtelierDesign.gutter), AtelierSerif
        )
    },
    credit = { text ->
        GenreDiscoveryCredit(text, AtelierDesign.textSecondary, Modifier.padding(horizontal = AtelierDesign.gutter, vertical = 10.dp), AtelierSerif)
    }
)
