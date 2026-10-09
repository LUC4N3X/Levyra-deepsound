package com.luc4n3x.levyra.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object LevyraCardDesign {
    val ThumbCorner: Dp = 12.dp
    val ArtworkCorner: Dp = 16.dp
    val EditorialCorner: Dp = 24.dp
    val SurfaceCorner: Dp = 28.dp

    val ThumbShape: Shape = RoundedCornerShape(ThumbCorner)
    val ArtworkShape: Shape = RoundedCornerShape(ArtworkCorner)
    val EditorialShape: Shape = RoundedCornerShape(EditorialCorner)
    val SurfaceShape: Shape = RoundedCornerShape(SurfaceCorner)

    val RowThumb: Dp = 56.dp
    val RowHeight: Dp = 72.dp
    val RowTextGap: Dp = 14.dp
    val RowLineGap: Dp = 3.dp
    val RowHorizontalPadding: Dp = 4.dp

    val CaptionTopGap: Dp = 10.dp
    val CaptionLineGap: Dp = 3.dp

    val CardTitleSize = 14.5.sp
    val CardSubtitleSize = 12.5.sp
    val RowTitleSize = 15.5.sp
    val RowSubtitleSize = 13.sp
    val StatusGlyph: Dp = 13.dp
}
