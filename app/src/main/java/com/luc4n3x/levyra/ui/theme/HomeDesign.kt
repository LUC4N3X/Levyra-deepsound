package com.luc4n3x.levyra.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object LevyraHomeDesign {
    val CanvasDark: Color = Color(0xFF050609)
    val CanvasMid: Color = Color(0xFF0A0C12)
    val CanvasLight: Color = Color(0xFFF1F3F8)

    val HeaderSurfaceDark: Color = Color(0xCC12141A)
    val HeaderSurfaceLight: Color = Color.White.copy(alpha = 0.90f)
    val HeaderBorderDark: Color = Color.White.copy(alpha = 0.08f)
    val HeaderBorderLight: Color = Color(0x1811131F)

    val HorizontalInset: Dp = 16.dp
    val SectionGap: Dp = 8.dp
    val SectionGapCompact: Dp = 6.dp
    val SectionStride: Dp = 24.dp
    val SectionStrideCompact: Dp = 20.dp
    val HeaderCorner: Dp get() = if (LevyraIsTorque) 2.dp else if (LevyraIsPrism) 24.dp else if (LevyraIsNocturne) 10.dp else 20.dp
    val HeaderPadding: Dp = 12.dp
    val SettingsControlHeight: Dp = 48.dp
    val MoodChipHeight: Dp = 48.dp
    val MoodChipVisualHeight: Dp = 32.dp
    val MoodChipCorner: Dp get() = if (LevyraIsNocturne) 6.dp else 16.dp
    val HeroCorner: Dp get() = if (LevyraIsTorque) 2.dp else if (LevyraIsPrism) 24.dp else if (LevyraIsNocturne) 14.dp else 28.dp
    val EditorialPadding: Dp = 20.dp
    val EditorialThumb: Dp = 104.dp
    val EditorialPeek: Dp = 40.dp
    val EditorialMaxWidth: Dp = 520.dp
    val EditorialHeight: Dp = 144.dp
    val EditorialLargeTextHeight: Dp = 240.dp
    val DiscoveryArtworkWidth: Dp get() = if (LevyraIsNocturne) 160.dp else 176.dp
    const val EditorialArtworkRatio: Float = 1.5f
    val HeroHeight: Dp = 340.dp
    val ShelfCorner: Dp = 2.dp
    val ArtworkCorner: Dp get() = if (LevyraIsTorque) 2.dp else if (LevyraIsPrism) 16.dp else if (LevyraIsNocturne) 6.dp else 20.dp
    val ThumbCorner: Dp get() = LevyraCardDesign.ThumbCorner
    val ArtworkCardWidth: Dp get() = if (LevyraIsNocturne) 148.dp else 156.dp
    val ArtworkGridCardWidth: Dp = 122.dp
    val ShelfItemGap: Dp = 10.dp
    val TrackRowHeight: Dp = 64.dp
    val TrackThumbSize: Dp = 48.dp
    val TrackColumnPeek: Dp = 28.dp
    val TrackColumnGap: Dp = 6.dp
    val OrbitTileGap: Dp = 4.5.dp
    val OrbitTileCorner: Dp get() = if (LevyraIsNocturne) 6.dp else 8.dp
    val OrbitTileTitleInset: Dp = 9.dp
    val OrbitPageEndInset: Dp = 20.dp
    val OrbitAvatarSize: Dp = 34.dp
    val OrbitAvatarGap: Dp = 14.dp
    val OrbitDotSize: Dp = 8.dp
    val OrbitDotGap: Dp = 4.5.dp
    val OrbitDotsTopGap: Dp = 8.dp
    val OrbitWallMinWidth: Dp = 600.dp
    val OrbitWallPageWidth: Dp = 360.dp
    const val TRACK_COLUMN_ROWS: Int = 4
    const val SPEED_DIAL_COLUMNS: Int = 3
    const val SPEED_DIAL_PAGE_SIZE: Int = 9
    val SpeedDialGap: Dp = 4.dp
    val SectionTitleSize: androidx.compose.ui.unit.TextUnit get() = if (LevyraIsNocturne) 20.sp else 21.sp
    val CardTitleSize = LevyraCardDesign.CardTitleSize
    val CardSubtitleSize = LevyraCardDesign.CardSubtitleSize
    val OrbitTileTitleSize = 15.sp
    val OrbitHeaderNameSize = 15.sp
    val OrbitHeaderTitleSize: androidx.compose.ui.unit.TextUnit get() = if (LevyraIsNocturne) 22.sp else 24.sp

    val HeaderShape: RoundedCornerShape get() = RoundedCornerShape(HeaderCorner)
    val SettingsShape = RoundedCornerShape(14.dp)
    val MoodChipShape: RoundedCornerShape get() = RoundedCornerShape(MoodChipCorner)
    val HeroShape: RoundedCornerShape get() = RoundedCornerShape(HeroCorner)
    val ShelfShape = RoundedCornerShape(ShelfCorner)
    val ArtworkShape: RoundedCornerShape get() = RoundedCornerShape(ArtworkCorner)
    val ThumbShape: RoundedCornerShape get() = RoundedCornerShape(ThumbCorner)

    fun headerContentGap(compact: Boolean): Dp = if (compact) SectionGapCompact else SectionGap

    fun sectionGap(compact: Boolean): Dp = headerContentGap(compact)

    fun sectionLead(compact: Boolean): Dp = if (compact) {
        SectionStrideCompact - SectionGapCompact
    } else {
        SectionStride - SectionGap
    }
}
