package com.luc4n3x.levyra.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import com.luc4n3x.levyra.domain.PrismPlayerLayout
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.PlayerControlLabels
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.formatSeekbarMillis
import com.luc4n3x.levyra.ui.theme.*
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings

@Composable
internal fun PrismPlayerScene(
    track: Track, slots: PlayerDeckSlots, artworkCap: Dp,
    metadata: @Composable () -> Unit, transport: @Composable () -> Unit, modifier: Modifier = Modifier
) {
    val focus = activePrismSettings.playerLayout == PrismPlayerLayout.Focus
    BoxWithConstraints(modifier.statusBarsPadding().navigationBarsPadding()) {
        val artSize = minOf(maxWidth - PrismDesign.gutter * 2, maxHeight * if (focus) 0.48f else 0.40f, artworkCap)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = PrismDesign.gutter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            slots.header()
            Spacer(Modifier.height(if (focus) 20.dp else 10.dp))
            slots.stage(track, Modifier.size(artSize).align(Alignment.CenterHorizontally), PrismDesign.artworkCorner)
            Spacer(Modifier.height(if (focus) 28.dp else 20.dp))
            if (focus) {
                slots.progress(PlayerProgressStyle.Classic)
                Spacer(Modifier.height(16.dp))
                metadata()
            } else {
                metadata()
                Spacer(Modifier.height(8.dp))
                slots.engagement(track)
                slots.progress(PlayerProgressStyle.Classic)
            }
            Spacer(Modifier.height(12.dp))
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { transport() }
            Spacer(Modifier.height(20.dp))
            slots.dock(track)
            if (focus) {
                Spacer(Modifier.height(12.dp))
                slots.engagement(track)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
internal fun PrismTransport(
    playing: Boolean, resolving: Boolean, shuffle: Boolean, repeat: RepeatMode,
    labels: PlayerControlLabels, onShuffle: () -> Unit, onPrevious: () -> Unit,
    onToggle: () -> Unit, onNext: () -> Unit, onRepeat: () -> Unit, modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth().heightIn(min = 80.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        PrismModeButton(Icons.Rounded.Shuffle, labels.shuffle, shuffle, onShuffle)
        IconButton(onClick = onPrevious, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Rounded.SkipPrevious, labels.previous, tint = colors.onSurface, modifier = Modifier.size(30.dp))
        }
        FilledIconButton(
            onClick = onToggle, modifier = Modifier.size(72.dp).semantics { contentDescription = if (playing) labels.pause else labels.play }, shape = RoundedCornerShape(24.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.primary, contentColor = colors.onPrimary)
        ) {
            if (resolving) LevyraLoadingIndicator(color = colors.onPrimary, modifier = Modifier.size(28.dp))
            else Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, modifier = Modifier.size(34.dp))
        }
        IconButton(onClick = onNext, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Rounded.SkipNext, labels.next, tint = colors.onSurface, modifier = Modifier.size(30.dp))
        }
        PrismModeButton(if (repeat == RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, labels.repeat, repeat != RepeatMode.Off, onRepeat)
    }
}

@Composable
private fun PrismModeButton(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    OutlinedIconButton(
        onClick = onClick, modifier = Modifier.size(48.dp).semantics { selected = active },
        shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, if (active) colors.primary else colors.outlineVariant),
        colors = IconButtonDefaults.outlinedIconButtonColors(
            containerColor = if (active) colors.primaryContainer else colors.surfaceContainerLow,
            contentColor = if (active) colors.onPrimaryContainer else colors.onSurfaceVariant
        )
    ) { Icon(icon, label, modifier = Modifier.size(22.dp)) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PrismSeekbar(positionMs: Long, bufferedMs: Long, durationMs: Long, onSeek: (Float) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    var dragging by remember(durationMs) { mutableStateOf<Float?>(null) }
    val fraction = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val sliderState = rememberSliderState(value = dragging ?: fraction, trackRange = 0f..1f)
    val seekLabel = LocalLevyraStrings.current.gestureActionSeek
    SideEffect { sliderState.value = dragging ?: fraction }
    Column(modifier.fillMaxWidth()) {
        Slider(
            state = sliderState, onValueChange = { dragging = it },
            onValueChangeFinished = { dragging?.let(onSeek); dragging = null },
            enabled = durationMs > 0L, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { contentDescription = seekLabel },
            colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary, inactiveTrackColor = colors.surfaceContainerHighest),
            thumb = { Surface(Modifier.size(4.dp, 20.dp), shape = RoundedCornerShape(2.dp), color = colors.primary) {} },
            track = { state ->
                Box(Modifier.fillMaxWidth().height(4.dp).background(colors.surfaceContainerHighest, RoundedCornerShape(2.dp))) {
                    val buffered = if (durationMs > 0L) (bufferedMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
                    Box(Modifier.fillMaxWidth(buffered).fillMaxHeight().background(colors.outlineVariant, RoundedCornerShape(2.dp)))
                    Box(Modifier.fillMaxWidth(state.value.coerceIn(0f, 1f)).fillMaxHeight().background(Brush.horizontalGradient(listOf(LevyraCyan, LevyraViolet)), RoundedCornerShape(2.dp)))
                }
            }
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatSeekbarMillis(dragging?.let { (durationMs * it).toLong() } ?: positionMs), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            Text(formatSeekbarMillis(durationMs), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
    }
}
