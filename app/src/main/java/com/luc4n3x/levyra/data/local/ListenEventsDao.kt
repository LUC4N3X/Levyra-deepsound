package com.luc4n3x.levyra.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Transaction
import androidx.room.Query

@Dao
interface ListenEventsDao {
    @Insert
    suspend fun insert(event: ListenEventEntity)

    @Query(
        """
        UPDATE listen_events
        SET listenedMs = CASE WHEN listenedMs > :listenedMs THEN listenedMs ELSE :listenedMs END,
            completed = CASE WHEN completed = 1 OR :completed = 1 THEN 1 ELSE 0 END,
            artistBrowseIds = CASE
                WHEN artistBrowseIds = '' AND :artistBrowseIds != '' THEN :artistBrowseIds
                ELSE artistBrowseIds
            END
        WHERE trackId = :trackId AND startedAt = :startedAt
        """
    )
    suspend fun updateSession(
        trackId: String,
        startedAt: Long,
        listenedMs: Long,
        completed: Int,
        artistBrowseIds: String
    ): Int

    @Query("SELECT * FROM listen_events WHERE trackId = :trackId AND startedAt = :startedAt LIMIT 1")
    suspend fun findSession(trackId: String, startedAt: Long): ListenEventEntity?

    @Query("SELECT * FROM listen_events WHERE id > :afterId ORDER BY id ASC LIMIT :limit")
    suspend fun pageAfter(afterId: Long, limit: Int): List<ListenEventEntity>

    @Query("SELECT * FROM listen_events WHERE startedAt >= :since ORDER BY startedAt DESC")
    suspend fun since(since: Long): List<ListenEventEntity>

    @Query("SELECT * FROM listen_events ORDER BY startedAt DESC LIMIT :limit")
    suspend fun latest(limit: Int): List<ListenEventEntity>

    @Query("SELECT * FROM listen_events ORDER BY startedAt DESC")
    suspend fun all(): List<ListenEventEntity>

    @Query("SELECT * FROM listen_events WHERE trackId = :trackId ORDER BY startedAt DESC LIMIT 1")
    suspend fun findLatestByTrackId(trackId: String): ListenEventEntity?

    @Query("SELECT * FROM listen_events WHERE lower(trim(artist)) = lower(trim(:artist)) ORDER BY startedAt DESC LIMIT 1")
    suspend fun findLatestByArtist(artist: String): ListenEventEntity?

    @Query(
        """
        SELECT
            COALESCE(SUM(listenedMs), 0) AS listenedMs,
            COALESCE(SUM(CASE
                WHEN listenedMs >= :countedPlayMs THEN 1
                WHEN completed = 1 AND durationMs > 0 AND durationMs < :countedPlayMs
                    AND listenedMs * :completionDenominator >= durationMs * :completionNumerator THEN 1
                ELSE 0 END), 0) AS countedPlays,
            COUNT(*) AS eventCount,
            COALESCE(SUM(CASE WHEN completed = 1 THEN 1 ELSE 0 END), 0) AS completedCount,
            COUNT(DISTINCT CASE WHEN trackId != '' THEN trackId ELSE lower(trim(title)) || '|' || lower(trim(artist)) END) AS distinctTracks,
            COUNT(DISTINCT CASE WHEN trim(artist) != '' THEN lower(trim(artist)) END) AS distinctArtists
        FROM listen_events
        WHERE startedAt >= :fromMs AND startedAt < :toMs AND listenedMs >= :minimumEventMs
        """
    )
    suspend fun insightsAggregate(
        fromMs: Long,
        toMs: Long,
        minimumEventMs: Long,
        countedPlayMs: Long,
        completionNumerator: Long,
        completionDenominator: Long
    ): ListeningInsightsAggregateRow

    @Query(
        """
        SELECT
            strftime('%Y-%m-%d', startedAt / 1000, 'unixepoch', 'localtime') AS dayKey,
            COALESCE(SUM(listenedMs), 0) AS listenedMs,
            COALESCE(SUM(CASE
                WHEN listenedMs >= :countedPlayMs THEN 1
                WHEN completed = 1 AND durationMs > 0 AND durationMs < :countedPlayMs
                    AND listenedMs * :completionDenominator >= durationMs * :completionNumerator THEN 1
                ELSE 0 END), 0) AS countedPlays
        FROM listen_events
        WHERE startedAt >= :fromMs AND startedAt < :toMs AND listenedMs >= :minimumEventMs
        GROUP BY dayKey
        ORDER BY dayKey ASC
        """
    )
    suspend fun insightsDays(
        fromMs: Long,
        toMs: Long,
        minimumEventMs: Long,
        countedPlayMs: Long,
        completionNumerator: Long,
        completionDenominator: Long
    ): List<ListeningInsightsDayRow>

    @Query(
        """
        SELECT
            CAST(strftime('%H', startedAt / 1000, 'unixepoch', 'localtime') AS INTEGER) AS hour,
            COALESCE(SUM(listenedMs), 0) AS listenedMs
        FROM listen_events
        WHERE startedAt >= :fromMs AND startedAt < :toMs AND listenedMs >= :minimumEventMs
        GROUP BY hour
        ORDER BY hour ASC
        """
    )
    suspend fun insightsHours(
        fromMs: Long,
        toMs: Long,
        minimumEventMs: Long
    ): List<ListeningInsightsHourRow>

    @Query(
        """
        SELECT
            trackId,
            MAX(title) AS title,
            MAX(artist) AS artist,
            MAX(album) AS album,
            MAX(thumbnailUrl) AS thumbnailUrl,
            MAX(largeThumbnailUrl) AS largeThumbnailUrl,
            COALESCE(SUM(listenedMs), 0) AS listenedMs,
            COALESCE(SUM(CASE
                WHEN listenedMs >= :countedPlayMs THEN 1
                WHEN completed = 1 AND durationMs > 0 AND durationMs < :countedPlayMs
                    AND listenedMs * :completionDenominator >= durationMs * :completionNumerator THEN 1
                ELSE 0 END), 0) AS countedPlays
        FROM listen_events
        WHERE startedAt >= :fromMs AND startedAt < :toMs AND listenedMs >= :minimumEventMs
        GROUP BY CASE WHEN trackId != '' THEN trackId ELSE lower(trim(title)) || '|' || lower(trim(artist)) END
        ORDER BY listenedMs DESC, countedPlays DESC
        LIMIT :limit
        """
    )
    suspend fun insightsTopTracks(
        fromMs: Long,
        toMs: Long,
        limit: Int,
        minimumEventMs: Long,
        countedPlayMs: Long,
        completionNumerator: Long,
        completionDenominator: Long
    ): List<ListeningInsightsTopTrackRow>

    @Query(
        """
        SELECT
            MAX(artist) AS name,
            MAX(CASE WHEN largeThumbnailUrl != '' THEN largeThumbnailUrl ELSE thumbnailUrl END) AS thumbnailUrl,
            COALESCE(SUM(listenedMs), 0) AS listenedMs,
            COALESCE(SUM(CASE
                WHEN listenedMs >= :countedPlayMs THEN 1
                WHEN completed = 1 AND durationMs > 0 AND durationMs < :countedPlayMs
                    AND listenedMs * :completionDenominator >= durationMs * :completionNumerator THEN 1
                ELSE 0 END), 0) AS countedPlays,
            COUNT(DISTINCT CASE WHEN trackId != '' THEN trackId ELSE lower(trim(title)) || '|' || lower(trim(artist)) END) AS trackCount
        FROM listen_events
        WHERE startedAt >= :fromMs AND startedAt < :toMs AND listenedMs >= :minimumEventMs AND trim(artist) != ''
        GROUP BY lower(trim(artist))
        ORDER BY listenedMs DESC, countedPlays DESC
        LIMIT :limit
        """
    )
    suspend fun insightsTopArtists(
        fromMs: Long,
        toMs: Long,
        limit: Int,
        minimumEventMs: Long,
        countedPlayMs: Long,
        completionNumerator: Long,
        completionDenominator: Long
    ): List<ListeningInsightsTopArtistRow>

    @Query(
        """
        SELECT * FROM listen_events
        WHERE startedAt >= :fromMs AND startedAt < :toMs
            AND listenedMs >= :minimumEventMs
            AND (:query = '' OR title LIKE '%' || :query || '%' COLLATE NOCASE OR artist LIKE '%' || :query || '%' COLLATE NOCASE OR album LIKE '%' || :query || '%' COLLATE NOCASE)
            AND (startedAt < :beforeStartedAt OR (startedAt = :beforeStartedAt AND id < :beforeId))
        ORDER BY startedAt DESC, id DESC
        LIMIT :limit
        """
    )
    suspend fun insightsHistoryPage(
        fromMs: Long,
        toMs: Long,
        query: String,
        beforeStartedAt: Long,
        beforeId: Long,
        limit: Int,
        minimumEventMs: Long
    ): List<ListenEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<ListenEventEntity>)

    @Transaction
    suspend fun replaceAll(events: List<ListenEventEntity>) {
        clear()
        if (events.isNotEmpty()) insertAll(events)
    }

    @Query("SELECT COUNT(*) FROM listen_events")
    suspend fun count(): Int

    @Query("DELETE FROM listen_events WHERE startedAt < :cutoff")
    suspend fun prune(cutoff: Long)

    @Query("DELETE FROM listen_events")
    suspend fun clear()
}
