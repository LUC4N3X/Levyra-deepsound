package com.luc4n3x.levyra.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraType

@Composable
internal fun LevyraPlayAllButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String = LocalLevyraStrings.current.playAll
) {
    if (com.luc4n3x.levyra.ui.theme.LevyraIsSpotify) {
        SpotifyTextAction(label, onClick, modifier, enabled)
        return
    }
    val colors = MaterialTheme.colorScheme
    val animated = LocalAnimationsEnabled.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner = levyraExpressiveCorner(
        interactionSource = interaction,
        rest = PlayAllRestCorner,
        pressed = PlayAllPressedCorner,
        label = "play-all-corner"
    )
    Box(
        modifier = modifier
            .heightIn(min = PlayAllTouchTarget)
            .levyraPressable(
                onClick = onClick,
                interactionSource = interaction,
                role = Role.Button,
                enabled = enabled,
                pressedScale = LevyraPressScale.Control
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = PlayAllVisualHeight)
                .clip(RoundedCornerShape(corner))
                .background(colors.primaryContainer.copy(alpha = if (enabled) 1f else 0.48f))
                .padding(start = 4.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            PlayAllDisc(size = PlayAllDiscSize, pressed = pressed, animated = animated)
            Text(
                text = label,
                color = colors.onPrimaryContainer,
                style = LevyraType.cardTitle.copy(
                    fontSize = 12.5.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun LevyraPlayAllIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = PlayAllVisualHeight,
    contentDescription: String = LocalLevyraStrings.current.playAll
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .size(maxOf(size, PlayAllTouchTarget))
            .levyraPressable(
                onClick = onClick,
                interactionSource = interaction,
                role = Role.Button,
                pressedScale = LevyraPressScale.Control
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        PlayAllDisc(size = size, pressed = pressed, animated = LocalAnimationsEnabled.current)
    }
}

@Composable
private fun PlayAllDisc(size: Dp, pressed: Boolean, animated: Boolean) {
    val colors = MaterialTheme.colorScheme
    val shape = rememberLevyraAvatarPressShape(pressed = pressed, animated = animated)
    val nudge by animateDpAsState(
        targetValue = if (pressed && animated) 1.5.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "play-all-nudge"
    )
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(colors.primary),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = colors.onPrimary,
            modifier = Modifier
                .offset(x = nudge)
                .size(size * 0.62f)
        )
    }
}

private val PlayAllTouchTarget = 48.dp
private val PlayAllVisualHeight = 34.dp
private val PlayAllDiscSize = 26.dp
private val PlayAllRestCorner = 24.dp
private val PlayAllPressedCorner = 12.dp
