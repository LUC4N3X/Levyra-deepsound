package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPlayPauseGlyph
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.PlayerControlLabels
import com.luc4n3x.levyra.ui.components.PlayerIcon
import com.luc4n3x.levyra.ui.components.levyraExpressiveCorner
import com.luc4n3x.levyra.ui.components.levyraExpressiveToggleCorner
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.components.rememberLevyraArtworkMorphShape
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics

private val ExpressiveScrollingStage = 300.dp
private val ExpressiveTransportHeight = 76.dp
private val ExpressiveTransportHeightCompact = 64.dp
private val ExpressiveStageCorner = 0.dp

@Composable
internal fun PlayerExpressiveDeck(
    track: Track,
    slots: PlayerDeckSlots,
    surfaces: PlayerSurfaceTokens,
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
    val metrics = expressiveDeckMetrics(compact)
    Column(
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .expressiveDeckScroll(scrollable)
            .padding(
                start = gutter,
                end = gutter,
                top = LevyraPlayerDesign.SpaceXs,
                bottom = metrics.bottom
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        slots.header()
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (scrollable) Modifier.height(ExpressiveScrollingStage) else Modifier.weight(1f))
                .padding(vertical = metrics.stagePadding),
            contentAlignment = Alignment.Center
        ) {
            val side = minOf(maxWidth, maxHeight)
            val artworkShape = rememberLevyraArtworkMorphShape(playing = isPlaying, animated = animated)
            slots.stage(
                track,
                Modifier
                    .size(side)
                    .clip(artworkShape),
                ExpressiveStageCorner
            )
            slots.gestures(track, Modifier.matchParentSize())
        }
        ExpressiveHeadline(
            linerNotes = slots.linerNotes,
            track = track,
            surfaces = surfaces,
            isFavorite = isFavorite,
            animated = animated,
            compact = compact,
            onArtistClick = onArtistClick,
            onToggleFavorite = onToggleFavorite,
            modifier = headlineModifier
        )
        Spacer(modifier = Modifier.height(metrics.headlineGap))
        slots.engagement(track)
        slots.progress(PlayerProgressStyle.Wavy)
        Spacer(modifier = Modifier.height(metrics.progressGap))
        ExpressiveTransportRow(
            isPlaying = isPlaying,
            isResolving = isResolving,
            labels = labels,
            height = metrics.transportHeight,
            surfaces = surfaces,
            animated = animated,
            onPrevious = onPrevious,
            onTogglePlay = onTogglePlay,
            onNext = onNext
        )
        Spacer(modifier = Modifier.height(metrics.modeGap))
        ExpressiveModeRow(
            track = track,
            slots = slots,
            shuffleOn = shuffleOn,
            repeatMode = repeatMode,
            labels = labels,
            surfaces = surfaces,
            animated = animated,
            onShuffle = onShuffle,
            onRepeat = onRepeat
        )
    }
}

private class ExpressiveDeckMetrics(
    val transportHeight: Dp,
    val bottom: Dp,
    val stagePadding: Dp,
    val headlineGap: Dp,
    val progressGap: Dp,
    val modeGap: Dp
)

private val ExpressiveCompactMetrics = ExpressiveDeckMetrics(
    transportHeight = ExpressiveTransportHeightCompact,
    bottom = LevyraPlayerDesign.SpaceMd,
    stagePadding = LevyraPlayerDesign.SpaceSm,
    headlineGap = LevyraPlayerDesign.SpaceXs,
    progressGap = LevyraPlayerDesign.SpaceXs,
    modeGap = LevyraPlayerDesign.SpaceMd
)

private val ExpressiveRegularMetrics = ExpressiveDeckMetrics(
    transportHeight = ExpressiveTransportHeight,
    bottom = LevyraPlayerDesign.SpaceXl,
    stagePadding = LevyraPlayerDesign.SpaceLg,
    headlineGap = LevyraPlayerDesign.SpaceSm,
    progressGap = LevyraPlayerDesign.SpaceMd,
    modeGap = LevyraPlayerDesign.SpaceLg
)

private fun expressiveDeckMetrics(compact: Boolean): ExpressiveDeckMetrics =
    if (compact) ExpressiveCompactMetrics else ExpressiveRegularMetrics

@Composable
private fun Modifier.expressiveDeckScroll(scrollable: Boolean): Modifier =
    if (scrollable) verticalScroll(rememberScrollState()) else this

@Composable
private fun ExpressiveTransportRow(
    isPlaying: Boolean,
    isResolving: Boolean,
    labels: PlayerControlLabels,
    height: Dp,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
            horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExpressiveRoundButton(
                icon = Icons.Rounded.SkipPrevious,
                label = labels.previous,
                size = height,
                surfaces = surfaces,
                animated = animated,
                onClick = onPrevious
            )
            ExpressivePlayPill(
                isPlaying = isPlaying,
                isResolving = isResolving,
                labels = labels,
                height = height,
                surfaces = surfaces,
                animated = animated,
                onClick = onTogglePlay
            )
            ExpressiveRoundButton(
                icon = Icons.Rounded.SkipNext,
                label = labels.next,
                size = height,
                surfaces = surfaces,
                animated = animated,
                onClick = onNext
            )
        }
    }
}

@Composable
private fun ExpressiveModeRow(
    track: Track,
    slots: PlayerDeckSlots,
    shuffleOn: Boolean,
    repeatMode: RepeatMode,
    labels: PlayerControlLabels,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerToggleControl(
                icon = Icons.Rounded.Shuffle,
                label = labels.shuffle,
                active = shuffleOn,
                toggle = true,
                surfaces = surfaces,
                animated = animated,
                glyph = LevyraPlayerDesign.TransportModeGlyph,
                onClick = onShuffle
            )
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                slots.dock(track)
            }
            PlayerToggleControl(
                icon = if (repeatMode == RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                label = labels.repeat,
                active = repeatMode != RepeatMode.Off,
                toggle = true,
                surfaces = surfaces,
                animated = animated,
                glyph = LevyraPlayerDesign.TransportModeGlyph,
                onClick = onRepeat
            )
        }
    }
}

@Composable
private fun ExpressiveHeadline(
    linerNotes: PlayerLinerNotesState?,
    track: Track,
    surfaces: PlayerSurfaceTokens,
    isFavorite: Boolean,
    animated: Boolean,
    compact: Boolean,
    onArtistClick: (Int) -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    val ink by animateColorAsState(
        targetValue = surfaces.hero,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.paletteTween()),
        label = "player-expressive-ink"
    )
    AnimatedContent(
        targetState = track,
        modifier = modifier,
        transitionSpec = { LevyraMotion.contentSwap(animated) },
        contentKey = { it.id },
        label = "player-expressive-headline"
    ) { shown ->
        val isCurrent = shown.id == track.id
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceMd)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val titleSize = expressiveTitleSize(shown.title, compact)
                Text(
                    text = shown.title,
                    color = ink,
                    fontSize = titleSize,
                    lineHeight = titleSize * 1.02f,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.4).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
                PlayerArtistText(
                    track = shown,
                    color = surfaces.contentMuted,
                    style = TextStyle(
                        fontSize = if (compact) 16.sp else 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.2).sp
                    ),
                    onClickLabel = strings.openArtist,
                    onArtistClick = onArtistClick,
                    enabled = isCurrent
                )
                PlayerLinerNotes(notes = linerNotes, track = shown, color = surfaces.contentMuted)
            }
            if (isCurrent) {
                PlayerFavoriteButton(
                    trackId = shown.id,
                    isFavorite = isFavorite,
                    surfaces = surfaces,
                    label = strings.favoritesPlain,
                    animated = animated,
                    onToggle = onToggleFavorite
                )
            } else {
                Box(modifier = Modifier.size(LevyraPlayerDesign.MinimumTouchTarget))
            }
        }
    }
}

@Composable
private fun RowScope.ExpressivePlayPill(
    isPlaying: Boolean,
    isResolving: Boolean,
    labels: PlayerControlLabels,
    height: Dp,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean,
    onClick: () -> Unit
) {
    val haptics = LocalLevyraHaptics.current
    val interaction = remember { MutableInteractionSource() }
    val restCorner = levyraExpressiveToggleCorner(
        checked = isPlaying,
        unchecked = height * 0.32f,
        checkedCorner = height / 2,
        label = "player-expressive-pill-corner"
    )
    val corner = levyraExpressiveCorner(
        interactionSource = interaction,
        rest = restCorner,
        pressed = height * 0.22f,
        label = "player-expressive-pill-press"
    )
    val fill by animateColorAsState(
        targetValue = surfaces.hero,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.paletteTween()),
        label = "player-expressive-pill-fill"
    )
    val content by animateColorAsState(
        targetValue = surfaces.heroContent,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.paletteTween()),
        label = "player-expressive-pill-content"
    )
    val label = if (isPlaying) labels.pause else labels.play
    Row(
        modifier = Modifier
            .weight(1f)
            .height(height)
            .clip(RoundedCornerShape(corner))
            .background(fill)
            .levyraPressable(
                onClick = {
                    haptics.perform(LevyraHapticAction.Transport)
                    onClick()
                },
                interactionSource = interaction,
                pressedScale = LevyraPressScale.Control,
                role = Role.Button
            )
            .semantics { contentDescription = label }
            .padding(horizontal = LevyraPlayerDesign.SpaceLg),
        horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isResolving) {
            LevyraLoadingIndicator(color = content, modifier = Modifier.size(height * 0.46f))
        } else {
            LevyraPlayPauseGlyph(
                playing = isPlaying,
                color = content,
                modifier = Modifier.size(height * 0.36f)
            )
        }
        Text(
            text = label.uppercase(),
            color = content,
            fontSize = if (height < ExpressiveTransportHeight) 19.sp else 22.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.6.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ExpressiveRoundButton(
    icon: ImageVector,
    label: String,
    size: Dp,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean,
    onClick: () -> Unit
) {
    val haptics = LocalLevyraHaptics.current
    val interaction = remember { MutableInteractionSource() }
    val corner = levyraExpressiveCorner(
        interactionSource = interaction,
        rest = size / 2,
        pressed = size * 0.28f,
        label = "player-expressive-round-corner"
    )
    val fill by animateColorAsState(
        targetValue = surfaces.tonal,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.paletteTween()),
        label = "player-expressive-round-fill"
    )
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(fill)
            .levyraPressable(
                onClick = {
                    haptics.perform(LevyraHapticAction.Transport)
                    onClick()
                },
                interactionSource = interaction,
                pressedScale = LevyraPressScale.Control,
                role = Role.Button
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        PlayerIcon(icon, surfaces.content, Modifier.size(size * 0.42f))
    }
}
