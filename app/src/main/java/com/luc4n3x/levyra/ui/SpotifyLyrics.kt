package com.luc4n3x.levyra.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.lyrics.activeLyricIndex
import com.luc4n3x.levyra.ui.player.SpotifySeekbar
import com.luc4n3x.levyra.ui.theme.SpotifyDesign
import com.luc4n3x.levyra.viewmodel.LevyraUiState

private const val SpotifyLyricsInactiveScale = 0.82f
private const val SpotifyLyricsManualHoldMs = 3_000L
private const val SpotifyLyricsAnchorFraction = 0.34f

internal class SpotifyLyricsActions(
    val onLineClick: (Int, LyricLine) -> Unit,
    val onSeekFraction: (Float) -> Unit,
    val onTogglePlay: () -> Unit,
    val onShare: (() -> Unit)?,
    val onOptions: () -> Unit,
    val onClose: () -> Unit
)

@Composable
internal fun SpotifyLyricsOverlayScreen(
    state: LevyraUiState,
    track: Track,
    lines: List<LyricLine>,
    position: () -> Long,
    selectionMode: Boolean,
    selectedRange: IntRange?,
    animations: Boolean,
    onLineClick: (Int, LyricLine) -> Unit,
    onSeekFraction: (Float) -> Unit,
    onTogglePlay: () -> Unit,
    onShare: (() -> Unit)?,
    onOptions: () -> Unit,
    onClose: () -> Unit
) {
    SpotifyLyricsScreen(
        track = track,
        lines = lines,
        synced = state.lyricsSynced,
        loading = state.lyricsLoading,
        position = position,
        positionMs = state.positionMs,
        bufferedMs = state.bufferedPositionMs,
        durationMs = state.durationMs,
        playing = state.isPlaying,
        resolving = state.isResolving,
        selectionMode = selectionMode,
        isSelected = { index -> selectedRange?.contains(index) == true },
        animations = animations,
        actions = SpotifyLyricsActions(onLineClick, onSeekFraction, onTogglePlay, onShare, onOptions, onClose)
    )
}

@Composable
private fun SpotifyLyricsScreen(
    track: Track,
    lines: List<LyricLine>,
    synced: Boolean,
    loading: Boolean,
    position: () -> Long,
    positionMs: Long,
    bufferedMs: Long,
    durationMs: Long,
    playing: Boolean,
    resolving: Boolean,
    selectionMode: Boolean,
    isSelected: (Int) -> Boolean,
    animations: Boolean,
    actions: SpotifyLyricsActions
) {
    val strings = LocalLevyraStrings.current
    val tone by rememberSpotifyCoverTone(track, SpotifyDesign::lyricsSurface)
    val active = rememberSpotifyLyricIndex(lines, synced, position)
    val listState = rememberSpotifyLyricScroll(active, animations)
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(tone, lerp(tone, Color.White, 0.06f))))
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = actions.onClose, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.KeyboardArrowDown, strings.closeLyrics, tint = SpotifyDesign.text, modifier = Modifier.size(34.dp))
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(track.title, color = SpotifyDesign.text, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, color = SpotifyDesign.text, fontSize = 14.sp, lineHeight = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.size(56.dp))
            }
            SpotifyLyricsBody(lines, synced, loading, active, listState, selectionMode, isSelected, animations, actions, tone, Modifier.weight(1f).fillMaxWidth())
            SpotifyLyricsControls(positionMs, bufferedMs, durationMs, playing, resolving, selectionMode, actions)
        }
    }
}

@Composable
private fun SpotifyLyricRow(
    line: LyricLine,
    current: Boolean,
    emphasized: Boolean,
    selected: Boolean,
    animations: Boolean,
    onClick: () -> Unit
) {
    val spec = if (animations) tween<Float>(300) else snap()
    val scale by animateFloatAsState(if (emphasized) 1f else SpotifyLyricsInactiveScale, spec, label = "spotify-lyric-scale")
    val color by animateColorAsState(
        if (emphasized || selected) SpotifyDesign.text else SpotifyDesign.text.copy(alpha = 0.7f),
        if (animations) tween(300) else snap(), label = "spotify-lyric-color"
    )
    Text(
        line.text,
        color = color, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .then(if (selected) Modifier.background(Color.White.copy(alpha = 0.14f)) else Modifier)
            .clickable(onClick = onClick)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
    )
}

@Composable
private fun rememberSpotifyLyricIndex(lines: List<LyricLine>, synced: Boolean, position: () -> Long): Int {
    var active by remember(lines, synced) { mutableIntStateOf(-1) }
    LaunchedEffect(lines, synced) {
        if (!synced || lines.isEmpty()) return@LaunchedEffect
        while (true) {
            withFrameMillis { }
            active = activeLyricIndex(position(), lines)
        }
    }
    return active
}

@Composable
private fun rememberSpotifyLyricScroll(active: Int, animations: Boolean): LazyListState {
    val listState = rememberLazyListState()
    var manualUntil by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start -> dragging = true
                is DragInteraction.Stop, is DragInteraction.Cancel -> {
                    dragging = false
                    manualUntil = android.os.SystemClock.elapsedRealtime() + SpotifyLyricsManualHoldMs
                }
                else -> Unit
            }
        }
    }
    LaunchedEffect(active, manualUntil, dragging) {
        if (active < 0 || dragging) return@LaunchedEffect
        val hold = manualUntil - android.os.SystemClock.elapsedRealtime()
        if (hold > 0) kotlinx.coroutines.delay(hold)
        val anchor = (listState.layoutInfo.viewportSize.height * SpotifyLyricsAnchorFraction).toInt()
        if (animations) listState.animateScrollToItem(active, -anchor) else listState.scrollToItem(active, -anchor)
    }
    return listState
}

@Composable
private fun SpotifyLyricsBody(lines: List<LyricLine>, synced: Boolean, loading: Boolean, active: Int, listState: LazyListState, selectionMode: Boolean, isSelected: (Int) -> Boolean, animations: Boolean, actions: SpotifyLyricsActions, tone: Color, modifier: Modifier) {
    val strings = LocalLevyraStrings.current
    Box(modifier) {
        when {
            lines.isNotEmpty() -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(lines, key = { index, line -> "$index-${line.startMs}" }) { index, line ->
                    SpotifyLyricRow(
                        line = line,
                        current = synced && index == active,
                        emphasized = !synced || index == active,
                        selected = selectionMode && isSelected(index),
                        animations = animations,
                        onClick = { actions.onLineClick(index, line) }
                    )
                }
            }
            loading -> LevyraLoadingIndicator(color = SpotifyDesign.text, modifier = Modifier.align(Alignment.Center).size(32.dp))
            else -> Text(
                strings.lyricsUnavailable, color = SpotifyDesign.text, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center).padding(32.dp)
            )
        }
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(72.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, lerp(tone, Color.White, 0.06f))))
        )
    }
}

@Composable
private fun SpotifyLyricsControls(positionMs: Long, bufferedMs: Long, durationMs: Long, playing: Boolean, resolving: Boolean, selectionMode: Boolean, actions: SpotifyLyricsActions) {
    val strings = LocalLevyraStrings.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        val share = actions.onShare
        if (share != null) IconButton(onClick = share, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Rounded.Share, strings.shareLyrics, tint = if (selectionMode) SpotifyDesign.green else SpotifyDesign.text, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = actions.onOptions, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Rounded.MoreVert, strings.options, tint = SpotifyDesign.text, modifier = Modifier.size(26.dp))
        }
    }
    SpotifySeekbar(positionMs, bufferedMs, durationMs, actions.onSeekFraction, Modifier.padding(horizontal = 24.dp))
    Box(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(64.dp).clip(CircleShape).background(Color.White)
                .clickable(role = Role.Button, onClick = actions.onTogglePlay)
                .semantics { contentDescription = if (playing) strings.pause else strings.play },
            contentAlignment = Alignment.Center
        ) {
            if (resolving) LevyraLoadingIndicator(color = Color.Black, modifier = Modifier.size(28.dp))
            else Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(36.dp))
        }
    }
}
