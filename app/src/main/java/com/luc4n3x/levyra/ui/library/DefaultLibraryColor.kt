package com.luc4n3x.levyra.ui.library

import com.luc4n3x.levyra.data.ArtworkToneMapper
import com.materialkolor.blend.Blend
import com.materialkolor.contrast.Contrast
import com.materialkolor.hct.Hct

internal fun libraryArtworkContainer(
    artwork: Int,
    role: Int,
    surface: Int,
    content: Int
): Int {
    val base = Hct.fromInt(surface)
    val artworkSeed = Hct.fromInt(artwork)
    if (artworkSeed.chroma <= ArtworkToneMapper.NeutralChromaCeiling) return surface

    val harmonized = Hct.fromInt(Blend.harmonize(artwork, role))
    val maxChroma = if (base.tone < 50.0) 9.0 else 12.0
    val tint = Hct.from(harmonized.hue, harmonized.chroma.coerceAtMost(maxChroma), base.tone)
    return if (Contrast.ratioOfTones(tint.tone, Hct.fromInt(content).tone) >= 4.5) tint.toInt() else surface
}
