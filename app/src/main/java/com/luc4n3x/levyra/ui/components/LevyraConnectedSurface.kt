package com.luc4n3x.levyra.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.theme.LevyraGlassBorder
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign

enum class LevyraConnectedPosition {
    Single,
    Top,
    Middle,
    Bottom;

    companion object {
        fun of(index: Int, count: Int): LevyraConnectedPosition = when {
            count <= 1 -> Single
            index <= 0 -> Top
            index >= count - 1 -> Bottom
            else -> Middle
        }
    }
}

@Immutable
class LevyraConnectedShapes(outerCorner: Dp, innerCorner: Dp) {
    val single: Shape = RoundedCornerShape(outerCorner)
    val top: Shape = RoundedCornerShape(
        topStart = outerCorner,
        topEnd = outerCorner,
        bottomStart = innerCorner,
        bottomEnd = innerCorner
    )
    val middle: Shape = RoundedCornerShape(innerCorner)
    val bottom: Shape = RoundedCornerShape(
        topStart = innerCorner,
        topEnd = innerCorner,
        bottomStart = outerCorner,
        bottomEnd = outerCorner
    )

    val leading: Shape = RoundedCornerShape(
        topStart = outerCorner,
        bottomStart = outerCorner,
        topEnd = innerCorner,
        bottomEnd = innerCorner
    )
    val trailing: Shape = RoundedCornerShape(
        topStart = innerCorner,
        bottomStart = innerCorner,
        topEnd = outerCorner,
        bottomEnd = outerCorner
    )

    fun forRowPosition(position: LevyraConnectedPosition): Shape = when (position) {
        LevyraConnectedPosition.Single -> single
        LevyraConnectedPosition.Top -> leading
        LevyraConnectedPosition.Middle -> middle
        LevyraConnectedPosition.Bottom -> trailing
    }

    fun forPosition(position: LevyraConnectedPosition): Shape = when (position) {
        LevyraConnectedPosition.Single -> single
        LevyraConnectedPosition.Top -> top
        LevyraConnectedPosition.Middle -> middle
        LevyraConnectedPosition.Bottom -> bottom
    }
}

@Immutable
class LevyraConnectedStyle(
    val gap: Dp,
    val shapes: LevyraConnectedShapes,
    val fill: Color,
    val selectedFill: Color,
    val disabledFill: Color,
    val borderColor: Color,
    val selectedBorderColor: Color,
    val borderWidth: Dp
) {
    fun fillFor(selected: Boolean, enabled: Boolean): Color = when {
        !enabled -> disabledFill
        selected -> selectedFill
        else -> fill
    }

    fun borderFor(selected: Boolean): Color = if (selected) selectedBorderColor else borderColor
}

object LevyraConnectedDefaults {

    val Gap: Dp = 2.dp
    val OuterCorner: Dp = LevyraPlayerDesign.CornerMd
    val InnerCorner: Dp = LevyraPlayerDesign.CornerXxs

    @Composable
    fun style(
        accent: Color? = null,
        gap: Dp = Gap,
        outerCorner: Dp = OuterCorner,
        innerCorner: Dp = InnerCorner
    ): LevyraConnectedStyle {
        val border = LevyraGlassBorder
        return remember(accent, gap, outerCorner, innerCorner, border) {
            LevyraConnectedStyle(
                gap = gap,
                shapes = LevyraConnectedShapes(outerCorner, innerCorner),
                fill = Color.White.copy(alpha = 0.058f),
                selectedFill = accent?.copy(alpha = 0.13f) ?: Color.White.copy(alpha = 0.10f),
                disabledFill = Color.White.copy(alpha = 0.02f),
                borderColor = border,
                selectedBorderColor = accent?.copy(alpha = 0.30f) ?: border,
                borderWidth = LevyraPlayerDesign.Hairline
            )
        }
    }
}

fun Modifier.levyraConnectedRowSurface(
    position: LevyraConnectedPosition,
    style: LevyraConnectedStyle,
    selected: Boolean = false,
    enabled: Boolean = true,
    bordered: Boolean = selected || position == LevyraConnectedPosition.Single
): Modifier {
    val shape = style.shapes.forRowPosition(position)
    val base = this.clip(shape).background(style.fillFor(selected, enabled), shape)
    return if (bordered) base.border(style.borderWidth, style.borderFor(selected), shape) else base
}

fun Modifier.levyraConnectedSurface(
    position: LevyraConnectedPosition,
    style: LevyraConnectedStyle,
    selected: Boolean = false,
    enabled: Boolean = true,
    bordered: Boolean = selected || position == LevyraConnectedPosition.Single
): Modifier {
    val shape = style.shapes.forPosition(position)
    val base = this.clip(shape).background(style.fillFor(selected, enabled), shape)
    return if (bordered) base.border(style.borderWidth, style.borderFor(selected), shape) else base
}

@Composable
fun LevyraConnectedGroup(
    modifier: Modifier = Modifier,
    style: LevyraConnectedStyle = LevyraConnectedDefaults.style(),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(style.gap),
        content = content
    )
}
