package com.luc4n3x.levyra.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.coroutines.flow.Flow
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Entity(
    tableName = "offline_download_tasks",
    indices = [Index(value = ["batchKey"])]
)
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
    val updatedAt: Long,
    val batchKey: String = "",
    val batchTitle: String = "",
    val batchKind: String = "",
    val batchArtworkUrl: String = "",
    val batchPosition: Int = 0
)

data class OfflineDownloadBatchRow(
    val batchKey: String,
    val batchTitle: String,
    val batchKind: String,
    val batchArtworkUrl: String,
    val total: Int,
    val completed: Int,
    val failed: Int,
    val active: Int,
    val progressSum: Int,
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

    @Query(
        """
        SELECT batchKey,
               MAX(batchTitle) AS batchTitle,
               MAX(batchKind) AS batchKind,
               MAX(batchArtworkUrl) AS batchArtworkUrl,
               COUNT(*) AS total,
               SUM(CASE WHEN state = 'SUCCEEDED' THEN 1 ELSE 0 END) AS completed,
               SUM(CASE WHEN state = 'FAILED' THEN 1 ELSE 0 END) AS failed,
               SUM(CASE WHEN state IN ('QUEUED','RUNNING','PAUSED','RETRYING') THEN 1 ELSE 0 END) AS active,
               SUM(CASE WHEN state = 'SUCCEEDED' THEN 100 ELSE progress END) AS progressSum,
               MAX(updatedAt) AS updatedAt
        FROM offline_download_tasks
        WHERE batchKey <> ''
        GROUP BY batchKey
        ORDER BY MIN(createdAt) ASC
        """
    )
    fun observeBatches(): Flow<List<OfflineDownloadBatchRow>>

    @Query("SELECT * FROM offline_download_tasks WHERE batchKey = :batchKey ORDER BY batchPosition ASC")
    suspend fun batchTasks(batchKey: String): List<OfflineDownloadTaskEntity>

    @Query("DELETE FROM offline_download_tasks WHERE batchKey = :batchKey")
    suspend fun deleteBatch(batchKey: String)

    @Query(
        """
        DELETE FROM offline_download_tasks
        WHERE state IN ('SUCCEEDED','CANCELLED')
          AND updatedAt < :before
          AND (
            batchKey = ''
            OR batchKey NOT IN (
              SELECT batchKey FROM offline_download_tasks
              WHERE batchKey <> '' AND state IN ('QUEUED','RUNNING','PAUSED','RETRYING','FAILED')
            )
          )
        """
    )
    suspend fun prune(before: Long)
}
