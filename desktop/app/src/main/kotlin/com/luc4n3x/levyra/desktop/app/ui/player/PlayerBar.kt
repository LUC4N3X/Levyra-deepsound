package com.luc4n3x.levyra.desktop.app.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.desktop.app.state.PlaybackUiState
import com.luc4n3x.levyra.desktop.app.ui.components.Artwork
import com.luc4n3x.levyra.desktop.app.ui.components.LocalDownloadActions
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.app.ui.icons.OfflineIcons
import com.luc4n3x.levyra.desktop.app.ui.theme.LocalAccentColor
import com.luc4n3x.levyra.desktop.app.util.Format
import com.luc4n3x.levyra.desktop.core.model.DesktopSettings
import com.luc4n3x.levyra.desktop.core.model.SleepTimerMode
import com.luc4n3x.levyra.desktop.core.model.SleepTimerState
import com.luc4n3x.levyra.desktop.core.storage.DownloadStatus
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
    onSpeedChange: (Float) -> Unit,
    onSleepTimerMinutes: (Int) -> Unit,
    onSleepAtEndOfTrack: () -> Unit,
    onCancelSleepTimer: () -> Unit,
    onClose: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val accent = LocalAccentColor.current
    val track = state.current
    val downloadActions = LocalDownloadActions.current
    val downloadRecord = track?.let { downloadActions?.recordFor?.invoke(it) }
    var dragPosition by remember(track?.id) { mutableStateOf<Float?>(null) }

    val duration = state.durationMs.coerceAtLeast(0L)
    val position = dragPosition?.toLong()
        ?: state.positionMs.coerceIn(0L, if (duration > 0L) duration else Long.MAX_VALUE)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.width(315.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Artwork(
                        url = track?.artworkUrl.orEmpty(),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .clickable(enabled = track != null, onClick = onOpenNowPlaying),
                        cornerRadius = 9.dp,
                        iconSize = 20.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track?.title.orEmpty(),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
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
                    IconButton(onClick = onToggleFavorite, enabled = track != null) {
                        Icon(
                            imageVector = if (isFavorite) LevyraIcons.HeartFilled else LevyraIcons.Heart,
                            contentDescription = if (isFavorite) strings.removeFromFavorites else strings.addToFavorites,
                            tint = if (isFavorite) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (track != null && downloadActions != null) {
                        IconButton(
                            onClick = {
                                when (downloadRecord?.status) {
                                    DownloadStatus.QUEUED,
                                    DownloadStatus.RESOLVING,
                                    DownloadStatus.DOWNLOADING -> downloadActions.onCancel(downloadRecord.id)
                                    DownloadStatus.FAILED,
                                    DownloadStatus.CANCELLED -> downloadActions.onRetry(downloadRecord.id)
                                    DownloadStatus.COMPLETED -> Unit
                                    null -> downloadActions.onDownload(track)
                                }
                            },
                            enabled = downloadRecord?.status != DownloadStatus.COMPLETED
                        ) {
                            when (downloadRecord?.status) {
                                DownloadStatus.QUEUED,
                                DownloadStatus.RESOLVING,
                                DownloadStatus.DOWNLOADING -> CircularProgressIndicator(
                                    progress = { downloadRecord.progress },
                                    modifier = Modifier.size(17.dp),
                                    strokeWidth = 2.dp
                                )

                                DownloadStatus.COMPLETED -> Icon(
                                    imageVector = OfflineIcons.Check,
                                    contentDescription = strings.downloadOfflineBadge,
                                    tint = accent,
                                    modifier = Modifier.size(18.dp)
                                )

                                DownloadStatus.FAILED,
                                DownloadStatus.CANCELLED -> Icon(
                                    imageVector = LevyraIcons.Refresh,
                                    contentDescription = strings.downloadRetry,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )

                                null -> Icon(
                                    imageVector = OfflineIcons.Download,
                                    contentDescription = strings.downloadAction,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        PlayerIconButton(
                            icon = LevyraIcons.Shuffle,
                            description = strings.playbackShuffle,
                            active = state.queue.shuffle,
                            onClick = onToggleShuffle
                        )
                        IconButton(onClick = onPrevious, enabled = track != null) {
                            Icon(
                                imageVector = LevyraIcons.SkipPrevious,
                                contentDescription = strings.playbackPrevious,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Surface(
                            modifier = Modifier
                                .size(42.dp)
                                .clickable(enabled = track != null, onClick = onPlayPause),
                            shape = CircleShape,
                            color = if (track != null) accent else MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = if (track != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (state.isPlaying) LevyraIcons.Pause else LevyraIcons.Play,
                                    contentDescription = if (state.isPlaying) strings.playbackPause else strings.playbackPlay,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        }
                        IconButton(onClick = onNext, enabled = track != null) {
                            Icon(
                                imageVector = LevyraIcons.SkipNext,
                                contentDescription = strings.playbackNext,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        PlayerIconButton(
                            icon = if (state.queue.repeat == RepeatMode.ONE) LevyraIcons.RepeatOne else LevyraIcons.Repeat,
                            description = strings.playbackRepeat,
                            active = state.queue.repeat != RepeatMode.OFF,
                            onClick = onCycleRepeat
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = Format.duration(position),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = if (duration > 0L) position.toFloat() else 0f,
                            onValueChange = { dragPosition = it },
                            onValueChangeFinished = {
                                dragPosition?.let { onSeek(it.toLong()) }
                                dragPosition = null
                            },
                            valueRange = 0f..if (duration > 0L) duration.toFloat() else 1f,
                            enabled = track != null && duration > 0L,
                            colors = SliderDefaults.colors(
                                thumbColor = accent,
                                activeTrackColor = accent,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            ),
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
                    modifier = Modifier.width(285.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    val statusLabel = playerStatusLabel(state)
                    if (statusLabel.isNotBlank()) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.sleepTimer.active || state.speed != DesktopSettings.DEFAULT_SPEED) {
                                accent
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                    val muted = state.muted || state.volume == 0
                    IconButton(onClick = onToggleMute) {
                        Icon(
                            imageVector = if (muted) LevyraIcons.VolumeMuted else LevyraIcons.VolumeHigh,
                            contentDescription = if (muted) strings.playbackUnmute else strings.playbackMute,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Slider(
                        value = state.volume.toFloat(),
                        onValueChange = { onVolumeChange(it.toInt()) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.onSurface,
                            activeTrackColor = MaterialTheme.colorScheme.onSurface,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                        modifier = Modifier.width(82.dp)
                    )
                    PlayerIconButton(
                        icon = LevyraIcons.Queue,
                        description = strings.queueTitle,
                        active = queueVisible,
                        onClick = onToggleQueue
                    )
                    PlaybackToolsMenu(
                        state = state,
                        onSpeedChange = onSpeedChange,
                        onSleepTimerMinutes = onSleepTimerMinutes,
                        onSleepAtEndOfTrack = onSleepAtEndOfTrack,
                        onCancelSleepTimer = onCancelSleepTimer
                    )
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = LevyraIcons.Close,
                            contentDescription = strings.playbackClose,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            if (state.unavailableReason.isNotBlank()) {
                Text(
                    text = state.unavailableReason,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PlayerIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val accent = LocalAccentColor.current
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (active) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun playerStatusLabel(state: PlaybackUiState): String {
    val strings = LocalStrings.current
    return when {
        state.sleepTimer.mode == SleepTimerMode.DURATION -> Format.duration(state.sleepRemainingMs)
        state.sleepTimer.mode == SleepTimerMode.END_OF_TRACK -> strings.sleepTimerEndOfTrack
        state.speed != DesktopSettings.DEFAULT_SPEED -> Format.speed(state.speed)
        state.streamLabel.isNotBlank() -> state.streamLabel
        else -> ""
    }
}

@Composable
private fun PlaybackToolsMenu(
    state: PlaybackUiState,
    onSpeedChange: (Float) -> Unit,
    onSleepTimerMinutes: (Int) -> Unit,
    onSleepAtEndOfTrack: () -> Unit,
    onCancelSleepTimer: () -> Unit
) {
    val strings = LocalStrings.current
    val accent = LocalAccentColor.current
    var expanded by remember { mutableStateOf(false) }
    val highlighted = state.sleepTimer.active || state.speed != DesktopSettings.DEFAULT_SPEED

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = LevyraIcons.More,
                contentDescription = strings.settingsPlayback,
                tint = if (highlighted) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SpeedMenuSection(
                selected = state.speed,
                onSelect = { speed ->
                    expanded = false
                    onSpeedChange(speed)
                }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            SleepTimerMenuSection(
                timer = state.sleepTimer,
                onSelect = { choice ->
                    expanded = false
                    when (choice) {
                        is SleepChoice.Off -> onCancelSleepTimer()
                        is SleepChoice.EndOfTrack -> onSleepAtEndOfTrack()
                        is SleepChoice.Minutes -> onSleepTimerMinutes(choice.value)
                    }
                }
            )
        }
    }
}

@Composable
private fun SpeedMenuSection(selected: Float, onSelect: (Float) -> Unit) {
    MenuSectionLabel(text = LocalStrings.current.settingsSpeed)
    DesktopSettings.SPEED_STEPS.forEach { step ->
        MenuChoiceItem(
            label = Format.speed(step),
            selected = selected == step,
            onClick = { onSelect(step) }
        )
    }
}

@Composable
private fun SleepTimerMenuSection(timer: SleepTimerState, onSelect: (SleepChoice) -> Unit) {
    val strings = LocalStrings.current
    MenuSectionLabel(text = strings.settingsSleepTimer)
    MenuChoiceItem(
        label = strings.sleepTimerOff,
        selected = !timer.active,
        onClick = { onSelect(SleepChoice.Off) }
    )
    SleepTimerState.PRESET_MINUTES.forEach { minutes ->
        MenuChoiceItem(
            label = "$minutes min",
            selected = timer.mode == SleepTimerMode.DURATION && timer.requestedMinutes == minutes,
            onClick = { onSelect(SleepChoice.Minutes(minutes)) }
        )
    }
    MenuChoiceItem(
        label = strings.sleepTimerEndOfTrack,
        selected = timer.mode == SleepTimerMode.END_OF_TRACK,
        onClick = { onSelect(SleepChoice.EndOfTrack) }
    )
}

@Composable
private fun MenuChoiceItem(label: String, selected: Boolean, onClick: () -> Unit) {
    val accent = LocalAccentColor.current
    DropdownMenuItem(
        text = { Text(label) },
        onClick = onClick,
        trailingIcon = {
            if (selected) {
                Icon(
                    imageVector = OfflineIcons.Check,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    )
}

@Composable
private fun MenuSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
    )
}

private sealed interface SleepChoice {
    data object Off : SleepChoice
    data object EndOfTrack : SleepChoice
    data class Minutes(val value: Int) : SleepChoice
}
