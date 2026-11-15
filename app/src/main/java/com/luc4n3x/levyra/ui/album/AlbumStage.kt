package com.luc4n3x.levyra.ui.album

import com.luc4n3x.levyra.ui.theme.LevyraIsNocturne

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.luc4n3x.levyra.ui.PlayerMinimumContrast
import com.luc4n3x.levyra.ui.playerAdjustForegroundToward
import com.luc4n3x.levyra.ui.playerMix
import androidx.compose.ui.unit.Dp
import com.luc4n3x.levyra.ui.media.ImmersiveMediaColors
import com.luc4n3x.levyra.ui.media.immersiveMediaColors
import com.luc4n3x.levyra.ui.media.immersiveMediaGutter
import com.luc4n3x.levyra.ui.media.immersivePortraitHeroHeight

internal typealias AlbumStageColors = ImmersiveMediaColors

internal val AlbumNeutralPaletteStart = Color(0xFF8E95A1)
internal val AlbumNeutralPaletteEnd = Color(0xFF585E69)

internal fun albumStageColors(primary: Color, secondary: Color, lightTheme: Boolean): AlbumStageColors {
    return immersiveMediaColors(primary, secondary, lightTheme, LevyraIsNocturne)
}

internal fun albumPageColors(primary: Color, secondary: Color, lightTheme: Boolean, defaultSkin: Boolean, pureBlack: Boolean): AlbumStageColors =
    if (defaultSkin && !pureBlack && !lightTheme) albumCoverStageColors(primary, secondary) else albumStageColors(primary, secondary, lightTheme)

internal fun albumCoverStageColors(primary: Color, secondary: Color): AlbumStageColors {
    val field = coverFieldTone(primary, AlbumCoverFieldMaxLuminance)
    val fields = listOf(field)
    val base = immersiveMediaColors(primary, secondary, lightTheme = false)
    val action = Color.White.playerMix(primary.copy(alpha = 1f), 0.10f)
    return base.copy(
        fieldTop = field,
        fieldMid = field,
        base = field,
        content = Color.White,
        contentMuted = Color.White.playerMix(field, 0.32f).playerAdjustForegroundToward(Color.White, fields, PlayerMinimumContrast).color,
        accent = base.accent.playerAdjustForegroundToward(Color.White, fields, PlayerMinimumContrast).color,
        actionStart = action,
        actionEnd = action,
        actionContent = field,
        secondaryFill = Color.White.copy(alpha = 0.12f),
        hairline = Color.White.copy(alpha = 0.12f)
    )
}

internal fun coverFieldTone(cover: Color, maxLuminance: Float): Color {
    var tone = cover.copy(alpha = 1f)
    var steps = 0
    while (tone.luminance() > maxLuminance && steps < 24) {
        tone = lerp(tone, Color.Black, 0.1f)
        steps++
    }
    return tone
}

private const val AlbumCoverFieldMaxLuminance = 0.09f

internal fun albumStackedHeroHeight(width: Dp, height: Dp): Dp =
    immersivePortraitHeroHeight(width, height)

internal fun albumContentGutter(width: Dp): Dp = immersiveMediaGutter(width)
