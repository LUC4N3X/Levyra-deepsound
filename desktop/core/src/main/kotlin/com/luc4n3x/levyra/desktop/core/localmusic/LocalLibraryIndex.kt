package com.luc4n3x.levyra.desktop.core.localmusic

import java.util.Locale

class LocalLibraryIndex private constructor(
    val tracks: List<LocalTrack>,
    private val entries: List<Entry>
) {

    class Entry(
        val track: LocalTrack,
        val title: String,
        val artist: String,
        val album: String
    )

    val albums: List<LocalAlbum> by lazy { buildAlbums() }
    val artists: List<LocalArtist> by lazy { buildArtists() }

    fun search(query: String, limit: Int = DEFAULT_SEARCH_LIMIT): List<LocalTrack> {
        val terms = query.lowercase(Locale.ROOT).split(' ').filter { it.isNotBlank() }
        if (terms.isEmpty()) return emptyList()
        val matches = ArrayList<Pair<Int, LocalTrack>>()
        entries.forEach { entry ->
            val score = score(entry, terms)
            if (score > 0) matches.add(score to entry.track)
        }
        return matches
            .sortedWith(compareByDescending<Pair<Int, LocalTrack>> { it.first }.thenBy { it.second.title })
            .take(limit)
            .map { it.second }
    }

    fun albumTracks(key: String): List<LocalTrack> = tracks.filter {
        LocalMusicIdentity.albumKey(it.album, it.effectiveAlbumArtist) == key
    }

    fun artistTracks(key: String): List<LocalTrack> = tracks.filter {
        LocalMusicIdentity.normalizeKey(it.effectiveAlbumArtist) == key ||
            LocalMusicIdentity.normalizeKey(it.artist) == key
    }

    private fun score(entry: Entry, terms: List<String>): Int {
        var total = 0
        terms.forEach { term ->
            val termScore = when {
                entry.title.startsWith(term) -> 6
                entry.artist.startsWith(term) -> 5
                entry.album.startsWith(term) -> 4
                entry.title.contains(term) -> 3
                entry.artist.contains(term) -> 2
                entry.album.contains(term) -> 1
                else -> return 0
            }
            total += termScore
        }
        return total
    }

    private fun buildAlbums(): List<LocalAlbum> {
        val grouped = LinkedHashMap<String, MutableList<LocalTrack>>()
        tracks.forEach { track ->
            if (track.album.isBlank()) return@forEach
            val key = LocalMusicIdentity.albumKey(track.album, track.effectiveAlbumArtist)
            grouped.getOrPut(key) { ArrayList() }.add(track)
        }
        return grouped.map { (key, items) ->
            val reference = items.first()
            LocalAlbum(
                key = key,
                title = reference.album,
                albumArtist = reference.effectiveAlbumArtist,
                year = items.maxOf { it.year },
                artworkPath = items.firstOrNull { it.artworkPath.isNotBlank() }?.artworkPath.orEmpty(),
                trackCount = items.size,
                durationMs = items.sumOf { it.durationMs }
            )
        }.sortedWith(
            compareBy(
                { it.albumArtist.lowercase(Locale.ROOT) },
                { it.title.lowercase(Locale.ROOT) }
            )
        )
    }

    private fun buildArtists(): List<LocalArtist> {
        val grouped = LinkedHashMap<String, MutableList<LocalTrack>>()
        tracks.forEach { track ->
            val name = track.effectiveAlbumArtist
            if (name.isBlank()) return@forEach
            grouped.getOrPut(LocalMusicIdentity.normalizeKey(name)) { ArrayList() }.add(track)
        }
        return grouped.map { (key, items) ->
            LocalArtist(
                key = key,
                name = items.first().effectiveAlbumArtist,
                artworkPath = items.firstOrNull { it.artworkPath.isNotBlank() }?.artworkPath.orEmpty(),
                trackCount = items.size,
                albumCount = items.mapTo(HashSet()) { LocalMusicIdentity.normalizeKey(it.album) }
                    .count { it.isNotBlank() }
            )
        }.sortedBy { it.name.lowercase(Locale.ROOT) }
    }

    companion object {
        const val DEFAULT_SEARCH_LIMIT = 120

        val EMPTY = LocalLibraryIndex(emptyList(), emptyList())

        fun of(tracks: List<LocalTrack>): LocalLibraryIndex {
            val available = tracks.filter { it.available }
            if (available.isEmpty()) return EMPTY
            return LocalLibraryIndex(
                tracks = available,
                entries = available.map { track ->
                    Entry(
                        track = track,
                        title = track.title.lowercase(Locale.ROOT),
                        artist = track.artist.lowercase(Locale.ROOT),
                        album = track.album.lowercase(Locale.ROOT)
                    )
                }
            )
        }
    }
}
