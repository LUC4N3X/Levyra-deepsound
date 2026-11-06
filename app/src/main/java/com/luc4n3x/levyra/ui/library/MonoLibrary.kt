package com.luc4n3x.levyra.ui.library

import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import com.luc4n3x.levyra.ui.MonoArtwork
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.DownloadOwnership
import com.luc4n3x.levyra.domain.DownloadedTrack
import com.luc4n3x.levyra.domain.LibrarySort
import com.luc4n3x.levyra.domain.LibrarySortDirection
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.playableTrackCount
import com.luc4n3x.levyra.domain.visibleInLibrary
import com.luc4n3x.levyra.ui.CoverImage
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.MonoOverline
import com.luc4n3x.levyra.ui.MonoPill
import com.luc4n3x.levyra.ui.MonoScreenTitle
import com.luc4n3x.levyra.ui.MonoSectionHeader
import com.luc4n3x.levyra.ui.MonoWaveLine
import com.luc4n3x.levyra.ui.i18n.LevyraMonoLibraryStrings
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.MonoLibraryCopy
import com.luc4n3x.levyra.ui.i18n.formatLibraryBytes
import com.luc4n3x.levyra.ui.i18n.playlistImportCopy
import com.luc4n3x.levyra.ui.levyraTabContentBottomPadding
import com.luc4n3x.levyra.ui.monoPanel
import com.luc4n3x.levyra.ui.theme.MonoDesign
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.LibraryViewModel
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class MonoLibraryFilter { All, Playlists, Albums, Artists, Downloads }

private const val MonoScrubberMinimum = 12
private const val MonoRecentLimit = 8

private sealed interface MonoLibraryEntry {
    val key: String
    val title: String
    val meta: String
    val covers: List<String> get() = emptyList()
    val track: Track? get() = null

    class PlaylistItem(val playlist: Playlist, override val meta: String) : MonoLibraryEntry {
        override val key = "playlist-${playlist.id}"
        override val title = playlist.name
        override val covers = (sequenceOf(playlist.coverUrl) + playlist.tracks.asSequence().map { it.thumbnailUrl })
            .filter(String::isNotBlank).distinct().take(4).toList()
    }

    class AlbumItem(val album: LibraryAlbum, override val meta: String) : MonoLibraryEntry {
        override val key = "album-${album.key}"
        override val title = album.title
        override val covers = listOf(album.artworkUrl).filter(String::isNotBlank)
    }

    class ArtistItem(val artist: LibraryArtist, override val meta: String) : MonoLibraryEntry {
        override val key = "artist-${artist.key}"
        override val title = artist.name
        override val covers = (sequenceOf(artist.artworkUrl) + artist.tracks.asSequence().map { it.thumbnailUrl })
            .filter(String::isNotBlank).distinct().take(4).toList()
    }

    class SongItem(override val track: Track, override val meta: String) : MonoLibraryEntry {
        override val key = "song-${libraryTrackKey(track)}"
        override val title = track.title
    }

    class OfflineItem(val item: LibraryOfflineItem, override val meta: String) : MonoLibraryEntry {
        override val key = "offline-${item.key}"
        override val title = item.track.title
        override val track = item.track
    }
}

private class MonoLibrarySnapshot(
    val filter: MonoLibraryFilter,
    val deck: List<MonoLibraryEntry>,
    val rows: List<MonoLibraryEntry>,
    val firstIndexBySlot: IntArray
)

private fun buildMonoLibrarySnapshot(
    filter: MonoLibraryFilter,
    catalog: LibraryCatalog,
    playlists: List<Playlist>,
    favorites: List<Track>,
    favoriteTimestamps: Map<String, Long>,
    query: String,
    sort: LibrarySort,
    direction: LibrarySortDirection,
    strings: LevyraStrings,
    copy: MonoLibraryCopy
): MonoLibrarySnapshot {
    val visible = playlists.visibleInLibrary()
    fun playlistItems() = filterLibraryPlaylists(visible, query, sort, direction).map {
        MonoLibraryEntry.PlaylistItem(it, "${copy.playlistKind} · ${strings.formatTrackCount(it.playableTrackCount)}")
    }
    fun albumItems() = filterLibraryAlbums(catalog.albums, query, sort, direction).map { album ->
        MonoLibraryEntry.AlbumItem(album, listOf(copy.albumKind, album.artist, album.year).filter(String::isNotBlank).joinToString(" · "))
    }
    val deck: List<MonoLibraryEntry> = when (filter) {
        MonoLibraryFilter.All -> playlistItems() + albumItems()
        MonoLibraryFilter.Playlists -> playlistItems()
        MonoLibraryFilter.Albums -> albumItems()
        MonoLibraryFilter.Artists -> filterLibraryArtists(catalog.artists, query, sort, direction).map { artist ->
            MonoLibraryEntry.ArtistItem(
                artist,
                if (artist.tracks.isEmpty()) copy.artistKind else "${copy.artistKind} · ${strings.formatTrackCount(artist.tracks.size)}"
            )
        }
        MonoLibraryFilter.Downloads -> emptyList()
    }.distinctBy { it.key }
    val rows: List<MonoLibraryEntry> = when (filter) {
        MonoLibraryFilter.All -> if (query.isBlank()) monoRecentAdditions(catalog, visible, favorites, favoriteTimestamps, strings, copy) else emptyList()
        MonoLibraryFilter.Downloads -> filterLibraryOfflineItems(catalog.offlineItems, query, sort, direction).map { item ->
            MonoLibraryEntry.OfflineItem(
                item,
                listOf(item.track.artist, strings.formatLibraryBytes(item.download.sizeBytes)).filter(String::isNotBlank).joinToString(" · ")
            )
        }
        else -> emptyList()
    }.distinctBy { it.key }
    val firstIndexBySlot = IntArray(MonoScrubberLetters.length) { -1 }
    deck.forEachIndexed { index, entry ->
        val slot = monoScrubberSlot(entry.title)
        if (firstIndexBySlot[slot] < 0) firstIndexBySlot[slot] = index
    }
    return MonoLibrarySnapshot(filter, deck, rows, firstIndexBySlot)
}

private fun monoRecentAdditions(
    catalog: LibraryCatalog,
    playlists: List<Playlist>,
    favorites: List<Track>,
    favoriteTimestamps: Map<String, Long>,
    strings: LevyraStrings,
    copy: MonoLibraryCopy
): List<MonoLibraryEntry> {
    val stamped = ArrayList<Pair<Long, MonoLibraryEntry>>()
    playlists.forEach { playlist ->
        stamped += playlist.createdAt to MonoLibraryEntry.PlaylistItem(
            playlist, "${copy.playlistKind} · ${strings.formatTrackCount(playlist.playableTrackCount)}"
        )
    }
    favorites.forEach { track ->
        val at = favoriteTimestamps[track.id] ?: return@forEach
        stamped += at to MonoLibraryEntry.SongItem(track, listOf(track.artist, copy.songKind).filter(String::isNotBlank).joinToString(" · "))
    }
    catalog.offlineItems.forEach { item ->
        stamped += item.download.savedAt to MonoLibraryEntry.OfflineItem(
            item, listOf(item.track.artist, strings.offline).filter(String::isNotBlank).joinToString(" · ")
        )
    }
    return stamped.sortedByDescending { it.first }.asSequence().map { it.second }.distinctBy { it.key }.take(MonoRecentLimit).toList()
}

@Suppress("ComplexMethod", "CognitiveComplexMethod")
@Composable
internal fun MonoLibraryScreen(
    viewModel: LibraryViewModel,
    state: LevyraUiState,
    catalog: LibraryCatalog,
    onOpenDownloads: () -> Unit,
    onOpenTools: () -> Unit,
    backEnabled: Boolean = true
) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraMonoLibraryStrings.forCode(strings.code) }
    val animations = LocalAnimationsEnabled.current
    var filterName by rememberSaveable { mutableStateOf(MonoLibraryFilter.All.name) }
    val filter = MonoLibraryFilter.entries.firstOrNull { it.name == filterName } ?: MonoLibraryFilter.All
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var smartCollection by rememberSaveable { mutableStateOf<String?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var addTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var deletingPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var deletingDownload by remember { mutableStateOf<DownloadedTrack?>(null) }
    val deleteOffline = rememberOfflineDeleteHandler(viewModel)
    val sort = state.interfaceSettings.librarySort
    val direction = state.interfaceSettings.librarySortDirection
    val playlists = state.playlists
    val favorites = state.favorites
    val favoriteTimestamps = state.favoriteTimestamps
    val snapshot by produceState<MonoLibrarySnapshot?>(
        null, filter, catalog, playlists, favorites, favoriteTimestamps, query, sort, direction, strings.code
    ) {
        value = withContext(Dispatchers.Default) {
            buildMonoLibrarySnapshot(filter, catalog, playlists, favorites, favoriteTimestamps, query, sort, direction, strings, copy)
        }
    }
    val listState = rememberSaveable(filterName, saver = LazyListState.Saver) { LazyListState() }
    val scope = rememberCoroutineScope()
    val spread = remember { Animatable(0f) }
    LaunchedEffect(listState, animations) {
        if (!animations) {
            spread.snapTo(0f)
            return@LaunchedEffect
        }
        snapshotFlow { listState.isScrollInProgress }.collectLatest { moving ->
            if (moving) {
                spread.animateTo(1f, tween(260))
            } else {
                spread.animateTo(0f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessLow))
            }
        }
    }
    BackHandler(enabled = backEnabled && searching && smartCollection == null) {
        searching = false
        query = ""
    }
    BackHandler(enabled = backEnabled && smartCollection != null) { smartCollection = null }
    val latestState by rememberUpdatedState(state)
    val menuFor: (MonoLibraryEntry) -> List<MonoLibraryMenuItem> = { entry ->
        monoLibraryMenu(entry, latestState, strings, viewModel, { addTracks = it }, { deletingPlaylist = it }, { deletingDownload = it })
    }
    val open: (MonoLibraryEntry) -> Unit = { entry -> openMonoLibraryEntry(viewModel, entry, latestState.favorites) }
    val selectFilter: (MonoLibraryFilter) -> Unit = { next -> filterName = next.name }
    val current = snapshot?.takeIf { it.filter == filter }
    val deck = current?.deck.orEmpty()
    val scrubberEligible = filter != MonoLibraryFilter.Downloads && deck.size >= MonoScrubberMinimum
    val showTiles = filter == MonoLibraryFilter.All && query.isBlank()
    val deckStart = 1 + (if (searching) 1 else 0) + 1 + (if (showTiles) 1 else 0) + 1
    val bottomPadding = levyraTabContentBottomPadding(state.currentTrack != null) + 16.dp
    val numbers = remember(strings.code) { NumberFormat.getIntegerInstance(Locale.forLanguageTag(strings.code)) }
    val caps = remember(strings.code) { Locale.forLanguageTag(strings.code) }

    Box(Modifier.fillMaxSize().background(MonoDesign.base).statusBarsPadding()) {
        LazyColumn(state = listState, contentPadding = PaddingValues(bottom = bottomPadding), modifier = Modifier.fillMaxSize()) {
            item(key = "title", contentType = "title") {
                MonoLibraryHeader(
                    strings = strings,
                    copy = copy,
                    searching = searching,
                    onToggleSearch = {
                        searching = !searching
                        if (!searching) query = ""
                    },
                    onCreate = { creating = true },
                    onImport = { viewModel.openPlaylistImport() },
                    onOpenTools = onOpenTools
                )
            }
            if (searching) {
                item(key = "search", contentType = "search") { MonoLibrarySearchField(strings, query) { query = it } }
            }
            item(key = "pills", contentType = "pills") { MonoLibraryPills(filter, strings, copy, selectFilter) }
            if (showTiles) {
                item(key = "tiles", contentType = "tiles") {
                    val visiblePlaylists = remember(playlists) { playlists.count { !it.hidden } }
                    val followed = state.followedArtists
                    Column {
                    MonoLibraryTiles(
                        hero = MonoShelfTile(
                                Icons.Outlined.FavoriteBorder, strings.favorites, strings.formatTrackCount(favorites.size),
                                favorites.take(6).map { it.thumbnailUrl }, false
                            ) { smartCollection = SMART_COLLECTION_FAVORITES },
                        tiles = listOf(
                            MonoShelfTile(
                                Icons.AutoMirrored.Outlined.QueueMusic, strings.playlistsPlain, copy.collections(visiblePlaylists, numbers.format(visiblePlaylists)),
                                playlists.filterNot { it.hidden }.map { it.coverUrl.ifBlank { it.tracks.firstOrNull()?.thumbnailUrl.orEmpty() } }, false
                            ) { selectFilter(MonoLibraryFilter.Playlists) },
                            MonoShelfTile(
                                Icons.Outlined.PersonOutline, strings.artists, numbers.format(followed.size),
                                followed.map { it.thumbnailUrl }, true
                            ) { selectFilter(MonoLibraryFilter.Artists) },
                            MonoShelfTile(
                                Icons.Outlined.History, strings.recent, copy.recentSubtitle,
                                state.recentListens.take(6).map { it.thumbnailUrl }, false
                            ) { smartCollection = SMART_COLLECTION_RECENT },
                            MonoShelfTile(
                                Icons.Outlined.FileDownload, strings.offline, strings.formatTrackCount(catalog.offlineItems.size),
                                catalog.offlineItems.take(6).map { it.track.thumbnailUrl }, false
                            ) { selectFilter(MonoLibraryFilter.Downloads) }
                        )
                    )
                    MonoPulseCard(state.listeningPulse, viewModel::openListeningInsights)
                    }
                }
            }
            if (filter != MonoLibraryFilter.Downloads) {
                item(key = "deck-header", contentType = "section") {
                    MonoDeckHeader(
                        title = when (filter) {
                            MonoLibraryFilter.Playlists -> strings.playlistsPlain
                            MonoLibraryFilter.Albums -> strings.albumsPlain
                            MonoLibraryFilter.Artists -> strings.artists
                            else -> copy.deckTitle
                        },
                        overline = if (current == null) copy.deckOverline else "${copy.deckOverline} · ${numbers.format(deck.size)}",
                        strings = strings,
                        sort = sort,
                        direction = direction,
                        onSort = { next, nextDirection -> viewModel.setLibrarySort(next, nextDirection) }
                    )
                }
                if (current != null && deck.isEmpty()) {
                    item(key = "deck-empty", contentType = "empty") {
                        MonoLibraryEmpty(filter, strings, query) { creating = true }
                    }
                }
                itemsIndexed(deck, key = { _, entry -> entry.key }, contentType = { _, _ -> "deck-card" }) { index, entry ->
                    MonoDeckCard(
                        index = deckStart + index,
                        title = entry.title,
                        meta = entry.meta.uppercase(caps),
                        ordinal = (index + 1).toString().padStart(2, '0'),
                        covers = entry.covers,
                        tint = MonoDesign.moodTints[Math.floorMod(entry.key.hashCode(), MonoDesign.moodTints.size)],
                        last = index == deck.lastIndex,
                        listState = listState,
                        spread = { spread.value },
                        onOpen = { open(entry) },
                        menu = { menuFor(entry) },
                        modifier = Modifier.padding(start = MonoDesign.gutter, end = if (scrubberEligible) 40.dp else MonoDesign.gutter)
                    )
                }
            }
            val rows = current?.rows.orEmpty()
            if (filter == MonoLibraryFilter.All && rows.isNotEmpty()) {
                item(key = "recent-header", contentType = "section") {
                    MonoSectionHeader(
                        copy.recentlyAdded,
                        Modifier.padding(top = MonoDesign.sectionGap, bottom = 4.dp),
                        overline = copy.recentOverline
                    )
                }
            }
            if (filter == MonoLibraryFilter.Downloads) {
                item(key = "downloads-header", contentType = "section") {
                    MonoSectionHeader(
                        strings.downloads,
                        Modifier.padding(top = 8.dp, bottom = 4.dp),
                        overline = strings.formatTrackCount(catalog.offlineItems.size),
                        action = copy.manage,
                        onAction = onOpenDownloads
                    )
                }
                if (current != null && rows.isEmpty()) {
                    item(key = "downloads-empty", contentType = "empty") {
                        MonoLibraryEmpty(filter, strings, query) { creating = true }
                    }
                }
            }
            items(rows, key = { "row-${it.key}" }, contentType = { "row" }) { entry ->
                MonoLibraryRow(entry, entry.track?.id?.let { it == state.currentTrack?.id } == true, { open(entry) }) { menuFor(entry) }
            }
        }
        if (scrubberEligible) {
            val scrubberVisible by remember(listState, deckStart, deck.size) {
                derivedStateOf {
                    val visible = listState.layoutInfo.visibleItemsInfo
                    visible.isNotEmpty() && visible.last().index >= deckStart + 2 && visible.first().index < deckStart + deck.size
                }
            }
            val slots = current?.firstIndexBySlot
            AnimatedVisibility(
                visible = scrubberVisible && slots != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp, bottom = bottomPadding / 2)
            ) {
                val available = remember(slots) { BooleanArray(MonoScrubberLetters.length) { (slots?.get(it) ?: -1) >= 0 } }
                MonoAlphaScrubber(
                    available = available,
                    label = copy.scrubber,
                    onLetter = { slot ->
                        val target = slots?.let { monoScrubberTarget(slot, it) } ?: -1
                        if (target >= 0) scope.launch { listState.scrollToItem(deckStart + target) }
                    },
                    modifier = Modifier.width(32.dp).heightIn(max = 432.dp).fillMaxHeight(0.62f)
                )
            }
        }
        smartCollection?.let { id ->
            SmartCollectionDetail(
                collectionId = id,
                state = state,
                tracks = if (id == SMART_COLLECTION_RECENT) catalog.recent else state.favorites,
                viewModel = viewModel,
                onClose = { smartCollection = null }
            )
        }
    }
    MonoLibraryDialogs(
        viewModel = viewModel,
        strings = strings,
        playlists = playlists,
        creating = creating,
        addTracks = addTracks,
        deletingPlaylist = deletingPlaylist,
        deletingDownload = deletingDownload,
        deleteOffline = deleteOffline,
        onDismissCreate = { creating = false },
        onClearAddTracks = { addTracks = emptyList() },
        onDismissDeletePlaylist = { deletingPlaylist = null },
        onDismissDeleteDownload = { deletingDownload = null }
    )
}

private fun openMonoLibraryEntry(viewModel: LibraryViewModel, entry: MonoLibraryEntry, favorites: List<Track>) {
    when (entry) {
        is MonoLibraryEntry.PlaylistItem -> viewModel.openPlaylist(entry.playlist.id)
        is MonoLibraryEntry.AlbumItem -> viewModel.openAlbum(entry.album.toAlbumHit())
        is MonoLibraryEntry.ArtistItem -> viewModel.openArtistReference(entry.artist.name, entry.artist.browseId, entry.artist.artworkUrl)
        is MonoLibraryEntry.SongItem -> viewModel.playFrom(favorites.ifEmpty { listOf(entry.track) }, entry.track)
        is MonoLibraryEntry.OfflineItem -> viewModel.playDownloaded(entry.item.download)
    }
}

private fun monoLibraryMenu(
    entry: MonoLibraryEntry,
    state: LevyraUiState,
    strings: LevyraStrings,
    viewModel: LibraryViewModel,
    onAddToPlaylist: (List<Track>) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit,
    onDeleteDownload: (DownloadedTrack) -> Unit
): List<MonoLibraryMenuItem> = buildList {
    when (entry) {
        is MonoLibraryEntry.PlaylistItem -> {
            val playlist = entry.playlist
            if (playlist.tracks.isNotEmpty()) {
                add(MonoLibraryMenuItem(strings.play) { viewModel.playPlaylist(playlist.id) })
                add(MonoLibraryMenuItem(strings.addToQueue) { viewModel.addTracksToQueue(playlist.tracks) })
                add(MonoLibraryMenuItem(strings.downloadPlaylist) { viewModel.exportTracks(playlist.tracks, playlist.name) })
            }
            add(MonoLibraryMenuItem(strings.playlistStudioEdit) { viewModel.openPlaylistStudio(playlist.id) })
            add(MonoLibraryMenuItem(strings.delete) { onDeletePlaylist(playlist) })
        }
        is MonoLibraryEntry.AlbumItem -> {
            val album = entry.album
            album.tracks.firstOrNull()?.let { first ->
                add(MonoLibraryMenuItem(strings.play) { viewModel.playFrom(album.tracks, first) })
                add(MonoLibraryMenuItem(strings.addToQueue) { viewModel.addTracksToQueue(album.tracks) })
                add(MonoLibraryMenuItem(strings.addToPlaylist) { onAddToPlaylist(album.tracks) })
                add(MonoLibraryMenuItem(strings.download) { viewModel.exportTracks(album.tracks, album.title) })
            }
            add(MonoLibraryMenuItem(strings.openAlbum) { viewModel.openAlbum(album.toAlbumHit()) })
        }
        is MonoLibraryEntry.ArtistItem -> {
            val artist = entry.artist
            artist.tracks.firstOrNull()?.let { first ->
                add(MonoLibraryMenuItem(strings.play) { viewModel.playFrom(artist.tracks, first) })
            }
            add(MonoLibraryMenuItem(strings.openArtist) { viewModel.openArtistReference(artist.name, artist.browseId, artist.artworkUrl) })
            val followed = (artist.browseId.isNotBlank() && artist.browseId in state.followedArtistKeys) ||
                artist.name.trim().lowercase() in state.followedArtistKeys
            add(MonoLibraryMenuItem(if (followed) strings.followingArtist else strings.followArtist) {
                viewModel.toggleFollowArtistReference(artist.name, artist.browseId, artist.artworkUrl)
            })
        }
        is MonoLibraryEntry.SongItem -> {
            val track = entry.track
            add(MonoLibraryMenuItem(if (track.id in state.favoriteIds) strings.removeFromFavorites else strings.addToFavorites) { viewModel.toggleFavorite(track) })
            add(MonoLibraryMenuItem(strings.playNext) { viewModel.playTracksNext(listOf(track)) })
            add(MonoLibraryMenuItem(strings.addToQueue) { viewModel.addToQueue(track) })
            add(MonoLibraryMenuItem(strings.addToPlaylist) { onAddToPlaylist(listOf(track)) })
            add(MonoLibraryMenuItem(strings.openArtist) { viewModel.openArtist(track) })
            val download = libraryDownloadForTrack(track, state.downloads)
            if (download != null) {
                add(MonoLibraryMenuItem(strings.deleteDownload) { onDeleteDownload(download) })
            } else {
                add(MonoLibraryMenuItem(strings.download) { viewModel.exportTrack(track) })
            }
        }
        is MonoLibraryEntry.OfflineItem -> {
            val track = entry.item.track
            val download = entry.item.download
            add(MonoLibraryMenuItem(if (track.id in state.favoriteIds) strings.removeFromFavorites else strings.addToFavorites) { viewModel.toggleFavorite(track) })
            add(MonoLibraryMenuItem(strings.addToQueue) { viewModel.addToQueue(track) })
            add(MonoLibraryMenuItem(strings.addToPlaylist) { onAddToPlaylist(listOf(track)) })
            if (download.ownership == DownloadOwnership.SMART_OFFLINE) {
                add(MonoLibraryMenuItem(strings.download) { viewModel.exportTrack(track) })
            }
            add(MonoLibraryMenuItem(strings.deleteDownload) { onDeleteDownload(download) })
        }
    }
}

@Composable
private fun MonoLibraryHeader(
    strings: LevyraStrings,
    copy: MonoLibraryCopy,
    searching: Boolean,
    onToggleSearch: () -> Unit,
    onCreate: () -> Unit,
    onImport: () -> Unit,
    onOpenTools: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    MonoScreenTitle(strings.libraryTitle, Modifier.padding(top = 12.dp, bottom = 8.dp)) {
        MonoLibraryKey(if (searching) Icons.Rounded.Close else Icons.Rounded.Search, strings.search, onToggleSearch)
        Box {
            Box(
                Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = copy.options) { menuOpen = true }
                    .semantics { contentDescription = copy.options },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(MonoDesign.surface).border(1.dp, MonoDesign.hairlineStrong, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Person, null, tint = MonoDesign.text, modifier = Modifier.size(18.dp))
                }
            }
            DropdownMenu(menuOpen, { menuOpen = false }) {
                listOf(
                    strings.newPlaylist to onCreate,
                    strings.playlistImportCopy().action to onImport,
                    copy.allTools to onOpenTools
                ).forEach { (label, action) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            menuOpen = false
                            action()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MonoLibraryKey(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, tint = MonoDesign.textSecondary, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun MonoLibrarySearchField(strings: LevyraStrings, query: String, onQuery: (String) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val style = MaterialTheme.typography.bodyLarge.copy(color = MonoDesign.text)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = MonoDesign.gutter, vertical = 4.dp).heightIn(min = 48.dp)
            .monoPanel(MonoDesign.pillShape, MonoDesign.surface).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, null, tint = MonoDesign.textSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(strings.search, style = style.copy(color = MonoDesign.textFaint), maxLines = 1, modifier = Modifier.clearAndSetSemantics { })
            }
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = style,
                cursorBrush = SolidColor(MonoDesign.accent),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester).semantics { contentDescription = strings.search }
            )
        }
    }
}

@Composable
private fun MonoLibraryPills(selected: MonoLibraryFilter, strings: LevyraStrings, copy: MonoLibraryCopy, onSelect: (MonoLibraryFilter) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = MonoDesign.gutter),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        items(MonoLibraryFilter.entries, key = { it.name }, contentType = { "pill" }) { option ->
            val label = when (option) {
                MonoLibraryFilter.All -> copy.yourLibrary
                MonoLibraryFilter.Playlists -> strings.playlistsPlain
                MonoLibraryFilter.Albums -> strings.albumsPlain
                MonoLibraryFilter.Artists -> strings.artists
                MonoLibraryFilter.Downloads -> strings.downloads
            }
            MonoPill(
                label,
                option == selected,
                { onSelect(option) },
                Modifier.semantics {
                    this.selected = option == selected
                    role = Role.Tab
                }
            )
        }
    }
}

internal class MonoShelfTile(
    val icon: ImageVector,
    val title: String,
    val detail: String,
    val covers: List<String>,
    val round: Boolean,
    val onClick: () -> Unit
)

@Composable
private fun MonoLibraryTiles(hero: MonoShelfTile, tiles: List<MonoShelfTile>) {
    Column(
        Modifier.fillMaxWidth().padding(start = MonoDesign.gutter, end = MonoDesign.gutter, top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MonoShelfHero(hero)
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { tile -> MonoShelfCard(tile, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private fun Modifier.monoShelfSurface(glow: Boolean): Modifier = drawBehind {
    drawRect(Brush.linearGradient(listOf(MonoDesign.raised, MonoDesign.surface), Offset.Zero, Offset(size.width, size.height)))
    if (glow) drawRect(Brush.radialGradient(listOf(MonoDesign.accent.copy(alpha = 0.16f), Color.Transparent), center = Offset(0f, 0f), radius = size.width * 0.8f))
}

@Composable
private fun MonoShelfHero(tile: MonoShelfTile) {
    Row(
        Modifier.fillMaxWidth().height(132.dp)
            .clip(MonoDesign.panelShape)
            .monoShelfSurface(glow = true)
            .border(1.dp, MonoDesign.hairline, MonoDesign.panelShape)
            .clickable(role = Role.Button, onClickLabel = tile.title, onClick = tile.onClick)
            .padding(start = 18.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(tile.icon, null, tint = MonoDesign.accent, modifier = Modifier.size(22.dp))
            Text(tile.title, style = MaterialTheme.typography.titleLarge, color = MonoDesign.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            MonoOverline(tile.detail)
        }
        MonoCoverFan(tile.covers, 76.dp, tile.round, Modifier.width(150.dp).fillMaxHeight())
    }
}

@Composable
private fun MonoShelfCard(tile: MonoShelfTile, modifier: Modifier) {
    Box(
        modifier.height(118.dp)
            .clip(MonoDesign.cardShape)
            .monoShelfSurface(glow = false)
            .border(1.dp, MonoDesign.hairline, MonoDesign.cardShape)
            .clickable(role = Role.Button, onClickLabel = tile.title, onClick = tile.onClick)
    ) {
        MonoCoverFan(tile.covers, 46.dp, tile.round, Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 8.dp).width(86.dp).height(58.dp))
        Column(Modifier.align(Alignment.BottomStart).padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(tile.icon, null, tint = MonoDesign.accent, modifier = Modifier.size(16.dp))
            Text(tile.title, style = MaterialTheme.typography.titleMedium, color = MonoDesign.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(tile.detail, style = MaterialTheme.typography.bodySmall, color = MonoDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MonoCoverFan(covers: List<String>, size: Dp, round: Boolean, modifier: Modifier) {
    val shown = covers.filter(String::isNotBlank).distinct().take(3)
    val shape = if (round) CircleShape else MonoDesign.artShape
    Box(modifier.clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        if (shown.isEmpty()) {
            repeat(3) { index ->
                Box(
                    Modifier.size(size).graphicsLayer { rotationZ = (index - 1) * 10f; translationX = (index - 1) * size.toPx() * 0.42f }
                        .border(1.dp, MonoDesign.hairlineStrong, shape)
                )
            }
        } else {
            shown.forEachIndexed { index, url ->
                val offset = index - (shown.size - 1) / 2f
                Box(
                    Modifier.size(size).graphicsLayer {
                        rotationZ = offset * 9f
                        translationX = offset * size.toPx() * 0.46f
                        shadowElevation = 6.dp.toPx()
                        this.shape = shape
                        clip = true
                    }.background(MonoDesign.raised)
                ) { MonoArtwork(url, Modifier.fillMaxSize()) }
            }
        }
    }
}

@Composable
private fun MonoDeckHeader(
    title: String,
    overline: String,
    strings: LevyraStrings,
    sort: LibrarySort,
    direction: LibrarySortDirection,
    onSort: (LibrarySort, LibrarySortDirection) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().padding(top = MonoDesign.sectionGap, bottom = MonoDesign.headerToContent)) {
        MonoSectionHeader(title, overline = overline, action = sort.libraryLabel(strings), onAction = { open = true })
        Box(Modifier.align(Alignment.BottomEnd).padding(end = MonoDesign.gutter)) {
            DropdownMenu(open, { open = false }) {
                LibrarySort.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.libraryLabel(strings), color = if (option == sort) MonoDesign.accent else MonoDesign.text) },
                        onClick = {
                            open = false
                            onSort(option, option.defaultDirection)
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text(sort.directionLabel(direction.inverted, strings)) },
                    onClick = {
                        open = false
                        onSort(sort, direction.inverted)
                    }
                )
            }
        }
    }
}

@Composable
private fun MonoLibraryRow(entry: MonoLibraryEntry, active: Boolean, onClick: () -> Unit, menu: () -> List<MonoLibraryMenuItem>) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = MonoDesign.rowHeight)
            .then(if (active) Modifier.semantics { selected = true } else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = MonoDesign.gutter, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val track = entry.track
        if (track != null) {
            Box(Modifier.size(MonoDesign.rowArtwork).clip(MonoDesign.artShape).background(MonoDesign.surface)) {
                CoverImage(track, Modifier.fillMaxSize())
            }
        } else {
            LibraryArtwork(entry.covers.firstOrNull().orEmpty(), entry.title, Modifier.size(MonoDesign.rowArtwork), MonoDesign.artShape, false)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                entry.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (active) MonoDesign.accent else MonoDesign.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (entry.meta.isNotBlank()) {
                Text(entry.meta, style = MaterialTheme.typography.bodySmall, color = MonoDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        MonoLibraryMoreKey(menu)
    }
}

@Composable
private fun MonoLibraryEmpty(filter: MonoLibraryFilter, strings: LevyraStrings, query: String, onCreate: () -> Unit) {
    val searching = query.isNotBlank()
    val title = when {
        searching -> strings.emptySearchPrompt
        filter == MonoLibraryFilter.Albums -> strings.albumUnavailable
        filter == MonoLibraryFilter.Artists -> strings.artistProfileUnavailable
        filter == MonoLibraryFilter.Downloads -> strings.noOfflineDownloads
        else -> strings.createFirstPlaylist
    }
    val detail = when {
        searching -> null
        filter == MonoLibraryFilter.Albums -> strings.savedTracks
        filter == MonoLibraryFilter.Artists -> strings.followedArtistsSubtitle
        filter == MonoLibraryFilter.Downloads -> strings.downloadTrackHint
        else -> strings.createFirstPlaylistSubtitle
    }
    val canCreate = !searching && (filter == MonoLibraryFilter.All || filter == MonoLibraryFilter.Playlists)
    Column(
        Modifier.fillMaxWidth().padding(horizontal = MonoDesign.gutter, vertical = 8.dp)
            .monoPanel(MonoDesign.panelShape, MonoDesign.surface)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MonoWaveLine(playing = false, modifier = Modifier.fillMaxWidth().height(24.dp), color = MonoDesign.hairlineStrong)
        Text(title, style = MaterialTheme.typography.titleMedium, color = MonoDesign.text, textAlign = TextAlign.Center)
        if (detail != null) {
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MonoDesign.textSecondary, textAlign = TextAlign.Center)
        }
        if (canCreate) {
            MonoPill(strings.newPlaylist, true, onCreate, Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun MonoLibraryDialogs(
    viewModel: LibraryViewModel,
    strings: LevyraStrings,
    playlists: List<Playlist>,
    creating: Boolean,
    addTracks: List<Track>,
    deletingPlaylist: Playlist?,
    deletingDownload: DownloadedTrack?,
    deleteOffline: (List<DownloadedTrack>) -> Unit,
    onDismissCreate: () -> Unit,
    onClearAddTracks: () -> Unit,
    onDismissDeletePlaylist: () -> Unit,
    onDismissDeleteDownload: () -> Unit
) {
    if (creating) {
        LibraryNameDialog(strings.newPlaylist, "", strings.create, onDismissCreate) { name ->
            viewModel.createPlaylist(name)
            onDismissCreate()
        }
    }
    if (addTracks.isNotEmpty()) {
        AddTracksToPlaylistDialog(
            tracks = addTracks,
            playlists = playlists,
            onDismiss = onClearAddTracks,
            onAdd = { playlistId ->
                viewModel.addTracksToPlaylist(playlistId, addTracks)
                onClearAddTracks()
            },
            onCreate = { name ->
                viewModel.createPlaylistWithTracks(name, addTracks)
                onClearAddTracks()
            }
        )
    }
    deletingPlaylist?.let { playlist ->
        MonoLibraryConfirm(strings, strings.delete, playlist.name, onDismissDeletePlaylist) { viewModel.deletePlaylist(playlist.id) }
    }
    deletingDownload?.let { download ->
        MonoLibraryConfirm(strings, strings.deleteDownload, download.title, onDismissDeleteDownload) { deleteOffline(listOf(download)) }
    }
}

@Composable
private fun MonoLibraryConfirm(strings: LevyraStrings, title: String, body: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                }
            ) { Text(strings.delete) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}
