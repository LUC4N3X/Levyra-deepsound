package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.data.local.PlaylistEntity
import com.luc4n3x.levyra.data.local.toPlaylistTrackEntity
import com.luc4n3x.levyra.data.local.toTrack
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.Track
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

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
        dao.createPlaylistWithTracks(
            playlist = PlaylistEntity(id, name.trim().ifBlank { "Playlist" }, cover, now, now),
            tracks = firstTrack?.let { listOf(it.toPlaylistTrackEntity(id, 0, now)) }.orEmpty()
        )
        val tracks = firstTrack?.let { listOf(it) } ?: emptyList()
        Playlist(id, name.trim().ifBlank { "Playlist" }, cover, tracks, now, now)
    }

    suspend fun createWithTracks(name: String, tracks: List<Track>): Playlist = withContext(Dispatchers.IO) {
        val cleanTracks = tracks
            .filter { it.id.isNotBlank() && it.title.isNotBlank() }
            .distinctBy { it.id }
        require(cleanTracks.isNotEmpty()) { "Cannot create a playlist without valid tracks" }

        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val cleanName = name.trim().ifBlank { "Playlist" }
        val cover = cleanTracks.firstNotNullOfOrNull { track ->
            track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }.takeIf(String::isNotBlank)
        }.orEmpty()
        val entity = PlaylistEntity(id, cleanName, cover, now, now)
        val trackEntities = cleanTracks.mapIndexed { index, track ->
            track.toPlaylistTrackEntity(id, index, now)
        }

        // Room executes both the playlist row and every track row in one transaction.
        // If insertion fails or the coroutine is cancelled, nothing is partially persisted.
        dao.createPlaylistWithTracks(entity, trackEntities)
        Playlist(id, cleanName, cover, cleanTracks, now, now)
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

    suspend fun addTracks(playlistId: String, tracks: List<Track>) = withContext(Dispatchers.IO) {
        val cleanTracks = tracks
            .filter { it.id.isNotBlank() && it.title.isNotBlank() }
            .distinctBy { it.id }
        if (cleanTracks.isEmpty()) return@withContext
        val existingIds = dao.tracksOf(playlistId).mapTo(hashSetOf()) { it.trackId }
        val pending = cleanTracks.filterNot { it.id in existingIds }
        if (pending.isEmpty()) return@withContext
        val now = System.currentTimeMillis()
        val startPosition = (dao.maxPosition(playlistId) ?: -1) + 1
        dao.insertTracks(pending.mapIndexed { index, track ->
            track.toPlaylistTrackEntity(playlistId, startPosition + index, now)
        })
        val cover = pending.firstNotNullOfOrNull { track ->
            track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }.takeIf(String::isNotBlank)
        }.orEmpty()
        if (cover.isNotBlank()) dao.updateCover(playlistId, cover, now) else dao.touch(playlistId, now)
    }

    suspend fun removeTrack(playlistId: String, trackId: String) = withContext(Dispatchers.IO) {
        dao.removeTracksAndCompact(playlistId, setOf(trackId))
    }

    suspend fun removeTracks(playlistId: String, trackIds: Set<String>) = withContext(Dispatchers.IO) {
        if (trackIds.isEmpty()) return@withContext
        dao.removeTracksAndCompact(playlistId, trackIds)
    }

    /** Riscrive l'ordine completo senza alterare metadati o data di aggiunta. */
    suspend fun reorder(playlistId: String, orderedTracks: List<Track>) = withContext(Dispatchers.IO) {
        val existing = dao.tracksOf(playlistId)
        val existingById = existing.associateBy { it.trackId }
        val orderedIds = orderedTracks.map { it.id }.filter(String::isNotBlank)
        if (orderedIds.size != existing.size || orderedIds.toSet().size != orderedIds.size) return@withContext
        if (orderedIds.toSet() != existingById.keys) return@withContext
        dao.replaceTracks(playlistId, orderedIds.mapIndexed { index, trackId ->
            existingById.getValue(trackId).copy(position = index)
        })
    }

    suspend fun updateTrackMetadata(playlists: List<Playlist>) = withContext(Dispatchers.IO) {
        runCatching {
            playlists.distinctBy { it.id }.forEach { playlist ->
                val existing = dao.tracksOf(playlist.id).associateBy { it.trackId }
                val updates = playlist.tracks.mapNotNull { track ->
                    existing[track.id]?.let { stored ->
                        track.toPlaylistTrackEntity(playlist.id, stored.position, stored.addedAt)
                    }
                }
                if (updates.isNotEmpty()) dao.insertTracks(updates)
            }
        }.onFailure { Timber.w(it, "Playlist metadata update failed") }
    }

    private fun PlaylistEntity.toPlaylist(tracks: List<Track>): Playlist =
        Playlist(id, name, coverUrl, tracks, createdAt, updatedAt)
}
