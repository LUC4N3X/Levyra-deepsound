package com.luc4n3x.levyra.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "playlist_sync",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountId", "remotePlaylistId"])
    ]
)
data class PlaylistSyncEntity(
    @PrimaryKey val playlistId: String,
    val accountId: String,
    val remotePlaylistId: String,
    @ColumnInfo(defaultValue = "''") val remoteTitle: String = "",
    @ColumnInfo(defaultValue = "''") val syncedVideoIds: String = "",
    @ColumnInfo(defaultValue = "0") val autoSync: Boolean = false,
    @ColumnInfo(defaultValue = "0") val lastSyncedAt: Long = 0L,
    @ColumnInfo(defaultValue = "0") val lastSyncAttemptAt: Long = 0L,
    @ColumnInfo(defaultValue = "''") val lastSyncError: String = "",
    @ColumnInfo(defaultValue = "0") val lastUnmatchedCount: Int = 0
)

@Dao
abstract class PlaylistSyncDao {
    @Query("SELECT * FROM playlist_sync")
    abstract fun observeAll(): Flow<List<PlaylistSyncEntity>>

    @Query("SELECT * FROM playlist_sync")
    abstract suspend fun all(): List<PlaylistSyncEntity>

    @Query("SELECT * FROM playlist_sync WHERE playlistId = :playlistId LIMIT 1")
    abstract suspend fun get(playlistId: String): PlaylistSyncEntity?

    @Query("SELECT * FROM playlist_sync WHERE accountId = :accountId AND remotePlaylistId = :remotePlaylistId LIMIT 1")
    abstract suspend fun findByRemote(accountId: String, remotePlaylistId: String): PlaylistSyncEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(entity: PlaylistSyncEntity)

    @Query("UPDATE playlist_sync SET autoSync = :autoSync WHERE playlistId = :playlistId")
    abstract suspend fun setAutoSync(playlistId: String, autoSync: Boolean)

    @Query("DELETE FROM playlist_sync WHERE playlistId = :playlistId")
    abstract suspend fun delete(playlistId: String)
}
