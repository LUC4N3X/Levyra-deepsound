package com.luc4n3x.levyra.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.PlayerMinimumContrast
import com.luc4n3x.levyra.ui.playerContrastGradient
import com.luc4n3x.levyra.ui.playerMix
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign

@Immutable
data class PlayerAccentColors(
    val primary: Color,
    val secondary: Color,
    val primaryTarget: Color,
    val secondaryTarget: Color
)

@Immutable
data class PlayerControlLabels(
    val shuffle: String,
    val previous: String,
    val play: String,
    val pause: String,
    val next: String,
    val repeat: String
)

fun Modifier.playerGlass(
    shape: Shape,
    fill: Color = LevyraPlayerDesign.GlassFill,
    borderTop: Color = LevyraPlayerDesign.GlassBorderTop,
    borderBottom: Color = LevyraPlayerDesign.GlassBorderBottom
): Modifier = this
    .background(
        brush = Brush.verticalGradient(
            listOf(
                fill.playerMix(Color.White, 0.08f),
                fill,
                fill.playerMix(Color.Black, 0.08f)
            )
        ),
        shape = shape
    )
    .border(
        BorderStroke(
            LevyraPlayerDesign.Hairline,
            borderTop.playerMix(borderBottom, 0.5f)
        ),
        shape
    )

@Composable
fun PlayerGlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = LevyraPlayerDesign.HeaderButton,
    iconSize: Dp = 21.dp,
    tint: Color = LevyraPlayerDesign.TextPrimary,
    fill: Color = LevyraPlayerDesign.GlassFillRaised,
    borderTop: Color = LevyraPlayerDesign.GlassBorderTop,
    borderBottom: Color = LevyraPlayerDesign.GlassBorderBottom,
    shape: Shape = LevyraPlayerDesign.ShapeMd,
    enabled: Boolean = true
) {
    SpringIconButton(
        onClick = onClick,
        modifier = modifier.sizeIn(
            minWidth = LevyraPlayerDesign.MinimumTouchTarget,
            minHeight = LevyraPlayerDesign.MinimumTouchTarget
        ),
        enabled = enabled,
        pressedScale = 0.93f,
        contentDescription = contentDescription
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .playerGlass(
                    shape = shape,
                    fill = fill,
                    borderTop = borderTop,
                    borderBottom = borderBottom
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

/**
 * Ordered two-level transport hierarchy.
 *
 * The primary playback actions stay visually dominant while shuffle and repeat
 * live in a quieter mode rail below. This keeps every existing action available
 * without making all five controls compete for attention.
 */
@Composable
fun PlayerTransportControls(
    isPlaying: Boolean,
    isResolving: Boolean,
    shuffleOn: Boolean,
    repeatOn: Boolean,
    repeatOne: Boolean,
    accents: PlayerAccentColors,
    compact: Boolean,
    animated: Boolean,
    labels: PlayerControlLabels,
    onShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transportSize = if (compact) {
        LevyraPlayerDesign.TransportButtonCompact
    } else {
        LevyraPlayerDesign.TransportButton
    }
    val primaryWidth = if (compact) {
        LevyraPlayerDesign.PrimaryWidthCompact
    } else {
        LevyraPlayerDesign.PrimaryWidth
    }
    val primaryHeight = if (compact) {
        LevyraPlayerDesign.PrimaryHeightCompact
    } else {
        LevyraPlayerDesign.PrimaryHeight
    }
    val controlGap = if (compact) {
        LevyraPlayerDesign.MainControlGapCompact
    } else {
        LevyraPlayerDesign.MainControlGap
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            PlayerGlassIconButton(
                icon = Icons.Rounded.SkipPrevious,
                contentDescription = labels.previous,
                size = transportSize,
                iconSize = if (compact) 28.dp else 30.dp,
                tint = LevyraPlayerDesign.TextPrimary,
                fill = LevyraPlayerDesign.TransportFill,
                borderTop = LevyraPlayerDesign.TransportBorder,
                borderBottom = LevyraPlayerDesign.GlassBorderBottom,
                shape = CircleShape,
                onClick = onPrevious
            )
            Spacer(modifier = Modifier.width(controlGap))
            PlayerPrimaryButton(
                isPlaying = isPlaying,
                isResolving = isResolving,
                accentTarget = accents.primaryTarget,
                accentSecondaryTarget = accents.secondaryTarget,
                width = primaryWidth,
                height = primaryHeight,
                animated = animated,
                playLabel = labels.play,
                pauseLabel = labels.pause,
                onClick = onToggle
            )
            Spacer(modifier = Modifier.width(controlGap))
            PlayerGlassIconButton(
                icon = Icons.Rounded.SkipNext,
                contentDescription = labels.next,
                size = transportSize,
                iconSize = if (compact) 28.dp else 30.dp,
                tint = LevyraPlayerDesign.TextPrimary,
                fill = LevyraPlayerDesign.TransportFill,
                borderTop = LevyraPlayerDesign.TransportBorder,
                borderBottom = LevyraPlayerDesign.GlassBorderBottom,
                shape = CircleShape,
                onClick = onNext
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (compact) 8.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerModeToggleButton(
                icon = Icons.Rounded.Shuffle,
                contentDescription = labels.shuffle,
                active = shuffleOn,
                accent = accents.primary,
                accentTarget = accents.primaryTarget,
                compact = compact,
                animated = animated,
                onClick = onShuffle
            )
            PlayerModeRail(
                primary = accents.primary,
                secondary = accents.secondary,
                active = shuffleOn || repeatOn,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = if (compact) 13.dp else 17.dp)
            )
            PlayerModeToggleButton(
                icon = if (repeatOne) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                contentDescription = labels.repeat,
                active = repeatOn,
                accent = accents.secondary,
                accentTarget = accents.secondaryTarget,
                compact = compact,
                animated = animated,
                onClick = onRepeat
            )
        }
    }
}

@Composable
private fun PlayerModeRail(
    primary: Color,
    secondary: Color,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (active) 0.62f else 0.28f,
        animationSpec = LevyraPlayerDesign.standardTween(220),
        label = "player-mode-rail-alpha"
    )
    Box(
        modifier = modifier
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        primary.copy(alpha = alpha),
                        secondary.copy(alpha = alpha),
                        Color.Transparent
                    )
                )
            )
    )
}

@Composable
private fun PlayerModeToggleButton(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean,
    accent: Color,
    accentTarget: Color,
    compact: Boolean,
    animated: Boolean,
    onClick: () -> Unit
) {
    val width = if (compact) LevyraPlayerDesign.ModeButtonWidthCompact else LevyraPlayerDesign.ModeButtonWidth
    val height = if (compact) LevyraPlayerDesign.ModeButtonHeightCompact else LevyraPlayerDesign.ModeButtonHeight
    val activeTint = remember(accentTarget) {
        Color.White.playerMix(accentTarget, 0.04f)
    }
    val fill by animateColorAsState(
        targetValue = if (active) {
            accent.copy(alpha = 0.34f)
        } else {
            LevyraPlayerDesign.ModeFill
        },
        animationSpec = if (animated) LevyraPlayerDesign.standardTween(180) else snap(),
        label = "player-mode-fill"
    )
    val tint by animateColorAsState(
        targetValue = if (active) activeTint else LevyraPlayerDesign.IconIdle,
        animationSpec = if (animated) LevyraPlayerDesign.standardTween(180) else snap(),
        label = "player-mode-tint"
    )
    val border by animateColorAsState(
        targetValue = if (active) {
            accent.playerMix(Color.White, 0.28f).copy(alpha = 0.48f)
        } else {
            LevyraPlayerDesign.ModeDivider
        },
        animationSpec = if (animated) LevyraPlayerDesign.standardTween(180) else snap(),
        label = "player-mode-border"
    )

    SpringIconButton(
        onClick = onClick,
        modifier = Modifier
            .sizeIn(
                minWidth = LevyraPlayerDesign.MinimumTouchTarget,
                minHeight = LevyraPlayerDesign.MinimumTouchTarget
            )
            .semantics { toggleableState = ToggleableState(active) },
        pressedScale = 0.92f,
        contentDescription = contentDescription
    ) {
        Box(
            modifier = Modifier
                .size(width = width, height = height)
                .background(fill, LevyraPlayerDesign.ShapePill)
                .border(
                    BorderStroke(LevyraPlayerDesign.Hairline, border),
                    LevyraPlayerDesign.ShapePill
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(if (compact) 20.dp else 21.dp)
            )
            if (active) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-8).dp, y = 7.dp)
                        .size(4.dp)
                        .background(tint, CircleShape)
                )
            }
        }
    }
}

private fun playerPrimaryIconTransition(): ContentTransform =
    (fadeIn(LevyraPlayerDesign.standardTween(150)) +
        scaleIn(initialScale = 0.78f, animationSpec = LevyraPlayerDesign.standardTween(150))) togetherWith
        (fadeOut(LevyraPlayerDesign.standardTween(100)) +
            scaleOut(targetScale = 0.78f, animationSpec = LevyraPlayerDesign.standardTween(100)))

private fun Modifier.playerPrimarySurface(
    shape: Shape,
    start: Color,
    end: Color
): Modifier = this
    .shadow(
        elevation = 10.dp,
        shape = shape,
        clip = false,
        ambientColor = start.copy(alpha = 0.28f),
        spotColor = end.copy(alpha = 0.34f)
    )
    .background(Brush.linearGradient(listOf(start, end)), shape)
    .border(
        BorderStroke(
            LevyraPlayerDesign.Hairline,
            Color.White.copy(alpha = 0.24f)
        ),
        shape
    )

@Composable
private fun PlayerPrimaryIcon(
    isPlaying: Boolean,
    iconSize: Dp,
    tint: Color,
    animated: Boolean
) {
    AnimatedContent(
        targetState = isPlaying,
        transitionSpec = {
            if (animated) {
                playerPrimaryIconTransition()
            } else {
                EnterTransition.None togetherWith ExitTransition.None
            }
        },
        label = "player-primary-icon"
    ) { playing ->
        Icon(
            imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(iconSize)
                .offset(x = if (playing) 0.dp else 1.dp)
        )
    }
}

@Composable
private fun PlayerPrimaryButton(
    isPlaying: Boolean,
    isResolving: Boolean,
    accentTarget: Color,
    accentSecondaryTarget: Color,
    width: Dp,
    height: Dp,
    animated: Boolean,
    playLabel: String,
    pauseLabel: String,
    onClick: () -> Unit
) {
    val gradient = remember(accentTarget, accentSecondaryTarget) {
        playerContrastGradient(
            start = accentTarget.playerMix(Color.White, 0.24f),
            end = accentSecondaryTarget.playerMix(Color.White, 0.16f),
            minimumContrast = PlayerMinimumContrast
        )
    }
    val colorSpec: AnimationSpec<Color> =
        if (animated) LevyraPlayerDesign.emphasizedTween(520) else snap()
    val gradientStart by animateColorAsState(
        targetValue = gradient.start,
        animationSpec = colorSpec,
        label = "player-primary-start"
    )
    val gradientEnd by animateColorAsState(
        targetValue = gradient.end,
        animationSpec = colorSpec,
        label = "player-primary-end"
    )
    val gradientContent by animateColorAsState(
        targetValue = gradient.content,
        animationSpec = colorSpec,
        label = "player-primary-content"
    )

    SpringIconButton(
        onClick = onClick,
        pressedScale = 0.94f,
        contentDescription = if (isPlaying) pauseLabel else playLabel
    ) {
        Box(
            modifier = Modifier
                .size(width = width, height = height)
                .playerPrimarySurface(
                    shape = CircleShape,
                    start = gradientStart,
                    end = gradientEnd
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isResolving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(height * 0.38f),
                    strokeWidth = 2.7.dp,
                    color = gradientContent
                )
            } else {
                PlayerPrimaryIcon(
                    isPlaying = isPlaying,
                    iconSize = height * 0.48f,
                    tint = gradientContent,
                    animated = animated
                )
            }
        }
    }
}
