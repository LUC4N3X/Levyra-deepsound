package com.luc4n3x.levyra.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(
    tableName = "lyrics_cache",
    indices = [
        Index(value = ["expiresAt"]),
        Index(value = ["lastAccessedAt"]),
        Index(value = ["titleKey", "artistKey", "durationBucket", "languageCode", "translate"])
    ]
)
data class LyricsCacheEntity(
    @PrimaryKey val cacheKey: String,
    val titleKey: String,
    val artistKey: String,
    val durationBucket: Long,
    val videoId: String,
    val languageCode: String,
    val translate: Boolean,
    val synced: Boolean,
    val provider: String,
    val confidence: Int,
    val payload: String,
    val negative: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val lastAccessedAt: Long,
    val expiresAt: Long
)

@Dao
interface LyricsCacheDao {
    @Query("SELECT * FROM lyrics_cache WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun get(cacheKey: String): LyricsCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LyricsCacheEntity)

    @Query("UPDATE lyrics_cache SET lastAccessedAt = :timestamp WHERE cacheKey = :cacheKey")
    suspend fun touch(cacheKey: String, timestamp: Long)

    @Query("DELETE FROM lyrics_cache WHERE cacheKey = :cacheKey")
    suspend fun delete(cacheKey: String)

    @Query("SELECT * FROM lyrics_cache WHERE negative = 0 AND titleKey = :titleKey AND artistKey = :artistKey AND durationBucket BETWEEN :minimumDurationBucket AND :maximumDurationBucket AND languageCode = :languageCode AND translate = :translate ORDER BY ABS(durationBucket - :durationBucket) ASC, confidence DESC, updatedAt DESC LIMIT 1")
    suspend fun findBestPositive(
        titleKey: String,
        artistKey: String,
        durationBucket: Long,
        minimumDurationBucket: Long,
        maximumDurationBucket: Long,
        languageCode: String,
        translate: Boolean
    ): LyricsCacheEntity?

    @Query("DELETE FROM lyrics_cache WHERE (negative = 1 AND expiresAt < :now) OR (negative = 0 AND expiresAt < :positiveCutoff)")
    suspend fun deleteExpired(now: Long, positiveCutoff: Long)

    @Query("SELECT COUNT(*) FROM lyrics_cache")
    suspend fun count(): Int

    @Query("DELETE FROM lyrics_cache WHERE cacheKey IN (SELECT cacheKey FROM lyrics_cache ORDER BY negative DESC, lastAccessedAt ASC LIMIT :count)")
    suspend fun deleteOldest(count: Int)
}
