package com.luc4n3x.levyra.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lyrics
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.audio.rememberLevyraAudioOutputState
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.i18n.LevyraQobuzSkinStrings
import com.luc4n3x.levyra.ui.components.PlayerControlLabels
import com.luc4n3x.levyra.ui.components.formatSeekbarMillis
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.rememberSpotifyCoverTone
import com.luc4n3x.levyra.ui.theme.QobuzDesign

internal fun qobuzPlayerField(seed: Color): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(seed.toArgb(), hsl)
    hsl[1] = hsl[1].coerceAtLeast(0.4f)
    hsl[2] = hsl[2].coerceIn(0.2f, 0.35f)
    return Color(ColorUtils.HSLToColor(hsl))
}

@Composable
internal fun QobuzPlayerScene(
    track: Track, slots: PlayerDeckSlots, artworkCap: Dp,
    metadata: @Composable () -> Unit, transport: @Composable () -> Unit, modifier: Modifier = Modifier
) {
    val field by rememberSpotifyCoverTone(track, ::qobuzPlayerField)
    Column(modifier.fillMaxSize().background(field).statusBarsPadding().navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.width(40.dp).height(5.dp).clip(RoundedCornerShape(50)).background(QobuzDesign.playerContentTertiary))
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { slots.modeSwitch() }
        BoxWithConstraints(
            Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            slots.stage(track, Modifier.size(minOf(maxWidth, maxHeight, artworkCap)), QobuzDesign.playerArtworkCorner)
        }
        QobuzPlayerPills(Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { metadata() }
            slots.headerTrailing()
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.padding(horizontal = 16.dp)) { slots.progress(PlayerProgressStyle.Classic) }
        Spacer(Modifier.height(16.dp))
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(Modifier.padding(horizontal = 16.dp)) { transport() }
        }
        Spacer(Modifier.height(24.dp))
        QobuzPlayerTabs(track, slots)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun QobuzPlayerPills(modifier: Modifier) {
    val runtime by rememberPlayerAudioSpec()
    val output = rememberLevyraAudioOutputState()
    val quality = remember(runtime) {
        listOfNotNull(
            runtime.codec.takeIf { it.isNotBlank() }?.uppercase(),
            runtime.sampleRateHz?.takeIf { it > 0 }?.let(::formatTechnicalSampleRate),
            runtime.bitrateKbps?.takeIf { it > 0 }?.let { "$it kbps" }?.takeIf { runtime.sampleRateHz == null }
        ).joinToString(" / ")
    }
    val device = output.active?.displayName.orEmpty()
    if (quality.isBlank() && device.isBlank()) return
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        if (quality.isNotBlank()) QobuzPill(null, quality, Modifier.weight(1f, fill = false))
        if (device.isNotBlank()) QobuzPill(Icons.Outlined.PhoneAndroid, device, Modifier.weight(1f, fill = false))
    }
}

@Composable
private fun QobuzPill(icon: ImageVector?, text: String, modifier: Modifier) {
    Row(
        modifier.heightIn(min = QobuzDesign.playerPill).clip(RoundedCornerShape(50))
            .background(QobuzDesign.playerFill).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = QobuzDesign.playerContent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, color = QobuzDesign.playerContent, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun QobuzPlayerTabs(track: Track, slots: PlayerDeckSlots) {
    val strings = LocalLevyraStrings.current
    val actions = slots.dockActions(track)
    val lyrics = actions.firstOrNull { it.key == "lyrics" }
    val queue = actions.firstOrNull { it.key == "queue" }
    val panel = slots.artistPanel?.takeIf { it.trackId == track.id }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
        panel?.let { QobuzTabButton(Icons.Outlined.AutoAwesome, strings.related, false, it.onOpenArtist) }
        lyrics?.let { QobuzTabButton(Icons.Outlined.Lyrics, it.label, it.active, it.onClick) }
        panel?.let { QobuzTabButton(Icons.Outlined.Info, LevyraQobuzSkinStrings.forCode(strings.code).credits, false, it.onCredits) }
        queue?.let { QobuzTabButton(Icons.AutoMirrored.Outlined.QueueMusic, it.label, it.active, it.onClick) }
    }
}

@Composable
private fun QobuzTabButton(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(56.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, tint = if (active) QobuzDesign.playerContent else QobuzDesign.playerContentSecondary, modifier = Modifier.size(28.dp))
    }
}

@Composable
internal fun QobuzPlayerFavoriteButton(isFavorite: Boolean, label: String, onToggle: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).toggleable(value = isFavorite, role = Role.Switch, onValueChange = { onToggle() })
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Icon(if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null, tint = QobuzDesign.playerContent, modifier = Modifier.size(26.dp))
    }
}

@Composable
internal fun QobuzTransport(
    playing: Boolean, resolving: Boolean, shuffle: Boolean, repeat: RepeatMode,
    labels: PlayerControlLabels, onShuffle: () -> Unit, onPrevious: () -> Unit,
    onToggle: () -> Unit, onNext: () -> Unit, onRepeat: () -> Unit, modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth().heightIn(min = 80.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        QobuzModeButton(Icons.Rounded.Shuffle, labels.shuffle, shuffle, onShuffle)
        QobuzControl(Icons.Rounded.SkipPrevious, labels.previous, 48.dp, onPrevious)
        Box(
            Modifier.size(72.dp).clip(CircleShape).clickable(role = Role.Button, enabled = !resolving, onClick = onToggle)
                .semantics { contentDescription = if (playing) labels.pause else labels.play },
            contentAlignment = Alignment.Center
        ) {
            if (resolving) LevyraLoadingIndicator(Modifier.size(32.dp), QobuzDesign.playerContent)
            else Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = QobuzDesign.playerContent, modifier = Modifier.size(64.dp))
        }
        QobuzControl(Icons.Rounded.SkipNext, labels.next, 48.dp, onNext)
        QobuzModeButton(if (repeat == RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, labels.repeat, repeat != RepeatMode.Off, onRepeat)
    }
}

@Composable
private fun QobuzControl(icon: ImageVector, label: String, size: Dp, onClick: () -> Unit) {
    Box(Modifier.size(56.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = label, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = QobuzDesign.playerContent, modifier = Modifier.size(size))
    }
}

@Composable
private fun QobuzModeButton(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).toggleable(value = active, role = Role.Switch, onValueChange = { onClick() }),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, tint = if (active) QobuzDesign.hiResGold else QobuzDesign.playerContent, modifier = Modifier.size(24.dp))
    }
}

@Composable
internal fun QobuzSeekbar(positionMs: Long, durationMs: Long, onSeek: (Float) -> Unit, modifier: Modifier = Modifier) {
    var dragging by remember(durationMs) { mutableStateOf<Float?>(null) }
    val fraction = dragging ?: if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val seekLabel = LocalLevyraStrings.current.gestureActionSeek
    Column(modifier.fillMaxWidth()) {
        androidx.compose.foundation.Canvas(
            Modifier.fillMaxWidth().height(28.dp).semantics {
                contentDescription = seekLabel
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                if (durationMs > 0L) setProgress { target -> onSeek(target.coerceIn(0f, 1f)); true }
            }
                .pointerInput(durationMs) {
                    if (durationMs <= 0L) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = { dragging = (it.x / size.width).coerceIn(0f, 1f) },
                        onDragEnd = { dragging?.let(onSeek); dragging = null },
                        onDragCancel = { dragging = null }
                    ) { change, _ -> dragging = (change.position.x / size.width).coerceIn(0f, 1f) }
                }
                .pointerInput(durationMs) {
                    if (durationMs <= 0L) return@pointerInput
                    detectTapGestures { onSeek((it.x / size.width).coerceIn(0f, 1f)) }
                }
        ) {
            val track = (if (dragging != null) 8.dp else 6.dp).toPx()
            val top = (size.height - track) / 2f
            val radius = CornerRadius(track / 2f)
            drawRoundRect(QobuzDesign.playerFillStrong, Offset(0f, top), Size(size.width, track), radius)
            drawRoundRect(QobuzDesign.playerContent, Offset(0f, top), Size(size.width * fraction, track), radius)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val shown = dragging?.let { (durationMs * it).toLong() } ?: positionMs
            Text(formatSeekbarMillis(shown), style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium), color = QobuzDesign.playerContentSecondary)
            Text("-" + formatSeekbarMillis((durationMs - shown).coerceAtLeast(0L)), style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium), color = QobuzDesign.playerContentSecondary)
        }
    }
}

@Composable
internal fun QobuzPlayerMoreButton(label: String, onClick: () -> Unit) {
    Box(Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = label, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.MoreVert, label, tint = QobuzDesign.playerContent, modifier = Modifier.size(24.dp))
    }
}

@Composable
internal fun QobuzModeSwitch(videoMode: Boolean, songLabel: String, videoLabel: String, onSong: () -> Unit, onVideo: () -> Unit) {
    Row(
        Modifier.padding(vertical = 4.dp).clip(RoundedCornerShape(50)).background(QobuzDesign.playerFill).padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        QobuzModeOption(songLabel, !videoMode, onSong)
        QobuzModeOption(videoLabel, videoMode, onVideo)
    }
}

@Composable
private fun QobuzModeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.heightIn(min = 36.dp).clip(RoundedCornerShape(50)).background(if (selected) Color.White else Color.Transparent)
            .toggleable(value = selected, role = Role.Tab, onValueChange = { if (!selected) onClick() }).padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
            color = if (selected) Color.Black else QobuzDesign.playerContent)
    }
}
