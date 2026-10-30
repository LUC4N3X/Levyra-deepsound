package com.luc4n3x.levyra.ui.player

import com.luc4n3x.levyra.ui.theme.*
import com.luc4n3x.levyra.ui.theme.LevyraIsNocturne

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp as OutlinedThumbUp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.PlayerGlassIconButton
import com.luc4n3x.levyra.ui.components.levyraMarquee
import com.luc4n3x.levyra.ui.components.rememberLevyraFavoriteMorphShape
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics

private const val FavoritePopScale = 1.14f
private const val TitleMarqueeDelayMs = 3_200

@Composable
internal fun PlayerTrackMetadata(
    track: Track,
    isFavorite: Boolean,
    surfaces: PlayerSurfaceTokens,
    animationsEnabled: Boolean,
    stepDirection: Int,
    compact: Boolean,
    openArtistLabel: String,
    favoritesLabel: String,
    onArtistClick: (Int) -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appleMusic = PlayerAppleMusicStyle
    val prism = PlayerPrismStyle
    val titleSize = metadataTitleSize(compact, appleMusic, prism)
    val artistSize = metadataArtistSize(compact, appleMusic)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (com.luc4n3x.levyra.ui.theme.LevyraIsAtelier) Spacer(modifier = Modifier.width(LevyraPlayerDesign.MinimumTouchTarget + LevyraPlayerDesign.SpaceMd))
        AnimatedContent(
            targetState = track,
            modifier = Modifier.weight(1f),
            transitionSpec = { LevyraMotion.trackChange(animationsEnabled, stepDirection) },
            contentKey = { it.id },
            label = "player-metadata"
        ) { shown ->
            val isCurrent = shown.id == track.id
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = if (com.luc4n3x.levyra.ui.theme.LevyraIsAtelier) Alignment.CenterHorizontally else Alignment.Start
            ) {
                Text(
                    text = shown.title,
                    color = if (LevyraIsQobuz) QobuzDesign.playerContent else surfaces.content,
                    fontSize = titleSize,
                    lineHeight = LevyraTypeRhythm.lineHeight(titleSize),
                    fontWeight = metadataTitleWeight(appleMusic, prism),
                    letterSpacing = if (LevyraIsQobuz) 0.sp else if (LevyraIsSpotify) (-0.4).sp else if (LevyraIsYouTubeMusic) (-0.2).sp else if (appleMusic) (-0.3).sp else (-0.8).sp,
                    maxLines = if (animationsEnabled) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.levyraMarquee(
                        enabled = animationsEnabled,
                        repeatDelayMillis = TitleMarqueeDelayMs
                    )
                )
                PlayerArtistText(
                    track = shown,
                    color = if (LevyraIsQobuz) QobuzDesign.playerContentSecondary else surfaces.contentMuted,
                    style = TextStyle(
                        fontFamily = MaterialTheme.typography.titleMedium.fontFamily,
                        fontSize = artistSize,
                        lineHeight = LevyraTypeRhythm.lineHeight(artistSize),
                        fontWeight = if (LevyraIsSpotify || appleMusic || LevyraIsYouTubeMusic || LevyraIsQobuz) FontWeight.Normal else FontWeight.Medium,
                        letterSpacing = (-0.1).sp
                    ),
                    onClickLabel = openArtistLabel,
                    onArtistClick = onArtistClick,
                    enabled = isCurrent
                )
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

private fun metadataTitleSize(compact: Boolean, appleMusic: Boolean, prism: Boolean): TextUnit = when {
    LevyraIsQobuz -> 18.sp
    com.luc4n3x.levyra.ui.theme.LevyraIsAtelier -> 20.sp
    LevyraIsYouTubeMusic -> 24.sp
    LevyraIsSpotify -> 22.sp
    appleMusic -> 20.sp
    prism -> 23.sp
    LevyraIsNocturne -> if (compact) 22.sp else 25.sp
    compact -> 23.sp
    else -> 27.sp
}

private fun metadataArtistSize(compact: Boolean, appleMusic: Boolean): TextUnit = when {
    LevyraIsQobuz -> 16.sp
    com.luc4n3x.levyra.ui.theme.LevyraIsAtelier -> 13.sp
    LevyraIsYouTubeMusic -> 16.sp
    LevyraIsSpotify -> 16.sp
    appleMusic -> 20.sp
    compact -> 15.sp
    else -> 16.5.sp
}

private fun metadataTitleWeight(appleMusic: Boolean, prism: Boolean): FontWeight = when {
    LevyraIsQobuz -> FontWeight.Bold
    com.luc4n3x.levyra.ui.theme.LevyraIsAtelier -> FontWeight.Medium
    LevyraIsYouTubeMusic -> FontWeight.Bold
    LevyraIsSpotify -> FontWeight.Bold
    appleMusic -> FontWeight.Medium
    LevyraIsNocturne || prism -> FontWeight.Medium
    else -> FontWeight.ExtraBold
}

@Composable
internal fun PlayerFavoriteButton(
    trackId: String,
    isFavorite: Boolean,
    surfaces: PlayerSurfaceTokens,
    label: String,
    animated: Boolean,
    onToggle: () -> Unit
) {
    val haptics = LocalLevyraHaptics.current
    val pop = rememberFavoritePop(trackId, isFavorite, animated)
    val handleToggle = {
        haptics.perform(LevyraHapticAction.Favorite)
        onToggle()
    }
    when {
        LevyraIsSpotify -> SpotifyPlayerFavoriteButton(isFavorite, label, pop.value, handleToggle)
        LevyraIsYouTubeMusic -> YouTubeMusicPlayerFavoriteButton(isFavorite, label, pop.value, handleToggle)
        LevyraIsQobuz -> QobuzPlayerFavoriteButton(isFavorite, label, handleToggle)
        PlayerAppleMusicStyle -> AppleMusicPlayerFavoriteButton(isFavorite, label, pop.value, handleToggle)
        else -> DefaultPlayerFavoriteButton(isFavorite, surfaces, label, animated, pop.value, handleToggle)
    }
}

@Composable
private fun SpotifyPlayerFavoriteButton(
    isFavorite: Boolean,
    label: String,
    scale: Float,
    onToggle: () -> Unit
) {
    IconButton(
        onClick = onToggle,
        modifier = Modifier.size(LevyraPlayerDesign.MinimumTouchTarget)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .semantics { toggleableState = ToggleableState(isFavorite) }
    ) {
        Icon(
            if (isFavorite) Icons.Rounded.CheckCircle else Icons.Rounded.AddCircleOutline, label,
            tint = if (isFavorite) SpotifyDesign.green else SpotifyDesign.text, modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun YouTubeMusicPlayerFavoriteButton(
    isFavorite: Boolean,
    label: String,
    scale: Float,
    onToggle: () -> Unit
) {
    IconButton(
        onClick = onToggle,
        modifier = Modifier.size(LevyraPlayerDesign.MinimumTouchTarget)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .semantics { toggleableState = ToggleableState(isFavorite) }
    ) {
        Icon(if (isFavorite) Icons.Rounded.ThumbUp else Icons.Outlined.OutlinedThumbUp, label, tint = Color.White, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun AppleMusicPlayerFavoriteButton(
    isFavorite: Boolean,
    label: String,
    scale: Float,
    onToggle: () -> Unit
) {
    IconButton(
        onClick = onToggle,
        modifier = Modifier.size(LevyraPlayerDesign.MinimumTouchTarget)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .semantics { toggleableState = ToggleableState(isFavorite) }
    ) {
        Box(
            Modifier.size(30.dp).clip(androidx.compose.foundation.shape.CircleShape)
                .background(if (isFavorite) AppleMusicVibrant.primary else AppleMusicVibrant.quaternary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder, label,
                tint = if (isFavorite) Color.Black.copy(alpha = 0.78f) else AppleMusicVibrant.primary, modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
private fun DefaultPlayerFavoriteButton(
    isFavorite: Boolean,
    surfaces: PlayerSurfaceTokens,
    label: String,
    animated: Boolean,
    scale: Float,
    onToggle: () -> Unit
) {
    val tint by animateColorAsState(
        targetValue = surfaces.tintFor(isFavorite, surfaces.content),
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.standardTween(200)),
        label = "player-favorite-tint"
    )
    val fill by animateColorAsState(
        targetValue = surfaces.fillFor(isFavorite),
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.standardTween(200)),
        label = "player-favorite-fill"
    )
    val outline = surfaces.segmentOutline
    PlayerGlassIconButton(
        icon = favoriteIcon(isFavorite),
        contentDescription = label,
        size = 44.dp,
        iconSize = 23.dp,
        tint = tint,
        fill = fill,
        borderTop = outline,
        borderBottom = outline,
        shape = if (PlayerPrismStyle) androidx.compose.foundation.shape.CircleShape else rememberLevyraFavoriteMorphShape(favorite = isFavorite, animated = animated),
        modifier = Modifier
            .size(LevyraPlayerDesign.MinimumTouchTarget)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .semantics { toggleableState = ToggleableState(isFavorite) },
        onClick = onToggle
    )
}

internal fun shouldTriggerFavoritePop(
    previousTrackId: String?,
    currentTrackId: String,
    wasFavorite: Boolean,
    isFavorite: Boolean,
    animated: Boolean
): Boolean =
    animated && previousTrackId == currentTrackId && !wasFavorite && isFavorite

@Composable
internal fun rememberFavoritePop(
    trackId: String,
    isFavorite: Boolean,
    animated: Boolean
): Animatable<Float, *> {
    val pop = remember(trackId) { Animatable(1f) }
    var wasFavorite by remember(trackId) { mutableStateOf(isFavorite) }

    LaunchedEffect(trackId, isFavorite, animated) {
        val shouldPop = shouldTriggerFavoritePop(
            previousTrackId = trackId,
            currentTrackId = trackId,
            wasFavorite = wasFavorite,
            isFavorite = isFavorite,
            animated = animated
        )
        wasFavorite = isFavorite

        if (!shouldPop) {
            pop.snapTo(1f)
            return@LaunchedEffect
        }
        pop.animateTo(FavoritePopScale, tween(LevyraMotion.Durations.Instant, easing = LevyraMotion.Easings.Decelerate))
        pop.animateTo(1f, LevyraMotion.feedback.spec())
    }
    return pop
}

private fun favoriteIcon(isFavorite: Boolean): ImageVector =
    if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder
