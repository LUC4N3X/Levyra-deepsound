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
        1 -> if (compact) 0.58f else 0.62f
        2 -> if (compact) 0.25f else 0.34f
        3 -> 0.18f
        else -> if (compact) 0.10f else 0.12f
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
        0, 1 -> 0f
        2 -> 0.8f
        3 -> 1.6f
        else -> 2.4f
    }
}

fun lyricsBackdropAlpha(focusMode: Boolean, cinema: Boolean): Float = when {
    !cinema -> 0f
    focusMode -> 0.26f
    else -> 0.18f
}
