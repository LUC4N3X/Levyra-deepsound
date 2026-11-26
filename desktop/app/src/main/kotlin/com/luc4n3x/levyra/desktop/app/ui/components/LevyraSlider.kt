package com.luc4n3x.levyra.desktop.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp

@Composable
fun LevyraSlider(
    fraction: Float,
    onFractionChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onChangeFinished: () -> Unit = {},
    enabled: Boolean = true,
    activeColor: Color = MaterialTheme.colorScheme.onSurface,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
    description: String = ""
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var dragging by remember { mutableStateOf(false) }
    val change by rememberUpdatedState(onFractionChange)
    val finished by rememberUpdatedState(onChangeFinished)
    val emphasized = enabled && (hovered || dragging)
    Canvas(
        modifier = modifier
            .height(16.dp)
            .hoverable(interaction, enabled)
            .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Hand).sliderInput(fraction, description, { dragging = it }, { change(it) }, { finished() }) else Modifier.sliderSemantics(fraction, description))
    ) {
        val track = (if (emphasized) 5.dp else 4.dp).toPx()
        val top = (size.height - track) / 2f
        val radius = CornerRadius(track / 2f)
        val value = fraction.coerceIn(0f, 1f)
        drawRoundRect(inactiveColor, Offset(0f, top), Size(size.width, track), radius)
        val active = activeTint(activeColor, enabled, emphasized)
        drawRoundRect(active, Offset(0f, top), Size(size.width * value, track), radius)
        if (emphasized) {
            drawCircle(activeColor, radius = 6.dp.toPx(), center = Offset(size.width * value, size.height / 2f))
        }
    }
}

private fun activeTint(color: Color, enabled: Boolean, emphasized: Boolean): Color = when {
    !enabled -> color.copy(alpha = 0.38f)
    emphasized -> color
    else -> color.copy(alpha = 0.92f)
}

private fun Modifier.sliderSemantics(fraction: Float, description: String, onSet: ((Float) -> Unit)? = null): Modifier = semantics {
    if (description.isNotBlank()) contentDescription = description
    progressBarRangeInfo = ProgressBarRangeInfo(fraction.coerceIn(0f, 1f), 0f..1f)
    if (onSet != null) {
        setProgress { target ->
            onSet(target.coerceIn(0f, 1f))
            true
        }
    }
}

private fun Modifier.sliderInput(
    fraction: Float,
    description: String,
    onDragging: (Boolean) -> Unit,
    change: (Float) -> Unit,
    finished: () -> Unit
): Modifier = this
    .sliderSemantics(fraction, description) { target ->
        change(target)
        finished()
    }
    .pointerInput(Unit) {
        detectTapGestures { offset ->
            change((offset.x / size.width).coerceIn(0f, 1f))
            finished()
        }
    }
    .pointerInput(Unit) {
        detectHorizontalDragGestures(
            onDragStart = { offset ->
                onDragging(true)
                change((offset.x / size.width).coerceIn(0f, 1f))
            },
            onDragEnd = {
                onDragging(false)
                finished()
            },
            onDragCancel = {
                onDragging(false)
                finished()
            },
            onHorizontalDrag = { pointer, _ ->
                change((pointer.position.x / size.width).coerceIn(0f, 1f))
            }
        )
    }
