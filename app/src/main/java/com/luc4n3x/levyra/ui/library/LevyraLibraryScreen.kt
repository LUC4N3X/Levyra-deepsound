package com.luc4n3x.levyra.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.DownloadedTrack
import com.luc4n3x.levyra.domain.OfflineDownloadTask
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraInk
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraPanelSoft
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraViolet
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.LevyraViewModel
import com.luc4n3x.levyra.viewmodel.LibraryViewModel
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun LevyraLibraryScreen(
    viewModel: LibraryViewModel,
    state: LevyraUiState,
    onOpenDownloads: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val isItalian = strings.code == "it"
    val catalog = remember(
        state.favorites,
        state.playlists,
        state.downloads,
        state.recentListens,
        state.followedArtists
    ) {
        buildLibraryCatalog(
            favorites = state.favorites,
            playlists = state.playlists,
            downloads = state.downloads,
            recentListens = state.recentListens,
            followedArtists = state.followedArtists
        )
    }
    var categoryName by rememberSaveable { mutableStateOf(LibraryCategory.Overview.name) }
    val category = LibraryCategory.entries.firstOrNull { it.name == categoryName } ?: LibraryCategory.Overview
    var layoutName by rememberSaveable { mutableStateOf(LibraryLayout.List.name) }
    val layout = LibraryLayout.entries.firstOrNull { it.name == layoutName } ?: LibraryLayout.List
    var sortName by rememberSaveable { mutableStateOf(LibrarySort.Recent.name) }
    val sort = LibrarySort.entries.firstOrNull { it.name == sortName } ?: LibrarySort.Recent
    var query by rememberSaveable { mutableStateOf("") }
    var selectedKeys by remember { mutableStateOf(emptySet<String>()) }
    var sortExpanded by remember { mutableStateOf(false) }
    var addToPlaylistTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showCreatePlaylist by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scrollPositions = remember { mutableStateMapOf<String, Pair<Int, Int>>() }

    fun switchCategory(next: LibraryCategory) {
        if (next == category) return
        scrollPositions[category.name] = listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        categoryName = next.name
        selectedKeys = emptySet()
    }

    LaunchedEffect(categoryName) {
        val position = scrollPositions[categoryName] ?: (0 to 0)
        runCatching { listState.scrollToItem(position.first, position.second) }
    }

    val visiblePlaylists = remember(state.playlists, query, sort) {
        filterLibraryPlaylists(state.playlists, query, sort)
    }
    val visibleAlbums = remember(catalog.albums, query, sort) {
        filterLibraryAlbums(catalog.albums, query, sort)
    }
    val visibleArtists = remember(catalog.artists, query, sort) {
        filterLibraryArtists(catalog.artists, query, sort)
    }
    val visibleTracks = remember(catalog.tracks, query, sort) {
        filterLibraryTracks(catalog.tracks, query, sort)
    }
    val visibleOffline = remember(catalog.offlineItems, query, sort) {
        filterLibraryOfflineItems(catalog.offlineItems, query, sort)
    }

    val selectedTracks = remember(category, selectedKeys, catalog, state.playlists) {
        when (category) {
            LibraryCategory.Playlists -> state.playlists
                .filter { "playlist:${it.id}" in selectedKeys }
                .flatMap { it.tracks }
            LibraryCategory.Albums -> catalog.albums
                .filter { "album:${it.key}" in selectedKeys }
                .flatMap { it.tracks }
            LibraryCategory.Artists -> catalog.artists
                .filter { "artist:${it.key}" in selectedKeys }
                .flatMap { it.tracks }
            LibraryCategory.Offline -> catalog.offlineItems
                .filter { it.key in selectedKeys }
                .map { it.track }
            LibraryCategory.Overview, LibraryCategory.Songs -> catalog.tracks.filter { libraryTrackKey(it) in selectedKeys }
        }.distinctBy(::libraryTrackKey)
    }
    val selectedPlaylists = remember(selectedKeys, state.playlists) {
        state.playlists.filter { "playlist:${it.id}" in selectedKeys }
    }
    val selectedDownloads = remember(category, selectedKeys, selectedTracks, state.downloads, catalog.offlineItems) {
        if (category == LibraryCategory.Offline) {
            catalog.offlineItems.filter { it.key in selectedKeys }.map { it.download }
        } else {
            selectedTracks.mapNotNull { libraryDownloadForTrack(it, state.downloads) }.distinctBy { it.id }
        }
    }
    val selectionActive = selectedKeys.isNotEmpty()

    BackHandler(enabled = selectionActive) { selectedKeys = emptySet() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LevyraInk)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = if (state.currentTrack != null || selectionActive) 230.dp else 116.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "library-title") {
                LibraryHero(
                    title = if (isItalian) "La tua libreria" else "Your library",
                    subtitle = if (isItalian) {
                        "${catalog.tracks.size} brani · ${state.playlists.size} playlist · ${state.downloads.size} offline"
                    } else {
                        "${catalog.tracks.size} tracks · ${state.playlists.size} playlists · ${state.downloads.size} offline"
                    }
                )
            }

            item(key = "library-search") {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    placeholder = { Text(if (isItalian) "Cerca nella tua musica" else "Search your music") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = null)
                            }
                        }
                    }
                )
            }

            item(key = "library-categories") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LibraryCategory.entries.forEach { item ->
                        FilterChip(
                            selected = item == category,
                            onClick = { switchCategory(item) },
                            label = { Text(item.libraryLabel(isItalian)) },
                            leadingIcon = if (item == category) {
                                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else {
                                null
                            }
                        )
                    }
                }
            }

            item(key = "library-toolbar") {
                LibraryToolbar(
                    category = category,
                    sort = sort,
                    layout = layout,
                    sortExpanded = sortExpanded,
                    onSortExpanded = { sortExpanded = it },
                    onSort = { sortName = it.name },
                    onLayout = {
                        layoutName = if (layout == LibraryLayout.List) LibraryLayout.Grid.name else LibraryLayout.List.name
                    },
                    onSelectAll = {
                        selectedKeys = when (category) {
                            LibraryCategory.Playlists -> visiblePlaylists.mapTo(linkedSetOf()) { "playlist:${it.id}" }
                            LibraryCategory.Albums -> visibleAlbums.mapTo(linkedSetOf()) { "album:${it.key}" }
                            LibraryCategory.Artists -> visibleArtists.mapTo(linkedSetOf()) { "artist:${it.key}" }
                            LibraryCategory.Offline -> visibleOffline.mapTo(linkedSetOf()) { it.key }
                            LibraryCategory.Overview, LibraryCategory.Songs -> visibleTracks.mapTo(linkedSetOf(), ::libraryTrackKey)
                        }
                    },
                    isItalian = isItalian
                )
            }

            when (category) {
                LibraryCategory.Overview -> {
                    item(key = "overview-smart-title") {
                        LibrarySectionTitle(
                            title = if (isItalian) "Collezioni intelligenti" else "Smart collections",
                            detail = if (isItalian) "Accesso rapido alla musica che conta" else "Quick access to the music that matters"
                        )
                    }
                    item(key = "overview-smart-grid") {
                        SmartCollectionGrid(
                            favorites = state.favorites,
                            downloads = catalog.offlineTracks,
                            recent = catalog.recent,
                            mostPlayed = catalog.mostPlayed,
                            onPlay = { tracks -> tracks.firstOrNull()?.let { viewModel.playFrom(tracks, it) } },
                            onOpenOffline = { switchCategory(LibraryCategory.Offline) },
                            isItalian = isItalian
                        )
                    }
                    if (visiblePlaylists.isNotEmpty()) {
                        item(key = "overview-playlists-title") {
                            LibrarySectionTitle(
                                title = if (isItalian) "Playlist" else "Playlists",
                                detail = if (isItalian) "Le tue raccolte personali" else "Your personal collections",
                                action = if (isItalian) "Vedi tutte" else "See all",
                                onAction = { switchCategory(LibraryCategory.Playlists) }
                            )
                        }
                        items(visiblePlaylists.take(3), key = { "overview-playlist-${it.id}" }) { playlist ->
                            LibraryPlaylistRow(
                                playlist = playlist,
                                selected = false,
                                selectionActive = false,
                                onClick = { viewModel.openPlaylist(playlist.id) },
                                onLongClick = {
                                    switchCategory(LibraryCategory.Playlists)
                                    selectedKeys = setOf("playlist:${playlist.id}")
                                },
                                onPlay = { viewModel.playPlaylist(playlist.id) },
                                isItalian = isItalian
                            )
                        }
                    }
                    if (catalog.albums.isNotEmpty()) {
                        item(key = "overview-albums-title") {
                            LibrarySectionTitle(
                                title = if (isItalian) "Album nella tua musica" else "Albums in your music",
                                detail = if (isItalian) "Raggruppati dai tuoi brani salvati" else "Grouped from your saved tracks",
                                action = if (isItalian) "Vedi tutti" else "See all",
                                onAction = { switchCategory(LibraryCategory.Albums) }
                            )
                        }
                        item(key = "overview-albums-grid") {
                            LibraryAlbumGridRows(
                                albums = catalog.albums.take(4),
                                selectedKeys = emptySet(),
                                selectionActive = false,
                                onOpen = { album -> viewModel.openAlbum(album.toAlbumHit()) },
                                onSelect = {}
                            )
                        }
                    }
                    if (catalog.recent.isNotEmpty()) {
                        item(key = "overview-recent-title") {
                            LibrarySectionTitle(
                                title = if (isItalian) "Ascoltati di recente" else "Recently played",
                                detail = if (isItalian) "Riprendi da dove eri rimasto" else "Continue where you left off",
                                action = if (isItalian) "Tutti i brani" else "All tracks",
                                onAction = { switchCategory(LibraryCategory.Songs) }
                            )
                        }
                        items(catalog.recent.take(6), key = { "overview-track-${libraryTrackKey(it)}" }) { track ->
                            LibraryTrackRow(
                                track = track,
                                selected = false,
                                selectionActive = false,
                                isCurrent = track.id == state.currentTrack?.id,
                                isPlaying = state.isPlaying && track.id == state.currentTrack?.id,
                                isFavorite = track.id in state.favoriteIds,
                                isDownloaded = libraryDownloadForTrack(track, state.downloads) != null,
                                downloadProgress = downloadProgressFor(track, state),
                                onClick = { viewModel.playFrom(catalog.recent, track) },
                                onLongClick = {
                                    switchCategory(LibraryCategory.Songs)
                                    selectedKeys = setOf(libraryTrackKey(track))
                                },
                                onFavorite = { viewModel.toggleFavorite(track) },
                                onDownload = { viewModel.exportTrack(track) },
                                isItalian = isItalian
                            )
                        }
                    }
                }

                LibraryCategory.Playlists -> {
                    item(key = "playlist-heading") {
                        LibrarySectionTitle(
                            title = if (isItalian) "Le tue playlist" else "Your playlists",
                            detail = if (isItalian) "${visiblePlaylists.size} raccolte" else "${visiblePlaylists.size} collections",
                            action = if (isItalian) "Nuova" else "New",
                            onAction = { showCreatePlaylist = true }
                        )
                    }
                    if (visiblePlaylists.isEmpty()) {
                        item { LibraryEmpty(Icons.Rounded.QueueMusic, if (isItalian) "Nessuna playlist trovata" else "No playlists found") }
                    } else if (layout == LibraryLayout.List) {
                        items(visiblePlaylists, key = { "playlist-${it.id}" }) { playlist ->
                            val key = "playlist:${playlist.id}"
                            LibraryPlaylistRow(
                                playlist = playlist,
                                selected = key in selectedKeys,
                                selectionActive = selectionActive,
                                onClick = {
                                    if (selectionActive) selectedKeys = selectedKeys.toggle(key)
                                    else viewModel.openPlaylist(playlist.id)
                                },
                                onLongClick = { selectedKeys = selectedKeys.toggle(key) },
                                onPlay = { viewModel.playPlaylist(playlist.id) },
                                isItalian = isItalian
                            )
                        }
                    } else {
                        items(
                            items = visiblePlaylists.chunked(2),
                            key = { row -> "playlist-grid-row-${row.joinToString("-") { it.id }}" }
                        ) { rowItems ->
                            LibraryPlaylistGridRow(
                                playlists = rowItems,
                                selectedKeys = selectedKeys,
                                selectionActive = selectionActive,
                                onOpen = { playlist ->
                                    val key = "playlist:${playlist.id}"
                                    if (selectionActive) selectedKeys = selectedKeys.toggle(key)
                                    else viewModel.openPlaylist(playlist.id)
                                },
                                onSelect = { playlist -> selectedKeys = selectedKeys.toggle("playlist:${playlist.id}") },
                                isItalian = isItalian
                            )
                        }
                    }
                }

                LibraryCategory.Albums -> {
                    item(key = "album-heading") {
                        LibrarySectionTitle(
                            title = if (isItalian) "Album" else "Albums",
                            detail = if (isItalian) "${visibleAlbums.size} album dai tuoi salvataggi" else "${visibleAlbums.size} albums from your saves"
                        )
                    }
                    if (visibleAlbums.isEmpty()) {
                        item { LibraryEmpty(Icons.Rounded.Album, if (isItalian) "Nessun album trovato" else "No albums found") }
                    } else if (layout == LibraryLayout.Grid) {
                        items(
                            items = visibleAlbums.chunked(2),
                            key = { row -> "album-grid-row-${row.joinToString("-") { it.key }}" }
                        ) { rowItems ->
                            LibraryAlbumGridRow(
                                albums = rowItems,
                                selectedKeys = selectedKeys,
                                selectionActive = selectionActive,
                                onOpen = { album ->
                                    val key = "album:${album.key}"
                                    if (selectionActive) selectedKeys = selectedKeys.toggle(key)
                                    else viewModel.openAlbum(album.toAlbumHit())
                                },
                                onSelect = { album -> selectedKeys = selectedKeys.toggle("album:${album.key}") }
                            )
                        }
                    } else {
                        items(visibleAlbums, key = { "album-${it.key}" }) { album ->
                            val key = "album:${album.key}"
                            LibraryAlbumRow(
                                album = album,
                                selected = key in selectedKeys,
                                selectionActive = selectionActive,
                                onClick = {
                                    if (selectionActive) selectedKeys = selectedKeys.toggle(key)
                                    else viewModel.openAlbum(album.toAlbumHit())
                                },
                                onLongClick = { selectedKeys = selectedKeys.toggle(key) },
                                onPlay = { album.tracks.firstOrNull()?.let { viewModel.playFrom(album.tracks, it) } },
                                isItalian = isItalian
                            )
                        }
                    }
                }

                LibraryCategory.Artists -> {
                    item(key = "artist-heading") {
                        LibrarySectionTitle(
                            title = if (isItalian) "Artisti" else "Artists",
                            detail = if (isItalian) "${visibleArtists.size} artisti nella tua libreria" else "${visibleArtists.size} artists in your library"
                        )
                    }
                    if (visibleArtists.isEmpty()) {
                        item { LibraryEmpty(Icons.Rounded.Person, if (isItalian) "Nessun artista trovato" else "No artists found") }
                    } else if (layout == LibraryLayout.Grid) {
                        items(
                            items = visibleArtists.chunked(2),
                            key = { row -> "artist-grid-row-${row.joinToString("-") { it.key }}" }
                        ) { rowItems ->
                            LibraryArtistGridRow(
                                artists = rowItems,
                                selectedKeys = selectedKeys,
                                selectionActive = selectionActive,
                                onOpen = { artist ->
                                    val key = "artist:${artist.key}"
                                    if (selectionActive) selectedKeys = selectedKeys.toggle(key)
                                    else viewModel.openArtistReference(artist.name, artist.browseId, artist.artworkUrl)
                                },
                                onSelect = { artist -> selectedKeys = selectedKeys.toggle("artist:${artist.key}") },
                                isItalian = isItalian
                            )
                        }
                    } else {
                        items(visibleArtists, key = { "artist-${it.key}" }) { artist ->
                            val key = "artist:${artist.key}"
                            LibraryArtistRow(
                                artist = artist,
                                selected = key in selectedKeys,
                                selectionActive = selectionActive,
                                onClick = {
                                    if (selectionActive) selectedKeys = selectedKeys.toggle(key)
                                    else viewModel.openArtistReference(artist.name, artist.browseId, artist.artworkUrl)
                                },
                                onLongClick = { selectedKeys = selectedKeys.toggle(key) },
                                onPlay = { artist.tracks.firstOrNull()?.let { viewModel.playFrom(artist.tracks, it) } },
                                isItalian = isItalian
                            )
                        }
                    }
                }

                LibraryCategory.Songs -> {
                    item(key = "songs-heading") {
                        LibrarySectionTitle(
                            title = if (isItalian) "Brani" else "Tracks",
                            detail = if (isItalian) "${visibleTracks.size} brani unificati, senza duplicati" else "${visibleTracks.size} unified tracks, without duplicates"
                        )
                    }
                    if (visibleTracks.isEmpty()) {
                        item { LibraryEmpty(Icons.Rounded.MusicNote, if (isItalian) "Nessun brano trovato" else "No tracks found") }
                    } else {
                        items(visibleTracks, key = { "song-${libraryTrackKey(it)}" }) { track ->
                            val key = libraryTrackKey(track)
                            LibraryTrackRow(
                                track = track,
                                selected = key in selectedKeys,
                                selectionActive = selectionActive,
                                isCurrent = track.id == state.currentTrack?.id,
                                isPlaying = state.isPlaying && track.id == state.currentTrack?.id,
                                isFavorite = track.id in state.favoriteIds,
                                isDownloaded = libraryDownloadForTrack(track, state.downloads) != null,
                                downloadProgress = downloadProgressFor(track, state),
                                onClick = {
                                    if (selectionActive) selectedKeys = selectedKeys.toggle(key)
                                    else viewModel.playFrom(visibleTracks, track)
                                },
                                onLongClick = { selectedKeys = selectedKeys.toggle(key) },
                                onFavorite = { viewModel.toggleFavorite(track) },
                                onDownload = { viewModel.exportTrack(track) },
                                isItalian = isItalian
                            )
                        }
                    }
                }

                LibraryCategory.Offline -> {
                    item(key = "offline-storage") {
                        LibraryStorageCard(
                            bytes = state.downloadStorageBytes,
                            count = state.downloads.size,
                            activeCount = state.downloadQueue.count { it.state in setOf("QUEUED", "RUNNING", "RETRYING", "PAUSED") },
                            onOpenFolder = onOpenDownloads,
                            isItalian = isItalian
                        )
                    }
                    if (state.downloadQueue.isNotEmpty()) {
                        item(key = "offline-queue-title") {
                            LibrarySectionTitle(
                                title = if (isItalian) "Attività download" else "Download activity",
                                detail = if (isItalian) "Pausa, riprendi o annulla senza uscire dalla Libreria" else "Pause, resume or cancel without leaving Library"
                            )
                        }
                        items(state.downloadQueue, key = { "task-${it.taskKey}" }) { task ->
                            LibraryDownloadTaskRow(
                                task = task,
                                onPause = { viewModel.pauseDownload(task.taskKey) },
                                onResume = { viewModel.resumeDownload(task.taskKey) },
                                onCancel = { viewModel.cancelDownload(task.taskKey) },
                                isItalian = isItalian
                            )
                        }
                    }
                    item(key = "offline-heading") {
                        LibrarySectionTitle(
                            title = if (isItalian) "Disponibili offline" else "Available offline",
                            detail = if (isItalian) "${visibleOffline.size} file locali" else "${visibleOffline.size} local files"
                        )
                    }
                    if (visibleOffline.isEmpty()) {
                        item { LibraryEmpty(Icons.Rounded.OfflinePin, if (isItalian) "Non hai ancora musica offline" else "You do not have offline music yet") }
                    } else {
                        items(visibleOffline, key = { "offline-${it.key}" }) { item ->
                            val track = item.track
                            val key = item.key
                            LibraryTrackRow(
                                track = track,
                                selected = key in selectedKeys,
                                selectionActive = selectionActive,
                                isCurrent = track.id == state.currentTrack?.id,
                                isPlaying = state.isPlaying && track.id == state.currentTrack?.id,
                                isFavorite = track.id in state.favoriteIds,
                                isDownloaded = true,
                                downloadProgress = null,
                                secondaryDetail = listOf(
                                    item.download.mimeType.substringAfter('/').uppercase(Locale.ROOT),
                                    formatBytes(item.download.sizeBytes)
                                ).filter(String::isNotBlank).joinToString(" · "),
                                onClick = {
                                    if (selectionActive) selectedKeys = selectedKeys.toggle(key)
                                    else viewModel.playDownloaded(item.download)
                                },
                                onLongClick = { selectedKeys = selectedKeys.toggle(key) },
                                onFavorite = { viewModel.toggleFavorite(track) },
                                onDownload = {},
                                isItalian = isItalian
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = selectionActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 12.dp, end = 12.dp, bottom = if (state.currentTrack != null) 82.dp else 12.dp)
        ) {
            LibrarySelectionBar(
                count = selectedKeys.size,
                canOperateTracks = selectedTracks.isNotEmpty(),
                canDelete = when (category) {
                    LibraryCategory.Playlists -> selectedPlaylists.isNotEmpty()
                    LibraryCategory.Offline -> selectedDownloads.isNotEmpty()
                    else -> selectedTracks.any { it.id in state.favoriteIds }
                },
                onClear = { selectedKeys = emptySet() },
                onPlay = {
                    selectedTracks.firstOrNull()?.let { viewModel.playFrom(selectedTracks, it) }
                    selectedKeys = emptySet()
                },
                onQueue = {
                    viewModel.addTracksToQueue(selectedTracks)
                    selectedKeys = emptySet()
                },
                onDownload = {
                    viewModel.exportTracks(selectedTracks, if (isItalian) "Selezione offline" else "Offline selection")
                    selectedKeys = emptySet()
                },
                onAddToPlaylist = { addToPlaylistTracks = selectedTracks },
                onDelete = { confirmDelete = true },
                isItalian = isItalian
            )
        }

        if (category == LibraryCategory.Playlists && !selectionActive) {
            FloatingActionButton(
                onClick = { showCreatePlaylist = true },
                containerColor = LevyraCyan,
                contentColor = Color.Black,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 18.dp, bottom = if (state.currentTrack != null) 94.dp else 24.dp)
            ) {
                Icon(Icons.Rounded.PlaylistAdd, contentDescription = null)
            }
        }
    }

    if (showCreatePlaylist) {
        LibraryNameDialog(
            title = if (isItalian) "Nuova playlist" else "New playlist",
            initialValue = "",
            confirmLabel = if (isItalian) "Crea" else "Create",
            onDismiss = { showCreatePlaylist = false },
            onConfirm = { name ->
                viewModel.createPlaylist(name)
                showCreatePlaylist = false
            }
        )
    }

    if (addToPlaylistTracks.isNotEmpty()) {
        AddTracksToPlaylistDialog(
            tracks = addToPlaylistTracks,
            playlists = state.playlists,
            isItalian = isItalian,
            onDismiss = { addToPlaylistTracks = emptyList() },
            onAdd = { playlistId ->
                viewModel.addTracksToPlaylist(playlistId, addToPlaylistTracks)
                addToPlaylistTracks = emptyList()
                selectedKeys = emptySet()
            },
            onCreate = { name ->
                viewModel.createPlaylistWithTracks(name, addToPlaylistTracks)
                addToPlaylistTracks = emptyList()
                selectedKeys = emptySet()
            }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(if (isItalian) "Conferma operazione" else "Confirm operation") },
            text = {
                Text(
                    when (category) {
                        LibraryCategory.Playlists -> if (isItalian) "Eliminare ${selectedPlaylists.size} playlist?" else "Delete ${selectedPlaylists.size} playlists?"
                        LibraryCategory.Offline -> if (isItalian) "Eliminare ${selectedDownloads.size} file dal dispositivo?" else "Delete ${selectedDownloads.size} files from the device?"
                        else -> if (isItalian) "Rimuovere i brani selezionati dai preferiti?" else "Remove selected tracks from favorites?"
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    when (category) {
                        LibraryCategory.Playlists -> viewModel.deletePlaylists(selectedPlaylists.map { it.id })
                        LibraryCategory.Offline -> viewModel.deleteDownloads(selectedDownloads)
                        else -> viewModel.removeFavorites(selectedTracks)
                    }
                    selectedKeys = emptySet()
                    confirmDelete = false
                }) {
                    Text(if (isItalian) "Conferma" else "Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(if (isItalian) "Annulla" else "Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun LevyraPlaylistDetailScreen(
    viewModel: LevyraViewModel,
    state: LevyraUiState
) {
    val playlist = state.openPlaylist ?: return
    val strings = LocalLevyraStrings.current
    val isItalian = strings.code == "it"
    var query by rememberSaveable(playlist.id) { mutableStateOf("") }
    var selectedKeys by remember(playlist.id) { mutableStateOf(emptySet<String>()) }
    var reorderMode by rememberSaveable(playlist.id) { mutableStateOf(false) }
    var orderedTracks by remember(playlist.id) { mutableStateOf(playlist.tracks) }
    var renameDialog by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }
    var addTracksDialog by remember { mutableStateOf(false) }

    LaunchedEffect(playlist.tracks, reorderMode) {
        if (!reorderMode) orderedTracks = playlist.tracks
    }

    val visibleTracks = remember(orderedTracks, query) {
        filterLibraryTracks(orderedTracks, query, LibrarySort.Recent)
    }
    val selectedTracks = remember(orderedTracks, selectedKeys) {
        orderedTracks.filter { playlistEntryKey(it) in selectedKeys }
    }
    val selectionActive = selectedKeys.isNotEmpty()

    BackHandler {
        when {
            selectionActive -> selectedKeys = emptySet()
            reorderMode -> {
                reorderMode = false
                orderedTracks = playlist.tracks
            }
            else -> viewModel.closePlaylist()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LevyraInk)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = if (state.currentTrack != null || selectionActive) 220.dp else 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "playlist-detail-header") {
                PlaylistDetailHeader(
                    playlist = playlist,
                    durationMs = orderedTracks.sumOf { it.durationMs },
                    reorderMode = reorderMode,
                    onBack = {
                        if (reorderMode) {
                            reorderMode = false
                            orderedTracks = playlist.tracks
                        } else {
                            viewModel.closePlaylist()
                        }
                    },
                    onPlay = { viewModel.playPlaylist(playlist.id) },
                    onShuffle = {
                        val shuffled = orderedTracks.shuffled()
                        shuffled.firstOrNull()?.let { first -> viewModel.playFrom(shuffled, first) }
                    },
                    onDownload = { viewModel.exportTracks(orderedTracks, if (isItalian) "Playlist offline" else "Offline playlist") },
                    onRename = { renameDialog = true },
                    onReorder = {
                        reorderMode = !reorderMode
                        orderedTracks = playlist.tracks
                        selectedKeys = emptySet()
                        query = ""
                    },
                    onSaveOrder = {
                        viewModel.reorderPlaylist(playlist.id, orderedTracks)
                        reorderMode = false
                    },
                    isItalian = isItalian
                )
            }

            if (!reorderMode) {
                item(key = "playlist-detail-search") {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        placeholder = { Text(if (isItalian) "Cerca nella playlist" else "Search in playlist") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = if (query.isNotBlank()) {
                            { IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, contentDescription = null) } }
                        } else null
                    )
                }
            }

            if (orderedTracks.isEmpty()) {
                item { LibraryEmpty(Icons.Rounded.QueueMusic, if (isItalian) "Questa playlist è vuota" else "This playlist is empty") }
            } else if (reorderMode) {
                items(orderedTracks, key = { "reorder-${playlistEntryKey(it)}" }) { track ->
                    val entryKey = playlistEntryKey(track)
                    val index = orderedTracks.indexOfFirst { playlistEntryKey(it) == entryKey }
                    PlaylistReorderRow(
                        track = track,
                        index = index,
                        count = orderedTracks.size,
                        onMoveUp = {
                            if (index > 0) orderedTracks = orderedTracks.move(index, index - 1)
                        },
                        onMoveDown = {
                            if (index in 0 until orderedTracks.lastIndex) orderedTracks = orderedTracks.move(index, index + 1)
                        }
                    )
                }
            } else {
                items(visibleTracks, key = { "playlist-track-${playlistEntryKey(it)}" }) { track ->
                    val key = playlistEntryKey(track)
                    LibraryTrackRow(
                        track = track,
                        selected = key in selectedKeys,
                        selectionActive = selectionActive,
                        isCurrent = track.id == state.currentTrack?.id,
                        isPlaying = state.isPlaying && track.id == state.currentTrack?.id,
                        isFavorite = track.id in state.favoriteIds,
                        isDownloaded = libraryDownloadForTrack(track, state.downloads) != null,
                        downloadProgress = downloadProgressFor(track, state),
                        onClick = {
                            if (selectionActive) selectedKeys = selectedKeys.toggle(key)
                            else viewModel.playPlaylist(playlist.id, track.id)
                        },
                        onLongClick = { selectedKeys = selectedKeys.toggle(key) },
                        onFavorite = { viewModel.toggleFavorite(track) },
                        onDownload = { viewModel.exportTrack(track) },
                        isItalian = isItalian
                    )
                }
            }
        }

        if (selectionActive) {
            LibrarySelectionBar(
                count = selectedKeys.size,
                canOperateTracks = selectedTracks.isNotEmpty(),
                canDelete = selectedTracks.isNotEmpty(),
                onClear = { selectedKeys = emptySet() },
                onPlay = {
                    selectedTracks.firstOrNull()?.let { viewModel.playFrom(selectedTracks, it) }
                    selectedKeys = emptySet()
                },
                onQueue = {
                    viewModel.addTracksToQueue(selectedTracks)
                    selectedKeys = emptySet()
                },
                onDownload = {
                    viewModel.exportTracks(selectedTracks, if (isItalian) "Selezione offline" else "Offline selection")
                    selectedKeys = emptySet()
                },
                onAddToPlaylist = { addTracksDialog = true },
                onDelete = { confirmRemove = true },
                isItalian = isItalian,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 12.dp, end = 12.dp, bottom = if (state.currentTrack != null) 82.dp else 12.dp)
            )
        } else if (state.currentTrack != null) {
            LibraryNowPlayingDock(
                track = state.currentTrack,
                isPlaying = state.isPlaying,
                onToggle = viewModel::togglePlay,
                onOpen = viewModel::openPlayerScreen,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(14.dp)
            )
        }
    }

    if (renameDialog) {
        LibraryNameDialog(
            title = if (isItalian) "Rinomina playlist" else "Rename playlist",
            initialValue = playlist.name,
            confirmLabel = if (isItalian) "Salva" else "Save",
            onDismiss = { renameDialog = false },
            onConfirm = { name ->
                viewModel.renamePlaylist(playlist.id, name)
                renameDialog = false
            }
        )
    }


    if (addTracksDialog && selectedTracks.isNotEmpty()) {
        AddTracksToPlaylistDialog(
            tracks = selectedTracks,
            playlists = state.playlists,
            isItalian = isItalian,
            onDismiss = { addTracksDialog = false },
            onAdd = { playlistId ->
                viewModel.addTracksToPlaylist(playlistId, selectedTracks)
                selectedKeys = emptySet()
                addTracksDialog = false
            },
            onCreate = { name ->
                viewModel.createPlaylistWithTracks(name, selectedTracks)
                selectedKeys = emptySet()
                addTracksDialog = false
            }
        )
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text(if (isItalian) "Rimuovi dalla playlist" else "Remove from playlist") },
            text = { Text(if (isItalian) "Rimuovere ${selectedTracks.size} brani?" else "Remove ${selectedTracks.size} tracks?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeTracksFromPlaylist(playlist.id, selectedTracks)
                    selectedKeys = emptySet()
                    confirmRemove = false
                }) { Text(if (isItalian) "Rimuovi" else "Remove") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) { Text(if (isItalian) "Annulla" else "Cancel") }
            }
        )
    }
}

@Composable
private fun LibraryHero(title: String, subtitle: String) {
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(30.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            LevyraViolet.copy(alpha = 0.24f),
                            LevyraPanel.copy(alpha = 0.94f),
                            LevyraCyan.copy(alpha = 0.12f)
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(30.dp))
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, color = LevyraText, fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.8).sp)
                Text(subtitle, color = LevyraMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun LibraryToolbar(
    category: LibraryCategory,
    sort: LibrarySort,
    layout: LibraryLayout,
    sortExpanded: Boolean,
    onSortExpanded: (Boolean) -> Unit,
    onSort: (LibrarySort) -> Unit,
    onLayout: () -> Unit,
    onSelectAll: () -> Unit,
    isItalian: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box {
            TextButton(onClick = { onSortExpanded(true) }) {
                Icon(Icons.Rounded.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text(sort.libraryLabel(isItalian), fontWeight = FontWeight.Bold)
            }
            DropdownMenu(expanded = sortExpanded, onDismissRequest = { onSortExpanded(false) }) {
                LibrarySort.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.libraryLabel(isItalian)) },
                        leadingIcon = if (option == sort) {
                            { Icon(Icons.Rounded.Check, contentDescription = null) }
                        } else null,
                        onClick = {
                            onSort(option)
                            onSortExpanded(false)
                        }
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onSelectAll) {
                Icon(Icons.Rounded.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (isItalian) "Seleziona" else "Select", fontWeight = FontWeight.Bold)
            }
            if (category != LibraryCategory.Overview && category != LibraryCategory.Offline && category != LibraryCategory.Songs) {
                IconButton(onClick = onLayout) {
                    Icon(
                        if (layout == LibraryLayout.List) Icons.Rounded.GridView else Icons.Rounded.ViewList,
                        contentDescription = null,
                        tint = LevyraText
                    )
                }
            }
        }
    }
}

@Composable
private fun LibrarySectionTitle(
    title: String,
    detail: String,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = LevyraText, fontSize = 21.sp, fontWeight = FontWeight.Black)
            Text(detail, color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) { Text(action, color = LevyraCyan, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun SmartCollectionGrid(
    favorites: List<Track>,
    downloads: List<Track>,
    recent: List<Track>,
    mostPlayed: List<Track>,
    onPlay: (List<Track>) -> Unit,
    onOpenOffline: () -> Unit,
    isItalian: Boolean
) {
    val cards = listOf(
        SmartCollection(
            title = if (isItalian) "Brani preferiti" else "Favorite tracks",
            detail = "${favorites.size}",
            icon = Icons.Rounded.Favorite,
            accent = LevyraPink,
            tracks = favorites,
            onClick = { onPlay(favorites) }
        ),
        SmartCollection(
            title = if (isItalian) "Scaricati" else "Downloaded",
            detail = "${downloads.size}",
            icon = Icons.Rounded.DownloadDone,
            accent = LevyraCyan,
            tracks = downloads,
            enabledWhenEmpty = true,
            onClick = onOpenOffline
        ),
        SmartCollection(
            title = if (isItalian) "Ascoltati di recente" else "Recently played",
            detail = "${recent.size}",
            icon = Icons.Rounded.History,
            accent = LevyraViolet,
            tracks = recent,
            onClick = { onPlay(recent) }
        ),
        SmartCollection(
            title = if (isItalian) "Più ascoltati" else "Most played",
            detail = "${mostPlayed.size}",
            icon = Icons.Rounded.Replay,
            accent = Color(0xFFFFC857),
            tracks = mostPlayed,
            onClick = { onPlay(mostPlayed) }
        )
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        cards.chunked(2).forEach { rowCards ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowCards.forEach { card -> SmartCollectionCard(card, Modifier.weight(1f)) }
                if (rowCards.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private data class SmartCollection(
    val title: String,
    val detail: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accent: Color,
    val tracks: List<Track>,
    val enabledWhenEmpty: Boolean = false,
    val onClick: () -> Unit
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SmartCollectionCard(card: SmartCollection, modifier: Modifier = Modifier) {
    Surface(
        color = LevyraPanel.copy(alpha = 0.90f),
        shape = RoundedCornerShape(23.dp),
        border = BorderStroke(1.dp, card.accent.copy(alpha = 0.20f)),
        modifier = modifier
            .height(126.dp)
            .combinedClickable(enabled = card.tracks.isNotEmpty() || card.enabledWhenEmpty, onClick = card.onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(card.accent.copy(alpha = 0.20f), Color.Transparent)))
                .padding(15.dp)
        ) {
            Icon(card.icon, contentDescription = null, tint = card.accent, modifier = Modifier.size(27.dp))
            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                Text(card.title, color = LevyraText, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(card.detail, color = LevyraMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryTrackRow(
    track: Track,
    selected: Boolean,
    selectionActive: Boolean,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    isDownloaded: Boolean,
    downloadProgress: Int?,
    secondaryDetail: String? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavorite: () -> Unit,
    onDownload: () -> Unit,
    isItalian: Boolean
) {
    Surface(
        color = when {
            selected -> LevyraCyan.copy(alpha = 0.14f)
            isCurrent -> LevyraViolet.copy(alpha = 0.11f)
            else -> Color.Transparent
        },
        shape = RoundedCornerShape(19.dp),
        border = if (selected) BorderStroke(1.dp, LevyraCyan.copy(alpha = 0.55f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LibraryArtwork(
                url = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl },
                title = track.title,
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(15.dp),
                selected = selected
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    track.title,
                    color = if (isCurrent) LevyraCyan else LevyraText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    secondaryDetail?.takeIf(String::isNotBlank)
                        ?: listOf(track.artist, track.album).filter(String::isNotBlank).joinToString(" · "),
                    color = LevyraMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (downloadProgress != null) {
                    LinearProgressIndicator(
                        progress = { (downloadProgress.coerceIn(0, 100) / 100f) },
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = LevyraCyan,
                        trackColor = LevyraPanelSoft
                    )
                }
            }
            if (selectionActive) {
                Icon(
                    if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = if (selected) LevyraCyan else LevyraMuted.copy(alpha = 0.35f)
                )
            } else {
                if (isDownloaded) {
                    Icon(Icons.Rounded.OfflinePin, contentDescription = null, tint = LevyraCyan, modifier = Modifier.size(17.dp))
                }
                IconButton(onClick = onFavorite) {
                    Icon(
                        if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorite) LevyraPink else LevyraMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (!isDownloaded && downloadProgress == null) {
                    IconButton(onClick = onDownload) {
                        Icon(Icons.Rounded.Download, contentDescription = if (isItalian) "Scarica" else "Download", tint = LevyraMuted, modifier = Modifier.size(21.dp))
                    }
                }
                if (isPlaying) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = LevyraCyan, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryPlaylistRow(
    playlist: Playlist,
    selected: Boolean,
    selectionActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPlay: () -> Unit,
    isItalian: Boolean
) {
    Surface(
        color = if (selected) LevyraCyan.copy(alpha = 0.14f) else Color.Transparent,
        shape = RoundedCornerShape(20.dp),
        border = if (selected) BorderStroke(1.dp, LevyraCyan.copy(alpha = 0.55f)) else null,
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            LibraryArtwork(playlist.coverUrl, playlist.name, Modifier.size(66.dp), RoundedCornerShape(18.dp), selected)
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(playlist.name, color = LevyraText, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (isItalian) "${playlist.size} brani · ${formatDuration(playlist.tracks.sumOf { it.durationMs })}" else "${playlist.size} tracks · ${formatDuration(playlist.tracks.sumOf { it.durationMs })}",
                    color = LevyraMuted,
                    fontSize = 11.sp
                )
            }
            if (selectionActive) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = if (selected) LevyraCyan else LevyraMuted.copy(alpha = 0.35f))
            } else {
                IconButton(onClick = onPlay, enabled = playlist.tracks.isNotEmpty()) {
                    Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, contentDescription = null, tint = if (playlist.tracks.isNotEmpty()) LevyraCyan else LevyraMuted.copy(alpha = 0.35f))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryAlbumRow(
    album: LibraryAlbum,
    selected: Boolean,
    selectionActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPlay: () -> Unit,
    isItalian: Boolean
) {
    Surface(
        color = if (selected) LevyraCyan.copy(alpha = 0.14f) else Color.Transparent,
        shape = RoundedCornerShape(20.dp),
        border = if (selected) BorderStroke(1.dp, LevyraCyan.copy(alpha = 0.55f)) else null,
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            LibraryArtwork(album.artworkUrl, album.title, Modifier.size(66.dp), RoundedCornerShape(18.dp), selected)
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(album.title, color = LevyraText, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(album.artist, album.year, if (isItalian) "${album.tracks.size} brani" else "${album.tracks.size} tracks")
                        .filter(String::isNotBlank)
                        .joinToString(" · "),
                    color = LevyraMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selectionActive) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = if (selected) LevyraCyan else LevyraMuted.copy(alpha = 0.35f))
            } else {
                IconButton(onClick = onPlay) { Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = LevyraCyan) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryArtistRow(
    artist: LibraryArtist,
    selected: Boolean,
    selectionActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPlay: () -> Unit,
    isItalian: Boolean
) {
    Surface(
        color = if (selected) LevyraCyan.copy(alpha = 0.14f) else Color.Transparent,
        shape = RoundedCornerShape(20.dp),
        border = if (selected) BorderStroke(1.dp, LevyraCyan.copy(alpha = 0.55f)) else null,
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            LibraryArtwork(artist.artworkUrl, artist.name, Modifier.size(64.dp), CircleShape, selected)
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(artist.name, color = LevyraText, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (isItalian) "${artist.tracks.size} brani nella tua libreria" else "${artist.tracks.size} tracks in your library",
                    color = LevyraMuted,
                    fontSize = 11.sp
                )
            }
            if (selectionActive) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = if (selected) LevyraCyan else LevyraMuted.copy(alpha = 0.35f))
            } else if (artist.tracks.isNotEmpty()) {
                IconButton(onClick = onPlay) { Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = LevyraCyan) }
            }
        }
    }
}

@Composable
private fun LibraryPlaylistGridRow(
    playlists: List<Playlist>,
    selectedKeys: Set<String>,
    selectionActive: Boolean,
    onOpen: (Playlist) -> Unit,
    onSelect: (Playlist) -> Unit,
    isItalian: Boolean
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        playlists.forEach { playlist ->
            LibraryPlaylistGridCard(
                playlist = playlist,
                selected = "playlist:${playlist.id}" in selectedKeys,
                selectionActive = selectionActive,
                onClick = { onOpen(playlist) },
                onLongClick = { onSelect(playlist) },
                modifier = Modifier.weight(1f),
                isItalian = isItalian
            )
        }
        if (playlists.size == 1) Spacer(Modifier.weight(1f))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryPlaylistGridCard(
    playlist: Playlist,
    selected: Boolean,
    selectionActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier,
    isItalian: Boolean
) {
    Column(modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        Box {
            LibraryArtwork(playlist.coverUrl, playlist.name, Modifier.fillMaxWidth().height(164.dp), RoundedCornerShape(22.dp), selected)
            if (selectionActive) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = if (selected) LevyraCyan else Color.White.copy(alpha = 0.55f), modifier = Modifier.align(Alignment.TopEnd).padding(10.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(playlist.name, color = LevyraText, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(if (isItalian) "${playlist.size} brani" else "${playlist.size} tracks", color = LevyraMuted, fontSize = 11.sp)
    }
}

@Composable
private fun LibraryAlbumGridRows(
    albums: List<LibraryAlbum>,
    selectedKeys: Set<String>,
    selectionActive: Boolean,
    onOpen: (LibraryAlbum) -> Unit,
    onSelect: (LibraryAlbum) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        albums.chunked(2).forEach { rowItems ->
            LibraryAlbumGridRow(
                albums = rowItems,
                selectedKeys = selectedKeys,
                selectionActive = selectionActive,
                onOpen = onOpen,
                onSelect = onSelect
            )
        }
    }
}

@Composable
private fun LibraryAlbumGridRow(
    albums: List<LibraryAlbum>,
    selectedKeys: Set<String>,
    selectionActive: Boolean,
    onOpen: (LibraryAlbum) -> Unit,
    onSelect: (LibraryAlbum) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        albums.forEach { album ->
            LibraryAlbumGridCard(
                album = album,
                selected = "album:${album.key}" in selectedKeys,
                selectionActive = selectionActive,
                onClick = { onOpen(album) },
                onLongClick = { onSelect(album) },
                modifier = Modifier.weight(1f)
            )
        }
        if (albums.size == 1) Spacer(Modifier.weight(1f))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryAlbumGridCard(
    album: LibraryAlbum,
    selected: Boolean,
    selectionActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier
) {
    Column(modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        Box {
            LibraryArtwork(album.artworkUrl, album.title, Modifier.fillMaxWidth().height(164.dp), RoundedCornerShape(22.dp), selected)
            if (selectionActive) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = if (selected) LevyraCyan else Color.White.copy(alpha = 0.55f), modifier = Modifier.align(Alignment.TopEnd).padding(10.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(album.title, color = LevyraText, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(listOf(album.artist, album.year).filter(String::isNotBlank).joinToString(" · "), color = LevyraMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun LibraryArtistGridRow(
    artists: List<LibraryArtist>,
    selectedKeys: Set<String>,
    selectionActive: Boolean,
    onOpen: (LibraryArtist) -> Unit,
    onSelect: (LibraryArtist) -> Unit,
    isItalian: Boolean
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        artists.forEach { artist ->
            LibraryArtistGridCard(
                artist = artist,
                selected = "artist:${artist.key}" in selectedKeys,
                selectionActive = selectionActive,
                onClick = { onOpen(artist) },
                onLongClick = { onSelect(artist) },
                modifier = Modifier.weight(1f),
                isItalian = isItalian
            )
        }
        if (artists.size == 1) Spacer(Modifier.weight(1f))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryArtistGridCard(
    artist: LibraryArtist,
    selected: Boolean,
    selectionActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier,
    isItalian: Boolean
) {
    Column(modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            LibraryArtwork(artist.artworkUrl, artist.name, Modifier.size(152.dp), CircleShape, selected)
            if (selectionActive) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = if (selected) LevyraCyan else Color.White.copy(alpha = 0.55f), modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(artist.name, color = LevyraText, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(if (isItalian) "${artist.tracks.size} brani" else "${artist.tracks.size} tracks", color = LevyraMuted, fontSize = 11.sp)
    }
}

@Composable
private fun LibraryArtwork(
    url: String,
    title: String,
    modifier: Modifier,
    shape: androidx.compose.ui.graphics.Shape,
    selected: Boolean
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(LevyraPanelSoft)
            .border(if (selected) 2.dp else 1.dp, if (selected) LevyraCyan else Color.White.copy(alpha = 0.08f), shape),
        contentAlignment = Alignment.Center
    ) {
        if (url.isNotBlank()) {
            AsyncImage(
                model = url,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(Icons.Rounded.LibraryMusic, contentDescription = null, tint = LevyraMuted, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun LibraryStorageCard(
    bytes: Long,
    count: Int,
    activeCount: Int,
    onOpenFolder: () -> Unit,
    isItalian: Boolean
) {
    Surface(
        color = LevyraPanel.copy(alpha = 0.92f),
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, LevyraCyan.copy(alpha = 0.18f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = LevyraCyan.copy(alpha = 0.15f), shape = CircleShape) {
                    Icon(Icons.Rounded.Storage, contentDescription = null, tint = LevyraCyan, modifier = Modifier.padding(10.dp).size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (isItalian) "Spazio offline" else "Offline storage", color = LevyraText, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (isItalian) "${formatBytes(bytes)} · $count file${if (activeCount > 0) " · $activeCount attivi" else ""}" else "${formatBytes(bytes)} · $count files${if (activeCount > 0) " · $activeCount active" else ""}",
                        color = LevyraMuted,
                        fontSize = 12.sp
                    )
                }
                TextButton(onClick = onOpenFolder) {
                    Text(if (isItalian) "Apri cartella" else "Open folder", color = LevyraCyan, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                if (isItalian) "Seleziona più brani per eliminarli insieme senza rimuoverli da playlist e preferiti." else "Select multiple tracks to delete them together without removing them from playlists or favorites.",
                color = LevyraMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun LibraryDownloadTaskRow(
    task: OfflineDownloadTask,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    isItalian: Boolean
) {
    val paused = task.state == "PAUSED"
    val failed = task.state == "FAILED"
    Surface(color = LevyraPanel.copy(alpha = 0.84f), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(task.title, color = LevyraText, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(task.artist.ifBlank { task.state }, color = LevyraMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = if (paused || failed) onResume else onPause) {
                    Icon(
                        if (paused || failed) Icons.Rounded.Refresh else Icons.Rounded.Pause,
                        contentDescription = if (paused || failed) {
                            if (isItalian) "Riprova" else "Retry"
                        } else {
                            if (isItalian) "Pausa" else "Pause"
                        },
                        tint = LevyraCyan
                    )
                }
                IconButton(onClick = onCancel) {
                    Icon(
                        Icons.Rounded.Cancel,
                        contentDescription = if (isItalian) "Annulla download" else "Cancel download",
                        tint = LevyraMuted
                    )
                }
            }
            LinearProgressIndicator(
                progress = { task.progress.coerceIn(0, 100) / 100f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = if (failed) MaterialTheme.colorScheme.error else LevyraCyan,
                trackColor = LevyraPanelSoft
            )
            Text(
                when {
                    failed -> task.error.ifBlank { if (isItalian) "Download non riuscito" else "Download failed" }
                    paused -> if (isItalian) "In pausa · ${task.progress}%" else "Paused · ${task.progress}%"
                    else -> "${task.progress}% · ${task.state.lowercase(Locale.ROOT)}"
                },
                color = if (failed) MaterialTheme.colorScheme.error else LevyraMuted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun LibrarySelectionBar(
    count: Int,
    canOperateTracks: Boolean,
    canDelete: Boolean,
    onClear: () -> Unit,
    onPlay: () -> Unit,
    onQueue: () -> Unit,
    onDownload: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDelete: () -> Unit,
    isItalian: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = LevyraPanel.copy(alpha = 0.98f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        shadowElevation = 14.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClear) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = if (isItalian) "Annulla selezione" else "Clear selection",
                        tint = LevyraText
                    )
                }
                Text(
                    if (isItalian) "$count selezionati" else "$count selected",
                    color = LevyraText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LibrarySelectionAction(Icons.Rounded.PlayArrow, if (isItalian) "Riproduci" else "Play", canOperateTracks, onPlay, LevyraCyan)
                LibrarySelectionAction(Icons.Rounded.QueueMusic, if (isItalian) "Coda" else "Queue", canOperateTracks, onQueue, LevyraText)
                LibrarySelectionAction(Icons.Rounded.PlaylistAdd, if (isItalian) "Playlist" else "Playlist", canOperateTracks, onAddToPlaylist, LevyraText)
                LibrarySelectionAction(Icons.Rounded.Download, if (isItalian) "Offline" else "Offline", canOperateTracks, onDownload, LevyraText)
                LibrarySelectionAction(Icons.Rounded.Delete, if (isItalian) "Elimina" else "Delete", canDelete, onDelete, LevyraPink)
            }
        }
    }
}

@Composable
private fun LibrarySelectionAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    tint: Color
) {
    TextButton(onClick = onClick, enabled = enabled, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(icon, contentDescription = label, tint = if (enabled) tint else LevyraMuted.copy(alpha = 0.35f), modifier = Modifier.size(21.dp))
            Text(label, color = if (enabled) LevyraMuted else LevyraMuted.copy(alpha = 0.35f), fontSize = 9.sp, maxLines = 1)
        }
    }
}

@Composable
private fun LibraryEmpty(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 42.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = LevyraMuted, modifier = Modifier.size(42.dp))
        Text(title, color = LevyraMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AddTracksToPlaylistDialog(
    tracks: List<Track>,
    playlists: List<Playlist>,
    isItalian: Boolean,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onCreate: (String) -> Unit
) {
    var creating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isItalian) "Aggiungi ${tracks.size} brani" else "Add ${tracks.size} tracks") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                if (creating) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text(if (isItalian) "Nome playlist" else "Playlist name") })
                } else {
                    TextButton(onClick = { creating = true }) {
                        Icon(Icons.Rounded.PlaylistAdd, contentDescription = null)
                        Spacer(Modifier.width(7.dp))
                        Text(if (isItalian) "Crea nuova playlist" else "Create new playlist")
                    }
                    playlists.take(12).forEach { playlist ->
                        Surface(
                            color = Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { onAdd(playlist.id) })
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.QueueMusic, contentDescription = null, tint = LevyraMuted)
                                Spacer(Modifier.width(10.dp))
                                Text(playlist.name, color = LevyraText, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (creating) {
                TextButton(onClick = { if (name.isNotBlank()) onCreate(name.trim()) }, enabled = name.isNotBlank()) {
                    Text(if (isItalian) "Crea" else "Create")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (isItalian) "Chiudi" else "Close") } }
    )
}

@Composable
private fun LibraryNameDialog(
    title: String,
    initialValue: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val isItalian = LocalLevyraStrings.current.code == "it"
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = value, onValueChange = { value = it }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
        confirmButton = {
            TextButton(onClick = { onConfirm(value.trim()) }, enabled = value.isNotBlank()) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (isItalian) "Annulla" else "Cancel") } }
    )
}

@Composable
private fun PlaylistDetailHeader(
    playlist: Playlist,
    durationMs: Long,
    reorderMode: Boolean,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onDownload: () -> Unit,
    onRename: () -> Unit,
    onReorder: () -> Unit,
    onSaveOrder: () -> Unit,
    isItalian: Boolean
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, tint = LevyraText) }
            Column(modifier = Modifier.weight(1f)) {
                Text(playlist.name, color = LevyraText, fontSize = 25.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (isItalian) "${playlist.size} brani · ${formatDuration(durationMs)}" else "${playlist.size} tracks · ${formatDuration(durationMs)}",
                    color = LevyraMuted,
                    fontSize = 12.sp
                )
            }
            if (reorderMode) {
                Button(onClick = onSaveOrder) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isItalian) "Salva" else "Save")
                }
            } else {
                Box {
                    IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = null, tint = LevyraText) }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(text = { Text(if (isItalian) "Rinomina" else "Rename") }, leadingIcon = { Icon(Icons.Rounded.Edit, null) }, onClick = { menuExpanded = false; onRename() })
                        DropdownMenuItem(text = { Text(if (isItalian) "Riordina" else "Reorder") }, leadingIcon = { Icon(Icons.Rounded.Sort, null) }, onClick = { menuExpanded = false; onReorder() })
                    }
                }
            }
        }
        if (!reorderMode && playlist.tracks.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onPlay, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (isItalian) "Riproduci" else "Play")
                }
                IconButton(onClick = onShuffle) { Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = LevyraText) }
                IconButton(onClick = onDownload) { Icon(Icons.Rounded.Download, contentDescription = null, tint = LevyraText) }
            }
        }
        if (reorderMode) {
            Text(
                if (isItalian) "Usa le frecce per spostare i brani. L'ordine viene scritto nel database solo quando premi Salva." else "Use the arrows to move tracks. The database is updated only when you press Save.",
                color = LevyraMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun PlaylistReorderRow(
    track: Track,
    index: Int,
    count: Int,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Surface(color = LevyraPanel.copy(alpha = 0.82f), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${index + 1}", color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
            LibraryArtwork(track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }, track.title, Modifier.size(50.dp), RoundedCornerShape(13.dp), false)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(track.title, color = LevyraText, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, color = LevyraMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onMoveUp, enabled = index > 0) { Icon(Icons.Rounded.ArrowUpward, contentDescription = null, tint = if (index > 0) LevyraText else LevyraMuted.copy(alpha = 0.3f)) }
            IconButton(onClick = onMoveDown, enabled = index < count - 1) { Icon(Icons.Rounded.ArrowDownward, contentDescription = null, tint = if (index < count - 1) LevyraText else LevyraMuted.copy(alpha = 0.3f)) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryNowPlayingDock(
    track: Track,
    isPlaying: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = LevyraPanel.copy(alpha = 0.98f),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
        shadowElevation = 12.dp,
        modifier = modifier.fillMaxWidth().combinedClickable(onClick = onOpen)
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            LibraryArtwork(track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }, track.title, Modifier.size(52.dp), RoundedCornerShape(14.dp), false)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(track.title, color = LevyraText, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, color = LevyraMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onToggle) { Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = null, tint = LevyraCyan) }
        }
    }
}

private fun LibraryCategory.libraryLabel(isItalian: Boolean): String = when (this) {
    LibraryCategory.Overview -> if (isItalian) "Raccolte" else "Overview"
    LibraryCategory.Playlists -> "Playlist"
    LibraryCategory.Albums -> "Album"
    LibraryCategory.Artists -> if (isItalian) "Artisti" else "Artists"
    LibraryCategory.Songs -> if (isItalian) "Brani" else "Tracks"
    LibraryCategory.Offline -> "Offline"
}

private fun LibrarySort.libraryLabel(isItalian: Boolean): String = when (this) {
    LibrarySort.Recent -> if (isItalian) "Aggiunti di recente" else "Recently added"
    LibrarySort.Title -> if (isItalian) "Titolo" else "Title"
    LibrarySort.Artist -> if (isItalian) "Artista" else "Artist"
    LibrarySort.Album -> "Album"
    LibrarySort.Duration -> if (isItalian) "Durata" else "Duration"
}

private fun LibraryAlbum.toAlbumHit(): AlbumHit = AlbumHit(
    title = title,
    artist = artist,
    year = year,
    thumbnailUrl = artworkUrl,
    query = listOf(artist, title).filter(String::isNotBlank).joinToString(" "),
    browseId = browseId,
    explicit = explicit
)

private fun Set<String>.toggle(value: String): Set<String> = if (value in this) this - value else this + value

private fun <T> List<T>.move(from: Int, to: Int): List<T> {
    if (from !in indices || to !in indices || from == to) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}


private fun downloadProgressFor(track: Track, state: LevyraUiState): Int? {
    state.downloadQueue.firstOrNull { task ->
        task.trackId.isNotBlank() && task.trackId == track.id && task.state in setOf("QUEUED", "RUNNING", "RETRYING", "PAUSED")
    }?.let { return it.progress.coerceIn(0, 100) }
    return state.downloadProgressByTrackId[track.id]
}

private fun formatDuration(durationMs: Long): String {
    val totalMinutes = (durationMs.coerceAtLeast(0L) / 60_000L)
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return if (hours > 0L) "${hours}h ${minutes}m" else "${minutes}m"
}

private fun formatBytes(bytes: Long): String {
    val safe = bytes.coerceAtLeast(0L).toDouble()
    return when {
        safe >= 1024.0 * 1024.0 * 1024.0 -> String.format(Locale.ROOT, "%.1f GB", safe / (1024.0 * 1024.0 * 1024.0))
        safe >= 1024.0 * 1024.0 -> String.format(Locale.ROOT, "%.0f MB", safe / (1024.0 * 1024.0))
        safe >= 1024.0 -> String.format(Locale.ROOT, "%.0f KB", safe / 1024.0)
        else -> "${safe.toLong()} B"
    }
}
