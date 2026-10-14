package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.data.spotify.MatchedYouTubeData
import com.luc4n3x.levyra.data.spotify.SpotifyYouTubeMatchCache
import com.luc4n3x.levyra.data.spotify.SpotifyYouTubeMatcher
import com.luc4n3x.levyra.data.spotify.preservesPersonalMetadata
import com.luc4n3x.levyra.data.spotify.withSpotifyMetadata
import com.luc4n3x.levyra.data.spotify.sameSpotifyAlbum
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
    val hasSpotify = existing.any(::isSpotifySearchTrack) || incoming.any(::isSpotifySearchTrack)
    if (!hasSpotify) {
        val strict = mergeSearchEntities(
            existing,
            incoming,
            ::searchSongIdentityKey,
            ::searchSongMetadataKey,
            ::richerSong
        )
        return absorbMatchedMusicVideos(mergeComplementarySearchSongMetadata(strict))
    }

    return absorbMatchedMusicVideos(mergeSpotifyAndYoutubeSongs(existing, incoming))
}

private fun isLocalSearchTrack(track: Track): Boolean {
    val stream = track.streamUrl.trim()
    return track.id.startsWith("local:", ignoreCase = true) ||
        track.source.equals("Offline", ignoreCase = true) ||
        stream.startsWith("content://", ignoreCase = true) ||
        stream.startsWith("file://", ignoreCase = true)
}

internal fun isSpotifySearchTrack(track: Track): Boolean {
    if (isLocalSearchTrack(track)) return false
    return track.metadataProvider.equals("spotify", ignoreCase = true) ||
        track.id.startsWith("spotify:", ignoreCase = true) ||
        track.source.equals("spotify", ignoreCase = true) ||
        track.source.equals("spotify_youtube", ignoreCase = true)
}

private fun isPlayableYoutubeId(id: String): Boolean =
    id.length == 11 &&
        !id.startsWith("spotify:", ignoreCase = true) &&
        id.all { it.isLetterOrDigit() || it == '-' || it == '_' }

private fun isYoutubeSearchTrack(track: Track): Boolean {
    if (isLocalSearchTrack(track) || isSpotifySearchTrack(track)) return false
    val videoUrl = track.videoUrl.trim()
    return track.source.contains("youtube", ignoreCase = true) ||
        track.metadataProvider.contains("youtube", ignoreCase = true) ||
        videoUrl.contains("youtube.com/", ignoreCase = true) ||
        videoUrl.contains("youtu.be/", ignoreCase = true) ||
        isPlayableYoutubeId(track.audioVideoId.trim()) ||
        isPlayableYoutubeId(track.counterpartVideoId.trim())
}

private enum class SearchTrackKind {
    Spotify,
    Youtube,
    Other
}

private fun searchTrackKind(track: Track): SearchTrackKind = when {
    isSpotifySearchTrack(track) -> SearchTrackKind.Spotify
    isYoutubeSearchTrack(track) -> SearchTrackKind.Youtube
    else -> SearchTrackKind.Other
}

private fun stableTrackMergeKeys(track: Track): List<String> {
    val kind = searchTrackKind(track)
    val identity = searchSongIdentityKey(track)
    val metadata = searchSongMetadataKey(track)
    return buildList {
        if (identity.isNotBlank()) add("id:$identity")
        if (metadata.isNotBlank()) {
            val albumContext = if (kind == SearchTrackKind.Spotify) {
                com.luc4n3x.levyra.nexus.identity.MusicIdentityText.album(track.album).let { "${it.core}|${it.editions.sortedBy { edition -> edition.name }}" }
            } else ""
            add("meta:${kind.name}:$metadata|$albumContext")
        }
    }
}

private fun mergeStableSearchTracks(existing: List<Track>, incoming: List<Track>): MutableList<Track> {
    val ordered = mutableListOf<Track>()
    val slotByKey = HashMap<String, Int>()
    (existing.asSequence() + incoming.asSequence()).forEach { candidate ->
        val keys = stableTrackMergeKeys(candidate)
        val slot = keys.firstNotNullOfOrNull(slotByKey::get)
        if (slot == null) {
            val index = ordered.size
            ordered.add(candidate)
            keys.forEach { key -> slotByKey[key] = index }
        } else {
            ordered[slot] = richerSong(ordered[slot], candidate)
            keys.forEach { key -> slotByKey.putIfAbsent(key, slot) }
        }
    }
    return ordered
}

internal fun mergeSpotifyAndYoutubeSongs(existing: List<Track>, incoming: List<Track>): List<Track> {
    val ordered = mergeStableSearchTracks(existing, incoming)
    val spotifyIndices = ordered.indices.filter { searchTrackKind(ordered[it]) == SearchTrackKind.Spotify }
    val availableYoutubeIndices = ordered.indices
        .filter { searchTrackKind(ordered[it]) == SearchTrackKind.Youtube }
        .toMutableSet()
    val availableLocalIndices = ordered.indices
        .filter { isLocalSearchTrack(ordered[it]) }
        .toMutableSet()
    val youtubeIndexById = availableYoutubeIndices
        .associateBy { ordered[it].id.trim() }
    val matchCache = SpotifyYouTubeMatchCache.get()
    val youtubeBySpotifyIndex = HashMap<Int, Int>()
    val localBySpotifyIndex = HashMap<Int, Int>()

    for (spotifyIndex in spotifyIndices) {
        val spotifyTrack = ordered[spotifyIndex]
        val metadataKey = searchSongMetadataKey(spotifyTrack)
        if (metadataKey.isNotBlank()) {
            val localIndex = availableLocalIndices.firstOrNull { localIndex ->
                searchSongMetadataKey(ordered[localIndex]) == metadataKey
            }
            if (localIndex != null) {
                localBySpotifyIndex[spotifyIndex] = localIndex
                availableLocalIndices.remove(localIndex)
            }
        }

        val candidates = availableYoutubeIndices.map { ordered[it] }.filter {
            compatibleMetadataAlbum(spotifyTrack, it)
        }
        val matched = SpotifyYouTubeMatcher.findBestMatch(spotifyTrack, candidates)?.candidate
        val cachedVideoId = spotifyTrack.id
            .takeIf { it.startsWith("spotify:", ignoreCase = true) }
            ?.let(matchCache::get)
            ?.videoId
            .orEmpty()

        val youtubeIndex = matched
            ?.let { match -> availableYoutubeIndices.firstOrNull { ordered[it].id == match.id } }
            ?: youtubeIndexById[cachedVideoId]?.takeIf { index ->
                index in availableYoutubeIndices && compatibleMetadataAlbum(spotifyTrack, ordered[index]) &&
                    SpotifyYouTubeMatcher.scoreMatch(spotifyTrack, ordered[index]) >= SpotifyYouTubeMatcher.DEFAULT_CONFIDENCE_THRESHOLD
            }

        if (youtubeIndex != null) {
            youtubeBySpotifyIndex[spotifyIndex] = youtubeIndex
            availableYoutubeIndices.remove(youtubeIndex)
        }
    }

    val spotifyIndexByYoutube = youtubeBySpotifyIndex.entries.associate { (spotify, youtube) -> youtube to spotify }
    val spotifyIndexByLocal = localBySpotifyIndex.entries.associate { (spotify, local) -> local to spotify }
    val result = mutableListOf<Track>()

    for (index in ordered.indices) {
        val spotifyIndex = when {
            index in youtubeBySpotifyIndex || index in localBySpotifyIndex -> index
            index in spotifyIndexByYoutube -> spotifyIndexByYoutube.getValue(index)
            index in spotifyIndexByLocal -> spotifyIndexByLocal.getValue(index)
            else -> null
        }
        if (spotifyIndex != null) {
            val participantIndices = buildList {
                add(spotifyIndex)
                youtubeBySpotifyIndex[spotifyIndex]?.let(::add)
                localBySpotifyIndex[spotifyIndex]?.let(::add)
            }
            if (index != participantIndices.minOrNull()) continue

            val spotifyTrack = ordered[spotifyIndex]
            var combined = spotifyTrack
            youtubeBySpotifyIndex[spotifyIndex]?.let { youtubeIndex ->
                val youtubeTrack = ordered[youtubeIndex]
                if (spotifyTrack.id.startsWith("spotify:", ignoreCase = true)) {
                    matchCache.put(spotifyTrack.id, youtubeTrack)
                }
                combined = richerSong(combined, youtubeTrack)
            }
            localBySpotifyIndex[spotifyIndex]?.let { localIndex ->
                combined = richerSong(combined, ordered[localIndex])
            }
            result.add(combined)
            continue
        }

        val track = ordered[index]
        if (
            searchTrackKind(track) == SearchTrackKind.Spotify &&
            track.id.startsWith("spotify:", ignoreCase = true) &&
            track.counterpartVideoId.isBlank() &&
            track.audioVideoId.isBlank()
        ) {
            val cached = matchCache.get(track.id)
            if (cached != null) {
                result.add(enrichWithCachedData(track, cached))
                continue
            }
        }
        result.add(track)
    }

    return result
}

private fun isSearchMusicVideo(track: Track): Boolean =
    searchTrackKind(track) == SearchTrackKind.Youtube && isMusicVideoResult(track.videoType)

private fun isSearchRecordingEntry(track: Track): Boolean =
    !isLocalSearchTrack(track) && !preservesPersonalMetadata(track) &&
        (isSpotifySearchTrack(track) || searchTrackKind(track) == SearchTrackKind.Youtube && !isMusicVideoResult(track.videoType))

private fun hasYoutubeAudioIdentity(track: Track): Boolean =
    isPlayableYoutubeId(track.audioVideoId.trim()) || isPlayableYoutubeId(track.id.trim())

private fun absorbMusicVideo(entry: Track, video: Track): Track {
    if (!hasYoutubeAudioIdentity(entry)) return richerSong(entry, video)
    val audioId = entry.audioVideoId.trim().ifBlank { entry.id.trim() }
    val counterpart = entry.counterpartVideoId.trim()
    val keepsCounterpart = isPlayableYoutubeId(counterpart) && counterpart != audioId &&
        !video.videoType.equals(MUSIC_VIDEO_TYPE_OFFICIAL, ignoreCase = true)
    return entry.copy(
        audioVideoId = audioId,
        counterpartVideoId = if (keepsCounterpart) counterpart else video.id.trim(),
        explicit = entry.explicit || video.explicit,
        youtubeViewCount = maxOf(entry.youtubeViewCount, video.youtubeViewCount)
    )
}

private fun absorbMatchedMusicVideos(tracks: List<Track>): List<Track> {
    if (tracks.size < 2) return tracks
    val entryIndices = tracks.indices.filter { isSearchRecordingEntry(tracks[it]) }
    if (entryIndices.isEmpty()) return tracks
    val entryByVideo = HashMap<Int, Int>()
    for (videoIndex in tracks.indices) {
        val video = tracks[videoIndex]
        if (!isSearchMusicVideo(video)) continue
        val entryIndex = entryIndices
            .map { it to SpotifyYouTubeMatcher.scoreMatch(tracks[it], video) }
            .filter { (_, score) -> score >= RECORDING_VIDEO_CONFIDENCE }
            .maxByOrNull { (_, score) -> score }
            ?.first
            ?: continue
        entryByVideo[videoIndex] = entryIndex
    }
    if (entryByVideo.isEmpty()) return tracks

    val merged = tracks.toMutableList()
    val videosByEntry = entryByVideo.entries.groupBy({ it.value }, { it.key })
    val slotByEntry = HashMap<Int, Int>()
    for ((entryIndex, videoIndices) in videosByEntry) {
        var combined = tracks[entryIndex]
        videoIndices
            .sortedByDescending { tracks[it].videoType.equals(MUSIC_VIDEO_TYPE_OFFICIAL, ignoreCase = true) }
            .forEach { combined = absorbMusicVideo(combined, tracks[it]) }
        merged[entryIndex] = combined
        slotByEntry[entryIndex] = minOf(entryIndex, videoIndices.minOrNull() ?: entryIndex)
    }
    val emitAt = slotByEntry.entries.associate { (entry, slot) -> slot to entry }
    return buildList {
        for (index in tracks.indices) {
            emitAt[index]?.let { add(merged[it]) }
            if (index in entryByVideo || index in slotByEntry) continue
            add(merged[index])
        }
    }
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

private fun hasSharedYoutubeIdentity(first: Track, second: Track): Boolean {
    val firstIds = sequenceOf(first.id, first.counterpartVideoId, first.audioVideoId)
        .filter(::isPlayableYoutubeId)
        .toSet()
    if (firstIds.isEmpty()) return false
    return sequenceOf(second.id, second.counterpartVideoId, second.audioVideoId)
        .filter(::isPlayableYoutubeId)
        .any(firstIds::contains)
}

private fun sameSearchRecording(first: Track, second: Track): Boolean {
    if ((isSpotifySearchTrack(first) || isSpotifySearchTrack(second)) && !compatibleMetadataAlbum(first, second)) {
        return false
    }
    val firstIdentity = searchSongIdentityKey(first)
    if (firstIdentity.isNotBlank() && firstIdentity == searchSongIdentityKey(second)) return true
    if (hasSharedYoutubeIdentity(first, second)) return true

    if ((isSpotifySearchTrack(first) || isSpotifySearchTrack(second)) &&
        SpotifyYouTubeMatcher.scoreMatch(first, second) < SpotifyYouTubeMatcher.DEFAULT_CONFIDENCE_THRESHOLD
    ) return false

    val firstIsrc = first.isrc.trim().uppercase(Locale.ROOT)
    val secondIsrc = second.isrc.trim().uppercase(Locale.ROOT)
    if (firstIsrc.isNotBlank() && firstIsrc == secondIsrc) return true

    val firstMetadata = searchSongMetadataKey(first)
    if (firstMetadata.isNotBlank() && firstMetadata == searchSongMetadataKey(second)) return true

    if (areComplementarySearchSongResults(first, second)) return true

    val firstSpotify = isSpotifySearchTrack(first)
    val secondSpotify = isSpotifySearchTrack(second)
    val firstYoutube = isYoutubeSearchTrack(first)
    val secondYoutube = isYoutubeSearchTrack(second)
    return when {
        firstSpotify && secondYoutube ->
            SpotifyYouTubeMatcher.scoreMatch(first, second) >= SpotifyYouTubeMatcher.DEFAULT_CONFIDENCE_THRESHOLD
        secondSpotify && firstYoutube ->
            SpotifyYouTubeMatcher.scoreMatch(second, first) >= SpotifyYouTubeMatcher.DEFAULT_CONFIDENCE_THRESHOLD
        else -> false
    }
}

private fun mergeTopResultDisplayDuplicates(tracks: List<Track>): List<Track> {
    if (tracks.size < 2) return tracks
    val merged = mutableListOf<Track>()
    tracks.forEach { candidate ->
        val slot = merged.indexOfFirst { existing -> sameSearchRecording(existing, candidate) }
        if (slot < 0) {
            merged.add(candidate)
        } else {
            merged[slot] = richerSong(merged[slot], candidate)
        }
    }
    return merged
}

internal fun selectSearchTopResultTracks(
    topTrack: Track?,
    songs: List<Track>,
    limit: Int = 3
): List<Track> {
    val hero = topTrack ?: return emptyList()
    val merged = mergeTopResultDisplayDuplicates(deduplicateSearchSongs(listOf(hero) + songs))
    val resolvedHero = merged.firstOrNull { candidate ->
        sameSearchRecording(hero, candidate)
    }?.let { candidate -> playableRecordingFor(candidate, merged) ?: candidate }
        ?: playableRecordingFor(hero, merged)
        ?: hero
    val heroTitle = com.luc4n3x.levyra.nexus.identity.MusicIdentityText.title(resolvedHero.title)
    val heroArtist = primaryArtistSegment(resolvedHero.artist).ifBlank { resolvedHero.artist.trim() }
    val heroBrowseIds = resolvedHero.artistBrowseIds
        .map { it.trim().lowercase(Locale.ROOT) }
        .filter(String::isNotBlank)
        .toSet()
    val boundedLimit = limit.coerceIn(1, 3)

    return buildList {
        add(resolvedHero)
        if (heroArtist.isBlank() && heroBrowseIds.isEmpty()) return@buildList
        val sameArtist = merged
            .filterNot { it.id == resolvedHero.id || hasNoPlayableIdentity(it) }
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
        val (sameTitle, otherTitles) = sameArtist.partition { candidate ->
            heroTitle.core.isNotBlank() &&
                com.luc4n3x.levyra.nexus.identity.MusicIdentityText.title(candidate.title).core == heroTitle.core
        }
        val versions = sameTitle.filter { candidate ->
            com.luc4n3x.levyra.nexus.identity.MusicIdentityText.title(candidate.title).versionSignature != heroTitle.versionSignature
        }
        (versions + otherTitles)
            .take(boundedLimit - 1)
            .forEach(::add)
    }.take(boundedLimit)
}

private fun hasNoPlayableIdentity(track: Track): Boolean =
    !isLocalSearchTrack(track) && track.streamUrl.isBlank() && !preservesPersonalMetadata(track) &&
        (searchTrackKind(track) == SearchTrackKind.Other || LevyraStartupCatalog.isStartupSeedId(track.id))

private fun playableRecordingFor(display: Track, candidates: List<Track>): Track? {
    if (!hasNoPlayableIdentity(display)) return null
    val looseKey = searchSongLooseMetadataKey(display)
    if (looseKey.isBlank()) return null
    val displayAlbum = com.luc4n3x.levyra.nexus.identity.MusicIdentityText.album(display.album)
    val displayTitle = com.luc4n3x.levyra.nexus.identity.MusicIdentityText.title(display.title)
    return candidates.firstOrNull { candidate ->
        if (candidate === display || hasNoPlayableIdentity(candidate)) return@firstOrNull false
        if (searchSongLooseMetadataKey(candidate) != looseKey) return@firstOrNull false
        val candidateTitle = com.luc4n3x.levyra.nexus.identity.MusicIdentityText.title(candidate.title)
        if (candidateTitle.versionSignature != displayTitle.versionSignature) return@firstOrNull false
        if (display.durationMs > 0L && candidate.durationMs > 0L &&
            kotlin.math.abs(display.durationMs - candidate.durationMs) > 5_000L
        ) return@firstOrNull false
        val candidateAlbum = com.luc4n3x.levyra.nexus.identity.MusicIdentityText.album(candidate.album)
        displayAlbum.isBlank || candidateAlbum.isBlank || displayAlbum.core == candidateAlbum.core
    }
}

internal fun filterSearchSongsExcludingTopResult(
    songs: List<Track>,
    topResultTracks: List<Track>
): List<Track> {
    if (songs.isEmpty() || topResultTracks.isEmpty()) return songs

    val topResultIds = topResultTracks
        .mapNotNullTo(HashSet()) { track -> searchSongIdentityKey(track).takeIf(String::isNotBlank) }
    return songs.filterNot { song ->
        searchSongIdentityKey(song) in topResultIds ||
            topResultTracks.any { topTrack -> sameSearchRecording(song, topTrack) }
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
    val currentIsSpotify = isSpotifySearchTrack(current)
    val candidateIsSpotify = isSpotifySearchTrack(candidate)
    return when {
        currentIsSpotify && !candidateIsSpotify -> current
        !currentIsSpotify && candidateIsSpotify -> candidate
        isSearchMusicVideo(current) && searchTrackKind(candidate) == SearchTrackKind.Youtube && !isSearchMusicVideo(candidate) -> candidate
        else -> current
    }
}

private fun selectSongPlaybackDonor(current: Track, candidate: Track): Track {
    if (isLocalSearchTrack(current)) return current
    if (isLocalSearchTrack(candidate)) return candidate
    val currentHasYtId = isYoutubeSearchTrack(current) ||
        isPlayableYoutubeId(current.id) ||
        isPlayableYoutubeId(current.counterpartVideoId) ||
        isPlayableYoutubeId(current.audioVideoId)
    val candidateHasYtId = isYoutubeSearchTrack(candidate) ||
        isPlayableYoutubeId(candidate.id) ||
        isPlayableYoutubeId(candidate.counterpartVideoId) ||
        isPlayableYoutubeId(candidate.audioVideoId)
    return when {
        candidateHasYtId && !currentHasYtId -> candidate
        currentHasYtId && candidateHasYtId && isSearchMusicVideo(current) && !isSearchMusicVideo(candidate) &&
            searchTrackKind(candidate) == SearchTrackKind.Youtube -> candidate
        currentHasYtId -> current
        candidate.videoUrl.isNotBlank() && current.videoUrl.isBlank() -> candidate
        else -> current
    }
}

private fun resolveMergedSongId(current: Track, candidate: Track, playbackDonor: Track): String {
    if (isLocalSearchTrack(playbackDonor)) return playbackDonor.id
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

private fun compatibleMetadataAlbum(first: Track, second: Track): Boolean =
    first.album.isBlank() || second.album.isBlank() || sameSpotifyAlbum(first.album, second.album)

internal fun richerSong(current: Track, candidate: Track): Track {
    if (preservesPersonalMetadata(current)) return current
    if (preservesPersonalMetadata(candidate)) return candidate
    val isAnySpotify = isSpotifySearchTrack(current) || isSpotifySearchTrack(candidate)
    if (isAnySpotify && !compatibleMetadataAlbum(current, candidate)) return current
    val metadataDonor = selectSongMetadataDonor(current, candidate)
    val playbackDonor = selectSongPlaybackDonor(current, candidate)
    val mergedId = resolveMergedSongId(current, candidate, playbackDonor)
    val videoId = resolveMergedVideoId(mergedId, current, candidate, playbackDonor)

    val videoUrl = playbackDonor.videoUrl.ifBlank {
        if (videoId.isNotBlank()) "https://www.youtube.com/watch?v=$videoId"
        else current.videoUrl.ifBlank { candidate.videoUrl }
    }

    val absorbedVideoId = sequenceOf(current, candidate)
        .firstOrNull { it !== playbackDonor && isSearchMusicVideo(it) && !isSearchMusicVideo(playbackDonor) }
        ?.id?.trim()
        ?.takeIf(::isPlayableYoutubeId)
    val pairedCounterpart = playbackDonor.counterpartVideoId.trim()
        .takeIf { isPlayableYoutubeId(it) && it != mergedId }
    val counterpart = pairedCounterpart ?: absorbedVideoId ?: videoId.ifBlank {
        sequenceOf(playbackDonor.counterpartVideoId, current.counterpartVideoId, candidate.counterpartVideoId)
            .firstOrNull(String::isNotBlank)
            .orEmpty()
    }

    val audioVideoId = playbackDonor.audioVideoId.ifBlank {
        videoId.ifBlank { current.audioVideoId.ifBlank { candidate.audioVideoId } }
    }

    val metadataBase = if (isAnySpotify) withSpotifyMetadata(current, metadataDonor) else current
    return metadataBase.copy(
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
        upc = metadataDonor.upc.ifBlank { current.upc.ifBlank { candidate.upc } },
        releaseDate = metadataDonor.releaseDate.ifBlank { current.releaseDate.ifBlank { candidate.releaseDate } },
        year = metadataDonor.year.ifBlank { current.year.ifBlank { candidate.year } },
        trackNumber = metadataDonor.trackNumber.takeIf { it > 0 } ?: current.trackNumber.takeIf { it > 0 } ?: candidate.trackNumber,
        discNumber = metadataDonor.discNumber.takeIf { it > 0 } ?: current.discNumber.takeIf { it > 0 } ?: candidate.discNumber,
        trackTotal = metadataDonor.trackTotal.takeIf { it > 0 } ?: current.trackTotal.takeIf { it > 0 } ?: candidate.trackTotal,
        discTotal = metadataDonor.discTotal.takeIf { it > 0 } ?: current.discTotal.takeIf { it > 0 } ?: candidate.discTotal,
        albumArtist = metadataDonor.albumArtist.ifBlank { current.albumArtist.ifBlank { candidate.albumArtist } },
        canonicalAlbumUrl = metadataDonor.canonicalAlbumUrl.ifBlank { current.canonicalAlbumUrl.ifBlank { candidate.canonicalAlbumUrl } },
        explicit = current.explicit || candidate.explicit,
        source = when {
            isLocalSearchTrack(playbackDonor) -> playbackDonor.source
            isAnySpotify && videoId.isNotBlank() -> "spotify_youtube"
            isAnySpotify -> "spotify"
            else -> current.source.ifBlank { candidate.source }
        },
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
private const val MUSIC_VIDEO_TYPE_OFFICIAL = "MUSIC_VIDEO_TYPE_OMV"
private const val RECORDING_VIDEO_CONFIDENCE = 0.90

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
