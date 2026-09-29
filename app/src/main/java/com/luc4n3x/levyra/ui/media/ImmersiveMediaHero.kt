package com.luc4n3x.levyra.ui.media

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.PlayerMinimumContrast
import com.luc4n3x.levyra.ui.components.PlayerGlassIconButton
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.harmonizePlayerAccents
import com.luc4n3x.levyra.ui.playerAdjustForegroundToward
import com.luc4n3x.levyra.ui.playerAmbienceOf
import com.luc4n3x.levyra.ui.playerContrastGradient
import com.luc4n3x.levyra.ui.playerMix
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign

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
    val viewportTarget = height * 0.42f
    val widthGuard = width * 1.08f
    return min(viewportTarget, widthGuard)
}

internal fun immersiveWideArtworkSize(width: Dp, height: Dp): Dp =
    min(300.dp, max(240.dp, min(width * 0.34f, height * 0.58f)))

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
    modifier: Modifier = Modifier
) {
    if (wide) {
        val artworkSize = immersiveWideArtworkSize(viewportWidth, viewportHeight)
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = topBarHeight, start = 32.dp, end = 32.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(artworkSize)
                    .clip(RoundedCornerShape(LevyraPlayerDesign.CornerLg))
            ) {
                artwork()
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ImmersiveMediaMetadata(
                    title = title,
                    subtitle = subtitle,
                    metadata = metadata,
                    colors = colors,
                    centered = false,
                    onSubtitleClick = onSubtitleClick
                )
                Spacer(modifier = Modifier.height(10.dp))
                actions()
            }
        }
    } else {
        val heroHeight = immersivePortraitHeroHeight(viewportWidth, viewportHeight)
        Column(modifier = modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heroHeight)
            ) {
                artwork()
                val scrim = remember(colors.fieldTop, colors.base) {
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.34f to Color.Transparent,
                        0.60f to colors.fieldTop.copy(alpha = 0.26f),
                        0.80f to colors.fieldMid.copy(alpha = 0.82f),
                        1f to colors.base
                    )
                }
                Box(modifier = Modifier.fillMaxSize().background(scrim))
                ImmersiveMediaMetadata(
                    title = title,
                    subtitle = subtitle,
                    metadata = metadata,
                    colors = colors.copy(
                        content = Color.White,
                        contentMuted = Color.White.copy(alpha = 0.78f),
                        accent = Color.White
                    ),
                    centered = true,
                    onSubtitleClick = onSubtitleClick,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 16.dp)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.base)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                actions()
            }
        }
    }
}

@Composable
private fun ImmersiveMediaMetadata(
    title: String,
    subtitle: String,
    metadata: String,
    colors: ImmersiveMediaColors,
    centered: Boolean,
    onSubtitleClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val horizontal = if (centered) Alignment.CenterHorizontally else Alignment.Start
    val textAlign = if (centered) TextAlign.Center else TextAlign.Start
    Column(modifier = modifier, horizontalAlignment = horizontal) {
        ImmersiveMediaTitle(title, colors, centered, textAlign)
        if (subtitle.isNotBlank()) {
            ImmersiveMediaSubtitle(subtitle, colors, textAlign, onSubtitleClick)
        }
        if (metadata.isNotBlank()) {
            Text(
                text = metadata,
                color = colors.contentMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = textAlign,
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
    centered: Boolean,
    textAlign: TextAlign
) {
    Text(
        text = title,
        color = colors.content,
        fontSize = if (centered) 28.sp else 30.sp,
        lineHeight = if (centered) 32.sp else 34.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.8).sp,
        textAlign = textAlign,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.semantics { heading() }
    )
}

@Composable
private fun ImmersiveMediaSubtitle(
    subtitle: String,
    colors: ImmersiveMediaColors,
    textAlign: TextAlign,
    onClick: (() -> Unit)?
) {
    val interactive = onClick != null
    val interactionModifier = if (onClick != null) {
        Modifier
            .clip(LevyraPlayerDesign.ShapePill)
            .levyraPressable(onClick = onClick, role = Role.Button)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    } else {
        Modifier.padding(top = 4.dp)
    }
    Text(
        text = subtitle,
        color = if (interactive) colors.accent else colors.contentMuted,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = textAlign,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .heightIn(min = if (interactive) 40.dp else 24.dp)
            .then(interactionModifier)
    )
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
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.widthIn(max = 430.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ImmersiveMediaSideAction(
            icon = Icons.Rounded.Shuffle,
            label = shuffleLabel,
            enabled = shuffleEnabled,
            colors = colors,
            onClick = onShuffle
        )
        Box(
            modifier = Modifier
                .weight(1f, fill = false)
                .widthIn(min = 132.dp, max = 190.dp)
                .height(48.dp)
                .shadow(
                    elevation = if (primary.enabled) 12.dp else 0.dp,
                    shape = LevyraPlayerDesign.ShapePill,
                    clip = false,
                    spotColor = colors.actionStart.copy(alpha = 0.45f)
                )
                .clip(LevyraPlayerDesign.ShapePill)
                .background(
                    if (primary.enabled) {
                        Brush.horizontalGradient(listOf(colors.actionStart, colors.actionEnd))
                    } else {
                        Brush.horizontalGradient(listOf(colors.secondaryFill, colors.secondaryFill))
                    }
                )
                .levyraPressable(
                    onClick = primary.onClick,
                    enabled = primary.enabled && !primary.loading,
                    role = Role.Button,
                    onClickLabel = primary.contentDescription
                )
                .semantics(mergeDescendants = true) {},
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (primary.loading) {
                    CircularProgressIndicator(
                        color = colors.actionContent,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = primary.icon,
                        contentDescription = null,
                        tint = if (primary.enabled) colors.actionContent else colors.contentMuted,
                        modifier = Modifier.size(23.dp)
                    )
                }
                Text(
                    text = primary.label,
                    color = if (primary.enabled) colors.actionContent else colors.contentMuted,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        ImmersiveMediaSideAction(
            icon = Icons.Rounded.Download,
            label = downloadLabel,
            enabled = downloadEnabled,
            colors = colors,
            onClick = onDownload
        )
    }
}

@Composable
private fun ImmersiveMediaSideAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    colors: ImmersiveMediaColors,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .size(48.dp)
            .clip(CircleShape)
            .background(colors.secondaryFill)
            .border(LevyraPlayerDesign.Hairline, colors.hairline, CircleShape)
            .levyraPressable(onClick = onClick, enabled = enabled, role = Role.Button),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) colors.content else colors.contentMuted.copy(alpha = 0.45f),
            modifier = Modifier.size(22.dp)
        )
    }
}

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
                size = 48.dp,
                iconSize = 21.dp,
                tint = Color.White,
                fill = Color.Black.copy(alpha = 0.34f),
                borderTop = Color.White.copy(alpha = 0.18f),
                borderBottom = Color.White.copy(alpha = 0.08f)
            )
            Text(
                text = title,
                color = colors.content,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
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
