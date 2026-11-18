package com.luc4n3x.levyra.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraIsDefaultSkin
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraType

@Composable
internal fun ChartRow(
    rank: Int,
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onAddToQueue: () -> Unit
) {
    HomeTrackRow(
        track = track,
        isCurrent = isCurrent,
        isPlaying = isPlaying,
        isResolving = isResolving,
        onPlay = onClick,
        leadingContent = {
            if (LevyraIsDefaultSkin) DefaultChartRankBadge(rank, isCurrent) else Box(
                modifier = Modifier
                    .padding(end = 2.dp)
                    .width(34.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rank.toString(),
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else LevyraText.copy(alpha = 0.62f),
                    style = LevyraType.contentTitle.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum"
                    ),
                    maxLines = 1
                )
            }
        },
        trailingContent = {
            var expanded by remember { mutableStateOf(false) }
            Box {
                IconButton(
                    onClick = { expanded = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = LocalLevyraStrings.current.options,
                        tint = LevyraMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    DropdownMenuItem(
                        text = { Text(if (isFavorite) LocalLevyraStrings.current.removeFromFavorites else LocalLevyraStrings.current.addToFavorites) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isFavorite) LevyraPink else LocalContentColor.current
                            )
                        },
                        onClick = {
                            expanded = false
                            onFavorite()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(LocalLevyraStrings.current.addToQueue) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, null) },
                        onClick = {
                            expanded = false
                            onAddToQueue()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(LocalLevyraStrings.current.addToPlaylist) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null) },
                        onClick = {
                            expanded = false
                            onAddToPlaylist()
                        }
                    )
                }
            }
        }
    )
}
