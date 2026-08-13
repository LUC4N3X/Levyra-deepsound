package com.luc4n3x.levyra.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraInk
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.LevyraViewModel
import com.luc4n3x.levyra.viewmodel.LibraryViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LevyraLibraryScreen(
    viewModel: LibraryViewModel,
    state: LevyraUiState,
    onOpenDownloads: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val catalog = remember(
        state.favorites,
        state.playlists,
        state.downloads,
        state.recentListens,
        state.mostPlayedTracks,
        state.followedArtists
    ) {
        buildLibraryCatalog(
            favorites = state.favorites,
            playlists = state.playlists,
            downloads = state.downloads,
            recentListens = state.recentListens,
            followedArtists = state.followedArtists,
            mostPlayedTracks = state.mostPlayedTracks
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
    var showImportPlaylist by remember { mutableStateOf(false) }
    var showImportPlaylistCard by rememberSaveable { mutableStateOf(true) }
    var openSmartCollectionName by rememberSaveable { mutableStateOf<String?>(null) }
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

    BackHandler(enabled = !selectionActive && openSmartCollectionName == null && category != LibraryCategory.Overview) {
        switchCategory(LibraryCategory.Overview)
    }
    BackHandler(enabled = !selectionActive && openSmartCollectionName != null) { openSmartCollectionName = null }
    BackHandler(enabled = selectionActive) { selectedKeys = emptySet() }

    Box(modifier = Modifier.fillMaxSize().background(LevyraInk)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
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
                    title = strings.libraryTitle,
                    subtitle = listOf(
                        strings.formatTrackCount(catalog.tracks.size),
                        "${state.playlists.size} ${strings.playlists}",
                        strings.formatDownloadedTrackCount(state.downloads.size)
                    ).joinToString(" · ")
                )
            }

            item(key = "library-search") {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = LevyraText,
                        fontWeight = FontWeight.SemiBold
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LevyraText,
                        unfocusedTextColor = LevyraText,
                        focusedContainerColor = LevyraPanel.copy(alpha = 0.72f),
                        unfocusedContainerColor = LevyraPanel.copy(alpha = 0.55f),
                        focusedBorderColor = LevyraCyan.copy(alpha = 0.56f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.10f),
                        cursorColor = LevyraCyan,
                        focusedLeadingIconColor = LevyraCyan,
                        unfocusedLeadingIconColor = LevyraMuted,
                        focusedTrailingIconColor = LevyraMuted,
                        unfocusedTrailingIconColor = LevyraMuted
                    ),
                    placeholder = { Text(strings.searchPlaceholder, color = LevyraMuted, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(22.dp))
                    },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = strings.clear)
                            }
                        }
                    }
                )
            }

            item(key = "library-categories") {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LibraryCategory.entries.forEach { item ->
                        LibraryCategoryChip(
                            label = item.libraryLabel(strings),
                            selected = item == category,
                            onClick = { switchCategory(item) }
                        )
                    }
                }
            }

            if (category != LibraryCategory.Overview) {
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
                        }
                    )
                }
            }

            when (category) {
                LibraryCategory.Overview -> {
                    item(key = "overview-smart-title") {
                        LibrarySectionTitle(strings.quickPicks, strings.librarySubtitle)
                    }
                    item(key = "overview-smart-grid") {
                        SmartCollectionGrid(
                            favorites = state.favorites,
                            downloads = catalog.offlineTracks,
                            recent = catalog.recent,
                            mostPlayed = catalog.mostPlayed,
                            onOpenCollection = { openSmartCollectionName = it },
                            onOpenOffline = { switchCategory(LibraryCategory.Offline) }
                        )
                    }
                    item(key = "overview-insights-title") {
                        LibrarySectionTitle(strings.pulseTitle, strings.pulseSubtitle)
                    }
                    item(key = "overview-insights-card") {
                        LibraryListeningDashboard(
                            pulse = state.listeningPulse,
                            artistCount = catalog.artists.size,
                            trackCount = catalog.tracks.size,
                            playlistCount = state.playlists.size,
                            offlineCount = state.downloads.size
                        )
                    }
                    if (visiblePlaylists.isNotEmpty()) {
                        item(key = "overview-playlists-title") {
                            LibrarySectionTitle(
                                title = strings.playlists,
                                detail = strings.personalPlaylists,
                                action = strings.showAll,
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
                                onPlay = { viewModel.playPlaylist(playlist.id) }
                            )
                        }
                    }
                    if (catalog.albums.isNotEmpty()) {
                        item(key = "overview-albums-title") {
                            LibrarySectionTitle(
                                title = strings.albumsPlain,
                                detail = strings.savedTracks,
                                action = strings.showAll,
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
                }

                LibraryCategory.Playlists -> {
                    item(key = "playlist-heading") {
                        LibrarySectionTitle(
                            title = strings.playlists,
                            detail = "${visiblePlaylists.size} ${strings.playlists}",
                            action = strings.newItem,
                            onAction = { showCreatePlaylist = true }
                        )
                    }
                    item(key = "playlist-import-action") {
                        if (showImportPlaylistCard) {
                            LibraryImportPlaylistCard(
                                onClick = { showImportPlaylist = true },
                                onDismiss = { showImportPlaylistCard = false }
                            )
                        } else {
                            LibraryImportPlaylistCompactAction(onClick = { showImportPlaylist = true })
                        }
                    }
                    if (visiblePlaylists.isEmpty()) {
                        item { LibraryEmpty(Icons.AutoMirrored.Rounded.QueueMusic, strings.createFirstPlaylistSubtitle) }
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
                                onPlay = { viewModel.playPlaylist(playlist.id) }
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
                                onSelect = { playlist -> selectedKeys = selectedKeys.toggle("playlist:${playlist.id}") }
                            )
                        }
                    }
                }

                LibraryCategory.Albums -> {
                    item(key = "album-heading") {
                        LibrarySectionTitle(strings.albumsPlain, strings.savedTracks)
                    }
                    if (visibleAlbums.isEmpty()) {
                        item { LibraryEmpty(Icons.Rounded.Album, strings.albumUnavailable) }
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
                                onPlay = { album.tracks.firstOrNull()?.let { viewModel.playFrom(album.tracks, it) } }
                            )
                        }
                    }
                }

                LibraryCategory.Artists -> {
                    item(key = "artist-heading") {
                        LibrarySectionTitle(strings.artists, strings.followedArtistsSubtitle)
                    }
                    if (visibleArtists.isEmpty()) {
                        item { LibraryEmpty(Icons.Rounded.Person, strings.artistProfileUnavailable) }
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
                                onSelect = { artist -> selectedKeys = selectedKeys.toggle("artist:${artist.key}") }
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
                                onPlay = { artist.tracks.firstOrNull()?.let { viewModel.playFrom(artist.tracks, it) } }
                            )
                        }
                    }
                }

                LibraryCategory.Songs -> {
                    item(key = "songs-heading") {
                        LibrarySectionTitle(strings.songsPlain, strings.savedTracks)
                    }
                    if (visibleTracks.isEmpty()) {
                        item { LibraryEmpty(Icons.Rounded.MusicNote, strings.emptySearchPrompt) }
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
                                onDownload = { viewModel.exportTrack(track) }
                            )
                        }
                    }
                }

                LibraryCategory.Offline -> {
                    item(key = "offline-storage") {
                        LibraryStorageCard(
                            bytes = state.downloadStorageBytes,
                            count = state.downloads.size,
                            activeCount = state.downloadQueue.count {
                                it.state in setOf("QUEUED", "RUNNING", "RETRYING", "PAUSED")
                            },
                            onOpenFolder = onOpenDownloads
                        )
                    }
                    if (state.downloadQueue.isNotEmpty()) {
                        item(key = "offline-queue-title") {
                            LibrarySectionTitle(strings.downloadsInProgress, strings.downloadInProgress)
                        }
                        items(state.downloadQueue, key = { "task-${it.taskKey}" }) { task ->
                            LibraryDownloadTaskRow(
                                task = task,
                                onPause = { viewModel.pauseDownload(task.taskKey) },
                                onResume = { viewModel.resumeDownload(task.taskKey) },
                                onCancel = { viewModel.cancelDownload(task.taskKey) }
                            )
                        }
                    }
                    item(key = "offline-heading") {
                        LibrarySectionTitle(strings.offlineDownloadsPlain, strings.downloadsFolder)
                    }
                    if (visibleOffline.isEmpty()) {
                        item { LibraryEmpty(Icons.Rounded.OfflinePin, strings.noOfflineDownloads) }
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
                                onDownload = {}
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
                    viewModel.exportTracks(selectedTracks, strings.offline)
                    selectedKeys = emptySet()
                },
                onAddToPlaylist = { addToPlaylistTracks = selectedTracks },
                onDelete = { confirmDelete = true }
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
                Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = strings.newPlaylist)
            }
        }

        openSmartCollectionName?.let { collectionId ->
            SmartCollectionDetail(
                collectionId = collectionId,
                state = state,
                tracks = when (collectionId) {
                    SMART_COLLECTION_RECENT -> catalog.recent
                    SMART_COLLECTION_MOST_PLAYED -> catalog.mostPlayed
                    else -> state.favorites
                },
                viewModel = viewModel,
                onClose = { openSmartCollectionName = null }
            )
        }
    }

    if (showCreatePlaylist) {
        LibraryNameDialog(
            title = strings.newPlaylist,
            initialValue = "",
            confirmLabel = strings.create,
            onDismiss = { showCreatePlaylist = false },
            onConfirm = { name ->
                viewModel.createPlaylist(name)
                showCreatePlaylist = false
            }
        )
    }

    if (showImportPlaylist) {
        LibraryImportPlaylistDialog(
            onDismiss = { showImportPlaylist = false },
            onImport = { input ->
                viewModel.importPlaylist(input)
                showImportPlaylist = false
            }
        )
    }

    if (addToPlaylistTracks.isNotEmpty()) {
        AddTracksToPlaylistDialog(
            tracks = addToPlaylistTracks,
            playlists = state.playlists,
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
        val targetCount = when (category) {
            LibraryCategory.Playlists -> selectedPlaylists.size
            LibraryCategory.Offline -> selectedDownloads.size
            else -> selectedTracks.size
        }
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(strings.check) },
            text = { Text("${category.libraryLabel(strings)} · $targetCount") },
            confirmButton = {
                TextButton(onClick = {
                    when (category) {
                        LibraryCategory.Playlists -> viewModel.deletePlaylists(selectedPlaylists.map { it.id })
                        LibraryCategory.Offline -> viewModel.deleteDownloads(selectedDownloads)
                        else -> viewModel.removeFavorites(selectedTracks)
                    }
                    selectedKeys = emptySet()
                    confirmDelete = false
                }) { Text(strings.delete) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(strings.cancel) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LevyraPlaylistDetailScreen(
    viewModel: LevyraViewModel,
    state: LevyraUiState
) {
    val playlist = state.openPlaylist ?: return
    val strings = LocalLevyraStrings.current
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

    Box(modifier = Modifier.fillMaxSize().background(LevyraInk)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = if (state.currentTrack != null || selectionActive) 220.dp else 110.dp
            ),
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
                    onDownload = { viewModel.exportTracks(orderedTracks, strings.offline) },
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
                    }
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
                        placeholder = { Text(strings.searchPlaceholder) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = if (query.isNotBlank()) {
                            { IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, contentDescription = strings.clear) } }
                        } else null
                    )
                }
            }

            if (orderedTracks.isEmpty()) {
                item { LibraryEmpty(Icons.AutoMirrored.Rounded.QueueMusic, strings.playlistEmpty) }
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
                        onDownload = { viewModel.exportTrack(track) }
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
                    viewModel.exportTracks(selectedTracks, strings.offline)
                    selectedKeys = emptySet()
                },
                onAddToPlaylist = { addTracksDialog = true },
                onDelete = { confirmRemove = true },
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
            title = strings.playlistName,
            initialValue = playlist.name,
            confirmLabel = strings.save,
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
            title = { Text(strings.remove) },
            text = { Text(strings.formatTrackCount(selectedTracks.size)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeTracksFromPlaylist(playlist.id, selectedTracks)
                    selectedKeys = emptySet()
                    confirmRemove = false
                }) { Text(strings.remove) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) { Text(strings.cancel) }
            }
        )
    }
}
