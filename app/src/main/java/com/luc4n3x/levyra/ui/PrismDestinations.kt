package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.ui.components.LevyraMediaTitle
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.DailyTop100Copy
import com.luc4n3x.levyra.ui.i18n.GenreChartCopy
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.genreDiscoveryCopy
import com.luc4n3x.levyra.viewmodel.ExploreGenreDiscoveryState
import com.luc4n3x.levyra.ui.i18n.genreChartCopy
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraActivePalette
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.PrismAccent
import com.luc4n3x.levyra.ui.theme.PrismDesign
import com.luc4n3x.levyra.viewmodel.DailyTop100ChartUiState
import java.util.Locale

internal val prismPageBackground: Color get() = if (LevyraActivePalette.isLight) Color(0xFFF7F2F5) else Color(0xFF060507)
private val PrismDestinationGutter = PrismDesign.gutter

@Composable
private fun PrismDestinationHero(
    title: String,
    eyebrow: String,
    artwork: String,
    playLabel: String,
    onBack: () -> Unit,
    onPlay: (() -> Unit)?,
    extra: @Composable RowScope.() -> Unit = {}
) {
    val strings = LocalLevyraStrings.current
    Box(Modifier.fillMaxWidth().aspectRatio(0.92f)) {
        PrismSplitArtwork(artwork, Modifier.fillMaxSize().clearAndSetSemantics { })
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.45f), 0.35f to Color.Transparent, 0.7f to Color.Black.copy(alpha = 0.55f), 1f to prismPageBackground)
            )
        )
        IconButton(onClick = onBack, modifier = Modifier.statusBarsPadding().padding(8.dp).size(48.dp).clip(PrismDesign.cut(14.dp)).background(Color.Black.copy(alpha = 0.4f))) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, strings.back, tint = Color.White)
        }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = PrismDestinationGutter, vertical = 8.dp)) {
            Row(
                Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)).padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(PrismAccent.gradient(false)))
                Spacer(Modifier.width(8.dp))
                Text(eyebrow.uppercase(), color = Color.White, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.4.sp, maxLines = 1)
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    title, color = LevyraText, fontSize = 40.sp, lineHeight = 43.sp, letterSpacing = (-1.2).sp, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).semantics { heading() }
                )
                extra()
                if (onPlay != null) {
                    Box(Modifier.padding(start = 10.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = playLabel, onClick = onPlay)) {
                        PrismPlayDisc(false, false, 60.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PrismDestinationHeader(title: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = PrismDestinationGutter).padding(top = 26.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(18.dp).height(3.dp).clip(CircleShape).background(PrismAccent.gradient(false)))
        Spacer(Modifier.width(10.dp))
        Text(title, color = LevyraText, style = MaterialTheme.typography.titleLarge, maxLines = 1, modifier = Modifier.semantics { heading() })
    }
}

@Composable
private fun PrismTrackRow(rank: Int, track: Track, current: Boolean, actionLabel: String, onPlay: () -> Unit, onAction: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 68.dp)
            .levyraPressable(onClick = onPlay, role = Role.Button, pressedScale = LevyraPressScale.Row)
            .padding(start = PrismDestinationGutter, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "%02d".format(Locale.ROOT, rank),
            style = if (rank <= 3) TextStyle(brush = PrismAccent.gradient(false), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            else MaterialTheme.typography.titleMedium.copy(color = LevyraMuted),
            modifier = Modifier.width(40.dp).clearAndSetSemantics { }
        )
        Box(Modifier.size(52.dp).clip(PrismDesign.cut(12.dp))) { CoverImage(track, Modifier.fillMaxSize()) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            LevyraMediaTitle(track.title, explicit = track.explicit,  color = if (current) LevyraPink else LevyraText, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = LevyraMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (onAction != null) IconButton(onClick = onAction) { Icon(Icons.Rounded.MoreVert, actionLabel, tint = LevyraMuted) }
    }
}

@Composable
private fun PrismStatus(loading: Boolean, message: String, retry: String?, onRetry: (() -> Unit)?) {
    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (loading) LevyraLoadingIndicator(color = LevyraPink, modifier = Modifier.size(36.dp))
        else Text(message, color = LevyraMuted, style = MaterialTheme.typography.bodyLarge)
        if (!loading && retry != null && onRetry != null) TextButton(onClick = onRetry) { Text(retry, color = LevyraPink) }
    }
}

@Composable
internal fun PrismCollectionDestination(
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
    LazyColumn(Modifier.fillMaxSize().background(prismPageBackground), contentPadding = PaddingValues(bottom = bottomInset + 32.dp)) {
        item(key = "prism-destination-hero") {
            val lead = tracks.firstOrNull() ?: editorial.chart.firstOrNull()
            if (lead != null) LaunchedEffect(lead.id) { onRequestTrackArtwork(lead) }
            PrismDestinationHero(
                title, subtitle?.takeIf(String::isNotBlank) ?: strings.explore, lead?.let(::atelierCoverUrl).orEmpty(),
                strings.playAll, onBack, onPlayAll.takeIf { tracks.isNotEmpty() || editorial.chart.isNotEmpty() }
            )
        }
        prismGenreChart(editorial, chartCopy, chartExpanded, currentTrackId, strings.songOptions, { chartExpanded = !chartExpanded }, onPlayTrack, onRequestTrackArtwork)
        when {
            isLoading && tracks.isEmpty() -> item(key = "prism-destination-loading") { PrismStatus(true, "", null, null) }
            tracks.isEmpty() -> if (editorial.chart.isEmpty() && !editorial.chartLoading && !discovery.hasVisibleContent && !discovery.isLoading) item(key = "prism-destination-empty") { PrismStatus(false, strings.exploreEmpty, null, null) }
            else -> {
                prismCoverRail("featured", strings.popularTracks, editorial.featured.map { Triple(it.title, it.artist, it) }, onPlayTrack, onRequestTrackArtwork)
            }
        }
        genreArtistRowItems(genreArtistEntries(editorial, discovery), strings.artists, PrismDiscoverySkin, onPlayTrack, onRequestTrackArtwork) { name ->
            discoveryActions?.openArtist?.invoke(name)
        }
        genreAlbumRowItems(genreAlbumEntries(editorial, discovery), strings.albumsPlain, PrismDiscoverySkin, onPlayTrack, onRequestTrackArtwork) { album ->
            discoveryActions?.openAlbum?.invoke(album)
        }
        if (discoveryActions != null) genreDiscoveryItems(discovery, discoveryCopy, PrismDiscoverySkin, discoveryActions)
        if (editorial.essentials.isNotEmpty()) {
            item(key = "prism-destination-essentials") { PrismDestinationHeader(strings.songsPlain) }
            itemsIndexed(editorial.essentials, key = { _, t -> "essential-${t.id}" }) { index, track ->
                LaunchedEffect(track.id) { onRequestTrackArtwork(track) }
                PrismTrackRow(index + 1, track, track.id == currentTrackId, strings.songOptions, { onPlayTrack(track) })
            }
        }
    }
}

private fun LazyListScope.prismGenreChart(
    editorial: ExploreGenreEditorial,
    copy: GenreChartCopy,
    expanded: Boolean,
    currentTrackId: String?,
    actionLabel: String,
    onToggle: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onRequestTrackArtwork: (Track) -> Unit
) {
    val chart = editorial.chart
    if (chart.isEmpty() && !editorial.chartLoading) return
    item(key = "prism-chart-header") {
        Column {
            PrismDestinationHeader(copy.title)
            Text(
                copy.subtitle, color = LevyraMuted, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = PrismDestinationGutter, end = PrismDestinationGutter, bottom = 6.dp)
            )
        }
    }
    if (chart.isEmpty()) {
        item(key = "prism-chart-loading") {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 68.dp).padding(horizontal = PrismDestinationGutter),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                LevyraLoadingIndicator(color = LevyraPink, modifier = Modifier.size(24.dp))
                Text(copy.loading, color = LevyraMuted, style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }
    itemsIndexed(chart.take(if (expanded) 50 else 10), key = { _, track -> "prism-chart-${track.id}" }) { index, track ->
        LaunchedEffect(track.id) { onRequestTrackArtwork(track) }
        PrismTrackRow(index + 1, track, track.id == currentTrackId, actionLabel, { onPlayTrack(track) })
    }
    item(key = "prism-chart-footer") {
        Row(
            Modifier.fillMaxWidth().padding(start = PrismDestinationGutter, end = 8.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(copy.credit, color = LevyraMuted, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
            if (chart.size > 10) TextButton(onClick = onToggle) { Text(if (expanded) copy.showLess else copy.showAll, color = LevyraPink, maxLines = 1) }
        }
    }
}

private fun LazyListScope.prismCoverRail(
    key: String,
    title: String,
    entries: List<Triple<String, String, Track>>,
    onPlayTrack: (Track) -> Unit,
    onRequestTrackArtwork: (Track) -> Unit
) {
    if (entries.isEmpty()) return
    item(key = "prism-destination-$key") {
        Column {
            PrismDestinationHeader(title)
            LazyRow(contentPadding = PaddingValues(horizontal = PrismDestinationGutter), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(entries, key = { it.third.id + key }) { (name, artist, track) ->
                    LaunchedEffect(track.id) { onRequestTrackArtwork(track) }
                    Column(
                        Modifier.width(150.dp)
                            .levyraPressable(onClick = { onPlayTrack(track) }, role = Role.Button, pressedScale = LevyraPressScale.Tile)
                            .semantics(mergeDescendants = true) { contentDescription = "$name, $artist" }
                    ) {
                        Box(Modifier.size(150.dp).clip(PrismDesign.cut(18.dp))) { PrismSplitArtwork(atelierCoverUrl(track), Modifier.fillMaxSize().clearAndSetSemantics { }) }
                        Text(name, color = LevyraText, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp).clearAndSetSemantics { })
                        Text(artist, color = LevyraMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.clearAndSetSemantics { })
                    }
                }
            }
        }
    }
}

@Composable
internal fun PrismTop100ChartScreen(
    open: DailyTop100ChartUiState,
    label: String,
    date: String,
    slot: Int,
    copy: DailyTop100Copy,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPlay: (List<Track>, Track) -> Unit,
    onTrackActions: (Track) -> Unit
) {
    BackHandler(onBack = onBack)
    val strings = LocalLevyraStrings.current
    val tracks = open.tracks
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyColumn(Modifier.fillMaxSize().background(prismPageBackground), contentPadding = PaddingValues(bottom = bottom + 24.dp)) {
        item(key = "prism-top100-hero") {
            Column(Modifier.fillMaxWidth().statusBarsPadding()) {
                IconButton(onClick = onBack, modifier = Modifier.padding(8.dp).size(48.dp)) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, strings.back, tint = LevyraText) }
                PrismTop100Card(label, date, slot, open.chart.global, Modifier.padding(horizontal = PrismDestinationGutter).fillMaxWidth().aspectRatio(1.35f))
                Row(Modifier.fillMaxWidth().padding(horizontal = PrismDestinationGutter, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(copy.title, color = LevyraText, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).semantics { heading() })
                    if (tracks.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                val shuffled = tracks.shuffled()
                                onPlay(shuffled, shuffled.first())
                            },
                            modifier = Modifier.size(52.dp)
                        ) { Icon(Icons.Rounded.Shuffle, strings.shuffle, tint = LevyraText) }
                        Box(Modifier.clip(CircleShape).clickable(role = Role.Button, onClickLabel = strings.play) { onPlay(tracks, tracks.first()) }) {
                            PrismPlayDisc(false, false, 56.dp)
                        }
                    }
                }
            }
        }
        when {
            tracks.isNotEmpty() -> itemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
                PrismTrackRow(index + 1, track, false, strings.options, { onPlay(tracks, track) }, { onTrackActions(track) })
            }
            open.loading -> item(key = "prism-top100-loading") { PrismStatus(true, "", null, null) }
            else -> item(key = "prism-top100-failed") { PrismStatus(false, copy.failed, copy.retry, onRetry) }
        }
    }
}

private val PrismChartHues = listOf(
    Color(0xFFF03A62) to Color(0xFFD13FC0),
    Color(0xFF7B5CFF) to Color(0xFF2EC5FF),
    Color(0xFFFF7A45) to Color(0xFFF03A62),
    Color(0xFF16C9A6) to Color(0xFF3D7BFF)
)

@Composable
internal fun PrismTop100Card(label: String, date: String, slot: Int, global: Boolean, modifier: Modifier) {
    val (start, end) = if (global) PrismChartHues[0] else PrismChartHues[slot.mod(PrismChartHues.size)]
    Box(
        modifier.clip(PrismDesign.cut(22.dp)).background(Color(0xFF0B090D))
            .background(Brush.linearGradient(listOf(start.copy(alpha = 0.92f), end.copy(alpha = 0.55f), Color(0xFF0B090D))))
            .padding(16.dp)
    ) {
        Text(date, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium, letterSpacing = 1.4.sp, modifier = Modifier.align(Alignment.TopStart))
        Text(
            "100", color = Color.White, fontSize = 64.sp, lineHeight = 64.sp, fontWeight = FontWeight.Light, letterSpacing = (-3).sp,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        Column(Modifier.align(Alignment.BottomStart)) {
            Box(Modifier.width(22.dp).height(3.dp).clip(CircleShape).background(Color.White))
            Spacer(Modifier.height(8.dp))
            Text(label, color = Color.White, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private val PrismDiscoverySkin = GenreDiscoverySkin(
    keyPrefix = "prism",
    rowModifier = Modifier,
    rowPadding = PaddingValues(horizontal = PrismDestinationGutter),
    itemGap = 14.dp,
    header = { title -> PrismDestinationHeader(title) },
    cover = { tile ->
        GenreDiscoveryCoverCard(tile, 150.dp, PrismDesign.cut(18.dp), LevyraPanel, LevyraText, LevyraMuted)
    },
    wide = { tile ->
        GenreDiscoveryCoverCard(tile, 280.dp, PrismDesign.cut(18.dp), LevyraPanel, LevyraText, LevyraMuted, aspectRatio = 16f / 9f)
    },
    artist = { tile ->
        GenreDiscoveryArtistCard(tile, 96.dp, CircleShape, LevyraPanel, LevyraText)
    },
    about = { text, copy ->
        GenreDiscoveryAboutText(text, copy, LevyraText, LevyraMuted, LevyraText, Modifier.padding(horizontal = PrismDestinationGutter))
    },
    credit = { text -> GenreDiscoveryCredit(text, LevyraMuted, Modifier.padding(horizontal = PrismDestinationGutter, vertical = 10.dp)) }
)
