package com.luc4n3x.levyra.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraArtistAvatar
import com.luc4n3x.levyra.ui.components.levyraArtistAccent
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.formatLibraryDuration
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanelSoft
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText

internal val LibraryRowShape = RoundedCornerShape(16.dp)
internal val LibraryArtworkShape = RoundedCornerShape(12.dp)
internal val LibraryCardShape = RoundedCornerShape(18.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LibraryTrackRow(
    track: Track,
    selected: Boolean,
    selectionActive: Boolean,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    isDownloaded: Boolean,
    downloadProgress: Int?,
    metadata: String? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavorite: () -> Unit,
    onDownload: () -> Unit,
    onQueue: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onDeleteDownload: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    var menuExpanded by remember { mutableStateOf(false) }
    Surface(
        color = when {
            selected -> LevyraCyan.copy(alpha = 0.14f)
            isCurrent -> LevyraCyan.copy(alpha = 0.08f)
            else -> Color.Transparent
        },
        shape = LibraryRowShape,
        border = if (selected) BorderStroke(1.dp, LevyraCyan.copy(alpha = 0.55f)) else null,
        modifier = modifier
            .fillMaxWidth()
            .clip(LibraryRowShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                LibraryArtwork(
                    url = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl },
                    title = track.title,
                    modifier = Modifier.size(52.dp),
                    shape = LibraryArtworkShape,
                    selected = selected
                )
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(LibraryArtworkShape)
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.GraphicEq,
                            contentDescription = strings.playing,
                            tint = LevyraCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    track.title,
                    color = if (isCurrent) LevyraCyan else LevyraText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    libraryTrackSubtitle(track, metadata),
                    color = LevyraMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (downloadProgress != null) {
                    LinearProgressIndicator(
                        progress = { downloadProgress.coerceIn(0, 100) / 100f },
                        modifier = Modifier.fillMaxWidth().padding(top = 3.dp).height(3.dp),
                        color = LevyraCyan,
                        trackColor = LevyraPanelSoft
                    )
                }
            }
            if (selectionActive) {
                Icon(
                    if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (selected) LevyraCyan else LevyraMuted.copy(alpha = 0.35f),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            } else {
                if (isDownloaded) {
                    Icon(
                        Icons.Rounded.OfflinePin,
                        contentDescription = strings.downloaded,
                        tint = LevyraCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                }
                IconButton(onClick = onFavorite) {
                    Icon(
                        if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (isFavorite) strings.removeFromFavorites else strings.addToFavorites,
                        tint = if (isFavorite) LevyraPink else LevyraMuted,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = strings.songOptions,
                            tint = LevyraMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(strings.play) },
                            leadingIcon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onClick()
                            }
                        )
                        if (onQueue != null) {
                            DropdownMenuItem(
                                text = { Text(strings.addToQueue) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onQueue()
                                }
                            )
                        }
                        if (onAddToPlaylist != null) {
                            DropdownMenuItem(
                                text = { Text(strings.addToPlaylist) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onAddToPlaylist()
                                }
                            )
                        }
                        if (onDeleteDownload != null) {
                            DropdownMenuItem(
                                text = { Text(strings.deleteDownload, color = LevyraPink) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = LevyraPink)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteDownload()
                                }
                            )
                        } else if (!isDownloaded && downloadProgress == null) {
                            DropdownMenuItem(
                                text = { Text(strings.download) },
                                leadingIcon = { Icon(Icons.Rounded.Download, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onDownload()
                                }
                            )
                        }
                        if (onRemoveFromPlaylist != null) {
                            DropdownMenuItem(
                                text = { Text(strings.removeFromPlaylist, color = LevyraPink) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = LevyraPink)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onRemoveFromPlaylist()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun libraryTrackSubtitle(track: Track, metadata: String?): String {
    val identity = listOf(track.artist, track.album).filter(String::isNotBlank).joinToString(" · ")
    val extra = metadata?.trim().orEmpty()
    return listOf(identity, extra).filter(String::isNotBlank).joinToString(" · ")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LibraryPlaylistRow(
    playlist: Playlist,
    selected: Boolean,
    selectionActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPlay: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    Surface(
        color = if (selected) LevyraCyan.copy(alpha = 0.14f) else Color.Transparent,
        shape = LibraryRowShape,
        border = if (selected) BorderStroke(1.dp, LevyraCyan.copy(alpha = 0.55f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(LibraryRowShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            LibraryArtwork(playlist.coverUrl, playlist.name, Modifier.size(56.dp), LibraryArtworkShape, selected)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    playlist.name,
                    color = LevyraText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${strings.formatTrackCount(playlist.size)} · ${strings.formatLibraryDuration(playlist.tracks.sumOf { it.durationMs })}",
                    color = LevyraMuted,
                    fontSize = 11.sp
                )
            }
            if (selectionActive) {
                Icon(
                    if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (selected) LevyraCyan else LevyraMuted.copy(alpha = 0.35f)
                )
            } else {
                IconButton(onClick = onPlay, enabled = playlist.tracks.isNotEmpty()) {
                    Icon(
                        Icons.AutoMirrored.Rounded.PlaylistPlay,
                        contentDescription = strings.play,
                        tint = if (playlist.tracks.isNotEmpty()) LevyraCyan else LevyraMuted.copy(alpha = 0.35f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LibraryAlbumRow(
    album: LibraryAlbum,
    selected: Boolean,
    selectionActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPlay: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    Surface(
        color = if (selected) LevyraCyan.copy(alpha = 0.14f) else Color.Transparent,
        shape = LibraryRowShape,
        border = if (selected) BorderStroke(1.dp, LevyraCyan.copy(alpha = 0.55f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(LibraryRowShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            LibraryArtwork(album.artworkUrl, album.title, Modifier.size(56.dp), LibraryArtworkShape, selected)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    album.title,
                    color = LevyraText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    listOf(album.artist, album.year, strings.formatTrackCount(album.tracks.size))
                        .filter(String::isNotBlank)
                        .joinToString(" · "),
                    color = LevyraMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selectionActive) {
                Icon(
                    if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (selected) LevyraCyan else LevyraMuted.copy(alpha = 0.35f)
                )
            } else {
                IconButton(onClick = onPlay) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = strings.play, tint = LevyraCyan)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LibraryArtistRow(
    artist: LibraryArtist,
    selected: Boolean,
    selectionActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPlay: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    Surface(
        color = if (selected) LevyraCyan.copy(alpha = 0.14f) else Color.Transparent,
        shape = LibraryRowShape,
        border = if (selected) BorderStroke(1.dp, LevyraCyan.copy(alpha = 0.55f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(LibraryRowShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            LibraryArtistAvatar(artist = artist, size = 56.dp, selected = selected)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    artist.name,
                    color = LevyraText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${strings.formatTrackCount(artist.tracks.size)} · ${strings.libraryTitle}",
                    color = LevyraMuted,
                    fontSize = 11.sp
                )
            }
            if (selectionActive) {
                Icon(
                    if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (selected) LevyraCyan else LevyraMuted.copy(alpha = 0.35f)
                )
            } else if (artist.tracks.isNotEmpty()) {
                IconButton(onClick = onPlay) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = strings.play, tint = LevyraCyan)
                }
            }
        }
    }
}

@Composable
internal fun LibraryPlaylistGridRow(
    playlists: List<Playlist>,
    selectedKeys: Set<String>,
    selectionActive: Boolean,
    onOpen: (Playlist) -> Unit,
    onSelect: (Playlist) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        playlists.forEach { playlist ->
            LibraryPlaylistGridCard(
                playlist = playlist,
                selected = "playlist:${playlist.id}" in selectedKeys,
                selectionActive = selectionActive,
                onClick = { onOpen(playlist) },
                onLongClick = { onSelect(playlist) },
                modifier = Modifier.weight(1f)
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
    modifier: Modifier
) {
    val strings = LocalLevyraStrings.current
    Column(modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        Box {
            LibraryArtwork(
                playlist.coverUrl,
                playlist.name,
                Modifier.fillMaxWidth().height(164.dp),
                LibraryCardShape,
                selected
            )
            if (selectionActive) {
                Icon(
                    if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (selected) LevyraCyan else Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            playlist.name,
            color = LevyraText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(strings.formatTrackCount(playlist.size), color = LevyraMuted, fontSize = 11.sp)
    }
}

@Composable
internal fun LibraryAlbumGridRows(
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
internal fun LibraryAlbumGridRow(
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
            LibraryArtwork(
                album.artworkUrl,
                album.title,
                Modifier.fillMaxWidth().height(164.dp),
                LibraryCardShape,
                selected
            )
            if (selectionActive) {
                Icon(
                    if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (selected) LevyraCyan else Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            album.title,
            color = LevyraText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            listOf(album.artist, album.year).filter(String::isNotBlank).joinToString(" · "),
            color = LevyraMuted,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun LibraryArtistGridRow(
    artists: List<LibraryArtist>,
    selectedKeys: Set<String>,
    selectionActive: Boolean,
    onOpen: (LibraryArtist) -> Unit,
    onSelect: (LibraryArtist) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        artists.forEach { artist ->
            LibraryArtistGridCard(
                artist = artist,
                selected = "artist:${artist.key}" in selectedKeys,
                selectionActive = selectionActive,
                onClick = { onOpen(artist) },
                onLongClick = { onSelect(artist) },
                modifier = Modifier.weight(1f)
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
    modifier: Modifier
) {
    val strings = LocalLevyraStrings.current
    Column(
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            LibraryArtistAvatar(artist = artist, size = 152.dp, selected = selected)
            if (selectionActive) {
                Icon(
                    if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (selected) LevyraCyan else Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            artist.name,
            color = LevyraText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(strings.formatTrackCount(artist.tracks.size), color = LevyraMuted, fontSize = 11.sp)
    }
}

@Composable
internal fun LibraryArtistAvatar(
    artist: LibraryArtist,
    size: Dp,
    selected: Boolean
) {
    val accent = levyraArtistAccent(artist.browseId.ifBlank { artist.key })
    LevyraArtistAvatar(
        name = artist.name,
        thumbnailUrl = artist.artworkUrl,
        accentStart = accent.first,
        accentEnd = accent.second,
        size = size,
        ringOverride = if (selected) LevyraCyan else null
    )
}

@Composable
internal fun LibraryArtwork(
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
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) LevyraCyan else Color.White.copy(alpha = 0.08f),
                shape
            ),
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
            Icon(
                Icons.Rounded.LibraryMusic,
                contentDescription = null,
                tint = LevyraMuted,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
