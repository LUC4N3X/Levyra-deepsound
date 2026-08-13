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
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanelSoft
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraViolet

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
    secondaryDetail: String? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavorite: () -> Unit,
    onDownload: () -> Unit
) {
    val strings = LocalLevyraStrings.current
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
                        progress = { downloadProgress.coerceIn(0, 100) / 100f },
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = LevyraCyan,
                        trackColor = LevyraPanelSoft
                    )
                }
            }
            if (selectionActive) {
                Icon(
                    if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
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
                        Icon(
                            Icons.Rounded.Download,
                            contentDescription = strings.download,
                            tint = LevyraMuted,
                            modifier = Modifier.size(21.dp)
                        )
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
        shape = RoundedCornerShape(20.dp),
        border = if (selected) BorderStroke(1.dp, LevyraCyan.copy(alpha = 0.55f)) else null,
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            LibraryArtwork(playlist.coverUrl, playlist.name, Modifier.size(66.dp), RoundedCornerShape(18.dp), selected)
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    playlist.name,
                    color = LevyraText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${strings.formatTrackCount(playlist.size)} · ${formatDuration(playlist.tracks.sumOf { it.durationMs })}",
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
        shape = RoundedCornerShape(20.dp),
        border = if (selected) BorderStroke(1.dp, LevyraCyan.copy(alpha = 0.55f)) else null,
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            LibraryArtwork(album.artworkUrl, album.title, Modifier.size(66.dp), RoundedCornerShape(18.dp), selected)
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    album.title,
                    color = LevyraText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
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
        shape = RoundedCornerShape(20.dp),
        border = if (selected) BorderStroke(1.dp, LevyraCyan.copy(alpha = 0.55f)) else null,
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            LibraryArtistAvatar(artist = artist, size = 64.dp, selected = selected)
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    artist.name,
                    color = LevyraText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
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
                RoundedCornerShape(22.dp),
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
                RoundedCornerShape(22.dp),
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
