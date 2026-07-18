package com.luc4n3x.levyra.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MotionArtworkDao {
    @Query("SELECT * FROM motion_artwork WHERE identityKey = :identityKey LIMIT 1")
    suspend fun get(identityKey: String): MotionArtworkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MotionArtworkEntity)

    @Query("DELETE FROM motion_artwork WHERE identityKey = :identityKey")
    suspend fun delete(identityKey: String)

    @Query("DELETE FROM motion_artwork WHERE expiresAt <= :now OR configEpoch != :configEpoch")
    suspend fun deleteExpiredOrObsolete(now: Long, configEpoch: Long)

    @Query(
        """
        DELETE FROM motion_artwork
        WHERE identityKey NOT IN (
            SELECT identityKey
            FROM motion_artwork
            ORDER BY lastVerifiedAt DESC
            LIMIT :maxEntries
        )
        """
    )
    suspend fun prune(maxEntries: Int)
}
