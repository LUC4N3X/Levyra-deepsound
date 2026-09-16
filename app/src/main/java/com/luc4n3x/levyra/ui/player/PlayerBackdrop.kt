package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign

private const val MistDecodePx = 96
private const val MistScale = 1.32f
private const val MistAlphaPlaying = 0.64f
private const val MistAlphaPaused = 0.48f
private const val BackdropColorMillis = 900
private const val MistFadeInMillis = 480
private const val MistFadeOutMillis = 320
private val DarkBackdropTop = Color(0xFF15161C)
private val DarkBackdropMiddle = Color(0xFF0C0D11)
private val DarkBackdropBottom = Color(0xFF050608)

@Composable
internal fun PlayerBackdrop(
    mode: PlayerBackgroundMode,
    artworkUrl: String,
    ambience: PlayerAmbience,
    isPlaying: Boolean,
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier,
    focusY: Float = 0.34f
) {
    val colorSpec = if (animationsEnabled) {
        tween<Color>(BackdropColorMillis, easing = LevyraPlayerDesign.Decelerate)
    } else {
        snap()
    }
    val primary = animateColorAsState(ambience.primary, colorSpec, label = "player-backdrop-primary")
    val secondary = animateColorAsState(ambience.secondary, colorSpec, label = "player-backdrop-secondary")
    val glowStrength = animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.7f,
        animationSpec = if (animationsEnabled) LevyraPlayerDesign.emphasizedTween(600) else snap(),
        label = "player-backdrop-glow"
    )

    when (mode) {
        PlayerBackgroundMode.PureBlack -> {
            Box(
                modifier = modifier
                    .background(Color.Black)
                    .drawBehind {
                        drawAmbientGlow(primary.value, 0.16f * glowStrength.value, focusY, 0.62f)
                    }
            )
        }
        PlayerBackgroundMode.Dark -> {
            Box(
                modifier = modifier
                    .background(
                        Brush.verticalGradient(listOf(DarkBackdropTop, DarkBackdropMiddle, DarkBackdropBottom))
                    )
                    .drawBehind {
                        drawAmbientGlow(primary.value, 0.14f * glowStrength.value, focusY, 0.72f)
                    }
            )
        }
        PlayerBackgroundMode.Blur -> {
            Box(modifier = modifier.background(Color.Black)) {
                PlayerArtworkMist(
                    artworkUrl = artworkUrl,
                    colorFilter = null,
                    isPlaying = isPlaying,
                    animationsEnabled = animationsEnabled
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind { drawLegibilityScrim(Color.Black, 0.62f) }
                )
            }
        }
        PlayerBackgroundMode.Dynamic -> {
            val base = animateColorAsState(ambience.base, colorSpec, label = "player-backdrop-base")
            val ambientColorFilter = remember { ColorFilter.colorMatrix(createPlayerAmbientColorMatrix()) }
            Box(modifier = modifier.drawBehind { drawRect(base.value) }) {
                PlayerArtworkMist(
                    artworkUrl = artworkUrl,
                    colorFilter = ambientColorFilter,
                    isPlaying = isPlaying,
                    animationsEnabled = animationsEnabled
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind { drawAura(primary, secondary, base, glowStrength.value, focusY) }
                )
            }
        }
    }
}

@Composable
private fun PlayerArtworkMist(
    artworkUrl: String,
    colorFilter: ColorFilter?,
    isPlaying: Boolean,
    animationsEnabled: Boolean
) {
    if (artworkUrl.isBlank()) return
    val context = LocalContext.current
    val mistAlpha by animateFloatAsState(
        targetValue = if (isPlaying) MistAlphaPlaying else MistAlphaPaused,
        animationSpec = if (animationsEnabled) LevyraPlayerDesign.emphasizedTween(600) else snap(),
        label = "player-backdrop-mist-alpha"
    )
    AnimatedContent(
        targetState = artworkUrl,
        transitionSpec = {
            if (animationsEnabled) {
                fadeIn(tween(MistFadeInMillis, easing = LevyraPlayerDesign.Decelerate)) togetherWith
                    fadeOut(tween(MistFadeOutMillis, easing = LevyraPlayerDesign.Standard))
            } else {
                EnterTransition.None togetherWith ExitTransition.None
            }
        },
        label = "player-backdrop-mist"
    ) { url ->
        val request = remember(context, url) {
            ImageRequest.Builder(context)
                .data(LevyraArtworkCache.large(url))
                .size(MistDecodePx, MistDecodePx)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .crossfade(false)
                .build()
        }
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = colorFilter,
            filterQuality = FilterQuality.High,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = MistScale
                    scaleY = MistScale
                    alpha = mistAlpha
                }
                .blur(48.dp)
        )
    }
}

private fun DrawScope.drawAmbientGlow(color: Color, alpha: Float, focusY: Float, reach: Float) {
    if (alpha <= 0f) return
    drawRect(
        Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), color.copy(alpha = alpha * 0.35f), Color.Transparent),
            center = Offset(size.width * 0.5f, size.height * focusY),
            radius = size.maxDimension * reach
        )
    )
}

private fun DrawScope.drawLegibilityScrim(ink: Color, strength: Float) {
    drawRect(
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to ink.copy(alpha = strength * 0.78f),
                0.16f to ink.copy(alpha = strength * 0.34f),
                0.48f to ink.copy(alpha = strength * 0.30f),
                0.70f to ink.copy(alpha = strength * 0.72f),
                1.00f to ink.copy(alpha = (strength * 1.3f).coerceAtMost(1f))
            )
        )
    )
}

private fun DrawScope.drawAura(
    primary: State<Color>,
    secondary: State<Color>,
    base: State<Color>,
    strength: Float,
    focusY: Float
) {
    val lead = primary.value
    val trail = secondary.value
    drawAmbientGlow(lead, 0.30f * strength, focusY, 0.68f)
    drawRect(
        Brush.radialGradient(
            colors = listOf(trail.copy(alpha = 0.22f * strength), Color.Transparent),
            center = Offset(size.width * 0.08f, size.height * 0.86f),
            radius = size.maxDimension * 0.58f
        )
    )
    drawRect(
        Brush.radialGradient(
            colors = listOf(lead.copy(alpha = 0.12f * strength), Color.Transparent),
            center = Offset(size.width * 0.96f, size.height * 0.58f),
            radius = size.maxDimension * 0.46f
        )
    )
    drawRect(
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to base.value.copy(alpha = 0.70f),
                0.14f to Color.Transparent,
                0.50f to Color.Transparent,
                0.72f to Color.Black.copy(alpha = 0.40f),
                1.00f to Color.Black.copy(alpha = 0.86f)
            )
        )
    )
}
