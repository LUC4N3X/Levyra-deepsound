package com.luc4n3x.levyra.ui.artwork

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.bitmapConfig
import coil3.toBitmap
import com.luc4n3x.levyra.data.ArtworkPalette
import com.luc4n3x.levyra.data.ArtworkPaletteCache
import com.luc4n3x.levyra.data.LevyraArtworkCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

@Composable
internal fun rememberArtworkPalette(
    paletteKey: String,
    artworkUrl: String,
    fallback: ArtworkPalette
): State<ArtworkPalette> {
    val context = LocalContext.current
    val memoryPalette = remember(paletteKey) {
        if (paletteKey.isNotBlank()) ArtworkPaletteCache.peek(paletteKey) else null
    }
    val paletteState = remember(paletteKey) {
        mutableStateOf(memoryPalette ?: fallback)
    }
    val latestArtworkUrl by rememberUpdatedState(artworkUrl)
    val latestFallback by rememberUpdatedState(fallback)

    LaunchedEffect(paletteKey) {
        if (paletteKey.isBlank() || memoryPalette != null) return@LaunchedEffect
        val persisted = ArtworkPaletteCache.load(context, paletteKey)
        if (persisted != null) {
            paletteState.value = persisted
            return@LaunchedEffect
        }
        val url = latestArtworkUrl
        if (url.isBlank()) return@LaunchedEffect
        val extracted = extractArtworkPalette(context, url, latestFallback) ?: return@LaunchedEffect
        paletteState.value = extracted
        ArtworkPaletteCache.store(context, paletteKey, extracted)
    }
    return paletteState
}

private suspend fun extractArtworkPalette(
    context: Context,
    artworkUrl: String,
    fallback: ArtworkPalette
): ArtworkPalette? = withContext(Dispatchers.IO) {
    val request = ImageRequest.Builder(context)
        .data(LevyraArtworkCache.small(artworkUrl))
        .size(96, 96)
        .allowHardware(false)
        .bitmapConfig(Bitmap.Config.ARGB_8888)
        .diskCachePolicy(CachePolicy.ENABLED)
        .memoryCachePolicy(CachePolicy.DISABLED)
        .build()
    val bitmap = try {
        SingletonImageLoader.get(context).execute(request).image?.toBitmap()
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Timber.d(error, "Artwork palette decode failed")
        null
    } ?: return@withContext null
    withContext(Dispatchers.Default) {
        ArtworkPaletteCache.extract(
            bitmap = bitmap,
            fallbackStart = fallback.start,
            fallbackEnd = fallback.end
        )
    }
}
