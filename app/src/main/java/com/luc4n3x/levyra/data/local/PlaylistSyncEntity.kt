package com.luc4n3x.levyra.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
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
    indices = [Index(value = ["accountId", "remotePlaylistId"], unique = true)]
)
data class PlaylistSyncEntity(
    @PrimaryKey val playlistId: String,
    val accountId: String,
    val remotePlaylistId: String,
    val localTitle: String,
    val remoteTitle: String,
    val videoIds: String,
    val autoSync: Boolean,
    val synced: Boolean,
    val localUpdatedAt: Long,
    val lastSyncedAt: Long,
    val lastError: String,
    val unmatched: Int,
    val keptLocal: Int
)

@Dao
abstract class PlaylistSyncDao {
    @Query("SELECT * FROM playlist_sync")
    abstract fun observeAll(): Flow<List<PlaylistSyncEntity>>

    @Query("SELECT * FROM playlist_sync")
    abstract suspend fun all(): List<PlaylistSyncEntity>

    @Query("SELECT * FROM playlist_sync WHERE playlistId = :playlistId")
    abstract suspend fun get(playlistId: String): PlaylistSyncEntity?

    @Query("SELECT * FROM playlist_sync WHERE accountId = :accountId AND remotePlaylistId = :remotePlaylistId")
    abstract suspend fun findByRemote(accountId: String, remotePlaylistId: String): PlaylistSyncEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(row: PlaylistSyncEntity)

    @Transaction
    open suspend fun claim(row: PlaylistSyncEntity): Boolean {
        val taken = findByRemote(row.accountId, row.remotePlaylistId)
        if (taken != null && taken.playlistId != row.playlistId) return false
        upsert(row)
        return true
    }

    @Query(
        "UPDATE playlist_sync SET localTitle = :localTitle, remoteTitle = :remoteTitle, videoIds = :videoIds, synced = 1, " +
            "localUpdatedAt = :localUpdatedAt, lastSyncedAt = :lastSyncedAt, lastError = '', unmatched = :unmatched, keptLocal = :keptLocal " +
            "WHERE playlistId = :playlistId AND accountId = :accountId AND remotePlaylistId = :remotePlaylistId"
    )
    abstract suspend fun recordSync(
        playlistId: String,
        accountId: String,
        remotePlaylistId: String,
        localTitle: String,
        remoteTitle: String,
        videoIds: String,
        localUpdatedAt: Long,
        lastSyncedAt: Long,
        unmatched: Int,
        keptLocal: Int
    )

    @Query("UPDATE playlist_sync SET lastError = :error WHERE playlistId = :playlistId AND accountId = :accountId AND remotePlaylistId = :remotePlaylistId")
    abstract suspend fun recordError(playlistId: String, accountId: String, remotePlaylistId: String, error: String)

    @Query("UPDATE playlist_sync SET autoSync = :enabled WHERE playlistId = :playlistId")
    abstract suspend fun setAutoSync(playlistId: String, enabled: Boolean)

    @Query("DELETE FROM playlist_sync WHERE playlistId = :playlistId")
    abstract suspend fun delete(playlistId: String)

    @Query("DELETE FROM playlist_sync")
    abstract suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertAll(rows: List<PlaylistSyncEntity>)

    @Transaction
    open suspend fun replaceAll(rows: List<PlaylistSyncEntity>) {
        clear()
        upsertAll(rows)
    }
}
