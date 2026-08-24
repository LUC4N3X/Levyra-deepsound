package com.luc4n3x.levyra.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ListenAllTimeAggregatesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(aggregate: ListenAllTimeAggregateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(aggregates: List<ListenAllTimeAggregateEntity>)

    @Query("SELECT * FROM listen_all_time_aggregates WHERE trackKey = :trackKey LIMIT 1")
    suspend fun get(trackKey: String): ListenAllTimeAggregateEntity?

    @Query("SELECT * FROM listen_all_time_aggregates ORDER BY listenedMs DESC")
    suspend fun all(): List<ListenAllTimeAggregateEntity>

    @Query("SELECT COUNT(*) FROM listen_all_time_aggregates")
    suspend fun count(): Int

    @Query("DELETE FROM listen_all_time_aggregates")
    suspend fun clear()
}
