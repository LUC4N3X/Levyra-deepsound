package com.luc4n3x.levyra.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.rounded.Share
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.lyrics.activeLyricIndex
import com.luc4n3x.levyra.ui.theme.AtelierDesign
import com.luc4n3x.levyra.viewmodel.LevyraUiState

private const val AtelierLyricsManualHoldMs = 3_000L
private const val AtelierLyricsColorMs = 220
private const val AtelierLyricsAnchorFraction = 0.3f
private const val AtelierLyricsInterludeMark = "· · ·"
private val AtelierLyricsMarkerWidth = 2.dp

@Composable
internal fun AtelierLyricsScreen(
    state: LevyraUiState,
    track: Track,
    lines: List<LyricLine>,
    position: () -> Long,
    animations: Boolean,
    onLineClick: (Int, LyricLine) -> Unit,
    onTogglePlay: () -> Unit,
    onOptions: () -> Unit,
    onClose: () -> Unit,
    selectionMode: Boolean = false,
    selectedRange: IntRange? = null,
    onLineLongClick: (Int, LyricLine) -> Unit = { _, _ -> },
    onShare: (() -> Unit)? = null
) {
    var active by remember(lines, state.lyricsSynced) { mutableIntStateOf(-1) }
    LaunchedEffect(lines, state.lyricsSynced) {
        if (!state.lyricsSynced || lines.isEmpty()) return@LaunchedEffect
        while (true) {
            withFrameMillis { }
            active = activeLyricIndex(position(), lines)
        }
    }
    val listState = rememberLazyListState()
    var manualUntil by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start -> dragging = true
                is DragInteraction.Stop, is DragInteraction.Cancel -> {
                    dragging = false
                    manualUntil = android.os.SystemClock.elapsedRealtime() + AtelierLyricsManualHoldMs
                }
                else -> Unit
            }
        }
    }
    LaunchedEffect(active, manualUntil, dragging) {
        if (active < 0 || dragging) return@LaunchedEffect
        val hold = manualUntil - android.os.SystemClock.elapsedRealtime()
        if (hold > 0) kotlinx.coroutines.delay(hold)
        val anchor = (listState.layoutInfo.viewportSize.height * AtelierLyricsAnchorFraction).toInt()
        if (animations) listState.animateScrollToItem(active, -anchor) else listState.scrollToItem(active, -anchor)
    }
    Column(Modifier.fillMaxSize().background(AtelierDesign.room).statusBarsPadding().navigationBarsPadding()) {
        AtelierLyricsTopBar(selectionMode, onShare, onOptions, onClose)
        HorizontalDivider(thickness = 1.dp, color = AtelierDesign.hairline)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AtelierLyricsBody(state, lines, listState, active, animations, selectionMode, selectedRange, onLineClick, onLineLongClick)
        }
        HorizontalDivider(thickness = 1.dp, color = AtelierDesign.hairline)
        AtelierLyricsTransport(state.isPlaying, track, onTogglePlay, onClose)
    }
}

@Composable
private fun AtelierLyricsTopBar(selectionMode: Boolean, onShare: (() -> Unit)?, onOptions: () -> Unit, onClose: () -> Unit) {
    val strings = LocalLevyraStrings.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        AtelierIconTarget(Icons.Rounded.KeyboardArrowDown, strings.close, AtelierDesign.text, onClose)
        Text(
            atelierUppercaseLabel(strings.lyrics), style = MaterialTheme.typography.labelMedium, color = AtelierDesign.accent,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp).semantics { heading() }
        )
        if (onShare != null) {
            AtelierIconTarget(Icons.Rounded.Share, strings.shareLyrics, if (selectionMode) AtelierDesign.accent else AtelierDesign.text, onShare)
        }
        AtelierIconTarget(Icons.Rounded.Tune, strings.options, AtelierDesign.text, onOptions)
    }
}

@Composable
private fun BoxScope.AtelierLyricsBody(
    state: LevyraUiState,
    lines: List<LyricLine>,
    listState: LazyListState,
    active: Int,
    animations: Boolean,
    selectionMode: Boolean,
    selectedRange: IntRange?,
    onLineClick: (Int, LyricLine) -> Unit,
    onLineLongClick: (Int, LyricLine) -> Unit
) {
    val strings = LocalLevyraStrings.current
    when {
        lines.isNotEmpty() -> LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = AtelierDesign.gutter, end = AtelierDesign.gutter, top = 28.dp, bottom = 240.dp)
        ) {
            itemsIndexed(lines, key = { index, line -> "${line.startMs}-$index" }) { index, line ->
                AtelierLyricLineRow(
                    line, index == active, state.lyricsSynced || selectionMode, selectedRange?.contains(index) == true, animations,
                    { onLineLongClick(index, line) }
                ) { onLineClick(index, line) }
            }
        }
        state.lyricsLoading -> Text(
            strings.searchingLyrics, style = MaterialTheme.typography.bodyLarge, color = AtelierDesign.textSecondary,
            modifier = Modifier.align(Alignment.Center).padding(AtelierDesign.gutter)
        )
        else -> Text(
            strings.lyricsUnavailable, style = MaterialTheme.typography.bodyLarge, color = AtelierDesign.textSecondary,
            modifier = Modifier.align(Alignment.Center).padding(AtelierDesign.gutter)
        )
    }
}

@Composable
private fun AtelierLyricLineRow(
    line: LyricLine, current: Boolean, synced: Boolean, selected: Boolean, animations: Boolean,
    onLongClick: () -> Unit, onClick: () -> Unit
) {
    val target = if (current || selected || !synced) AtelierDesign.text else AtelierDesign.textFaint
    val color by animateColorAsState(
        target,
        if (animations) tween(AtelierLyricsColorMs) else snap(),
        label = "atelier-lyric-line"
    )
    Text(
        line.text.ifBlank { AtelierLyricsInterludeMark },
        style = MaterialTheme.typography.headlineMedium,
        color = color,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 3.dp)
            .combinedClickable(onLongClick = onLongClick, onClick = { if (synced) onClick() })
            .then(if (selected) Modifier.background(AtelierDesign.accent.copy(alpha = 0.18f)) else Modifier)
            .drawBehind {
                if (current) {
                    val markerWidth = AtelierLyricsMarkerWidth.toPx()
                    val startX = if (layoutDirection == LayoutDirection.Ltr) 0f else size.width - markerWidth
                    drawRect(AtelierDesign.accent, Offset(startX, 0f), Size(markerWidth, size.height))
                }
            }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
private fun AtelierLyricsTransport(isPlaying: Boolean, track: Track, onTogglePlay: () -> Unit, onClose: () -> Unit) {
    val strings = LocalLevyraStrings.current
    Row(
        Modifier.fillMaxWidth().background(AtelierDesign.cabinet)
            .clickable(role = Role.Button, onClickLabel = strings.close, onClick = onClose)
            .padding(start = AtelierDesign.gutter, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(AtelierDesign.rowArtwork).clip(AtelierDesign.sleeveShape).border(1.dp, AtelierDesign.hairline, AtelierDesign.sleeveShape)) {
            CoverImage(track, Modifier.fillMaxSize())
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, style = MaterialTheme.typography.titleMedium, color = AtelierDesign.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = AtelierDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        AtelierIconTarget(
            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            if (isPlaying) strings.pause else strings.play,
            AtelierDesign.accent,
            onTogglePlay
        )
    }
}
