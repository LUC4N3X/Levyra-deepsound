package com.luc4n3x.levyra.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface PlaybackQueueDao {
    @Query("SELECT * FROM playback_queue_items ORDER BY position ASC")
    suspend fun items(): List<PlaybackQueueItemEntity>

    @Query("SELECT * FROM playback_queue_state WHERE singletonId = 1 LIMIT 1")
    suspend fun state(): PlaybackQueueStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<PlaybackQueueItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertState(state: PlaybackQueueStateEntity)

    @Query("DELETE FROM playback_queue_items")
    suspend fun clearItems()

    @Query("UPDATE playback_queue_state SET positionMs = :positionMs, updatedAt = :updatedAt WHERE singletonId = 1")
    suspend fun updatePosition(positionMs: Long, updatedAt: Long)

    @Transaction
    suspend fun replace(items: List<PlaybackQueueItemEntity>, state: PlaybackQueueStateEntity) {
        clearItems()
        if (items.isNotEmpty()) insertItems(items)
        insertState(state)
    }
}
