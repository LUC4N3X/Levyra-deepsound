package com.luc4n3x.levyra.desktop.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.desktop.app.state.CollectionUiState
import com.luc4n3x.levyra.desktop.app.ui.components.Artwork
import com.luc4n3x.levyra.desktop.app.ui.components.CollectionCard
import com.luc4n3x.levyra.desktop.app.ui.components.EmptyState
import com.luc4n3x.levyra.desktop.app.ui.components.LoadingRow
import com.luc4n3x.levyra.desktop.app.ui.components.ScrollableColumn
import com.luc4n3x.levyra.desktop.app.ui.components.ShelfRow
import com.luc4n3x.levyra.desktop.app.ui.components.TrackActions
import com.luc4n3x.levyra.desktop.app.ui.components.TrackRow
import com.luc4n3x.levyra.desktop.app.ui.components.rememberArtworkRequest
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.app.util.Format
import com.luc4n3x.levyra.desktop.core.catalog.artistCreditMatches
import com.luc4n3x.levyra.desktop.core.model.ArtistDetail
import com.luc4n3x.levyra.desktop.core.model.CollectionRef
import com.luc4n3x.levyra.desktop.core.model.Track
import com.luc4n3x.levyra.desktop.core.storage.LibraryData
import com.luc4n3x.levyra.ui.i18n.LevyraSpotifySkinStrings
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.artistCatalogCopy
import com.luc4n3x.levyra.ui.i18n.searchDiscographyCopy
import com.luc4n3x.levyra.ui.i18n.speedDialCopy

@Composable
internal fun ArtistScreen(
    state: CollectionUiState,
    library: LibraryData,
    contentCountry: String,
    vlcDirectory: String,
    motionEnabled: Boolean,
    actions: TrackActions,
    onBack: () -> Unit,
    onShuffle: (List<Track>) -> Unit,
    onEnqueue: (List<Track>) -> Unit,
    onDownload: (List<Track>) -> Unit,
    onToggleFollow: (CollectionRef) -> Unit,
    onTogglePin: (CollectionRef) -> Unit,
    onToggleExclude: (CollectionRef) -> Unit,
    onOpen: (CollectionRef) -> Unit
) {
    val desktop = LocalStrings.current
    val strings = remember(desktop.languageCode) { LevyraStrings.forCode(desktop.languageCode) }
    val copy = remember(strings.code) { LevyraSpotifySkinStrings.forCode(strings.code) }
    val artist = state.artist
    val ref = state.ref
    if (artist == null || ref == null) {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            FilledTonalIconButton(onClick = onBack) { Icon(LevyraIcons.ChevronLeft, strings.back) }
            if (state.loading) LoadingRow(desktop.loading) else EmptyState(
                icon = LevyraIcons.Disc,
                title = state.error.ifBlank { strings.artistProfileUnavailable }
            )
        }
        return
    }

    val favorites = remember(artist.name, library.favorites) {
        library.favorites.filter { track -> artistCreditMatches(track.artist, artist.name) }
    }
    var showFavorites by remember(ref.id) { mutableStateOf(false) }
    val playAll = { actions.onPlay(artist.tracks, 0) }
    val latest = remember(artist.albums, artist.singles) {
        (artist.albums + artist.singles).maxByOrNull { it.subtitle.takeLast(4).toIntOrNull() ?: 0 }
    }

    ScrollableColumn(
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "artist-hero") {
            ArtistHero(
                artist = artist,
                ref = ref,
                contentCountry = contentCountry,
                vlcDirectory = vlcDirectory,
                motionEnabled = motionEnabled,
                strings = strings,
                followed = library.followedArtists.any { it.id == ref.id },
                pinned = library.pinnedArtists.any { it.id == ref.id },
                excluded = ref.id in library.excludedArtists,
                onBack = onBack,
                onPlay = playAll,
                onShuffle = { onShuffle(artist.tracks) },
                onEnqueue = { onEnqueue(artist.tracks) },
                onDownload = { onDownload(artist.tracks) },
                onFollow = { onToggleFollow(ref) },
                onPin = { onTogglePin(ref) },
                onExclude = { onToggleExclude(ref) }
            )
        }
        artistLatestRelease(latest, copy.latestRelease, onOpen)
        if (favorites.isNotEmpty()) {
            item(key = "artist-favorites-title") { ArtistSectionTitle(strings.favorites) }
            itemsIndexed(if (showFavorites) favorites else favorites.take(4), key = { _, track -> "liked-${track.id}" }) { index, _ ->
                ArtistTrackRow(favorites, index, actions, Modifier.padding(horizontal = 16.dp))
            }
            if (favorites.size > 4) {
                item(key = "artist-favorites-reveal") {
                    TextButton(onClick = { showFavorites = !showFavorites }, modifier = Modifier.padding(horizontal = 12.dp)) {
                        Text(if (showFavorites) strings.showLess else strings.showMore)
                    }
                }
            }
        }

        artistPopularTracks(artist.tracks, actions, strings, playAll)

        artistShelf("albums", strings.albumsPlain, artist.albums, onOpen)
        artistShelf("singles", strings.singlesAndEps, artist.singles, onOpen)
        artistShelf("compilations", strings.compilations, artist.compilations, onOpen)
        artistShelf("playlists", strings.playlists, artist.playlists, onOpen)
        artistShelf("featured", searchDiscographyCopy(strings.code).featuredOn, artist.featuredPlaylists, onOpen)

        artistVideos(artist.videos, actions, strings)
        if (artist.biography.isNotBlank()) {
            item(key = "artist-about") { ArtistBiography(artist, copy.aboutArtist, strings) }
        }
        artistShelf("related", strings.similarArtists, artist.relatedArtists, onOpen)
        if (!artist.hasContent) {
            item(key = "artist-empty") { EmptyState(icon = LevyraIcons.Disc, title = strings.artistProfileUnavailable) }
        }
    }
}

private fun LazyListScope.artistLatestRelease(
    latest: CollectionRef?,
    label: String,
    onOpen: (CollectionRef) -> Unit
) {
    if (latest != null) {
        item(key = "artist-latest") {
            Surface(
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().clickable { onOpen(latest) },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Artwork(latest.artworkUrl, Modifier.width(72.dp).height(72.dp), cornerRadius = 16.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(latest.title, style = MaterialTheme.typography.titleMedium)
                        if (latest.subtitle.isNotBlank()) Text(latest.subtitle, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

private fun LazyListScope.artistPopularTracks(
    popularTracks: List<Track>,
    actions: TrackActions,
    strings: LevyraStrings,
    playAll: () -> Unit
) {
    if (popularTracks.isNotEmpty()) {
        item(key = "artist-popular") {
            Column {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.popularTracks, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = playAll) { Text(strings.artistCatalogCopy().playAllSongs) }
                }
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val width = if (popularTracks.size <= PopularPageRows) maxWidth - 32.dp
                        else (maxWidth - 56.dp).coerceAtMost(520.dp)
                    val pages = remember(popularTracks) { popularTracks.chunked(PopularPageRows) }
                    ShelfRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(pages, key = { _, tracks -> tracks.first().id }) { page, tracks ->
                            Column(Modifier.width(width)) {
                                tracks.indices.forEach { index ->
                                    ArtistTrackRow(popularTracks, page * PopularPageRows + index, actions, ranked = true)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.artistVideos(videos: List<Track>, actions: TrackActions, strings: LevyraStrings) {
    if (videos.isNotEmpty()) {
        item(key = "artist-videos") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ArtistSectionTitle(strings.video)
                ShelfRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(videos.take(20), key = { _, track -> track.id }) { index, track ->
                        VideoCard(
                            track,
                            strings.code,
                            strings.pulsePlays,
                            menu = { ArtistVideoMenu(track, actions, strings) }
                        ) { actions.onPlay(videos, index) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistHero(
    artist: ArtistDetail,
    ref: CollectionRef,
    contentCountry: String,
    vlcDirectory: String,
    motionEnabled: Boolean,
    strings: LevyraStrings,
    followed: Boolean,
    pinned: Boolean,
    excluded: Boolean,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onEnqueue: () -> Unit,
    onDownload: () -> Unit,
    onFollow: () -> Unit,
    onPin: () -> Unit,
    onExclude: () -> Unit
) {
    val color = MaterialTheme.colorScheme.surface
    var menu by remember(ref.id) { mutableStateOf(false) }
    Column {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val height = (maxWidth * 0.57f).coerceIn(260.dp, 460.dp)
            Box(Modifier.fillMaxWidth().height(height).background(color)) {
                val artwork = artist.bannerUrl.ifBlank { artist.portraitUrl }
                AsyncImage(
                    model = rememberArtworkRequest(artwork),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize()
                )
                if (motionEnabled) ArtistCanvas(ref.id, artist.name, contentCountry, vlcDirectory, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.44f), 0.28f to Color.Transparent,
                    0.52f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.92f)
                )))
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FilledTonalIconButton(onClick = onBack) { Icon(LevyraIcons.ChevronLeft, strings.back) }
                    Box {
                        FilledTonalIconButton(onClick = { menu = true }) { Icon(LevyraIcons.More, strings.options) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            val catalog = strings.artistCatalogCopy()
                            val speedDial = strings.speedDialCopy()
                            DropdownMenuItem(
                                text = { Text(catalog.playAllSongs) },
                                enabled = artist.tracks.isNotEmpty(),
                                onClick = { menu = false; onPlay() }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.addToQueue) },
                                enabled = artist.tracks.isNotEmpty(),
                                onClick = { menu = false; onEnqueue() }
                            )
                            DropdownMenuItem(
                                text = { Text(catalog.downloadAllSongs) },
                                enabled = artist.tracks.isNotEmpty(),
                                onClick = { menu = false; onDownload() }
                            )
                            DropdownMenuItem(
                                text = { Text(if (excluded) strings.includeArtist else strings.excludeArtist) },
                                onClick = { menu = false; onExclude() }
                            )
                            DropdownMenuItem(
                                text = { Text(if (pinned) speedDial.removeFromHome else speedDial.addToHome) },
                                onClick = { menu = false; onPin() }
                            )
                        }
                    }
                }
                Column(
                    Modifier.align(Alignment.BottomStart).padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(artist.name, style = MaterialTheme.typography.displaySmall, color = Color.White, fontWeight = FontWeight.Bold)
                    val listeners = artist.monthlyListeners
                    val count = listeners ?: artist.subscriberCount.takeIf { it > 0 }
                    if (count != null) {
                        val label = if (listeners != null) strings.monthlyListeners else LocalStrings.current.artistSubscribers
                        Text("${formatViewCount(count, strings.code)} $label", color = Color.White.copy(alpha = 0.86f))
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(onClick = onShuffle, enabled = artist.tracks.size > 1) {
                Icon(LevyraIcons.Shuffle, strings.shuffle)
            }
            Button(onClick = onPlay, enabled = artist.tracks.isNotEmpty(), modifier = Modifier.weight(1f, fill = false), shape = CircleShape) {
                Text(strings.play)
            }
            OutlinedButton(onClick = onFollow, modifier = Modifier.weight(1f, fill = false), shape = CircleShape) {
                Text(if (followed) strings.followingArtist else strings.followArtist)
            }
        }
    }
}

@Composable
private fun ArtistVideoMenu(track: Track, actions: TrackActions, strings: LevyraStrings) {
    var expanded by remember { mutableStateOf(false) }
    FilledTonalIconButton(onClick = { expanded = true }) {
        Icon(LevyraIcons.More, strings.songOptions)
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        val favoriteLabel = if (actions.isFavorite(track)) strings.removeFromFavorites else strings.addToFavorites
        val entries = listOf(
            strings.playNext to { actions.onPlayNext(track) },
            strings.addToQueue to { actions.onEnqueue(track) },
            favoriteLabel to { actions.onToggleFavorite(track) },
            strings.addToPlaylist to { actions.onAddToPlaylist(track) }
        )
        entries.forEach { (label, action) ->
            DropdownMenuItem(text = { Text(label) }, onClick = { expanded = false; action() })
        }
    }
}

@Composable
private fun ArtistTrackRow(tracks: List<Track>, index: Int, actions: TrackActions, modifier: Modifier = Modifier, ranked: Boolean = false) {
    val track = tracks[index]
    val language = LocalStrings.current.languageCode
    val strings = remember(language) { LevyraStrings.forCode(language) }
    val metadata = if (ranked) remember(track.viewCount, track.durationMs, strings) {
        listOfNotNull(
            formatViewCount(track.viewCount, language).takeIf(String::isNotBlank)?.let { "$it ${strings.pulsePlays}" },
            track.durationMs.takeIf { it > 0 }?.let(Format::duration)
        ).joinToString(" · ").ifBlank { track.artist }
    } else track.displaySubtitle
    TrackRow(
        track = track,
        isCurrent = track.id == actions.currentTrackId,
        isFavorite = actions.isFavorite(track),
        onPlay = { actions.onPlay(tracks, index) },
        onPlayNext = { actions.onPlayNext(track) },
        onEnqueue = { actions.onEnqueue(track) },
        onToggleFavorite = { actions.onToggleFavorite(track) },
        onAddToPlaylist = { actions.onAddToPlaylist(track) },
        modifier = modifier,
        position = if (ranked) index + 1 else null,
        subtitle = metadata,
        showDuration = !ranked
    )
}

private fun LazyListScope.artistShelf(key: String, title: String, entries: List<CollectionRef>, onOpen: (CollectionRef) -> Unit) {
    if (entries.isEmpty()) return
    item(key = "artist-$key") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ArtistSectionTitle(title)
            ShelfRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(entries, key = { it.id }) { ref ->
                    CollectionCard(ref = ref, onClick = { onOpen(ref) }, modifier = Modifier.width(152.dp))
                }
            }
        }
    }
}

@Composable
private fun ArtistBiography(artist: ArtistDetail, title: String, strings: LevyraStrings) {
    var expanded by remember(artist.name) { mutableStateOf(false) }
    val dismiss = { expanded = false }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clickable(onClickLabel = strings.readAll) { expanded = true },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (artist.viewsLabel.isNotBlank()) Text(artist.viewsLabel, style = MaterialTheme.typography.labelLarge)
            Text(artist.biography, maxLines = 4, overflow = TextOverflow.Ellipsis)
            Text(strings.readAll, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
    if (expanded) {
        AlertDialog(
            onDismissRequest = dismiss,
            title = { Text(artist.name) },
            text = { Text(artist.biography, Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(onClick = dismiss) { Text(strings.close) } }
        )
    }
}

@Composable
private fun ArtistSectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(title, modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
}

private const val PopularPageRows = 4
