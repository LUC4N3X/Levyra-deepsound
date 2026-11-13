package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.data.apple.AppleCatalogItem
import com.luc4n3x.levyra.data.apple.AppleCatalogItemKind
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.viewmodel.ExploreGenreDiscoveryState
import java.text.Normalizer
import java.util.Locale

internal fun dedupeGenreDiscovery(
    editorial: ExploreGenreEditorial,
    discovery: ExploreGenreDiscoveryState
): ExploreGenreDiscoveryState {
    val seenArtists = HashSet<String>()
    val seenAlbums = HashSet<String>()
    val seenItems = HashSet<String>()
    fun acceptArtist(name: String): Boolean {
        val names = genreArtistNames(name)
        if (names.isEmpty() || names.any { it in seenArtists }) return false
        seenArtists += names
        return true
    }
    fun acceptAlbum(title: String, artist: String): Boolean = seenAlbums.add(genreAlbumKey(title, artist))

    editorial.artists.forEach { seenArtists += genreArtistNames(it.name) }
    editorial.albums.forEach { seenAlbums += genreAlbumKey(it.title, it.artist) }

    val lastFm = discovery.lastFm?.let { source ->
        source.copy(
            artists = source.artists.filter(::acceptArtist),
            albums = source.albums.filter { album -> acceptAlbum(album.title, album.artist) }
        )
    }
    val apple = discovery.apple.mapNotNull { shelf ->
        val items = shelf.items.filter { item ->
            when (item.kind) {
                AppleCatalogItemKind.Artist -> acceptArtist(item.title)
                AppleCatalogItemKind.Album -> acceptAlbum(item.title, item.subtitle)
                else -> seenItems.add(appleItemKey(item))
            }
        }
        if (items.isEmpty()) null else shelf.copy(items = items)
    }
    return discovery.copy(apple = apple, lastFm = lastFm)
}

internal fun genreArtistNames(name: String): List<String> =
    ArtistSeparators.split(name).map(::normalizeGenreName).filter(String::isNotBlank).distinct()

internal fun genreAlbumKey(title: String, artist: String): String =
    normalizeGenreName(title) + "|" + genreArtistNames(artist).firstOrNull().orEmpty()

private fun appleItemKey(item: AppleCatalogItem): String = "${item.kind}:${item.id}"

private fun normalizeGenreName(value: String): String =
    Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Marks, "")
        .replace(NonAlphanumeric, " ")
        .trim()
        .replace(Spaces, " ")

private val ArtistSeparators = Regex("\\s*,\\s*|\\s+(?:feat\\.?|ft\\.?|featuring)\\s+|\\s*\\((?:feat\\.?|ft\\.?|featuring)\\s+", RegexOption.IGNORE_CASE)
private val Marks = Regex("\\p{M}+")
private val NonAlphanumeric = Regex("[^\\p{L}\\p{N}]+")
private val Spaces = Regex("\\s+")

internal data class GenreArtistEntry(val key: String, val name: String, val fallbackArtworkUrl: String, val track: Track?)

internal data class GenreAlbumEntry(val key: String, val title: String, val artist: String, val artworkUrl: String, val track: Track?)

internal fun genreArtistEntries(editorial: ExploreGenreEditorial, discovery: ExploreGenreDiscoveryState): List<GenreArtistEntry> =
    editorial.artists.map { card ->
        GenreArtistEntry("ytm:${card.key}", card.name, card.artworkUrl.ifBlank { genreTrackCover(card.track) }, card.track)
    } + discovery.lastFm?.artists.orEmpty().map { name -> GenreArtistEntry("lastfm:$name", name, "", null) }

internal fun genreAlbumEntries(editorial: ExploreGenreEditorial, discovery: ExploreGenreDiscoveryState): List<GenreAlbumEntry> =
    editorial.albums.map { card ->
        GenreAlbumEntry("ytm:${card.key}", card.title, card.artist, card.artworkUrl.ifBlank { genreTrackCover(card.track) }, card.track)
    } + discovery.lastFm?.albums.orEmpty().map { album ->
        GenreAlbumEntry("lastfm:${album.artist}|${album.title}", album.title, album.artist, album.imageUrl, null)
    }

private fun genreTrackCover(track: Track): String = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }

internal val ExploreGenreDiscoveryState.hasVisibleContent: Boolean
    get() = apple.isNotEmpty() || lastFm?.isEmpty == false
