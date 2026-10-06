package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.data.spotify.MatchedYouTubeData
import com.luc4n3x.levyra.data.spotify.SpotifyYouTubeMatchCache
import com.luc4n3x.levyra.data.spotify.SpotifyYouTubeMatcher
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.ReleaseType
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.artistIdentityKey
import com.luc4n3x.levyra.domain.artistIdentityMatches
import com.luc4n3x.levyra.domain.primaryArtistSegment
import java.util.Locale

internal fun searchSongIdentityKey(track: Track): String =
    track.id.trim().lowercase(Locale.ROOT)

internal fun searchSongMetadataKey(track: Track): String {
    val title = albumRecommendationTextKey(track.title)
    val artist = artistIdentityKey(primaryArtistSegment(track.artist))
    val durationSeconds = track.durationMs / 1_000L
    if (title.isBlank() || artist.isBlank() || durationSeconds <= 0L) return ""
    return "recording:$title|$artist|$durationSeconds"
}

private fun searchSongLooseMetadataKey(track: Track): String {
    val title = albumRecommendationTextKey(track.title)
    val artist = artistIdentityKey(primaryArtistSegment(track.artist))
    if (title.isBlank() || artist.isBlank()) return ""
    return "recording:$title|$artist"
}

internal fun searchAlbumCanonicalKey(album: AlbumHit): String {
    val browseId = album.browseId.trim().lowercase(Locale.ROOT)
    if (browseId.isNotBlank()) return "browse:$browseId"
    val playlistId = album.audioPlaylistId.trim().lowercase(Locale.ROOT)
    if (playlistId.isNotBlank()) return "playlist:$playlistId"
    val upc = album.upc.filter(Char::isLetterOrDigit).lowercase(Locale.ROOT)
    if (upc.isNotBlank()) return "upc:$upc"
    return ""
}

internal fun searchAlbumMetadataKey(album: AlbumHit): String = homeAlbumDeduplicationKey(album)

internal fun searchArtistCanonicalKey(artist: ArtistHit): String {
    val browseId = artist.browseId.trim().lowercase(Locale.ROOT)
    return if (browseId.isBlank()) "" else "browse:$browseId"
}

internal fun searchArtistMetadataKey(artist: ArtistHit): String {
    val primary = artistIdentityKey(primaryArtistSegment(artist.name))
    return if (primary.isBlank()) "" else "artist:$primary"
}

internal fun searchPlaylistCanonicalKey(playlist: PlaylistHit): String {
    val playlistId = playlist.playlistId.trim().lowercase(Locale.ROOT)
    if (playlistId.isNotBlank()) return "playlist:$playlistId"
    val browseId = playlist.browseId.trim().lowercase(Locale.ROOT)
    return if (browseId.isBlank()) "" else "browse:$browseId"
}

internal fun searchPlaylistMetadataKey(playlist: PlaylistHit): String {
    val title = albumRecommendationTextKey(playlist.title)
    if (title.isBlank()) return ""
    val author = albumRecommendationTextKey(primaryArtistSegment(playlist.author))
    return "playlist:$title|$author"
}

internal fun nextSearchContinuation(requested: String, returned: String): String {
    val cleanReturned = returned.trim()
    if (cleanReturned.isBlank()) return ""
    return if (cleanReturned == requested.trim()) "" else cleanReturned
}

internal fun isMusicVideoResult(videoType: String): Boolean {
    val normalized = videoType.trim().uppercase(Locale.ROOT)
    if (normalized.isBlank()) return false
    return normalized != MUSIC_VIDEO_TYPE_AUDIO
}

internal fun mergeSearchSongs(existing: List<Track>, incoming: List<Track>): List<Track> {
    val hasSpotify = existing.any(::isSpotifyTrack) || incoming.any(::isSpotifyTrack)
    if (!hasSpotify) {
        val strict = mergeSearchEntities(
            existing,
            incoming,
            ::searchSongIdentityKey,
            ::searchSongMetadataKey,
            ::richerSong
        )
        return mergeComplementarySearchSongMetadata(strict)
    }

    return mergeSpotifyAndYoutubeSongs(existing, incoming)
}

private fun isSpotifyTrack(track: Track): Boolean =
    track.metadataProvider.equals("spotify", ignoreCase = true) ||
        track.id.startsWith("spotify:", ignoreCase = true) ||
        track.source.equals("spotify", ignoreCase = true)

private fun isPlayableYoutubeId(id: String): Boolean =
    id.length == 11 && !id.startsWith("spotify:") && id.all { it.isLetterOrDigit() || it == '-' || it == '_' }

internal fun mergeSpotifyAndYoutubeSongs(existing: List<Track>, incoming: List<Track>): List<Track> {
    val existingSpotify = existing.filter(::isSpotifyTrack)
    val incomingSpotify = incoming.filter(::isSpotifyTrack)
    val existingYt = existing.filterNot(::isSpotifyTrack)
    val incomingYt = incoming.filterNot(::isSpotifyTrack)

    val allSpotify = (existingSpotify + incomingSpotify).distinctBy { searchSongIdentityKey(it) }
    val allYt = (existingYt + incomingYt).distinctBy { searchSongIdentityKey(it) }

    val matchCache = SpotifyYouTubeMatchCache.get()
    val availableYt = allYt.toMutableList()
    val consumedYtIds = HashSet<String>()

    val resultSongs = mutableListOf<Track>()

    for (spotifyTrack in allSpotify) {
        val cached = matchCache.get(spotifyTrack.id)
        val matchedFromCandidates = SpotifyYouTubeMatcher.findBestMatch(spotifyTrack, availableYt)

        val enriched = if (matchedFromCandidates != null) {
            val yt = matchedFromCandidates.candidate
            consumedYtIds.add(yt.id)
            availableYt.removeAll { it.id == yt.id }
            matchCache.put(spotifyTrack.id, yt)
            richerSong(spotifyTrack, yt)
        } else if (cached != null) {
            val cachedTrack = availableYt.firstOrNull { it.id == cached.videoId }
            if (cachedTrack != null) {
                consumedYtIds.add(cachedTrack.id)
                availableYt.removeAll { it.id == cachedTrack.id }
                richerSong(spotifyTrack, cachedTrack)
            } else {
                enrichWithCachedData(spotifyTrack, cached)
            }
        } else {
            spotifyTrack
        }
        resultSongs.add(enriched)
    }

    for (ytTrack in allYt) {
        if (ytTrack.id !in consumedYtIds) {
            val isDuplicate = resultSongs.any { existingSong ->
                existingSong.id == ytTrack.id ||
                    existingSong.counterpartVideoId == ytTrack.id ||
                    existingSong.audioVideoId == ytTrack.id ||
                    SpotifyYouTubeMatcher.scoreMatch(existingSong, ytTrack) >= 0.75
            }
            if (!isDuplicate) {
                resultSongs.add(ytTrack)
            }
        }
    }

    return resultSongs
}

private fun enrichWithCachedData(spotifyTrack: Track, cached: MatchedYouTubeData): Track {
    val videoId = cached.videoId
    val videoUrl = cached.videoUrl.ifBlank { "https://www.youtube.com/watch?v=$videoId" }
    return spotifyTrack.copy(
        id = videoId,
        videoUrl = videoUrl,
        counterpartVideoId = videoId,
        audioVideoId = cached.audioVideoId.ifBlank { videoId },
        videoType = cached.videoType.ifBlank { spotifyTrack.videoType },
        source = "spotify_youtube",
        metadataProvider = "spotify",
        youtubeViewCount = maxOf(spotifyTrack.youtubeViewCount, cached.youtubeViewCount)
    )
}

private fun mergeComplementarySearchSongMetadata(songs: List<Track>): List<Track> {
    if (songs.size < 2) return songs

    val merged = songs.toMutableList()
    val removed = HashSet<Int>()
    val indicesByLooseKey = songs.indices.groupBy { index ->
        searchSongLooseMetadataKey(songs[index])
    }

    for ((looseKey, indices) in indicesByLooseKey) {
        if (looseKey.isBlank()) continue
        val knownDurationIndices = indices.filter { index -> songs[index].durationMs > 0L }
        val incompleteDurationIndices = indices.filter { index -> songs[index].durationMs <= 0L }
        if (knownDurationIndices.size != 1 || incompleteDurationIndices.isEmpty()) continue

        val participants = indices.sorted()
        val targetIndex = participants.first()
        var combined = songs[targetIndex]
        for (participantIndex in participants.drop(1)) {
            combined = richerSong(combined, songs[participantIndex])
            removed.add(participantIndex)
        }
        merged[targetIndex] = combined
    }

    if (removed.isEmpty()) return songs
    return merged.filterIndexed { index, _ -> index !in removed }
}

internal fun selectSearchTopResultTracks(
    topTrack: Track?,
    songs: List<Track>,
    limit: Int = 3
): List<Track> {
    val hero = topTrack ?: return emptyList()
    val merged = deduplicateSearchSongs(listOf(hero) + songs)
    val resolvedHero = merged.firstOrNull { it.id == hero.id } ?: hero
    val heroArtist = primaryArtistSegment(resolvedHero.artist).ifBlank { resolvedHero.artist.trim() }
    val heroBrowseIds = resolvedHero.artistBrowseIds
        .map { it.trim().lowercase(Locale.ROOT) }
        .filter(String::isNotBlank)
        .toSet()
    val boundedLimit = limit.coerceIn(1, 3)

    return buildList {
        add(resolvedHero)
        if (heroArtist.isBlank() && heroBrowseIds.isEmpty()) return@buildList
        merged.asSequence()
            .filterNot { it.id == resolvedHero.id }
            .filter { candidate ->
                val candidateBrowseIds = candidate.artistBrowseIds
                    .asSequence()
                    .map { it.trim().lowercase(Locale.ROOT) }
                    .filter(String::isNotBlank)
                    .toSet()
                val sharesBrowseId = heroBrowseIds.isNotEmpty() && candidateBrowseIds.any(heroBrowseIds::contains)
                val candidateArtist = primaryArtistSegment(candidate.artist).ifBlank { candidate.artist.trim() }
                sharesBrowseId || (heroArtist.isNotBlank() && artistIdentityMatches(heroArtist, candidateArtist))
            }
            .take(boundedLimit - 1)
            .forEach(::add)
    }.take(boundedLimit)
}

internal fun filterSearchSongsExcludingTopResult(
    songs: List<Track>,
    topResultTracks: List<Track>
): List<Track> {
    if (songs.isEmpty() || topResultTracks.isEmpty()) return songs

    val topResultIds = topResultTracks
        .mapNotNullTo(HashSet()) { track -> searchSongIdentityKey(track).takeIf(String::isNotBlank) }
    val topResultMetadataKeys = topResultTracks
        .mapNotNullTo(HashSet()) { track -> searchSongMetadataKey(track).takeIf(String::isNotBlank) }

    return songs.filterNot { song ->
        val identity = searchSongIdentityKey(song)
        val metadata = searchSongMetadataKey(song)
        identity in topResultIds ||
            (metadata.isNotBlank() && metadata in topResultMetadataKeys) ||
            topResultTracks.any { topTrack -> areComplementarySearchSongResults(song, topTrack) }
    }
}

private fun areComplementarySearchSongResults(first: Track, second: Track): Boolean {
    val firstHasDuration = first.durationMs > 0L
    val secondHasDuration = second.durationMs > 0L
    if (firstHasDuration == secondHasDuration) return false

    val firstKey = searchSongLooseMetadataKey(first)
    if (firstKey.isBlank()) return false
    return firstKey == searchSongLooseMetadataKey(second)
}

internal fun mergeSearchAlbums(existing: List<AlbumHit>, incoming: List<AlbumHit>): List<AlbumHit> =
    mergeSearchEntities(existing, incoming, ::searchAlbumCanonicalKey, ::searchAlbumMetadataKey, ::richerAlbum)

internal fun mergeSearchArtists(existing: List<ArtistHit>, incoming: List<ArtistHit>): List<ArtistHit> =
    mergeSearchEntities(existing, incoming, ::searchArtistCanonicalKey, ::searchArtistMetadataKey, ::richerArtist)

internal fun mergeSearchPlaylists(existing: List<PlaylistHit>, incoming: List<PlaylistHit>): List<PlaylistHit> =
    mergeSearchEntities(existing, incoming, ::searchPlaylistCanonicalKey, ::searchPlaylistMetadataKey, ::richerPlaylist)

internal fun deduplicateSearchSongs(songs: List<Track>): List<Track> = mergeSearchSongs(emptyList(), songs)

internal fun deduplicateSearchAlbums(albums: List<AlbumHit>): List<AlbumHit> = mergeSearchAlbums(emptyList(), albums)

internal fun deduplicateSearchArtists(artists: List<ArtistHit>): List<ArtistHit> =
    mergeSearchArtists(emptyList(), artists)

internal fun deduplicateSearchPlaylists(playlists: List<PlaylistHit>): List<PlaylistHit> =
    mergeSearchPlaylists(emptyList(), playlists)

private fun <T> mergeSearchEntities(
    existing: List<T>,
    incoming: List<T>,
    canonicalKey: (T) -> String,
    metadataKey: (T) -> String,
    richer: (T, T) -> T
): List<T> {
    if (existing.isEmpty() && incoming.size < 2) return incoming
    val ordered = mutableListOf<T>()
    val slotByKey = HashMap<String, Int>()
    (existing.asSequence() + incoming.asSequence()).forEach { candidate ->
        val keys = entityKeys(candidate, canonicalKey, metadataKey)
        val slot = keys.firstNotNullOfOrNull(slotByKey::get)
        if (slot == null) {
            val index = ordered.size
            ordered.add(candidate)
            keys.forEach { key -> slotByKey[key] = index }
        } else {
            ordered[slot] = richer(ordered[slot], candidate)
            keys.forEach { key -> slotByKey.putIfAbsent(key, slot) }
        }
    }
    return ordered.toList()
}

private fun <T> entityKeys(
    entity: T,
    canonicalKey: (T) -> String,
    metadataKey: (T) -> String
): List<String> {
    val canonical = canonicalKey(entity)
    val metadata = metadataKey(entity)
    return buildList {
        if (canonical.isNotBlank()) add("id$canonical")
        if (metadata.isNotBlank()) add("meta$metadata")
    }
}

private fun selectSongMetadataDonor(current: Track, candidate: Track): Track {
    val currentIsSpotify = isSpotifyTrack(current)
    val candidateIsSpotify = isSpotifyTrack(candidate)
    return when {
        currentIsSpotify && !candidateIsSpotify -> current
        !currentIsSpotify && candidateIsSpotify -> candidate
        else -> current
    }
}

private fun selectSongPlaybackDonor(current: Track, candidate: Track): Track {
    val currentHasYtId = isPlayableYoutubeId(current.id) || current.counterpartVideoId.isNotBlank()
    val candidateHasYtId = isPlayableYoutubeId(candidate.id) || candidate.counterpartVideoId.isNotBlank()
    return when {
        candidateHasYtId && !currentHasYtId -> candidate
        currentHasYtId -> current
        candidate.videoUrl.isNotBlank() && current.videoUrl.isBlank() -> candidate
        else -> current
    }
}

private fun resolveMergedSongId(current: Track, candidate: Track, playbackDonor: Track): String {
    val nonSpotify = sequenceOf(playbackDonor.id, current.id, candidate.id)
        .firstOrNull { it.isNotBlank() && !it.startsWith("spotify:") }
    return nonSpotify ?: current.id.ifBlank { candidate.id }
}

private fun resolveMergedVideoId(mergedId: String, current: Track, candidate: Track, playbackDonor: Track): String {
    if (isPlayableYoutubeId(mergedId)) return mergedId
    return sequenceOf(playbackDonor.counterpartVideoId, current.counterpartVideoId, candidate.counterpartVideoId)
        .firstOrNull(String::isNotBlank)
        .orEmpty()
}

private fun selectBestDuration(metadataDonor: Track, current: Track, candidate: Track): Long {
    if (metadataDonor.durationMs > 0L) return metadataDonor.durationMs
    if (current.durationMs > 0L) return current.durationMs
    return candidate.durationMs
}

internal fun richerSong(current: Track, candidate: Track): Track {
    val isAnySpotify = isSpotifyTrack(current) || isSpotifyTrack(candidate)
    val metadataDonor = selectSongMetadataDonor(current, candidate)
    val playbackDonor = selectSongPlaybackDonor(current, candidate)
    val mergedId = resolveMergedSongId(current, candidate, playbackDonor)
    val videoId = resolveMergedVideoId(mergedId, current, candidate, playbackDonor)

    val videoUrl = playbackDonor.videoUrl.ifBlank {
        if (videoId.isNotBlank()) "https://www.youtube.com/watch?v=$videoId"
        else current.videoUrl.ifBlank { candidate.videoUrl }
    }

    val counterpart = videoId.ifBlank {
        sequenceOf(playbackDonor.counterpartVideoId, current.counterpartVideoId, candidate.counterpartVideoId)
            .firstOrNull(String::isNotBlank)
            .orEmpty()
    }

    val audioVideoId = playbackDonor.audioVideoId.ifBlank {
        videoId.ifBlank { current.audioVideoId.ifBlank { candidate.audioVideoId } }
    }

    return current.copy(
        id = mergedId,
        title = metadataDonor.title.ifBlank { current.title.ifBlank { candidate.title } },
        artist = metadataDonor.artist.ifBlank { current.artist.ifBlank { candidate.artist } },
        thumbnailUrl = metadataDonor.thumbnailUrl.ifBlank { current.thumbnailUrl.ifBlank { candidate.thumbnailUrl } },
        largeThumbnailUrl = metadataDonor.largeThumbnailUrl.ifBlank { current.largeThumbnailUrl.ifBlank { candidate.largeThumbnailUrl } },
        album = metadataDonor.album.ifBlank { current.album.ifBlank { candidate.album } },
        albumBrowseId = current.albumBrowseId.ifBlank { candidate.albumBrowseId },
        artistBrowseIds = current.artistBrowseIds.ifEmpty { candidate.artistBrowseIds },
        durationMs = selectBestDuration(metadataDonor, current, candidate),
        videoUrl = videoUrl,
        streamUrl = if (playbackDonor.streamUrl.isNotBlank()) playbackDonor.streamUrl else current.streamUrl.ifBlank { candidate.streamUrl },
        counterpartVideoId = counterpart,
        audioVideoId = audioVideoId,
        videoType = playbackDonor.videoType.ifBlank { current.videoType.ifBlank { candidate.videoType } },
        isrc = metadataDonor.isrc.ifBlank { current.isrc.ifBlank { candidate.isrc } },
        explicit = current.explicit || candidate.explicit,
        source = if (isAnySpotify) "spotify_youtube" else current.source.ifBlank { candidate.source },
        metadataProvider = if (isAnySpotify) "spotify" else current.metadataProvider.ifBlank { candidate.metadataProvider },
        metadataConfidence = maxOf(current.metadataConfidence, candidate.metadataConfidence),
        youtubeViewCount = maxOf(current.youtubeViewCount, candidate.youtubeViewCount)
    )
}

private fun richerAlbum(current: AlbumHit, candidate: AlbumHit): AlbumHit {
    val ytBrowseId = sequenceOf(current.browseId, candidate.browseId)
        .firstOrNull { it.isNotBlank() && !it.startsWith("spotify:", ignoreCase = true) }
        .orEmpty()
    val ytPlaylistId = sequenceOf(current.audioPlaylistId, candidate.audioPlaylistId)
        .firstOrNull { it.isNotBlank() && !it.startsWith("spotify:", ignoreCase = true) }
        .orEmpty()
    val spotifyArtwork = sequenceOf(current.thumbnailUrl, candidate.thumbnailUrl)
        .firstOrNull { it.contains("scdn.co") || it.contains("spotifycdn.com") }
        .orEmpty()
    val chosenArtwork = spotifyArtwork.ifBlank { current.thumbnailUrl.ifBlank { candidate.thumbnailUrl } }

    return current.copy(
        browseId = ytBrowseId.ifBlank { current.browseId.ifBlank { candidate.browseId } },
        artistBrowseId = current.artistBrowseId.ifBlank { candidate.artistBrowseId },
        audioPlaylistId = ytPlaylistId.ifBlank { current.audioPlaylistId.ifBlank { candidate.audioPlaylistId } },
        thumbnailUrl = chosenArtwork,
        year = current.year.ifBlank { candidate.year },
        releaseDate = current.releaseDate.ifBlank { candidate.releaseDate },
        upc = current.upc.ifBlank { candidate.upc },
        canonicalUrl = current.canonicalUrl.ifBlank { candidate.canonicalUrl },
        explicit = current.explicit || candidate.explicit,
        releaseType = if (current.releaseType == ReleaseType.Unknown) candidate.releaseType else current.releaseType
    )
}

private fun richerArtist(current: ArtistHit, candidate: ArtistHit): ArtistHit {
    val ytBrowseId = sequenceOf(current.browseId, candidate.browseId)
        .firstOrNull { it.isNotBlank() && !it.startsWith("spotify:", ignoreCase = true) }
        .orEmpty()
    val spotifyArtwork = sequenceOf(current.thumbnailUrl, candidate.thumbnailUrl)
        .firstOrNull { isAllowedSpotifyArtistArtworkUrl(it) }
        .orEmpty()
    val chosenArtwork = spotifyArtwork.ifBlank { current.thumbnailUrl.ifBlank { candidate.thumbnailUrl } }
    val chosenSubscribers = current.subscribers.ifBlank { candidate.subscribers }
    return current.copy(
        browseId = ytBrowseId.ifBlank { current.browseId.ifBlank { candidate.browseId } },
        thumbnailUrl = chosenArtwork,
        subscribers = chosenSubscribers,
        officialArtwork = current.officialArtwork || candidate.officialArtwork || spotifyArtwork.isNotBlank()
    )
}

private fun richerPlaylist(current: PlaylistHit, candidate: PlaylistHit): PlaylistHit {
    val ytPlaylistId = sequenceOf(current.playlistId, candidate.playlistId)
        .firstOrNull { it.isNotBlank() && !it.startsWith("spotify:", ignoreCase = true) }
        .orEmpty()
    val ytBrowseId = sequenceOf(current.browseId, candidate.browseId)
        .firstOrNull { it.isNotBlank() && !it.startsWith("spotify:", ignoreCase = true) }
        .orEmpty()
    return current.copy(
        playlistId = ytPlaylistId.ifBlank { current.playlistId.ifBlank { candidate.playlistId } },
        browseId = ytBrowseId.ifBlank { current.browseId.ifBlank { candidate.browseId } },
        thumbnailUrl = current.thumbnailUrl.ifBlank { candidate.thumbnailUrl },
        author = current.author.ifBlank { candidate.author },
        trackCountLabel = current.trackCountLabel.ifBlank { candidate.trackCountLabel }
    )
}

private const val MUSIC_VIDEO_TYPE_AUDIO = "MUSIC_VIDEO_TYPE_ATV"

internal fun findVerifiedTopResultArtist(
    candidates: List<ArtistHit>,
    heroTrack: Track?,
    query: String = ""
): ArtistHit? {
    if (candidates.isEmpty()) return null
    val verified = candidates.filter { candidate ->
        isVerifiedArtistCandidate(candidate, heroTrack, query)
    }
    if (verified.isEmpty()) return null
    if (verified.size == 1) return verified.first()

    val cleanQuery = query.trim()
    if (cleanQuery.isNotBlank()) {
        val qKey = artistIdentityKey(cleanQuery)
        val queryMatch = verified.firstOrNull { candidate ->
            val cKey = artistIdentityKey(candidate.name)
            cKey == qKey || cKey.startsWith("$qKey ") || artistIdentityMatches(candidate.name, cleanQuery)
        }
        if (queryMatch != null) return queryMatch
    }

    val heroPrimary = heroTrack?.artist?.let { primaryArtistSegment(it).ifBlank { it.trim() } }.orEmpty()
    if (heroPrimary.isNotBlank()) {
        val primaryMatch = verified.firstOrNull { candidate ->
            candidate.name.equals(heroPrimary, ignoreCase = true) || artistIdentityMatches(candidate.name, heroPrimary)
        }
        if (primaryMatch != null) return primaryMatch
    }

    return verified.first()
}

internal fun isVerifiedArtistCandidate(
    candidate: ArtistHit,
    heroTrack: Track?,
    query: String = ""
): Boolean {
    val candidateName = candidate.name.trim()
    if (candidateName.isBlank()) return false
    val candidateBrowseId = candidate.browseId.trim().lowercase(Locale.ROOT)

    if (heroTrack != null && candidateBrowseId.isNotBlank()) {
        val heroBrowseIds = heroTrack.artistBrowseIds
            .asSequence()
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter(String::isNotBlank)
            .toSet()
        if (candidateBrowseId in heroBrowseIds) return true
    }

    val heroArtist = heroTrack?.artist?.trim().orEmpty()
    val heroPrimary = if (heroArtist.isNotBlank()) {
        primaryArtistSegment(heroArtist).ifBlank { heroArtist }
    } else ""

    if (heroPrimary.isNotBlank()) {
        if (candidateName.equals(heroPrimary, ignoreCase = true) || artistIdentityMatches(candidateName, heroPrimary)) {
            return true
        }
    }
    if (heroArtist.isNotBlank()) {
        if (candidateName.equals(heroArtist, ignoreCase = true) || artistIdentityMatches(candidateName, heroArtist)) {
            return true
        }
    }

    val cleanQuery = query.trim()
    if (cleanQuery.isNotBlank()) {
        val cKey = artistIdentityKey(candidateName)
        val qKey = artistIdentityKey(cleanQuery)
        val queryPrimary = primaryArtistSegment(cleanQuery).ifBlank { cleanQuery }
        val matchesQuery = candidateName.equals(cleanQuery, ignoreCase = true) ||
            candidateName.equals(queryPrimary, ignoreCase = true) ||
            artistIdentityMatches(candidateName, cleanQuery) ||
            artistIdentityMatches(candidateName, queryPrimary) ||
            (qKey.isNotBlank() && (cKey == qKey || cKey.startsWith("$qKey ") || qKey.startsWith("$cKey ")))

        if (matchesQuery) {
            if (heroArtist.isBlank()) {
                return true
            }
            val hKey = artistIdentityKey(heroPrimary)
            if (heroArtist.contains(candidateName, ignoreCase = true) ||
                artistIdentityMatches(candidateName, heroPrimary) ||
                artistIdentityMatches(candidateName, heroArtist) ||
                (hKey.isNotBlank() && (cKey == hKey || cKey.startsWith("$hKey ") || hKey.startsWith("$cKey ")))
            ) {
                return true
            }
        }
    }

    return false
}
