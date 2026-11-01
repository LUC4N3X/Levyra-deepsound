package com.luc4n3x.levyra.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.playableTrackCount
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.LibrarySort
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.CoverImage
import com.luc4n3x.levyra.ui.QobuzArtworkBox
import com.luc4n3x.levyra.ui.QobuzChip
import com.luc4n3x.levyra.ui.QobuzExplicitBadge
import com.luc4n3x.levyra.ui.QobuzText
import com.luc4n3x.levyra.ui.i18n.LevyraQobuzSkinStrings
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.QobuzSkinCopy
import com.luc4n3x.levyra.ui.levyraTabContentBottomPadding
import com.luc4n3x.levyra.ui.theme.QobuzDesign
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.LibraryViewModel

private sealed interface QobuzLibraryRow {
    val key: String
    val title: String
    val subtitle: String
    val image: String
    val circle: Boolean
    val stamp: Long
    data class Artist(val artist: LibraryArtist, override val subtitle: String) : QobuzLibraryRow {
        override val key = "artist-${artist.key}"; override val title = artist.name; override val image = artist.artworkUrl
        override val circle = true; override val stamp = artist.followedAt
    }
    data class Playlist(val playlist: com.luc4n3x.levyra.domain.Playlist, override val subtitle: String) : QobuzLibraryRow {
        override val key = "playlist-${playlist.id}"; override val title = playlist.name
        override val image = playlist.coverUrl.ifBlank { playlist.tracks.firstOrNull()?.thumbnailUrl.orEmpty() }
        override val circle = false; override val stamp = playlist.updatedAt
    }
    data class Album(val album: LibraryAlbum, override val subtitle: String) : QobuzLibraryRow {
        override val key = "album-${album.key}"; override val title = album.title; override val image = album.artworkUrl
        override val circle = false; override val stamp = 0L
    }
    data class Song(val track: Track, override val subtitle: String, override val stamp: Long) : QobuzLibraryRow {
        override val key = "song-${track.id}"; override val title = track.title; override val image = ""
        override val circle = false
    }
}

private enum class QobuzLibraryFilter { Playlists, Artists, Albums, Songs }

@Composable
internal fun QobuzLibraryScreen(
    viewModel: LibraryViewModel,
    state: LevyraUiState,
    catalog: LibraryCatalog,
    onOpenDownloads: () -> Unit,
    onOpenTools: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraQobuzSkinStrings.forCode(strings.code) }
    var filterName by rememberSaveable { mutableStateOf<String?>(null) }
    val filter = filterName?.let { name -> QobuzLibraryFilter.entries.firstOrNull { it.name == name } }
    var grid by rememberSaveable { mutableStateOf(false) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var sortOpen by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    val sort = state.interfaceSettings.librarySort
    val rows = remember(catalog, state.playlists, filter, sort, query, strings.code) {
        buildQobuzLibraryRows(catalog, state.playlists, filter, sort, query, strings, copy)
    }
    Column(Modifier.fillMaxSize().background(QobuzDesign.surfacePrimary).statusBarsPadding()) {
        QobuzLibraryHeader(
            strings = strings,
            searching = searching,
            onCreate = { creating = true },
            onToggleSearch = {
                searching = !searching
                if (!searching) query = ""
            }
        )
        if (searching) QobuzLibrarySearchField(strings, query) { query = it }
        QobuzLibraryFilterRow(strings, copy, filter, { filterName = it }, onOpenDownloads, onOpenTools)
        Box(Modifier.fillMaxWidth().height(1.dp).background(QobuzDesign.separator))
        QobuzLibraryControls(
            strings = strings,
            copy = copy,
            grid = grid,
            sort = sort,
            sortOpen = sortOpen,
            onGrid = { grid = it },
            onSortOpen = { sortOpen = it },
            onSort = { viewModel.setLibrarySort(it, it.defaultDirection) }
        )
        QobuzLibraryContent(viewModel, state, catalog, copy, rows, grid)
    }
    if (creating) QobuzCreatePlaylistDialog(strings, { creating = false }, viewModel::createPlaylist)
}

private fun buildQobuzLibraryRows(
    catalog: LibraryCatalog,
    playlists: List<com.luc4n3x.levyra.domain.Playlist>,
    filter: QobuzLibraryFilter?,
    sort: LibrarySort,
    query: String,
    strings: LevyraStrings,
    copy: QobuzSkinCopy
): List<QobuzLibraryRow> {
    val artistLabel = copy.artist
    val all = buildList {
        if (filter == null || filter == QobuzLibraryFilter.Artists) catalog.artists.forEach { add(QobuzLibraryRow.Artist(it, artistLabel)) }
        if (filter == null || filter == QobuzLibraryFilter.Playlists) playlists.filterNot { it.hidden }.forEach {
            add(QobuzLibraryRow.Playlist(it, listOf(strings.playlistsPlain, it.playableTrackCount.takeIf { size -> size > 0 }?.toString().orEmpty()).filter(String::isNotBlank).joinToString(" • ")))
        }
        if (filter == null || filter == QobuzLibraryFilter.Albums) catalog.albums.forEach { add(QobuzLibraryRow.Album(it, listOf(strings.albumPlain, it.artist).filter(String::isNotBlank).joinToString(" • "))) }
        if (filter == QobuzLibraryFilter.Songs) catalog.tracks.forEach {
            add(QobuzLibraryRow.Song(it, listOf(copy.trackType, it.artist).joinToString(" • "), catalog.trackRecency[it.id] ?: 0L))
        }
    }
    val needle = query.trim().lowercase()
    val visible = if (needle.isBlank()) all else all.filter { it.title.lowercase().contains(needle) || it.subtitle.lowercase().contains(needle) }
    return when (sort) {
        LibrarySort.Recent -> visible.sortedByDescending { it.stamp }
        LibrarySort.Title, LibrarySort.Album, LibrarySort.Duration -> visible.sortedBy { it.title.lowercase() }
        LibrarySort.Artist -> visible.sortedBy { it.subtitle.lowercase() }
    }
}

@Composable
private fun QobuzLibraryHeader(strings: LevyraStrings, searching: Boolean, onCreate: () -> Unit, onToggleSearch: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(QobuzDesign.topBarHeight).padding(start = QobuzDesign.gutter, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(strings.libraryTitle, style = QobuzText.pageTitle, modifier = Modifier.weight(1f).semantics { heading() })
        QobuzLibraryIcon(Icons.Rounded.Add, strings.newPlaylist, onCreate)
        QobuzLibraryIcon(if (searching) Icons.Rounded.Close else Icons.Rounded.Search, strings.search, onToggleSearch)
    }
}

@Composable
private fun QobuzLibrarySearchField(strings: LevyraStrings, query: String, onQuery: (String) -> Unit) {
    Box(
        Modifier.padding(horizontal = QobuzDesign.gutter, vertical = 6.dp).fillMaxWidth().height(48.dp)
            .clip(RoundedCornerShape(50)).background(QobuzDesign.surfaceSecondary).padding(horizontal = 18.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (query.isEmpty()) Text(strings.search, style = MaterialTheme.typography.bodyLarge, color = QobuzDesign.textSecondary)
        BasicTextField(query, onQuery, singleLine = true, cursorBrush = SolidColor(QobuzDesign.textAccent),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = QobuzDesign.textPrimary), modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun QobuzLibraryFilterRow(
    strings: LevyraStrings,
    copy: QobuzSkinCopy,
    filter: QobuzLibraryFilter?,
    onFilterName: (String?) -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenTools: () -> Unit
) {
    LazyRow(contentPadding = PaddingValues(horizontal = QobuzDesign.gutter, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(QobuzDesign.chipGap)) {
        items(QobuzLibraryFilter.entries, key = { it.name }) { option ->
            val label = when (option) {
                QobuzLibraryFilter.Playlists -> strings.playlistsPlain
                QobuzLibraryFilter.Artists -> strings.artists
                QobuzLibraryFilter.Albums -> strings.albumsPlain
                QobuzLibraryFilter.Songs -> strings.songsPlain
            }
            QobuzChip(label, filter == option, { onFilterName(if (filter == option) null else option.name) })
        }
        item(key = "downloads") { QobuzLibraryChip(Icons.Outlined.DownloadForOffline, strings.downloads, onOpenDownloads) }
        item(key = "tools") { QobuzLibraryChip(Icons.Rounded.Tune, copy.levyraTools, onOpenTools) }
    }
}

@Composable
private fun QobuzLibraryControls(
    strings: LevyraStrings,
    copy: QobuzSkinCopy,
    grid: Boolean,
    sort: LibrarySort,
    sortOpen: Boolean,
    onGrid: (Boolean) -> Unit,
    onSortOpen: (Boolean) -> Unit,
    onSort: (LibrarySort) -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = QobuzDesign.gutter, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.clip(RoundedCornerShape(50)).background(QobuzDesign.surfaceSecondary)) {
            QobuzLayoutToggle(Icons.AutoMirrored.Rounded.ViewList, !grid) { onGrid(false) }
            QobuzLayoutToggle(Icons.Rounded.GridView, grid) { onGrid(true) }
        }
        Box(Modifier.padding(horizontal = 16.dp).width(1.dp).height(28.dp).background(QobuzDesign.separatorStrong))
        Box {
            Row(Modifier.heightIn(min = 48.dp).clickable(role = Role.Button) { onSortOpen(true) }, verticalAlignment = Alignment.CenterVertically) {
                Text(if (sort == LibrarySort.Recent) copy.recentlyAdded else sort.libraryLabel(strings), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium), color = QobuzDesign.textPrimary)
                Icon(Icons.Rounded.ArrowDropDown, null, tint = QobuzDesign.textPrimary)
            }
            DropdownMenu(sortOpen, { onSortOpen(false) }) {
                LibrarySort.entries.forEach { option ->
                    DropdownMenuItem(text = { Text(if (option == LibrarySort.Recent) copy.recentlyAdded else option.libraryLabel(strings)) }, onClick = {
                        onSortOpen(false)
                        onSort(option)
                    })
                }
            }
        }
    }
}

@Composable
private fun QobuzLibraryContent(
    viewModel: LibraryViewModel,
    state: LevyraUiState,
    catalog: LibraryCatalog,
    copy: QobuzSkinCopy,
    rows: List<QobuzLibraryRow>,
    grid: Boolean
) {
    val bottom = levyraTabContentBottomPadding(state.currentTrack != null) + 16.dp
    LazyVerticalGrid(
        columns = if (grid) GridCells.Adaptive(QobuzDesign.gridCard) else GridCells.Fixed(1),
        contentPadding = PaddingValues(start = QobuzDesign.gutter, end = if (grid) QobuzDesign.gutter else 4.dp, bottom = bottom),
        horizontalArrangement = Arrangement.spacedBy(QobuzDesign.itemGap),
        verticalArrangement = Arrangement.spacedBy(if (grid) 20.dp else 0.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(rows, key = { it.key }) { row ->
            val open = { openRow(row, viewModel, catalog, rows) }
            if (grid) QobuzLibraryCard(row, open) else QobuzLibraryListRow(row, state, copy.explicit, open, { viewModel.toggleFollowArtistRow(row) })
        }
        item(key = "count", span = { GridItemSpan(maxLineSpan) }) {
            Text(copy.itemsCount.format(rows.size), style = MaterialTheme.typography.bodyLarge, color = QobuzDesign.textSecondary, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp))
        }
    }
}

@Composable
private fun QobuzCreatePlaylistDialog(strings: LevyraStrings, onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.newPlaylist) },
        text = { OutlinedTextField(name, { name = it }, singleLine = true, label = { Text(strings.playlistName) }) },
        confirmButton = { TextButton(onClick = {
                if (name.isNotBlank()) {
                    onCreate(name.trim())
                }
                onDismiss()
            }, enabled = name.isNotBlank()) { Text(strings.create) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

private fun LibraryViewModel.toggleFollowArtistRow(row: QobuzLibraryRow) {
    if (row is QobuzLibraryRow.Artist) toggleFollowArtistReference(row.artist.name, row.artist.browseId, row.artist.artworkUrl)
}

private fun openRow(row: QobuzLibraryRow, viewModel: LibraryViewModel, catalog: LibraryCatalog, rows: List<QobuzLibraryRow>) {
    when (row) {
        is QobuzLibraryRow.Artist -> viewModel.openArtistReference(row.artist.name, row.artist.browseId, row.artist.artworkUrl)
        is QobuzLibraryRow.Playlist -> viewModel.openPlaylist(row.playlist.id)
        is QobuzLibraryRow.Album -> viewModel.openAlbum(
            AlbumHit(row.album.title, row.album.artist, row.album.year, row.album.artworkUrl, "${row.album.title} ${row.album.artist}".trim(),
                browseId = row.album.browseId, explicit = row.album.explicit)
        )
        is QobuzLibraryRow.Song -> {
            val songs = rows.filterIsInstance<QobuzLibraryRow.Song>().map { it.track }
            viewModel.playFrom(songs.ifEmpty { catalog.tracks }, row.track)
        }
    }
}

@Composable
private fun QobuzLibraryListRow(row: QobuzLibraryRow, state: LevyraUiState, explicitLabel: String, onClick: () -> Unit, onFollow: () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().heightIn(min = 80.dp).clickable(role = Role.Button, onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
            QobuzLibraryArtwork(row, Modifier.size(QobuzDesign.listArtwork))
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(row.title, style = MaterialTheme.typography.titleSmall.copy(fontSize = 17.sp, fontWeight = FontWeight.Medium),
                    color = if (row is QobuzLibraryRow.Song && row.track.id == state.currentTrack?.id) QobuzDesign.textAccent else QobuzDesign.textPrimary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (row is QobuzLibraryRow.Song && row.track.explicit) {
                        QobuzExplicitBadge(explicitLabel)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(row.subtitle, style = QobuzText.cardSubtitle.copy(fontSize = 15.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (row is QobuzLibraryRow.Artist) QobuzLibraryIcon(Icons.Rounded.HowToReg, LocalLevyraStrings.current.followingArtist, onFollow)
            QobuzLibraryIcon(Icons.Rounded.MoreVert, LocalLevyraStrings.current.more, onClick)
        }
        Box(Modifier.fillMaxWidth().padding(end = 12.dp).height(1.dp).background(QobuzDesign.separator))
    }
}

@Composable
private fun QobuzLibraryCard(row: QobuzLibraryRow, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick), horizontalAlignment = if (row.circle) Alignment.CenterHorizontally else Alignment.Start) {
        QobuzLibraryArtwork(row, Modifier.fillMaxWidth().aspectRatio(1f))
        Spacer(Modifier.height(8.dp))
        Text(row.title, style = QobuzText.cardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(row.subtitle, style = QobuzText.cardSubtitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun QobuzLibraryArtwork(row: QobuzLibraryRow, modifier: Modifier) {
    val shape = if (row.circle) CircleShape else QobuzDesign.artworkShape
    if (row is QobuzLibraryRow.Song) {
        Box(modifier.clip(shape).background(QobuzDesign.placeholder)) { CoverImage(row.track, Modifier.fillMaxSize()) }
    } else {
        QobuzArtworkBox(row.image, modifier, shape)
    }
}

@Composable
private fun QobuzLayoutToggle(icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(width = 52.dp, height = 40.dp).clip(RoundedCornerShape(50)).background(if (active) QobuzDesign.buttonPrimary else androidx.compose.ui.graphics.Color.Transparent)
            .semantics { selected = active }.clickable(role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = if (active) QobuzDesign.buttonPrimaryContent else QobuzDesign.textPrimary, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun QobuzLibraryChip(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.heightIn(min = QobuzDesign.chipHeight).clip(QobuzDesign.chipShape).background(QobuzDesign.surfaceSecondary)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = QobuzDesign.chipHorizontal, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = QobuzDesign.textPrimary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Normal), color = QobuzDesign.textPrimary)
    }
}

@Composable
private fun QobuzLibraryIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = label, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = QobuzDesign.iconPrimary, modifier = Modifier.size(26.dp))
    }
}
