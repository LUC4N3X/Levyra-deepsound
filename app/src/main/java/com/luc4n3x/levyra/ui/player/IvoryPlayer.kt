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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.PlayerControlLabels
import com.luc4n3x.levyra.ui.components.formatSeekbarMillis
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.*

@Composable
internal fun IvoryPlayerScene(
    track: Track, slots: PlayerDeckSlots, artworkCap: Dp,
    metadata: @Composable () -> Unit, transport: @Composable () -> Unit, modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier.fillMaxSize().background(LevyraBlack).statusBarsPadding().navigationBarsPadding()) {
        val gutter = 28.dp
        val artSize = minOf(maxWidth - gutter * 2, maxHeight * 0.44f, artworkCap)
        val viewport = maxHeight
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(Modifier.fillMaxWidth().heightIn(min = viewport), verticalArrangement = Arrangement.SpaceBetween) {
                Box(Modifier.padding(horizontal = 12.dp)) { slots.header() }
                Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                    slots.stage(track, Modifier.size(artSize), 22.dp)
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = gutter), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    slots.progress(PlayerProgressStyle.Classic)
                    metadata()
                }
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { transport() }
                Column(Modifier.fillMaxWidth().padding(start = gutter, end = gutter, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    slots.engagement(track)
                    slots.dock(track)
                }
            }
        }
    }
}

@Composable
internal fun IvoryTransport(
    playing: Boolean, resolving: Boolean, shuffle: Boolean, repeat: RepeatMode,
    labels: PlayerControlLabels, onShuffle: () -> Unit, onPrevious: () -> Unit,
    onToggle: () -> Unit, onNext: () -> Unit, onRepeat: () -> Unit, modifier: Modifier = Modifier
) {
    val ink = IvoryDesign.ink
    val onInk = IvoryDesign.onInk
    Row(modifier.fillMaxWidth().height(84.dp), verticalAlignment = Alignment.CenterVertically) {
        IvoryModeTab(true, if (repeat == RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, labels.repeat, repeat != RepeatMode.Off, onRepeat)
        Row(
            Modifier.weight(1f).padding(horizontal = 12.dp).height(72.dp).clip(RoundedCornerShape(36.dp)).background(ink),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Rounded.KeyboardDoubleArrowLeft, labels.previous, tint = onInk, modifier = Modifier.size(34.dp))
            }
            Box(
                Modifier.size(60.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onToggle)
                    .semantics { contentDescription = if (playing) labels.pause else labels.play },
                contentAlignment = Alignment.Center
            ) {
                if (resolving) LevyraLoadingIndicator(color = onInk, modifier = Modifier.size(28.dp))
                else Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.ChevronRight, null, tint = onInk, modifier = Modifier.size(if (playing) 34.dp else 44.dp))
            }
            IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Rounded.KeyboardDoubleArrowRight, labels.next, tint = onInk, modifier = Modifier.size(34.dp))
            }
        }
        IvoryModeTab(false, Icons.Rounded.Shuffle, labels.shuffle, shuffle, onShuffle)
    }
}

@Composable
private fun IvoryModeTab(start: Boolean, icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.width(54.dp).height(64.dp).clip(ivoryEdgeTabShape(start, 0.16f))
            .background(if (active) IvoryDesign.ink else LevyraPanelSoft)
            .toggleable(value = active, role = Role.Switch, onValueChange = { onClick() }),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, label, tint = if (active) IvoryDesign.onInk else LevyraText.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = if (start) 0.dp else 6.dp, end = if (start) 6.dp else 0.dp).size(22.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun IvorySeekbar(positionMs: Long, bufferedMs: Long, durationMs: Long, onSeek: (Float) -> Unit, modifier: Modifier = Modifier) {
    var dragging by remember(durationMs) { mutableStateOf<Float?>(null) }
    val fraction = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val sliderState = rememberSliderState(value = dragging ?: fraction, trackRange = 0f..1f)
    val seekLabel = LocalLevyraStrings.current.gestureActionSeek
    val shape = RoundedCornerShape(2.dp)
    SideEffect { sliderState.value = dragging ?: fraction }
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatSeekbarMillis(dragging?.let { (durationMs * it).toLong() } ?: positionMs), style = MaterialTheme.typography.labelMedium, color = LevyraText)
            Text(formatSeekbarMillis(durationMs), style = MaterialTheme.typography.labelMedium, color = LevyraMuted)
        }
        Slider(
            state = sliderState, onValueChange = { dragging = it },
            onValueChangeFinished = { dragging?.let(onSeek); dragging = null },
            enabled = durationMs > 0L, modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp).semantics { contentDescription = seekLabel },
            thumb = {
                Box(Modifier.size(if (dragging != null) 16.dp else 8.dp).clip(CircleShape).background(IvoryDesign.rose))
            },
            track = { state ->
                Box(Modifier.fillMaxWidth().height(PrismDesign.seekbarHeight).clip(shape).background(LevyraPanelSoft)) {
                    val buffered = if (durationMs > 0L) (bufferedMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
                    Box(Modifier.fillMaxWidth(buffered).fillMaxHeight().background(LevyraText.copy(alpha = 0.14f)))
                    Box(Modifier.fillMaxWidth(state.value.coerceIn(0f, 1f)).fillMaxHeight().clip(shape).background(IvoryDesign.ink))
                }
            }
        )
    }
}
