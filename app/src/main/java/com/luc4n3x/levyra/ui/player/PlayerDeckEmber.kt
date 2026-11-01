package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.PlayerDarkSurface
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPlayPauseGlyph
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.PlayerControlLabels
import com.luc4n3x.levyra.ui.components.PlayerIcon
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.playerMix
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics

private val EmberStageCorner = 22.dp
private val EmberScrollingStage = 320.dp
private val EmberTitleCorner = 12.dp
private val EmberSeekHeight = 56.dp
private const val EmberSeekBarCount = 48
private const val EmberSeekGapRatio = 0.32f
private const val EmberSeekFloor = 0.08f
private const val EmberUnplayedAlpha = 0.38f
private const val EmberTopTint = 0.55f
private const val EmberBottomDepth = 0.9f
private val EmberSpecStyle = TextStyle(fontFeatureSettings = "tnum")

private const val EmberActionLyrics = "lyrics"
private const val EmberActionQueue = "queue"
private const val EmberActionDownload = "download"
private const val EmberActionDeck = "visual-mode"

@Composable
internal fun PlayerEmberDeck(
    track: Track,
    slots: PlayerDeckSlots,
    surfaces: PlayerSurfaceTokens,
    accent: Color,
    isFavorite: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    shuffleOn: Boolean,
    repeatMode: RepeatMode,
    labels: PlayerControlLabels,
    animated: Boolean,
    compact: Boolean,
    scrollable: Boolean,
    gutter: Dp,
    artworkCap: Dp,
    stepDirection: Int,
    onArtistClick: (Int) -> Unit,
    onToggleFavorite: () -> Unit,
    onShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
    modifier: Modifier = Modifier,
    headlineModifier: Modifier = Modifier
) {
    val tone by animateColorAsState(
        targetValue = accent,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.paletteTween()),
        label = "player-ember-tone"
    )
    val top = if (surfaces.amoled) Color.Black else tone.playerMix(PlayerDarkSurface, EmberTopTint)
    val bottom = if (surfaces.amoled) Color.Black else tone.playerMix(PlayerDarkSurface, EmberBottomDepth)
    val gap = if (compact) LevyraPlayerDesign.SpaceSm else LevyraPlayerDesign.SpaceMd
    Column(
        modifier = modifier
            .drawBehind { drawRect(Brush.verticalGradient(listOf(top, bottom))) }
            .statusBarsPadding()
            .navigationBarsPadding()
            .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(start = gutter, end = gutter, top = LevyraPlayerDesign.SpaceXs, bottom = gap),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        slots.header()
        EmberStage(
            track = track,
            slots = slots,
            artworkCap = artworkCap,
            modifier = emberStageModifier(scrollable, gap)
        )
        EmberSpecLine(color = surfaces.contentFaint)
        Spacer(Modifier.height(gap))
        EmberHeadline(
            track = track,
            tone = tone,
            surfaces = surfaces,
            animated = animated,
            compact = compact,
            stepDirection = stepDirection,
            onArtistClick = onArtistClick,
            modifier = headlineModifier.fillMaxWidth()
        )
        slots.engagement(track)
        Spacer(Modifier.height(gap))
        slots.progress(PlayerProgressStyle.Bars)
        EmberActions(
            track = track,
            slots = slots,
            surfaces = surfaces,
            tone = tone,
            isFavorite = isFavorite,
            shuffleOn = shuffleOn,
            onShuffle = onShuffle,
            onToggleFavorite = onToggleFavorite
        )
        Spacer(Modifier.height(gap))
        EmberTransport(
            isPlaying = isPlaying,
            isResolving = isResolving,
            repeatMode = repeatMode,
            labels = labels,
            tone = tone,
            surfaces = surfaces,
            compact = compact,
            animated = animated,
            onRepeat = onRepeat,
            onPrevious = onPrevious,
            onTogglePlay = onTogglePlay,
            onNext = onNext
        )
    }
}

private fun ColumnScope.emberStageModifier(scrollable: Boolean, verticalPadding: Dp): Modifier = Modifier
    .fillMaxWidth()
    .then(if (scrollable) Modifier.height(EmberScrollingStage) else Modifier.weight(1f))
    .padding(vertical = verticalPadding)

@Composable
private fun EmberStage(
    track: Track,
    slots: PlayerDeckSlots,
    artworkCap: Dp,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight, artworkCap)
        Box(modifier = Modifier.size(side)) {
            slots.stage(track, Modifier.size(side), EmberStageCorner)
            slots.gestures(track, Modifier.matchParentSize())
        }
    }
}

@Composable
private fun EmberSpecLine(color: Color) {
    val spec by rememberPlayerAudioSpec()
    val label = remember(spec) { playerAudioSpecLabels(spec).joinToString("  •  ") }
    Text(
        text = label,
        color = color,
        fontSize = 13.sp,
        letterSpacing = 0.6.sp,
        style = EmberSpecStyle,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().heightIn(min = 18.dp)
    )
}

@Composable
private fun EmberHeadline(
    track: Track,
    tone: Color,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean,
    compact: Boolean,
    stepDirection: Int,
    onArtistClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    AnimatedContent(
        targetState = track,
        modifier = modifier,
        transitionSpec = { LevyraMotion.trackChange(animated, stepDirection) },
        contentKey = { it.id },
        label = "player-ember-headline"
    ) { shown ->
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = shown.title,
                    color = surfaces.content,
                    fontSize = if (compact) 22.sp else 26.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(EmberTitleCorner))
                        .background(Color.Black.copy(alpha = 0.18f))
                        .padding(horizontal = LevyraPlayerDesign.SpaceMd, vertical = LevyraPlayerDesign.SpaceXs)
                        .semantics { heading() }
                )
                Spacer(Modifier.size(LevyraPlayerDesign.SpaceMd))
                PlayerIcon(Icons.Rounded.GraphicEq, tone.playerMix(Color.White, 0.25f), Modifier.size(24.dp))
            }
            Spacer(Modifier.height(LevyraPlayerDesign.SpaceSm))
            PlayerArtistText(
                track = shown,
                color = surfaces.contentMuted,
                style = TextStyle(fontSize = if (compact) 18.sp else 20.sp, fontWeight = FontWeight.Medium),
                onClickLabel = strings.openArtist,
                onArtistClick = onArtistClick,
                enabled = shown.id == track.id,
                modifier = Modifier.padding(horizontal = LevyraPlayerDesign.SpaceMd)
            )
            val album = shown.album.trim()
            if (album.isNotEmpty()) {
                Spacer(Modifier.height(LevyraPlayerDesign.SpaceSm))
                Text(
                    text = album,
                    color = surfaces.content,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = LevyraPlayerDesign.SpaceMd)
                )
            }
        }
    }
}

@Composable
private fun EmberActions(
    track: Track,
    slots: PlayerDeckSlots,
    surfaces: PlayerSurfaceTokens,
    tone: Color,
    isFavorite: Boolean,
    shuffleOn: Boolean,
    onShuffle: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val dockActions = slots.dockActions(track)
    val byKey = dockActions.associateBy { it.key }
    val highlight = tone.playerMix(Color.White, 0.3f)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        byKey[EmberActionQueue]?.let { EmberIconAction(it.icon, it.label, surfaces.content, onClick = it.onClick) }
        byKey[EmberActionLyrics]?.let {
            EmberIconAction(it.icon, it.label, if (it.active) highlight else surfaces.content, toggled = it.active, onClick = it.onClick)
        }
        EmberIconAction(
            icon = Icons.Rounded.Shuffle,
            label = strings.shuffle,
            tint = if (shuffleOn) highlight else surfaces.content,
            toggled = shuffleOn,
            onClick = onShuffle
        )
        EmberIconAction(
            icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            label = strings.favoritesPlain,
            tint = surfaces.content,
            toggled = isFavorite,
            haptic = LevyraHapticAction.Confirm,
            onClick = onToggleFavorite
        )
        byKey[EmberActionDownload]?.let {
            EmberIconAction(
                it.icon,
                it.label,
                if (it.active) highlight else surfaces.content,
                enabled = it.enabled && !it.busy,
                onClick = it.onClick
            )
        }
        byKey[EmberActionDeck]?.let { EmberIconAction(it.icon, it.label, surfaces.content, onClick = it.onClick) }
    }
}

@Composable
private fun EmberIconAction(
    icon: ImageVector,
    label: String,
    tint: Color,
    toggled: Boolean? = null,
    enabled: Boolean = true,
    haptic: LevyraHapticAction = LevyraHapticAction.Transport,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(LevyraPlayerDesign.MinimumTouchTarget)
            .clip(CircleShape)
            .levyraPressable(
                onClick = onClick,
                enabled = enabled,
                pressedScale = LevyraPressScale.Control,
                role = if (toggled != null) Role.Switch else Role.Button,
                haptic = haptic
            )
            .semantics {
                contentDescription = label
                if (toggled != null) toggleableState = ToggleableState(toggled)
            },
        contentAlignment = Alignment.Center
    ) {
        PlayerIcon(icon, if (enabled) tint else tint.copy(alpha = 0.4f), Modifier.size(28.dp))
    }
}

@Composable
private fun EmberTransport(
    isPlaying: Boolean,
    isResolving: Boolean,
    repeatMode: RepeatMode,
    labels: PlayerControlLabels,
    tone: Color,
    surfaces: PlayerSurfaceTokens,
    compact: Boolean,
    animated: Boolean,
    onRepeat: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit
) {
    val haptics = LocalLevyraHaptics.current
    val button = tone.playerMix(Color.White, 0.2f)
    val buttonContent = surfaces.heroContent
    val play = if (compact) 84.dp else 100.dp
    val skip = if (compact) 56.dp else 64.dp
    val playCorner by animateDpAsState(
        targetValue = if (isPlaying) 26.dp else play / 2,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.expressiveSpring()),
        label = "player-ember-play-corner"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceMd, Alignment.End),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EmberIconAction(
            icon = if (repeatMode == RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
            label = labels.repeat,
            tint = if (repeatMode != RepeatMode.Off) tone.playerMix(Color.White, 0.3f) else surfaces.content,
            toggled = repeatMode != RepeatMode.Off,
            onClick = onRepeat
        )
        EmberRoundButton(Icons.Rounded.SkipPrevious, labels.previous, skip, button, buttonContent) {
            haptics.perform(LevyraHapticAction.Transport)
            onPrevious()
        }
        EmberRoundButton(Icons.Rounded.SkipNext, labels.next, skip * 1.2f, button, buttonContent) {
            haptics.perform(LevyraHapticAction.Transport)
            onNext()
        }
        Box(
            modifier = Modifier
                .size(play)
                .clip(RoundedCornerShape(playCorner))
                .background(button)
                .levyraPressable(
                    onClick = {
                        haptics.perform(LevyraHapticAction.Transport)
                        onTogglePlay()
                    },
                    pressedScale = LevyraPressScale.Control,
                    role = Role.Button
                )
                .semantics { contentDescription = if (isPlaying) labels.pause else labels.play },
            contentAlignment = Alignment.Center
        ) {
            if (isResolving) {
                LevyraLoadingIndicator(color = buttonContent, modifier = Modifier.size(play * 0.42f))
            } else {
                LevyraPlayPauseGlyph(
                    playing = isPlaying,
                    color = buttonContent,
                    modifier = Modifier.size(play * 0.44f),
                    animateMorph = animated
                )
            }
        }
    }
}

@Composable
private fun EmberRoundButton(
    icon: ImageVector,
    label: String,
    size: Dp,
    container: Color,
    content: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(container)
            .levyraPressable(onClick = onClick, pressedScale = LevyraPressScale.Control, role = Role.Button)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        PlayerIcon(icon, content, Modifier.size(size * 0.42f))
    }
}

internal fun emberSeekBars(waveform: FloatArray?, seed: String): FloatArray {
    if (waveform != null && waveform.isNotEmpty()) {
        return FloatArray(EmberSeekBarCount) { index ->
            val start = index * waveform.size / EmberSeekBarCount
            val end = ((index + 1) * waveform.size / EmberSeekBarCount).coerceAtLeast(start + 1)
            var peak = 0f
            for (source in start until end.coerceAtMost(waveform.size)) peak = maxOf(peak, waveform[source])
            peak.coerceIn(EmberSeekFloor, 1f)
        }
    }
    var state = seed.hashCode().toLong() xor 0x5DEECE66DL
    return FloatArray(EmberSeekBarCount) { index ->
        state = (state * 0x5DEECE66DL + 0xBL) and ((1L shl 48) - 1)
        val noise = (state ushr 17).toInt() % 1000 / 1000f
        val ramp = when {
            index < 4 -> 0.15f + index * 0.12f
            index > EmberSeekBarCount - 4 -> 0.5f
            else -> 1f
        }
        ((0.55f + 0.45f * noise) * ramp).coerceIn(EmberSeekFloor, 1f)
    }
}

@Composable
internal fun EmberWaveSeek(
    bars: FloatArray,
    positionMs: Long,
    durationMs: Long,
    activeColor: Color,
    interactionKey: String,
    onSeek: (Float) -> Unit
) {
    var dragging by remember(interactionKey) { mutableStateOf(false) }
    var dragFraction by remember(interactionKey) { mutableFloatStateOf(0f) }
    val playedFraction = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val fraction = if (dragging) dragFraction else playedFraction
    val enabled = durationMs > 0L
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = LevyraPlayerDesign.SpaceMd)
            .height(EmberSeekHeight)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                if (enabled) {
                    setProgress { target ->
                        onSeek(target.coerceIn(0f, 1f))
                        true
                    }
                }
            }
            .pointerInput(interactionKey, enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset -> onSeek((offset.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(interactionKey, enabled) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        onSeek(dragFraction)
                        dragging = false
                    },
                    onDragCancel = { dragging = false },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                    }
                )
            }
    ) {
        if (bars.isEmpty()) return@Canvas
        val slot = size.width / bars.size
        val barWidth = slot * (1f - EmberSeekGapRatio)
        val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
        val unplayed = activeColor.copy(alpha = EmberUnplayedAlpha)
        bars.forEachIndexed { index, value ->
            val height = size.height * value
            val left = index * slot + (slot - barWidth) / 2f
            val center = (index + 0.5f) / bars.size
            drawRoundRect(
                color = if (center <= fraction) activeColor else unplayed,
                topLeft = Offset(left, size.height - height),
                size = Size(barWidth, height),
                cornerRadius = radius
            )
        }
    }
}
