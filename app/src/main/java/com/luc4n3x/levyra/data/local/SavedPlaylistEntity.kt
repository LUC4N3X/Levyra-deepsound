package com.luc4n3x.levyra.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "saved_playlists")
data class SavedPlaylistEntity(
    @PrimaryKey val playlistKey: String,
    val playlistId: String,
    val browseId: String,
    val appleStorefront: String,
    val title: String,
    val author: String,
    val thumbnailUrl: String,
    val trackCountLabel: String,
    val savedAt: Long
)

@Dao
abstract class SavedPlaylistsDao {
    @Query("SELECT * FROM saved_playlists ORDER BY savedAt DESC")
    abstract fun observeAll(): Flow<List<SavedPlaylistEntity>>

    @Query("SELECT * FROM saved_playlists ORDER BY savedAt DESC")
    abstract suspend fun all(): List<SavedPlaylistEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_playlists WHERE playlistKey = :playlistKey)")
    abstract suspend fun contains(playlistKey: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(row: SavedPlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertAll(rows: List<SavedPlaylistEntity>)

    @Query("DELETE FROM saved_playlists WHERE playlistKey = :playlistKey")
    abstract suspend fun delete(playlistKey: String)

    @Query("DELETE FROM saved_playlists")
    abstract suspend fun clear()

    @Transaction
    open suspend fun toggle(row: SavedPlaylistEntity): Boolean =
        if (contains(row.playlistKey)) {
            delete(row.playlistKey)
            false
        } else {
            upsert(row)
            true
        }

    @Transaction
    open suspend fun replaceAll(rows: List<SavedPlaylistEntity>) {
        clear()
        upsertAll(rows)
    }
}
