package com.luc4n3x.levyra.ui

import androidx.compose.foundation.gestures.PressGestureScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch as launchChild

/**
 * Keeps delayed long-press work attached to the active pointer-input coroutine.
 * Compose's press callback is a suspend receiver rather than a CoroutineScope,
 * so calling the deprecated scope-less launch would otherwise fail compilation.
 */
internal suspend fun PressGestureScope.launch(
    block: suspend CoroutineScope.() -> Unit
): Job = CoroutineScope(currentCoroutineContext()).launchChild(block = block)
