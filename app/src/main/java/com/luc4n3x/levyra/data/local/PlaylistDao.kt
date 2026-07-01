package com.luc4n3x.levyra.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTracks(tracks: List<PlaylistTrackEntity>)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    abstract suspend fun deletePlaylist(playlistId: String)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    abstract suspend fun removeTrack(playlistId: String, trackId: String)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    abstract suspend fun clearTracks(playlistId: String)

    @Query("UPDATE playlists SET name = :name, updatedAt = :updatedAt WHERE id = :playlistId")
    abstract suspend fun rename(playlistId: String, name: String, updatedAt: Long)

    @Query("UPDATE playlists SET coverUrl = :coverUrl, updatedAt = :updatedAt WHERE id = :playlistId")
    abstract suspend fun updateCover(playlistId: String, coverUrl: String, updatedAt: Long)

    @Query("UPDATE playlists SET updatedAt = :updatedAt WHERE id = :playlistId")
    abstract suspend fun touch(playlistId: String, updatedAt: Long)

    /** Riscrive l'intero ordine di una playlist (usato dopo un riordino o rimozione). */
    @Transaction
    open suspend fun replaceTracks(playlistId: String, tracks: List<PlaylistTrackEntity>) {
        clearTracks(playlistId)
        if (tracks.isNotEmpty()) insertTracks(tracks)
        touch(playlistId, System.currentTimeMillis())
    }
}
