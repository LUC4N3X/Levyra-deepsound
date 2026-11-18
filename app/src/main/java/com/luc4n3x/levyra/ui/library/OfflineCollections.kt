package com.luc4n3x.levyra.ui.library

import androidx.compose.runtime.Immutable
import com.luc4n3x.levyra.domain.DownloadOwnership
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.Track

internal enum class OfflineMix { Recent, Favorites, MostPlayed, SmartOffline }

@Immutable
internal data class OfflineCollection(
    val key: String,
    val title: String,
    val mix: OfflineMix?,
    val artwork: Track,
    val itemKeys: List<String>
)

@Immutable
internal data class OfflineCollections(
    val mixes: List<OfflineCollection> = emptyList(),
    val artists: List<OfflineCollection> = emptyList(),
    val playlists: List<OfflineCollection> = emptyList()
) {
    fun find(key: String?): OfflineCollection? =
        key?.let { wanted -> (mixes + artists + playlists).firstOrNull { it.key == wanted } }

    companion object {
        val Empty = OfflineCollections()
    }
}

private const val OFFLINE_RECENT_LIMIT = 50

internal fun buildOfflineCollections(
    items: List<LibraryOfflineItem>,
    playlists: List<Playlist>,
    favorites: List<Track>,
    mostPlayed: List<Track>
): OfflineCollections {
    if (items.isEmpty()) return OfflineCollections.Empty
    val byTrackId = HashMap<String, LibraryOfflineItem>()
    val byText = HashMap<Pair<String, String>, LibraryOfflineItem>()
    items.forEach { item ->
        item.download.trackId.trim().takeIf(String::isNotBlank)?.let { byTrackId.putIfAbsent(it, item) }
        byText.putIfAbsent(normalizeLibraryText(item.download.title) to normalizeLibraryText(item.download.artist), item)
    }
    fun match(track: Track): LibraryOfflineItem? =
        track.id.trim().takeIf(String::isNotBlank)?.let(byTrackId::get)
            ?: byText[normalizeLibraryText(track.title) to normalizeLibraryText(track.artist)]
                ?.takeIf { sameRecordingLength(it.track, track) }
    fun matchAll(tracks: List<Track>): List<LibraryOfflineItem> =
        tracks.mapNotNull(::match).distinctBy { it.key }

    val mixes = buildList {
        add(OfflineMix.Recent to items.sortedByDescending { it.download.savedAt }.take(OFFLINE_RECENT_LIMIT))
        add(OfflineMix.Favorites to matchAll(favorites))
        add(OfflineMix.MostPlayed to matchAll(mostPlayed))
        add(OfflineMix.SmartOffline to items.filter { it.download.ownership == DownloadOwnership.SMART_OFFLINE })
    }.mapNotNull { (mix, matched) ->
        matched.firstOrNull()?.let { first ->
            OfflineCollection("mix:${mix.name}", "", mix, first.track, matched.map { it.key })
        }
    }

    val artists = items
        .groupBy { item -> normalizeLibraryText(splitLibraryArtists(item.download.artist).firstOrNull().orEmpty()) }
        .filterKeys(String::isNotBlank)
        .map { (key, matched) ->
            val name = splitLibraryArtists(matched.first().download.artist).first()
            OfflineCollection("artist:$key", name, null, matched.first().track, matched.map { it.key })
        }
        .sortedWith(compareByDescending<OfflineCollection> { it.itemKeys.size }.thenBy { normalizeLibraryText(it.title) })

    val offlinePlaylists = playlists
        .filterNot { it.hidden }
        .mapNotNull { playlist ->
            val matched = matchAll(playlist.tracks)
            matched.firstOrNull()?.let { first ->
                OfflineCollection("playlist:${playlist.id}", playlist.name, null, first.track, matched.map { it.key })
            }
        }

    return OfflineCollections(mixes, artists, offlinePlaylists)
}

private fun sameRecordingLength(downloaded: Track, candidate: Track): Boolean {
    val a = downloaded.durationMs
    val b = candidate.durationMs
    return a <= 0L || b <= 0L || kotlin.math.abs(a - b) <= OFFLINE_DURATION_TOLERANCE_MS
}

private const val OFFLINE_DURATION_TOLERANCE_MS = 5_000L
