package com.luc4n3x.levyra.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackQueueDao {
    @Query("SELECT * FROM playback_queue_items WHERE spaceId = :spaceId ORDER BY position ASC")
    suspend fun items(spaceId: String): List<PlaybackQueueItemEntity>

    @Query("SELECT * FROM playback_queue_items ORDER BY spaceId ASC, position ASC")
    suspend fun allItems(): List<PlaybackQueueItemEntity>

    @Query("SELECT * FROM playback_queue_state WHERE spaceId = :spaceId LIMIT 1")
    suspend fun state(spaceId: String): PlaybackQueueStateEntity?

    @Query("SELECT * FROM playback_queue_state")
    suspend fun allStates(): List<PlaybackQueueStateEntity>

    @Query("SELECT * FROM queue_spaces ORDER BY lastActiveAt DESC, createdAt DESC")
    fun observeSpaces(): Flow<List<QueueSpaceEntity>>

    @Query("SELECT * FROM queue_spaces ORDER BY lastActiveAt DESC, createdAt DESC")
    suspend fun spaces(): List<QueueSpaceEntity>

    @Query("SELECT * FROM queue_spaces WHERE id = :spaceId LIMIT 1")
    suspend fun space(spaceId: String): QueueSpaceEntity?

    @Query("SELECT * FROM queue_spaces WHERE isActive = 1 ORDER BY lastActiveAt DESC LIMIT 1")
    suspend fun activeSpace(): QueueSpaceEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSpace(space: QueueSpaceEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSpaces(spaces: List<QueueSpaceEntity>)

    @Query("UPDATE queue_spaces SET name = :name, updatedAt = :updatedAt WHERE id = :spaceId")
    suspend fun renameSpace(spaceId: String, name: String, updatedAt: Long): Int

    @Query(
        "UPDATE queue_spaces SET trackCount = :trackCount, durationMs = :durationMs, " +
            "artworkUrls = :artworkUrls, updatedAt = :updatedAt WHERE id = :spaceId"
    )
    suspend fun updateSpaceSummary(
        spaceId: String,
        trackCount: Int,
        durationMs: Long,
        artworkUrls: String,
        updatedAt: Long
    ): Int

    @Query(
        "UPDATE queue_spaces SET isActive = (id = :spaceId), " +
            "lastActiveAt = CASE WHEN id = :spaceId THEN :now ELSE lastActiveAt END"
    )
    suspend fun markActive(spaceId: String, now: Long)

    @Query("DELETE FROM queue_spaces WHERE id = :spaceId")
    suspend fun deleteSpaceRow(spaceId: String): Int

    @Query("DELETE FROM queue_spaces")
    suspend fun deleteAllSpaces()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<PlaybackQueueItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertState(state: PlaybackQueueStateEntity)

    @Query("DELETE FROM playback_queue_items WHERE spaceId = :spaceId")
    suspend fun clearItems(spaceId: String)

    @Query("DELETE FROM playback_queue_state WHERE spaceId = :spaceId")
    suspend fun clearState(spaceId: String)

    @Query("DELETE FROM playback_queue_items")
    suspend fun clearAllItems()

    @Query("DELETE FROM playback_queue_state")
    suspend fun clearAllStates()

    @Query("UPDATE playback_queue_state SET positionMs = :positionMs, updatedAt = :updatedAt WHERE spaceId = :spaceId")
    suspend fun updatePosition(spaceId: String, positionMs: Long, updatedAt: Long)

    @Query(
        "INSERT INTO playback_queue_items (spaceId, position, payload, identity) " +
            "SELECT :targetId, position, payload, identity FROM playback_queue_items WHERE spaceId = :sourceId"
    )
    suspend fun copyItems(sourceId: String, targetId: String)

    @Query(
        "INSERT INTO playback_queue_state (spaceId, currentIndex, positionMs, shuffleEnabled, shuffleOrder, " +
            "shuffleCursor, history, repeatMode, radioEnabled, generation, updatedAt) " +
            "SELECT :targetId, currentIndex, positionMs, shuffleEnabled, shuffleOrder, shuffleCursor, history, " +
            "repeatMode, radioEnabled, generation, :updatedAt FROM playback_queue_state WHERE spaceId = :sourceId"
    )
    suspend fun copyState(sourceId: String, targetId: String, updatedAt: Long)

    @Transaction
    suspend fun replace(
        spaceId: String,
        items: List<PlaybackQueueItemEntity>,
        state: PlaybackQueueStateEntity,
        summary: QueueSpaceSummaryValues
    ): Boolean {
        if (space(spaceId) == null) return false
        clearItems(spaceId)
        if (items.isNotEmpty()) insertItems(items)
        insertState(state)
        updateSpaceSummary(spaceId, summary.trackCount, summary.durationMs, summary.artworkUrls, summary.updatedAt)
        return true
    }

    @Transaction
    suspend fun clearSpaceContent(spaceId: String, now: Long) {
        clearItems(spaceId)
        clearState(spaceId)
        updateSpaceSummary(spaceId, 0, 0L, "", now)
    }

    @Transaction
    suspend fun ensureActiveSpace(fallbackName: String, now: Long): QueueSpaceEntity {
        activeSpace()?.let { return it }
        val existing = spaces().firstOrNull()
        if (existing != null) {
            markActive(existing.id, now)
            return existing.copy(isActive = true, lastActiveAt = now)
        }
        val created = QueueSpaceEntity(
            id = DEFAULT_QUEUE_SPACE_ID,
            name = fallbackName,
            createdAt = now,
            updatedAt = now,
            lastActiveAt = now,
            isActive = true,
            trackCount = 0,
            durationMs = 0L,
            artworkUrls = ""
        )
        insertSpace(created)
        return created
    }

    @Transaction
    suspend fun duplicateSpace(sourceId: String, target: QueueSpaceEntity): Boolean {
        if (space(sourceId) == null) return false
        insertSpace(target)
        copyItems(sourceId, target.id)
        copyState(sourceId, target.id, target.updatedAt)
        return true
    }

    @Transaction
    suspend fun deleteSpace(spaceId: String): Boolean {
        clearItems(spaceId)
        clearState(spaceId)
        return deleteSpaceRow(spaceId) > 0
    }

    @Transaction
    suspend fun replaceAllSpaces(
        spaces: List<QueueSpaceEntity>,
        items: List<PlaybackQueueItemEntity>,
        states: List<PlaybackQueueStateEntity>
    ) {
        clearAllItems()
        clearAllStates()
        deleteAllSpaces()
        if (spaces.isEmpty()) return
        insertSpaces(spaces)
        val knownIds = spaces.mapTo(hashSetOf()) { it.id }
        items.filter { it.spaceId in knownIds }.takeIf { it.isNotEmpty() }?.let { insertItems(it) }
        states.filter { it.spaceId in knownIds }.forEach { insertState(it) }
    }
}

data class QueueSpaceSummaryValues(
    val trackCount: Int,
    val durationMs: Long,
    val artworkUrls: String,
    val updatedAt: Long
)
