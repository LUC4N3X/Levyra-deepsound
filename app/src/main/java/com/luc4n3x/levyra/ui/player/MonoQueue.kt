package com.luc4n3x.levyra.ui.player

import com.luc4n3x.levyra.ui.components.LevyraMediaTitle
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.CoverImage
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.MonoOverline
import com.luc4n3x.levyra.ui.QueueEntry
import com.luc4n3x.levyra.ui.buildQueueEntries
import com.luc4n3x.levyra.ui.drawMonoWave
import com.luc4n3x.levyra.ui.i18n.LevyraMonoPlayerStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.MonoPlayerCopy
import com.luc4n3x.levyra.ui.monoPanel
import com.luc4n3x.levyra.ui.reorderQueueEntries
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import com.luc4n3x.levyra.ui.theme.MonoDesign
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import kotlin.math.PI

private const val MonoQueuePlayedAlpha = 0.5f

@Composable
internal fun MonoQueueScreen(
    state: LevyraUiState,
    onPlay: (Track) -> Unit,
    onToggleRadio: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onTrackActions: (Track) -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraMonoPlayerStrings.forCode(strings.code) }
    var history by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = history) { history = false }
    var queueEntries by remember(state.activeQueueSpaceId) { mutableStateOf(buildQueueEntries(state.queue)) }
    val entries = remember(state.queue, queueEntries) {
        if (queueEntries.size == state.queue.size && queueEntries.map { it.track } == state.queue) {
            queueEntries
        } else {
            buildQueueEntries(state.queue, queueEntries).also { queueEntries = it }
        }
    }
    val moveEntry: (Int, Int) -> Unit = { from, to ->
        queueEntries = reorderQueueEntries(entries, from, to)
        onMove(from, to)
    }
    val current = state.currentTrack
    val currentIndex = state.queueCurrentIndex.takeIf { it in entries.indices && entries[it].track.id == current?.id }
        ?: entries.indexOfFirst { it.track.id == current?.id }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = if (currentIndex > 0) currentIndex + 1 else 0)
    Column(Modifier.fillMaxSize().background(MonoDesign.base).statusBarsPadding().navigationBarsPadding()) {
        MonoQueueTopBar(if (history) strings.listeningHistory else strings.queue, history, { history = !history }, onClose)
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 120.dp)) {
            if (history) {
                monoQueueHistory(state.recentListens, copy, onPlay, onTrackActions)
            } else {
                monoQueueSections(
                    entries, current, currentIndex, state.isPlaying, state.radioEnabled, state.animationsEnabled, copy,
                    onPlay, onToggleRadio, moveEntry, onTrackActions
                )
            }
        }
    }
}

private fun LazyListScope.monoQueueHistory(
    recent: List<Track>,
    copy: MonoPlayerCopy,
    onPlay: (Track) -> Unit,
    onTrackActions: (Track) -> Unit
) {
    if (recent.isEmpty()) {
        item(key = "mono-history-empty") { MonoQueueNotice(LocalLevyraStrings.current.listeningHistoryEmptyTitle) }
        return
    }
    itemsIndexed(recent, key = { index, track -> "mono-history-$index-${track.id}" }) { _, track ->
        MonoQueueRow(track, played = false, copy, { onPlay(track) }, { onTrackActions(track) })
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.monoQueueSections(
    entries: List<QueueEntry>,
    current: Track?,
    currentIndex: Int,
    playing: Boolean,
    radioEnabled: Boolean,
    animations: Boolean,
    copy: MonoPlayerCopy,
    onPlay: (Track) -> Unit,
    onToggleRadio: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onTrackActions: (Track) -> Unit
) {
    if (entries.isEmpty() && current == null) {
        item(key = "mono-queue-empty") { MonoQueueNotice(LocalLevyraStrings.current.queueEmpty) }
    }
    val previous = if (currentIndex > 0) entries.take(currentIndex) else emptyList()
    val upcoming = if (currentIndex >= 0) entries.drop(currentIndex + 1) else entries
    val upcomingStart = if (currentIndex >= 0) currentIndex + 1 else 0
    if (previous.isNotEmpty()) {
        item(key = "mono-queue-previous-title") { MonoQueueLabel(LocalLevyraStrings.current.previous) }
        itemsIndexed(previous, key = { _, entry -> "mono-queue-${entry.key}" }) { index, entry ->
            MonoQueueRow(
                entry.track, played = true, copy, { onPlay(entry.track) }, { onTrackActions(entry.track) },
                index, entries.size, onMove,
                if (animations) Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null, placementSpec = LevyraMotion.reorder.spec(visibilityThreshold = IntOffset.VisibilityThreshold)) else Modifier
            )
        }
    }
    if (current != null) {
        item(key = "mono-queue-current") { MonoNowPlayingCard(current, playing) { onTrackActions(current) } }
    }
    item(key = "mono-queue-radio") { MonoQueueRadioRow(radioEnabled, onToggleRadio) }
    if (upcoming.isNotEmpty()) {
        item(key = "mono-queue-upnext-title") { MonoQueueLabel(copy.upNext, upcoming.size.toString()) }
        itemsIndexed(upcoming, key = { _, entry -> "mono-queue-${entry.key}" }) { index, entry ->
            MonoQueueRow(
                entry.track, played = false, copy, { onPlay(entry.track) }, { onTrackActions(entry.track) },
                upcomingStart + index, entries.size, onMove,
                if (animations) Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null, placementSpec = LevyraMotion.reorder.spec(visibilityThreshold = IntOffset.VisibilityThreshold)) else Modifier
            )
        }
    }
}

@Composable
private fun MonoIconTarget(icon: ImageVector, label: String, tint: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun MonoQueueTopBar(title: String, history: Boolean, onToggleHistory: () -> Unit, onClose: () -> Unit) {
    val strings = LocalLevyraStrings.current
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        MonoIconTarget(Icons.Rounded.KeyboardArrowDown, strings.close, MonoDesign.text, onClose)
        Text(
            title, style = MaterialTheme.typography.headlineMedium, color = MonoDesign.text,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp).semantics { heading() }
        )
        Box(
            Modifier.size(48.dp).clip(CircleShape)
                .background(if (history) MonoDesign.accent else androidx.compose.ui.graphics.Color.Transparent)
                .toggleable(value = history, role = Role.Switch, onValueChange = { onToggleHistory() })
                .semantics { contentDescription = strings.listeningHistory },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.History, null, tint = if (history) MonoDesign.onAccent else MonoDesign.textSecondary, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun MonoQueueLabel(text: String, count: String? = null) {
    Row(
        Modifier.fillMaxWidth().padding(start = MonoDesign.gutter, end = MonoDesign.gutter, top = 24.dp, bottom = 8.dp).semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        MonoOverline(text, Modifier.weight(1f))
        if (count != null) MonoOverline(count, color = MonoDesign.textFaint)
    }
}

@Composable
private fun MonoQueueNotice(text: String) {
    Text(
        text, style = MaterialTheme.typography.bodyLarge, color = MonoDesign.textSecondary,
        modifier = Modifier.fillMaxWidth().padding(horizontal = MonoDesign.gutter, vertical = 32.dp)
    )
}

@Composable
private fun MonoNowPlayingCard(track: Track, playing: Boolean, onActions: () -> Unit) {
    val strings = LocalLevyraStrings.current
    val animated = LocalAnimationsEnabled.current
    val clock = rememberMonoClock(playing && animated)
    val wavePath = remember { androidx.compose.ui.graphics.Path() }
    val amplitude = animateFloatAsState(if (playing) 1f else 0.08f, if (animated) tween(520) else snap(), label = "mono-queue-wave")
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 8.dp).monoPanel(MonoDesign.panelShape, MonoDesign.raised)
            .padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 14.dp)
    ) {
        MonoOverline(strings.nowPlaying, Modifier.padding(start = 2.dp, bottom = 10.dp), color = MonoDesign.accent)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(MonoDesign.artShape).border(1.dp, MonoDesign.hairline, MonoDesign.artShape)) {
                CoverImage(track, Modifier.fillMaxSize())
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                LevyraMediaTitle(track.title, explicit = track.explicit,  style = MaterialTheme.typography.titleMedium, color = MonoDesign.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MonoDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            MonoIconTarget(Icons.Rounded.MoreVert, strings.more, MonoDesign.textSecondary, onActions)
        }
        Canvas(Modifier.fillMaxWidth().padding(top = 10.dp, end = 12.dp).height(22.dp)) {
            drawMonoWave(MonoDesign.text, clock.floatValue * 2f * PI.toFloat() / 2.4f, amplitude.value, cycles = 7f, strokeWidth = 1.6.dp, path = wavePath)
        }
    }
}

@Composable
private fun MonoQueueRadioRow(enabled: Boolean, onToggle: () -> Unit) {
    val strings = LocalLevyraStrings.current
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 56.dp)
            .toggleable(value = enabled, role = Role.Switch, onValueChange = { onToggle() })
            .padding(horizontal = MonoDesign.gutter, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(strings.continuousRadio, style = MaterialTheme.typography.titleSmall, color = MonoDesign.text, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = enabled,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MonoDesign.onAccent,
                checkedTrackColor = MonoDesign.accent,
                checkedBorderColor = MonoDesign.accent,
                uncheckedThumbColor = MonoDesign.textSecondary,
                uncheckedTrackColor = MonoDesign.surface,
                uncheckedBorderColor = MonoDesign.hairlineStrong
            )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MonoQueueRow(
    track: Track,
    played: Boolean,
    copy: MonoPlayerCopy,
    onClick: () -> Unit,
    onActions: () -> Unit,
    queueIndex: Int = -1,
    queueSize: Int = 0,
    onMove: ((Int, Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    val haptics = LocalLevyraHaptics.current
    val reorderable = onMove != null && queueIndex >= 0
    val latestIndex by rememberUpdatedState(queueIndex)
    val latestSize by rememberUpdatedState(queueSize)
    val latestMove by rememberUpdatedState(onMove)
    var drag by remember { mutableFloatStateOf(0f) }
    var rowHeight by remember { mutableIntStateOf(1) }
    Row(
        modifier.fillMaxWidth()
            .onSizeChanged { rowHeight = it.height.coerceAtLeast(1) }
            .graphicsLayer { translationY = drag }
            .background(if (drag != 0f) MonoDesign.raised else MonoDesign.base)
            .semantics {
                if (reorderable) customActions = monoQueueMoveActions(queueIndex, queueSize, copy, onMove)
            }
            .combinedClickable(onLongClickLabel = strings.more, onLongClick = onActions, onClick = onClick)
            .heightIn(min = MonoDesign.rowHeight)
            .padding(start = MonoDesign.gutter, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.weight(1f).alpha(if (played) MonoQueuePlayedAlpha else 1f), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(MonoDesign.rowArtwork).clip(MonoDesign.artShape).border(1.dp, MonoDesign.hairline, MonoDesign.artShape)) {
                CoverImage(track, Modifier.fillMaxSize())
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                LevyraMediaTitle(track.title,  style = MaterialTheme.typography.titleSmall, color = MonoDesign.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (track.explicit) {
                        Box(
                            Modifier.border(1.dp, MonoDesign.hairlineStrong, MonoDesign.artShape).padding(horizontal = 4.dp)
                                .semantics { contentDescription = copy.explicit },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(copy.explicit.take(1).uppercase(), style = MaterialTheme.typography.labelSmall, color = MonoDesign.textSecondary, maxLines = 1)
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MonoDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        MonoIconTarget(Icons.Rounded.MoreVert, strings.more, MonoDesign.textSecondary, onActions)
        if (reorderable) {
            Box(
                Modifier.size(48.dp).pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = { drag = 0f },
                        onDragCancel = { drag = 0f },
                        onVerticalDrag = { change, amount ->
                            change.consume()
                            drag += amount
                            val index = latestIndex
                            val target = monoQueueDragTarget(drag, rowHeight.toFloat(), index, latestSize)
                            if (target != index) {
                                latestMove?.invoke(index, target)
                                drag = 0f
                                haptics.perform(LevyraHapticAction.Reorder)
                            }
                        }
                    )
                }.semantics { contentDescription = strings.dragToReorder },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.DragHandle, null, tint = MonoDesign.textFaint, modifier = Modifier.size(22.dp))
            }
        }
    }
}

private fun monoQueueMoveActions(
    queueIndex: Int,
    queueSize: Int,
    copy: MonoPlayerCopy,
    onMove: ((Int, Int) -> Unit)?
): List<CustomAccessibilityAction> = buildList {
    if (queueIndex > 0) add(CustomAccessibilityAction(copy.moveUp) { onMove?.invoke(queueIndex, queueIndex - 1); true })
    if (queueIndex < queueSize - 1) add(CustomAccessibilityAction(copy.moveDown) { onMove?.invoke(queueIndex, queueIndex + 1); true })
}

private fun monoQueueDragTarget(drag: Float, threshold: Float, index: Int, size: Int): Int = when {
    drag > threshold && index < size - 1 -> index + 1
    drag < -threshold && index > 0 -> index - 1
    else -> index
}
