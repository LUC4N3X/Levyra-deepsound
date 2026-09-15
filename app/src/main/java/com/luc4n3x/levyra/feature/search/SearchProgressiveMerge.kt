package com.luc4n3x.levyra.feature.search

import com.luc4n3x.levyra.data.albumRecommendationTextKey
import com.luc4n3x.levyra.data.mergeSearchAlbums
import com.luc4n3x.levyra.data.mergeSearchArtists
import com.luc4n3x.levyra.data.mergeSearchPlaylists
import com.luc4n3x.levyra.data.mergeSearchSongs
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.ArtistProfile
import com.luc4n3x.levyra.domain.SearchFilter
import com.luc4n3x.levyra.domain.SearchResults
import com.luc4n3x.levyra.domain.SearchSuggestionBundle
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.artistIdentityKey
import com.luc4n3x.levyra.viewmodel.enrichSearchTracksWithExactArtist
import com.luc4n3x.levyra.viewmodel.mergeReliableArtistSearchResults
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal suspend inline fun <T> searchCatching(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (error: Throwable) {
    currentCoroutineContext().ensureActive()
    Result.failure(error)
}

internal fun provisionalSearchArtists(query: String, artists: List<ArtistHit>): List<ArtistHit> {
    if (artists.isEmpty()) return artists
    return mergeReliableArtistSearchResults(query = query, exactArtist = null, verifiedArtists = artists)
}

internal fun mergeFastSearchResults(
    existing: SearchResults,
    incoming: SearchResults,
    query: String
): SearchResults = SearchResults(
    topTrack = existing.topTrack ?: incoming.topTrack,
    songs = mergeSearchSongs(existing.songs, incoming.songs),
    videos = mergeSearchSongs(existing.videos, incoming.videos),
    artists = provisionalSearchArtists(query, mergeSearchArtists(existing.artists, incoming.artists)),
    albums = mergeSearchAlbums(existing.albums, incoming.albums),
    playlists = mergeSearchPlaylists(existing.playlists, incoming.playlists),
    failedSections = existing.failedSections + incoming.failedSections
)

internal fun mergeRichSuggestionResults(
    base: SearchResults,
    bundle: SearchSuggestionBundle,
    query: String
): SearchResults {
    if (!bundle.hasRichItems) return base
    val incoming = SearchResults(
        topTrack = bundle.songs.firstOrNull() ?: bundle.videos.firstOrNull(),
        songs = bundle.songs,
        videos = bundle.videos,
        artists = bundle.artists,
        albums = bundle.albums
    )
    return mergeFastSearchResults(base, incoming, query)
}

internal fun narrowSuggestionBundle(bundle: SearchSuggestionBundle, query: String): SearchSuggestionBundle {
    val key = searchQueryKey(query)
    if (key.isEmpty()) return SearchSuggestionBundle()
    return SearchSuggestionBundle(queries = bundle.queries.filter { searchQueryKey(it).startsWith(key) })
}

internal fun searchEntityTextKeys(results: SearchResults): Set<String> = buildSet {
    (listOfNotNull(results.topTrack) + results.songs.take(ENTITY_KEY_SONG_LIMIT)).forEach { track ->
        val title = searchQueryKey(track.title)
        if (title.isEmpty()) return@forEach
        add(title)
        val artist = searchQueryKey(track.artist)
        if (artist.isNotEmpty()) {
            add("$title $artist")
            add("$artist $title")
        }
    }
    results.artists.forEach { artist -> searchQueryKey(artist.name).takeIf(String::isNotEmpty)?.let(::add) }
    results.albums.forEach { album -> searchQueryKey(album.title).takeIf(String::isNotEmpty)?.let(::add) }
}

internal fun SearchSessionSnapshot.withSuggestionBundle(
    bundle: SearchSuggestionBundle,
    query: String
): SearchSessionSnapshot {
    val results = if (verified || !bundle.hasRichItems) {
        this.results
    } else {
        mergeRichSuggestionResults(freshResults, bundle, query)
    }
    val stillCarried = carriedOver && (verified || !bundle.hasRichItems)
    val shownResults = if (stillCarried) SearchResults() else results
    return copy(
        results = results,
        carriedOver = stillCarried,
        suggestions = mergeSearchQuerySuggestions(query, bundle.queries, searchEntityTextKeys(shownResults))
    )
}

internal fun SearchSessionSnapshot.withLocalMatches(matches: List<LocalSearchMatch>): SearchSessionSnapshot {
    if (matches.isEmpty()) return this
    val local = matches.map(LocalSearchMatch::track)
    val base = freshResults
    val songs = if (base.songs.isEmpty()) mergeSearchSongs(local, emptyList()) else mergeSearchSongs(base.songs, local)
    val strongTop = matches.first().takeIf(LocalSearchMatch::strong)?.track
    val lockTop = !topLocked && strongTop != null
    return copy(
        results = base.copy(topTrack = if (lockTop) strongTop else base.topTrack, songs = songs),
        carriedOver = false,
        topLocked = topLocked || lockTop,
        failure = SearchFailure.None
    )
}

internal fun SearchSessionSnapshot.withOverview(raw: SearchResults, query: String): SearchSessionSnapshot {
    val base = freshResults
    val merged = mergeFastSearchResults(base, raw, query)
    val top = if (topLocked) base.topTrack ?: raw.topTrack else raw.topTrack ?: base.topTrack
    return copy(
        results = merged.copy(topTrack = top),
        carriedOver = false,
        topLocked = topLocked || top != null
    )
}

internal fun SearchSessionSnapshot.withVerifiedArtists(
    query: String,
    artists: List<ArtistHit>,
    artistsFailed: Boolean
): SearchSessionSnapshot {
    val base = freshResults
    val displayedArtists = if (SearchFilter.Artists in sectionContinuations) {
        mergeSearchArtists(artists, base.artists)
    } else {
        artists
    }
    val enriched = enrichSearchTracksWithExactArtist(query, base, artists)
    val failed = if (artistsFailed) base.failedSections + SearchFilter.Artists else base.failedSections
    return copy(
        results = enriched.copy(artists = displayedArtists, failedSections = failed),
        carriedOver = false
    )
}

internal fun SearchSessionSnapshot.withSectionPage(
    filter: SearchFilter,
    page: SearchSectionPage,
    nextContinuation: String
): SearchSessionSnapshot {
    val continuations = sectionContinuations + (filter to nextContinuation)
    if (carriedOver && page.isEmpty) {
        return copy(sectionContinuations = continuations, pendingSectionFailures = pendingSectionFailures - filter)
    }
    val merged = mergeSectionPage(freshResults, filter, page)
    return copy(
        results = merged.copy(failedSections = merged.failedSections - filter),
        carriedOver = false,
        failure = if (merged.isEmpty) failure else SearchFailure.None,
        sectionContinuations = continuations
    )
}

private fun mergeSectionPage(base: SearchResults, filter: SearchFilter, page: SearchSectionPage): SearchResults =
    when (filter) {
        SearchFilter.Songs -> base.copy(
            topTrack = base.topTrack ?: page.songs.firstOrNull(),
            songs = mergeSearchSongs(base.songs, page.songs)
        )
        SearchFilter.Videos -> base.copy(videos = mergeSearchSongs(base.videos, page.videos))
        SearchFilter.Albums -> base.copy(albums = mergeSearchAlbums(base.albums, page.albums))
        SearchFilter.Artists -> base.copy(artists = mergeSearchArtists(base.artists, page.artists))
        SearchFilter.Playlists -> base.copy(playlists = mergeSearchPlaylists(base.playlists, page.playlists))
        SearchFilter.All -> base
    }

internal fun SearchSessionSnapshot.withFailedSection(filter: SearchFilter): SearchSessionSnapshot =
    if (carriedOver) {
        copy(pendingSectionFailures = pendingSectionFailures + filter)
    } else {
        copy(results = results.copy(failedSections = results.failedSections + filter))
    }

internal sealed interface SearchAlbumRefinement {
    data object Keep : SearchAlbumRefinement
    data object DropSongTitleEchoes : SearchAlbumRefinement
    data class Official(val albums: List<AlbumHit>) : SearchAlbumRefinement
}

internal suspend fun refineSearchAlbums(
    query: String,
    artists: List<ArtistHit>,
    officialAlbums: suspend (ArtistHit) -> List<AlbumHit>
): SearchAlbumRefinement {
    val queryKey = artistIdentityKey(query)
    val exactArtist = artists.firstOrNull { artistIdentityKey(it.name) == queryKey }
        ?: return SearchAlbumRefinement.Keep
    val official = searchCatching { officialAlbums(exactArtist) }
        .getOrDefault(emptyList())
        .asSequence()
        .filter { album -> album.title.isNotBlank() && album.browseId.isNotBlank() }
        .distinctBy(::searchAlbumTextKey)
        .take(SEARCH_ALBUM_LIMIT)
        .toList()
    return if (official.isNotEmpty()) SearchAlbumRefinement.Official(official) else SearchAlbumRefinement.DropSongTitleEchoes
}

internal fun SearchSessionSnapshot.withAlbumRefinement(refinement: SearchAlbumRefinement): SearchSessionSnapshot {
    val base = freshResults
    val albums = when (refinement) {
        SearchAlbumRefinement.Keep -> base.albums
        SearchAlbumRefinement.DropSongTitleEchoes -> albumsWithoutSongTitleEchoes(base.albums, base.songs)
        is SearchAlbumRefinement.Official -> if (SearchFilter.Albums in sectionContinuations) {
            mergeSearchAlbums(refinement.albums, base.albums)
        } else {
            refinement.albums
        }
    }
    return copy(results = base.copy(albums = albums), carriedOver = false, verified = true)
}

internal fun albumsWithoutSongTitleEchoes(albums: List<AlbumHit>, songs: List<Track>): List<AlbumHit> {
    val songTitles = songs.asSequence()
        .map { track -> albumRecommendationTextKey(track.title) }
        .filter(String::isNotBlank)
        .toSet()
    return albums
        .filterNot { album -> albumRecommendationTextKey(album.title) in songTitles }
        .distinctBy(::searchAlbumTextKey)
        .take(SEARCH_ALBUM_LIMIT)
}

internal fun searchAlbumTextKey(album: AlbumHit): String =
    "${albumRecommendationTextKey(album.title)}|${albumRecommendationTextKey(album.artist)}"

internal fun officialSearchAlbums(profile: ArtistProfile?, artist: ArtistHit): List<AlbumHit> {
    profile ?: return emptyList()
    val artistName = profile.name.ifBlank { artist.name }
    val artistBrowseId = profile.browseId.ifBlank { artist.browseId }
    return profile.albums
        .asSequence()
        .filter { release -> release.title.isNotBlank() && release.browseId.isNotBlank() }
        .map { release ->
            AlbumHit(
                title = release.title,
                artist = artistName,
                year = release.year,
                thumbnailUrl = release.thumbnailUrl,
                query = listOf(release.title, artistName, "album").filter(String::isNotBlank).joinToString(" "),
                browseId = release.browseId,
                artistBrowseId = artistBrowseId,
                audioPlaylistId = release.playlistId,
                explicit = release.explicit,
                releaseType = release.releaseType
            )
        }
        .toList()
}

private const val ENTITY_KEY_SONG_LIMIT = 8
internal const val SEARCH_ALBUM_LIMIT = 10
