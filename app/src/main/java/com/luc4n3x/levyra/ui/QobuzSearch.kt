package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PersonAddAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.SearchFilter
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.i18n.LevyraQobuzSkinStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.QobuzSkinCopy
import com.luc4n3x.levyra.ui.theme.QobuzDesign
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.SearchViewModel

private sealed interface QobuzSearchRow {
    val key: String
    data class Song(val track: Track) : QobuzSearchRow { override val key = "song-${track.id}" }
    data class Album(val album: AlbumHit) : QobuzSearchRow { override val key = "album-${album.browseId.ifBlank { album.title + album.artist }}" }
    data class Artist(val artist: ArtistHit) : QobuzSearchRow { override val key = "artist-${artist.browseId.ifBlank { artist.name }}" }
    data class Playlist(val playlist: PlaylistHit) : QobuzSearchRow { override val key = "playlist-${playlist.playlistId.ifBlank { playlist.browseId.ifBlank { playlist.title } }}" }
    data class Video(val track: Track) : QobuzSearchRow { override val key = "video-${track.id}" }
}

private val QobuzSearchFilters = listOf(SearchFilter.All, SearchFilter.Artists, SearchFilter.Albums, SearchFilter.Songs, SearchFilter.Playlists, SearchFilter.Videos)

@Composable
internal fun QobuzSearchEntry(viewModel: SearchViewModel, state: LevyraUiState, onTrackActions: (Track) -> Unit) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraQobuzSkinStrings.forCode(strings.code) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val query = state.query
    val data = state.searchData
    val filter = state.searchFilter
    val rows = remember(data, filter) { qobuzSearchRows(data, filter) }
    val listState = rememberLazyListState()
    val nearEnd by remember(listState) {
        derivedStateOf {
            val info = listState.layoutInfo
            info.totalItemsCount > 0 && (info.visibleItemsInfo.lastOrNull()?.index ?: 0) >= info.totalItemsCount - 4
        }
    }
    LaunchedEffect(nearEnd, filter, query) { if (nearEnd && filter != SearchFilter.All && query.isNotBlank()) viewModel.loadMoreSearchSection(filter) }
    val followed: (ArtistHit) -> Boolean = { hit ->
        (hit.browseId.isNotBlank() && hit.browseId in state.followedArtistKeys) || hit.name.trim().lowercase() in state.followedArtistKeys
    }
    Column(Modifier.fillMaxSize().background(QobuzDesign.surfacePrimary).statusBarsPadding()) {
        QobuzSearchField(query, copy, {
            viewModel.setQuery(it)
        }, {
            focusManager.clearFocus()
            keyboard?.hide()
            viewModel.searchNow(query)
        }) { viewModel.setQuery("") }
        if (query.isBlank()) {
            QobuzRecentSearches(state.recentSearches, copy, state.currentTrack?.id, levyraTabContentBottomPadding(state.currentTrack != null) + 16.dp, viewModel, onTrackActions)
            return@Column
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = QobuzDesign.gutter, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(QobuzDesign.chipGap)
        ) {
            items(QobuzSearchFilters, key = { it.name }) { option ->
                QobuzChip(qobuzFilterLabel(option, copy), filter == option, { viewModel.setSearchFilter(option) })
            }
        }
        when {
            rows.isEmpty() && state.isSearching -> Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                LevyraLoadingIndicator(color = QobuzDesign.textAccent)
            }
            rows.isEmpty() -> Text(strings.emptySearchPrompt, style = QobuzText.baseline, modifier = Modifier.padding(QobuzDesign.gutter))
            else -> LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = QobuzDesign.gutter, end = 4.dp, top = 4.dp, bottom = levyraTabContentBottomPadding(state.currentTrack != null) + 16.dp)
            ) {
                items(rows, key = { it.key }) { row ->
                    when (row) {
                        is QobuzSearchRow.Song -> QobuzSearchResultRow(
                            row.track.title, listOf(copy.trackType, row.track.artist, row.track.album), row.track.explicit, copy,
                            square = true, track = row.track, current = row.track.id == state.currentTrack?.id,
                            onClick = { viewModel.playFrom(data.songs.ifEmpty { listOf(row.track) }, row.track) }, onMore = { onTrackActions(row.track) }
                        )
                        is QobuzSearchRow.Video -> QobuzSearchResultRow(
                            row.track.title, listOf(strings.video, row.track.artist), row.track.explicit, copy,
                            square = true, track = row.track, current = row.track.id == state.currentTrack?.id,
                            onClick = { viewModel.playFrom(data.videos, row.track) }, onMore = { onTrackActions(row.track) }
                        )
                        is QobuzSearchRow.Album -> QobuzSearchResultRow(
                            row.album.title, listOf(strings.albumPlain, row.album.artist), row.album.explicit, copy,
                            square = true, imageUrl = row.album.thumbnailUrl, onClick = { viewModel.openAlbum(row.album) }
                        )
                        is QobuzSearchRow.Playlist -> QobuzSearchResultRow(
                            row.playlist.title, listOf(strings.playlistsPlain, row.playlist.author), false, copy,
                            square = true, imageUrl = row.playlist.thumbnailUrl, onClick = { viewModel.openPlaylistHit(row.playlist) }
                        )
                        is QobuzSearchRow.Artist -> QobuzSearchResultRow(
                            row.artist.name, listOf(copy.artist), false, copy, square = false, imageUrl = row.artist.thumbnailUrl,
                            onClick = { viewModel.openArtistFromHit(row.artist) },
                            trailing = {
                                val isFollowed = followed(row.artist)
                                QobuzSearchIcon(if (isFollowed) Icons.Rounded.HowToReg else Icons.Rounded.PersonAddAlt,
                                    if (isFollowed) copy.following else copy.follow) { viewModel.toggleFollowArtist(row.artist) }
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun qobuzSearchRows(data: com.luc4n3x.levyra.domain.SearchResults, filter: SearchFilter): List<QobuzSearchRow> = when (filter) {
    SearchFilter.All -> buildList {
        data.artists.take(2).forEach { add(QobuzSearchRow.Artist(it)) }
        val songs = (listOfNotNull(data.topTrack) + data.songs).distinctBy { it.id }
        val albums = data.albums
        val longest = maxOf(songs.size, albums.size)
        for (index in 0 until longest) {
            songs.getOrNull(index)?.let { add(QobuzSearchRow.Song(it)) }
            if (index % 2 == 1) albums.getOrNull(index / 2)?.let { add(QobuzSearchRow.Album(it)) }
        }
        albums.drop((longest + 1) / 2).forEach { add(QobuzSearchRow.Album(it)) }
        data.playlists.take(4).forEach { add(QobuzSearchRow.Playlist(it)) }
    }.distinctBy { it.key }
    SearchFilter.Songs -> data.songs.map { QobuzSearchRow.Song(it) }
    SearchFilter.Artists -> data.artists.map { QobuzSearchRow.Artist(it) }
    SearchFilter.Albums -> data.albums.map { QobuzSearchRow.Album(it) }
    SearchFilter.Playlists -> data.playlists.map { QobuzSearchRow.Playlist(it) }
    SearchFilter.Videos -> data.videos.map { QobuzSearchRow.Video(it) }
}.distinctBy { it.key }

@Composable
private fun qobuzFilterLabel(filter: SearchFilter, copy: QobuzSkinCopy): String {
    val strings = LocalLevyraStrings.current
    return when (filter) {
        SearchFilter.All -> copy.topResults
        SearchFilter.Songs -> strings.songsPlain
        SearchFilter.Artists -> strings.artists
        SearchFilter.Albums -> strings.albumsPlain
        SearchFilter.Playlists -> strings.playlistsPlain
        SearchFilter.Videos -> strings.video
    }
}

@Composable
private fun QobuzSearchField(query: String, copy: QobuzSkinCopy, onQuery: (String) -> Unit, onSearch: () -> Unit, onClear: () -> Unit) {
    Row(
        Modifier.padding(horizontal = QobuzDesign.gutter, vertical = 8.dp).fillMaxWidth().height(56.dp)
            .clip(RoundedCornerShape(50)).background(QobuzDesign.surfaceSecondary).padding(start = 18.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, null, tint = QobuzDesign.iconSecondary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) Text(copy.searchHint, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp), color = QobuzDesign.textSecondary, maxLines = 1)
            BasicTextField(
                value = query, onValueChange = onQuery, singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, color = QobuzDesign.textPrimary),
                cursorBrush = SolidColor(QobuzDesign.textAccent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (query.isNotEmpty()) QobuzSearchIcon(Icons.Rounded.Cancel, LocalLevyraStrings.current.clear, onClear)
    }
}

@Composable
private fun QobuzRecentSearches(recent: List<Track>, copy: QobuzSkinCopy, currentId: String?, bottomPadding: androidx.compose.ui.unit.Dp, viewModel: SearchViewModel, onTrackActions: (Track) -> Unit) {
    val strings = LocalLevyraStrings.current
    if (recent.isEmpty()) {
        Text(strings.emptySearchPrompt, style = QobuzText.baseline, modifier = Modifier.padding(QobuzDesign.gutter))
        return
    }
    Row(Modifier.fillMaxWidth().padding(start = QobuzDesign.gutter, end = 4.dp, top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(strings.recentSearches, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = QobuzDesign.textPrimary,
            modifier = Modifier.weight(1f).semantics { heading() })
        QobuzSearchIcon(Icons.Outlined.Delete, copy.clearRecent) { recent.forEach(viewModel::removeRecentSearch) }
    }
    LazyColumn(contentPadding = PaddingValues(start = QobuzDesign.gutter, end = 4.dp, bottom = bottomPadding)) {
        items(recent, key = { "recent-${it.id}" }) { track ->
            QobuzSearchResultRow(
                track.title, listOf(copy.trackType, track.artist), track.explicit, copy, square = true, track = track, current = track.id == currentId,
                onClick = { viewModel.playFrom(recent, track) },
                trailing = { QobuzSearchIcon(Icons.Rounded.Close, strings.removeFromRecentSearches) { viewModel.removeRecentSearch(track) } }
            )
        }
    }
}

@Composable
private fun QobuzSearchResultRow(
    title: String,
    meta: List<String>,
    explicit: Boolean,
    copy: QobuzSkinCopy,
    square: Boolean,
    imageUrl: String = "",
    track: Track? = null,
    current: Boolean = false,
    onClick: () -> Unit,
    onMore: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Column {
        Row(Modifier.fillMaxWidth().heightIn(min = 80.dp).clickable(role = Role.Button, onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
            val shape = if (square) QobuzDesign.artworkShape else CircleShape
            if (track != null) {
                Box(Modifier.size(QobuzDesign.listArtwork).clip(shape).background(QobuzDesign.placeholder)) { CoverImage(track, Modifier.fillMaxSize()) }
            } else {
                QobuzArtworkBox(imageUrl, Modifier.size(QobuzDesign.listArtwork), shape)
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontSize = 17.sp, fontWeight = FontWeight.Medium),
                    color = if (current) QobuzDesign.textAccent else QobuzDesign.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (explicit) {
                        QobuzExplicitBadge(copy.explicit)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(meta.filter(String::isNotBlank).distinctBy { it.lowercase() }.joinToString(" • "), style = QobuzText.cardSubtitle.copy(fontSize = 15.sp),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            trailing?.invoke()
            if (onMore != null) QobuzSearchIcon(Icons.Rounded.MoreVert, LocalLevyraStrings.current.more, onMore)
        }
        Box(Modifier.fillMaxWidth().padding(end = 12.dp).height(1.dp).background(QobuzDesign.separator))
    }
}

@Composable
private fun QobuzSearchIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = label, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = QobuzDesign.iconPrimary, modifier = Modifier.size(24.dp))
    }
}
