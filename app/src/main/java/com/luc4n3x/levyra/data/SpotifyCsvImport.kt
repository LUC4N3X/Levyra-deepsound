package com.luc4n3x.levyra.data

import java.util.Locale

data class SpotifyCsvEntry(
    val title: String,
    val artist: String,
    val durationMs: Long
)

internal const val MAX_SPOTIFY_CSV_ROWS = 2_000
internal const val UTF8_BOM = "\uFEFF"

private val TITLE_HEADERS = listOf(
    "track name",
    "track title",
    "song name",
    "song title",
    "song",
    "title",
    "name",
    "track"
)

private val ARTIST_HEADERS = listOf(
    "artist name(s)",
    "artist name",
    "artist names",
    "artist(s)",
    "artists",
    "artist",
    "album artist"
)

private val DURATION_HEADERS = listOf(
    "track duration (ms)",
    "duration (ms)",
    "duration_ms",
    "durationms",
    "duration ms",
    "length (ms)"
)

private val ARTIST_SEPARATORS = Regex("""\s*(?:,|;|&|\||\bfeat\.?\b|\bft\.?\b|\bwith\b|\bx\b)\s*""", RegexOption.IGNORE_CASE)

internal fun parseCsvRows(text: String, maxRows: Int = MAX_SPOTIFY_CSV_ROWS): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    var row = mutableListOf<String>()
    val field = StringBuilder()
    var quoted = false
    var index = 0
    var sawField = false
    val source = text.removePrefix(UTF8_BOM)

    fun endField() {
        row.add(field.toString())
        field.setLength(0)
        sawField = false
    }

    fun endRow() {
        endField()
        if (row.any { it.isNotBlank() }) rows.add(row.toList())
        row = mutableListOf()
    }

    while (index < source.length && rows.size < maxRows) {
        val char = source[index]
        when {
            quoted && char == '"' -> {
                if (index + 1 < source.length && source[index + 1] == '"') {
                    field.append('"')
                    index++
                } else {
                    quoted = false
                }
            }
            quoted -> field.append(char)
            char == '"' && !sawField -> {
                quoted = true
                sawField = true
            }
            char == ',' -> endField()
            char == '\r' -> {
                if (index + 1 < source.length && source[index + 1] == '\n') index++
                endRow()
            }
            char == '\n' -> endRow()
            else -> {
                field.append(char)
                sawField = true
            }
        }
        index++
    }
    if (field.isNotEmpty() || row.isNotEmpty()) endRow()
    return rows
}

internal data class SpotifyCsvColumns(
    val titleIndex: Int,
    val artistIndex: Int,
    val durationIndex: Int
)

internal fun detectSpotifyCsvColumns(header: List<String>): SpotifyCsvColumns? {
    val normalized = header.map { it.trim().lowercase(Locale.ROOT).removeSurrounding("\"").trim() }
    val titleIndex = indexOfHeader(normalized, TITLE_HEADERS)
    if (titleIndex < 0) return null
    val artistIndex = indexOfHeader(normalized, ARTIST_HEADERS)
    val durationIndex = indexOfHeader(normalized, DURATION_HEADERS)
    return SpotifyCsvColumns(titleIndex, artistIndex, durationIndex)
}

private fun indexOfHeader(normalized: List<String>, candidates: List<String>): Int {
    candidates.forEach { candidate ->
        val exact = normalized.indexOf(candidate)
        if (exact >= 0) return exact
    }
    return -1
}

fun parseSpotifyCsv(text: String, maxRows: Int = MAX_SPOTIFY_CSV_ROWS): List<SpotifyCsvEntry> {
    val rows = parseCsvRows(text, maxRows + 1)
    if (rows.isEmpty()) return emptyList()
    val columns = detectSpotifyCsvColumns(rows.first()) ?: return emptyList()
    return rows.asSequence()
        .drop(1)
        .mapNotNull { row -> spotifyCsvEntry(row, columns) }
        .take(maxRows)
        .toList()
}

private fun spotifyCsvEntry(row: List<String>, columns: SpotifyCsvColumns): SpotifyCsvEntry? {
    val title = row.getOrNull(columns.titleIndex)?.trim().orEmpty()
    if (title.isBlank()) return null
    val artist = row.getOrNull(columns.artistIndex)?.trim().orEmpty()
    val durationMs = row.getOrNull(columns.durationIndex)
        ?.trim()
        ?.toLongOrNull()
        ?.takeIf { it in 1L..(6L * 60L * 60L * 1_000L) }
        ?: 0L
    return SpotifyCsvEntry(title, normalizeCsvArtist(artist), durationMs)
}

internal fun normalizeCsvArtist(value: String): String {
    val cleaned = value.trim().removeSurrounding("\"").trim()
    if (cleaned.isBlank()) return ""
    return ARTIST_SEPARATORS.split(cleaned)
        .map { it.trim() }
        .firstOrNull { it.isNotBlank() }
        .orEmpty()
}

internal fun spotifyCsvEntryLabel(entry: SpotifyCsvEntry): String =
    if (entry.artist.isBlank()) entry.title else "${entry.title} — ${entry.artist}"
