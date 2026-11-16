package com.luc4n3x.levyra.domain

import java.util.Locale

private const val ALBUM_SUGGESTION_LIMIT = 15

private fun AlbumHit.nameKey(): String =
    "${title.trim()}\u0000${artist.trim()}".lowercase(Locale.ROOT)

private fun AlbumHit.sameRelease(other: AlbumHit): Boolean =
    (browseId.isNotBlank() && browseId == other.browseId) ||
        (title.trim().equals(other.title.trim(), ignoreCase = true) && artist.trim().equals(other.artist.trim(), ignoreCase = true))

fun buildAlbumSuggestionShelves(
    current: AlbumHit,
    moreByTitle: String,
    artistReleases: List<AlbumHit>,
    youtubeShelves: List<AlbumSuggestionShelf>,
    similarTitle: String,
    similarAlbums: List<AlbumHit>
): List<AlbumSuggestionShelf> {
    val shown = HashSet<String>()
    fun shelf(title: String, albums: List<AlbumHit>): AlbumSuggestionShelf? {
        if (title.isBlank()) return null
        val unique = albums.asSequence()
            .filter { it.title.isNotBlank() && it.thumbnailUrl.isNotBlank() && !it.sameRelease(current) }
            .filter { album ->
                val idNew = album.browseId.isBlank() || album.browseId !in shown
                val nameNew = album.nameKey() !in shown
                if (idNew && nameNew) {
                    if (album.browseId.isNotBlank()) shown += album.browseId
                    shown += album.nameKey()
                }
                idNew && nameNew
            }
            .take(ALBUM_SUGGESTION_LIMIT)
            .toList()
        return if (unique.isEmpty()) null else AlbumSuggestionShelf(title, unique)
    }
    val currentArtist = current.artist.trim()
    return listOfNotNull(shelf(moreByTitle, artistReleases)) +
        youtubeShelves.mapNotNull { shelf(it.title, it.albums) } +
        listOfNotNull(shelf(similarTitle, similarAlbums.filterNot { it.artist.trim().equals(currentArtist, ignoreCase = true) }))
}
