package com.luc4n3x.levyra.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Entity(
    tableName = "playback_source_matches",
    indices = [
        Index(value = ["canonicalKey"]),
        Index(value = ["sourceVideoId"]),
        Index(value = ["updatedAt"])
    ]
)
data class PlaybackSourceMatchEntity(
    @androidx.room.PrimaryKey val matchKey: String,
    val canonicalKey: String,
    val mode: String,
    val audioQuality: String,
    val sourceVideoId: String,
    val sourceVideoUrl: String,
    val provider: String,
    val manifestJson: String,
    val confidence: Int,
    val successCount: Int,
    val failureCount: Int,
    val blockedUntil: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val lastValidatedAt: Long
)

@Dao
interface PlaybackSourceMatchDao {
    @Query("SELECT * FROM playback_source_matches WHERE matchKey = :matchKey LIMIT 1")
    suspend fun get(matchKey: String): PlaybackSourceMatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PlaybackSourceMatchEntity)

    @Query(
        "UPDATE playback_source_matches SET successCount = successCount + 1, failureCount = CASE WHEN failureCount > 0 THEN failureCount - 1 ELSE 0 END, blockedUntil = 0, updatedAt = :now, lastValidatedAt = :now WHERE matchKey = :matchKey"
    )
    suspend fun recordSuccess(matchKey: String, now: Long)

    @Query(
        "UPDATE playback_source_matches SET failureCount = failureCount + 1, blockedUntil = :blockedUntil, updatedAt = :now WHERE matchKey = :matchKey"
    )
    suspend fun recordFailure(matchKey: String, blockedUntil: Long, now: Long)

    @Query("DELETE FROM playback_source_matches WHERE matchKey = :matchKey")
    suspend fun delete(matchKey: String)

    @Query("DELETE FROM playback_source_matches WHERE updatedAt < :olderThan AND successCount = 0")
    suspend fun deleteUnusedOlderThan(olderThan: Long): Int
}
