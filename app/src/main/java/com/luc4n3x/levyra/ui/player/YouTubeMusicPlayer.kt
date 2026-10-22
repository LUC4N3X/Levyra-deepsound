package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.PlayerControlLabels
import com.luc4n3x.levyra.ui.components.formatSeekbarMillis
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraYouTubeMusicSkinStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.rememberSpotifyCoverTone
import com.luc4n3x.levyra.ui.theme.YouTubeMusicDesign

@Composable
internal fun YouTubeMusicPlayerScene(
    track: Track, slots: PlayerDeckSlots, artworkCap: Dp, playing: Boolean,
    metadata: @Composable () -> Unit, transport: @Composable () -> Unit, modifier: Modifier = Modifier,
    canvas: AppleMusicCanvas? = null
) {
    val field by rememberSpotifyCoverTone(track, YouTubeMusicDesign::playerField)
    val deep by rememberSpotifyCoverTone(track, YouTubeMusicDesign::playerFieldDeep)
    BoxWithConstraints(modifier.fillMaxSize().background(Brush.verticalGradient(0f to field, 0.6f to field, 1f to deep))) {
        val landscape = maxWidth > maxHeight
        val immersive = !landscape && canvas?.immersive == true
        val canvasAlpha by animateFloatAsState(if (immersive) 1f else 0f, label = "ytm-player-canvas-alpha")
        if (canvas != null && canvasAlpha > 0f) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = canvasAlpha }) {
                AppleMusicCanvasBackdrop(track, canvas, playing)
            }
        }
        val artwork: @Composable (Dp) -> Unit = { size ->
            Box(Modifier.size(size).graphicsLayer { alpha = 1f - canvasAlpha }) {
                if (!immersive) slots.stage(track, Modifier.fillMaxSize(), YouTubeMusicDesign.playerArtworkCorner)
            }
        }
        if (landscape) {
            YouTubeMusicLandscapeDeck(track, slots, minOf(maxHeight - 32.dp, artworkCap), artwork, metadata, transport)
        } else {
            YouTubeMusicPortraitDeck(track, slots, artworkCap, artwork, metadata, transport)
        }
    }
}

@Composable
private fun YouTubeMusicPortraitDeck(
    track: Track, slots: PlayerDeckSlots, artworkCap: Dp, artwork: @Composable (Dp) -> Unit,
    metadata: @Composable () -> Unit, transport: @Composable () -> Unit
) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Box(Modifier.padding(horizontal = 4.dp)) { slots.header() }
        BoxWithConstraints(
            Modifier.fillMaxWidth().weight(1f).padding(horizontal = YouTubeMusicDesign.playerInset, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) { artwork(minOf(maxWidth, maxHeight, artworkCap)) }
        Box(Modifier.padding(horizontal = YouTubeMusicDesign.playerInset - 16.dp).clipToBounds()) { metadata() }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.padding(start = 4.dp)) { slots.engagement(track) }
        Box(Modifier.padding(horizontal = YouTubeMusicDesign.playerInset)) { slots.progress(PlayerProgressStyle.Classic) }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(Modifier.padding(horizontal = YouTubeMusicDesign.playerInset - 12.dp)) { transport() }
        }
        YouTubeMusicPlayerTabs(slots.dockActions(track), Modifier.padding(top = 8.dp, bottom = 12.dp))
    }
}

@Composable
private fun YouTubeMusicLandscapeDeck(
    track: Track, slots: PlayerDeckSlots, artworkSize: Dp, artwork: @Composable (Dp) -> Unit,
    metadata: @Composable () -> Unit, transport: @Composable () -> Unit
) {
    Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().displayCutoutPadding()) {
        Row(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true), contentAlignment = Alignment.Center) {
                artwork(artworkSize)
            }
            Spacer(Modifier.width(24.dp))
            Column(
                Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(Modifier.padding(horizontal = 4.dp)) { slots.header() }
                metadata()
                slots.engagement(track)
                slots.progress(PlayerProgressStyle.Classic)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { transport() }
                YouTubeMusicPlayerTabs(slots.dockActions(track))
            }
        }
    }
}

private val YouTubeMusicTabOrder = listOf("queue", "lyrics", "download")

@Composable
private fun YouTubeMusicPlayerTabs(dockActions: List<PlayerDockAction>, modifier: Modifier = Modifier) {
    val actions = remember(dockActions) {
        dockActions.filter { it.key != "visual-mode" }.sortedBy { action -> YouTubeMusicTabOrder.indexOf(action.key).let { if (it < 0) YouTubeMusicTabOrder.size else it } }
    }
    if (actions.isEmpty()) return
    val copy = LevyraYouTubeMusicSkinStrings.forCode(LocalLevyraStrings.current.code)
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    Row(modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        actions.forEach { action ->
            val label = when (action.key) {
                "queue" -> copy.upNext
                "lyrics" -> copy.lyrics
                else -> action.label
            }
            Box(
                Modifier.weight(1f).fillMaxHeight().clip(CircleShape)
                    .levyraPressable(onClick = action.onClick, enabled = action.enabled, role = Role.Tab, pressedScale = LevyraPressScale.Control)
                    .semantics {
                        contentDescription = action.label
                        action.stateDescription?.let { stateDescription = it }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (action.busy) {
                    LevyraLoadingIndicator(color = YouTubeMusicDesign.textSecondary, modifier = Modifier.size(18.dp))
                } else {
                    Text(
                        label.uppercase(locale), style = MaterialTheme.typography.labelMedium,
                        color = if (action.active) Color.White else Color.White.copy(alpha = 0.78f),
                        fontWeight = if (action.active) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
internal fun YouTubeMusicTransport(
    playing: Boolean, resolving: Boolean, shuffle: Boolean, repeat: RepeatMode,
    labels: PlayerControlLabels, onShuffle: () -> Unit, onPrevious: () -> Unit,
    onToggle: () -> Unit, onNext: () -> Unit, onRepeat: () -> Unit, modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth().height(96.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        YouTubeMusicModeToggle(Icons.Rounded.Shuffle, labels.shuffle, shuffle, onShuffle)
        YouTubeMusicSkipButton(Icons.Rounded.SkipPrevious, labels.previous, onPrevious)
        Box(
            Modifier.size(YouTubeMusicDesign.playerPlay).clip(CircleShape).background(Color.White)
                .levyraPressable(onClick = onToggle, role = Role.Button, pressedScale = LevyraPressScale.Control)
                .semantics { contentDescription = if (playing) labels.pause else labels.play },
            contentAlignment = Alignment.Center
        ) {
            if (resolving) LevyraLoadingIndicator(color = Color.Black, modifier = Modifier.size(32.dp))
            else Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(40.dp))
        }
        YouTubeMusicSkipButton(Icons.Rounded.SkipNext, labels.next, onNext)
        YouTubeMusicModeToggle(if (repeat == RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, labels.repeat, repeat != RepeatMode.Off, onRepeat)
    }
}

@Composable
private fun YouTubeMusicSkipButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(56.dp).clip(CircleShape).levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Control)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(YouTubeMusicDesign.playerSkip))
    }
}

@Composable
private fun YouTubeMusicModeToggle(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape)
            .toggleable(value = active, role = Role.Switch, onValueChange = { onClick() })
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = if (active) Color.White else Color.White.copy(alpha = 0.62f), modifier = Modifier.size(YouTubeMusicDesign.playerSide))
        if (active) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp).size(4.dp).clip(CircleShape).background(Color.White))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun YouTubeMusicSeekbar(positionMs: Long, bufferedMs: Long, durationMs: Long, onSeek: (Float) -> Unit, modifier: Modifier = Modifier) {
    var dragging by remember(durationMs) { mutableStateOf<Float?>(null) }
    val fraction = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val sliderState = rememberSliderState(value = dragging ?: fraction, trackRange = 0f..1f)
    val seekLabel = LocalLevyraStrings.current.gestureActionSeek
    SideEffect { sliderState.value = dragging ?: fraction }
    Column(modifier.fillMaxWidth()) {
        Slider(
            state = sliderState, onValueChange = { dragging = it },
            onValueChangeFinished = { dragging?.let(onSeek); dragging = null },
            enabled = durationMs > 0L, modifier = Modifier.fillMaxWidth().heightIn(min = 32.dp).semantics { contentDescription = seekLabel },
            thumb = { Box(Modifier.size(if (dragging != null) 16.dp else 12.dp).clip(CircleShape).background(Color.White)) },
            track = { state ->
                Box(Modifier.fillMaxWidth().height(2.dp).background(YouTubeMusicDesign.seekTrack)) {
                    val buffered = if (durationMs > 0L) (bufferedMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
                    Box(Modifier.fillMaxWidth(buffered).fillMaxHeight().background(Color.White.copy(alpha = 0.12f)))
                    Box(Modifier.fillMaxWidth(state.value.coerceIn(0f, 1f)).fillMaxHeight().background(Color.White))
                }
            }
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val timeStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
            val shown = dragging?.let { (durationMs * it).toLong() } ?: positionMs
            Text(formatSeekbarMillis(shown), style = timeStyle, color = YouTubeMusicDesign.textSecondary)
            Text(formatSeekbarMillis(durationMs.coerceAtLeast(0L)), style = timeStyle, color = YouTubeMusicDesign.textSecondary)
        }
    }
}
