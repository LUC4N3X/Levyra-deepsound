package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.luc4n3x.levyra.data.LevyraArtworkCache
import com.luc4n3x.levyra.domain.PlayerBackgroundMode
import com.luc4n3x.levyra.ui.PlayerAmbience
import com.luc4n3x.levyra.ui.createPlayerAmbientColorMatrix

@Composable
internal fun PlayerBackdrop(
    mode: PlayerBackgroundMode,
    artworkUrl: String,
    ambience: PlayerAmbience,
    isPlaying: Boolean,
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    when (mode) {
        PlayerBackgroundMode.PureBlack -> {
            Box(modifier = modifier.background(Color.Black))
        }
        PlayerBackgroundMode.Dark -> {
            Box(
                modifier = modifier.background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF14151B),
                            Color(0xFF0C0D11),
                            Color(0xFF060709)
                        )
                    )
                )
            )
        }
        PlayerBackgroundMode.Blur -> {
            Box(modifier = modifier.background(Color.Black)) {
                if (artworkUrl.isNotBlank()) {
                    AsyncImage(
                        model = remember(context, artworkUrl) {
                            ImageRequest.Builder(context)
                                .data(LevyraArtworkCache.large(artworkUrl))
                                .size(512, 512)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .crossfade(false)
                                .build()
                        },
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(64.dp)
                            .graphicsLayer {
                                scaleX = 1.20f
                                scaleY = 1.20f
                                alpha = if (isPlaying) 0.52f else 0.40f
                            }
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.58f))
                )
            }
        }
        PlayerBackgroundMode.Dynamic -> {
            val primary = animateColorAsState(
                targetValue = ambience.primary,
                animationSpec = if (animationsEnabled) tween(900, easing = LinearOutSlowInEasing) else snap(),
                label = "player-backdrop-primary"
            )
            val secondary = animateColorAsState(
                targetValue = ambience.secondary,
                animationSpec = if (animationsEnabled) tween(900, easing = LinearOutSlowInEasing) else snap(),
                label = "player-backdrop-secondary"
            )
            val base = animateColorAsState(
                targetValue = ambience.base,
                animationSpec = if (animationsEnabled) tween(900, easing = LinearOutSlowInEasing) else snap(),
                label = "player-backdrop-base"
            )
            val ambientMatrix = remember { createPlayerAmbientColorMatrix() }
            val ambientColorFilter = remember(ambientMatrix) { ColorFilter.colorMatrix(ambientMatrix) }

            Box(modifier = modifier) {
                Box(modifier = Modifier.fillMaxSize().background(base.value))
                if (artworkUrl.isNotBlank()) {
                    AnimatedContent(
                        targetState = artworkUrl,
                        transitionSpec = {
                            if (animationsEnabled) {
                                fadeIn(tween(440, easing = LinearOutSlowInEasing)) togetherWith
                                    fadeOut(tween(300, easing = FastOutSlowInEasing))
                            } else {
                                EnterTransition.None togetherWith ExitTransition.None
                            }
                        },
                        label = "player-backdrop-artwork"
                    ) { url ->
                        val ambientImageRequest = remember<ImageRequest>(context, url) {
                            ImageRequest.Builder(context)
                                .data(LevyraArtworkCache.large(url))
                                .size(512, 512)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .crossfade(false)
                                .build()
                        }
                        AsyncImage(
                            model = ambientImageRequest,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            colorFilter = ambientColorFilter,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(52.dp)
                                .graphicsLayer {
                                    scaleX = 1.12f
                                    scaleY = 1.12f
                                    alpha = if (isPlaying) 0.60f else 0.46f
                                }
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val animatedPrimary = primary.value
                            val animatedSecondary = secondary.value
                            val animatedBase = base.value
                            drawRect(
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.00f to animatedBase.copy(alpha = 0.90f),
                                        0.18f to animatedPrimary.copy(alpha = 0.26f),
                                        0.54f to Color.Transparent,
                                        0.78f to animatedSecondary.copy(alpha = 0.22f),
                                        1.00f to Color.Black.copy(alpha = 0.88f)
                                    )
                                )
                            )
                            drawRect(
                                Brush.radialGradient(
                                    colors = listOf(
                                        animatedPrimary.copy(alpha = 0.20f),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width * 0.20f, size.height * 0.16f),
                                    radius = size.maxDimension * 0.82f
                                )
                            )
                        }
                )
            }
        }
    }
}
