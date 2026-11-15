package com.luc4n3x.levyra.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.max
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraExpressiveCorner
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LevyraType

@Composable
internal fun CenteredAlbumHero(
    title: String,
    overline: String,
    subtitle: String,
    metadata: String,
    colors: ImmersiveMediaColors,
    viewportWidth: Dp,
    viewportHeight: Dp,
    artworkAspect: Float?,
    onSubtitleClick: (() -> Unit)?,
    actions: @Composable () -> Unit,
    artwork: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    val gutter = immersiveMediaGutter(viewportWidth)
    val heroHeight by animateDpAsState(
        centeredHeroHeight(viewportWidth, viewportHeight, artworkAspect),
        tween(LevyraPlayerDesign.PaletteMillis),
        label = "album-hero-height"
    )
    val fade = remember(colors.base) {
        Brush.verticalGradient(
            0f to Color.Transparent,
            0.45f to Color.Transparent,
            0.72f to colors.base.copy(alpha = 0.72f),
            1f to colors.base
        )
    }
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight)
        ) {
            artwork()
            Box(Modifier.fillMaxSize().background(fade))
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = gutter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    color = colors.content,
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
                if (subtitle.isNotBlank()) {
                    val subtitleModifier = if (onSubtitleClick != null) {
                        Modifier
                            .heightIn(min = LevyraPlayerDesign.MinimumTouchTarget)
                            .clip(LevyraPlayerDesign.ShapePill)
                            .levyraPressable(onClick = onSubtitleClick, role = Role.Button, pressedScale = LevyraPressScale.Row)
                            .padding(horizontal = 12.dp)
                    } else {
                        Modifier.padding(vertical = 8.dp)
                    }
                    Box(subtitleModifier, contentAlignment = Alignment.Center) {
                        Text(
                            text = subtitle,
                            color = colors.content.copy(alpha = 0.92f),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        val details = listOf(overline, metadata).filter { it.isNotBlank() }.joinToString(" · ")
        if (details.isNotBlank()) {
            Text(
                text = details,
                color = colors.contentMuted,
                style = LevyraType.metadata,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = gutter)
            )
        }
        Spacer(Modifier.height(18.dp))
        actions()
        Spacer(Modifier.height(16.dp))
    }
}

internal fun centeredHeroHeight(width: Dp, height: Dp, artworkAspect: Float?): Dp {
    val base = immersiveHeroHeight(false, width, height, 0.dp)
    val aspect = artworkAspect?.takeIf { it.isFinite() && it > 0f } ?: return base
    val upper = max(base, height * 0.82f)
    return (width / aspect).coerceIn(minOf(width * 0.5f, upper), upper)
}

@Composable
internal fun CenteredMediaActionRow(
    primary: ImmersiveMediaPrimaryAction,
    shuffleLabel: String,
    colors: ImmersiveMediaColors,
    shuffleEnabled: Boolean,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
    secondary: @Composable RowScope.() -> Unit = {}
) {
    val ring = colors.content.copy(alpha = 0.32f)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.size(RingActionSize).border(1.5.dp, ring, CircleShape),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = secondary
        )
        PrimaryAction(primary, colors)
        Box(
            modifier = Modifier
                .size(RingActionSize)
                .clip(CircleShape)
                .border(1.5.dp, ring, CircleShape)
                .levyraPressable(onClick = onShuffle, enabled = shuffleEnabled, role = Role.Button, pressedScale = LevyraPressScale.Control, onClickLabel = shuffleLabel)
                .semantics { contentDescription = shuffleLabel },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = if (shuffleEnabled) colors.content else colors.contentMuted, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun PrimaryAction(primary: ImmersiveMediaPrimaryAction, colors: ImmersiveMediaColors) {
    val interaction = remember { MutableInteractionSource() }
    val corner = levyraExpressiveCorner(
        interactionSource = interaction,
        rest = PrimaryActionSize / 2,
        pressed = PrimaryActionPressedCorner,
        label = "album-primary-corner"
    )
    val content = if (primary.enabled) colors.actionContent else colors.contentMuted
    Box(
        modifier = Modifier
            .size(PrimaryActionSize)
            .clip(RoundedCornerShape(corner))
            .background(if (primary.enabled) colors.actionStart else colors.secondaryFill)
            .levyraPressable(
                onClick = primary.onClick,
                enabled = primary.enabled && !primary.loading,
                pressedScale = LevyraPressScale.Control,
                interactionSource = interaction,
                role = Role.Button,
                onClickLabel = primary.contentDescription
            )
            .semantics { contentDescription = primary.contentDescription },
        contentAlignment = Alignment.Center
    ) {
        if (primary.loading) {
            LevyraLoadingIndicator(color = content, modifier = Modifier.size(32.dp))
        } else {
            Icon(primary.icon, contentDescription = null, tint = content, modifier = Modifier.size(38.dp))
        }
    }
}

private val RingActionSize: Dp = 52.dp
private val PrimaryActionSize: Dp = 72.dp
private val PrimaryActionPressedCorner: Dp = 24.dp
