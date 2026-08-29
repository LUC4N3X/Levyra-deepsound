package com.luc4n3x.levyra.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    val SectionGap: Dp = 10.dp
    val SectionGapCompact: Dp = 8.dp
    val SectionStride: Dp = 30.dp
    val SectionStrideCompact: Dp = 22.dp
    val SectionStrideFeature: Dp = 40.dp
    val SectionStrideFeatureCompact: Dp = 28.dp
    val SectionStrideQuiet: Dp = 24.dp
    val SectionStrideQuietCompact: Dp = 18.dp
    val HeaderCorner: Dp = 20.dp
    val HeaderPadding: Dp = 14.dp
    val SettingsControlHeight: Dp = 48.dp
    val MoodChipHeight: Dp = 48.dp
    val MoodChipCorner: Dp = 14.dp
    val HeroCorner: Dp = 26.dp
    val HeroHeight: Dp = 244.dp
    val ShelfCorner: Dp = 16.dp
    val ArtworkCorner: Dp = 18.dp
    val SearchFieldHeight: Dp = 50.dp
    val SearchFieldCorner: Dp = 16.dp
    val SectionMarkerWidth: Dp = 3.dp
    val SectionMarkerHeight: Dp = 20.dp
    val SectionMarkerTopInset: Dp = 5.dp
    val RankColumnWidth: Dp = 30.dp
    val ArtworkCardWidth: Dp = 156.dp
    val ArtworkCardWidthLarge: Dp = 182.dp
    val CollectionCardWidth: Dp = 268.dp
    val CollectionCardHeight: Dp = 188.dp
    val ArtworkGridCardWidth: Dp = 126.dp
    val ShelfItemGap: Dp = 14.dp

    val FeatureTitleSize: TextUnit = 23.sp
    val StandardTitleSize: TextUnit = 19.sp

    val HeaderShape = RoundedCornerShape(HeaderCorner)
    val SettingsShape = RoundedCornerShape(16.dp)
    val MoodChipShape = RoundedCornerShape(MoodChipCorner)
    val SearchFieldShape = RoundedCornerShape(SearchFieldCorner)
    val HeroShape = RoundedCornerShape(HeroCorner)
    val ShelfShape = RoundedCornerShape(ShelfCorner)
    val ArtworkShape = RoundedCornerShape(ArtworkCorner)

    fun sectionGap(compact: Boolean): Dp = if (compact) SectionGapCompact else SectionGap

    fun sectionLead(compact: Boolean): Dp = if (compact) {
        SectionStrideCompact - SectionGapCompact
    } else {
        SectionStride - SectionGap
    }

    fun featureSectionLead(compact: Boolean): Dp = if (compact) {
        SectionStrideFeatureCompact - SectionGapCompact
    } else {
        SectionStrideFeature - SectionGap
    }

    fun quietSectionLead(compact: Boolean): Dp = if (compact) {
        SectionStrideQuietCompact - SectionGapCompact
    } else {
        SectionStrideQuiet - SectionGap
    }
}
