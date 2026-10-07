package com.luc4n3x.levyra.ui.media

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.PlayerMinimumContrast
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraExpressiveCorner
import com.luc4n3x.levyra.ui.components.PlayerGlassIconButton
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.harmonizePlayerAccents
import com.luc4n3x.levyra.ui.playerAdjustForegroundToward
import com.luc4n3x.levyra.ui.playerAmbienceOf
import com.luc4n3x.levyra.ui.playerContrastGradient
import com.luc4n3x.levyra.ui.playerMix
import com.luc4n3x.levyra.ui.theme.LevyraCardDesign
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LevyraType

@Immutable
internal data class ImmersiveMediaColors(
    val fieldTop: Color,
    val fieldMid: Color,
    val base: Color,
    val content: Color,
    val contentMuted: Color,
    val accent: Color,
    val actionStart: Color,
    val actionEnd: Color,
    val actionContent: Color,
    val secondaryFill: Color,
    val hairline: Color
)

@Immutable
internal data class ImmersiveMediaPrimaryAction(
    val enabled: Boolean,
    val label: String,
    val contentDescription: String,
    val icon: ImageVector = Icons.Rounded.PlayArrow,
    val loading: Boolean = false,
    val onClick: () -> Unit
)

internal fun immersiveMediaColors(primary: Color, secondary: Color, lightTheme: Boolean): ImmersiveMediaColors {
    val accents = harmonizePlayerAccents(primary, secondary)
    val action = playerContrastGradient(accents.primary, accents.secondary, PlayerMinimumContrast)
    val fieldTop: Color
    val fieldMid: Color
    val base: Color
    val content: Color
    val mutedAlpha: Float
    if (lightTheme) {
        fieldTop = accents.primary.playerMix(Color.White, 0.80f).copy(alpha = 1f)
        fieldMid = accents.primary.playerMix(Color.White, 0.91f).copy(alpha = 1f)
        base = accents.primary.playerMix(Color.White, 0.96f).copy(alpha = 1f)
        content = Color(0xFF111218)
        mutedAlpha = 0.68f
    } else {
        val ambience = playerAmbienceOf(accents.primary, accents.secondary)
        fieldTop = ambience.tint.playerMix(ambience.control, 0.30f).copy(alpha = 1f)
        fieldMid = ambience.elevated.playerMix(ambience.tint, 0.18f).copy(alpha = 1f)
        base = ambience.base
        content = Color.White
        mutedAlpha = 0.70f
    }
    val fields = listOf(fieldTop, fieldMid, base)
    val muted = content.copy(alpha = mutedAlpha)
        .playerAdjustForegroundToward(content, fields, PlayerMinimumContrast)
        .color
    val accent = accents.primary
        .playerAdjustForegroundToward(content, fields, PlayerMinimumContrast)
        .color
    return ImmersiveMediaColors(
        fieldTop = fieldTop,
        fieldMid = fieldMid,
        base = base,
        content = content,
        contentMuted = muted,
        accent = accent,
        actionStart = action.start,
        actionEnd = action.end,
        actionContent = action.content,
        secondaryFill = content.copy(alpha = 0.10f),
        hairline = content.copy(alpha = 0.09f)
    )
}

internal fun immersivePortraitHeroHeight(width: Dp, height: Dp): Dp {
    val viewportTarget = height * 0.54f
    return min(viewportTarget, width)
}

internal fun immersiveWideArtworkSize(width: Dp, height: Dp): Dp =
    min(300.dp, max(240.dp, min(width * 0.34f, height * 0.58f)))

internal fun immersiveHeroHeight(wide: Boolean, width: Dp, height: Dp, topBarHeight: Dp): Dp =
    if (wide) {
        topBarHeight + immersiveWideArtworkSize(width, height) + 20.dp
    } else {
        immersivePortraitHeroHeight(width, height)
    }

@Composable
internal fun animatedImmersiveMediaColors(
    target: ImmersiveMediaColors,
    animated: Boolean,
    labelPrefix: String
): ImmersiveMediaColors {
    val spec: AnimationSpec<Color> = if (animated) {
        tween(LevyraPlayerDesign.PaletteMillis)
    } else {
        snap()
    }
    val fieldTop by animateColorAsState(target.fieldTop, spec, label = "$labelPrefix-field-top")
    val fieldMid by animateColorAsState(target.fieldMid, spec, label = "$labelPrefix-field-mid")
    val base by animateColorAsState(target.base, spec, label = "$labelPrefix-base")
    val content by animateColorAsState(target.content, spec, label = "$labelPrefix-content")
    val contentMuted by animateColorAsState(target.contentMuted, spec, label = "$labelPrefix-content-muted")
    val accent by animateColorAsState(target.accent, spec, label = "$labelPrefix-accent")
    val actionStart by animateColorAsState(target.actionStart, spec, label = "$labelPrefix-action-start")
    val actionEnd by animateColorAsState(target.actionEnd, spec, label = "$labelPrefix-action-end")
    val actionContent by animateColorAsState(target.actionContent, spec, label = "$labelPrefix-action-content")
    val secondaryFill by animateColorAsState(target.secondaryFill, spec, label = "$labelPrefix-secondary-fill")
    val hairline by animateColorAsState(target.hairline, spec, label = "$labelPrefix-hairline")
    return target.copy(
        fieldTop = fieldTop,
        fieldMid = fieldMid,
        base = base,
        content = content,
        contentMuted = contentMuted,
        accent = accent,
        actionStart = actionStart,
        actionEnd = actionEnd,
        actionContent = actionContent,
        secondaryFill = secondaryFill,
        hairline = hairline
    )
}

@Composable
internal fun ImmersiveMediaHero(
    title: String,
    subtitle: String,
    metadata: String,
    colors: ImmersiveMediaColors,
    wide: Boolean,
    viewportWidth: Dp,
    viewportHeight: Dp,
    topBarHeight: Dp,
    onSubtitleClick: (() -> Unit)?,
    actions: @Composable () -> Unit,
    artwork: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    overline: String = ""
) {
    val heroHeight = immersiveHeroHeight(wide, viewportWidth, viewportHeight, topBarHeight)
    val gutter = immersiveMediaGutter(viewportWidth)
    if (wide) {
        val artworkSize = immersiveWideArtworkSize(viewportWidth, viewportHeight)
        val artworkShape = RoundedCornerShape(LevyraCardDesign.EditorialCorner)
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(heroHeight)
                .padding(top = topBarHeight, start = 32.dp, end = 32.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Box(
                modifier = Modifier
                    .size(artworkSize)
                    .shadow(
                        elevation = 18.dp,
                        shape = artworkShape,
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = 0.40f),
                        spotColor = Color.Black.copy(alpha = 0.55f)
                    )
                    .clip(artworkShape)
            ) {
                artwork()
            }
            Column(modifier = Modifier.weight(1f)) {
                ImmersiveMediaMetadata(
                    title = title,
                    overline = overline,
                    subtitle = subtitle,
                    metadata = metadata,
                    colors = colors,
                    wide = true,
                    onSubtitleClick = onSubtitleClick
                )
                Spacer(modifier = Modifier.height(18.dp))
                actions()
            }
        }
    } else {
        Column(modifier = modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heroHeight)
            ) {
                artwork()
                val scrim = remember(colors.base) { immersiveHeroScrim(colors.base) }
                Box(modifier = Modifier.fillMaxSize().background(scrim))
                ImmersiveMediaMetadata(
                    title = title,
                    overline = overline,
                    subtitle = subtitle,
                    metadata = "",
                    colors = colors,
                    wide = false,
                    onSubtitleClick = onSubtitleClick,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(start = gutter, end = gutter, bottom = 2.dp)
                )
            }
            val tail = remember(colors.base) {
                Brush.verticalGradient(0f to colors.base, 1f to colors.base.copy(alpha = 0f))
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(tail)
                    .padding(start = gutter, end = gutter, bottom = 12.dp)
            ) {
                if (metadata.isNotBlank()) {
                    Text(
                        text = metadata,
                        color = colors.contentMuted,
                        style = LevyraType.metadata,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = if (subtitle.isBlank()) 8.dp else 0.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                actions()
            }
        }
    }
}

private val ImmersiveMediaMinimumGutter: Dp = 20.dp
private val ImmersiveMediaContentMaxWidth: Dp = 680.dp

internal fun immersiveMediaGutter(width: Dp): Dp =
    max(ImmersiveMediaMinimumGutter, (width - ImmersiveMediaContentMaxWidth) / 2)

private fun immersiveHeroScrim(base: Color): Brush = Brush.verticalGradient(
    0f to Color.Transparent,
    0.36f to Color.Transparent,
    0.58f to base.copy(alpha = 0.50f),
    0.80f to base.copy(alpha = 0.88f),
    1f to base
)

@Composable
private fun ImmersiveMediaMetadata(
    title: String,
    overline: String,
    subtitle: String,
    metadata: String,
    colors: ImmersiveMediaColors,
    wide: Boolean,
    onSubtitleClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        if (overline.isNotBlank()) {
            Text(
                text = overline.uppercase(),
                color = colors.contentMuted,
                style = LevyraType.overline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
        ImmersiveMediaTitle(title, colors, wide)
        if (subtitle.isNotBlank()) {
            ImmersiveMediaSubtitle(subtitle, colors, onSubtitleClick)
        }
        if (metadata.isNotBlank()) {
            Text(
                text = metadata,
                color = colors.contentMuted,
                style = LevyraType.metadata,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ImmersiveMediaTitle(
    title: String,
    colors: ImmersiveMediaColors,
    wide: Boolean
) {
    val size = remember(title, wide) { LevyraType.heroTitleSize(title, wide) }
    Text(
        text = title,
        color = colors.content,
        style = LevyraType.heroTitle.copy(
            fontSize = size,
            lineHeight = LevyraType.heroLineHeight(size)
        ),
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.semantics { heading() }
    )
}

@Composable
private fun ImmersiveMediaSubtitle(
    subtitle: String,
    colors: ImmersiveMediaColors,
    onClick: (() -> Unit)?
) {
    val interactionModifier = if (onClick != null) {
        Modifier
            .heightIn(min = LevyraPlayerDesign.MinimumTouchTarget)
            .clip(LevyraPlayerDesign.ShapeXs)
            .levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Row)
    } else {
        Modifier.padding(vertical = 6.dp)
    }
    Box(modifier = interactionModifier, contentAlignment = Alignment.CenterStart) {
        Text(
            text = subtitle,
            color = colors.content.copy(alpha = 0.92f),
            style = LevyraType.artist,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun ImmersiveMediaActionRow(
    primary: ImmersiveMediaPrimaryAction,
    shuffleLabel: String,
    downloadLabel: String,
    colors: ImmersiveMediaColors,
    shuffleEnabled: Boolean,
    downloadEnabled: Boolean,
    onShuffle: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
    secondary: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ImmersiveMediaPlayButton(primary = primary, colors = colors)
        Spacer(modifier = Modifier.width(12.dp))
        ImmersiveMediaShuffleButton(
            label = shuffleLabel,
            enabled = shuffleEnabled,
            colors = colors,
            onClick = onShuffle
        )
        Spacer(modifier = Modifier.weight(1f))
        secondary()
        ImmersiveMediaQuietAction(
            icon = Icons.Rounded.Download,
            label = downloadLabel,
            enabled = downloadEnabled,
            colors = colors,
            onClick = onDownload
        )
    }
}

@Composable
private fun ImmersiveMediaPlayButton(
    primary: ImmersiveMediaPrimaryAction,
    colors: ImmersiveMediaColors
) {
    val interaction = remember { MutableInteractionSource() }
    val corner = levyraExpressiveCorner(
        interactionSource = interaction,
        rest = ImmersiveMediaActionHeight / 2,
        pressed = ImmersiveMediaPressedCorner,
        label = "immersive-play-corner"
    )
    val shape = RoundedCornerShape(corner)
    val fill = if (primary.enabled) colors.actionStart else colors.secondaryFill
    val content = if (primary.enabled) colors.actionContent else colors.contentMuted
    Row(
        modifier = Modifier
            .widthIn(min = 136.dp, max = 220.dp)
            .height(ImmersiveMediaActionHeight)
            .clip(shape)
            .background(fill)
            .levyraPressable(
                onClick = primary.onClick,
                enabled = primary.enabled && !primary.loading,
                pressedScale = LevyraPressScale.Control,
                interactionSource = interaction,
                role = Role.Button,
                onClickLabel = primary.contentDescription
            )
            .semantics(mergeDescendants = true) {}
            .padding(start = 22.dp, end = 26.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (primary.loading) {
            LevyraLoadingIndicator(
                color = content,
                modifier = Modifier.size(28.dp)
            )
        } else {
            Icon(
                imageVector = primary.icon,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = primary.label,
            color = content,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.2).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ImmersiveMediaShuffleButton(
    label: String,
    enabled: Boolean,
    colors: ImmersiveMediaColors,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val corner = levyraExpressiveCorner(
        interactionSource = interaction,
        rest = ImmersiveMediaActionHeight / 2,
        pressed = ImmersiveMediaPressedCorner,
        label = "immersive-shuffle-corner"
    )
    Box(
        modifier = Modifier
            .size(ImmersiveMediaActionHeight)
            .clip(RoundedCornerShape(corner))
            .background(colors.secondaryFill)
            .levyraPressable(
                onClick = onClick,
                enabled = enabled,
                pressedScale = LevyraPressScale.Control,
                interactionSource = interaction,
                role = Role.Button
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Shuffle,
            contentDescription = label,
            tint = if (enabled) colors.content else colors.contentMuted.copy(alpha = 0.45f),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
internal fun ImmersiveMediaQuietAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    colors: ImmersiveMediaColors,
    onClick: () -> Unit,
    active: Boolean = false,
    activeTint: Color = colors.accent,
    toggleable: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(LevyraPlayerDesign.MinimumTouchTarget)
            .clip(CircleShape)
            .levyraPressable(
                onClick = onClick,
                enabled = enabled,
                pressedScale = LevyraPressScale.Control,
                role = Role.Button
            )
            .semantics {
                if (toggleable) toggleableState = ToggleableState(active)
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = when {
                !enabled -> colors.contentMuted.copy(alpha = 0.40f)
                active -> activeTint
                else -> colors.contentMuted
            },
            modifier = Modifier.size(24.dp)
        )
    }
}

private val ImmersiveMediaActionHeight: Dp = 56.dp
private val ImmersiveMediaPressedCorner: Dp = 16.dp

@Composable
internal fun ImmersiveMediaTopBar(
    title: String,
    colors: ImmersiveMediaColors,
    collapsedState: State<Boolean>,
    height: Dp,
    animated: Boolean,
    backLabel: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    val collapsed by collapsedState
    val collapse by animateFloatAsState(
        targetValue = if (collapsed) 1f else 0f,
        animationSpec = if (animated) tween(220) else snap(),
        label = "immersive-media-topbar"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .drawBehind {
                if (collapse < 1f) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.42f * (1f - collapse)),
                            1f to Color.Transparent
                        )
                    )
                }
                if (collapse > 0f) drawRect(colors.fieldTop.copy(alpha = collapse * 0.96f))
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PlayerGlassIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = backLabel,
                onClick = onBack,
                size = ImmersiveTopBarButtonSize,
                iconSize = 21.dp,
                tint = Color.White,
                fill = ImmersiveTopBarButtonFill,
                borderTop = Color.Transparent,
                borderBottom = Color.Transparent
            )
            Text(
                text = title,
                color = colors.content,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.2).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer { alpha = collapse }
                    .then(if (collapse <= 0.05f) Modifier.clearAndSetSemantics {} else Modifier)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
    }
}

internal val ImmersiveTopBarButtonSize: Dp = 40.dp
internal val ImmersiveTopBarButtonFill: Color = Color.Black.copy(alpha = 0.30f)
