package com.luc4n3x.levyra.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.PlayerDarkSurface
import com.luc4n3x.levyra.ui.PlayerMinimumContrast
import com.luc4n3x.levyra.ui.playerCompositeOver
import com.luc4n3x.levyra.ui.playerContentColor
import com.luc4n3x.levyra.ui.playerContrastGradient
import com.luc4n3x.levyra.ui.playerMix
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign

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
    .background(fill, shape)
    .background(
        Brush.verticalGradient(
            listOf(
                LevyraPlayerDesign.GlassSpecular,
                Color.Transparent,
                Color.Black.copy(alpha = 0.06f)
            )
        ),
        shape
    )
    .border(
        BorderStroke(
            LevyraPlayerDesign.Hairline,
            Brush.verticalGradient(listOf(borderTop, borderBottom))
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
    fill: Color = LevyraPlayerDesign.GlassFill,
    borderTop: Color = LevyraPlayerDesign.GlassBorderTop,
    borderBottom: Color = LevyraPlayerDesign.GlassBorderBottom,
    shape: Shape = CircleShape,
    enabled: Boolean = true
) {
    SpringIconButton(
        onClick = onClick,
        modifier = modifier.sizeIn(
            minWidth = LevyraPlayerDesign.MinimumTouchTarget,
            minHeight = LevyraPlayerDesign.MinimumTouchTarget
        ),
        enabled = enabled,
        pressedScale = 0.88f,
        contentDescription = contentDescription
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .playerGlass(shape = shape, fill = fill, borderTop = borderTop, borderBottom = borderBottom),
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

@Composable
fun PlayerTransportControls(
    isPlaying: Boolean,
    isResolving: Boolean,
    shuffleOn: Boolean,
    repeatOn: Boolean,
    repeatOne: Boolean,
    accent: Color,
    accentSecondary: Color,
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
    val utilitySize = if (compact) LevyraPlayerDesign.UtilityButtonCompact else LevyraPlayerDesign.UtilityButton
    val transportSize = if (compact) LevyraPlayerDesign.TransportButtonCompact else LevyraPlayerDesign.TransportButton
    val primarySize = if (compact) LevyraPlayerDesign.PrimaryButtonCompact else LevyraPlayerDesign.PrimaryButton

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        PlayerModeToggleButton(
            icon = Icons.Rounded.Shuffle,
            contentDescription = labels.shuffle,
            active = shuffleOn,
            accent = accent,
            size = utilitySize,
            iconSize = if (compact) 20.dp else 21.dp,
            animated = animated,
            onClick = onShuffle
        )
        PlayerGlassIconButton(
            icon = Icons.Rounded.SkipPrevious,
            contentDescription = labels.previous,
            size = transportSize,
            iconSize = if (compact) 28.dp else 30.dp,
            shape = LevyraPlayerDesign.ShapeMd,
            onClick = onPrevious
        )
        PlayerPrimaryButton(
            isPlaying = isPlaying,
            isResolving = isResolving,
            accent = accent,
            accentSecondary = accentSecondary,
            size = primarySize,
            animated = animated,
            playLabel = labels.play,
            pauseLabel = labels.pause,
            onClick = onToggle
        )
        PlayerGlassIconButton(
            icon = Icons.Rounded.SkipNext,
            contentDescription = labels.next,
            size = transportSize,
            iconSize = if (compact) 28.dp else 30.dp,
            shape = LevyraPlayerDesign.ShapeMd,
            onClick = onNext
        )
        PlayerModeToggleButton(
            icon = if (repeatOne) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
            contentDescription = labels.repeat,
            active = repeatOn,
            accent = accentSecondary,
            size = utilitySize,
            iconSize = if (compact) 20.dp else 21.dp,
            animated = animated,
            onClick = onRepeat
        )
    }
}

@Composable
private fun PlayerModeToggleButton(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean,
    accent: Color,
    size: Dp,
    iconSize: Dp,
    animated: Boolean,
    onClick: () -> Unit
) {
    val fill = if (active) accent.copy(alpha = 0.30f) else Color.Transparent
    val activeTint = remember(accent) {
        accent.playerContentColor(listOf(accent.copy(alpha = 0.30f).playerCompositeOver(PlayerDarkSurface)))
    }
    val tint = if (active) activeTint else LevyraPlayerDesign.IconIdle
    val corner by animateDpAsState(
        targetValue = if (active) size / 2f else size * 0.34f,
        animationSpec = if (animated) LevyraPlayerDesign.expressiveSpring() else snap(),
        label = "player-toggle-corner"
    )
    val borderAlpha by animateFloatAsState(
        targetValue = if (active) 0.55f else 0f,
        animationSpec = if (animated) LevyraPlayerDesign.standardTween() else snap(),
        label = "player-toggle-border"
    )

    SpringIconButton(
        onClick = onClick,
        modifier = Modifier.sizeIn(
            minWidth = LevyraPlayerDesign.MinimumTouchTarget,
            minHeight = LevyraPlayerDesign.MinimumTouchTarget
        ),
        pressedScale = 0.86f,
        contentDescription = contentDescription
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .background(fill, RoundedCornerShape(corner))
                .border(
                    BorderStroke(LevyraPlayerDesign.Hairline, accent.copy(alpha = borderAlpha)),
                    RoundedCornerShape(corner)
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

@Composable
private fun PlayerPrimaryButton(
    isPlaying: Boolean,
    isResolving: Boolean,
    accent: Color,
    accentSecondary: Color,
    size: Dp,
    animated: Boolean,
    playLabel: String,
    pauseLabel: String,
    onClick: () -> Unit
) {
    val gradient = remember(accent, accentSecondary) {
        playerContrastGradient(
            start = accent.playerMix(Color.White, 0.16f),
            end = accentSecondary.playerMix(Color.White, 0.06f),
            minimumContrast = PlayerMinimumContrast
        )
    }
    val corner by animateDpAsState(
        targetValue = if (isPlaying) size * 0.34f else size / 2f,
        animationSpec = if (animated) LevyraPlayerDesign.expressiveSpring() else snap(),
        label = "player-primary-corner"
    )
    val shape = RoundedCornerShape(corner)

    SpringIconButton(
        onClick = onClick,
        pressedScale = 0.92f,
        contentDescription = if (isPlaying) pauseLabel else playLabel
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .shadow(
                    elevation = 20.dp,
                    shape = shape,
                    clip = false,
                    ambientColor = accent.copy(alpha = 0.50f),
                    spotColor = accentSecondary.copy(alpha = 0.58f)
                )
                .background(Brush.linearGradient(listOf(gradient.start, gradient.end)), shape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.18f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.10f)
                        )
                    ),
                    shape
                )
                .border(
                    BorderStroke(
                        LevyraPlayerDesign.Hairline,
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.34f),
                                Color.White.copy(alpha = 0.08f)
                            )
                        )
                    ),
                    shape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isResolving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(size * 0.36f),
                    strokeWidth = 3.dp,
                    color = gradient.content
                )
            } else {
                AnimatedContent(
                    targetState = isPlaying,
                    transitionSpec = {
                        (fadeIn(LevyraPlayerDesign.standardTween(140)) +
                            scaleIn(initialScale = 0.72f, animationSpec = LevyraPlayerDesign.standardTween(140))) togetherWith
                            (fadeOut(LevyraPlayerDesign.standardTween(100)) +
                                scaleOut(targetScale = 0.72f, animationSpec = LevyraPlayerDesign.standardTween(100)))
                    },
                    label = "player-primary-icon"
                ) { playing ->
                    Icon(
                        imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = gradient.content,
                        modifier = Modifier
                            .size(size * 0.46f)
                            .offset(x = if (playing) 0.dp else size * 0.02f)
                    )
                }
            }
        }
    }
}
