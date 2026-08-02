package com.luc4n3x.levyra.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Visual scale for the discovery surface.
 *
 * The Home screen deliberately stays quieter than the player: artwork supplies the colour,
 * while the chrome uses a small, predictable surface and radius family.
 */
object LevyraHomeDesign {
    val CanvasDark: Color = Color(0xFF08090D)
    val CanvasMid: Color = Color(0xFF0C0D12)
    val TileSurfaceDark: Color = Color(0xFF18191F)
    val TileSurfacePressedDark: Color = Color(0xFF202127)
    val TileBorderDark: Color = Color.White.copy(alpha = 0.075f)
    val TileBorderActiveDark: Color = Color.White.copy(alpha = 0.20f)

    val TileSurfaceLight: Color = Color.White.copy(alpha = 0.90f)
    val TileBorderLight: Color = Color(0x1A11131F)

    val TextPrimaryDark: Color = Color.White
    val TextSecondaryDark: Color = Color.White.copy(alpha = 0.64f)

    val HorizontalInset: Dp = 18.dp
    val SectionGap: Dp = 22.dp
    val SectionGapCompact: Dp = 12.dp
    val TileGap: Dp = 10.dp
    val TileHeight: Dp = 70.dp
    val ArtworkSize: Dp = 56.dp
    val ArtworkCorner: Dp = 11.dp
    val TileCorner: Dp = 14.dp
    val HeroCorner: Dp = 22.dp
    val HeroHeight: Dp = 216.dp
    val AtmosphereHeight: Dp = 470.dp

    val TileShape = RoundedCornerShape(TileCorner)
    val ArtworkShape = RoundedCornerShape(ArtworkCorner)
    val HeroShape = RoundedCornerShape(HeroCorner)
}
