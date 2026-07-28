package com.luc4n3x.levyra.desktop.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import java.util.concurrent.atomic.AtomicInteger

object TextInputFocus {
    private val focusedFields = AtomicInteger(0)

    val active: Boolean get() = focusedFields.get() > 0

    internal fun acquire() {
        focusedFields.incrementAndGet()
    }

    internal fun release() {
        focusedFields.updateAndGet { current -> if (current > 0) current - 1 else 0 }
    }
}

@Composable
fun Modifier.tracksTextInputFocus(): Modifier {
    val focused = remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        onDispose {
            if (focused.value) {
                focused.value = false
                TextInputFocus.release()
            }
        }
    }
    return onFocusChanged { state ->
        if (state.isFocused == focused.value) return@onFocusChanged
        focused.value = state.isFocused
        if (state.isFocused) TextInputFocus.acquire() else TextInputFocus.release()
    }
}
