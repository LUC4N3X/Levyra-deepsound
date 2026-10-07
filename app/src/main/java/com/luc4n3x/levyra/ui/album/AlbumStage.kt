package com.luc4n3x.levyra.ui.album

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.luc4n3x.levyra.ui.media.ImmersiveMediaColors
import com.luc4n3x.levyra.ui.media.immersiveMediaColors
import com.luc4n3x.levyra.ui.media.immersiveMediaGutter
import com.luc4n3x.levyra.ui.media.immersivePortraitHeroHeight

internal typealias AlbumStageColors = ImmersiveMediaColors

internal val AlbumNeutralPaletteStart = Color(0xFF8E95A1)
internal val AlbumNeutralPaletteEnd = Color(0xFF585E69)

internal fun albumStageColors(primary: Color, secondary: Color, lightTheme: Boolean): AlbumStageColors {
    return immersiveMediaColors(primary, secondary, lightTheme)
}

internal fun albumStackedHeroHeight(width: Dp, height: Dp): Dp =
    immersivePortraitHeroHeight(width, height)

internal fun albumContentGutter(width: Dp): Dp = immersiveMediaGutter(width)
