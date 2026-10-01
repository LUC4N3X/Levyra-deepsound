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
    val SectionGap: Dp = 7.dp
    val SectionGapCompact: Dp = 5.dp
    val SectionStride: Dp = 29.dp
    val SectionStrideCompact: Dp = 23.dp
    val HeaderCorner: Dp = 16.dp
    val HeaderPadding: Dp = 12.dp
    val SettingsControlHeight: Dp = 48.dp
    val MoodChipHeight: Dp = 48.dp
    val MoodChipVisualHeight: Dp = 32.dp
    val MoodChipCorner: Dp = 8.dp
    val HeroCorner: Dp = 12.dp
    val HeroHeight: Dp = 472.dp
    val ShelfCorner: Dp = 2.dp
    val ArtworkCorner: Dp = 6.dp
    val ThumbCorner: Dp = 4.dp
    val ArtworkCardWidth: Dp = 156.dp
    val ArtworkGridCardWidth: Dp = 122.dp
    val ShelfItemGap: Dp = 8.dp
    val TrackRowHeight: Dp = 60.dp
    val TrackThumbSize: Dp = 48.dp
    val TrackColumnPeek: Dp = 28.dp
    val TrackColumnGap: Dp = 6.dp
    const val TRACK_COLUMN_ROWS: Int = 4
    const val SPEED_DIAL_COLUMNS: Int = 3
    const val SPEED_DIAL_PAGE_SIZE: Int = 9
    val SpeedDialGap: Dp = 4.dp
    val SectionTitleSize = 21.sp
    val CardTitleSize = 14.sp
    val CardSubtitleSize = 12.sp

    val HeaderShape = RoundedCornerShape(HeaderCorner)
    val SettingsShape = RoundedCornerShape(14.dp)
    val MoodChipShape = RoundedCornerShape(MoodChipCorner)
    val HeroShape = RoundedCornerShape(HeroCorner)
    val ShelfShape = RoundedCornerShape(ShelfCorner)
    val ArtworkShape = RoundedCornerShape(ArtworkCorner)
    val ThumbShape = RoundedCornerShape(ThumbCorner)

    fun sectionGap(compact: Boolean): Dp = if (compact) SectionGapCompact else SectionGap

    fun sectionLead(compact: Boolean): Dp = if (compact) {
        SectionStrideCompact - SectionGapCompact
    } else {
        SectionStride - SectionGap
    }
}
