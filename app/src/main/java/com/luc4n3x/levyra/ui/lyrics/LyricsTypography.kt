package com.luc4n3x.levyra.ui.lyrics

const val LYRICS_MIN_FONT_SIZE_SP = 15f
const val LYRICS_MAX_SHRINK_FACTOR = 0.76f

fun lyricsWidthScale(availableWidthDp: Float): Float = when {
    availableWidthDp <= 0f -> 1f
    availableWidthDp < 340f -> 0.90f
    availableWidthDp < 600f -> 1f
    availableWidthDp < 840f -> 1.10f
    else -> 1.18f
}

fun lyricsHeightScale(availableHeightDp: Float): Float = when {
    availableHeightDp <= 0f -> 1f
    availableHeightDp < 420f -> 0.84f
    availableHeightDp < 560f -> 0.93f
    else -> 1f
}

fun adaptiveLyricFontSizeSp(
    baseSizeSp: Float,
    characterCount: Int,
    availableWidthDp: Float,
    availableHeightDp: Float,
    fontScale: Float
): Float {
    if (baseSizeSp <= 0f) return baseSizeSp
    val scaled = baseSizeSp * lyricsWidthScale(availableWidthDp) * lyricsHeightScale(availableHeightDp)
    if (characterCount <= 0 || availableWidthDp <= 0f) return scaled
    val safeFontScale = fontScale.coerceAtLeast(0.5f)
    val charactersPerLine = availableWidthDp / (scaled * safeFontScale * AVERAGE_GLYPH_WIDTH_RATIO)
    if (charactersPerLine <= 0f) return scaled
    val projectedLines = characterCount / charactersPerLine
    if (projectedLines <= COMFORTABLE_LINE_COUNT) return scaled
    val shrink = (COMFORTABLE_LINE_COUNT / projectedLines).coerceAtLeast(LYRICS_MAX_SHRINK_FACTOR)
    return (scaled * shrink).coerceAtLeast(LYRICS_MIN_FONT_SIZE_SP / safeFontScale.coerceAtLeast(1f))
}

private const val AVERAGE_GLYPH_WIDTH_RATIO = 0.52f
private const val COMFORTABLE_LINE_COUNT = 2f
