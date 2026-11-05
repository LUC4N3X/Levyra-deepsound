package com.luc4n3x.levyra.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
internal fun LibraryCoverStack(
    covers: List<String>,
    ringColor: Color,
    modifier: Modifier = Modifier,
    round: Boolean = false,
    size: Dp = 48.dp,
    step: Dp = 30.dp
) {
    if (covers.isEmpty()) return
    val colors = MaterialTheme.colorScheme
    val shape: Shape = if (round) CircleShape else RoundedCornerShape(size * 0.22f)
    Box(
        modifier = modifier
            .width(size + step * (covers.size - 1))
            .height(size)
            .clearAndSetSemantics {}
    ) {
        covers.forEachIndexed { index, url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .offset(x = step * index)
                    .size(size)
                    .clip(shape)
                    .background(colors.surfaceContainerHighest)
                    .border(2.dp, ringColor, shape)
            )
        }
    }
}
