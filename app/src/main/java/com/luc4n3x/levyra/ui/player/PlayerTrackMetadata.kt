package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.PlayerGlassIconButton
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign

@Composable
internal fun PlayerTrackMetadata(
    track: Track,
    isFavorite: Boolean,
    favoriteScale: Float,
    favoriteTint: Color,
    favoriteFill: Color,
    favoriteBorderTop: Color,
    favoriteBorderBottom: Color,
    animationsEnabled: Boolean,
    compact: Boolean,
    openArtistLabel: String,
    favoritesLabel: String,
    addToPlaylistLabel: String,
    onArtistClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
    engagementContent: (@Composable () -> Unit)? = null
) {
    val heartButtonSize = if (compact) 38.dp else 40.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LevyraPlayerDesign.SpaceXxs)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = track,
                    transitionSpec = {
                        if (animationsEnabled) {
                            (fadeIn(tween(240)) + slideInVertically(tween(240)) { it / 4 }) togetherWith
                                (fadeOut(tween(140)) + slideOutVertically(tween(140)) { -it / 4 })
                        } else {
                            EnterTransition.None togetherWith ExitTransition.None
                        }
                    },
                    contentKey = { it.id },
                    label = "player-title"
                ) { titleTrack ->
                    Text(
                        text = titleTrack.title,
                        color = LevyraPlayerDesign.TextPrimary,
                        fontSize = if (compact) 22.sp else 25.sp,
                        lineHeight = if (compact) 26.sp else 29.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.4).sp,
                        maxLines = if (animationsEnabled) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = if (animationsEnabled) {
                            Modifier.basicMarquee(
                                iterations = Int.MAX_VALUE,
                                repeatDelayMillis = 3_200
                            )
                        } else {
                            Modifier
                        }
                    )
                }
                Row(
                    modifier = Modifier
                        .heightIn(min = LevyraPlayerDesign.MinimumTouchTarget)
                        .clip(LevyraPlayerDesign.ShapePill)
                        .clickable(
                            onClickLabel = openArtistLabel,
                            onClick = onArtistClick
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = track.artist,
                        color = LevyraPlayerDesign.TextSecondary,
                        fontSize = if (compact) 14.sp else 15.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.1).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(LevyraPlayerDesign.SpaceSm))
            PlayerGlassIconButton(
                icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = favoritesLabel,
                size = heartButtonSize,
                iconSize = if (compact) 22.dp else 24.dp,
                tint = favoriteTint,
                fill = favoriteFill,
                borderTop = favoriteBorderTop,
                borderBottom = favoriteBorderBottom,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = favoriteScale
                        scaleY = favoriteScale
                    }
                    .semantics { toggleableState = ToggleableState(isFavorite) },
                onClick = onToggleFavorite
            )
            Spacer(modifier = Modifier.width(LevyraPlayerDesign.SpaceXs))
            PlayerGlassIconButton(
                icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                contentDescription = addToPlaylistLabel,
                size = heartButtonSize,
                iconSize = if (compact) 22.dp else 24.dp,
                tint = Color.White.copy(alpha = 0.78f),
                fill = Color.White.copy(alpha = 0.05f),
                borderTop = Color.White.copy(alpha = 0.14f),
                borderBottom = Color.White.copy(alpha = 0.05f),
                onClick = onAddToPlaylist
            )
        }
        engagementContent?.invoke()
    }
}
