package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.ui.components.LevyraPlayPauseGlyph
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.PlayerControlLabels
import com.luc4n3x.levyra.ui.components.PlayerIcon
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign

private const val ModeSegmentWeight = 0.86f
private const val SkipSegmentWeight = 1f
private const val PlaySegmentWeight = 1.56f

@Immutable
internal data class PlayerTransportWeights(
    val modeWeight: Float,
    val skipWeight: Float,
    val playWeight: Float
)

internal fun resolveTransportWeights(
    availableWidth: Dp,
    gap: Dp = LevyraPlayerDesign.TransportGap,
    minTouchTarget: Dp = LevyraPlayerDesign.MinimumTouchTarget
): PlayerTransportWeights {
    val totalGaps = gap * 4
    val availableSegmentWidth = availableWidth - totalGaps
    if (availableSegmentWidth <= 0.dp) {
        return PlayerTransportWeights(
            modeWeight = ModeSegmentWeight,
            skipWeight = SkipSegmentWeight,
            playWeight = PlaySegmentWeight
        )
    }

    val minFraction = (minTouchTarget / availableSegmentWidth).coerceIn(0f, 0.2f)
    val standardSum = ModeSegmentWeight * 2 + SkipSegmentWeight * 2 + PlaySegmentWeight
    val standardModeFraction = ModeSegmentWeight / standardSum
    val standardSkipFraction = SkipSegmentWeight / standardSum

    if (standardModeFraction >= minFraction) {
        return PlayerTransportWeights(
            modeWeight = ModeSegmentWeight,
            skipWeight = SkipSegmentWeight,
            playWeight = PlaySegmentWeight
        )
    }

    val modeFraction = maxOf(standardModeFraction, minFraction)
    val skipFraction = maxOf(standardSkipFraction, minFraction)
    val playFraction = (1f - 2f * modeFraction - 2f * skipFraction).coerceAtLeast(minFraction)

    return PlayerTransportWeights(
        modeWeight = modeFraction,
        skipWeight = skipFraction,
        playWeight = playFraction
    )
}

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

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
    ) {
        val weights = remember(maxWidth) { resolveTransportWeights(maxWidth) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
            horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.TransportGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerModeControl(
                icon = Icons.Rounded.Shuffle,
                label = labels.shuffle,
                active = shuffleOn,
                surfaces = surfaces,
                animated = animated,
                weight = weights.modeWeight,
                onClick = onShuffle
            )
            PlayerSkipControl(
                icon = Icons.Rounded.SkipPrevious,
                label = labels.previous,
                tint = surfaces.content,
                glyph = skipGlyph,
                weight = weights.skipWeight,
                onClick = onPrevious
            )
            PlayerPlayControl(
                isPlaying = isPlaying,
                isResolving = isResolving,
                surfaces = surfaces,
                diameter = if (compact) LevyraPlayerDesign.TransportPlayDiameterCompact else LevyraPlayerDesign.TransportPlayDiameter,
                glyph = if (compact) LevyraPlayerDesign.TransportPlayGlyphCompact else LevyraPlayerDesign.TransportPlayGlyph,
                animated = animated,
                labels = labels,
                weight = weights.playWeight,
                onClick = onTogglePlay
            )
            PlayerSkipControl(
                icon = Icons.Rounded.SkipNext,
                label = labels.next,
                tint = surfaces.content,
                glyph = skipGlyph,
                weight = weights.skipWeight,
                onClick = onNext
            )
            PlayerModeControl(
                icon = if (repeatMode == RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                label = labels.repeat,
                active = repeatMode != RepeatMode.Off,
                surfaces = surfaces,
                animated = animated,
                weight = weights.modeWeight,
                onClick = onRepeat
            )
        }
    }
}

@Composable
private fun RowScope.PlayerSkipControl(
    icon: ImageVector,
    label: String,
    tint: Color,
    glyph: Dp,
    weight: Float,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = PlayerSkipTouchSize)
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(CircleShape)
                .levyraPressable(
                    onClick = onClick,
                    pressedScale = LevyraPressScale.Control,
                    role = Role.Button,
                    haptic = LevyraHapticAction.Transport
                )
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center
        ) {
            PlayerIcon(icon, tint, Modifier.size(glyph))
        }
    }
}

@Composable
private fun RowScope.PlayerPlayControl(
    isPlaying: Boolean,
    isResolving: Boolean,
    surfaces: PlayerSurfaceTokens,
    diameter: Dp,
    glyph: Dp,
    animated: Boolean,
    labels: PlayerControlLabels,
    weight: Float,
    onClick: () -> Unit
) {
    val corner by animateDpAsState(
        targetValue = if (isPlaying) diameter * PlayingCornerRatio else diameter / 2,
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
    val shape = RoundedCornerShape(corner.coerceAtLeast(0.dp))
    Box(
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = diameter)
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(
                    elevation = PlayShadowElevation,
                    shape = shape,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.35f),
                    spotColor = Color.Black.copy(alpha = 0.45f)
                )
                .clip(shape)
                .background(hero)
                .levyraPressable(
                    onClick = onClick,
                    pressedScale = LevyraPressScale.Control,
                    role = Role.Button,
                    haptic = LevyraHapticAction.Transport
                )
                .semantics { contentDescription = playDescription(isPlaying, labels) },
            contentAlignment = Alignment.Center
        ) {
            PlayGlyphContent(
                isResolving = isResolving,
                isPlaying = isPlaying,
                content = content,
                glyph = glyph
            )
        }
    }
}

private fun playDescription(isPlaying: Boolean, labels: PlayerControlLabels): String =
    if (isPlaying) labels.pause else labels.play

@Composable
private fun PlayGlyphContent(
    isResolving: Boolean,
    isPlaying: Boolean,
    content: Color,
    glyph: Dp
) {
    if (isResolving) {
        CircularProgressIndicator(
            modifier = Modifier.size(glyph * 0.72f),
            strokeWidth = 2.8.dp,
            color = content
        )
    } else {
        LevyraPlayPauseGlyph(
            playing = isPlaying,
            color = content,
            modifier = Modifier.size(glyph)
        )
    }
}

@Composable
private fun RowScope.PlayerModeControl(
    icon: ImageVector,
    label: String,
    active: Boolean,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean,
    weight: Float,
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
    Box(
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(LevyraPlayerDesign.MinimumTouchTarget)
                .clip(CircleShape)
                .levyraPressable(
                    onClick = onClick,
                    pressedScale = LevyraPressScale.Control,
                    role = Role.Button
                )
                .semantics {
                    contentDescription = label
                    toggleableState = ToggleableState(active)
                },
            contentAlignment = Alignment.Center
        ) {
            PlayerIcon(icon, tint, Modifier.size(LevyraPlayerDesign.TransportModeGlyph))
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = LevyraPlayerDesign.SpaceXs)
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
}

private const val PlayingCornerRatio = 0.32f
private val PlayShadowElevation: Dp = 10.dp
private val PlayerSkipTouchSize: Dp = 56.dp
