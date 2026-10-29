package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.lyrics.activeLyricIndex
import com.luc4n3x.levyra.ui.player.qobuzPlayerField
import com.luc4n3x.levyra.ui.theme.QobuzDesign
import com.luc4n3x.levyra.viewmodel.LevyraUiState

private const val QobuzLyricsManualHoldMs = 3_000L

@Composable
internal fun QobuzLyricsScreen(
    state: LevyraUiState,
    track: Track,
    lines: List<LyricLine>,
    position: () -> Long,
    animations: Boolean,
    onLineClick: (Int, LyricLine) -> Unit,
    onTogglePlay: () -> Unit,
    onClose: () -> Unit
) {
    val field by rememberSpotifyCoverTone(track, ::qobuzPlayerField)
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
                    manualUntil = android.os.SystemClock.elapsedRealtime() + QobuzLyricsManualHoldMs
                }
                else -> Unit
            }
        }
    }
    LaunchedEffect(active, manualUntil, dragging) {
        if (active < 0 || dragging) return@LaunchedEffect
        val hold = manualUntil - android.os.SystemClock.elapsedRealtime()
        if (hold > 0) kotlinx.coroutines.delay(hold)
        val anchor = (listState.layoutInfo.viewportSize.height * 0.32f).toInt()
        if (animations) listState.animateScrollToItem(active, -anchor) else listState.scrollToItem(active, -anchor)
    }
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(field.copy(alpha = 1f), Color(0xFF2B2420))))
            .statusBarsPadding().navigationBarsPadding()
    ) {
        QobuzLyricsHandle(onClose)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            QobuzLyricsBody(state, lines, listState, active, onLineClick)
        }
        QobuzLyricsMiniPlayer(state, track, onTogglePlay, onClose)
    }
}

@Composable
private fun QobuzLyricsHandle(onClose: () -> Unit) {
    val strings = LocalLevyraStrings.current
    Box(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = strings.close, onClick = onClose).padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) { Box(Modifier.width(40.dp).height(5.dp).clip(RoundedCornerShape(50)).background(QobuzDesign.playerContentTertiary)) }
}

@Composable
private fun BoxScope.QobuzLyricsBody(
    state: LevyraUiState,
    lines: List<LyricLine>,
    listState: LazyListState,
    active: Int,
    onLineClick: (Int, LyricLine) -> Unit
) {
    val strings = LocalLevyraStrings.current
    when {
        lines.isNotEmpty() -> LazyColumn(state = listState, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 240.dp)) {
            itemsIndexed(lines, key = { index, line -> "${line.startMs}-$index" }) { index, line ->
                val current = index == active
                val text = line.text.ifBlank { "~ ~ ~" }
                Text(
                    text,
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
                    color = if (current || !state.lyricsSynced) Color.White else Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(16.dp))
                        .background(if (current) Color.White.copy(alpha = 0.10f) else Color.Transparent)
                        .clickable(enabled = state.lyricsSynced) { onLineClick(index, line) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
        }
        state.lyricsLoading -> Text(strings.searchingLyrics, color = QobuzDesign.playerContentSecondary, fontSize = 20.sp,
            modifier = Modifier.align(Alignment.Center))
        else -> Text(strings.lyricsUnavailable, color = QobuzDesign.playerContentSecondary, fontSize = 20.sp,
            modifier = Modifier.align(Alignment.Center).padding(24.dp))
    }
}

@Composable
private fun QobuzLyricsMiniPlayer(
    state: LevyraUiState,
    track: Track,
    onTogglePlay: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    Row(
        Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.10f))
            .clickable(onClick = onClose).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(4.dp))) { CoverImage(track, Modifier.fillMaxSize()) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = QobuzDesign.playerContentSecondary, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = if (state.isPlaying) strings.pause else strings.play, onClick = onTogglePlay),
            contentAlignment = Alignment.Center
        ) { Icon(if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
    }
}
