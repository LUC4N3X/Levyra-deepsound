package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.data.local.PlaylistEntity
import com.luc4n3x.levyra.data.local.toPlaylistTrackEntity
import com.luc4n3x.levyra.data.local.toTrack
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID

class PlaylistStore(context: Context) {
    private val dao = LevyraDatabase.get(context.applicationContext).playlistDao()

    suspend fun loadAll(): List<Playlist> = withContext(Dispatchers.IO) {
        runCatching {
            dao.allPlaylists().map { entity ->
                val tracks = dao.tracksOf(entity.id).map { it.toTrack() }
                entity.toPlaylist(tracks)
            }
        }.onFailure { Timber.w(it, "Playlist load failed") }.getOrDefault(emptyList())
    }

    suspend fun load(playlistId: String): Playlist? = withContext(Dispatchers.IO) {
        runCatching {
            val entity = dao.playlist(playlistId) ?: return@runCatching null
            entity.toPlaylist(dao.tracksOf(playlistId).map { it.toTrack() })
        }.onFailure { Timber.w(it, "Playlist load failed") }.getOrNull()
    }

    suspend fun create(name: String, firstTrack: Track? = null): Playlist = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val cover = firstTrack?.largeThumbnailUrl?.ifBlank { firstTrack.thumbnailUrl }.orEmpty()
        dao.upsertPlaylist(PlaylistEntity(id, name.trim().ifBlank { "Playlist" }, cover, now, now))
        if (firstTrack != null) {
            dao.insertTracks(listOf(firstTrack.toPlaylistTrackEntity(id, 0, now)))
        }
        val tracks = firstTrack?.let { listOf(it) } ?: emptyList()
        Playlist(id, name.trim().ifBlank { "Playlist" }, cover, tracks, now, now)
    }

    suspend fun rename(playlistId: String, name: String) = withContext(Dispatchers.IO) {
        dao.rename(playlistId, name.trim().ifBlank { "Playlist" }, System.currentTimeMillis())
    }

    suspend fun delete(playlistId: String) = withContext(Dispatchers.IO) {
        // CASCADE elimina anche le tracce collegate.
        dao.deletePlaylist(playlistId)
    }

    /** Aggiunge una traccia in coda. Ignora i duplicati (la PK è playlistId+trackId). */
    suspend fun addTrack(playlistId: String, track: Track) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val nextPos = (dao.maxPosition(playlistId) ?: -1) + 1
        dao.insertTracks(listOf(track.toPlaylistTrackEntity(playlistId, nextPos, now)))
        val cover = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }
        if (cover.isNotBlank()) dao.updateCover(playlistId, cover, now) else dao.touch(playlistId, now)
    }

    suspend fun removeTrack(playlistId: String, trackId: String) = withContext(Dispatchers.IO) {
        dao.removeTrack(playlistId, trackId)
        // ricompatta le posizioni
        val remaining = dao.tracksOf(playlistId)
        dao.replaceTracks(playlistId, remaining.mapIndexed { i, e -> e.copy(position = i) })
    }

    /** Riscrive l'ordine completo (drag & drop). */
    suspend fun reorder(playlistId: String, orderedTracks: List<Track>) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val entities = orderedTracks.mapIndexed { i, t -> t.toPlaylistTrackEntity(playlistId, i, now) }
        dao.replaceTracks(playlistId, entities)
    }

    private fun PlaylistEntity.toPlaylist(tracks: List<Track>): Playlist =
        Playlist(id, name, coverUrl, tracks, createdAt, updatedAt)
}
