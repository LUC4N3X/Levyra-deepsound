package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.data.local.toTrack
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmartPlaylistStore(context: Context) {
    private val db = LevyraDatabase.get(context.applicationContext)
    private val listenEventsDao = db.listenEventsDao()
    private val favoriteTracksDao = db.favoriteTracksDao()
    private val downloadedTracksDao = db.downloadedTracksDao()

    suspend fun getMostPlayedPlaylist(limit: Int = 30): Playlist = withContext(Dispatchers.IO) {
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24L * 60L * 60L * 1000L
        val events = listenEventsDao.since(thirtyDaysAgo)
        val trackCounts = events.groupingBy { it.trackId }.eachCount()
        
        val topTrackIds = trackCounts.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }

        val favorites = favoriteTracksDao.all().associateBy { it.trackId }
        val tracks = topTrackIds.mapNotNull { id ->
            favorites[id]?.toTrack() ?: events.firstOrNull { it.trackId == id }?.let { event ->
                Track(
                    id = event.trackId,
                    title = event.title,
                    artist = event.artist,
                    durationSeconds = event.durationSeconds,
                    thumbnailUrl = event.thumbnailUrl,
                    largeThumbnailUrl = event.thumbnailUrl
                )
            }
        }

        val cover = tracks.firstOrNull()?.largeThumbnailUrl?.ifBlank { tracks.firstOrNull()?.thumbnailUrl }.orEmpty()
        Playlist(
            id = "smart_most_played",
            name = "Più Ascoltati",
            coverUrl = cover,
            tracks = tracks,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun getRecentlyAddedFavoritesPlaylist(limit: Int = 30): Playlist = withContext(Dispatchers.IO) {
        val favorites = favoriteTracksDao.all()
            .take(limit)
            .map { it.toTrack() }

        val cover = favorites.firstOrNull()?.largeThumbnailUrl?.ifBlank { favorites.firstOrNull()?.thumbnailUrl }.orEmpty()
        Playlist(
            id = "smart_recently_added",
            name = "Preferiti Recenti",
            coverUrl = cover,
            tracks = favorites,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun getRecentlyDownloadedPlaylist(limit: Int = 30): Playlist = withContext(Dispatchers.IO) {
        val downloads = downloadedTracksDao.all()
            .take(limit)
            .map { it.toTrack() }

        val cover = downloads.firstOrNull()?.largeThumbnailUrl?.ifBlank { downloads.firstOrNull()?.thumbnailUrl }.orEmpty()
        Playlist(
            id = "smart_downloads",
            name = "Download Offline",
            coverUrl = cover,
            tracks = downloads,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
}
