package com.luc4n3x.levyra.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalMediaDao {
    @Query("SELECT * FROM local_media WHERE available = 1 ORDER BY title COLLATE NOCASE ASC, id ASC")
    fun observeAvailable(): Flow<List<LocalMediaEntity>>

    @Query("SELECT * FROM local_media")
    suspend fun all(): List<LocalMediaEntity>

    @Query("SELECT COUNT(*) FROM local_media")
    suspend fun count(): Int

    @Query("SELECT contentUri FROM local_media WHERE available = 0 AND contentUri IN (:uris)")
    suspend fun unavailableAmong(uris: List<String>): List<String>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(rows: List<LocalMediaEntity>)

    @Update
    suspend fun updateAll(rows: List<LocalMediaEntity>)

    @Query("UPDATE local_media SET available = 0, missingSince = :now WHERE id IN (:ids) AND available = 1")
    suspend fun markMissing(ids: List<Long>, now: Long)

    @Query("DELETE FROM local_media WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Transaction
    suspend fun apply(
        inserts: List<LocalMediaEntity>,
        updates: List<LocalMediaEntity>,
        missingIds: List<Long>,
        deleteIds: List<Long>,
        now: Long
    ) {
        deleteIds.chunked(SQL_CHUNK).forEach { deleteByIds(it) }
        updates.chunked(SQL_CHUNK).forEach { updateAll(it) }
        missingIds.chunked(SQL_CHUNK).forEach { markMissing(it, now) }
        inserts.chunked(SQL_CHUNK).forEach { insertAll(it) }
    }

    companion object {
        const val SQL_CHUNK = 400
    }
}
