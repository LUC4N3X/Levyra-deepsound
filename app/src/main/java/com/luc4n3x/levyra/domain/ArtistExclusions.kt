package com.luc4n3x.levyra.domain

private val EXCLUSION_ARTIST_SEPARATOR =
    Regex("""(?i)\s+(?:feat(?:uring)?\.?|ft\.?|with|con|vs\.?|&|and|e|y|et|und)\s+|,\s*|;\s*|\s+[x×/]\s+""")

data class ExcludedArtist(
    val browseId: String,
    val name: String,
    val excludedAt: Long
) {
    val key: String
        get() = excludedArtistKeyOf(browseId, name)
}

data class ArtistExclusions(
    val browseIds: Set<String> = emptySet(),
    val nameKeys: Set<String> = emptySet()
) {
    val isEmpty: Boolean get() = browseIds.isEmpty() && nameKeys.isEmpty()

    fun excludesBrowseId(browseId: String): Boolean {
        if (browseIds.isEmpty()) return false
        val clean = browseId.trim()
        return clean.isNotEmpty() && clean in browseIds
    }

    fun excludesArtistName(artist: String): Boolean {
        if (nameKeys.isEmpty()) return false
        val clean = artist.trim()
        if (clean.isEmpty()) return false
        if (artistIdentityKeys(clean).any(nameKeys::contains)) return true
        return EXCLUSION_ARTIST_SEPARATOR.split(clean).any { segment ->
            val part = segment.trim()
            part.isNotEmpty() && artistIdentityKeys(part).any(nameKeys::contains)
        }
    }

    fun excludesTrack(track: Track): Boolean {
        if (isEmpty) return false
        if (track.artistBrowseIds.any(::excludesBrowseId)) return true
        return excludesArtistName(track.artist)
    }

    fun excludesArtistHit(artist: ArtistHit): Boolean {
        if (isEmpty) return false
        return excludesBrowseId(artist.browseId) || excludesArtistName(artist.name)
    }

    fun filterTracks(tracks: List<Track>): List<Track> {
        if (isEmpty) return tracks
        return keepSourceWhenUnchanged(tracks, tracks.filterNot(::excludesTrack))
    }

    fun filterArtistHits(artists: List<ArtistHit>): List<ArtistHit> {
        if (isEmpty) return artists
        return keepSourceWhenUnchanged(artists, artists.filterNot(::excludesArtistHit))
    }

    fun filterReleaseRadar(entries: List<ReleaseRadarEntry>): List<ReleaseRadarEntry> {
        if (isEmpty) return entries
        val kept = entries.filterNot { entry ->
            excludesBrowseId(entry.artistBrowseId) || excludesArtistName(entry.artistName)
        }
        return keepSourceWhenUnchanged(entries, kept)
    }

    fun filterHomeSections(sections: List<HomeSection>): List<HomeSection> {
        if (isEmpty) return sections
        var changed = false
        val kept = sections.mapNotNull { section ->
            val keptTracks = filterTracks(section.tracks)
            when {
                keptTracks.size == section.tracks.size -> section
                keptTracks.isEmpty() -> {
                    changed = true
                    null
                }
                else -> {
                    changed = true
                    section.copy(tracks = keptTracks)
                }
            }
        }
        return if (changed) kept else sections
    }

    fun filterAlbumHits(albums: List<AlbumHit>): List<AlbumHit> {
        if (isEmpty) return albums
        val kept = albums.filterNot { album ->
            excludesBrowseId(album.artistBrowseId) || excludesArtistName(album.artist)
        }
        return keepSourceWhenUnchanged(albums, kept)
    }

    companion object {
        val Empty = ArtistExclusions()

        fun from(excluded: Collection<ExcludedArtist>): ArtistExclusions {
            if (excluded.isEmpty()) return Empty
            val browseIds = HashSet<String>(excluded.size)
            val nameKeys = HashSet<String>(excluded.size * 2)
            excluded.forEach { artist ->
                artist.browseId.trim().takeIf(String::isNotEmpty)?.let(browseIds::add)
                nameKeys += artistIdentityKeys(artist.name)
            }
            return ArtistExclusions(browseIds = browseIds, nameKeys = nameKeys)
        }
    }
}

private fun <T> keepSourceWhenUnchanged(source: List<T>, filtered: List<T>): List<T> =
    if (filtered.size == source.size) source else filtered

fun excludedArtistKeyOf(browseId: String, name: String): String =
    browseId.trim().ifBlank { "name:${artistIdentityKey(name)}" }

fun isExcludableArtist(browseId: String, name: String): Boolean =
    browseId.trim().isNotEmpty() || artistIdentityKey(name).length >= 2
