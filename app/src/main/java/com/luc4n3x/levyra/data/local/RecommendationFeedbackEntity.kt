package com.luc4n3x.levyra.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction

@Entity(tableName = "recommendation_feedback")
data class RecommendationFeedbackEntity(
    @PrimaryKey val trackKey: String,
    val artistKeys: String,
    val kind: String,
    val updatedAt: Long
)

@Dao
interface RecommendationFeedbackDao {
    @Query("SELECT * FROM recommendation_feedback ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<RecommendationFeedbackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: RecommendationFeedbackEntity)

    @Query("DELETE FROM recommendation_feedback WHERE trackKey = :trackKey")
    suspend fun delete(trackKey: String)

    @Query(
        "DELETE FROM recommendation_feedback WHERE trackKey NOT IN " +
            "(SELECT trackKey FROM recommendation_feedback ORDER BY updatedAt DESC LIMIT :keep)"
    )
    suspend fun trim(keep: Int)

    @Transaction
    suspend fun upsertAndTrim(row: RecommendationFeedbackEntity, keep: Int) {
        upsert(row)
        trim(keep)
    }

    @Query("DELETE FROM recommendation_feedback")
    suspend fun clear()
}
