package com.luc4n3x.levyra.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.Locale

internal val TorqueRiseShape = androidx.compose.foundation.shape.GenericShape { size, _ ->
    moveTo(0f, size.height * 0.56f)
    lineTo(size.width, size.height * 0.34f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}

internal val TorqueStripShape = androidx.compose.foundation.shape.GenericShape { size, _ ->
    moveTo(0f, size.height * 0.74f)
    lineTo(size.width, size.height * 0.62f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}

internal object SkinChrome {
    val active: Boolean get() = LevyraSkinManagesLayout
    val outerCorner: Dp get() = if (LevyraIsPrism) PrismDesign.surfaceCorner else Nocturne78Design.cardCorner
    val innerCorner: Dp get() = if (LevyraIsPrism) PrismDesign.controlCorner.coerceAtMost(6.dp) else 2.dp
    val fieldShape: Shape get() = RoundedCornerShape(if (LevyraIsPrism) PrismDesign.controlCorner else Nocturne78Design.controlCorner)
    val chipShape: Shape get() = when {
        LevyraIsTorque -> RoundedCornerShape(2.dp)
        LevyraIsPrism -> CircleShape
        else -> RoundedCornerShape(Nocturne78Design.controlCorner)
    }
    val iconShape: Shape get() = if (LevyraIsPrism) PrismDesign.cut(PrismDesign.controlCorner) else RoundedCornerShape(Nocturne78Design.controlCorner)
    val iconBackground: Color get() = when {
        LevyraIsTorque || LevyraIsIvory -> Color.Transparent
        LevyraIsPrism -> PrismAccent.Tint
        else -> LevyraPanelSoft
    }
    val iconTint: Color get() = if (LevyraIsTorque || LevyraIsIvory) LevyraText else LevyraCyan
    val chipSelectedContainer: Color get() = when {
        LevyraIsTorque -> LevyraActivePalette.blue
        LevyraIsIvory -> IvoryDesign.ink
        LevyraIsPrism -> PrismAccent.Tint
        else -> Color.Transparent
    }
    val chipSelectedContent: Color get() = if (LevyraIsTorque) Color.White else if (LevyraIsIvory) IvoryDesign.onInk else LevyraCyan
    val labelsUppercase: Boolean get() = LevyraIsNocturne || LevyraIsTorque

    fun label(text: String, locale: Locale): String = if (labelsUppercase) text.uppercase(locale) else text
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SkinSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val torque = LevyraIsTorque
    val travel by animateFloatAsState(if (checked) 1f else 0f, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "skin-switch")
    val accent = if (torque) LevyraActivePalette.blue else LevyraCyan
    val idle = LevyraMuted
    Canvas(modifier.size(width = 52.dp, height = 26.dp)) {
        val stroke = 1.5.dp.toPx()
        val inset = 3.dp.toPx()
        if (torque) drawTorqueSwitch(checked, travel, accent, idle, stroke, inset)
        else if (LevyraIsIvory) drawIvorySwitch(travel, IvoryDesign.ink, IvoryDesign.onInk, idle, stroke)
        else drawRectangularSwitch(checked, travel, accent, idle, stroke, inset)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTorqueSwitch(checked: Boolean, travel: Float, accent: Color, idle: Color, stroke: Float, inset: Float) {
    val w = size.width
    val h = size.height
    val slant = h * 0.45f
    val track = Path().apply {
        moveTo(slant, 0f); lineTo(w, 0f); lineTo(w - slant, h); lineTo(0f, h); close()
    }
    if (checked) drawPath(track, accent.copy(alpha = 0.25f))
    drawPath(track, if (checked) accent else idle, style = Stroke(stroke))
    val thumbWidth = w * 0.46f
    val x = slant * 0.25f + (w - thumbWidth - slant * 0.5f) * travel
    val thumb = Path().apply {
        moveTo(x + slant, inset); lineTo(x + thumbWidth, inset)
        lineTo(x + thumbWidth - slant, h - inset); lineTo(x, h - inset); close()
    }
    drawPath(thumb, if (checked) accent else idle)

}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawIvorySwitch(travel: Float, ink: Color, onInk: Color, idle: Color, stroke: Float) {
    val radius = CornerRadius(size.height / 2)
    drawRoundRect(ink.copy(alpha = travel), cornerRadius = radius)
    drawRoundRect(androidx.compose.ui.graphics.lerp(idle.copy(alpha = 0.6f), ink, travel), cornerRadius = radius, style = Stroke(stroke))
    val dot = size.height * 0.16f + size.height * 0.06f * travel
    val x = size.height / 2 + (size.width - size.height) * travel
    drawCircle(androidx.compose.ui.graphics.lerp(idle, onInk, travel), dot, Offset(x, size.height / 2))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRectangularSwitch(checked: Boolean, travel: Float, accent: Color, idle: Color, stroke: Float, inset: Float) {
    val w = size.width
    val h = size.height
    val radius = CornerRadius(3.dp.toPx())
    if (checked) drawRoundRect(accent.copy(alpha = 0.22f), cornerRadius = radius)
    drawRoundRect(if (checked) accent else idle, cornerRadius = radius, style = Stroke(stroke))
    val thumb = h - inset * 2
    val x = inset + (w - thumb - inset * 2) * travel
    drawRoundRect(if (checked) accent else idle, Offset(x, inset), Size(thumb, thumb), CornerRadius(2.dp.toPx()))
}
