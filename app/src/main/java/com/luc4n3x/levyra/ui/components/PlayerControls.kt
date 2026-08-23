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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val compactMetaAction = size <= 36.dp
    if (compactMetaAction && icon.name.substringAfterLast('.') == "AutoAwesome") return

    SpringIconButton(
        onClick = onClick,
        modifier = if (compactMetaAction) {
            modifier.size(width = 40.dp, height = LevyraPlayerDesign.MinimumTouchTarget)
        } else {
            modifier.sizeIn(
                minWidth = LevyraPlayerDesign.MinimumTouchTarget,
                minHeight = LevyraPlayerDesign.MinimumTouchTarget
            )
        },
        enabled = enabled,
        pressedScale = if (compactMetaAction) 0.89f else 0.94f,
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
    val previousInteraction = remember { MutableInteractionSource() }
    val primaryInteraction = remember { MutableInteractionSource() }
    val nextInteraction = remember { MutableInteractionSource() }
    val previousPressed by previousInteraction.collectIsPressedAsState()
    val primaryPressed by primaryInteraction.collectIsPressedAsState()
    val nextPressed by nextInteraction.collectIsPressedAsState()
    val weightSpec = if (animated) LevyraPlayerDesign.expressiveSpring<Float>() else snap()

    val previousWeight by animateFloatAsState(
        targetValue = when {
            previousPressed -> 0.66f
            primaryPressed -> 0.36f
            else -> 0.48f
        },
        animationSpec = weightSpec,
        label = "player-previous-weight"
    )
    val primaryWeight by animateFloatAsState(
        targetValue = when {
            primaryPressed -> 1.88f
            previousPressed || nextPressed -> 1.12f
            else -> 1.38f
        },
        animationSpec = weightSpec,
        label = "player-primary-weight"
    )
    val nextWeight by animateFloatAsState(
        targetValue = when {
            nextPressed -> 0.66f
            primaryPressed -> 0.36f
            else -> 0.48f
        },
        animationSpec = weightSpec,
        label = "player-next-weight"
    )

    val modeAccent = accents.primary.playerMix(accents.secondary, 0.30f)
    val transportHeight = if (compact) LevyraPlayerDesign.TransportHeightCompact else LevyraPlayerDesign.TransportHeight
    val sideGlyph = if (compact) LevyraPlayerDesign.TransportSideGlyphCompact else LevyraPlayerDesign.TransportSideGlyph
    val primaryGlyph = if (compact) LevyraPlayerDesign.TransportPrimaryGlyphCompact else LevyraPlayerDesign.TransportPrimaryGlyph
    val modeGlyph = if (compact) LevyraPlayerDesign.ModeGlyphCompact else LevyraPlayerDesign.ModeGlyph

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (compact) LevyraPlayerDesign.SpaceXxs else LevyraPlayerDesign.SpaceXs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp)
    ) {
        PlayerModeToggleButton(
            icon = Icons.Rounded.Shuffle,
            contentDescription = labels.shuffle,
            active = shuffleOn,
            iconSize = modeGlyph,
            accent = modeAccent,
            compact = compact,
            animated = animated,
            onClick = onShuffle
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .height(transportHeight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)
        ) {
            PlayerTransportSideButton(
                icon = Icons.Rounded.SkipPrevious,
                contentDescription = labels.previous,
                iconSize = sideGlyph,
                interactionSource = previousInteraction,
                modifier = Modifier
                    .weight(previousWeight)
                    .fillMaxHeight(),
                onClick = onPrevious
            )
            PlayerTransportPrimaryButton(
                isPlaying = isPlaying,
                isResolving = isResolving,
                iconSize = primaryGlyph,
                compact = compact,
                animated = animated,
                playLabel = labels.play,
                pauseLabel = labels.pause,
                interactionSource = primaryInteraction,
                modifier = Modifier
                    .weight(primaryWeight)
                    .fillMaxHeight(),
                onClick = onToggle
            )
            PlayerTransportSideButton(
                icon = Icons.Rounded.SkipNext,
                contentDescription = labels.next,
                iconSize = sideGlyph,
                interactionSource = nextInteraction,
                modifier = Modifier
                    .weight(nextWeight)
                    .fillMaxHeight(),
                onClick = onNext
            )
        }

        PlayerModeToggleButton(
            icon = if (repeatOne) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
            contentDescription = labels.repeat,
            active = repeatOn,
            iconSize = modeGlyph,
            accent = modeAccent,
            compact = compact,
            animated = animated,
            onClick = onRepeat
        )
    }
}

@Composable
private fun PlayerTransportSideButton(
    icon: ImageVector,
    contentDescription: String,
    iconSize: Dp,
    interactionSource: MutableInteractionSource,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val pressed by interactionSource.collectIsPressedAsState()
    val fill by animateColorAsState(
        targetValue = if (pressed) Color.White.copy(alpha = 0.17f) else Color.White.copy(alpha = 0.105f),
        animationSpec = LevyraPlayerDesign.standardTween(140),
        label = "player-side-fill"
    )

    SpringIconButton(
        onClick = onClick,
        modifier = modifier,
        pressedScale = 0.97f,
        contentDescription = contentDescription,
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(fill, LevyraPlayerDesign.ShapePill)
                .border(
                    LevyraPlayerDesign.Hairline,
                    Color.White.copy(alpha = if (pressed) 0.18f else 0.10f),
                    LevyraPlayerDesign.ShapePill
                ),
            contentAlignment = Alignment.Center
        ) {
            PlayerIcon(
                icon = icon,
                tint = Color.White.copy(alpha = 0.96f),
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
private fun PlayerModeToggleButton(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean,
    iconSize: Dp,
    accent: Color,
    compact: Boolean,
    animated: Boolean,
    onClick: () -> Unit
) {
    val tint by animateColorAsState(
        targetValue = if (active) accent.playerMix(Color.White, 0.62f) else Color.White.copy(alpha = 0.42f),
        animationSpec = if (animated) LevyraPlayerDesign.standardTween() else snap(),
        label = "player-toggle-tint"
    )
    val surfaceAlpha by animateFloatAsState(
        targetValue = if (active) 0.15f else 0f,
        animationSpec = if (animated) LevyraPlayerDesign.standardTween() else snap(),
        label = "player-toggle-surface"
    )
    val visualSize = if (compact) 38.dp else 40.dp

    SpringIconButton(
        onClick = onClick,
        modifier = Modifier
            .size(LevyraPlayerDesign.ModeSlot)
            .semantics { toggleableState = ToggleableState(active) },
        pressedScale = 0.88f,
        contentDescription = contentDescription
    ) {
        Box(
            modifier = Modifier
                .size(visualSize)
                .background(accent.copy(alpha = surfaceAlpha), CircleShape)
                .border(
                    LevyraPlayerDesign.Hairline,
                    Color.White.copy(alpha = surfaceAlpha * 0.60f),
                    CircleShape
                ),
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

private fun playerPrimaryIconTransition(): ContentTransform =
    (fadeIn(LevyraPlayerDesign.standardTween(150)) +
        scaleIn(initialScale = 0.86f, animationSpec = LevyraPlayerDesign.standardTween(150))) togetherWith
        (fadeOut(LevyraPlayerDesign.standardTween(100)) +
            scaleOut(targetScale = 0.86f, animationSpec = LevyraPlayerDesign.standardTween(100)))

@Composable
private fun PlayerPrimaryIcon(isPlaying: Boolean, iconSize: Dp, tint: Color, animated: Boolean) {
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
            tint = tint,
            modifier = Modifier
                .size(iconSize)
                .offset(x = if (playing) 0.dp else 1.dp)
        )
    }
}

@Composable
private fun PlayerTransportPrimaryButton(
    isPlaying: Boolean,
    isResolving: Boolean,
    iconSize: Dp,
    compact: Boolean,
    animated: Boolean,
    playLabel: String,
    pauseLabel: String,
    interactionSource: MutableInteractionSource,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val pressed by interactionSource.collectIsPressedAsState()
    val fill by animateColorAsState(
        targetValue = if (pressed) Color.White.copy(alpha = 0.86f) else Color.White.copy(alpha = 0.94f),
        animationSpec = if (animated) LevyraPlayerDesign.standardTween(130) else snap(),
        label = "player-primary-fill"
    )
    val label = if (isPlaying) pauseLabel else playLabel

    SpringIconButton(
        onClick = onClick,
        modifier = modifier,
        pressedScale = 0.985f,
        contentDescription = label,
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(fill, LevyraPlayerDesign.ShapePill)
                .border(
                    LevyraPlayerDesign.Hairline,
                    Color.White.copy(alpha = 0.30f),
                    LevyraPlayerDesign.ShapePill
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isResolving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(if (compact) 24.dp else 26.dp),
                    strokeWidth = 2.6.dp,
                    color = LevyraPlayerDesign.PrimaryContent
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    PlayerPrimaryIcon(
                        isPlaying = isPlaying,
                        iconSize = iconSize,
                        tint = LevyraPlayerDesign.PrimaryContent,
                        animated = animated
                    )
                    Spacer(modifier = Modifier.width(if (compact) 6.dp else 8.dp))
                    Text(
                        text = label,
                        color = LevyraPlayerDesign.PrimaryContent,
                        fontSize = if (compact) 13.sp else 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
