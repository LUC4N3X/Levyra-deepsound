package com.luc4n3x.levyra.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.luc4n3x.levyra.data.ArtworkPalette
import com.luc4n3x.levyra.data.ArtworkPaletteCache
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.artwork.rememberArtworkPalette
import kotlinx.coroutines.delay

@Immutable
internal data class NavArtworkSeed(val primary: Color, val secondary: Color)

internal val LocalNavArtworkSeed = compositionLocalOf<NavArtworkSeed?> { null }

internal val NavArtworkDemoSeed = navArtworkSeedOf(Color(0xFFE0566F), Color(0xFF6A5AE0))

private const val NavArtworkSettleMillis = 900L

internal fun navArtworkSeedOf(primary: Color, secondary: Color): NavArtworkSeed {
    val pair = harmonizePlayerAccents(primary, secondary)
    return NavArtworkSeed(primary = pair.primary.navArtworkTamed(), secondary = pair.secondary.navArtworkTamed())
}

private fun Color.navArtworkTamed(): Color {
    val (hue, saturation, value) = hsvComponents()
    if (saturation < 0.08f) return Color.hsv(hue, saturation, value.coerceIn(0.55f, 0.85f))
    return Color.hsv(hue, saturation.coerceIn(0.32f, 0.80f), value.coerceIn(0.52f, 0.92f))
}

internal fun Color.hsvComponents(): Triple<Float, Float, Float> {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val delta = max - min
    val hue = when {
        delta <= 0f -> 0f
        max == red -> 60f * ((green - blue) / delta).mod(6f)
        max == green -> 60f * ((blue - red) / delta + 2f)
        else -> 60f * ((red - green) / delta + 4f)
    }
    val saturation = if (max <= 0f) 0f else delta / max
    return Triple(hue.mod(360f), saturation, max)
}

@Composable
internal fun rememberNavArtworkSeed(track: Track?, enabled: Boolean): NavArtworkSeed? {
    if (!enabled || track == null) return null
    val key = remember(track.id, track.thumbnailUrl, track.largeThumbnailUrl) {
        ArtworkPaletteCache.key(
            trackId = track.id,
            thumbnailUrl = track.thumbnailUrl,
            largeThumbnailUrl = track.largeThumbnailUrl
        )
    }
    val url = remember(track.id, track.thumbnailUrl, track.largeThumbnailUrl) {
        preferredPlayerArtworkUrl(track).ifBlank { track.largeThumbnailUrl.ifBlank { track.thumbnailUrl } }
    }
    val fallback = remember(track.accentStart, track.accentEnd) { ArtworkPalette(track.accentStart, track.accentEnd) }
    val palette by rememberArtworkPalette(paletteKey = key, artworkUrl = url, fallback = fallback)
    var settled by remember { mutableStateOf<ArtworkPalette?>(null) }
    LaunchedEffect(key, palette) {
        if (palette != fallback || url.isBlank() || settled == null) {
            settled = palette
            return@LaunchedEffect
        }
        delay(NavArtworkSettleMillis)
        settled = palette
    }
    val resolved = settled ?: palette
    return remember(resolved) { navArtworkSeedOf(Color(resolved.start), Color(resolved.end)) }
}
