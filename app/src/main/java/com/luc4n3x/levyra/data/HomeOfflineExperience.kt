package com.luc4n3x.levyra.data

import androidx.compose.runtime.Immutable
import com.luc4n3x.levyra.domain.DownloadedTrack
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.Track

internal object HomeOfflinePolicy {
    fun shouldAttemptRemoteRefresh(deviceOffline: Boolean): Boolean = !deviceOffline

    fun remoteLoading(requested: Boolean, deviceOffline: Boolean): Boolean = requested && !deviceOffline

    fun showOfflineHome(deviceOffline: Boolean): Boolean = deviceOffline

    fun shouldRecoverOnReconnect(previousOffline: Boolean, deviceOffline: Boolean): Boolean =
        previousOffline && !deviceOffline

    fun homeErrorAfterRemoteFailure(deviceOffline: Boolean, fallback: String): String? =
        if (deviceOffline) null else fallback
}

@Immutable
internal data class HomeOfflineContent(
    val downloads: List<Track> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val favorites: List<Track> = emptyList(),
    val recentlyPlayed: List<Track> = emptyList()
) {
    val isEmpty: Boolean
        get() = downloads.isEmpty() &&
            playlists.isEmpty() &&
            favorites.isEmpty() &&
            recentlyPlayed.isEmpty()

    companion object {
        val Empty = HomeOfflineContent()
    }
}

internal object HomeOfflineContentBuilder {
    const val SHELF_LIMIT = 24
    const val PLAYLIST_LIMIT = 12

    fun build(
        deviceOffline: Boolean,
        downloads: List<DownloadedTrack>,
        playlists: List<Playlist>,
        favorites: List<Track>,
        recentListens: List<Track>,
        artworkPool: List<Track>
    ): HomeOfflineContent {
        if (!deviceOffline) return HomeOfflineContent.Empty
        val playableDownloads = downloads.filter { it.uri.isNotBlank() }
        val artworkByIdentity = buildArtworkIndex(artworkPool)
        val offlineTracks = playableDownloads
            .map { download -> download.toOfflineTrack(artworkByIdentity) }
            .distinctBy(::offlineIdentity)
            .take(SHELF_LIMIT)
        val downloadedIdentities = playableDownloads
            .map { download -> trackIdentity(download.title, download.artist) }
            .filter(String::isNotBlank)
            .toSet()
        val downloadedIds = playableDownloads
            .map { download -> download.trackId.trim() }
            .filter(String::isNotBlank)
            .toSet()
        val offlinePlaylists = playlists
            .filterNot { it.hidden }
            .filter { playlist -> playlist.tracks.any { track -> track.isAvailableOffline(downloadedIds, downloadedIdentities) } }
            .take(PLAYLIST_LIMIT)
        return HomeOfflineContent(
            downloads = offlineTracks,
            playlists = offlinePlaylists,
            favorites = favorites.distinctBy(::offlineIdentity).take(SHELF_LIMIT),
            recentlyPlayed = recentListens.distinctBy(::offlineIdentity).take(SHELF_LIMIT)
        )
    }

    private fun Track.isAvailableOffline(downloadedIds: Set<String>, downloadedIdentities: Set<String>): Boolean {
        if (id.trim() in downloadedIds) return true
        val identity = trackIdentity(title, artist)
        return identity.isNotBlank() && identity in downloadedIdentities
    }

    private fun buildArtworkIndex(pool: List<Track>): Map<String, Track> {
        if (pool.isEmpty()) return emptyMap()
        val index = LinkedHashMap<String, Track>()
        pool.forEach { track ->
            if (track.thumbnailUrl.isBlank() && track.largeThumbnailUrl.isBlank()) return@forEach
            val id = track.id.trim()
            if (id.isNotEmpty()) index.putIfAbsent(id, track)
            val identity = trackIdentity(track.title, track.artist)
            if (identity.isNotEmpty()) index.putIfAbsent(identity, track)
        }
        return index
    }

    private fun DownloadedTrack.toOfflineTrack(artworkIndex: Map<String, Track>): Track {
        val reference = artworkIndex[trackId.trim()] ?: artworkIndex[trackIdentity(title, artist)]
        return Track(
            id = trackId.ifBlank { "offline-$id" },
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            streamUrl = uri,
            videoUrl = "",
            thumbnailUrl = reference?.thumbnailUrl.orEmpty(),
            largeThumbnailUrl = reference?.largeThumbnailUrl.orEmpty(),
            source = "Offline",
            moodTags = emptySet(),
            energy = 0,
            vocal = 0,
            replayScore = 0,
            cacheScore = 0,
            accentStart = reference?.accentStart ?: 0,
            accentEnd = reference?.accentEnd ?: 0
        )
    }

    private fun offlineIdentity(track: Track): String =
        trackIdentity(track.title, track.artist).ifBlank { track.id.trim().lowercase() }

    private fun trackIdentity(title: String, artist: String): String {
        val normalizedTitle = title.trim().lowercase()
        val normalizedArtist = artist.trim().lowercase()
        if (normalizedTitle.isEmpty() && normalizedArtist.isEmpty()) return ""
        return "$normalizedTitle|$normalizedArtist"
    }
}
