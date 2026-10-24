package com.luc4n3x.levyra.ui

fun lyricsFocusAlpha(
    distance: Int,
    focusMode: Boolean,
    compact: Boolean,
    synced: Boolean
): Float {
    if (!synced) return 1f
    if (!focusMode && !compact) return 1f
    val steps = distance.coerceAtLeast(0)
    return when (steps) {
        0 -> 1f
        1 -> if (compact) 0.70f else 0.78f
        2 -> if (compact) 0.46f else 0.56f
        3 -> if (compact) 0.34f else 0.42f
        else -> if (compact) 0.26f else 0.32f
    }
}

fun lyricsFocusBlurDp(
    distance: Int,
    focusMode: Boolean,
    synced: Boolean,
    blurEnabled: Boolean
): Float {
    if (!focusMode || !synced || !blurEnabled) return 0f
    return when (distance.coerceAtLeast(0)) {
        0, 1, 2 -> 0f
        3 -> 0.8f
        else -> 1.6f
    }
}

fun lyricsBackdropAlpha(focusMode: Boolean, cinema: Boolean): Float = when {
    !cinema -> 0f
    focusMode -> 0.26f
    else -> 0.18f
}
