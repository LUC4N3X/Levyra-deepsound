package com.luc4n3x.levyra.data

import coil3.Extras
import coil3.getExtra
import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.size.Dimension
import coil3.size.Size
import com.luc4n3x.levyra.domain.LevyraPersonalOrbit
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

private const val FULL_RESOLUTION_ARTWORK_SIZE = 1200
private const val SPOTIFY_MEDIUM_ARTWORK_SIZE = 300
private val AppleArtworkSizePattern = Regex("/(\\d+)x(\\d+)bb(?=[.-])")
private val GoogleWidthHeightPattern = Regex("=w(\\d+)-h(\\d+)")
private val GoogleSquarePattern = Regex("=s(\\d+)")

internal enum class ArtworkTier(val pixels: Int) {
    Thumbnail(256),
    Card(512),
    Detail(768),
    Full(FULL_RESOLUTION_ARTWORK_SIZE)
}

internal object ArtworkQualityPolicy {
    fun tierFor(targetPixels: Int?): ArtworkTier {
        if (targetPixels == null || targetPixels <= 0) return ArtworkTier.Full
        return ArtworkTier.entries.firstOrNull { targetPixels <= it.pixels } ?: ArtworkTier.Full
    }

    fun tierFor(size: Size): ArtworkTier {
        val width = (size.width as? Dimension.Pixels)?.px
        val height = (size.height as? Dimension.Pixels)?.px
        return tierFor(listOfNotNull(width, height).maxOrNull())
    }
}

internal object ArtworkUrlResolver {
    fun resolve(url: String, tier: ArtworkTier): String {
        val clean = url.trim()
        if (!clean.startsWith("https://", ignoreCase = true)) return clean
        if (clean.indexOf('?') >= 0) return clean
        val parsed = clean.toHttpUrlOrNull() ?: return clean
        val host = parsed.host.lowercase()
        return when {
            host == "i.scdn.co" && parsed.encodedPath.contains("/image/ab67616d00001e02", ignoreCase = true) ->
                if (tier.pixels > SPOTIFY_MEDIUM_ARTWORK_SIZE) {
                    clean.replace("ab67616d00001e02", "ab67616d0000b273", ignoreCase = true)
                } else {
                    clean
                }
            isHostOrSubdomain(host, "mzstatic.com") -> appleArtworkUrl(clean, tier)
            isHostOrSubdomain(host, "googleusercontent.com") || isHostOrSubdomain(host, "ggpht.com") ->
                googleArtworkUrl(clean, tier)
            host == "e-cdns-images.dzcdn.net" -> deezerArtworkUrl(clean, tier)
            else -> clean
        }
    }

    private fun appleArtworkUrl(url: String, tier: ArtworkTier): String {
        val size = tier.pixels.toString()
        val templated = url
            .replace("{w}", size, ignoreCase = true)
            .replace("{h}", size, ignoreCase = true)
        if (templated != url) return templated

        val match = AppleArtworkSizePattern.find(url) ?: return url
        val width = match.groupValues[1].toIntOrNull() ?: return url
        val height = match.groupValues[2].toIntOrNull() ?: return url
        if (tier == ArtworkTier.Full && (width >= tier.pixels || height >= tier.pixels)) return url
        if (width == tier.pixels && height == tier.pixels) return url
        return url.replaceRange(match.range, "/${tier.pixels}x${tier.pixels}bb")
    }

    private fun googleArtworkUrl(url: String, tier: ArtworkTier): String {
        if (tier == ArtworkTier.Full && googleArtworkAlreadyLargeEnough(url, tier.pixels)) return url
        return LevyraPersonalOrbit.upscaledArtworkUrl(url, tier.pixels)
    }

    private fun googleArtworkAlreadyLargeEnough(url: String, pixels: Int): Boolean {
        GoogleWidthHeightPattern.find(url)?.let { match ->
            val width = match.groupValues[1].toIntOrNull() ?: return@let
            val height = match.groupValues[2].toIntOrNull() ?: return@let
            return width >= pixels || height >= pixels
        }
        GoogleSquarePattern.find(url)?.let { match ->
            val size = match.groupValues[1].toIntOrNull() ?: return@let
            return size >= pixels
        }
        return false
    }

    private fun deezerArtworkUrl(url: String, tier: ArtworkTier): String = when (tier) {
        ArtworkTier.Thumbnail -> url
        ArtworkTier.Card -> url.replace("/cover_medium/", "/cover_big/", ignoreCase = true)
        ArtworkTier.Detail, ArtworkTier.Full -> url
            .replace("/cover_medium/", "/cover_xl/", ignoreCase = true)
            .replace("/cover_big/", "/cover_xl/", ignoreCase = true)
    }

    private fun isHostOrSubdomain(host: String, domain: String): Boolean =
        host == domain || host.endsWith(".$domain")
}

internal fun fullResolutionArtworkUrl(url: String): String = ArtworkUrlResolver.resolve(url, ArtworkTier.Full)

internal val ArtworkTierOverride = Extras.Key<ArtworkTier?>(default = null)

internal class ArtworkResolutionInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val source = chain.request.data as? String ?: return chain.proceed()
        val tier = chain.request.getExtra(ArtworkTierOverride) ?: ArtworkQualityPolicy.tierFor(chain.size)
        val resolved = ArtworkUrlResolver.resolve(source, tier)
        if (resolved == source) return chain.proceed()
        val request = chain.request.newBuilder()
            .data(resolved)
            .build()
        return chain.withRequest(request).proceed()
    }
}
