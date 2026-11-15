package com.luc4n3x.levyra.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val LocalSectionActionIconOnly = staticCompositionLocalOf { false }

internal enum class SectionHeaderArrangement { Inline, IconOnly, Stacked }

internal fun sectionHeaderArrangement(
    width: Int,
    gap: Int,
    titleWidestWord: Int,
    titleSingleLine: Int,
    action: Int,
    iconOnlyAction: Int
): SectionHeaderArrangement {
    fun titleFitsTwoLinesBeside(actionWidth: Int): Boolean {
        val room = width - gap - actionWidth
        return 2 * room >= titleSingleLine + titleWidestWord
    }
    return when {
        action == 0 || titleFitsTwoLinesBeside(action) -> SectionHeaderArrangement.Inline
        iconOnlyAction < action && titleFitsTwoLinesBeside(iconOnlyAction) -> SectionHeaderArrangement.IconOnly
        else -> SectionHeaderArrangement.Stacked
    }
}

@Composable
internal fun LevyraAdaptiveSectionHeader(
    modifier: Modifier = Modifier,
    gap: Dp = 12.dp,
    stackedGap: Dp = 4.dp,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    title: @Composable () -> Unit,
    action: (@Composable RowScope.() -> Unit)? = null
) {
    if (action == null) {
        Box(modifier) { title() }
        return
    }
    Layout(
        contents = listOf(
            { Box { title() } },
            {
                CompositionLocalProvider(LocalSectionActionIconOnly provides false) {
                    Row(verticalAlignment = Alignment.CenterVertically, content = action)
                }
            },
            {
                CompositionLocalProvider(LocalSectionActionIconOnly provides true) {
                    Row(verticalAlignment = Alignment.CenterVertically, content = action)
                }
            }
        ),
        modifier = modifier
    ) { (titleSlot, actionSlot, iconOnlySlot), constraints ->
        val titleMeasurable = titleSlot.first()
        val actionMeasurable = actionSlot.first()
        val iconOnlyMeasurable = iconOnlySlot.first()
        val titleSingleLine = titleMeasurable.maxIntrinsicWidth(Constraints.Infinity)
        val actionWidth = actionMeasurable.maxIntrinsicWidth(Constraints.Infinity)
        val gapPx = if (actionWidth > 0) gap.roundToPx() else 0
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else titleSingleLine + gapPx + actionWidth
        val arrangement = sectionHeaderArrangement(
            width = width,
            gap = gapPx,
            titleWidestWord = titleMeasurable.minIntrinsicWidth(Constraints.Infinity),
            titleSingleLine = titleSingleLine,
            action = actionWidth,
            iconOnlyAction = iconOnlyMeasurable.maxIntrinsicWidth(Constraints.Infinity)
        )
        val looseAction = Constraints(maxWidth = width, maxHeight = constraints.maxHeight)
        if (arrangement == SectionHeaderArrangement.Stacked) {
            val titlePlaceable = titleMeasurable.measure(Constraints.fixedWidth(width).copy(maxHeight = constraints.maxHeight))
            val actionPlaceable = actionMeasurable.measure(looseAction)
            val stackedGapPx = stackedGap.roundToPx()
            val height = maxOf(constraints.minHeight, titlePlaceable.height + stackedGapPx + actionPlaceable.height)
            layout(width, height) {
                titlePlaceable.placeRelative(0, 0)
                actionPlaceable.placeRelative(0, titlePlaceable.height + stackedGapPx)
            }
        } else {
            val chosen = if (arrangement == SectionHeaderArrangement.Inline) actionMeasurable else iconOnlyMeasurable
            val actionPlaceable = chosen.measure(looseAction)
            val titleWidth = (width - gapPx - actionPlaceable.width).coerceAtLeast(0)
            val titlePlaceable = titleMeasurable.measure(Constraints.fixedWidth(titleWidth).copy(maxHeight = constraints.maxHeight))
            val height = maxOf(constraints.minHeight, titlePlaceable.height, actionPlaceable.height)
            layout(width, height) {
                titlePlaceable.placeRelative(0, verticalAlignment.align(titlePlaceable.height, height))
                actionPlaceable.placeRelative(width - actionPlaceable.width, verticalAlignment.align(actionPlaceable.height, height))
            }
        }
    }
}
