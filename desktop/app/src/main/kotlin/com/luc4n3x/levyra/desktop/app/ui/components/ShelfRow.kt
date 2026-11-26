package com.luc4n3x.levyra.desktop.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import kotlinx.coroutines.launch

@Composable
fun ShelfRow(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: LazyListScope.() -> Unit
) {
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val direction = LocalLayoutDirection.current
    val dragState = rememberDraggableState { delta -> state.dispatchRawDelta(-delta) }
    val scrollable = state.canScrollForward || state.canScrollBackward

    fun page(forward: Boolean) {
        val viewport = state.layoutInfo.viewportSize.width * PAGE_FRACTION
        scope.launch { state.animateScrollBy(if (forward) viewport else -viewport) }
    }

    Column(modifier = modifier.hoverable(interaction)) {
        Box(Modifier.fillMaxWidth()) {
            LazyRow(
                state = state,
                contentPadding = contentPadding,
                horizontalArrangement = horizontalArrangement,
                modifier = Modifier
                    .fillMaxWidth()
                    .draggable(dragState, Orientation.Horizontal, reverseDirection = false),
                content = content
            )
            ShelfArrow(
                visible = hovered && state.canScrollBackward,
                forward = false,
                onClick = { page(forward = false) },
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp)
            )
            ShelfArrow(
                visible = hovered && state.canScrollForward,
                forward = true,
                onClick = { page(forward = true) },
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)
            )
        }
        if (scrollable) {
            val onSurface = MaterialTheme.colorScheme.onSurface
            HorizontalScrollbar(
                adapter = rememberScrollbarAdapter(state),
                style = ScrollbarStyle(
                    minimalHeight = 32.dp,
                    thickness = 6.dp,
                    shape = RoundedCornerShape(3.dp),
                    hoverDurationMillis = 200,
                    unhoverColor = onSurface.copy(alpha = if (hovered) 0.22f else 0.10f),
                    hoverColor = onSurface.copy(alpha = 0.45f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = contentPadding.calculateStartPadding(direction),
                        end = contentPadding.calculateEndPadding(direction),
                        top = 8.dp
                    )
            )
        }
    }
}

@Composable
private fun ShelfArrow(
    visible: Boolean,
    forward: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.94f))
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = LevyraIcons.ChevronLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = if (forward) 180f else 0f }
            )
        }
    }
}

private const val PAGE_FRACTION = 0.8f
