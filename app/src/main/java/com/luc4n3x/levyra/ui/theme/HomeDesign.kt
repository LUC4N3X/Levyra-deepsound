package com.luc4n3x.levyra.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Visual scale for the discovery surface.
 *
 * Home stays quieter than the player: artwork supplies the colour, while the header and chrome
 * use a compact, predictable surface and radius family.
 */
object LevyraHomeDesign {
    val CanvasDark: Color = Color(0xFF08090D)
    val CanvasMid: Color = Color(0xFF0C0D12)

    val HeaderSurfaceDark: Color = Color(0xD914151D)
    val HeaderSurfaceLight: Color = Color.White.copy(alpha = 0.88f)
    val HeaderBorderDark: Color = Color.White.copy(alpha = 0.085f)
    val HeaderBorderLight: Color = Color(0x1A11131F)

    val HorizontalInset: Dp = 18.dp
    val SectionGap: Dp = 22.dp
    val SectionGapCompact: Dp = 12.dp
    val HeaderCorner: Dp = 26.dp
    val HeaderPadding: Dp = 16.dp
    val SettingsControlHeight: Dp = 48.dp
    val HeroCorner: Dp = 22.dp
    val HeroHeight: Dp = 216.dp
    val AtmosphereHeight: Dp = 470.dp

    val HeaderShape = RoundedCornerShape(HeaderCorner)
    val SettingsShape = RoundedCornerShape(17.dp)
    val HeroShape = RoundedCornerShape(HeroCorner)
}
