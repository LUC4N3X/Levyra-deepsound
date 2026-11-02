package com.luc4n3x.levyra.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

private const val PODCAST_COMPLETION_TAIL_MS = 30_000L
private const val PODCAST_COMPLETION_RATIO = 0.97
internal const val PODCAST_HISTORY_LIMIT = 200

@Entity(tableName = "podcast_subscriptions")
data class PodcastSubscriptionEntity(
    @PrimaryKey val browseId: String,
    val title: String,
    val author: String,
    val thumbnailUrl: String,
    val subscribedAt: Long
)

@Entity(tableName = "podcast_episode_progress")
data class PodcastEpisodeProgressEntity(
    @PrimaryKey val episodeId: String,
    val payload: String,
    val positionMs: Long,
    val durationMs: Long,
    val completed: Boolean,
    val updatedAt: Long
)

internal fun podcastEpisodeCompleted(positionMs: Long, durationMs: Long): Boolean =
    durationMs > 0L && (durationMs - positionMs <= PODCAST_COMPLETION_TAIL_MS || positionMs >= durationMs * PODCAST_COMPLETION_RATIO)

@Dao
interface PodcastLibraryDao {
    @Query("SELECT * FROM podcast_subscriptions ORDER BY subscribedAt DESC")
    fun subscriptions(): Flow<List<PodcastSubscriptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun subscribe(row: PodcastSubscriptionEntity)

    @Query("DELETE FROM podcast_subscriptions WHERE browseId = :browseId")
    suspend fun unsubscribe(browseId: String)

    @Query("SELECT * FROM podcast_episode_progress ORDER BY updatedAt DESC LIMIT :limit")
    fun history(limit: Int = PODCAST_HISTORY_LIMIT): Flow<List<PodcastEpisodeProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(row: PodcastEpisodeProgressEntity)

    @Query("UPDATE podcast_episode_progress SET completed = :completed, positionMs = 0 WHERE episodeId = :episodeId")
    suspend fun setCompleted(episodeId: String, completed: Boolean)

    @Query("DELETE FROM podcast_episode_progress WHERE episodeId = :episodeId")
    suspend fun removeProgress(episodeId: String)

    @Query("DELETE FROM podcast_episode_progress")
    suspend fun clearHistory()

    @Query("DELETE FROM podcast_episode_progress WHERE episodeId NOT IN (SELECT episodeId FROM podcast_episode_progress ORDER BY updatedAt DESC LIMIT :limit)")
    suspend fun trimHistory(limit: Int = PODCAST_HISTORY_LIMIT)
}
