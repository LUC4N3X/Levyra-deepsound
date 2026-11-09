package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.luc4n3x.levyra.ui.components.levyraExpressiveCorner
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.YoutubeComment
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.PlayerIcon
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign

@Immutable
internal class PlayerCommentsTeaser(
    val trackId: String,
    val countBadge: String,
    val topComments: List<YoutubeComment>,
    val loading: Boolean,
    val fallbackText: String,
    val onOpen: () -> Unit
)

@Composable
internal fun PlayerCommentsTeaserCard(
    teaser: PlayerCommentsTeaser,
    surfaces: PlayerSurfaceTokens,
    modifier: Modifier = Modifier
) {
    val lead = teaser.topComments.firstOrNull()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner = levyraExpressiveCorner(
        interactionSource = interaction,
        rest = TeaserRestCorner,
        pressed = TeaserPressedCorner,
        label = "player-comments-teaser-corner"
    )
    val wash by animateColorAsState(
        targetValue = if (pressed) surfaces.tonal else Color.Transparent,
        label = "player-comments-teaser-wash"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = LevyraPlayerDesign.MinimumTouchTarget)
            .clip(RoundedCornerShape(corner))
            .background(wash)
            .levyraPressable(
                onClick = teaser.onOpen,
                interactionSource = interaction,
                pressedScale = LevyraPressScale.Control,
                role = Role.Button,
                haptic = LevyraHapticAction.Confirm
            )
            .semantics { contentDescription = teaser.fallbackText }
            .padding(horizontal = LevyraPlayerDesign.SpaceXs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm)
    ) {
        TeaserAvatarStack(teaser.topComments, surfaces)
        AnimatedContent(
            targetState = lead,
            modifier = Modifier.weight(1f),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            contentKey = { it?.id },
            label = "player-comments-teaser-text"
        ) { comment ->
            Text(
                text = comment?.text?.replace('\n', ' ')?.let { "“$it”" } ?: teaser.fallbackText,
                color = surfaces.contentMuted,
                fontSize = 13.sp,
                fontStyle = if (comment != null) FontStyle.Italic else FontStyle.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        TeaserCount(teaser, surfaces)
    }
}

@Composable
private fun TeaserAvatarStack(comments: List<YoutubeComment>, surfaces: PlayerSurfaceTokens) {
    val avatars = comments.take(TeaserAvatarCount)
    if (avatars.isEmpty()) {
        PlayerIcon(Icons.Rounded.ChatBubbleOutline, surfaces.contentMuted, Modifier.size(18.dp))
        return
    }
    Box {
        avatars.forEachIndexed { index, comment ->
            Box(
                modifier = Modifier
                    .offset(x = TeaserAvatarStep * index)
                    .size(TeaserAvatarSize)
                    .clip(CircleShape)
                    .background(surfaces.control)
                    .border(1.5.dp, Color.Black.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = comment.author.trimStart('@').take(1).uppercase(),
                    color = surfaces.content,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                if (comment.authorAvatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = comment.authorAvatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(TeaserAvatarSize)
                    )
                }
            }
        }
        Box(Modifier.size(TeaserAvatarSize + TeaserAvatarStep * (avatars.size - 1), TeaserAvatarSize))
    }
}

@Composable
private fun TeaserCount(teaser: PlayerCommentsTeaser, surfaces: PlayerSurfaceTokens) {
    Row(
        modifier = Modifier
            .height(TeaserPillHeight)
            .clip(CircleShape)
            .background(surfaces.tonal)
            .padding(horizontal = LevyraPlayerDesign.SpaceSm + LevyraPlayerDesign.SpaceXxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceXs + LevyraPlayerDesign.SpaceXxs)
    ) {
        PlayerIcon(Icons.Rounded.ChatBubbleOutline, surfaces.content, Modifier.size(14.dp))
        when {
            teaser.loading && teaser.countBadge.isBlank() -> LevyraLoadingIndicator(
                color = surfaces.content,
                modifier = Modifier.size(12.dp)
            )
            teaser.countBadge.isNotBlank() -> Text(
                text = teaser.countBadge,
                color = surfaces.content,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

private const val TeaserAvatarCount = 3
private val TeaserAvatarSize = 22.dp
private val TeaserAvatarStep = 14.dp
private val TeaserPillHeight = 30.dp
private val TeaserRestCorner = 16.dp
private val TeaserPressedCorner = 24.dp
