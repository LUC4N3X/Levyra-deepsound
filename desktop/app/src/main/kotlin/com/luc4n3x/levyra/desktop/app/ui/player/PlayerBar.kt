package com.luc4n3x.levyra.desktop.app.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.desktop.app.state.PlaybackUiState
import com.luc4n3x.levyra.desktop.app.ui.components.Artwork
import com.luc4n3x.levyra.desktop.app.ui.components.LevyraSlider
import com.luc4n3x.levyra.desktop.app.ui.components.LocalDownloadActions
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.app.ui.icons.OfflineIcons
import com.luc4n3x.levyra.desktop.app.ui.theme.ArtworkPalette
import com.luc4n3x.levyra.desktop.app.ui.theme.LocalAccentColor
import kotlin.math.roundToInt
import com.luc4n3x.levyra.desktop.app.util.Format
import com.luc4n3x.levyra.desktop.core.model.DesktopSettings
import com.luc4n3x.levyra.desktop.core.model.SleepTimerMode
import com.luc4n3x.levyra.desktop.core.model.SleepTimerState
import com.luc4n3x.levyra.desktop.core.storage.DownloadStatus
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import com.luc4n3x.levyra.desktop.player.RepeatMode

@Composable
fun PlayerBar(
    state: PlaybackUiState,
    playbackStateFlow: kotlinx.coroutines.flow.StateFlow<PlaybackUiState>,
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
    settings: DesktopSettings,
    onUpdateSettings: ((DesktopSettings) -> DesktopSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val accent = LocalAccentColor.current
    val track = state.current
    val downloadActions = LocalDownloadActions.current
    val recordFlow = remember(downloadActions, track?.id) {
        if (track != null && downloadActions != null) {
            downloadActions.stateFlow.map { data -> data.records.firstOrNull { it.id == track.id } }.distinctUntilChanged()
        } else {
            flowOf(null)
        }
    }
    val initialRecord = track?.let { currentTrack ->
        downloadActions?.stateFlow?.value?.records?.firstOrNull { it.id == currentTrack.id }
    }
    val downloadRecord by recordFlow.collectAsState(initial = initialRecord)

    val artworkUrl = track?.artworkUrl.orEmpty()
    val tint by produceState(Color.Transparent, artworkUrl) {
        value = ArtworkPalette.accentFor(artworkUrl) ?: accent
    }
    val glow by animateColorAsState(tint, tween(450))
    val chrome = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(PlayerHeight)
            .drawBehind {
                drawRect(chrome)
                drawRect(
                    Brush.horizontalGradient(
                        0f to glow.copy(alpha = 0.22f),
                        0.38f to glow.copy(alpha = 0.06f),
                        1f to Color.Transparent
                    )
                )
                drawRect(Color.White.copy(alpha = 0.06f), size = androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx()))
            }
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            val compact = maxWidth < 980.dp
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = if (compact) Modifier.weight(1f) else Modifier.width(320.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Artwork(
                        url = artworkUrl,
                        modifier = Modifier
                            .size(58.dp)
                            .clickable(enabled = track != null, onClick = onOpenNowPlaying),
                        cornerRadius = 8.dp,
                        iconSize = 20.dp
                    )
                    Column(
                        modifier = Modifier.weight(1f).clickable(enabled = track != null, onClick = onOpenNowPlaying),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
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
                        if (state.unavailableReason.isNotBlank()) {
                            Text(state.unavailableReason, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        } else if (state.streamLabel.isNotBlank()) {
                            Text(state.streamLabel, style = MaterialTheme.typography.labelSmall,
                                color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    PlayerIconButton(
                        icon = if (isFavorite) LevyraIcons.HeartFilled else LevyraIcons.Heart,
                        description = if (isFavorite) strings.removeFromFavorites else strings.addToFavorites,
                        active = isFavorite,
                        enabled = track != null,
                        onClick = onToggleFavorite
                    )
                    if (track != null && downloadActions != null && !track.isLocalFile && !track.live) {
                        val record = downloadRecord
                        IconButton(
                            onClick = {
                                when (record?.status) {
                                    DownloadStatus.QUEUED,
                                    DownloadStatus.RESOLVING,
                                    DownloadStatus.DOWNLOADING -> downloadActions.onCancel(record.id)
                                    DownloadStatus.FAILED,
                                    DownloadStatus.CANCELLED -> downloadActions.onRetry(record.id)
                                    DownloadStatus.COMPLETED -> Unit
                                    null -> downloadActions.onDownload(track)
                                }
                            },
                            enabled = record?.status != DownloadStatus.COMPLETED,
                            modifier = Modifier.size(36.dp)
                        ) {
                            when (record?.status) {
                                DownloadStatus.QUEUED,
                                DownloadStatus.RESOLVING,
                                DownloadStatus.DOWNLOADING -> CircularProgressIndicator(
                                    progress = { record.progress },
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = accent
                                )

                                DownloadStatus.COMPLETED -> Icon(OfflineIcons.Check, strings.downloadOfflineBadge,
                                    tint = accent, modifier = Modifier.size(18.dp))

                                DownloadStatus.FAILED,
                                DownloadStatus.CANCELLED -> Icon(LevyraIcons.Refresh, strings.downloadRetry,
                                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))

                                null -> Icon(OfflineIcons.Download, strings.downloadAction,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(if (compact) 1.2f else 1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PlayerIconButton(LevyraIcons.Shuffle, strings.playbackShuffle, state.queue.shuffle, onClick = onToggleShuffle)
                        PlayerIconButton(LevyraIcons.SkipPrevious, strings.playbackPrevious, false, track != null, 20.dp, onPrevious)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (track != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceContainerHighest)
                                .clickable(enabled = track != null, role = Role.Button, onClick = onPlayPause),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (state.isPlaying) LevyraIcons.Pause else LevyraIcons.Play,
                                contentDescription = if (state.isPlaying) strings.playbackPause else strings.playbackPlay,
                                tint = MaterialTheme.colorScheme.background,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        PlayerIconButton(LevyraIcons.SkipNext, strings.playbackNext, false, track != null, 20.dp, onNext)
                        PlayerIconButton(
                            if (state.queue.repeat == RepeatMode.ONE) LevyraIcons.RepeatOne else LevyraIcons.Repeat,
                            strings.playbackRepeat,
                            state.queue.repeat != RepeatMode.OFF,
                            onClick = onCycleRepeat
                        )
                    }
                    PlayerProgressBar(
                        playbackStateFlow = playbackStateFlow,
                        onSeek = onSeek,
                        trackId = track?.id,
                        modifier = Modifier.widthIn(max = 640.dp)
                    )
                }

                Row(
                    modifier = if (compact) Modifier else Modifier.width(320.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    val statusLabel = playerStatusLabel(state)
                    if (!compact && statusLabel.isNotBlank() && statusLabel != state.streamLabel) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false).padding(end = 6.dp)
                        )
                    }
                    var soundPanelOpen by remember { mutableStateOf(false) }
                    Box {
                        PlayerIconButton(LevyraIcons.Tune, strings.settingsEqualizer, settings.equalizer.enabled || soundPanelOpen, onClick = { soundPanelOpen = !soundPanelOpen })
                        if (soundPanelOpen) {
                            SoundPanel(settings = settings, onUpdate = onUpdateSettings, onDismiss = { soundPanelOpen = false })
                        }
                    }
                    PlayerIconButton(LevyraIcons.Queue, strings.queueTitle, queueVisible, onClick = onToggleQueue)
                    val muted = state.muted || state.volume == 0
                    PlayerIconButton(
                        if (muted) LevyraIcons.VolumeMuted else LevyraIcons.VolumeHigh,
                        if (muted) strings.playbackUnmute else strings.playbackMute,
                        false,
                        onClick = onToggleMute
                    )
                    LevyraSlider(
                        fraction = if (muted) 0f else state.volume / 100f,
                        onFractionChange = { onVolumeChange((it * 100f).roundToInt()) },
                        description = strings.playbackMute,
                        modifier = Modifier.width(if (compact) 64.dp else 96.dp)
                    )
                    PlaybackToolsMenu(
                        state = state,
                        onSpeedChange = onSpeedChange,
                        onSleepTimerMinutes = onSleepTimerMinutes,
                        onSleepAtEndOfTrack = onSleepAtEndOfTrack,
                        onCancelSleepTimer = onCancelSleepTimer
                    )
                    PlayerIconButton(LevyraIcons.Close, strings.playbackClose, false, size = 16.dp, onClick = onClose)
                }
            }
        }
    }
}

@Composable
private fun PlayerIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    active: Boolean,
    enabled: Boolean = true,
    size: androidx.compose.ui.unit.Dp = 17.dp,
    onClick: () -> Unit
) {
    val accent = LocalAccentColor.current
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(36.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = when {
                !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f)
                active -> accent
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
            },
            modifier = Modifier.size(size)
        )
    }
}

@Composable
private fun PlayerProgressBar(
    playbackStateFlow: kotlinx.coroutines.flow.StateFlow<PlaybackUiState>,
    onSeek: (Long) -> Unit,
    trackId: String?,
    modifier: Modifier = Modifier
) {
    val state by playbackStateFlow.collectAsState()
    var dragFraction by remember(trackId) { mutableStateOf<Float?>(null) }
    val duration = state.durationMs.coerceAtLeast(0L)
    val fraction = dragFraction
        ?: if (duration > 0L) (state.positionMs.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val position = (fraction * duration).toLong()

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = Format.duration(position),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.width(40.dp)
        )
        LevyraSlider(
            fraction = fraction,
            onFractionChange = { dragFraction = it },
            onChangeFinished = {
                dragFraction?.let { onSeek((it * duration).toLong()) }
                dragFraction = null
            },
            enabled = trackId != null && duration > 0L,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = Format.duration(duration),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(40.dp)
        )
    }
}

private val PlayerHeight = 88.dp

@Composable
private fun playerStatusLabel(state: PlaybackUiState): String {
    val strings = LocalStrings.current
    return when {
        state.sleepTimer.active && state.sleepTimer.mode == SleepTimerMode.DURATION ->
            Format.duration(state.sleepRemainingMs)
        state.sleepTimer.active && state.sleepTimer.mode == SleepTimerMode.END_OF_TRACK ->
            strings.sleepTimerEndOfTrack
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
            selected = timer.active && timer.mode == SleepTimerMode.DURATION && timer.requestedMinutes == minutes,
            onClick = { onSelect(SleepChoice.Minutes(minutes)) }
        )
    }
    MenuChoiceItem(
        label = strings.sleepTimerEndOfTrack,
        selected = timer.active && timer.mode == SleepTimerMode.END_OF_TRACK,
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
