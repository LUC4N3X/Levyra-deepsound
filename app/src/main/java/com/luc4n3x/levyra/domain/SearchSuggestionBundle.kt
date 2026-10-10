package com.luc4n3x.levyra.domain

data class SearchSuggestionBundle(
    val queries: List<String> = emptyList(),
    val songs: List<Track> = emptyList(),
    val videos: List<Track> = emptyList(),
    val artists: List<ArtistHit> = emptyList(),
    val albums: List<AlbumHit> = emptyList()
) {
    val hasRichItems: Boolean
        get() = songs.isNotEmpty() || videos.isNotEmpty() || artists.isNotEmpty() || albums.isNotEmpty()

    val isEmpty: Boolean
        get() = queries.isEmpty() && !hasRichItems
}
