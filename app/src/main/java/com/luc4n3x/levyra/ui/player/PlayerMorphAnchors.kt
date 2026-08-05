package com.luc4n3x.levyra.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

enum class PlayerMorphSlot {
    Mini,
    Full
}

@Stable
class PlayerMorphAnchors {
    var miniBounds by mutableStateOf<Rect?>(null)
        private set
    var fullBounds by mutableStateOf<Rect?>(null)
        private set

    fun update(slot: PlayerMorphSlot, bounds: Rect) {
        val sanitized = bounds.takeIf { it.width > 0f && it.height > 0f } ?: return
        when (slot) {
            PlayerMorphSlot.Mini -> if (miniBounds != sanitized) miniBounds = sanitized
            PlayerMorphSlot.Full -> if (fullBounds != sanitized) fullBounds = sanitized
        }
    }

    fun resolve(fraction: Float): Rect? {
        val start = miniBounds ?: return null
        val end = fullBounds ?: return null
        return lerpRect(start, end, fraction.coerceIn(0f, 1f))
    }
}

@Composable
fun rememberPlayerMorphAnchors(): PlayerMorphAnchors = remember { PlayerMorphAnchors() }

fun Modifier.playerMorphAnchor(
    anchors: PlayerMorphAnchors,
    slot: PlayerMorphSlot
): Modifier = this.onGloballyPositioned { coordinates ->
    if (coordinates.isAttached) {
        anchors.update(slot, coordinates.boundsInRoot())
    }
}

fun lerpRect(start: Rect, end: Rect, fraction: Float): Rect {
    val t = fraction.coerceIn(0f, 1f)
    return Rect(
        left = start.left + (end.left - start.left) * t,
        top = start.top + (end.top - start.top) * t,
        right = start.right + (end.right - start.right) * t,
        bottom = start.bottom + (end.bottom - start.bottom) * t
    )
}

fun morphCornerRadius(startRadius: Float, endRadius: Float, fraction: Float): Float {
    val t = fraction.coerceIn(0f, 1f)
    return startRadius + (endRadius - startRadius) * t
}
