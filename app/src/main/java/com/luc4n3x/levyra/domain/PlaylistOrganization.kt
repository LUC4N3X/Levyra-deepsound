package com.luc4n3x.levyra.domain

import java.text.Normalizer
import java.util.Locale

private val TAG_COMBINING_MARKS = Regex("""\p{M}+""")
private val TAG_WHITESPACE = Regex("""\s+""")

const val PLAYLIST_TAG_MAX_LENGTH = 24
const val PLAYLIST_TAG_MAX_PER_PLAYLIST = 8

data class PlaylistTag(
    val id: String,
    val name: String,
    val normalizedName: String,
    val createdAt: Long
)

fun sanitizePlaylistTagName(value: String): String =
    value.replace(TAG_WHITESPACE, " ").trim().take(PLAYLIST_TAG_MAX_LENGTH)

fun normalizePlaylistTagName(value: String): String {
    val sanitized = sanitizePlaylistTagName(value)
    if (sanitized.isEmpty()) return ""
    return Normalizer.normalize(sanitized, Normalizer.Form.NFKD)
        .replace(TAG_COMBINING_MARKS, "")
        .lowercase(Locale.ROOT)
        .replace(TAG_WHITESPACE, " ")
        .trim()
}

fun isValidPlaylistTagName(value: String): Boolean = normalizePlaylistTagName(value).isNotEmpty()

fun List<Playlist>.visibleInLibrary(): List<Playlist> = filterNot { it.hidden }

fun List<Playlist>.hiddenInLibrary(): List<Playlist> = filter { it.hidden }

fun List<Playlist>.filterByTagIds(selectedTagIds: Set<String>): List<Playlist> {
    if (selectedTagIds.isEmpty()) return this
    return filter { playlist ->
        val owned = playlist.tags.mapTo(hashSetOf()) { it.id }
        selectedTagIds.all { it in owned }
    }
}

fun playlistTagsInUse(playlists: List<Playlist>): List<PlaylistTag> =
    playlists.asSequence()
        .flatMap { it.tags.asSequence() }
        .distinctBy { it.id }
        .sortedBy { it.normalizedName }
        .toList()

fun mergePlaylistTagSelection(current: List<PlaylistTag>, tag: PlaylistTag): List<PlaylistTag> {
    if (current.any { it.id == tag.id }) return current.filterNot { it.id == tag.id }
    if (current.size >= PLAYLIST_TAG_MAX_PER_PLAYLIST) return current
    return (current + tag).sortedBy { it.normalizedName }
}
