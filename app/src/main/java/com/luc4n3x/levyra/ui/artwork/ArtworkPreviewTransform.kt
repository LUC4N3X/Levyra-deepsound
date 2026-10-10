package com.luc4n3x.levyra.ui.artwork

const val ARTWORK_PREVIEW_MIN_SCALE = 1f
const val ARTWORK_PREVIEW_MAX_SCALE = 4.5f
const val ARTWORK_PREVIEW_DOUBLE_TAP_SCALE = 2.5f

data class ArtworkPreviewBounds(
    val width: Float,
    val height: Float
)

data class ArtworkPreviewOffset(
    val x: Float,
    val y: Float
)

fun artworkPreviewFittedBounds(
    viewportWidth: Float,
    viewportHeight: Float,
    artworkWidth: Float,
    artworkHeight: Float
): ArtworkPreviewBounds {
    if (viewportWidth <= 0f || viewportHeight <= 0f) return ArtworkPreviewBounds(0f, 0f)
    if (artworkWidth <= 0f || artworkHeight <= 0f) {
        val side = minOf(viewportWidth, viewportHeight)
        return ArtworkPreviewBounds(side, side)
    }
    val scale = minOf(viewportWidth / artworkWidth, viewportHeight / artworkHeight)
    return ArtworkPreviewBounds(artworkWidth * scale, artworkHeight * scale)
}

fun artworkPreviewClampScale(scale: Float): Float =
    scale.coerceIn(ARTWORK_PREVIEW_MIN_SCALE, ARTWORK_PREVIEW_MAX_SCALE)

fun artworkPreviewMaxOffset(
    renderedSize: Float,
    viewportSize: Float,
    scale: Float
): Float {
    val scaled = renderedSize * scale
    if (scaled <= viewportSize) return 0f
    return (scaled - viewportSize) / 2f
}

fun artworkPreviewClampOffset(
    offset: ArtworkPreviewOffset,
    bounds: ArtworkPreviewBounds,
    viewportWidth: Float,
    viewportHeight: Float,
    scale: Float
): ArtworkPreviewOffset {
    val maxX = artworkPreviewMaxOffset(bounds.width, viewportWidth, scale)
    val maxY = artworkPreviewMaxOffset(bounds.height, viewportHeight, scale)
    return ArtworkPreviewOffset(
        x = offset.x.coerceIn(-maxX, maxX),
        y = offset.y.coerceIn(-maxY, maxY)
    )
}
