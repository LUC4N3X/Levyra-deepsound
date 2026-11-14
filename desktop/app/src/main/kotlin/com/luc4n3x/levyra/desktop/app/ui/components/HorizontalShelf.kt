package com.luc4n3x.levyra.desktop.app.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp

internal fun Modifier.horizontalShelfBleed(gutter: Dp): Modifier = layout { measurable, constraints ->
    val extra = gutter.roundToPx()
    val expanded = if (constraints.hasBoundedWidth) constraints.copy(maxWidth = constraints.maxWidth + extra * 2) else constraints
    val content = measurable.measure(expanded)
    layout((content.width - extra * 2).coerceAtLeast(0), content.height) { content.placeRelative(-extra, 0) }
}
