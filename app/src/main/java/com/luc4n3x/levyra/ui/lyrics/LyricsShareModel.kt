package com.luc4n3x.levyra.ui.lyrics

import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.Track

internal const val LYRICS_SHARE_MAX_LINES = 5

internal enum class LyricsShareCardStyle {
    ARTWORK,
    GRADIENT,
    MINIMAL
}

internal enum class LyricsShareTextMode {
    ORIGINAL,
    TRANSLATION,
    ROMANIZATION
}

internal fun isLyricsShareSelectable(line: LyricLine): Boolean =
    line.text.isNotBlank() && !line.isInstrumental && !line.isMetadata

internal fun lyricsShareSelectableCount(range: IntRange?, lines: List<LyricLine>): Int {
    if (range == null) return 0
    var count = 0
    for (index in range) {
        if (lines.getOrNull(index)?.let(::isLyricsShareSelectable) == true) count++
    }
    return count
}

internal fun toggleLyricsShareSelection(
    current: IntRange?,
    index: Int,
    lines: List<LyricLine>,
    maxLines: Int = LYRICS_SHARE_MAX_LINES
): IntRange? {
    val tapped = lines.getOrNull(index) ?: return current
    if (!isLyricsShareSelectable(tapped) || maxLines <= 0) return current
    if (current == null || current.isEmpty()) return index..index

    val first = current.first
    val last = current.last
    return when {
        index == first -> nextSelectable(lines, first + 1, last, forward = true)?.let { it..last }
        index == last -> nextSelectable(lines, last - 1, first, forward = false)?.let { first..it }
        index in current -> first..index
        index > last -> trimToLimit(first, index, lines, maxLines, keepEnd = true)
        else -> trimToLimit(index, last, lines, maxLines, keepEnd = false)
    }
}

private fun nextSelectable(lines: List<LyricLine>, from: Int, limit: Int, forward: Boolean): Int? {
    var index = from
    while (if (forward) index <= limit else index >= limit) {
        if (lines.getOrNull(index)?.let(::isLyricsShareSelectable) == true) return index
        index += if (forward) 1 else -1
    }
    return null
}

private fun trimToLimit(
    start: Int,
    end: Int,
    lines: List<LyricLine>,
    maxLines: Int,
    keepEnd: Boolean
): IntRange {
    var first = start
    var last = end
    while (lyricsShareSelectableCount(first..last, lines) > maxLines) {
        if (keepEnd) {
            first = nextSelectable(lines, first + 1, last, forward = true) ?: last
        } else {
            last = nextSelectable(lines, last - 1, first, forward = false) ?: first
        }
    }
    return first..last
}

internal fun lyricsShareSelectedLines(range: IntRange?, lines: List<LyricLine>): List<LyricLine> {
    if (range == null) return emptyList()
    return range.mapNotNull { index -> lines.getOrNull(index)?.takeIf(::isLyricsShareSelectable) }
}

internal data class LyricsShareLine(
    val original: String,
    val translation: String,
    val romanization: String
)

internal data class LyricsShareSnapshot(
    val track: Track,
    val lines: List<LyricsShareLine>
) {
    val trackId: String get() = track.id

    val availableModes: List<LyricsShareTextMode> = buildList {
        add(LyricsShareTextMode.ORIGINAL)
        if (lines.any { it.translation.isNotBlank() }) add(LyricsShareTextMode.TRANSLATION)
        if (lines.any { it.romanization.isNotBlank() }) add(LyricsShareTextMode.ROMANIZATION)
    }

    fun textLines(mode: LyricsShareTextMode): List<String> = lines.mapNotNull { line ->
        val preferred = when (mode) {
            LyricsShareTextMode.ORIGINAL -> line.original
            LyricsShareTextMode.TRANSLATION -> line.translation
            LyricsShareTextMode.ROMANIZATION -> line.romanization
        }
        preferred.trim().ifBlank { line.original.trim() }.takeIf(String::isNotBlank)
    }
}

internal fun lyricsShareSnapshot(track: Track?, selected: List<LyricLine>): LyricsShareSnapshot? {
    if (track == null) return null
    val lines = selected
        .filter(::isLyricsShareSelectable)
        .take(LYRICS_SHARE_MAX_LINES)
        .map { line ->
            LyricsShareLine(
                original = line.text.trim(),
                translation = line.translated.trim(),
                romanization = line.romanized.ifBlank {
                    line.words.joinToString("") { word -> word.romanized }
                }.trim()
            )
        }
    return lines.takeIf(List<LyricsShareLine>::isNotEmpty)?.let { LyricsShareSnapshot(track, it) }
}

internal data class LyricsShareCardContent(
    val title: String,
    val artist: String,
    val lyrics: List<String>
)

internal fun lyricsShareCardContent(
    snapshot: LyricsShareSnapshot,
    mode: LyricsShareTextMode
): LyricsShareCardContent = LyricsShareCardContent(
    title = snapshot.track.title.trim(),
    artist = snapshot.track.artist.trim(),
    lyrics = snapshot.textLines(mode)
        .map { boundedLyricsShareText(it, LYRICS_SHARE_MAX_LINE_CODE_POINTS) }
        .filter(String::isNotBlank)
)

internal const val LYRICS_SHARE_MAX_LINE_CODE_POINTS = 240

internal fun lyricsShareCaption(content: LyricsShareCardContent): String =
    listOf(content.title, content.artist).filter(String::isNotBlank).joinToString(" — ")

internal object LyricsShareCardColors {
    private const val MAX_BACKGROUND_LUMINANCE = 0.16
    private const val STEP = 0.92

    fun background(color: Int, fallback: Int): Int {
        val opaque = if (color == 0) fallback else color
        var red = (opaque shr 16) and 0xFF
        var green = (opaque shr 8) and 0xFF
        var blue = opaque and 0xFF
        while (luminance(red, green, blue) > MAX_BACKGROUND_LUMINANCE && red + green + blue > 0) {
            red = (red * STEP).toInt()
            green = (green * STEP).toInt()
            blue = (blue * STEP).toInt()
        }
        return (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
    }

    fun contrastWithWhite(color: Int): Double {
        val l = luminance((color shr 16) and 0xFF, (color shr 8) and 0xFF, color and 0xFF)
        return 1.05 / (l + 0.05)
    }

    private fun luminance(red: Int, green: Int, blue: Int): Double =
        0.2126 * linear(red) + 0.7152 * linear(green) + 0.0722 * linear(blue)

    private fun linear(channel: Int): Double {
        val c = channel / 255.0
        return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
    }
}
