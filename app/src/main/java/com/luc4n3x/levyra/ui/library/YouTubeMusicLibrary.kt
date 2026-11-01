package com.luc4n3x.levyra.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.playableTrackCount
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.CoverImage
import com.luc4n3x.levyra.ui.StableRemoteArtwork
import com.luc4n3x.levyra.ui.YouTubeMusicChipRow
import com.luc4n3x.levyra.ui.YouTubeMusicIconAction
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.theme.YouTubeMusicDesign

internal class YouTubeMusicLibraryRows(
    val likedTitle: String,
    val likedSubtitle: String,
    val playlistLabel: String,
    val albumLabel: String,
    val artistLabel: String,
    val moreLabel: String,
    val trackCount: (Int) -> String
)

internal class YouTubeMusicLibraryActions(
    val onOpenLiked: () -> Unit,
    val onOpenPlaylist: (Playlist) -> Unit,
    val onPlaylistMore: (Playlist) -> Unit,
    val onOpenAlbum: (LibraryAlbum) -> Unit,
    val onOpenArtist: (LibraryArtist) -> Unit
)

internal fun Modifier.youTubeMusicBleed(gutter: Dp): Modifier = layout { measurable, constraints ->
    val extra = (gutter * 2).roundToPx()
    val width = constraints.maxWidth + extra
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-gutter.roundToPx(), 0) }
}

@Composable
internal fun YouTubeMusicLibraryChips(
    categories: List<Pair<LibraryCategory, String>>,
    onSelect: (LibraryCategory) -> Unit
) {
    val labels = remember(categories) { categories.map { it.second } }
    YouTubeMusicChipRow(
        labels, selected = -1,
        onSelect = { index -> categories.getOrNull(index)?.let { onSelect(it.first) } },
        modifier = Modifier.youTubeMusicBleed(16.dp)
    )
}

@Composable
internal fun YouTubeMusicLibraryOverview(
    favorites: List<Track>,
    playlists: List<Playlist>,
    albums: List<LibraryAlbum>,
    artists: List<LibraryArtist>,
    copy: YouTubeMusicLibraryRows,
    actions: YouTubeMusicLibraryActions
) {
    Column(Modifier.youTubeMusicBleed(16.dp)) {
        YouTubeMusicLibraryRow(
            title = copy.likedTitle,
            subtitle = copy.likedSubtitle,
            pinned = true,
            onClick = actions.onOpenLiked,
            artwork = {
                Box(
                    Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF8E5CF7), Color(0xFFF162AC)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.ThumbUp, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        )
        playlists.forEach { playlist ->
            YouTubeMusicLibraryRow(
                title = playlist.name,
                subtitle = "${copy.playlistLabel} • ${copy.trackCount(playlist.playableTrackCount)}",
                onClick = { actions.onOpenPlaylist(playlist) },
                onMore = { actions.onPlaylistMore(playlist) },
                moreLabel = copy.moreLabel,
                artwork = { YouTubeMusicLibraryCover(playlist.coverUrl, playlist.tracks.firstOrNull()) }
            )
        }
        albums.forEach { album ->
            YouTubeMusicLibraryRow(
                title = album.title,
                subtitle = listOf(copy.albumLabel, album.artist, album.year).filter(String::isNotBlank).joinToString(" • "),
                onClick = { actions.onOpenAlbum(album) },
                artwork = { YouTubeMusicLibraryCover(album.artworkUrl, album.tracks.firstOrNull()) }
            )
        }
        artists.forEach { artist ->
            YouTubeMusicLibraryRow(
                title = artist.name,
                subtitle = copy.artistLabel,
                circular = true,
                onClick = { actions.onOpenArtist(artist) },
                artwork = { YouTubeMusicLibraryCover(artist.artworkUrl, artist.tracks.firstOrNull()) }
            )
        }
    }
}

@Composable
private fun YouTubeMusicLibraryCover(url: String, fallback: Track?) {
    if (url.isNotBlank()) StableRemoteArtwork(url, null, Modifier.fillMaxSize(), ContentScale.Crop)
    else if (fallback != null) CoverImage(fallback, Modifier.fillMaxSize())
}

@Composable
private fun YouTubeMusicLibraryRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    artwork: @Composable () -> Unit,
    circular: Boolean = false,
    pinned: Boolean = false,
    onMore: (() -> Unit)? = null,
    moreLabel: String = ""
) {
    Row(
        Modifier.fillMaxWidth().height(YouTubeMusicDesign.listRow)
            .levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Row)
            .padding(start = YouTubeMusicDesign.gutter),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(YouTubeMusicDesign.listArtwork)
                .clip(if (circular) CircleShape else YouTubeMusicDesign.thumbShape)
                .background(YouTubeMusicDesign.placeholder)
        ) { artwork() }
        Spacer(Modifier.width(YouTubeMusicDesign.gutter))
        Column(Modifier.weight(1f)) {
            Text(title, color = YouTubeMusicDesign.text, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (pinned) Icon(Icons.Rounded.PushPin, null, tint = YouTubeMusicDesign.textSecondary, modifier = Modifier.padding(end = 6.dp).size(16.dp))
                Text(subtitle, color = YouTubeMusicDesign.textSecondary, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (onMore != null) YouTubeMusicIconAction(Icons.Rounded.MoreVert, moreLabel, onMore) else Spacer(Modifier.width(16.dp))
    }
}

@Composable
internal fun YouTubeMusicNewButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.height(56.dp).clip(CircleShape).background(Color.White)
            .levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Control)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Add, null, tint = Color.Black, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = Color.Black, style = MaterialTheme.typography.labelLarge)
    }
}
