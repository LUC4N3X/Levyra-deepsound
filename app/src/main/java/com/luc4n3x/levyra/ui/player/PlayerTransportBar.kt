package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.ui.components.PlayerControlLabels
import com.luc4n3x.levyra.ui.components.PlayerIcon
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LevyraSegment

private const val ModeSegmentWeight = 0.86f
private const val SkipSegmentWeight = 1f
private const val PlaySegmentWeight = 1.56f

@Composable
internal fun PlayerTransportBar(
    isPlaying: Boolean,
    isResolving: Boolean,
    shuffleOn: Boolean,
    repeatMode: RepeatMode,
    surfaces: PlayerSurfaceTokens,
    compact: Boolean,
    animated: Boolean,
    labels: PlayerControlLabels,
    onShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val height = if (compact) LevyraPlayerDesign.TransportHeightCompact else LevyraPlayerDesign.TransportHeight
    val skipGlyph = if (compact) LevyraPlayerDesign.TransportGlyphCompact else LevyraPlayerDesign.TransportGlyph
    val controlOutline = if (surfaces.amoled) surfaces.outline else Color.Transparent

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.TransportGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayerModeSegment(
            position = LevyraSegment.Leading,
            icon = Icons.Rounded.Shuffle,
            label = labels.shuffle,
            active = shuffleOn,
            surfaces = surfaces,
            outline = controlOutline,
            animated = animated,
            onClick = onShuffle
        )
        PlayerSegmentButton(
            position = LevyraSegment.Middle,
            weight = SkipSegmentWeight,
            container = surfaces.control,
            innerCorner = LevyraPlayerDesign.TransportInnerCorner,
            contentDescription = labels.previous,
            animated = animated,
            outline = controlOutline,
            haptic = LevyraHapticAction.Transport,
            onClick = onPrevious
        ) {
            PlayerIcon(Icons.Rounded.SkipPrevious, surfaces.content, Modifier.size(skipGlyph))
        }
        PlayerPlaySegment(
            isPlaying = isPlaying,
            isResolving = isResolving,
            surfaces = surfaces,
            height = height,
            glyph = if (compact) LevyraPlayerDesign.TransportPlayGlyphCompact else LevyraPlayerDesign.TransportPlayGlyph,
            animated = animated,
            labels = labels,
            onClick = onTogglePlay
        )
        PlayerSegmentButton(
            position = LevyraSegment.Middle,
            weight = SkipSegmentWeight,
            container = surfaces.control,
            innerCorner = LevyraPlayerDesign.TransportInnerCorner,
            contentDescription = labels.next,
            animated = animated,
            outline = controlOutline,
            haptic = LevyraHapticAction.Transport,
            onClick = onNext
        ) {
            PlayerIcon(Icons.Rounded.SkipNext, surfaces.content, Modifier.size(skipGlyph))
        }
        PlayerModeSegment(
            position = LevyraSegment.Trailing,
            icon = if (repeatMode == RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
            label = labels.repeat,
            active = repeatMode != RepeatMode.Off,
            surfaces = surfaces,
            outline = controlOutline,
            animated = animated,
            onClick = onRepeat
        )
    }
}

@Composable
private fun RowScope.PlayerPlaySegment(
    isPlaying: Boolean,
    isResolving: Boolean,
    surfaces: PlayerSurfaceTokens,
    height: Dp,
    glyph: Dp,
    animated: Boolean,
    labels: PlayerControlLabels,
    onClick: () -> Unit
) {
    val innerCorner by animateDpAsState(
        targetValue = playInnerCorner(isPlaying, height),
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.expressiveSpring()),
        label = "player-play-corner"
    )
    val content by animateColorAsState(
        targetValue = surfaces.heroContent,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.paletteTween()),
        label = "player-play-content"
    )
    val hero by animateColorAsState(
        targetValue = surfaces.hero,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.paletteTween()),
        label = "player-play-fill"
    )
    PlayerSegmentButton(
        position = LevyraSegment.Middle,
        weight = PlaySegmentWeight,
        container = hero,
        innerCorner = innerCorner,
        contentDescription = playDescription(isPlaying, labels),
        animated = animated,
        haptic = LevyraHapticAction.Transport,
        onClick = onClick
    ) {
        PlayGlyphContent(
            isResolving = isResolving,
            isPlaying = isPlaying,
            content = content,
            glyph = glyph,
            animated = animated
        )
    }
}

private fun playInnerCorner(isPlaying: Boolean, height: Dp): Dp =
    if (isPlaying) LevyraPlayerDesign.TransportInnerCorner else height / 2

private fun playDescription(isPlaying: Boolean, labels: PlayerControlLabels): String =
    if (isPlaying) labels.pause else labels.play

@Composable
private fun PlayGlyphContent(
    isResolving: Boolean,
    isPlaying: Boolean,
    content: Color,
    glyph: Dp,
    animated: Boolean
) {
    if (isResolving) {
        CircularProgressIndicator(
            modifier = Modifier.size(glyph * 0.72f),
            strokeWidth = 2.8.dp,
            color = content
        )
    } else {
        PlayIconAnimated(
            isPlaying = isPlaying,
            content = content,
            glyph = glyph,
            animated = animated
        )
    }
}

@Composable
private fun PlayIconAnimated(
    isPlaying: Boolean,
    content: Color,
    glyph: Dp,
    animated: Boolean
) {
    AnimatedContent(
        targetState = isPlaying,
        transitionSpec = { playTransition(animated) },
        label = "player-play-glyph"
    ) { playing ->
        PlayerIcon(
            icon = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            tint = content,
            modifier = Modifier
                .size(glyph)
                .offset(x = if (playing) 0.dp else 2.dp)
        )
    }
}

private fun playTransition(animated: Boolean): ContentTransform =
    if (animated) {
        fadeIn(LevyraPlayerDesign.standardTween(150)) +
            scaleIn(LevyraPlayerDesign.expressiveSpring(), initialScale = 0.6f) togetherWith
            fadeOut(LevyraPlayerDesign.standardTween(90)) +
                scaleOut(LevyraPlayerDesign.standardTween(90), targetScale = 0.6f)
    } else {
        EnterTransition.None togetherWith ExitTransition.None
    }

@Composable
private fun RowScope.PlayerModeSegment(
    position: LevyraSegment,
    icon: ImageVector,
    label: String,
    active: Boolean,
    surfaces: PlayerSurfaceTokens,
    outline: Color,
    animated: Boolean,
    onClick: () -> Unit
) {
    val tint by animateColorAsState(
        targetValue = if (active) surfaces.activeContent else surfaces.contentMuted,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.standardTween(200)),
        label = "player-mode-tint"
    )
    val indicator by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.expressiveSpring()),
        label = "player-mode-indicator"
    )
    PlayerSegmentButton(
        position = position,
        weight = ModeSegmentWeight,
        container = if (active) surfaces.active else surfaces.controlQuiet,
        innerCorner = LevyraPlayerDesign.TransportInnerCorner,
        contentDescription = label,
        animated = animated,
        toggleState = ToggleableState(active),
        outline = outline,
        onClick = onClick
    ) {
        PlayerIcon(icon, tint, Modifier.size(LevyraPlayerDesign.TransportModeGlyph))
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = LevyraPlayerDesign.SpaceMd)
                .size(LevyraPlayerDesign.ModeIndicator)
                .graphicsLayer {
                    alpha = indicator
                    scaleX = indicator
                    scaleY = indicator
                }
                .background(tint, CircleShape)
        )
    }
}
