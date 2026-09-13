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
    val SectionStride: Dp = 34.dp
    val SectionStrideCompact: Dp = 26.dp
    val HeaderCorner: Dp = 20.dp
    val HeaderPadding: Dp = 14.dp
    val SettingsControlHeight: Dp = 48.dp
    val MoodChipHeight: Dp = 48.dp
    val MoodChipVisualHeight: Dp = 34.dp
    val MoodChipCorner: Dp = 10.dp
    val HeroCorner: Dp = 20.dp
    val HeroHeight: Dp = 472.dp
    val ShelfCorner: Dp = 6.dp
    val ArtworkCorner: Dp = 8.dp
    val ThumbCorner: Dp = 5.dp
    val ArtworkCardWidth: Dp = 160.dp
    val ArtworkGridCardWidth: Dp = 128.dp
    val ShelfItemGap: Dp = 12.dp
    val TrackRowHeight: Dp = 68.dp
    val TrackThumbSize: Dp = 54.dp
    val TrackColumnPeek: Dp = 32.dp
    val TrackColumnGap: Dp = 8.dp
    const val TRACK_COLUMN_ROWS: Int = 4
    const val SPEED_DIAL_COLUMNS: Int = 3
    const val SPEED_DIAL_PAGE_SIZE: Int = 9
    val SpeedDialGap: Dp = 5.dp
    val SectionTitleSize = 22.sp
    val CardTitleSize = 15.sp
    val CardSubtitleSize = 13.sp

    val HeaderShape = RoundedCornerShape(HeaderCorner)
    val SettingsShape = RoundedCornerShape(16.dp)
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
