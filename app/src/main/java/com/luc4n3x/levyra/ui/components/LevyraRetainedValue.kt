package com.luc4n3x.levyra.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

private class RetainedValueHolder<T : Any> {
    var value: T? = null
}

@Composable
internal fun <T : Any> rememberLastNonNull(value: T?, key: Any? = null): T? {
    val holder = remember(key) { RetainedValueHolder<T>() }
    if (value != null) holder.value = value
    return holder.value
}
