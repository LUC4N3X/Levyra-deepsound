package com.luc4n3x.levyra.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraCardDesign
import com.luc4n3x.levyra.ui.theme.LevyraType

@Composable
internal fun LibraryPlaylistShelf(
    playlists: List<Playlist>,
    onOpen: (Playlist) -> Unit,
    onSelect: (Playlist) -> Unit,
    onPlay: (Playlist) -> Unit
) {
    val strings = LocalLevyraStrings.current
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cardWidth = (maxWidth * 0.52f).coerceAtMost(184.dp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items = playlists, key = { it.id }, contentType = { "library-playlist-artwork" }) { playlist ->
                Column(
                    modifier = Modifier.width(cardWidth)
                        .levyraPressable(
                            onClick = { onOpen(playlist) },
                            onLongClick = { onSelect(playlist) },
                            role = Role.Button,
                            pressedScale = LevyraPressScale.Tile
                        ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box {
                        LibraryArtwork(playlist.coverUrl, playlist.name, Modifier.fillMaxWidth().aspectRatio(1f), LevyraCardDesign.ArtworkShape, selected = false)
                        IconButton(
                            onClick = { onPlay(playlist) },
                            enabled = playlist.tracks.isNotEmpty(),
                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).size(48.dp)
                                .clip(CircleShape).background(colors.surfaceContainerHigh)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "${strings.play}, ${playlist.name}", tint = if (playlist.tracks.isNotEmpty()) colors.onSurface else colors.onSurfaceVariant)
                        }
                    }
                    Text(playlist.name, style = LevyraType.cardTitle, color = colors.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(strings.formatTrackCount(playlist.size), style = LevyraType.caption, color = colors.onSurfaceVariant)
                }
            }
        }
    }
}
