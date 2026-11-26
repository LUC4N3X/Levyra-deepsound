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
            .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
            .semantics {
                if (description.isNotBlank()) contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(fraction.coerceIn(0f, 1f), 0f..1f)
                if (enabled) {
                    setProgress { target ->
                        change(target.coerceIn(0f, 1f))
                        finished()
                        true
                    }
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    change((offset.x / size.width).coerceIn(0f, 1f))
                    finished()
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        change((offset.x / size.width).coerceIn(0f, 1f))
                    },
                    onDragEnd = {
                        dragging = false
                        finished()
                    },
                    onDragCancel = {
                        dragging = false
                        finished()
                    },
                    onHorizontalDrag = { pointer, _ ->
                        change((pointer.position.x / size.width).coerceIn(0f, 1f))
                    }
                )
            }
    ) {
        val track = (if (emphasized) 5.dp else 4.dp).toPx()
        val top = (size.height - track) / 2f
        val radius = CornerRadius(track / 2f)
        val value = fraction.coerceIn(0f, 1f)
        drawRoundRect(inactiveColor, Offset(0f, top), Size(size.width, track), radius)
        val active = if (enabled) (if (emphasized) activeColor else activeColor.copy(alpha = 0.92f)) else activeColor.copy(alpha = 0.38f)
        drawRoundRect(active, Offset(0f, top), Size(size.width * value, track), radius)
        if (emphasized) {
            drawCircle(activeColor, radius = 6.dp.toPx(), center = Offset(size.width * value, size.height / 2f))
        }
    }
}
