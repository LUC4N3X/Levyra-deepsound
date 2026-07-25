package com.luc4n3x.levyra.desktop.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.desktop.app.state.PlaybackUiState
import com.luc4n3x.levyra.desktop.app.ui.components.Artwork
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.app.util.Format
import com.luc4n3x.levyra.desktop.player.RepeatMode

@Composable
fun PlayerBar(
    state: PlaybackUiState,
    isFavorite: Boolean,
    queueVisible: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Int) -> Unit,
    onToggleMute: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val track = state.current
    var dragPosition by remember { mutableStateOf<Float?>(null) }

    val duration = state.durationMs.coerceAtLeast(0L)
    val position = dragPosition?.toLong() ?: state.positionMs.coerceIn(0L, if (duration > 0L) duration else Long.MAX_VALUE)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.width(280.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Artwork(
                    url = track?.artworkUrl.orEmpty(),
                    modifier = Modifier.size(52.dp).clickable(onClick = onOpenNowPlaying),
                    cornerRadius = 10.dp,
                    iconSize = 20.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track?.title ?: strings.nowPlayingEmpty,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track?.displaySubtitle.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (track != null) {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (isFavorite) LevyraIcons.HeartFilled else LevyraIcons.Heart,
                            contentDescription = null,
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(onClick = onToggleShuffle) {
                        Icon(
                            imageVector = LevyraIcons.Shuffle,
                            contentDescription = null,
                            tint = if (state.queue.shuffle) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onPrevious, enabled = track != null) {
                        Icon(
                            imageVector = LevyraIcons.SkipPrevious,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(onClick = onPlayPause, enabled = track != null) {
                        Icon(
                            imageVector = if (state.isPlaying) LevyraIcons.Pause else LevyraIcons.Play,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    IconButton(onClick = onNext, enabled = track != null) {
                        Icon(
                            imageVector = LevyraIcons.SkipNext,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(onClick = onCycleRepeat) {
                        Icon(
                            imageVector = if (state.queue.repeat == RepeatMode.ONE) {
                                LevyraIcons.RepeatOne
                            } else {
                                LevyraIcons.Repeat
                            },
                            contentDescription = null,
                            tint = if (state.queue.repeat == RepeatMode.OFF) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = Format.duration(position),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = if (duration > 0L) position.toFloat() else 0f,
                        onValueChange = { value -> dragPosition = value },
                        onValueChangeFinished = {
                            dragPosition?.let { value -> onSeek(value.toLong()) }
                            dragPosition = null
                        },
                        valueRange = 0f..(if (duration > 0L) duration.toFloat() else 1f),
                        enabled = track != null && duration > 0L,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = Format.duration(duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.width(240.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                if (state.streamLabel.isNotBlank()) {
                    Text(
                        text = state.streamLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                IconButton(onClick = onToggleMute) {
                    Icon(
                        imageVector = if (state.muted || state.volume == 0) {
                            LevyraIcons.VolumeMuted
                        } else {
                            LevyraIcons.VolumeHigh
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Slider(
                    value = state.volume.toFloat(),
                    onValueChange = { value -> onVolumeChange(value.toInt()) },
                    valueRange = 0f..100f,
                    modifier = Modifier.width(96.dp)
                )
                IconButton(onClick = onToggleQueue) {
                    Icon(
                        imageVector = LevyraIcons.Queue,
                        contentDescription = strings.queueTitle,
                        tint = if (queueVisible) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (state.unavailableReason.isNotBlank()) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Text(
                    text = state.unavailableReason,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
