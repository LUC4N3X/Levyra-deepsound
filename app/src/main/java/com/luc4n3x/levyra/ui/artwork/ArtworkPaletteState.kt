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
    fallback: ArtworkPalette,
    retainPreviousUntilLoaded: Boolean = false
): State<ArtworkPalette> {
    val context = LocalContext.current
    val memoryPalette = remember(paletteKey) {
        if (paletteKey.isNotBlank()) ArtworkPaletteCache.peek(paletteKey) else null
    }
    val paletteState = if (retainPreviousUntilLoaded) {
        remember { mutableStateOf(memoryPalette ?: fallback) }
    } else {
        remember(paletteKey) { mutableStateOf(memoryPalette ?: fallback) }
    }
    val latestArtworkUrl by rememberUpdatedState(artworkUrl)
    val latestFallback by rememberUpdatedState(fallback)

    LaunchedEffect(paletteKey, artworkUrl) {
        if (paletteKey.isBlank() || memoryPalette != null) {
            if (retainPreviousUntilLoaded) paletteState.value = memoryPalette ?: latestFallback
            return@LaunchedEffect
        }
        val loaded = loadArtworkPalette(context, paletteKey, { latestArtworkUrl }, { latestFallback }) {
            paletteState.value = it
        }
        if (loaded != null) paletteState.value = loaded
        else if (retainPreviousUntilLoaded) paletteState.value = latestFallback
    }
    return paletteState
}

private suspend fun loadArtworkPalette(
    context: Context,
    key: String,
    artworkUrl: () -> String,
    fallback: () -> ArtworkPalette,
    onExtracted: (ArtworkPalette) -> Unit
): ArtworkPalette? {
    ArtworkPaletteCache.load(context, key)?.let { return it }
    val url = artworkUrl()
    if (url.isBlank()) return null
    val extracted = extractArtworkPalette(context, url, fallback()) ?: return null
    onExtracted(extracted)
    ArtworkPaletteCache.store(context, key, extracted)
    return extracted
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
