package com.luc4n3x.levyra.data.apple

import com.luc4n3x.levyra.data.playbackAlternativeSearchQueries
import com.luc4n3x.levyra.data.runCatchingPreservingCancellation
import com.luc4n3x.levyra.data.spotify.SpotifyYouTubeMatcher
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.json.JSONArray

internal fun appleTrendingSongs(page: AppleCatalogPage?): List<Track> =
    page?.shelves.orEmpty()
        .filter { shelf -> shelf.kind == AppleCatalogShelfKind.Songs }
        .getOrNull(TRENDING_SONGS_SHELF_INDEX)
        ?.items.orEmpty()
        .mapNotNull(AppleCatalogTracks::fromItem)

internal fun withAppleSongDetails(tracks: List<Track>, songs: JSONArray?): List<Track> {
    songs ?: return tracks
    val attributesById = (0 until songs.length())
        .mapNotNull(songs::optJSONObject)
        .mapNotNull { song -> song.optJSONObject("attributes")?.let { song.optString("id") to it } }
        .toMap()
    return tracks.map { track ->
        val attributes = attributesById[track.appleSongId] ?: return@map track
        track.copy(
            durationMs = attributes.optLong("durationInMillis", 0L).takeIf { it > 0L } ?: track.durationMs,
            isrc = attributes.optString("isrc").trim().ifBlank { track.isrc },
            album = track.album.ifBlank { attributes.optString("albumName").trim() }
        )
    }
}

internal object AppleTrendingQuickPicks {
    suspend fun match(appleTracks: List<Track>, search: suspend (String) -> List<Track>): List<Track> = coroutineScope {
        val gate = Semaphore(MATCH_PARALLELISM)
        appleTracks.take(MAX_TRENDING_TRACKS)
            .map { apple -> async { gate.withPermit { matchOne(apple, search) } } }
            .awaitAll()
            .filterNotNull()
            .distinctBy { it.id }
    }

    private suspend fun matchOne(apple: Track, search: suspend (String) -> List<Track>): Track? {
        val candidates = LinkedHashMap<String, Track>()
        for (query in playbackAlternativeSearchQueries(apple).take(MAX_QUERIES_PER_TRACK)) {
            runCatchingPreservingCancellation { search(query) }
                .getOrDefault(emptyList())
                .filter { it.id.length == YOUTUBE_VIDEO_ID_LENGTH }
                .forEach { candidates.putIfAbsent(it.id, it) }
            SpotifyYouTubeMatcher.findBestMatch(apple, candidates.values.toList())?.let { match ->
                return match.candidate.copy(appleSongId = apple.appleSongId)
            }
        }
        return null
    }

    private const val MAX_TRENDING_TRACKS = 20
    private const val MAX_QUERIES_PER_TRACK = 2
    private const val MATCH_PARALLELISM = 4
    private const val YOUTUBE_VIDEO_ID_LENGTH = 11
}

private const val TRENDING_SONGS_SHELF_INDEX = 1
