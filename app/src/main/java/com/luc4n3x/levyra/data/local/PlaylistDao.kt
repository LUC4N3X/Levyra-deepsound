package com.luc4n3x.levyra.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.luc4n3x.levyra.domain.PlaylistLayoutSlot
import com.luc4n3x.levyra.domain.layoutAlbumsAroundTracks

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

    @Query("SELECT * FROM playlist_albums WHERE playlistId = :playlistId ORDER BY position ASC")
    abstract suspend fun albumsOf(playlistId: String): List<PlaylistAlbumEntity>

    @Query("SELECT MAX(position) FROM playlist_albums WHERE playlistId = :playlistId")
    abstract suspend fun maxAlbumPosition(playlistId: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAlbums(albums: List<PlaylistAlbumEntity>)

    @Query("DELETE FROM playlist_albums WHERE playlistId = :playlistId AND entryId IN (:entryIds)")
    abstract suspend fun deleteAlbumEntries(playlistId: String, entryIds: Collection<String>)

    @Query("DELETE FROM playlist_albums WHERE playlistId = :playlistId")
    abstract suspend fun clearAlbums(playlistId: String)

    @Query("UPDATE playlist_albums SET position = :position WHERE playlistId = :playlistId AND entryId = :entryId")
    abstract suspend fun updateAlbumPosition(playlistId: String, entryId: String, position: Int)

    @Query("SELECT COUNT(*) FROM playlist_albums WHERE playlistId = :playlistId AND albumKey = :albumKey")
    abstract suspend fun albumKeyCount(playlistId: String, albumKey: String): Int

    open suspend fun nextPosition(playlistId: String): Int =
        maxOf(maxPosition(playlistId) ?: -1, maxAlbumPosition(playlistId) ?: -1) + 1

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

        val albums = albumsOf(playlistId)
        val slots = layoutAlbumsAroundTracks(layoutSlots(existing, albums), orderedEntries.map { it.entryId })
        if (applyPositions(playlistId, slots, existing, albums)) touch(playlistId, System.currentTimeMillis())
    }

    @Transaction
    open suspend fun reorderItems(playlistId: String, ordered: List<PlaylistLayoutSlot>): Boolean {
        val tracks = tracksOf(playlistId)
        val albums = albumsOf(playlistId)
        val expected = layoutSlots(tracks, albums)
        if (ordered.size != expected.size || ordered.toSet() != expected.toSet()) return false
        if (applyPositions(playlistId, ordered, tracks, albums)) touch(playlistId, System.currentTimeMillis())
        return true
    }

    @Transaction
    open suspend fun addAlbum(album: PlaylistAlbumEntity): Boolean {
        if (playlist(album.playlistId) == null) return false
        if (album.albumKey.isNotBlank() && albumKeyCount(album.playlistId, album.albumKey) > 0) return false
        insertAlbums(listOf(album.copy(position = nextPosition(album.playlistId))))
        return true
    }

    @Transaction
    open suspend fun removeEntries(playlistId: String, trackEntryIds: Set<String>, albumEntryIds: Set<String>) {
        if (trackEntryIds.isEmpty() && albumEntryIds.isEmpty()) return
        if (albumEntryIds.isNotEmpty()) deleteAlbumEntries(playlistId, albumEntryIds)
        val remaining = tracksOf(playlistId).filterNot { it.entryId in trackEntryIds }
        writeTracksKeepingAlbums(playlistId, remaining)
        touch(playlistId, System.currentTimeMillis())
    }

    @Transaction
    open suspend fun replaceAlbumTracks(playlistId: String, entryId: String, tracksJson: String) {
        val current = albumsOf(playlistId).firstOrNull { it.entryId == entryId } ?: return
        if (current.tracksJson == tracksJson) return
        insertAlbums(listOf(current.copy(tracksJson = tracksJson)))
    }

    @Transaction
    open suspend fun restoreLayout(
        playlist: PlaylistEntity,
        tracks: List<PlaylistTrackEntity>,
        albums: List<PlaylistAlbumEntity>
    ): Boolean {
        if (updatePlaylist(playlist) == 0) return false
        clearTracks(playlist.id)
        clearAlbums(playlist.id)
        if (tracks.isNotEmpty()) insertTracks(tracks)
        if (albums.isNotEmpty()) insertAlbums(albums)
        return true
    }

    private suspend fun writeTracksKeepingAlbums(playlistId: String, tracks: List<PlaylistTrackEntity>) {
        val previous = tracksOf(playlistId)
        val albums = albumsOf(playlistId)
        clearTracks(playlistId)
        if (albums.isEmpty()) {
            if (tracks.isNotEmpty()) insertTracks(tracks.mapIndexed { index, entity -> entity.copy(position = index) })
            return
        }
        val slots = layoutAlbumsAroundTracks(layoutSlots(previous, albums), tracks.map { it.entryId })
        val trackPositions = HashMap<String, Int>()
        val albumPositions = HashMap<String, Int>()
        slots.forEachIndexed { index, slot ->
            if (slot.isAlbum) albumPositions[slot.entryId] = index else trackPositions[slot.entryId] = index
        }
        if (tracks.isNotEmpty()) {
            insertTracks(tracks.mapIndexed { index, entity ->
                entity.copy(position = trackPositions[entity.entryId] ?: (slots.size + index))
            })
        }
        albums.forEach { album ->
            val position = albumPositions[album.entryId] ?: return@forEach
            if (position != album.position) updateAlbumPosition(playlistId, album.entryId, position)
        }
    }

    private suspend fun applyPositions(
        playlistId: String,
        slots: List<PlaylistLayoutSlot>,
        tracks: List<PlaylistTrackEntity>,
        albums: List<PlaylistAlbumEntity>
    ): Boolean {
        val trackPositions = tracks.associate { it.entryId to it.position }
        val albumPositions = albums.associate { it.entryId to it.position }
        var changed = false
        slots.forEachIndexed { position, slot ->
            if (slot.isAlbum) {
                if (albumPositions[slot.entryId] != position) {
                    updateAlbumPosition(playlistId, slot.entryId, position)
                    changed = true
                }
            } else if (trackPositions[slot.entryId] != position) {
                updateEntryPosition(playlistId, slot.entryId, position)
                changed = true
            }
        }
        return changed
    }

    /** Riscrive l'intero ordine di una playlist (usato dopo un riordino o rimozione). */
    @Transaction
    open suspend fun replaceTracks(playlistId: String, tracks: List<PlaylistTrackEntity>) {
        writeTracksKeepingAlbums(playlistId, tracks)
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
        writeTracksKeepingAlbums(playlistId, tracks)
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
        writeTracksKeepingAlbums(playlistId, remaining)
        touch(playlistId, System.currentTimeMillis())
    }
}

internal fun layoutSlots(
    tracks: List<PlaylistTrackEntity>,
    albums: List<PlaylistAlbumEntity>
): List<PlaylistLayoutSlot> {
    if (albums.isEmpty()) return tracks.sortedBy { it.position }.map { PlaylistLayoutSlot(it.entryId, isAlbum = false) }
    val rows = tracks.map { Triple(it.position, 0, PlaylistLayoutSlot(it.entryId, isAlbum = false)) } +
        albums.map { Triple(it.position, 1, PlaylistLayoutSlot(it.entryId, isAlbum = true)) }
    return rows.sortedWith(compareBy({ it.first }, { it.second })).map { it.third }
}

internal fun albumTrackOffsets(
    tracks: List<PlaylistTrackEntity>,
    albums: List<PlaylistAlbumEntity>
): Map<String, Int> {
    if (albums.isEmpty()) return emptyMap()
    val trackPositions = tracks.map { it.position }.sorted()
    return albums.associate { album ->
        var count = 0
        while (count < trackPositions.size && trackPositions[count] < album.position) count++
        album.entryId to count
    }
}
