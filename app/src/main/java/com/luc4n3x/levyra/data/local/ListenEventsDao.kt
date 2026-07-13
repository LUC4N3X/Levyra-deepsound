package com.luc4n3x.levyra.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Transaction
import androidx.room.Query

@Dao
interface ListenEventsDao {
    @Insert
    suspend fun insert(event: ListenEventEntity)

    @Query(
        """
        UPDATE listen_events
        SET listenedMs = CASE WHEN listenedMs > :listenedMs THEN listenedMs ELSE :listenedMs END,
            completed = CASE WHEN completed = 1 OR :completed = 1 THEN 1 ELSE 0 END
        WHERE trackId = :trackId AND startedAt = :startedAt
        """
    )
    suspend fun updateSession(trackId: String, startedAt: Long, listenedMs: Long, completed: Int): Int

    @Query("SELECT * FROM listen_events WHERE startedAt >= :since ORDER BY startedAt DESC")
    suspend fun since(since: Long): List<ListenEventEntity>

    @Query("SELECT * FROM listen_events ORDER BY startedAt DESC LIMIT :limit")
    suspend fun latest(limit: Int): List<ListenEventEntity>

    @Query("SELECT * FROM listen_events ORDER BY startedAt DESC")
    suspend fun all(): List<ListenEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<ListenEventEntity>)

    @Transaction
    suspend fun replaceAll(events: List<ListenEventEntity>) {
        clear()
        if (events.isNotEmpty()) insertAll(events)
    }

    @Query("SELECT COUNT(*) FROM listen_events")
    suspend fun count(): Int

    @Query("DELETE FROM listen_events WHERE startedAt < :cutoff")
    suspend fun prune(cutoff: Long)

    @Query("DELETE FROM listen_events")
    suspend fun clear()
}
