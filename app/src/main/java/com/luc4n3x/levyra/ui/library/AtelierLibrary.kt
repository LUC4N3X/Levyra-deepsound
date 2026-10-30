package com.luc4n3x.levyra.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size as CoilSize
import com.luc4n3x.levyra.domain.DownloadOwnership
import com.luc4n3x.levyra.domain.DownloadedTrack
import com.luc4n3x.levyra.domain.LibrarySort
import com.luc4n3x.levyra.domain.LibrarySortDirection
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.filterByTagIds
import com.luc4n3x.levyra.domain.hiddenInLibrary
import com.luc4n3x.levyra.domain.visibleInLibrary
import com.luc4n3x.levyra.ui.CoverImage
import com.luc4n3x.levyra.ui.i18n.AtelierCopy
import com.luc4n3x.levyra.ui.i18n.LevyraAtelierStrings
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.formatLibraryBytes
import com.luc4n3x.levyra.ui.i18n.homeStudioCopy
import com.luc4n3x.levyra.ui.levyraTabContentBottomPadding
import com.luc4n3x.levyra.ui.rememberCurrentGreetingHour
import com.luc4n3x.levyra.ui.theme.AtelierDesign
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.LibraryViewModel
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class AtelierLibraryTab { Playlists, Albums, Artists, Songs, Favorites, Downloads }

private val AtelierLibrarySleeveMin = 150.dp
private const val AtelierLibraryBleedAlpha = 0.14f
private const val AtelierLibraryBleedPx = 128

private class AtelierLibraryRackCell(
    val label: String,
    val count: Int,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit
)

private sealed interface AtelierLibraryEntry {
    val key: String
    val title: String
    val subtitle: String
    val imageUrl: String get() = ""
    val artworkTrack: Track? get() = null
    val circle: Boolean get() = false
    val explicit: Boolean get() = false

    class PlaylistEntry(val playlist: Playlist, override val subtitle: String) : AtelierLibraryEntry {
        override val key = "playlist-${playlist.id}"
        override val title = playlist.name
        override val imageUrl = playlist.coverUrl.ifBlank { playlist.tracks.firstOrNull()?.thumbnailUrl.orEmpty() }
    }

    class AlbumEntry(val album: LibraryAlbum) : AtelierLibraryEntry {
        override val key = "album-${album.key}"
        override val title = album.title
        override val subtitle = album.artist
        override val imageUrl = album.artworkUrl
        override val explicit = album.explicit
    }

    class ArtistEntry(val artist: LibraryArtist, override val subtitle: String) : AtelierLibraryEntry {
        override val key = "artist-${artist.key}"
        override val title = artist.name
        override val imageUrl = artist.artworkUrl
        override val circle = true
    }

    class SongEntry(val track: Track) : AtelierLibraryEntry {
        override val key = "song-${libraryTrackKey(track)}"
        override val title = track.title
        override val subtitle = track.artist
        override val artworkTrack = track
        override val explicit = track.explicit
    }

    class OfflineEntry(val item: LibraryOfflineItem, override val subtitle: String) : AtelierLibraryEntry {
        override val key = "offline-${item.key}"
        override val title = item.track.title
        override val artworkTrack = item.track
        override val explicit = item.track.explicit
    }
}

private class AtelierLibrarySnapshot(val tab: AtelierLibraryTab, val entries: List<AtelierLibraryEntry>, val songs: List<Track>)

private class AtelierLibraryMenuItem(val label: String, val action: () -> Unit)

private fun atelierLibraryCaps(text: String, code: String): String = text.uppercase(Locale.forLanguageTag(code))

private fun buildAtelierLibrarySnapshot(
    tab: AtelierLibraryTab,
    catalog: LibraryCatalog,
    favorites: List<Track>,
    playlists: List<Playlist>,
    tagIds: Set<String>,
    hiddenOnly: Boolean,
    query: String,
    sort: LibrarySort,
    direction: LibrarySortDirection,
    strings: LevyraStrings
): AtelierLibrarySnapshot {
    val songs = when (tab) {
        AtelierLibraryTab.Songs -> filterLibraryTracks(catalog.tracks, query, sort, direction, catalog::recencyOf)
        AtelierLibraryTab.Favorites -> filterLibraryTracks(favorites, query, sort, direction, catalog::recencyOf)
        else -> emptyList()
    }
    val entries: List<AtelierLibraryEntry> = when (tab) {
        AtelierLibraryTab.Playlists -> {
            val scoped = if (hiddenOnly) playlists.hiddenInLibrary() else playlists.visibleInLibrary()
            filterLibraryPlaylists(scoped.filterByTagIds(tagIds), query, sort, direction)
                .map { AtelierLibraryEntry.PlaylistEntry(it, strings.formatTrackCount(it.size)) }
        }
        AtelierLibraryTab.Albums -> filterLibraryAlbums(catalog.albums, query, sort, direction).map { AtelierLibraryEntry.AlbumEntry(it) }
        AtelierLibraryTab.Artists -> filterLibraryArtists(catalog.artists, query, sort, direction).map { artist ->
            AtelierLibraryEntry.ArtistEntry(artist, if (artist.tracks.isEmpty()) "" else strings.formatTrackCount(artist.tracks.size))
        }
        AtelierLibraryTab.Songs, AtelierLibraryTab.Favorites -> songs.map { AtelierLibraryEntry.SongEntry(it) }
        AtelierLibraryTab.Downloads -> filterLibraryOfflineItems(catalog.offlineItems, query, sort, direction).map { item ->
            AtelierLibraryEntry.OfflineEntry(
                item,
                listOf(item.track.artist, strings.formatLibraryBytes(item.download.sizeBytes)).filter(String::isNotBlank).joinToString(" · ")
            )
        }
    }
    return AtelierLibrarySnapshot(tab, entries.distinctBy { it.key }, songs)
}

@Composable
internal fun AtelierLibraryScreen(
    viewModel: LibraryViewModel,
    state: LevyraUiState,
    catalog: LibraryCatalog,
    onOpenDownloads: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenShelf: () -> Unit,
    backEnabled: Boolean = true
) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraAtelierStrings.forCode(strings.code) }
    var tabName by rememberSaveable { mutableStateOf(AtelierLibraryTab.Playlists.name) }
    val tab = AtelierLibraryTab.entries.firstOrNull { it.name == tabName } ?: AtelierLibraryTab.Playlists
    var grid by rememberSaveable { mutableStateOf(true) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedTagIds by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var showHidden by rememberSaveable { mutableStateOf(false) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var addTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var deletingPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var deletingDownload by remember { mutableStateOf<DownloadedTrack?>(null) }
    val deleteOffline = rememberOfflineDeleteHandler(viewModel)
    val sort = state.interfaceSettings.librarySort
    val direction = state.interfaceSettings.librarySortDirection
    val playlists = state.playlists
    val favorites = state.favorites
    val hiddenCount = remember(playlists) { playlists.count { it.hidden } }
    val hiddenOnly = showHidden && hiddenCount > 0
    val activeTagIds = remember(selectedTagIds, state.playlistTags) {
        val known = state.playlistTags.mapTo(hashSetOf()) { it.id }
        selectedTagIds.filterTo(hashSetOf()) { it in known }.toSet()
    }
    val snapshot by produceState<AtelierLibrarySnapshot?>(
        null, tab, catalog, favorites, playlists, activeTagIds, hiddenOnly, query, sort, direction, strings.code
    ) {
        value = withContext(Dispatchers.Default) {
            buildAtelierLibrarySnapshot(tab, catalog, favorites, playlists, activeTagIds, hiddenOnly, query, sort, direction, strings)
        }
    }
    val latestState by rememberUpdatedState(state)
    val gridState = rememberSaveable(tabName, saver = LazyGridState.Saver) { LazyGridState() }
    var headerOffsetPx by remember { mutableFloatStateOf(0f) }
    var collapsiblePx by remember { mutableIntStateOf(0) }
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val headerConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                headerOffsetPx = (headerOffsetPx + consumed.y).coerceIn(-collapsiblePx.toFloat(), 0f)
                return Offset.Zero
            }
        }
    }
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.canScrollBackward }.collect { canScroll -> if (!canScroll) headerOffsetPx = 0f }
    }
    BackHandler(enabled = backEnabled && searching) {
        searching = false
        query = ""
    }
    val headerHeight = with(LocalDensity.current) { headerHeightPx.toDp() }
    val bottomPadding = levyraTabContentBottomPadding(state.currentTrack != null) + 16.dp
    val currentId = state.currentTrack?.id
    val menuFor: (AtelierLibraryEntry) -> List<AtelierLibraryMenuItem> = { entry ->
        atelierLibraryMenu(
            entry = entry,
            state = latestState,
            strings = strings,
            viewModel = viewModel,
            onAddToPlaylist = { addTracks = it },
            onDeletePlaylist = { deletingPlaylist = it },
            onDeleteDownload = { deletingDownload = it }
        )
    }
    val openEntry: (AtelierLibraryEntry) -> Unit = { entry ->
        when (entry) {
            is AtelierLibraryEntry.PlaylistEntry -> viewModel.openPlaylist(entry.playlist.id)
            is AtelierLibraryEntry.AlbumEntry -> viewModel.openAlbum(entry.album.toAlbumHit())
            is AtelierLibraryEntry.ArtistEntry -> viewModel.openArtistReference(entry.artist.name, entry.artist.browseId, entry.artist.artworkUrl)
            is AtelierLibraryEntry.SongEntry -> viewModel.playFrom(snapshot?.songs.orEmpty().ifEmpty { listOf(entry.track) }, entry.track)
            is AtelierLibraryEntry.OfflineEntry -> viewModel.playDownloaded(entry.item.download)
        }
    }
    Box(
        Modifier.fillMaxSize().background(AtelierDesign.room).statusBarsPadding().clipToBounds().nestedScroll(headerConnection)
    ) {
        LazyVerticalGrid(
            state = gridState,
            columns = if (grid) GridCells.Adaptive(AtelierLibrarySleeveMin) else GridCells.Fixed(1),
            contentPadding = PaddingValues(
                start = if (grid) AtelierDesign.gutter else 0.dp,
                end = if (grid) AtelierDesign.gutter else 0.dp,
                top = headerHeight + if (grid) 16.dp else 4.dp,
                bottom = bottomPadding
            ),
            horizontalArrangement = Arrangement.spacedBy(AtelierDesign.itemGap),
            verticalArrangement = Arrangement.spacedBy(if (grid) 20.dp else 0.dp),
            modifier = Modifier.fillMaxSize().then(if (searching) Modifier.imePadding() else Modifier)
        ) {
            val current = snapshot
            if (current != null) {
                items(
                    current.entries,
                    key = { it.key },
                    contentType = { entry -> if (grid) "sleeve" else if (entry.artworkTrack != null) "track-row" else "item-row" }
                ) { entry ->
                    val active = entry.artworkTrack?.id?.let { it == currentId } == true
                    if (grid) {
                        AtelierLibrarySleeve(entry, active, copy, { openEntry(entry) }) { menuFor(entry) }
                    } else {
                        AtelierLibraryRow(entry, active, copy, { openEntry(entry) }) { menuFor(entry) }
                    }
                }
                if (current.tab == tab && current.entries.isEmpty()) {
                    item(key = "empty", span = { GridItemSpan(maxLineSpan) }, contentType = "empty") {
                        AtelierLibraryEmpty(tab, strings, query, hiddenOnly, activeTagIds.isNotEmpty()) { creating = true }
                    }
                }
            }
        }
        Column(
            Modifier.fillMaxWidth()
                .graphicsLayer { translationY = headerOffsetPx }
                .background(AtelierDesign.room)
                .onSizeChanged { headerHeightPx = it.height }
        ) {
            val selectTab: (AtelierLibraryTab) -> Unit = { next ->
                if (next != tab) {
                    tabName = next.name
                    if (next != AtelierLibraryTab.Playlists) {
                        selectedTagIds = emptySet()
                        showHidden = false
                    }
                }
            }
            val rack = listOf(
                AtelierLibraryRackCell(strings.favorites, favorites.size, Icons.Outlined.FavoriteBorder, tab == AtelierLibraryTab.Favorites) { selectTab(AtelierLibraryTab.Favorites) },
                AtelierLibraryRackCell(strings.downloads, catalog.offlineItems.size, Icons.Outlined.DownloadForOffline, tab == AtelierLibraryTab.Downloads, onOpenDownloads),
                AtelierLibraryRackCell(strings.playlistsPlain, playlists.size - hiddenCount, Icons.AutoMirrored.Outlined.QueueMusic, tab == AtelierLibraryTab.Playlists) { selectTab(AtelierLibraryTab.Playlists) },
                AtelierLibraryRackCell(strings.albumsPlain, catalog.albums.size, Icons.Outlined.Album, tab == AtelierLibraryTab.Albums) { selectTab(AtelierLibraryTab.Albums) },
                AtelierLibraryRackCell(strings.artists, catalog.artists.size, Icons.Outlined.Person, tab == AtelierLibraryTab.Artists) { selectTab(AtelierLibraryTab.Artists) },
                AtelierLibraryRackCell(strings.songsPlain, catalog.tracks.size, Icons.Outlined.MusicNote, tab == AtelierLibraryTab.Songs) { selectTab(AtelierLibraryTab.Songs) }
            )
            AtelierLibraryMasthead(
                strings = strings,
                copy = copy,
                userName = state.userName,
                rack = rack,
                modifier = Modifier.onSizeChanged { collapsiblePx = it.height },
                onCreate = { creating = true },
                onOpenShelf = onOpenShelf,
                onOpenTools = onOpenTools
            )
            AtelierLibraryTabs(tab, strings, selectTab)
            if (searching) {
                AtelierLibrarySearchField(strings, query) { query = it }
            }
            if (tab == AtelierLibraryTab.Playlists && (state.playlistTags.isNotEmpty() || hiddenCount > 0)) {
                AtelierLibraryPlaylistFilters(
                    state = state,
                    strings = strings,
                    activeTagIds = activeTagIds,
                    hiddenOnly = hiddenOnly,
                    hiddenCount = hiddenCount,
                    onClearTags = { selectedTagIds = emptySet() },
                    onToggleTag = { id -> selectedTagIds = if (id in selectedTagIds) selectedTagIds - id else selectedTagIds + id },
                    onToggleHidden = { showHidden = !showHidden }
                )
            }
            AtelierLibraryControls(
                strings = strings,
                sort = sort,
                direction = direction,
                grid = grid,
                searching = searching,
                onSort = { viewModel.setLibrarySort(it, it.defaultDirection) },
                onToggleDirection = { viewModel.setLibrarySort(sort, direction.inverted) },
                onToggleLayout = { grid = !grid },
                onToggleSearch = {
                    searching = !searching
                    if (!searching) query = ""
                }
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(AtelierDesign.hairline))
        }
    }
    if (creating) {
        AtelierLibraryCreateDialog(strings, { creating = false }) { name -> viewModel.createPlaylist(name) }
    }
    if (addTracks.isNotEmpty()) {
        AddTracksToPlaylistDialog(
            tracks = addTracks,
            playlists = playlists,
            onDismiss = { addTracks = emptyList() },
            onAdd = { playlistId ->
                viewModel.addTracksToPlaylist(playlistId, addTracks)
                addTracks = emptyList()
            },
            onCreate = { name ->
                viewModel.createPlaylistWithTracks(name, addTracks)
                addTracks = emptyList()
            }
        )
    }
    deletingPlaylist?.let { playlist ->
        AtelierLibraryDeleteDialog(strings, strings.delete, playlist.name, { deletingPlaylist = null }) {
            viewModel.deletePlaylist(playlist.id)
        }
    }
    deletingDownload?.let { download ->
        AtelierLibraryDeleteDialog(strings, strings.deleteDownload, download.title, { deletingDownload = null }) {
            deleteOffline(listOf(download))
        }
    }
}

private fun atelierLibraryMenu(
    entry: AtelierLibraryEntry,
    state: LevyraUiState,
    strings: LevyraStrings,
    viewModel: LibraryViewModel,
    onAddToPlaylist: (List<Track>) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit,
    onDeleteDownload: (DownloadedTrack) -> Unit
): List<AtelierLibraryMenuItem> = buildList {
    when (entry) {
        is AtelierLibraryEntry.PlaylistEntry -> {
            val playlist = entry.playlist
            if (playlist.tracks.isNotEmpty()) {
                add(AtelierLibraryMenuItem(strings.play) { viewModel.playPlaylist(playlist.id) })
                add(AtelierLibraryMenuItem(strings.addToQueue) { viewModel.addTracksToQueue(playlist.tracks) })
                add(AtelierLibraryMenuItem(strings.downloadPlaylist) { viewModel.exportTracks(playlist.tracks, playlist.name) })
            }
            add(AtelierLibraryMenuItem(strings.playlistStudioEdit) { viewModel.openPlaylistStudio(playlist.id) })
            add(AtelierLibraryMenuItem(strings.delete) { onDeletePlaylist(playlist) })
        }
        is AtelierLibraryEntry.AlbumEntry -> {
            val album = entry.album
            album.tracks.firstOrNull()?.let { first ->
                add(AtelierLibraryMenuItem(strings.play) { viewModel.playFrom(album.tracks, first) })
                add(AtelierLibraryMenuItem(strings.addToQueue) { viewModel.addTracksToQueue(album.tracks) })
                add(AtelierLibraryMenuItem(strings.addToPlaylist) { onAddToPlaylist(album.tracks) })
                add(AtelierLibraryMenuItem(strings.download) { viewModel.exportTracks(album.tracks, album.title) })
            }
            add(AtelierLibraryMenuItem(strings.openAlbum) { viewModel.openAlbum(album.toAlbumHit()) })
        }
        is AtelierLibraryEntry.ArtistEntry -> {
            val artist = entry.artist
            artist.tracks.firstOrNull()?.let { first ->
                add(AtelierLibraryMenuItem(strings.play) { viewModel.playFrom(artist.tracks, first) })
            }
            add(AtelierLibraryMenuItem(strings.openArtist) { viewModel.openArtistReference(artist.name, artist.browseId, artist.artworkUrl) })
            val followed = (artist.browseId.isNotBlank() && artist.browseId in state.followedArtistKeys) ||
                artist.name.trim().lowercase() in state.followedArtistKeys
            add(AtelierLibraryMenuItem(if (followed) strings.followingArtist else strings.followArtist) {
                viewModel.toggleFollowArtistReference(artist.name, artist.browseId, artist.artworkUrl)
            })
        }
        is AtelierLibraryEntry.SongEntry -> {
            val track = entry.track
            add(AtelierLibraryMenuItem(if (track.id in state.favoriteIds) strings.removeFromFavorites else strings.addToFavorites) { viewModel.toggleFavorite(track) })
            add(AtelierLibraryMenuItem(strings.playNext) { viewModel.playTracksNext(listOf(track)) })
            add(AtelierLibraryMenuItem(strings.addToQueue) { viewModel.addToQueue(track) })
            add(AtelierLibraryMenuItem(strings.addToPlaylist) { onAddToPlaylist(listOf(track)) })
            add(AtelierLibraryMenuItem(strings.openArtist) { viewModel.openArtist(track) })
            val download = libraryDownloadForTrack(track, state.downloads)
            if (download != null) {
                add(AtelierLibraryMenuItem(strings.deleteDownload) { onDeleteDownload(download) })
            } else {
                add(AtelierLibraryMenuItem(strings.download) { viewModel.exportTrack(track) })
            }
        }
        is AtelierLibraryEntry.OfflineEntry -> {
            val track = entry.item.track
            val download = entry.item.download
            add(AtelierLibraryMenuItem(if (track.id in state.favoriteIds) strings.removeFromFavorites else strings.addToFavorites) { viewModel.toggleFavorite(track) })
            add(AtelierLibraryMenuItem(strings.addToQueue) { viewModel.addToQueue(track) })
            add(AtelierLibraryMenuItem(strings.addToPlaylist) { onAddToPlaylist(listOf(track)) })
            if (download.ownership == DownloadOwnership.SMART_OFFLINE) {
                add(AtelierLibraryMenuItem(strings.download) { viewModel.exportTrack(track) })
            }
            add(AtelierLibraryMenuItem(strings.deleteDownload) { onDeleteDownload(download) })
        }
    }
}

@Composable
private fun AtelierLibraryMasthead(
    strings: LevyraStrings,
    copy: AtelierCopy,
    userName: String,
    rack: List<AtelierLibraryRackCell>,
    modifier: Modifier,
    onCreate: () -> Unit,
    onOpenShelf: () -> Unit,
    onOpenTools: () -> Unit
) {
    val hour by rememberCurrentGreetingHour()
    val greeting = remember(strings.code, userName, hour) { strings.formatGreeting(userName, hour) }
    Column(modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(start = AtelierDesign.gutter, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                atelierLibraryCaps(greeting, strings.code),
                style = MaterialTheme.typography.labelMedium,
                color = AtelierDesign.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            AtelierLibraryIconTarget(Icons.Outlined.Tune, copy.allTools, onOpenTools)
            AtelierLibraryShelfButton(copy.goToShelf, onOpenShelf)
        }
        Row(
            Modifier.fillMaxWidth().padding(start = AtelierDesign.gutter, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                strings.libraryTitle,
                style = MaterialTheme.typography.displayMedium,
                color = AtelierDesign.text,
                modifier = Modifier.weight(1f).semantics { heading() }
            )
            AtelierLibraryIconTarget(Icons.Rounded.Add, strings.newPlaylist, onCreate)
        }
        AtelierLibraryRack(rack, strings.code, Modifier.padding(start = AtelierDesign.gutter, end = AtelierDesign.gutter, top = 14.dp, bottom = 10.dp))
    }
}

@Composable
private fun AtelierLibraryShelfButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(AtelierDesign.cardShape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(24.dp)) {
            val stroke = 1.4.dp.toPx()
            val side = size.minDimension * 0.62f
            val step = (size.minDimension - side) / 2f
            val corner = CornerRadius(1.5.dp.toPx())
            for (index in 2 downTo 0) {
                val origin = Offset(step * (2 - index), step * index)
                val tone = when (index) {
                    0 -> AtelierDesign.accentBright
                    1 -> AtelierDesign.accent
                    else -> AtelierDesign.accent.copy(alpha = 0.55f)
                }
                drawRoundRect(AtelierDesign.room, origin, Size(side, side), corner)
                drawRoundRect(tone, origin, Size(side, side), corner, style = Stroke(stroke))
            }
            val front = Offset(step * 2f, 0f)
            drawCircle(AtelierDesign.accentBright, radius = side * 0.16f, center = Offset(front.x + side / 2f, front.y + side / 2f), style = Stroke(stroke))
        }
    }
}

@Composable
private fun AtelierLibraryRack(cells: List<AtelierLibraryRackCell>, code: String, modifier: Modifier) {
    val numbers = remember(code) { NumberFormat.getIntegerInstance(Locale.forLanguageTag(code)) }
    Column(modifier.fillMaxWidth().selectableGroup()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(AtelierDesign.hairlineStrong))
        cells.chunked(2).forEachIndexed { rowIndex, pair ->
            if (rowIndex > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(AtelierDesign.hairline))
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                pair.forEachIndexed { index, cell ->
                    if (index > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(AtelierDesign.hairline))
                    AtelierLibraryRackSlot(cell, numbers.format(cell.count), code, Modifier.weight(1f))
                }
                if (pair.size == 1) {
                    Box(Modifier.width(1.dp).fillMaxHeight().background(AtelierDesign.hairline))
                    Spacer(Modifier.weight(1f))
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(AtelierDesign.hairlineStrong))
    }
}

@Composable
private fun AtelierLibraryRackSlot(cell: AtelierLibraryRackCell, count: String, code: String, modifier: Modifier) {
    val label = atelierLibraryCaps(cell.label, code)
    Column(
        modifier.fillMaxHeight().heightIn(min = 96.dp)
            .selectable(selected = cell.selected, role = Role.Tab, onClick = cell.onClick)
            .semantics(mergeDescendants = true) { contentDescription = "$count · ${cell.label}" }
            .padding(horizontal = 10.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(cell.icon, null, tint = if (cell.selected) AtelierDesign.accentBright else AtelierDesign.accent, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            count,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Light),
            color = if (cell.selected) AtelierDesign.accentBright else AtelierDesign.text,
            maxLines = 1,
            modifier = Modifier.clearAndSetSemantics { }
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.2.sp),
            color = if (cell.selected) AtelierDesign.text else AtelierDesign.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.clearAndSetSemantics { }
        )
        Spacer(Modifier.height(6.dp))
        Box(Modifier.width(18.dp).height(1.dp).background(if (cell.selected) AtelierDesign.accent else Color.Transparent))
    }
}

@Composable
private fun AtelierLibraryTabs(selected: AtelierLibraryTab, strings: LevyraStrings, onSelect: (AtelierLibraryTab) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = AtelierDesign.gutter - 8.dp),
        modifier = Modifier.fillMaxWidth().selectableGroup()
    ) {
        items(AtelierLibraryTab.entries, key = { it.name }, contentType = { "tab" }) { option ->
            AtelierLibraryTextTab(atelierLibraryTabLabel(option, strings), option == selected, strings.code) { onSelect(option) }
        }
    }
}

private fun atelierLibraryTabLabel(tab: AtelierLibraryTab, strings: LevyraStrings): String = when (tab) {
    AtelierLibraryTab.Playlists -> strings.playlistsPlain
    AtelierLibraryTab.Albums -> strings.albumsPlain
    AtelierLibraryTab.Artists -> strings.artists
    AtelierLibraryTab.Songs -> strings.songsPlain
    AtelierLibraryTab.Favorites -> strings.favorites
    AtelierLibraryTab.Downloads -> strings.downloads
}

@Composable
private fun AtelierLibraryTextTab(label: String, selected: Boolean, code: String, onClick: () -> Unit) {
    Column(
        Modifier.heightIn(min = 48.dp).selectable(selected = selected, role = Role.Tab, onClick = onClick).padding(horizontal = 8.dp).width(IntrinsicSize.Max),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            atelierLibraryCaps(label, code),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) AtelierDesign.text else AtelierDesign.textSecondary,
            maxLines = 1
        )
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(if (selected) AtelierDesign.accent else Color.Transparent))
    }
}

@Composable
private fun AtelierLibraryPlaylistFilters(
    state: LevyraUiState,
    strings: LevyraStrings,
    activeTagIds: Set<String>,
    hiddenOnly: Boolean,
    hiddenCount: Int,
    onClearTags: () -> Unit,
    onToggleTag: (String) -> Unit,
    onToggleHidden: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = AtelierDesign.gutter - 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (state.playlistTags.isNotEmpty()) {
            item(key = "tags-all", contentType = "tab") {
                AtelierLibraryTextTab(strings.all, activeTagIds.isEmpty(), strings.code, onClearTags)
            }
            items(state.playlistTags, key = { "tag-${it.id}" }, contentType = { "tab" }) { tag ->
                AtelierLibraryTextTab(tag.name, tag.id in activeTagIds, strings.code) { onToggleTag(tag.id) }
            }
        }
        if (hiddenCount > 0) {
            item(key = "hidden", contentType = "tab") {
                AtelierLibraryTextTab("${strings.hiddenPlaylists} · $hiddenCount", hiddenOnly, strings.code, onToggleHidden)
            }
        }
    }
}

@Composable
private fun AtelierLibrarySearchField(strings: LevyraStrings, query: String, onQuery: (String) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Light, color = AtelierDesign.text)
    Box(
        Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter, vertical = 8.dp).heightIn(min = 40.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (query.isEmpty()) {
            Text(
                strings.search,
                style = style.copy(color = AtelierDesign.textFaint),
                maxLines = 1,
                modifier = Modifier.clearAndSetSemantics { }
            )
        }
        BasicTextField(
            value = query,
            onValueChange = onQuery,
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(AtelierDesign.accent),
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester).semantics { contentDescription = strings.search }
        )
    }
}

@Composable
private fun AtelierLibraryControls(
    strings: LevyraStrings,
    sort: LibrarySort,
    direction: LibrarySortDirection,
    grid: Boolean,
    searching: Boolean,
    onSort: (LibrarySort) -> Unit,
    onToggleDirection: () -> Unit,
    onToggleLayout: () -> Unit,
    onToggleSearch: () -> Unit
) {
    var sortOpen by remember { mutableStateOf(false) }
    val layoutCopy = strings.homeStudioCopy
    Row(Modifier.fillMaxWidth().padding(start = AtelierDesign.gutter, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            Row(
                Modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClickLabel = strings.librarySortBy) { sortOpen = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    atelierLibraryCaps("${strings.librarySortBy} · ${sort.libraryLabel(strings)}", strings.code),
                    style = MaterialTheme.typography.labelSmall,
                    color = AtelierDesign.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Icon(Icons.Rounded.ArrowDropDown, null, tint = AtelierDesign.textSecondary, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(sortOpen, { sortOpen = false }) {
                LibrarySort.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.libraryLabel(strings)) },
                        onClick = {
                            sortOpen = false
                            onSort(option)
                        }
                    )
                }
            }
        }
        AtelierLibraryIconTarget(
            if (direction == LibrarySortDirection.Ascending) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
            sort.directionLabel(direction, strings),
            onToggleDirection
        )
        AtelierLibraryIconTarget(
            if (grid) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
            if (grid) layoutCopy.layoutList else layoutCopy.layoutGrid,
            onToggleLayout
        )
        AtelierLibraryIconTarget(if (searching) Icons.Rounded.Close else Icons.Rounded.Search, strings.search, onToggleSearch)
    }
}

@Composable
private fun AtelierLibraryArtwork(entry: AtelierLibraryEntry, modifier: Modifier) {
    val shape: Shape = if (entry.circle) CircleShape else AtelierDesign.sleeveShape
    val track = entry.artworkTrack
    if (track != null) {
        Box(modifier.border(1.dp, AtelierDesign.hairline, shape).clip(shape).background(AtelierDesign.panel)) {
            CoverImage(track, Modifier.fillMaxSize())
        }
    } else {
        LibraryArtwork(entry.imageUrl, entry.title, modifier.border(1.dp, AtelierDesign.hairline, shape), shape, false)
    }
}

@Composable
private fun AtelierLibraryBleed(url: String, modifier: Modifier) {
    if (url.isBlank()) return
    val context = LocalContext.current
    val request = remember(url, context) {
        ImageRequest.Builder(context).data(url).size(CoilSize(AtelierLibraryBleedPx, AtelierLibraryBleedPx)).build()
    }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val veil = remember(rtl) {
        val stops = listOf(AtelierDesign.room.copy(alpha = 0.35f), AtelierDesign.room.copy(alpha = 0.8f), AtelierDesign.room)
        Brush.horizontalGradient(if (rtl) stops.reversed() else stops)
    }
    Box(modifier.clipToBounds()) {
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = AtelierLibraryBleedAlpha,
            modifier = Modifier.fillMaxSize()
        )
        Box(Modifier.fillMaxSize().background(veil))
    }
}

@Composable
private fun AtelierLibrarySleeve(
    entry: AtelierLibraryEntry,
    active: Boolean,
    copy: AtelierCopy,
    onClick: () -> Unit,
    menu: () -> List<AtelierLibraryMenuItem>
) {
    Column(
        Modifier.fillMaxWidth()
            .then(if (active) Modifier.semantics { selected = true } else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        AtelierLibraryArtwork(
            entry,
            Modifier.fillMaxWidth().aspectRatio(1f).shadow(12.dp, if (entry.circle) CircleShape else AtelierDesign.sleeveShape)
        )
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Column(Modifier.weight(1f).padding(top = 2.dp)) {
                Text(
                    entry.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (active) AtelierDesign.accentBright else AtelierDesign.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                AtelierLibraryMeta(entry, copy)
            }
            AtelierLibraryMoreTarget(menu)
        }
    }
}

@Composable
private fun AtelierLibraryRow(
    entry: AtelierLibraryEntry,
    active: Boolean,
    copy: AtelierCopy,
    onClick: () -> Unit,
    menu: () -> List<AtelierLibraryMenuItem>
) {
    val bleedUrl = entry.imageUrl.ifBlank { entry.artworkTrack?.thumbnailUrl.orEmpty() }
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth()) {
            AtelierLibraryBleed(bleedUrl, Modifier.matchParentSize())
            Row(
                Modifier.fillMaxWidth().heightIn(min = AtelierDesign.rowHeight)
                    .then(if (active) Modifier.semantics { selected = true } else Modifier)
                    .clickable(role = Role.Button, onClick = onClick)
                    .padding(start = AtelierDesign.gutter, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AtelierLibraryArtwork(entry, Modifier.size(AtelierDesign.rowArtwork).shadow(6.dp, if (entry.circle) CircleShape else AtelierDesign.sleeveShape))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        entry.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (active) AtelierDesign.accentBright else AtelierDesign.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    AtelierLibraryMeta(entry, copy)
                }
                AtelierLibraryMoreTarget(menu)
            }
        }
        Box(Modifier.padding(start = AtelierDesign.gutter).fillMaxWidth().height(1.dp).background(AtelierDesign.hairline))
    }
}

@Composable
private fun AtelierLibraryMeta(entry: AtelierLibraryEntry, copy: AtelierCopy) {
    if (entry.subtitle.isBlank() && !entry.explicit) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (entry.explicit) {
            Box(
                Modifier.size(14.dp).border(1.dp, AtelierDesign.hairlineStrong, AtelierDesign.sleeveShape)
                    .clearAndSetSemantics { contentDescription = copy.explicit },
                contentAlignment = Alignment.Center
            ) {
                Text("E", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp), color = AtelierDesign.textSecondary, maxLines = 1)
            }
            Spacer(Modifier.width(6.dp))
        }
        Text(entry.subtitle, style = MaterialTheme.typography.bodySmall, color = AtelierDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AtelierLibraryEmpty(
    tab: AtelierLibraryTab,
    strings: LevyraStrings,
    query: String,
    hiddenOnly: Boolean,
    filtered: Boolean,
    onCreate: () -> Unit
) {
    val searching = query.isNotBlank()
    val title = when {
        searching -> strings.emptySearchPrompt
        tab == AtelierLibraryTab.Playlists && hiddenOnly -> strings.hiddenPlaylistsEmpty
        tab == AtelierLibraryTab.Playlists && filtered -> strings.filterByTag
        tab == AtelierLibraryTab.Playlists -> strings.createFirstPlaylist
        tab == AtelierLibraryTab.Albums -> strings.albumUnavailable
        tab == AtelierLibraryTab.Artists -> strings.artistProfileUnavailable
        tab == AtelierLibraryTab.Songs -> strings.savedTracks
        tab == AtelierLibraryTab.Favorites -> strings.favorites
        else -> strings.noOfflineDownloads
    }
    val detail = when {
        searching -> null
        tab == AtelierLibraryTab.Playlists && !hiddenOnly && !filtered -> strings.createFirstPlaylistSubtitle
        tab == AtelierLibraryTab.Albums -> strings.savedTracks
        tab == AtelierLibraryTab.Artists -> strings.followedArtistsSubtitle
        tab == AtelierLibraryTab.Songs || tab == AtelierLibraryTab.Favorites -> strings.tapHeartToAdd
        else -> null
    }
    val canCreate = !searching && tab == AtelierLibraryTab.Playlists && !hiddenOnly && !filtered
    Column(
        Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AtelierLibraryEmptySleeve(Modifier.padding(bottom = 14.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Light), color = AtelierDesign.text, textAlign = TextAlign.Center)
        if (detail != null) {
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = AtelierDesign.textSecondary, textAlign = TextAlign.Center)
        }
        if (canCreate) {
            Text(strings.newPlaylist.uppercase(), style = MaterialTheme.typography.labelMedium, color = AtelierDesign.onAccent, modifier = Modifier.padding(top = 16.dp).heightIn(min = 48.dp).clip(CircleShape).background(AtelierDesign.accent).clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onCreate).padding(horizontal = 18.dp, vertical = 14.dp))
        }
    }
}

@Composable
private fun AtelierLibraryEmptySleeve(modifier: Modifier) {
    Canvas(modifier.size(width = 176.dp, height = 132.dp)) {
        val stroke = 1.dp.toPx()
        val side = size.height
        val sleeveLeft = size.width * 0.06f
        val discRadius = side * 0.46f
        val discCenter = Offset(sleeveLeft + side * 0.78f, size.height / 2f)
        drawCircle(AtelierDesign.cabinet, discRadius, discCenter)
        drawCircle(AtelierDesign.hairlineStrong, discRadius, discCenter, style = Stroke(stroke))
        var ring = 0.42f
        while (ring < 0.95f) {
            drawCircle(AtelierDesign.hairline, discRadius * ring, discCenter, style = Stroke(stroke))
            ring += 0.11f
        }
        drawCircle(AtelierDesign.accent.copy(alpha = 0.7f), discRadius * 0.24f, discCenter)
        drawCircle(AtelierDesign.room, discRadius * 0.035f, discCenter)
        val corner = CornerRadius(3.dp.toPx())
        val origin = Offset(sleeveLeft, 0f)
        drawRoundRect(AtelierDesign.panel, origin, Size(side, side), corner)
        drawRoundRect(AtelierDesign.hairlineStrong, origin, Size(side, side), corner, style = Stroke(stroke))
        val inset = side * 0.14f
        drawRoundRect(
            AtelierDesign.hairline,
            Offset(sleeveLeft + inset, inset),
            Size(side - inset * 2f, side - inset * 2f),
            CornerRadius(2.dp.toPx()),
            style = Stroke(stroke)
        )
        drawLine(AtelierDesign.accent.copy(alpha = 0.6f), Offset(sleeveLeft + inset, side - inset * 0.55f), Offset(sleeveLeft + side * 0.5f, side - inset * 0.55f), stroke)
    }
}

@Composable
private fun AtelierLibraryCreateDialog(strings: LevyraStrings, onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.newPlaylist) },
        text = { OutlinedTextField(name, { name = it }, singleLine = true, label = { Text(strings.playlistName) }) },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreate(name.trim())
                    onDismiss()
                },
                enabled = name.isNotBlank()
            ) { Text(strings.create) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
private fun AtelierLibraryDeleteDialog(strings: LevyraStrings, title: String, body: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
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

@Composable
private fun AtelierLibraryMoreTarget(items: () -> List<AtelierLibraryMenuItem>) {
    var open by remember { mutableStateOf(false) }
    Box {
        AtelierLibraryIconTarget(Icons.Rounded.MoreVert, LocalLevyraStrings.current.more) { open = true }
        DropdownMenu(open, { open = false }) {
            items().forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.label) },
                    onClick = {
                        open = false
                        item.action()
                    }
                )
            }
        }
    }
}

@Composable
private fun AtelierLibraryIconTarget(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, tint = AtelierDesign.textSecondary, modifier = Modifier.size(22.dp))
    }
}
