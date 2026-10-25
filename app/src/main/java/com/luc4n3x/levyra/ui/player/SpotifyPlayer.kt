package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.rememberSpotifyCoverTone
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.PlayerControlLabels
import com.luc4n3x.levyra.ui.components.formatSeekbarMillis
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.*

@Composable
internal fun SpotifyPlayerScene(
    track: Track, slots: PlayerDeckSlots, artworkCap: Dp,
    metadata: @Composable () -> Unit, transport: @Composable () -> Unit, modifier: Modifier = Modifier
) {
    val tone by rememberSpotifyCoverTone(track, SpotifyDesign::heroSurface)
    val field = Brush.verticalGradient(0f to tone, 0.62f to lerp(tone, SpotifyDesign.base, 0.55f), 1f to lerp(tone, SpotifyDesign.base, 0.8f))
    BoxWithConstraints(modifier.fillMaxSize().background(field).statusBarsPadding().navigationBarsPadding()) {
        val gutter = 24.dp
        val artSize = minOf(maxWidth - gutter * 2, maxHeight * 0.46f, artworkCap)
        val viewport = maxHeight
        val preview = slots.lyricsPreview?.takeIf { it.trackId == track.id }
        val activeLine = preview?.let { rememberSpotifyActiveLine(it) } ?: -1
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(Modifier.fillMaxWidth().heightIn(min = viewport), verticalArrangement = Arrangement.SpaceBetween) {
                Box(Modifier.padding(horizontal = 4.dp)) { slots.header() }
                Box(Modifier.fillMaxWidth().padding(horizontal = gutter, vertical = 8.dp), contentAlignment = Alignment.Center) {
                    slots.stage(track, Modifier.size(artSize), SpotifyDesign.cornerLarge)
                }
                val currentText = preview?.lines?.getOrNull(activeLine)?.text?.takeIf { preview.synced && it.isNotBlank() }
                if (currentText != null) Text(
                    currentText, color = SpotifyDesign.text, fontSize = 16.sp, lineHeight = 20.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                        .padding(start = gutter, end = gutter, top = 8.dp, bottom = 8.dp)
                )
                Column(Modifier.fillMaxWidth().padding(horizontal = gutter), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    metadata()
                    slots.progress(PlayerProgressStyle.Classic)
                }
                Box(Modifier.padding(horizontal = 12.dp)) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { transport() }
                }
                Column(Modifier.fillMaxWidth().padding(start = gutter, end = gutter, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    slots.dock(track)
                }
            }
            Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (preview != null) SpotifyLyricsPreviewCard(track, preview, activeLine, Modifier)
                slots.artistPanel?.takeIf { it.trackId == track.id }?.let { panel -> SpotifyArtistCards(track, panel) }
            }
        }
    }
}

@Composable
private fun rememberSpotifyActiveLine(preview: PlayerLyricsPreview): Int {
    val position = preview.positionMs
    return remember(preview.lines, preview.synced, position / 100) {
        if (preview.synced) com.luc4n3x.levyra.ui.lyrics.activeLyricIndex(position, preview.lines) else -1
    }
}

private const val SpotifyPreviewWindow = 5
private const val SpotifyPreviewActiveScale = 1.18f

@Composable
private fun SpotifyLyricsPreviewCard(track: Track, preview: PlayerLyricsPreview, activeLine: Int, modifier: Modifier) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { com.luc4n3x.levyra.ui.i18n.LevyraSpotifySkinStrings.forCode(strings.code) }
    val surface by rememberSpotifyCoverTone(track, SpotifyDesign::lyricsSurface)
    val start = (activeLine - (SpotifyPreviewWindow - 2)).coerceAtLeast(0)
    val window = remember(preview.lines, start) {
        preview.lines.withIndex().drop(start).filter { it.value.text.isNotBlank() }.take(SpotifyPreviewWindow)
    }
    val animated = LocalAnimationsEnabled.current
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(surface)
            .clickable(onClickLabel = copy.showLyrics, role = Role.Button, onClick = preview.onOpen)
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 24.dp)
    ) {
        Text(copy.lyricsPreview, color = SpotifyDesign.text, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth().heightIn(max = 280.dp).clipToBounds()) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                window.forEach { (index, line) ->
                    val active = index == activeLine
                    val passed = preview.synced && activeLine >= 0 && index < activeLine
                    val scale by animateFloatAsState(if (active) SpotifyPreviewActiveScale else 1f, if (animated) tween(300) else snap(), label = "spotify-preview-scale")
                    Text(
                        line.text,
                        color = when {
                            active -> SpotifyDesign.text
                            passed || !preview.synced -> SpotifyDesign.text.copy(alpha = 0.7f)
                            else -> Color.Black.copy(alpha = 0.55f)
                        },
                        fontSize = 18.sp, lineHeight = 24.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(1f / SpotifyPreviewActiveScale).graphicsLayer {
                            scaleX = scale; scaleY = scale
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                        }
                    )
                }
            }
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(56.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, surface)))
            )
        }
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier.heightIn(min = 48.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                copy.showLyrics, color = Color.Black, fontSize = 14.sp, lineHeight = 18.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, maxLines = 1,
                modifier = Modifier.clip(CircleShape).background(Color.White).padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
internal fun SpotifyTransport(
    playing: Boolean, resolving: Boolean, shuffle: Boolean, repeat: RepeatMode,
    labels: PlayerControlLabels, onShuffle: () -> Unit, onPrevious: () -> Unit,
    onToggle: () -> Unit, onNext: () -> Unit, onRepeat: () -> Unit, modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth().height(88.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        SpotifyModeToggle(Icons.Rounded.Shuffle, labels.shuffle, shuffle, onShuffle)
        IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Rounded.SkipPrevious, labels.previous, tint = SpotifyDesign.text, modifier = Modifier.size(40.dp))
        }
        Box(
            Modifier.size(64.dp).clip(CircleShape).background(Color.White).clickable(role = Role.Button, onClick = onToggle)
                .semantics { contentDescription = if (playing) labels.pause else labels.play },
            contentAlignment = Alignment.Center
        ) {
            if (resolving) LevyraLoadingIndicator(color = Color.Black, modifier = Modifier.size(28.dp))
            else Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(36.dp))
        }
        IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Rounded.SkipNext, labels.next, tint = SpotifyDesign.text, modifier = Modifier.size(40.dp))
        }
        SpotifyModeToggle(
            if (repeat == RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, labels.repeat, repeat != RepeatMode.Off, onRepeat,
            stateLabel = when (repeat) {
                RepeatMode.All -> androidx.compose.ui.res.stringResource(com.luc4n3x.levyra.R.string.notification_repeat_all)
                RepeatMode.One -> androidx.compose.ui.res.stringResource(com.luc4n3x.levyra.R.string.notification_repeat_one)
                else -> null
            }
        )
    }
}

@Composable
private fun SpotifyModeToggle(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit, stateLabel: String? = null) {
    Column(
        Modifier.size(48.dp).clip(CircleShape).toggleable(value = active, role = Role.Switch, onValueChange = { onClick() })
            .semantics {
                contentDescription = label
                if (stateLabel != null) stateDescription = stateLabel
            },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = if (active) SpotifyDesign.green else SpotifyDesign.text, modifier = Modifier.size(26.dp))
        Box(Modifier.padding(top = 3.dp).size(4.dp).clip(CircleShape).background(if (active) SpotifyDesign.green else Color.Transparent))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SpotifySeekbar(positionMs: Long, bufferedMs: Long, durationMs: Long, onSeek: (Float) -> Unit, modifier: Modifier = Modifier) {
    var dragging by remember(durationMs) { mutableStateOf<Float?>(null) }
    val fraction = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val sliderState = rememberSliderState(value = dragging ?: fraction, trackRange = 0f..1f)
    val seekLabel = LocalLevyraStrings.current.gestureActionSeek
    val shape = RoundedCornerShape(2.dp)
    SideEffect { sliderState.value = dragging ?: fraction }
    Column(modifier.fillMaxWidth()) {
        Slider(
            state = sliderState, onValueChange = { dragging = it },
            onValueChangeFinished = { dragging?.let(onSeek); dragging = null },
            enabled = durationMs > 0L, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { contentDescription = seekLabel },
            thumb = { Box(Modifier.size(if (dragging != null) 16.dp else 12.dp).clip(CircleShape).background(Color.White)) },
            track = { state ->
                Box(Modifier.fillMaxWidth().height(4.dp).clip(shape).background(Color.White.copy(alpha = 0.3f))) {
                    val buffered = if (durationMs > 0L) (bufferedMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
                    Box(Modifier.fillMaxWidth(buffered).fillMaxHeight().background(Color.White.copy(alpha = 0.12f)))
                    Box(Modifier.fillMaxWidth(state.value.coerceIn(0f, 1f)).fillMaxHeight().clip(shape).background(if (dragging != null) SpotifyDesign.green else Color.White))
                }
            }
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatSeekbarMillis(dragging?.let { (durationMs * it).toLong() } ?: positionMs), style = MaterialTheme.typography.labelSmall, fontSize = 12.sp, color = SpotifyDesign.subdued)
            Text(formatSeekbarMillis(durationMs), style = MaterialTheme.typography.labelSmall, fontSize = 12.sp, color = SpotifyDesign.subdued)
        }
    }
}
