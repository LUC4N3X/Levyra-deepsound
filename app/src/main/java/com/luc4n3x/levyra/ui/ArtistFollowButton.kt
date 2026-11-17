package com.luc4n3x.levyra.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraExpressiveCorner
import com.luc4n3x.levyra.ui.components.levyraExpressiveToggleCorner
import com.luc4n3x.levyra.ui.components.levyraMarquee
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraType

@Composable
internal fun ArtistFollowButton(isFollowed: Boolean, onClick: () -> Unit) {
    val strings = LocalLevyraStrings.current
    if (SkinFollowPill(isFollowed, strings.followArtist, strings.followingArtist, onClick)) return
    val animated = LocalAnimationsEnabled.current
    val followProgress by animateFloatAsState(
        targetValue = if (isFollowed) 1f else 0f,
        animationSpec = LevyraMotion.spec(animated, LevyraMotion.standard()),
        label = "artist-follow-progress"
    )
    val container by animateColorAsState(
        targetValue = if (isFollowed) Color.Transparent else LevyraText.copy(alpha = 0.10f),
        animationSpec = LevyraMotion.spec(animated, LevyraMotion.standard()),
        label = "artist-follow-container"
    )
    val outline by animateColorAsState(
        targetValue = LevyraText.copy(alpha = if (isFollowed) 0.24f else 0f),
        animationSpec = LevyraMotion.spec(animated, LevyraMotion.standard()),
        label = "artist-follow-outline"
    )
    val interaction = remember { MutableInteractionSource() }
    val corner = levyraExpressiveToggleCorner(
        checked = isFollowed,
        unchecked = 24.dp,
        checkedCorner = 14.dp,
        label = "artist-follow-corner"
    )
    val pressedCorner = levyraExpressiveCorner(
        interactionSource = interaction,
        rest = corner,
        pressed = 12.dp,
        label = "artist-follow-press"
    )
    Surface(
        color = container,
        shape = RoundedCornerShape(pressedCorner),
        border = BorderStroke(1.dp, outline),
        modifier = Modifier
            .heightIn(min = 48.dp)
            .levyraPressable(
                onClick = onClick,
                pressedScale = LevyraPressScale.Control,
                interactionSource = interaction,
                role = Role.Switch,
                haptic = LevyraHapticAction.Confirm
            )
            .clearAndSetSemantics {
                contentDescription = strings.followArtist
                toggleableState = ToggleableState(isFollowed)
                role = Role.Switch
            }
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            ArtistFollowLabel(
                icon = Icons.Rounded.PersonAdd,
                text = strings.followArtist,
                color = LevyraText,
                visibility = { 1f - followProgress }
            )
            ArtistFollowLabel(
                icon = Icons.Rounded.Check,
                text = strings.followingArtist,
                color = LevyraText,
                visibility = { followProgress }
            )
        }
    }
}

@Composable
private fun ArtistFollowLabel(
    icon: ImageVector,
    text: String,
    color: Color,
    visibility: () -> Float
) {
    Row(
        modifier = Modifier.graphicsLayer {
            val shown = visibility()
            alpha = shown
            val scale = 0.92f + 0.08f * shown
            scaleX = scale
            scaleY = scale
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Text(
            text = text,
            color = color,
            style = LevyraType.metadata.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            modifier = Modifier.weight(1f, fill = false).levyraMarquee()
        )
    }
}
