package com.luc4n3x.levyra.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared visual scale for Levyra's discovery surface.
 *
 * Home deliberately uses one radius family, generous spacing and restrained chrome. Artwork is the
 * source of colour; controls and shelves stay neutral so the page reads like one music product.
 */
object LevyraHomeDesign {
    val CanvasDark: Color = Color(0xFF050609)
    val CanvasMid: Color = Color(0xFF0A0C12)

    val HeaderSurfaceDark: Color = Color(0xCC12141A)
    val HeaderSurfaceLight: Color = Color.White.copy(alpha = 0.90f)
    val HeaderBorderDark: Color = Color.White.copy(alpha = 0.08f)
    val HeaderBorderLight: Color = Color(0x1811131F)

    val HorizontalInset: Dp = 18.dp
    val SectionGap: Dp = 28.dp
    val SectionGapCompact: Dp = 18.dp
    val HeaderCorner: Dp = 20.dp
    val HeaderPadding: Dp = 14.dp
    val SettingsControlHeight: Dp = 48.dp
    val MoodChipHeight: Dp = 38.dp
    val HeroCorner: Dp = 28.dp
    val HeroHeight: Dp = 252.dp
    val ShelfCorner: Dp = 18.dp
    val AtmosphereHeight: Dp = 780.dp

    val HeaderShape = RoundedCornerShape(HeaderCorner)
    val SettingsShape = RoundedCornerShape(16.dp)
    val HeroShape = RoundedCornerShape(HeroCorner)
    val ShelfShape = RoundedCornerShape(ShelfCorner)
}
