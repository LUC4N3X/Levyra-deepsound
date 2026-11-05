package com.luc4n3x.levyra.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.lyrics.activeLyricIndex
import com.luc4n3x.levyra.ui.player.rememberMonoClock
import com.luc4n3x.levyra.ui.theme.MonoDesign
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import kotlin.math.PI

private const val MonoLyricsManualHoldMs = 3_000L
private const val MonoLyricsColorMs = 240
private const val MonoLyricsAnchorFraction = 0.32f

@Composable
internal fun MonoLyricsScreen(
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
    var sungWords by remember(lines, state.lyricsSynced) { mutableIntStateOf(0) }
    LaunchedEffect(lines, state.lyricsSynced) {
        if (!state.lyricsSynced || lines.isEmpty()) return@LaunchedEffect
        while (true) {
            withFrameMillis { }
            val now = position()
            val index = activeLyricIndex(now, lines)
            active = index
            val words = lines.getOrNull(index)?.words.orEmpty()
            sungWords = if (words.isEmpty()) 0 else words.count { it.startMs <= now }
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
                    manualUntil = android.os.SystemClock.elapsedRealtime() + MonoLyricsManualHoldMs
                }
                else -> Unit
            }
        }
    }
    LaunchedEffect(active, manualUntil, dragging) {
        if (active < 0 || dragging) return@LaunchedEffect
        val hold = manualUntil - android.os.SystemClock.elapsedRealtime()
        if (hold > 0) kotlinx.coroutines.delay(hold)
        val anchor = (listState.layoutInfo.viewportSize.height * MonoLyricsAnchorFraction).toInt()
        if (animations) listState.animateScrollToItem(active, -anchor) else listState.scrollToItem(active, -anchor)
    }
    Column(Modifier.fillMaxSize().background(MonoDesign.base).statusBarsPadding().navigationBarsPadding()) {
        MonoLyricsTopBar(track, selectionMode, onShare, onOptions, onClose)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            MonoLyricsBody(state, lines, listState, active, sungWords, animations, selectionMode, selectedRange, onLineClick, onLineLongClick)
        }
        HorizontalDivider(thickness = 1.dp, color = MonoDesign.hairline)
        MonoLyricsTransport(state.isPlaying, track, onTogglePlay, onClose)
    }
}

@Composable
private fun MonoLyricsKey(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun MonoLyricsTopBar(track: Track, selectionMode: Boolean, onShare: (() -> Unit)?, onOptions: () -> Unit, onClose: () -> Unit) {
    val strings = LocalLevyraStrings.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        MonoLyricsKey(Icons.Rounded.KeyboardArrowDown, strings.closeLyrics, MonoDesign.text, onClose)
        Column(Modifier.weight(1f).padding(horizontal = 8.dp).semantics(mergeDescendants = true) { heading() }) {
            MonoOverline(strings.lyrics)
            Text(track.title, style = MaterialTheme.typography.titleSmall, color = MonoDesign.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (onShare != null) MonoLyricsKey(Icons.Rounded.Share, strings.shareLyrics, if (selectionMode) MonoDesign.accent else MonoDesign.text, onShare)
        MonoLyricsKey(Icons.Rounded.Tune, strings.options, MonoDesign.text, onOptions)
    }
}

@Composable
private fun BoxScope.MonoLyricsBody(
    state: LevyraUiState,
    lines: List<LyricLine>,
    listState: LazyListState,
    active: Int,
    sungWords: Int,
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
            contentPadding = PaddingValues(start = MonoDesign.gutter, end = MonoDesign.gutter, top = 24.dp, bottom = 280.dp)
        ) {
            itemsIndexed(lines, key = { index, line -> "${line.startMs}-$index" }) { index, line ->
                MonoLyricLineRow(
                    line, index == active, if (index == active) sungWords else 0, state.lyricsSynced, selectionMode,
                    selectedRange?.contains(index) == true, animations, state.isPlaying,
                    { onLineLongClick(index, line) }
                ) { onLineClick(index, line) }
            }
        }
        state.lyricsLoading -> Column(
            Modifier.align(Alignment.Center).padding(MonoDesign.gutter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MonoWaveLine(playing = true, modifier = Modifier.width(96.dp).height(20.dp), color = MonoDesign.textSecondary, cycles = 3f, strokeWidth = 1.6.dp)
            Spacer(Modifier.height(12.dp))
            Text(strings.searchingLyrics, style = MaterialTheme.typography.bodyLarge, color = MonoDesign.textSecondary)
        }
        else -> Text(
            strings.lyricsUnavailable, style = MaterialTheme.typography.bodyLarge, color = MonoDesign.textSecondary,
            modifier = Modifier.align(Alignment.Center).padding(MonoDesign.gutter)
        )
    }
}

@Composable
private fun MonoLyricLineRow(
    line: LyricLine, current: Boolean, sungWords: Int, synced: Boolean, selectionMode: Boolean, selected: Boolean,
    animations: Boolean, playing: Boolean, onLongClick: () -> Unit, onClick: () -> Unit
) {
    val lit = current || selected || !synced
    val color by animateColorAsState(
        if (lit) MonoDesign.text else MonoDesign.textFaint,
        if (animations) tween(MonoLyricsColorMs) else snap(),
        label = "mono-lyric-line"
    )
    val interactive = synced || selectionMode
    val rowModifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 2.dp)
        .clip(MonoDesign.cardShape)
        .then(if (selected) Modifier.background(MonoDesign.accentGlow) else Modifier)
        .combinedClickable(onLongClick = onLongClick, onClick = { if (interactive) onClick() })
        .padding(horizontal = 4.dp, vertical = 8.dp)
    if (line.text.isBlank()) {
        Box(rowModifier, contentAlignment = Alignment.CenterStart) { MonoInterlude(current && playing, animations) }
        return
    }
    val words = line.words
    val text: AnnotatedString = if (current && synced && words.isNotEmpty() && sungWords in 1 until words.size) {
        remember(line, sungWords) { monoWordSyncText(line, sungWords) }
    } else {
        AnnotatedString(line.text)
    }
    Text(
        text,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = rowModifier
    )
}

private fun monoWordSyncText(line: LyricLine, sungWords: Int): AnnotatedString {
    val sung = line.words.take(sungWords).joinToString("") { it.text }
    val rest = line.text.removePrefix(sung)
    return if (sung.isEmpty() || rest.length == line.text.length) {
        AnnotatedString(line.text)
    } else {
        buildAnnotatedString {
            append(sung)
            withStyle(SpanStyle(color = MonoDesign.textSecondary)) { append(rest) }
        }
    }
}

@Composable
private fun MonoInterlude(active: Boolean, animations: Boolean) {
    val clock = rememberMonoClock(active && animations)
    Canvas(Modifier.width(72.dp).height(20.dp)) {
        drawMonoWave(
            if (active) MonoDesign.text else MonoDesign.textFaint,
            clock.floatValue * 2f * PI.toFloat() / 2.4f,
            if (active) 1f else 0.1f, cycles = 3f, strokeWidth = 2.dp
        )
    }
}

@Composable
private fun MonoLyricsTransport(isPlaying: Boolean, track: Track, onTogglePlay: () -> Unit, onClose: () -> Unit) {
    val strings = LocalLevyraStrings.current
    Row(
        Modifier.fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = strings.closeLyrics, onClick = onClose)
            .padding(start = MonoDesign.gutter, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(MonoDesign.miniArtwork).clip(MonoDesign.artShape).border(1.dp, MonoDesign.hairline, MonoDesign.artShape)) {
            CoverImage(track, Modifier.fillMaxSize())
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, style = MaterialTheme.typography.titleSmall, color = MonoDesign.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MonoDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        val label = if (isPlaying) strings.pause else strings.play
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(MonoDesign.accent)
                .clickable(role = Role.Button, onClickLabel = label, onClick = onTogglePlay)
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center
        ) {
            Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = MonoDesign.onAccent, modifier = Modifier.size(24.dp))
        }
    }
}
