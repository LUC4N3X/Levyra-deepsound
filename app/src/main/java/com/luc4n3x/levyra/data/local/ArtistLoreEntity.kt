package com.luc4n3x.levyra.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(
    tableName = "artist_lore",
    indices = [
        Index(value = ["artistKey", "languageCode"]),
        Index(value = ["browseId", "languageCode"]),
        Index(value = ["expiresAt"]),
        Index(value = ["lastAccessedAt"])
    ]
)
data class ArtistLoreEntity(
    @PrimaryKey val cacheKey: String,
    val artistKey: String,
    val browseId: String,
    val languageCode: String,
    val text: String,
    val description: String,
    val pageTitle: String,
    val pageId: Int,
    val entityId: String,
    val thumbnailUrl: String,
    val originalImageUrl: String,
    val sourceUrl: String,
    val confidence: Int,
    val negative: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val lastAccessedAt: Long,
    val expiresAt: Long,
    val staleUntil: Long
)

@Dao
interface ArtistLoreDao {
    @Query("SELECT * FROM artist_lore WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun get(cacheKey: String): ArtistLoreEntity?

    @Query("SELECT * FROM artist_lore WHERE negative = 0 AND browseId = :browseId AND languageCode = :languageCode ORDER BY confidence DESC, updatedAt DESC LIMIT 1")
    suspend fun findByBrowseId(browseId: String, languageCode: String): ArtistLoreEntity?

    @Query("SELECT * FROM artist_lore WHERE negative = 0 AND artistKey = :artistKey AND languageCode = :languageCode ORDER BY confidence DESC, updatedAt DESC LIMIT 1")
    suspend fun findByArtistKey(artistKey: String, languageCode: String): ArtistLoreEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ArtistLoreEntity)

    @Query("UPDATE artist_lore SET lastAccessedAt = :timestamp WHERE cacheKey = :cacheKey")
    suspend fun touch(cacheKey: String, timestamp: Long)

    @Query("DELETE FROM artist_lore WHERE staleUntil < :now")
    suspend fun deleteExpired(now: Long)

    @Query("SELECT COUNT(*) FROM artist_lore")
    suspend fun count(): Int

    @Query("DELETE FROM artist_lore WHERE cacheKey IN (SELECT cacheKey FROM artist_lore ORDER BY negative DESC, lastAccessedAt ASC LIMIT :count)")
    suspend fun deleteOldest(count: Int)
}
