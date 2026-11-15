package com.luc4n3x.levyra.ui.components

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.modifiers.TextAutoSizeLayoutScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal data class LevyraWordSafeAutoSize(
    val minFontSize: TextUnit,
    val maxFontSize: TextUnit,
    val step: TextUnit = 1.sp
) : TextAutoSize {
    override fun TextAutoSizeLayoutScope.getFontSize(constraints: Constraints, text: AnnotatedString): TextUnit {
        var size = maxFontSize.value
        while (size > minFontSize.value) {
            val layout = performLayout(constraints, text, size.sp)
            val widestWordFits = layout.multiParagraph.intrinsics.minIntrinsicWidth <= constraints.maxWidth
            if (!layout.hasVisualOverflow && widestWordFits) return size.sp
            size -= step.value
        }
        return minFontSize
    }
}

@Composable
internal fun LevyraWrappingHeaderRow(
    middleMinWidth: Dp,
    modifier: Modifier = Modifier,
    leadGap: Dp = 12.dp,
    trailingGap: Dp = 8.dp,
    wrappedGap: Dp = 6.dp,
    lead: @Composable () -> Unit,
    middle: @Composable () -> Unit,
    trailing: @Composable () -> Unit
) {
    Layout(contents = listOf(lead, middle, trailing), modifier = modifier) { (leadSlot, middleSlot, trailingSlot), constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val leadPlaceable = leadSlot.first().measure(loose)
        val trailingPlaceable = trailingSlot.first().measure(loose)
        val leadGapPx = leadGap.roundToPx()
        val chrome = leadPlaceable.width + leadGapPx + trailingGap.roundToPx() + trailingPlaceable.width
        val width = if (constraints.hasBoundedWidth) {
            constraints.maxWidth
        } else {
            chrome + middleSlot.first().maxIntrinsicWidth(Constraints.Infinity)
        }
        val room = width - chrome
        if (room >= middleMinWidth.roundToPx()) {
            val middlePlaceable = middleSlot.first().measure(Constraints(maxWidth = room, maxHeight = constraints.maxHeight))
            val height = maxOf(constraints.minHeight, leadPlaceable.height, middlePlaceable.height, trailingPlaceable.height)
            layout(width, height) {
                leadPlaceable.placeRelative(0, (height - leadPlaceable.height) / 2)
                middlePlaceable.placeRelative(leadPlaceable.width + leadGapPx, (height - middlePlaceable.height) / 2)
                trailingPlaceable.placeRelative(width - trailingPlaceable.width, (height - trailingPlaceable.height) / 2)
            }
        } else {
            val topHeight = maxOf(leadPlaceable.height, trailingPlaceable.height)
            val wrappedGapPx = wrappedGap.roundToPx()
            val middlePlaceable = middleSlot.first().measure(Constraints(maxWidth = width))
            val height = maxOf(constraints.minHeight, topHeight + wrappedGapPx + middlePlaceable.height)
            layout(width, height) {
                leadPlaceable.placeRelative(0, (topHeight - leadPlaceable.height) / 2)
                trailingPlaceable.placeRelative(width - trailingPlaceable.width, (topHeight - trailingPlaceable.height) / 2)
                middlePlaceable.placeRelative(0, topHeight + wrappedGapPx)
            }
        }
    }
}

@Composable
internal fun LevyraFirstFitting(variants: List<@Composable () -> Unit>, modifier: Modifier = Modifier) {
    Layout(contents = variants, modifier = modifier) { slots, constraints ->
        val chosen = slots.firstOrNull { slot ->
            slot.first().maxIntrinsicWidth(Constraints.Infinity) <= constraints.maxWidth
        } ?: slots.last()
        val placeable = chosen.first().measure(constraints.copy(minWidth = 0))
        layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
    }
}
