package com.luc4n3x.levyra.ui.theme

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

enum class LevyraSegment {
    Leading,
    Middle,
    Trailing,
    Single
}

object LevyraPlayerShapes {

    private val Round = CornerSize(percent = 50)

    fun artworkCorner(artworkSize: Dp): Dp =
        (artworkSize * LevyraPlayerDesign.ArtworkCornerRatio)
            .coerceIn(LevyraPlayerDesign.ArtworkCornerMin, LevyraPlayerDesign.ArtworkCornerMax)

    fun segment(position: LevyraSegment, innerCorner: Dp): Shape {
        val inner = CornerSize(innerCorner)
        return when (position) {
            LevyraSegment.Leading -> RoundedCornerShape(
                topStart = Round,
                topEnd = inner,
                bottomEnd = inner,
                bottomStart = Round
            )
            LevyraSegment.Middle -> RoundedCornerShape(inner)
            LevyraSegment.Trailing -> RoundedCornerShape(
                topStart = inner,
                topEnd = Round,
                bottomEnd = Round,
                bottomStart = inner
            )
            LevyraSegment.Single -> RoundedCornerShape(Round)
        }
    }
}
