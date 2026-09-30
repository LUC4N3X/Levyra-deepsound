package com.luc4n3x.levyra.ui.artwork

import kotlin.math.abs
import kotlin.math.max

private const val BAR_TOLERANCE = 22
private const val BAR_SHARE = 0.9f
private const val EDGE_MATCH_TOLERANCE = 28
private const val EDGE_SHARE = 0.04f
private const val BAND_START = 0.3f
private const val BAND_END = 0.7f
private const val MIN_CONTENT_SHARE = 0.25f
private const val FULL_CONTENT_SHARE = 0.97f
private const val SQUARE_CONTENT_MIN = 0.5f
private const val SQUARE_CONTENT_MAX = 0.62f
private const val FIT_MARGIN = 1.02f
private const val MAX_FIT_ZOOM = 2.2f

internal class VideoFrameFit(val zoom: Float, val squareContent: Boolean) {
    companion object {
        val Full = VideoFrameFit(zoom = 1f, squareContent = false)
    }
}

internal class FramePixels(val values: IntArray, val width: Int, val height: Int)

internal fun measureVideoFrameFit(pixels: IntArray, width: Int, height: Int): VideoFrameFit {
    if (width < 16 || height < 16 || pixels.size < width * height) return VideoFrameFit.Full
    val frame = FramePixels(pixels, width, height)
    val horizontal = contentShare(frame, rows = false)
    val vertical = contentShare(frame, rows = true)
    val measurable = horizontal >= MIN_CONTENT_SHARE && vertical >= MIN_CONTENT_SHARE
    return if (measurable) fitFor(horizontal, vertical) else VideoFrameFit.Full
}

private fun fitFor(horizontal: Float, vertical: Float): VideoFrameFit {
    val full = horizontal >= FULL_CONTENT_SHARE && vertical >= FULL_CONTENT_SHARE
    val zoom = if (full) 1f else max(1f / horizontal, 1f / vertical) * FIT_MARGIN
    return VideoFrameFit(
        zoom = zoom.coerceIn(1f, MAX_FIT_ZOOM),
        squareContent = horizontal in SQUARE_CONTENT_MIN..SQUARE_CONTENT_MAX && vertical >= FULL_CONTENT_SHARE
    )
}

private fun contentShare(frame: FramePixels, rows: Boolean): Float {
    val lines = if (rows) frame.height else frame.width
    val edge = max(1, (lines * EDGE_SHARE).toInt())
    val leading = edgeStats(frame, 0 until edge, rows)
    val trailing = edgeStats(frame, lines - edge until lines, rows)
    val barred = leading.uniform && trailing.uniform &&
        channelDistance(leading.mean, trailing.mean) <= EDGE_MATCH_TOLERANCE
    return if (barred) barredContentShare(frame, rows, averageColor(leading.mean, trailing.mean)) else 1f
}

private fun barredContentShare(frame: FramePixels, rows: Boolean, bar: Int): Float {
    val lines = if (rows) frame.height else frame.width
    var start = 0
    while (start < lines && lineMatches(frame, start, rows, bar)) start += 1
    var end = lines - 1
    while (end > start && lineMatches(frame, end, rows, bar)) end -= 1
    return (end - start + 1).coerceAtLeast(0).toFloat() / lines
}

private class EdgeStats(val mean: Int, val uniform: Boolean)

private fun edgeStats(frame: FramePixels, lines: IntRange, rows: Boolean): EdgeStats {
    var red = 0L
    var green = 0L
    var blue = 0L
    var count = 0
    for (line in lines) {
        frame.forEachBandPixel(line, rows) { color ->
            red += color shr 16 and 0xFF
            green += color shr 8 and 0xFF
            blue += color and 0xFF
            count += 1
        }
    }
    if (count == 0) return EdgeStats(0, false)
    val mean = rgb((red / count).toInt(), (green / count).toInt(), (blue / count).toInt())
    var close = 0
    for (line in lines) {
        frame.forEachBandPixel(line, rows) { color ->
            if (channelDistance(color, mean) <= BAR_TOLERANCE) close += 1
        }
    }
    return EdgeStats(mean, close >= count * BAR_SHARE)
}

private fun lineMatches(frame: FramePixels, line: Int, rows: Boolean, bar: Int): Boolean {
    var close = 0
    var count = 0
    frame.forEachBandPixel(line, rows) { color ->
        count += 1
        if (channelDistance(color, bar) <= BAR_TOLERANCE) close += 1
    }
    return count > 0 && close >= count * BAR_SHARE
}

private inline fun FramePixels.forEachBandPixel(line: Int, rows: Boolean, action: (Int) -> Unit) {
    if (rows) {
        for (x in (width * BAND_START).toInt() until (width * BAND_END).toInt()) action(values[line * width + x])
    } else {
        for (y in (height * BAND_START).toInt() until (height * BAND_END).toInt()) action(values[y * width + line])
    }
}

private fun channelDistance(first: Int, second: Int): Int = max(
    abs((first shr 16 and 0xFF) - (second shr 16 and 0xFF)),
    max(
        abs((first shr 8 and 0xFF) - (second shr 8 and 0xFF)),
        abs((first and 0xFF) - (second and 0xFF))
    )
)

private fun averageColor(first: Int, second: Int): Int = rgb(
    ((first shr 16 and 0xFF) + (second shr 16 and 0xFF)) / 2,
    ((first shr 8 and 0xFF) + (second shr 8 and 0xFF)) / 2,
    ((first and 0xFF) + (second and 0xFF)) / 2
)

private fun rgb(red: Int, green: Int, blue: Int): Int =
    0xFF shl 24 or (red shl 16) or (green shl 8) or blue
