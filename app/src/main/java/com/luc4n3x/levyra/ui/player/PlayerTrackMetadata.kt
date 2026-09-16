package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.PlayerGlassIconButton
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics

private const val FavoritePopScale = 1.22f
private const val TitleMarqueeDelayMs = 3_200

@Composable
internal fun PlayerTrackMetadata(
    track: Track,
    isFavorite: Boolean,
    surfaces: PlayerSurfaceTokens,
    animationsEnabled: Boolean,
    compact: Boolean,
    openArtistLabel: String,
    favoritesLabel: String,
    onArtistClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val titleSize = if (compact) 22.sp else 26.sp
    val artistSize = if (compact) 15.sp else 17.sp

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedContent(
            targetState = track,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                if (animationsEnabled) {
                    (fadeIn(LevyraPlayerDesign.standardTween(260)) +
                        slideInVertically(LevyraPlayerDesign.smoothSpring()) { it / 5 }) togetherWith
                        (fadeOut(LevyraPlayerDesign.standardTween(120)) +
                            slideOutVertically(LevyraPlayerDesign.standardTween(160)) { -it / 5 })
                } else {
                    EnterTransition.None togetherWith ExitTransition.None
                }
            },
            contentKey = { it.id },
            label = "player-metadata"
        ) { shown ->
            Column {
                Text(
                    text = shown.title,
                    color = surfaces.content,
                    fontSize = titleSize,
                    lineHeight = LevyraTypeRhythm.lineHeight(titleSize),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.6).sp,
                    maxLines = if (animationsEnabled) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (animationsEnabled) {
                        Modifier.basicMarquee(
                            iterations = Int.MAX_VALUE,
                            repeatDelayMillis = TitleMarqueeDelayMs
                        )
                    } else {
                        Modifier
                    }
                )
                Row(
                    modifier = Modifier
                        .heightIn(min = LevyraPlayerDesign.MinimumTouchTarget)
                        .widthIn(min = LevyraPlayerDesign.MinimumTouchTarget)
                        .clip(LevyraPlayerDesign.ShapeXxs)
                        .clickable(
                            onClickLabel = openArtistLabel,
                            onClick = onArtistClick
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = shown.artist,
                        color = surfaces.contentMuted,
                        fontSize = artistSize,
                        lineHeight = LevyraTypeRhythm.lineHeight(artistSize),
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.1).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(LevyraPlayerDesign.SpaceMd))
        PlayerFavoriteButton(
            trackId = track.id,
            isFavorite = isFavorite,
            surfaces = surfaces,
            label = favoritesLabel,
            animated = animationsEnabled,
            onToggle = onToggleFavorite
        )
    }
}

@Composable
private fun PlayerFavoriteButton(
    trackId: String,
    isFavorite: Boolean,
    surfaces: PlayerSurfaceTokens,
    label: String,
    animated: Boolean,
    onToggle: () -> Unit
) {
    val haptics = LocalLevyraHaptics.current
    val pop = remember { Animatable(1f) }
    var settled by remember(trackId) { mutableStateOf(isFavorite) }
    LaunchedEffect(trackId, isFavorite, animated) {
        if (settled == isFavorite) return@LaunchedEffect
        settled = isFavorite
        if (animated && isFavorite) {
            pop.snapTo(FavoritePopScale)
            pop.animateTo(1f, LevyraPlayerDesign.expressiveSpring())
        }
    }
    val tint by animateColorAsState(
        targetValue = if (isFavorite) surfaces.activeContent else surfaces.content,
        animationSpec = if (animated) LevyraPlayerDesign.standardTween(200) else snap(),
        label = "player-favorite-tint"
    )
    val fill by animateColorAsState(
        targetValue = if (isFavorite) surfaces.active else surfaces.controlQuiet,
        animationSpec = if (animated) LevyraPlayerDesign.standardTween(200) else snap(),
        label = "player-favorite-fill"
    )
    PlayerGlassIconButton(
        icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
        contentDescription = label,
        size = 44.dp,
        iconSize = 23.dp,
        tint = tint,
        fill = fill,
        borderTop = if (surfaces.amoled) surfaces.outline else Color.Transparent,
        borderBottom = if (surfaces.amoled) surfaces.outline else Color.Transparent,
        shape = CircleShape,
        modifier = Modifier
            .size(LevyraPlayerDesign.MinimumTouchTarget)
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            }
            .semantics { toggleableState = ToggleableState(isFavorite) },
        onClick = {
            haptics.perform(LevyraHapticAction.Favorite)
            onToggle()
        }
    )
}
