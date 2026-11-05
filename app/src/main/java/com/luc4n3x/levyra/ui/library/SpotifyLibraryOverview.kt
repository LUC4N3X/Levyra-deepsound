package com.luc4n3x.levyra.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowCircleDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.SpotifyLibraryCopy
import com.luc4n3x.levyra.ui.theme.SpotifyDesign
import java.util.Locale

internal enum class SpotifyLibraryFilter { Playlists, Artists, Albums, Downloaded }

internal enum class SpotifyLibrarySort { Recents, RecentlyAdded, Alphabetical, Creator }

internal enum class SpotifyLibraryKind { Liked, Downloads, Playlist, Album, Artist }

@Immutable
internal data class SpotifyLibraryItem(
    val id: String,
    val kind: SpotifyLibraryKind,
    val title: String,
    val creator: String,
    val artworkUrl: String,
    val count: Int,
    val playedAt: Long,
    val addedAt: Long,
    val downloaded: Boolean
) {
    val pinned: Boolean get() = kind == SpotifyLibraryKind.Liked || kind == SpotifyLibraryKind.Downloads
    val key: String get() = "${kind.name}:$id"
}

internal fun spotifyLibraryArrange(
    items: List<SpotifyLibraryItem>,
    filter: SpotifyLibraryFilter?,
    sort: SpotifyLibrarySort
): List<SpotifyLibraryItem> {
    val filtered = items.filter { item ->
        when (filter) {
            null -> true
            SpotifyLibraryFilter.Playlists -> item.kind == SpotifyLibraryKind.Liked || item.kind == SpotifyLibraryKind.Playlist
            SpotifyLibraryFilter.Artists -> item.kind == SpotifyLibraryKind.Artist
            SpotifyLibraryFilter.Albums -> item.kind == SpotifyLibraryKind.Album
            SpotifyLibraryFilter.Downloaded -> item.downloaded
        }
    }
    val (pinned, rest) = filtered.partition { it.pinned }
    val title = compareBy<SpotifyLibraryItem> { it.title.lowercase(Locale.ROOT) }
    val sorted = when (sort) {
        SpotifyLibrarySort.Recents -> rest.sortedWith(compareByDescending<SpotifyLibraryItem> { it.playedAt }.then(title))
        SpotifyLibrarySort.RecentlyAdded -> rest.sortedWith(compareByDescending<SpotifyLibraryItem> { it.addedAt }.then(title))
        SpotifyLibrarySort.Alphabetical -> rest.sortedWith(title)
        SpotifyLibrarySort.Creator -> rest.sortedWith(compareBy<SpotifyLibraryItem> { it.creator.lowercase(Locale.ROOT) }.then(title))
    }
    return pinned + sorted
}

internal fun buildSpotifyLibraryItems(
    favorites: List<com.luc4n3x.levyra.domain.Track>,
    downloadCount: Int,
    playlists: List<com.luc4n3x.levyra.domain.Playlist>,
    catalog: LibraryCatalog,
    downloadedIds: Set<String>,
    owner: String,
    likedTitle: String,
    downloadsTitle: String
): List<SpotifyLibraryItem> {
    fun played(tracks: List<com.luc4n3x.levyra.domain.Track>): Long = tracks.maxOfOrNull(catalog::recencyOf) ?: 0L
    fun saved(tracks: List<com.luc4n3x.levyra.domain.Track>): Boolean = tracks.isNotEmpty() && tracks.all { it.id in downloadedIds }
    return buildList {
        add(SpotifyLibraryItem("liked", SpotifyLibraryKind.Liked, likedTitle, "", "", favorites.size, 0L, 0L, saved(favorites)))
        if (downloadCount > 0) add(SpotifyLibraryItem("downloads", SpotifyLibraryKind.Downloads, downloadsTitle, "", "", downloadCount, 0L, 0L, true))
        playlists.forEach { playlist ->
            val artwork = playlist.coverUrl.ifBlank { playlist.tracks.firstOrNull()?.thumbnailUrl.orEmpty() }
            add(
                SpotifyLibraryItem(
                    playlist.id, SpotifyLibraryKind.Playlist, playlist.name, owner, artwork, playlist.size,
                    maxOf(played(playlist.tracks), playlist.updatedAt), playlist.createdAt, saved(playlist.tracks)
                )
            )
        }
        catalog.albums.forEach { album ->
            val last = played(album.tracks)
            add(SpotifyLibraryItem(album.key, SpotifyLibraryKind.Album, album.title, album.artist, album.artworkUrl, album.tracks.size, last, last, saved(album.tracks)))
        }
        catalog.artists.forEach { artist ->
            add(
                SpotifyLibraryItem(
                    artist.key, SpotifyLibraryKind.Artist, artist.name, artist.name, artist.artworkUrl, artist.tracks.size,
                    maxOf(played(artist.tracks), artist.followedAt), artist.followedAt.takeIf { it > 0L } ?: played(artist.tracks), false
                )
            )
        }
    }
}

@Immutable
internal class SpotifyLibraryLabels(
    val title: String,
    val userName: String,
    val copy: SpotifyLibraryCopy,
    val filterLabel: (SpotifyLibraryFilter) -> String,
    val subtitle: (SpotifyLibraryItem) -> String,
    val jam: String,
    val search: String,
    val create: String,
    val clear: String,
    val cancel: String
)

@Immutable
internal class SpotifyLibraryActions(
    val onJam: () -> Unit,
    val onSearch: () -> Unit,
    val onCreate: () -> Unit,
    val onFilter: (SpotifyLibraryFilter?) -> Unit,
    val onSortOpen: () -> Unit,
    val onToggleLayout: () -> Unit,
    val onOpen: (SpotifyLibraryItem) -> Unit
)

internal fun LazyListScope.spotifyLibraryOverview(
    items: List<SpotifyLibraryItem>,
    filter: SpotifyLibraryFilter?,
    sort: SpotifyLibrarySort,
    grid: Boolean,
    labels: SpotifyLibraryLabels,
    actions: SpotifyLibraryActions
) {
    item(key = "spotify-library-filters", contentType = "spotify-library-filters") { SpotifyLibraryFilters(filter, labels, actions.onFilter) }
    item(key = "spotify-library-sort", contentType = "spotify-library-sort") { SpotifyLibrarySortRow(sort, grid, labels, actions) }
    if (grid) {
        items.chunked(SpotifyLibraryGridColumns).forEach { row ->
            item(key = "spotify-library-grid-${row.first().key}", contentType = "spotify-library-grid") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { entry -> SpotifyLibraryTile(entry, labels, Modifier.weight(1f)) { actions.onOpen(entry) } }
                    repeat(SpotifyLibraryGridColumns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    } else {
        items(items, key = { "spotify-library-${it.key}" }, contentType = { "spotify-library-row" }) { entry ->
            SpotifyLibraryListRow(entry, labels) { actions.onOpen(entry) }
        }
    }
}

@Composable
internal fun SpotifyLibraryHeader(labels: SpotifyLibraryLabels, actions: SpotifyLibraryActions) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(SpotifyAvatarColor), contentAlignment = Alignment.Center) {
            Text(
                labels.userName.trim().take(1).uppercase().ifBlank { "L" },
                color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Bold
            )
        }
        Text(
            labels.title, color = SpotifyDesign.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 12.dp).semantics { heading() }
        )
        SpotifyHeaderIcon(Icons.Rounded.Groups, labels.jam, actions.onJam)
        SpotifyHeaderIcon(Icons.Rounded.Search, labels.search, actions.onSearch)
        SpotifyHeaderIcon(Icons.Rounded.Add, labels.create, actions.onCreate)
    }
}

@Composable
private fun SpotifyHeaderIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
        Icon(icon, contentDescription = label, tint = SpotifyDesign.text, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun SpotifyLibraryFilters(
    filter: SpotifyLibraryFilter?,
    labels: SpotifyLibraryLabels,
    onFilter: (SpotifyLibraryFilter?) -> Unit
) {
    LazyRow(
        modifier = Modifier.libraryCategoriesBleed(LibraryCategoriesGutter),
        contentPadding = PaddingValues(horizontal = LibraryCategoriesGutter),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (filter != null) {
            item(key = "clear") {
                Box(
                    Modifier.size(SpotifyDesign.chipHeight).clip(CircleShape).background(SpotifyDesign.chip)
                        .levyraPressable(onClick = { onFilter(null) }, onClickLabel = labels.clear, role = Role.Button, pressedScale = LevyraPressScale.Control),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = labels.clear, tint = SpotifyDesign.text, modifier = Modifier.size(18.dp))
                }
            }
        }
        items(SpotifyLibraryFilter.entries.filter { filter == null || it == filter }, key = { it.name }) { option ->
            val selected = option == filter
            Box(
                Modifier.height(SpotifyDesign.chipHeight).clip(CircleShape)
                    .background(if (selected) SpotifyDesign.green else SpotifyDesign.chip)
                    .levyraPressable(
                        onClick = { onFilter(if (selected) null else option) },
                        onClickLabel = labels.filterLabel(option), role = Role.Tab, pressedScale = LevyraPressScale.Control
                    )
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    labels.filterLabel(option), color = if (selected) Color.Black else SpotifyDesign.text,
                    fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SpotifyLibrarySortRow(sort: SpotifyLibrarySort, grid: Boolean, labels: SpotifyLibraryLabels, actions: SpotifyLibraryActions) {
    Row(Modifier.fillMaxWidth().padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.clip(CircleShape)
                .levyraPressable(onClick = actions.onSortOpen, onClickLabel = labels.copy.sortBy, role = Role.Button, pressedScale = LevyraPressScale.Control)
                .padding(vertical = 8.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.SwapVert, contentDescription = null, tint = SpotifyDesign.text, modifier = Modifier.size(18.dp))
            Text(
                spotifyLibrarySortLabel(sort, labels.copy), color = SpotifyDesign.text, fontSize = 14.sp,
                fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 8.dp)
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = actions.onToggleLayout, modifier = Modifier.size(40.dp)) {
            Icon(
                if (grid) Icons.Rounded.ViewList else Icons.Rounded.GridView,
                contentDescription = if (grid) labels.copy.listView else labels.copy.gridView,
                tint = SpotifyDesign.text, modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SpotifyLibraryListRow(entry: SpotifyLibraryItem, labels: SpotifyLibraryLabels, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = SpotifyDesign.libraryCover)
            .levyraPressable(onClick = onClick, onClickLabel = entry.title, role = Role.Button, pressedScale = LevyraPressScale.Control),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SpotifyLibraryArtwork(entry, Modifier.size(SpotifyDesign.libraryCover))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(entry.title, color = SpotifyDesign.text, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            SpotifyLibraryMeta(entry, labels, Modifier.padding(top = 4.dp))
        }
        if (entry.kind == SpotifyLibraryKind.Downloads) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = SpotifyDesign.subdued, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun SpotifyLibraryMeta(entry: SpotifyLibraryItem, labels: SpotifyLibraryLabels, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (entry.pinned) {
            Icon(Icons.Rounded.PushPin, contentDescription = null, tint = SpotifyDesign.green, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
        }
        if (entry.downloaded) {
            Icon(Icons.Rounded.ArrowCircleDown, contentDescription = null, tint = SpotifyDesign.green, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(labels.subtitle(entry), color = SpotifyDesign.subdued, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SpotifyLibraryTile(entry: SpotifyLibraryItem, labels: SpotifyLibraryLabels, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.levyraPressable(onClick = onClick, onClickLabel = entry.title, role = Role.Button, pressedScale = LevyraPressScale.Tile)
            .padding(bottom = 6.dp)
    ) {
        SpotifyLibraryArtwork(entry, Modifier.fillMaxWidth().aspectRatio(1f))
        Text(
            entry.title, color = SpotifyDesign.text, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp)
        )
        Text(labels.subtitle(entry), color = SpotifyDesign.subdued, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SpotifyLibraryArtwork(entry: SpotifyLibraryItem, modifier: Modifier) {
    val shape = if (entry.kind == SpotifyLibraryKind.Artist) CircleShape else RoundedCornerShape(SpotifyDesign.cornerSmall)
    when (entry.kind) {
        SpotifyLibraryKind.Liked -> SpotifyLibraryGlyph(Icons.Rounded.Favorite, SpotifyLikedBrush, shape, modifier)
        SpotifyLibraryKind.Downloads -> SpotifyLibraryGlyph(Icons.Rounded.ArrowCircleDown, SpotifyDownloadsBrush, shape, modifier)
        else -> if (entry.artworkUrl.isNotBlank()) {
            AsyncImage(
                model = entry.artworkUrl, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = modifier.clip(shape).background(SpotifyDesign.placeholder)
            )
        } else {
            Box(modifier.clip(shape).background(SpotifyDesign.highlight).border(BorderStroke(0.5.dp, SpotifyDesign.decorative), shape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = SpotifyDesign.subdued, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
private fun SpotifyLibraryGlyph(icon: ImageVector, brush: Brush, shape: androidx.compose.ui.graphics.Shape, modifier: Modifier) {
    Box(modifier.clip(shape).background(brush), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SpotifyLibrarySortSheet(
    sort: SpotifyLibrarySort,
    copy: SpotifyLibraryCopy,
    cancel: String,
    onSelect: (SpotifyLibrarySort) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SpotifyDesign.sheet,
        dragHandle = null
    ) {
        Column(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 12.dp)) {
            Text(
                copy.sortBy, color = SpotifyDesign.subdued, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).semantics { heading() }
            )
            SpotifyLibrarySort.entries.forEach { option ->
                val selected = option == sort
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 52.dp)
                        .levyraPressable(onClick = { onSelect(option) }, onClickLabel = spotifyLibrarySortLabel(option, copy), role = Role.RadioButton, pressedScale = LevyraPressScale.Control)
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        spotifyLibrarySortLabel(option, copy), color = if (selected) SpotifyDesign.greenText else SpotifyDesign.text,
                        fontSize = 16.sp, modifier = Modifier.weight(1f)
                    )
                    if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = SpotifyDesign.green, modifier = Modifier.size(22.dp))
                }
            }
            Box(
                Modifier.fillMaxWidth().padding(top = 12.dp).heightIn(min = 52.dp)
                    .levyraPressable(onClick = onDismiss, onClickLabel = cancel, role = Role.Button, pressedScale = LevyraPressScale.Control),
                contentAlignment = Alignment.Center
            ) {
                Text(cancel, color = SpotifyDesign.subdued, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

internal fun spotifyLibrarySortLabel(sort: SpotifyLibrarySort, copy: SpotifyLibraryCopy): String = when (sort) {
    SpotifyLibrarySort.Recents -> copy.recents
    SpotifyLibrarySort.RecentlyAdded -> copy.recentlyAdded
    SpotifyLibrarySort.Alphabetical -> copy.alphabetical
    SpotifyLibrarySort.Creator -> copy.creator
}

private const val SpotifyLibraryGridColumns = 3
private val SpotifyAvatarColor = Color(0xFFF59B23)
private val SpotifyLikedBrush = Brush.linearGradient(listOf(Color(0xFF450AF5), Color(0xFF8E8EE5), Color(0xFFC4EFD9)))
private val SpotifyDownloadsBrush = Brush.linearGradient(listOf(Color(0xFF006450), Color(0xFF1ED760)))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SpotifyLibraryCreateSheet(newLabel: String, importLabel: String, onNew: () -> Unit, onImport: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SpotifyDesign.sheet
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            SpotifyCreateOption(Icons.Rounded.MusicNote, newLabel, onNew)
            SpotifyCreateOption(Icons.AutoMirrored.Rounded.PlaylistAdd, importLabel, onImport)
        }
    }
}

@Composable
private fun SpotifyCreateOption(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp)
            .levyraPressable(onClick = onClick, onClickLabel = label, role = Role.Button, pressedScale = LevyraPressScale.Control)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(SpotifyDesign.highlight), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = SpotifyDesign.text, modifier = Modifier.size(26.dp))
        }
        Text(label, color = SpotifyDesign.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp))
    }
}
