package com.luc4n3x.levyra.ui

enum class LevyraLayoutMode {
    Compact,
    Medium,
    Expanded
}

enum class LevyraPlayerPane {
    Stacked,
    SideBySide
}

const val LevyraMediumWidthDp: Float = 600f
const val LevyraExpandedWidthDp: Float = 840f

fun resolveLevyraLayoutMode(widthDp: Float, heightDp: Float): LevyraLayoutMode {
    val safeWidth = widthDp.coerceAtLeast(0f)
    val safeHeight = heightDp.coerceAtLeast(0f)
    return when {
        safeWidth >= LevyraExpandedWidthDp -> LevyraLayoutMode.Expanded
        safeWidth >= LevyraMediumWidthDp -> LevyraLayoutMode.Medium
        safeHeight > 0f && safeHeight < 400f && safeWidth >= 480f -> LevyraLayoutMode.Medium
        else -> LevyraLayoutMode.Compact
    }
}

fun resolvePlayerPane(widthDp: Float, heightDp: Float): LevyraPlayerPane {
    val landscape = widthDp > heightDp
    val wideEnough = widthDp >= LevyraExpandedWidthDp || (landscape && widthDp >= LevyraMediumWidthDp)
    val tallEnough = heightDp >= 320f
    return if (wideEnough && tallEnough) LevyraPlayerPane.SideBySide else LevyraPlayerPane.Stacked
}

fun levyraContentMaxWidthDp(mode: LevyraLayoutMode): Float = when (mode) {
    LevyraLayoutMode.Compact -> 560f
    LevyraLayoutMode.Medium -> 720f
    LevyraLayoutMode.Expanded -> 1080f
}

fun levyraPlayerArtworkMaxWidthDp(pane: LevyraPlayerPane, mode: LevyraLayoutMode): Float = when {
    pane == LevyraPlayerPane.SideBySide -> 460f
    mode == LevyraLayoutMode.Compact -> 520f
    else -> 560f
}

fun levyraMiniPlayerMaxWidthDp(mode: LevyraLayoutMode): Float = when (mode) {
    LevyraLayoutMode.Compact -> Float.POSITIVE_INFINITY
    LevyraLayoutMode.Medium -> 640f
    LevyraLayoutMode.Expanded -> 760f
}

fun levyraFoldAwareGutterDp(mode: LevyraLayoutMode, compactPlayer: Boolean): Float = when {
    mode == LevyraLayoutMode.Expanded -> 34f
    mode == LevyraLayoutMode.Medium -> 28f
    compactPlayer -> 18f
    else -> 22f
}
