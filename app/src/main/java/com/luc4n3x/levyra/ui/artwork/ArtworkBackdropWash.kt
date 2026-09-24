package com.luc4n3x.levyra.ui.artwork

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.luc4n3x.levyra.data.LevyraArtworkCache
import com.luc4n3x.levyra.ui.playerMix
import com.luc4n3x.levyra.ui.rememberGlassBlurAllowed
import com.luc4n3x.levyra.ui.theme.LevyraActivePalette
import com.luc4n3x.levyra.ui.theme.LevyraIsPureBlack

private const val WashArtworkPx = 96
private const val DefaultWashImageFraction = 0.62f
private const val DefaultWashOverscale = 1.35f
private val DefaultWashBlur = 64.dp

@Composable
internal fun ArtworkBackdropWash(
    artworkUrl: String,
    tint: Color,
    base: Color,
    modifier: Modifier = Modifier,
    washFraction: Float = DefaultWashImageFraction,
    overscale: Float = DefaultWashOverscale,
    blurRadius: Dp = DefaultWashBlur,
    blurAllowed: Boolean = rememberGlassBlurAllowed()
) {
    val isLight = LevyraActivePalette.isLight
    val pureBlack = LevyraIsPureBlack
    val context = LocalContext.current
    val canBlur = blurAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && artworkUrl.isNotBlank()
    val request = remember(artworkUrl) {
        ImageRequest.Builder(context)
            .data(LevyraArtworkCache.small(artworkUrl))
            .size(WashArtworkPx, WashArtworkPx)
            .crossfade(true)
            .build()
    }
    val imageAlpha = when {
        isLight -> 0.26f
        pureBlack -> 0.30f
        else -> 0.46f
    }
    val washTop = when {
        isLight -> tint.playerMix(base, 0.78f)
        pureBlack -> tint.playerMix(base, 0.62f)
        else -> tint.playerMix(base, 0.38f)
    }
    val scrim = remember(washTop, base) {
        Brush.verticalGradient(
            0f to washTop.copy(alpha = 0.55f),
            0.35f to washTop.copy(alpha = 0.78f),
            0.62f to washTop.playerMix(base, 0.45f),
            0.82f to washTop.playerMix(base, 0.86f),
            1f to base
        )
    }
    Box(modifier = modifier.drawBehind { drawRect(base) }) {
        if (canBlur) {
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(washFraction)
                    .graphicsLayer {
                        scaleX = overscale
                        scaleY = overscale
                        alpha = imageAlpha
                    }
                    .blur(blurRadius, BlurredEdgeTreatment.Unbounded)
            )
        }
        Box(modifier = Modifier.fillMaxSize().drawBehind { drawRect(scrim) })
    }
}
