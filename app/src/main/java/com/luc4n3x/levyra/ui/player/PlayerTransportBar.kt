package com.luc4n3x.levyra.ui.player

import com.luc4n3x.levyra.ui.components.rememberLevyraPlayMorphShape
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPlayPauseGlyph
import com.luc4n3x.levyra.ui.components.PlayerControlLabels
import com.luc4n3x.levyra.ui.components.PlayerIcon
import com.luc4n3x.levyra.ui.components.levyraExpressiveToggleCorner
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LevyraSegment

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
            PlayerModeSlot(weight = weights.modeWeight) {
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
            }
            PlayerSegmentButton(
                position = LevyraSegment.Single,
                weight = weights.skipWeight,
                container = surfaces.tonal,
                innerCorner = LevyraPlayerDesign.TransportInnerCorner,
                contentDescription = labels.previous,
                animated = animated,
                outline = surfaces.segmentOutline,
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
                weight = weights.playWeight,
                onClick = onTogglePlay
            )
            PlayerSegmentButton(
                position = LevyraSegment.Single,
                weight = weights.skipWeight,
                container = surfaces.tonal,
                innerCorner = LevyraPlayerDesign.TransportInnerCorner,
                contentDescription = labels.next,
                animated = animated,
                outline = surfaces.segmentOutline,
                haptic = LevyraHapticAction.Transport,
                onClick = onNext
            ) {
                PlayerIcon(Icons.Rounded.SkipNext, surfaces.content, Modifier.size(skipGlyph))
            }
            PlayerModeSlot(weight = weights.modeWeight) {
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
}

@Composable
private fun RowScope.PlayerModeSlot(weight: Float, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        content()
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
    weight: Float,
    onClick: () -> Unit
) {
    val innerCorner = levyraExpressiveToggleCorner(
        checked = isPlaying,
        unchecked = height * PausedCornerRatio,
        checkedCorner = height / 2,
        label = "player-play-corner"
    )
    val morphShape = rememberLevyraPlayMorphShape(playing = isPlaying, animated = animated)
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
        weight = weight,
        container = hero,
        innerCorner = innerCorner,
        contentDescription = playDescription(isPlaying, labels),
        animated = animated,
        haptic = LevyraHapticAction.Transport,
        shapeOverride = morphShape,
        onClick = onClick
    ) {
        PlayGlyphContent(
            isResolving = isResolving,
            isPlaying = isPlaying,
            content = content,
            glyph = glyph
        )
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
        LevyraLoadingIndicator(
            color = content,
            modifier = Modifier.size(glyph * 1.2f)
        )
    } else {
        LevyraPlayPauseGlyph(
            playing = isPlaying,
            color = content,
            modifier = Modifier.size(glyph)
        )
    }
}

private const val PausedCornerRatio = 0.30f
