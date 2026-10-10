package com.luc4n3x.levyra.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
abstract class PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    abstract suspend fun allPlaylists(): List<PlaylistEntity>

    @Query("SELECT * FROM playlists WHERE id = :playlistId LIMIT 1")
    abstract suspend fun playlist(playlistId: String): PlaylistEntity?

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position ASC")
    abstract suspend fun tracksOf(playlistId: String): List<PlaylistTrackEntity>

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :playlistId")
    abstract suspend fun countOf(playlistId: String): Int

    @Query("SELECT MAX(position) FROM playlist_tracks WHERE playlistId = :playlistId")
    abstract suspend fun maxPosition(playlistId: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertPlaylist(playlist: PlaylistEntity)

    @Update
    abstract suspend fun updatePlaylist(playlist: PlaylistEntity): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTracks(tracks: List<PlaylistTrackEntity>)

    @Query("DELETE FROM playlists")
    abstract suspend fun clearAll()

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    abstract suspend fun deletePlaylist(playlistId: String)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    abstract suspend fun removeTrack(playlistId: String, trackId: String)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND entryId = :entryId")
    abstract suspend fun removeEntry(playlistId: String, entryId: String)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    abstract suspend fun clearTracks(playlistId: String)

    @Query("UPDATE playlists SET name = :name, updatedAt = :updatedAt WHERE id = :playlistId")
    abstract suspend fun rename(playlistId: String, name: String, updatedAt: Long)

    @Query(
        "UPDATE playlists SET coverUrl = CASE WHEN coverMode = 'AUTO' THEN :coverUrl ELSE coverUrl END, " +
            "updatedAt = :updatedAt WHERE id = :playlistId"
    )
    abstract suspend fun updateAutomaticCover(playlistId: String, coverUrl: String, updatedAt: Long)

    @Query("UPDATE playlists SET coverUrl = :coverUrl, coverMode = 'CUSTOM', updatedAt = :updatedAt WHERE id = :playlistId")
    abstract suspend fun updateCustomCover(playlistId: String, coverUrl: String, updatedAt: Long)

    @Query("UPDATE playlists SET coverUrl = :coverUrl, coverMode = 'AUTO', updatedAt = :updatedAt WHERE id = :playlistId")
    abstract suspend fun resetCover(playlistId: String, coverUrl: String, updatedAt: Long)

    @Query("UPDATE playlists SET updatedAt = :updatedAt WHERE id = :playlistId")
    abstract suspend fun touch(playlistId: String, updatedAt: Long)

    @Query("UPDATE playlists SET hidden = :hidden, updatedAt = :updatedAt WHERE id = :playlistId")
    abstract suspend fun setHidden(playlistId: String, hidden: Boolean, updatedAt: Long)

    @Query("UPDATE playlist_tracks SET position = :position WHERE playlistId = :playlistId AND entryId = :entryId")
    abstract suspend fun updateEntryPosition(playlistId: String, entryId: String, position: Int)

    @Transaction
    open suspend fun createPlaylistWithTracks(
        playlist: PlaylistEntity,
        tracks: List<PlaylistTrackEntity>
    ) {
        upsertPlaylist(playlist)
        if (tracks.isNotEmpty()) insertTracks(tracks)
    }

    /**
     * Applica un nuovo ordine aggiornando soltanto le righe la cui posizione cambia.
     * Le occorrenze duplicate dello stesso trackId restano righe distinte grazie a entryId.
     */
    @Transaction
    open suspend fun reorderTracks(playlistId: String, orderedTrackIds: List<String>) {
        val existing = tracksOf(playlistId)
        if (orderedTrackIds.size != existing.size) return

        val pools = existing
            .groupBy { it.trackId }
            .mapValues { (_, rows) -> rows.sortedBy { it.position }.toMutableList() }
            .toMutableMap()
        val orderedEntries = ArrayList<PlaylistTrackEntity>(orderedTrackIds.size)
        for (trackId in orderedTrackIds) {
            val bucket = pools[trackId] ?: return
            if (bucket.isEmpty()) return
            orderedEntries += bucket.removeAt(0)
        }
        if (pools.values.any { it.isNotEmpty() }) return

        var changed = false
        orderedEntries.forEachIndexed { position, entry ->
            if (entry.position != position) {
                updateEntryPosition(playlistId, entry.entryId, position)
                changed = true
            }
        }
        if (changed) touch(playlistId, System.currentTimeMillis())
    }

    /** Riscrive l'intero ordine di una playlist (usato dopo un riordino o rimozione). */
    @Transaction
    open suspend fun replaceTracks(playlistId: String, tracks: List<PlaylistTrackEntity>) {
        clearTracks(playlistId)
        if (tracks.isNotEmpty()) insertTracks(tracks)
        touch(playlistId, System.currentTimeMillis())
    }

    @Transaction
    open suspend fun applyStudioEdit(
        playlistId: String,
        name: String,
        tracks: List<PlaylistTrackEntity>,
        automaticCover: String,
        updatedAt: Long
    ): Boolean {
        if (playlist(playlistId) == null) return false
        rename(playlistId, name, updatedAt)
        clearTracks(playlistId)
        if (tracks.isNotEmpty()) insertTracks(tracks)
        updateAutomaticCover(playlistId, automaticCover, updatedAt)
        return true
    }

    @Transaction
    open suspend fun replaceTrackInPlace(
        playlistId: String,
        oldTrackId: String,
        oldEntryId: String?,
        replacement: PlaylistTrackEntity
    ): Boolean {
        val current = tracksOf(playlistId)
        val previous = oldEntryId?.takeIf(String::isNotBlank)?.let { entryId ->
            current.firstOrNull { it.entryId == entryId }
        } ?: current.firstOrNull { it.trackId == oldTrackId } ?: return false
        removeEntry(playlistId, previous.entryId)
        insertTracks(
            listOf(
                replacement.copy(
                    entryId = previous.entryId,
                    playlistId = playlistId,
                    position = previous.position,
                    addedAt = previous.addedAt
                )
            )
        )
        touch(playlistId, System.currentTimeMillis())
        return true
    }

    @Transaction
    open suspend fun removeTracksAndCompact(playlistId: String, trackIds: Set<String>) {
        if (trackIds.isEmpty()) return
        val remaining = tracksOf(playlistId).filterNot { it.trackId in trackIds }
        clearTracks(playlistId)
        if (remaining.isNotEmpty()) insertTracks(remaining.mapIndexed { index, entity -> entity.copy(position = index) })
        touch(playlistId, System.currentTimeMillis())
    }
}