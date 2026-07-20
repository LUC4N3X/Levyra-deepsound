package com.luc4n3x.levyra.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.coroutines.flow.Flow
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Entity(tableName = "offline_download_tasks")
data class OfflineDownloadTaskEntity(
    @PrimaryKey val taskKey: String,
    val trackId: String,
    val payload: String,
    val title: String,
    val artist: String,
    val state: String,
    val progress: Int,
    val workId: String,
    val error: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Dao
interface OfflineDownloadTasksDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: OfflineDownloadTaskEntity)

    @Query("SELECT * FROM offline_download_tasks WHERE state IN ('QUEUED','RUNNING','PAUSED','RETRYING','FAILED') ORDER BY createdAt ASC")
    fun observeActive(): Flow<List<OfflineDownloadTaskEntity>>

    @Query("SELECT * FROM offline_download_tasks WHERE taskKey = :taskKey LIMIT 1")
    suspend fun byKey(taskKey: String): OfflineDownloadTaskEntity?

    @Query("UPDATE offline_download_tasks SET state = :state, progress = :progress, error = :error, updatedAt = :updatedAt WHERE taskKey = :taskKey")
    suspend fun updateState(taskKey: String, state: String, progress: Int, error: String, updatedAt: Long)

    @Query("UPDATE offline_download_tasks SET state = :state, progress = :progress, error = :error, updatedAt = :updatedAt WHERE taskKey = :taskKey AND workId = :workId")
    suspend fun updateStateForWork(taskKey: String, workId: String, state: String, progress: Int, error: String, updatedAt: Long): Int

    @Query("UPDATE offline_download_tasks SET progress = :progress, updatedAt = :updatedAt WHERE taskKey = :taskKey AND workId = :workId AND state = 'RUNNING'")
    suspend fun updateRunningProgress(taskKey: String, workId: String, progress: Int, updatedAt: Long): Int

    @Query("DELETE FROM offline_download_tasks WHERE taskKey = :taskKey")
    suspend fun delete(taskKey: String)

    @Query("DELETE FROM offline_download_tasks WHERE state IN ('SUCCEEDED','CANCELLED') AND updatedAt < :before")
    suspend fun prune(before: Long)
}
