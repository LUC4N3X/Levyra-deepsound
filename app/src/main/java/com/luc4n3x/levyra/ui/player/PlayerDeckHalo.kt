package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPlayPauseGlyph
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.PlayerControlLabels
import com.luc4n3x.levyra.ui.components.PlayerIcon
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.components.rememberLevyraPlayMorphShape
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics

private val HaloArtworkCorner = 40.dp
private val HaloToolbarHeight = 80.dp
private val HaloFabSize = 64.dp
private val HaloScrollingStage = 300.dp

@Composable
internal fun PlayerHaloDeck(
    track: Track,
    slots: PlayerDeckSlots,
    surfaces: PlayerSurfaceTokens,
    isFavorite: Boolean,
    queuePosition: PlayerDeckQueuePosition?,
    isPlaying: Boolean,
    isResolving: Boolean,
    shuffleOn: Boolean,
    repeatMode: RepeatMode,
    labels: PlayerControlLabels,
    animated: Boolean,
    scrollable: Boolean,
    gutter: Dp,
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
    val halo by animateColorAsState(
        targetValue = surfaces.hero,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.paletteTween()),
        label = "player-halo-glow"
    )
    Column(
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(start = gutter, end = gutter, top = LevyraPlayerDesign.SpaceXs, bottom = LevyraPlayerDesign.SpaceLg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        slots.header()
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (scrollable) Modifier.height(HaloScrollingStage) else Modifier.weight(1f))
                .padding(vertical = LevyraPlayerDesign.SpaceLg)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(halo.copy(alpha = 0.42f), halo.copy(alpha = 0.12f), Color.Transparent),
                            center = center,
                            radius = size.minDimension * 0.72f
                        ),
                        radius = size.minDimension * 0.72f,
                        center = center
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            val side = minOf(maxWidth, maxHeight) * 0.9f
            Box(modifier = Modifier.size(side)) {
                slots.stage(track, Modifier.size(side), HaloArtworkCorner)
                if (queuePosition != null) {
                    HaloQueueIndex(
                        position = queuePosition,
                        surfaces = surfaces,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = (-10).dp, y = 22.dp)
                    )
                }
            }
            slots.gestures(track, Modifier.matchParentSize())
        }
        HaloHeadline(
            track = track,
            surfaces = surfaces,
            isFavorite = isFavorite,
            animated = animated,
            onArtistClick = onArtistClick,
            onToggleFavorite = onToggleFavorite,
            modifier = headlineModifier
        )
        Spacer(Modifier.height(LevyraPlayerDesign.SpaceSm))
        slots.progress(PlayerProgressStyle.Wavy)
        Spacer(Modifier.height(LevyraPlayerDesign.SpaceSm))
        HaloToolbar(
            surfaces = surfaces,
            isPlaying = isPlaying,
            isResolving = isResolving,
            shuffleOn = shuffleOn,
            repeatMode = repeatMode,
            labels = labels,
            animated = animated,
            onShuffle = onShuffle,
            onPrevious = onPrevious,
            onTogglePlay = onTogglePlay,
            onNext = onNext,
            onRepeat = onRepeat
        )
        Spacer(Modifier.height(LevyraPlayerDesign.SpaceLg))
        slots.dock(track)
    }
}

@Composable
private fun HaloQueueIndex(position: PlayerDeckQueuePosition, surfaces: PlayerSurfaceTokens, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        Text(
            text = position.indexLabel,
            color = surfaces.content,
            style = TextStyle(fontSize = 58.sp, lineHeight = 58.sp, fontWeight = FontWeight.Thin, letterSpacing = (-2).sp, fontFeatureSettings = "tnum, lnum")
        )
        Text(
            text = " / ${position.totalLabel}",
            color = surfaces.contentMuted,
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp, fontFeatureSettings = "tnum"),
            modifier = Modifier.padding(bottom = 10.dp)
        )
    }
}

@Composable
private fun HaloHeadline(
    track: Track,
    surfaces: PlayerSurfaceTokens,
    isFavorite: Boolean,
    animated: Boolean,
    onArtistClick: (Int) -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        AnimatedContent(
            targetState = track,
            modifier = Modifier.weight(1f),
            transitionSpec = { LevyraMotion.contentSwap(animated) },
            contentKey = { it.id },
            label = "player-halo-headline"
        ) { shown ->
            Column {
                val deckline = haloDeckline(shown)
                if (deckline.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(width = 18.dp, height = 1.5.dp)
                                .background(surfaces.contentMuted)
                        )
                        Text(
                            text = deckline.uppercase(),
                            color = surfaces.contentMuted,
                            style = TextStyle(fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
                Text(
                    text = shown.title,
                    color = surfaces.content,
                    fontSize = 28.sp,
                    lineHeight = 31.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.0).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
                PlayerArtistText(
                    track = shown,
                    color = surfaces.contentMuted,
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                    onClickLabel = strings.openArtist,
                    onArtistClick = onArtistClick,
                    enabled = shown.id == track.id
                )
            }
        }
        PlayerFavoriteButton(
            trackId = track.id,
            isFavorite = isFavorite,
            surfaces = surfaces,
            label = strings.favoritesPlain,
            animated = animated,
            onToggle = onToggleFavorite
        )
    }
}

private fun haloDeckline(track: Track): String =
    listOf(track.album, track.year.ifBlank { track.releaseDate.take(4) })
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
        .joinToString("  ·  ")

@Composable
private fun HaloToolbar(
    surfaces: PlayerSurfaceTokens,
    isPlaying: Boolean,
    isResolving: Boolean,
    shuffleOn: Boolean,
    repeatMode: RepeatMode,
    labels: PlayerControlLabels,
    animated: Boolean,
    onShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit
) {
    val haptics = LocalLevyraHaptics.current
    val container by animateColorAsState(
        targetValue = surfaces.tonal,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.paletteTween()),
        label = "player-halo-toolbar"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(HaloToolbarHeight)
            .shadow(12.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.3f))
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HaloToolbarIcon(Icons.Rounded.Shuffle, labels.shuffle, shuffleOn, surfaces, onShuffle)
        HaloToolbarIcon(Icons.Rounded.SkipPrevious, labels.previous, false, surfaces) {
            haptics.perform(LevyraHapticAction.Transport)
            onPrevious()
        }
        Box(
            modifier = Modifier
                .size(HaloFabSize)
                .clip(rememberLevyraPlayMorphShape(playing = isPlaying, animated = animated, spinWhilePlaying = !isResolving))
                .background(surfaces.hero)
                .levyraPressable(
                    onClick = {
                        haptics.perform(LevyraHapticAction.Transport)
                        onTogglePlay()
                    },
                    role = Role.Button,
                    pressedScale = LevyraPressScale.Control
                )
                .semantics { contentDescription = if (isPlaying) labels.pause else labels.play },
            contentAlignment = Alignment.Center
        ) {
            if (isResolving) {
                LevyraLoadingIndicator(color = surfaces.heroContent, modifier = Modifier.size(34.dp))
            } else {
                LevyraPlayPauseGlyph(playing = isPlaying, color = surfaces.heroContent, modifier = Modifier.size(28.dp))
            }
        }
        HaloToolbarIcon(Icons.Rounded.SkipNext, labels.next, false, surfaces) {
            haptics.perform(LevyraHapticAction.Transport)
            onNext()
        }
        HaloToolbarIcon(
            if (repeatMode == RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
            labels.repeat,
            repeatMode != RepeatMode.Off,
            surfaces,
            onRepeat
        )
    }
}

@Composable
private fun HaloToolbarIcon(
    icon: ImageVector,
    label: String,
    active: Boolean,
    surfaces: PlayerSurfaceTokens,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (active) surfaces.active else Color.Transparent)
            .levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Control)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        PlayerIcon(icon, if (active) surfaces.activeContent else surfaces.content, Modifier.size(26.dp))
    }
}
