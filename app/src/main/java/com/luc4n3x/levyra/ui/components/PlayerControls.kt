package com.luc4n3x.levyra.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.playerMix
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign

@Immutable
data class PlayerAccentColors(
    val primary: Color,
    val secondary: Color,
    val primaryTarget: Color,
    val secondaryTarget: Color
)

data class PlayerControlLabels(
    val shuffle: String,
    val previous: String,
    val play: String,
    val pause: String,
    val next: String,
    val repeat: String
)

private fun ioniconForPlayer(icon: ImageVector): ImageVector? {
    return when (icon.name.substringAfterLast('.')) {
        "PlayArrow" -> LevyraIonicons.Play
        "Pause" -> LevyraIonicons.Pause
        "SkipPrevious" -> LevyraIonicons.SkipPrevious
        "SkipNext" -> LevyraIonicons.SkipNext
        "Shuffle" -> LevyraIonicons.Shuffle
        "Repeat" -> LevyraIonicons.Repeat
        "RepeatOne" -> LevyraIonicons.RepeatOne
        "KeyboardArrowDown" -> LevyraIonicons.ChevronDown
        "MoreVert" -> LevyraIonicons.MoreVertical
        "PlaylistAdd" -> LevyraIonicons.AddCircle
        "Favorite" -> LevyraIonicons.Heart
        "FavoriteBorder" -> LevyraIonicons.HeartOutline
        "Download", "FileDownload" -> LevyraIonicons.Download
        "QueueMusic", "PlaylistPlay" -> LevyraIonicons.Queue
        "Subject", "TextFields" -> LevyraIonicons.Lyrics
        "Bedtime", "Schedule", "Timer" -> LevyraIonicons.Timer
        "Equalizer", "GraphicEq", "Tune" -> LevyraIonicons.Equalizer
        "PhoneAndroid", "Devices" -> LevyraIonicons.Device
        "Settings" -> LevyraIonicons.Settings
        "Share" -> LevyraIonicons.Share
        else -> null
    }
}

@Composable
private fun PlayerIcon(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val resolved = remember(icon) { ioniconForPlayer(icon) ?: icon }
    Icon(
        imageVector = resolved,
        contentDescription = null,
        tint = tint,
        modifier = modifier
    )
}

fun Modifier.playerGlass(
    shape: Shape,
    fill: Color = LevyraPlayerDesign.GlassFill,
    borderTop: Color = LevyraPlayerDesign.GlassBorderTop,
    borderBottom: Color = LevyraPlayerDesign.GlassBorderBottom
): Modifier = this
    .background(fill, shape)
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
        pressedScale = 0.94f,
        contentDescription = contentDescription
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .playerGlass(shape = shape, fill = fill, borderTop = borderTop, borderBottom = borderBottom),
            contentAlignment = Alignment.Center
        ) {
            PlayerIcon(
                icon = icon,
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
    val skipIconSize = if (compact) 34.dp else 38.dp
    val modeIconSize = if (compact) 20.dp else 22.dp
    val primaryIconSize = if (compact) 36.dp else 40.dp

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        PlayerModeToggleButton(
            icon = Icons.Rounded.Shuffle,
            contentDescription = labels.shuffle,
            active = shuffleOn,
            accentTarget = accents.primaryTarget,
            iconSize = modeIconSize,
            animated = animated,
            onClick = onShuffle
        )
        PlayerSkipButton(
            icon = Icons.Rounded.SkipPrevious,
            contentDescription = labels.previous,
            iconSize = skipIconSize,
            onClick = onPrevious
        )
        PlayerPrimaryButton(
            isPlaying = isPlaying,
            isResolving = isResolving,
            iconSize = primaryIconSize,
            accentColor = accents.primaryTarget,
            animated = animated,
            playLabel = labels.play,
            pauseLabel = labels.pause,
            onClick = onToggle
        )
        PlayerSkipButton(
            icon = Icons.Rounded.SkipNext,
            contentDescription = labels.next,
            iconSize = skipIconSize,
            onClick = onNext
        )
        PlayerModeToggleButton(
            icon = if (repeatOne) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
            contentDescription = labels.repeat,
            active = repeatOn,
            accentTarget = accents.secondaryTarget,
            iconSize = modeIconSize,
            animated = animated,
            onClick = onRepeat
        )
    }
}

@Composable
private fun PlayerSkipButton(
    icon: ImageVector,
    contentDescription: String,
    iconSize: Dp,
    onClick: () -> Unit
) {
    SpringIconButton(
        onClick = onClick,
        modifier = Modifier.sizeIn(
            minWidth = 58.dp,
            minHeight = 58.dp
        ),
        pressedScale = 0.86f,
        contentDescription = contentDescription
    ) {
        PlayerIcon(
            icon = icon,
            tint = Color.White.copy(alpha = 0.96f),
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun PlayerModeToggleButton(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean,
    accentTarget: Color,
    iconSize: Dp,
    animated: Boolean,
    onClick: () -> Unit
) {
    val activeTint = remember(accentTarget) {
        accentTarget.playerMix(Color.White, 0.40f)
    }
    val tint by animateColorAsState(
        targetValue = if (active) activeTint else Color.White.copy(alpha = 0.46f),
        animationSpec = if (animated) LevyraPlayerDesign.standardTween() else snap(),
        label = "player-toggle-tint"
    )
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = if (animated) LevyraPlayerDesign.standardTween(180) else snap(),
        label = "player-toggle-indicator"
    )

    SpringIconButton(
        onClick = onClick,
        modifier = Modifier
            .sizeIn(
                minWidth = LevyraPlayerDesign.MinimumTouchTarget,
                minHeight = LevyraPlayerDesign.MinimumTouchTarget
            )
            .semantics { toggleableState = ToggleableState(active) },
        pressedScale = 0.84f,
        contentDescription = contentDescription
    ) {
        Box(
            modifier = Modifier.size(LevyraPlayerDesign.MinimumTouchTarget),
            contentAlignment = Alignment.Center
        ) {
            PlayerIcon(
                icon = icon,
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
                    .size(3.dp)
                    .graphicsLayer { alpha = indicatorAlpha }
                    .background(activeTint, CircleShape)
            )
        }
    }
}

private fun playerPrimaryIconTransition(): ContentTransform =
    (fadeIn(LevyraPlayerDesign.standardTween(130)) +
        scaleIn(initialScale = 0.88f, animationSpec = LevyraPlayerDesign.standardTween(130))) togetherWith
        (fadeOut(LevyraPlayerDesign.standardTween(90)) +
            scaleOut(targetScale = 0.88f, animationSpec = LevyraPlayerDesign.standardTween(90)))

@Composable
private fun PlayerPrimaryIcon(isPlaying: Boolean, iconSize: Dp, animated: Boolean) {
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
        PlayerIcon(
            icon = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            tint = Color.White,
            modifier = Modifier
                .size(iconSize)
                .offset(x = if (playing) 0.dp else 1.5.dp)
        )
    }
}

@Composable
private fun PlayerPrimaryButton(
    isPlaying: Boolean,
    isResolving: Boolean,
    iconSize: Dp,
    accentColor: Color,
    animated: Boolean,
    playLabel: String,
    pauseLabel: String,
    onClick: () -> Unit
) {
    val halo = remember(accentColor) { accentColor.playerMix(Color.White, 0.22f) }
    val haloAlpha by animateFloatAsState(
        targetValue = if (isPlaying) 0.10f else 0.05f,
        animationSpec = if (animated) LevyraPlayerDesign.standardTween(240) else snap(),
        label = "player-primary-halo"
    )

    SpringIconButton(
        onClick = onClick,
        modifier = Modifier.sizeIn(minWidth = 72.dp, minHeight = 72.dp),
        pressedScale = 0.88f,
        contentDescription = if (isPlaying) pauseLabel else playLabel
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .drawBehind {
                    val radius = size.minDimension * 0.62f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                halo.copy(alpha = haloAlpha),
                                Color.Transparent
                            ),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (isResolving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(iconSize * 0.74f),
                    strokeWidth = 2.8.dp,
                    color = Color.White
                )
            } else {
                PlayerPrimaryIcon(
                    isPlaying = isPlaying,
                    iconSize = iconSize,
                    animated = animated
                )
            }
        }
    }
}
