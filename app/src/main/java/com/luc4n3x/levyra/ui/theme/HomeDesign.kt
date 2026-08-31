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
    val SectionGap: Dp = 8.dp
    val SectionGapCompact: Dp = 6.dp
    val SectionStride: Dp = 26.dp
    val SectionStrideCompact: Dp = 20.dp
    val HeaderCorner: Dp = 20.dp
    val HeaderPadding: Dp = 14.dp
    val SettingsControlHeight: Dp = 48.dp
    val MoodChipHeight: Dp = 48.dp
    val MoodChipCorner: Dp = 14.dp
    val HeroCorner: Dp = 20.dp
    val HeroHeight: Dp = 240.dp
    val ShelfCorner: Dp = 12.dp
    val ArtworkCorner: Dp = 14.dp
    val ArtworkCardWidth: Dp = 156.dp
    val ArtworkGridCardWidth: Dp = 126.dp
    val ShelfItemGap: Dp = 12.dp

    val HeaderShape = RoundedCornerShape(HeaderCorner)
    val SettingsShape = RoundedCornerShape(16.dp)
    val MoodChipShape = RoundedCornerShape(MoodChipCorner)
    val HeroShape = RoundedCornerShape(HeroCorner)
    val ShelfShape = RoundedCornerShape(ShelfCorner)
    val ArtworkShape = RoundedCornerShape(ArtworkCorner)

    fun sectionGap(compact: Boolean): Dp = if (compact) SectionGapCompact else SectionGap

    fun sectionLead(compact: Boolean): Dp = if (compact) {
        SectionStrideCompact - SectionGapCompact
    } else {
        SectionStride - SectionGap
    }
}
