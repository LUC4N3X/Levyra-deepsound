package com.luc4n3x.levyra.domain

class LyricsEngine {
    fun currentLine(positionMs: Long, lines: List<LyricLine>): LyricLine? {
        return lines.firstOrNull { positionMs in it.startMs..it.endMs }
            ?: lines.lastOrNull { it.startMs <= positionMs }
    }

    // Lyrics provider not connected yet: return nothing instead of placeholder text.
    fun syntheticLyrics(track: Track): List<LyricLine> = emptyList()
}
