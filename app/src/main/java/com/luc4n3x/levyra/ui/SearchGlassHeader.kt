package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.unit.dp

@Composable
internal fun SearchGlassHeader(
    glass: GlassBackdropState,
    listState: LazyListState,
    gutter: Dp,
    onHeight: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scrolled by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 }
    }
    val visibility by animateFloatAsState(
        targetValue = if (scrolled) 1f else 0f,
        animationSpec = if (LocalAnimationsEnabled.current) spring() else snap(),
        label = "searchGlassHeader"
    )
    SideEffect { glass.sampling = scrolled || visibility > 0f }
    val surface = MaterialTheme.colorScheme.surfaceContainer
    Box(
        modifier
            .fillMaxWidth()
            .recentSearchesBleed(gutter)
            .onSizeChanged { onHeight(it.height) }
    ) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    alpha = visibility
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        Brush.verticalGradient(0f to Color.Black, 0.9f to Color.Black, 1f to Color.Transparent),
                        blendMode = BlendMode.DstIn
                    )
                }
                .glassFrost(
                    state = glass,
                    blurRadius = SearchGlassBlur,
                    onDrawFrosted = { drawRect(surface.copy(alpha = 0.88f)) },
                    onDrawFallback = { drawRect(surface.copy(alpha = 0.97f)) }
                )
        )
        Box(Modifier.padding(horizontal = gutter)) { content() }
    }
}

private val SearchGlassBlur = 24.dp
