package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.LevyraPersonalOrbit
import com.luc4n3x.levyra.domain.Track

internal fun preferredPlayerArtworkUrl(track: Track): String {
    val candidates = sequenceOf(track.largeThumbnailUrl, track.thumbnailUrl)
        .map(String::trim)
        .filter(String::isNotBlank)
        .toList()
    val albumArtwork = candidates.firstOrNull { !LevyraPersonalOrbit.isVideoFrameArtworkUrl(it) }
    return highResolutionPlayerArtworkUrl(albumArtwork ?: candidates.firstOrNull().orEmpty())
}

internal fun highResolutionPlayerArtworkUrl(url: String): String {
    val clean = url.trim()
    if (!clean.startsWith("https://", ignoreCase = true)) return clean
    val lower = clean.lowercase()
    return when {
        lower.contains("i.scdn.co/image/ab67616d00001e02") ->
            clean.replace("ab67616d00001e02", "ab67616d0000b273", ignoreCase = true)
        lower.contains("mzstatic.com/") ->
            clean.replace("{w}", "1200", ignoreCase = true)
                .replace("{h}", "1200", ignoreCase = true)
                .replace(Regex("/\\d+x\\d+bb\\."), "/1200x1200bb.")
        lower.contains("googleusercontent.com/") || lower.contains("ggpht.com/") ->
            clean.replace(Regex("=w\\d+-h\\d+"), "=w1200-h1200")
                .replace(Regex("=s\\d+"), "=s1200")
        lower.contains("e-cdns-images.dzcdn.net/") ->
            clean.replace("/cover_medium/", "/cover_xl/", ignoreCase = true)
                .replace("/cover_big/", "/cover_xl/", ignoreCase = true)
        else -> clean
    }
}
