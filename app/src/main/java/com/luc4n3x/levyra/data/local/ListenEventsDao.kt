package com.luc4n3x.levyra.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ListenEventsDao {
    @Insert
    suspend fun insert(event: ListenEventEntity)

    @Query("SELECT * FROM listen_events WHERE startedAt >= :since ORDER BY startedAt DESC")
    suspend fun since(since: Long): List<ListenEventEntity>

    @Query("SELECT * FROM listen_events ORDER BY startedAt DESC LIMIT :limit")
    suspend fun latest(limit: Int): List<ListenEventEntity>

    @Query("SELECT COUNT(*) FROM listen_events")
    suspend fun count(): Int

    @Query("DELETE FROM listen_events WHERE startedAt < :cutoff")
    suspend fun prune(cutoff: Long)

    @Query("DELETE FROM listen_events")
    suspend fun clear()
}
