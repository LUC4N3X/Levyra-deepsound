package com.luc4n3x.levyra.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

@Composable
internal fun LevyraTabLabel(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    val maxSize = style.fontSize.takeIf { it.isSpecified } ?: 12.sp
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(textAlign = TextAlign.Center),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        autoSize = TextAutoSize.StepBased(minFontSize = tabLabelMinSize(maxSize), maxFontSize = maxSize)
    )
}

private fun tabLabelMinSize(maxSize: TextUnit): TextUnit = if (maxSize.value > 8f) 8.sp else maxSize
