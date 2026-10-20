package com.luc4n3x.levyra.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
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
import com.luc4n3x.levyra.ui.components.PlayerControlLabels
import com.luc4n3x.levyra.ui.components.formatSeekbarMillis
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.*

private val TorqueBandShape = GenericShape { size, _ ->
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height * 0.42f)
    lineTo(0f, size.height * 0.92f)
    close()
}

private const val TorqueDiagonalCenter = 0.67f

@Composable
internal fun TorquePlayerScene(track: Track, slots: PlayerDeckSlots, metadata: @Composable () -> Unit, transport: @Composable () -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxSize().background(LevyraBlack)) {
        val coverHeight = minOf(maxWidth, maxHeight * 0.5f)
        val bandHeight = (maxHeight * 0.32f).coerceIn(220.dp, 270.dp)
        val viewport = maxHeight
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(Modifier.fillMaxWidth().heightIn(min = viewport)) {
                Box(Modifier.fillMaxWidth().height(coverHeight)) {
                    slots.stage(track, Modifier.fillMaxSize(), 0.dp)
                    Box(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp)) { slots.header() }
                }
                slots.progress(PlayerProgressStyle.Classic)
                TorqueBand(track, bandHeight, transport)
                Spacer(Modifier.weight(1f))
                Column(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    metadata()
                    slots.engagement(track)
                    slots.dock(track)
                }
            }
        }
    }
}

@Composable
private fun TorqueBand(track: Track, height: Dp, transport: @Composable () -> Unit) {
    val onBand = Color.White
    Box(Modifier.fillMaxWidth().height(height)) {
        Box(Modifier.matchParentSize().clip(TorqueBandShape).background(LevyraActivePalette.blue))
        Column(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                track.title.uppercase(), color = onBand, fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 2.5.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                track.artist, color = onBand.copy(alpha = 0.9f), fontSize = 16.sp, letterSpacing = 1.sp,
                textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Box(Modifier.fillMaxWidth().padding(top = height * TorqueDiagonalCenter - 44.dp, start = 12.dp, end = 12.dp)) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { transport() }
        }
    }
}

@Composable
internal fun TorqueTransport(
    playing: Boolean, resolving: Boolean, shuffle: Boolean, repeat: RepeatMode,
    labels: PlayerControlLabels, onShuffle: () -> Unit, onPrevious: () -> Unit,
    onToggle: () -> Unit, onNext: () -> Unit, onRepeat: () -> Unit, modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth().height(88.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        TorqueModeButton(if (repeat == RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, labels.repeat, repeat != RepeatMode.Off, onRepeat)
        IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Rounded.FastRewind, labels.previous, tint = LevyraText, modifier = Modifier.size(40.dp))
        }
        Box(
            Modifier.size(84.dp).clip(CircleShape).background(LevyraBlack)
                .border(6.dp, LevyraActivePalette.blue, CircleShape)
                .clickable(role = Role.Button, onClick = onToggle)
                .semantics { contentDescription = if (playing) labels.pause else labels.play },
            contentAlignment = Alignment.Center
        ) {
            if (resolving) LevyraLoadingIndicator(color = LevyraText, modifier = Modifier.size(30.dp))
            else Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = LevyraText, modifier = Modifier.size(40.dp))
        }
        IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Rounded.FastForward, labels.next, tint = LevyraText, modifier = Modifier.size(40.dp))
        }
        TorqueModeButton(Icons.Rounded.Shuffle, labels.shuffle, shuffle, onShuffle)
    }
}

@Composable
private fun TorqueModeButton(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).semantics { selected = active }.clickable(role = Role.Switch, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, tint = if (active) LevyraText else LevyraText.copy(alpha = 0.55f), modifier = Modifier.size(24.dp))
        if (active) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp).size(width = 14.dp, height = 2.dp).background(LevyraText))
    }
}

@Composable
internal fun TorqueStrip(positionMs: Long, durationMs: Long, onSeek: (Float) -> Unit, modifier: Modifier = Modifier) {
    var dragging by remember(durationMs) { mutableStateOf<Float?>(null) }
    val fraction = dragging ?: if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val seekLabel = LocalLevyraStrings.current.gestureActionSeek
    val enabled = durationMs > 0L
    val seekGestures = if (enabled) Modifier
        .pointerInput(durationMs) { detectTapGestures { offset -> onSeek((offset.x / size.width).coerceIn(0f, 1f)) } }
        .pointerInput(durationMs) {
            detectHorizontalDragGestures(
                onDragStart = { offset -> dragging = (offset.x / size.width).coerceIn(0f, 1f) },
                onDragEnd = { dragging?.let(onSeek); dragging = null },
                onDragCancel = { dragging = null },
                onHorizontalDrag = { change, _ -> dragging = (change.position.x / size.width).coerceIn(0f, 1f) }
            )
        } else Modifier
    Box(
        modifier.fillMaxWidth().height(44.dp).background(LevyraInk)
            .semantics {
                contentDescription = seekLabel
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                if (enabled) setProgress { target -> onSeek(target.coerceIn(0f, 1f)); true }
            }
            .then(seekGestures)
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).background(LevyraPanelSoft))
        Box(Modifier.align(Alignment.BottomStart).height(2.dp).fillMaxWidth(fraction).background(LevyraActivePalette.blue))
        Row(Modifier.matchParentSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(formatSeekbarMillis(dragging?.let { (durationMs * it).toLong() } ?: positionMs), color = LevyraText, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.weight(1f))
            Text(formatSeekbarMillis(durationMs), color = LevyraMuted, style = MaterialTheme.typography.labelLarge)
        }
    }
}
