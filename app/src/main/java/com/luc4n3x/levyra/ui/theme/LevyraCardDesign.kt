package com.luc4n3x.levyra.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object LevyraCardDesign {
    val ThumbCorner: Dp = 6.dp
    val ArtworkCorner: Dp = 8.dp
    val EditorialCorner: Dp = 12.dp
    val SurfaceCorner: Dp = 18.dp

    val ThumbShape: Shape = RoundedCornerShape(ThumbCorner)
    val ArtworkShape: Shape = RoundedCornerShape(ArtworkCorner)
    val EditorialShape: Shape = RoundedCornerShape(EditorialCorner)
    val SurfaceShape: Shape = RoundedCornerShape(SurfaceCorner)

    val RowThumb: Dp = 52.dp
    val RowHeight: Dp = 64.dp
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
