package com.luc4n3x.levyra.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.hct.Hct

internal data class HomeSoundtrackHeroTones(
    val action: Color,
    val onAction: Color,
    val chip: Color,
    val onChip: Color,
    val badge: Color
)

internal fun homeSoundtrackHeroSeed(portrait: Color, artwork: Color): Color {
    val portraitChroma = Hct.fromInt(portrait.toArgb()).chroma
    return if (portraitChroma >= HeroMinimumPortraitChroma) portrait else artwork
}

internal fun homeSoundtrackHeroTones(seed: Color, isLight: Boolean): HomeSoundtrackHeroTones {
    val source = Hct.fromInt(seed.toArgb())
    val hue = source.hue
    val chroma = source.chroma.coerceIn(HeroActionMinimumChroma, HeroActionMaximumChroma)
    fun tone(tone: Double, toneChroma: Double = chroma) = Color(Hct.from(hue, toneChroma, tone).toInt())
    return if (isLight) {
        HomeSoundtrackHeroTones(
            action = tone(40.0),
            onAction = tone(100.0),
            chip = tone(94.0, HeroChipChroma).copy(alpha = 0.86f),
            onChip = tone(18.0, HeroChipChroma),
            badge = tone(48.0)
        )
    } else {
        HomeSoundtrackHeroTones(
            action = tone(82.0),
            onAction = tone(18.0),
            chip = tone(16.0, HeroChipChroma).copy(alpha = 0.78f),
            onChip = tone(92.0, HeroChipChroma),
            badge = tone(74.0)
        )
    }
}

private const val HeroMinimumPortraitChroma = 16.0
private const val HeroActionMinimumChroma = 36.0
private const val HeroActionMaximumChroma = 64.0
private const val HeroChipChroma = 20.0
