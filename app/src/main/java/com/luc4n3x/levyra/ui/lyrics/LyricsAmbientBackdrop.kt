package com.luc4n3x.levyra.ui.lyrics

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

const val LYRICS_AMBIENT_MAX_LUMINANCE = 0.12f
const val LYRICS_AMBIENT_DEEP_LUMINANCE = 0.035f
private const val LYRICS_AMBIENT_DECODE_PX = 24
private const val LYRICS_AMBIENT_DRIFT_MS = 26_000
private val LyricsAmbientFloor = Color(0xFF07070A)

fun lyricsAmbientTone(color: Color, maxLuminance: Float): Color {
    val luminance = color.luminance()
    if (luminance <= maxLuminance || luminance <= 0f) return color.copy(alpha = 1f)
    val scale = kotlin.math.sqrt(maxLuminance / luminance).coerceIn(0f, 1f)
    return Color(red = color.red * scale, green = color.green * scale, blue = color.blue * scale)
}

@Composable
fun LyricsAmbientBackdrop(
    artworkUrl: String?,
    trackKey: String,
    accentStart: Color,
    accentEnd: Color,
    motionEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val toneStart = remember(accentStart) { lyricsAmbientTone(accentStart, LYRICS_AMBIENT_MAX_LUMINANCE) }
    val toneEnd = remember(accentEnd) { lyricsAmbientTone(accentEnd, LYRICS_AMBIENT_MAX_LUMINANCE) }
    val deepTone = remember(accentEnd) { lyricsAmbientTone(accentEnd, LYRICS_AMBIENT_DEEP_LUMINANCE) }
    val baseBrush = remember(toneStart, toneEnd, deepTone) {
        Brush.verticalGradient(listOf(toneStart, toneEnd, deepTone, LyricsAmbientFloor))
    }
    val glowBrush = remember(accentEnd) {
        Brush.radialGradient(
            colors = listOf(accentEnd.copy(alpha = 0.26f), Color.Transparent),
            center = Offset(Float.POSITIVE_INFINITY, 0f),
            radius = 1_400f
        )
    }
    val drift = lyricsAmbientDrift(motionEnabled)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBrush)
    ) {
        if (!artworkUrl.isNullOrBlank()) {
            val context = LocalContext.current
            val request = remember(context, trackKey, artworkUrl) {
                ImageRequest.Builder(context)
                    .data(artworkUrl)
                    .size(LYRICS_AMBIENT_DECODE_PX, LYRICS_AMBIENT_DECODE_PX)
                    .crossfade(true)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.High,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val phase = drift.value
                        alpha = 0.42f
                        scaleX = 1.45f + 0.08f * phase
                        scaleY = 1.45f + 0.08f * phase
                        rotationZ = -6f + 12f * phase
                    }
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(glowBrush)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.40f),
                            Color.Black.copy(alpha = 0.32f),
                            Color.Black.copy(alpha = 0.50f),
                            Color.Black.copy(alpha = 0.80f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun lyricsAmbientDrift(enabled: Boolean): State<Float> {
    if (!enabled) return remember { mutableFloatStateOf(0.5f) }
    val transition = rememberInfiniteTransition(label = "lyrics-ambient")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = LYRICS_AMBIENT_DRIFT_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lyrics-ambient-drift"
    )
}
