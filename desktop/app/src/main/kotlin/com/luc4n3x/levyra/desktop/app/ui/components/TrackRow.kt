package com.luc4n3x.levyra.desktop.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.app.util.Format
import com.luc4n3x.levyra.desktop.core.model.Track

data class TrackRowAction(val label: String, val onClick: () -> Unit)

@Composable
fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onEnqueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
    extraAction: TrackRowAction? = null,
    position: Int? = null
) {
    val strings = LocalStrings.current
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    var menuExpanded by remember { mutableStateOf(false) }

    val background = when {
        isCurrent -> MaterialTheme.colorScheme.primaryContainer
        hovered -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .hoverable(interactionSource)
            .clickable(onClick = onPlay)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (position != null) {
            Text(
                text = position.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(24.dp)
            )
        }

        Artwork(url = track.artworkUrl, modifier = Modifier.size(44.dp), cornerRadius = 8.dp, iconSize = 18.dp)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (track.displaySubtitle.isNotBlank()) {
                Text(
                    text = track.displaySubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Text(
            text = Format.duration(track.durationMs),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (isFavorite) LevyraIcons.HeartFilled else LevyraIcons.Heart,
                contentDescription = if (isFavorite) strings.removeFromFavorites else strings.addToFavorites,
                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = LevyraIcons.More,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(strings.playNext) },
                    onClick = {
                        menuExpanded = false
                        onPlayNext()
                    }
                )
                DropdownMenuItem(
                    text = { Text(strings.addToQueue) },
                    onClick = {
                        menuExpanded = false
                        onEnqueue()
                    }
                )
                DropdownMenuItem(
                    text = { Text(strings.addToPlaylist) },
                    onClick = {
                        menuExpanded = false
                        onAddToPlaylist()
                    }
                )
                if (extraAction != null) {
                    DropdownMenuItem(
                        text = { Text(extraAction.label) },
                        onClick = {
                            menuExpanded = false
                            extraAction.onClick()
                        }
                    )
                }
            }
        }
    }
}
