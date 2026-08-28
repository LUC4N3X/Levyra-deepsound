package com.luc4n3x.levyra.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "recognition_history",
    indices = [Index(value = ["recognizedAt"])]
)
data class RecognitionHistoryEntity(
    @PrimaryKey
    val id: String,
    val recognizedAt: Long,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String,
    val provider: String,
    val providerTrackId: String,
    val isrc: String,
    val youtubeVideoId: String,
    val year: String
)

@Dao
interface RecognitionHistoryDao {
    @Query("SELECT * FROM recognition_history ORDER BY recognizedAt DESC LIMIT :limit")
    fun observe(limit: Int): Flow<List<RecognitionHistoryEntity>>

    @Query("SELECT * FROM recognition_history ORDER BY recognizedAt DESC LIMIT 1")
    suspend fun latest(): RecognitionHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RecognitionHistoryEntity)

    @Query("DELETE FROM recognition_history WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM recognition_history")
    suspend fun clear()

    @Query(
        """
        DELETE FROM recognition_history
        WHERE id NOT IN (
            SELECT id
            FROM recognition_history
            ORDER BY recognizedAt DESC
            LIMIT :maxEntries
        )
        """
    )
    suspend fun prune(maxEntries: Int)
}
