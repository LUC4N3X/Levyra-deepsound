package com.luc4n3x.levyra.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.HorizontalDivider
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
import com.luc4n3x.levyra.ui.AtelierIconTarget
import com.luc4n3x.levyra.ui.CoverImage
import com.luc4n3x.levyra.ui.QueueEntry
import com.luc4n3x.levyra.ui.atelierUppercaseLabel
import com.luc4n3x.levyra.ui.buildQueueEntries
import com.luc4n3x.levyra.ui.i18n.AtelierCopy
import com.luc4n3x.levyra.ui.i18n.LevyraAtelierStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.reorderQueueEntries
import com.luc4n3x.levyra.ui.theme.AtelierDesign
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import com.luc4n3x.levyra.viewmodel.LevyraUiState

private const val AtelierQueuePlayedAlpha = 0.6f

@Composable
internal fun AtelierQueueScreen(
    state: LevyraUiState,
    onPlay: (Track) -> Unit,
    onToggleRadio: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onTrackActions: (Track) -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraAtelierStrings.forCode(strings.code) }
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
    Column(Modifier.fillMaxSize().background(AtelierDesign.room).statusBarsPadding().navigationBarsPadding()) {
        AtelierQueueTopBar(if (history) strings.listeningHistory else strings.queue, strings.listeningHistory, history, { history = !history }, onClose)
        HorizontalDivider(thickness = 1.dp, color = AtelierDesign.hairline)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 120.dp)) {
            if (history) {
                atelierQueueHistory(state.recentListens, copy, onPlay, onTrackActions)
            } else {
                atelierQueueSections(
                    entries, current, currentIndex, state.radioEnabled, state.animationsEnabled, copy,
                    onPlay, onToggleRadio, moveEntry, onTrackActions
                )
            }
        }
    }
}

private fun LazyListScope.atelierQueueHistory(
    recent: List<Track>,
    copy: AtelierCopy,
    onPlay: (Track) -> Unit,
    onTrackActions: (Track) -> Unit
) {
    if (recent.isEmpty()) {
        item(key = "atelier-history-empty") { AtelierQueueNotice(copy.queueHistoryEmpty) }
        return
    }
    itemsIndexed(recent, key = { index, track -> "atelier-history-$index-${track.id}" }) { _, track ->
        AtelierQueueRow(track, played = false, copy, { onPlay(track) }, { onTrackActions(track) })
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.atelierQueueSections(
    entries: List<QueueEntry>,
    current: Track?,
    currentIndex: Int,
    radioEnabled: Boolean,
    animations: Boolean,
    copy: AtelierCopy,
    onPlay: (Track) -> Unit,
    onToggleRadio: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onTrackActions: (Track) -> Unit
) {
    if (entries.isEmpty() && current == null) {
        item(key = "atelier-queue-empty") {
            AtelierQueueNotice(LocalLevyraStrings.current.queueEmpty)
        }
    }
    val previous = if (currentIndex > 0) entries.take(currentIndex) else emptyList()
    val upcoming = if (currentIndex >= 0) entries.drop(currentIndex + 1) else entries
    val upcomingStart = if (currentIndex >= 0) currentIndex + 1 else 0
    if (previous.isNotEmpty()) {
        item(key = "atelier-queue-previous-title") { AtelierQueueLabel(LocalLevyraStrings.current.previous) }
        itemsIndexed(previous, key = { _, entry -> "atelier-queue-${entry.key}" }) { index, entry ->
            AtelierQueueRow(
                entry.track, played = true, copy, { onPlay(entry.track) }, { onTrackActions(entry.track) },
                index, entries.size, onMove,
                if (animations) Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null, placementSpec = LevyraMotion.reorder.spec(visibilityThreshold = IntOffset.VisibilityThreshold)) else Modifier
            )
        }
    }
    if (current != null) {
        item(key = "atelier-queue-current-title") { AtelierQueueLabel(LocalLevyraStrings.current.nowPlaying) }
        item(key = "atelier-queue-current") {
            AtelierQueueRow(current, played = false, copy, null, { onTrackActions(current) }, highlighted = true)
        }
    }
    item(key = "atelier-queue-radio") { AtelierQueueRadioRow(radioEnabled, onToggleRadio) }
    if (upcoming.isNotEmpty()) {
        item(key = "atelier-queue-upnext-title") { AtelierQueueLabel(copy.queueUpNext) }
        itemsIndexed(upcoming, key = { _, entry -> "atelier-queue-${entry.key}" }) { index, entry ->
            AtelierQueueRow(
                entry.track, played = false, copy, { onPlay(entry.track) }, { onTrackActions(entry.track) },
                upcomingStart + index, entries.size, onMove,
                if (animations) Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null, placementSpec = LevyraMotion.reorder.spec(visibilityThreshold = IntOffset.VisibilityThreshold)) else Modifier
            )
        }
    }
}

@Composable
private fun AtelierQueueTopBar(title: String, historyLabel: String, history: Boolean, onToggleHistory: () -> Unit, onClose: () -> Unit) {
    val strings = LocalLevyraStrings.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        AtelierIconTarget(Icons.Rounded.KeyboardArrowDown, strings.close, AtelierDesign.text, onClose)
        Text(
            title, style = MaterialTheme.typography.headlineSmall, color = AtelierDesign.text,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp).semantics { heading() }
        )
        Box(
            Modifier.size(48.dp).clip(AtelierDesign.cardShape)
                .toggleable(value = history, role = Role.Switch, onValueChange = { onToggleHistory() }),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.History, historyLabel, tint = if (history) AtelierDesign.accent else AtelierDesign.textSecondary, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun AtelierQueueLabel(text: String) {
    Text(
        atelierUppercaseLabel(text), style = MaterialTheme.typography.labelMedium, color = AtelierDesign.textSecondary,
        modifier = Modifier.fillMaxWidth().padding(start = AtelierDesign.gutter, end = AtelierDesign.gutter, top = 28.dp, bottom = 8.dp).semantics { heading() }
    )
}

@Composable
private fun AtelierQueueNotice(text: String) {
    Text(
        text, style = MaterialTheme.typography.bodyLarge, color = AtelierDesign.textSecondary,
        modifier = Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter, vertical = 32.dp)
    )
}

@Composable
private fun AtelierQueueRadioRow(enabled: Boolean, onToggle: () -> Unit) {
    val strings = LocalLevyraStrings.current
    Row(
        Modifier.fillMaxWidth().padding(top = 16.dp).heightIn(min = AtelierDesign.rowHeight)
            .toggleable(value = enabled, role = Role.Switch, onValueChange = { onToggle() })
            .padding(horizontal = AtelierDesign.gutter, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(strings.continuousRadio, style = MaterialTheme.typography.titleMedium, color = AtelierDesign.text, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = enabled,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AtelierDesign.onAccent,
                checkedTrackColor = AtelierDesign.accent,
                checkedBorderColor = AtelierDesign.accent,
                uncheckedThumbColor = AtelierDesign.textSecondary,
                uncheckedTrackColor = AtelierDesign.panel,
                uncheckedBorderColor = AtelierDesign.hairlineStrong
            )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AtelierQueueRow(
    track: Track,
    played: Boolean,
    copy: AtelierCopy,
    onClick: (() -> Unit)?,
    onActions: () -> Unit,
    queueIndex: Int = -1,
    queueSize: Int = 0,
    onMove: ((Int, Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false
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
            .background(if (highlighted) AtelierDesign.panel else AtelierDesign.room)
            .onSizeChanged { rowHeight = it.height.coerceAtLeast(1) }
            .graphicsLayer { translationY = drag }
            .semantics {
                if (reorderable) {
                    val moves = atelierQueueMoveActions(queueIndex, queueSize, copy, onMove)
                    customActions = moves
                }
            }
            .combinedClickable(
                enabled = onClick != null,
                onLongClickLabel = strings.more,
                onLongClick = onActions,
                onClick = { onClick?.invoke() }
            )
            .heightIn(min = 72.dp)
            .padding(start = AtelierDesign.gutter, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.weight(1f).alpha(if (played) AtelierQueuePlayedAlpha else 1f), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(AtelierDesign.rowArtwork).clip(AtelierDesign.sleeveShape).border(1.dp, AtelierDesign.hairline, AtelierDesign.sleeveShape)) {
                CoverImage(track, Modifier.fillMaxSize())
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    track.title, style = MaterialTheme.typography.titleMedium,
                    color = if (highlighted) AtelierDesign.accentBright else AtelierDesign.text,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (track.explicit) {
                        Box(
                            Modifier.border(1.dp, AtelierDesign.hairlineStrong, AtelierDesign.sleeveShape).padding(horizontal = 4.dp)
                                .semantics { contentDescription = copy.explicit },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(copy.explicit.take(1).uppercase(), style = MaterialTheme.typography.labelSmall, color = AtelierDesign.textSecondary, maxLines = 1)
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = AtelierDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        AtelierIconTarget(Icons.Rounded.MoreVert, strings.more, AtelierDesign.textSecondary, onActions)
        if (reorderable) {
            Box(
                Modifier.size(48.dp).pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = { drag = 0f },
                        onDragCancel = { drag = 0f },
                        onVerticalDrag = { change, amount ->
                            change.consume()
                            drag += amount
                            val threshold = rowHeight.toFloat()
                            val index = latestIndex
                            val target = atelierQueueDragTarget(drag, threshold, index) { latestSize }
                            if (target != index) {
                                latestMove?.invoke(index, target)
                                drag = 0f
                                haptics.perform(LevyraHapticAction.Reorder)
                            }
                        }
                    )
                },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.DragHandle, strings.dragToReorder, tint = AtelierDesign.textSecondary, modifier = Modifier.size(24.dp))
            }
        }
    }
}

private fun atelierQueueMoveActions(
    queueIndex: Int,
    queueSize: Int,
    copy: AtelierCopy,
    onMove: ((Int, Int) -> Unit)?
): List<CustomAccessibilityAction> = buildList {
    if (queueIndex > 0) {
        add(CustomAccessibilityAction(copy.queueMoveUp) { onMove?.invoke(queueIndex, queueIndex - 1); true })
    }
    if (queueIndex < queueSize - 1) {
        add(CustomAccessibilityAction(copy.queueMoveDown) { onMove?.invoke(queueIndex, queueIndex + 1); true })
    }
}

private inline fun atelierQueueDragTarget(drag: Float, threshold: Float, index: Int, size: () -> Int): Int = when {
    drag > threshold && index < size() - 1 -> index + 1
    drag < -threshold && index > 0 -> index - 1
    else -> index
}
