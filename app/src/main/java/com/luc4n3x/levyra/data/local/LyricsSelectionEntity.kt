package com.luc4n3x.levyra.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "lyrics_selections")
data class LyricsSelectionEntity(
    @PrimaryKey val trackKey: String,
    val candidateId: String,
    val provider: String,
    val title: String,
    val artist: String,
    val durationSec: Long,
    val payload: String,
    val updatedAt: Long
)

@Dao
interface LyricsSelectionDao {
    @Query("SELECT * FROM lyrics_selections WHERE trackKey = :trackKey LIMIT 1")
    suspend fun get(trackKey: String): LyricsSelectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LyricsSelectionEntity)

    @Query("DELETE FROM lyrics_selections WHERE trackKey = :trackKey")
    suspend fun delete(trackKey: String)

    @Query("SELECT COUNT(*) FROM lyrics_selections")
    suspend fun count(): Int

    @Query("DELETE FROM lyrics_selections WHERE trackKey IN (SELECT trackKey FROM lyrics_selections ORDER BY updatedAt ASC LIMIT :count)")
    suspend fun deleteOldest(count: Int)
}
