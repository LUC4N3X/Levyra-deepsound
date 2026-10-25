package com.luc4n3x.levyra.ui.player

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
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(Modifier.fillMaxWidth().heightIn(min = viewport), verticalArrangement = Arrangement.SpaceBetween) {
                Box(Modifier.padding(horizontal = 4.dp)) { slots.header() }
                Box(Modifier.fillMaxWidth().padding(horizontal = gutter, vertical = 8.dp), contentAlignment = Alignment.Center) {
                    slots.stage(track, Modifier.size(artSize), SpotifyDesign.cornerLarge)
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = gutter), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    metadata()
                    slots.progress(PlayerProgressStyle.Classic)
                }
                Box(Modifier.padding(horizontal = 12.dp)) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { transport() }
                }
                Column(Modifier.fillMaxWidth().padding(start = gutter, end = gutter, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    slots.engagement(track)
                    slots.dock(track)
                }
            }
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
            Icon(Icons.Rounded.SkipPrevious, labels.previous, tint = SpotifyDesign.text, modifier = Modifier.size(32.dp))
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
            Icon(Icons.Rounded.SkipNext, labels.next, tint = SpotifyDesign.text, modifier = Modifier.size(32.dp))
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
        Icon(icon, null, tint = if (active) SpotifyDesign.green else SpotifyDesign.text, modifier = Modifier.size(24.dp))
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
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    Column(modifier.fillMaxWidth()) {
        Slider(
            state = sliderState, onValueChange = { dragging = it },
            onValueChangeFinished = { dragging?.let(onSeek); dragging = null },
            enabled = durationMs > 0L, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { contentDescription = seekLabel },
            thumb = { Box(Modifier.size(8.dp).clip(CircleShape).background(Color.White)) },
            track = { state ->
                Box(Modifier.fillMaxWidth().height(3.dp).clip(shape).background(Color.White.copy(alpha = 0.3f))) {
                    val buffered = if (durationMs > 0L) (bufferedMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
                    Box(Modifier.fillMaxWidth(buffered).fillMaxHeight().background(Color.White.copy(alpha = 0.12f)))
                    Box(Modifier.fillMaxWidth(state.value.coerceIn(0f, 1f)).fillMaxHeight().clip(shape).background(Color.White))
                }
            }
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatSeekbarMillis(dragging?.let { (durationMs * it).toLong() } ?: positionMs), style = MaterialTheme.typography.labelSmall, fontSize = 11.sp, color = SpotifyDesign.subdued)
            Text(formatSeekbarMillis(durationMs), style = MaterialTheme.typography.labelSmall, fontSize = 11.sp, color = SpotifyDesign.subdued)
        }
    }
    }
}
