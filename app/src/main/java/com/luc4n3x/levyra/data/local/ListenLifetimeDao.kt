package com.luc4n3x.levyra.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface ListenLifetimeDao {

    @Query(
        """
        UPDATE listen_lifetime_tracks SET
            listenedMs = listenedMs + :listenedMs,
            countedPlays = countedPlays + :countedPlays,
            completedCount = completedCount + :completedCount,
            eventCount = eventCount + :eventCount,
            trackId = CASE WHEN trackId = '' AND :trackId != '' THEN :trackId ELSE trackId END,
            title = CASE WHEN :title != '' THEN :title ELSE title END,
            artist = CASE WHEN :artist != '' THEN :artist ELSE artist END,
            firstPlayedAt = CASE
                WHEN :playedAt > 0 AND (firstPlayedAt = 0 OR :playedAt < firstPlayedAt)
                THEN :playedAt ELSE firstPlayedAt END,
            lastPlayedAt = CASE WHEN :playedAt > lastPlayedAt THEN :playedAt ELSE lastPlayedAt END
        WHERE trackKey = :trackKey
        """
    )
    suspend fun updateTrackDelta(
        trackKey: String,
        trackId: String,
        title: String,
        artist: String,
        listenedMs: Long,
        countedPlays: Int,
        completedCount: Int,
        eventCount: Int,
        playedAt: Long
    ): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTrack(entity: ListenLifetimeTrackEntity): Long

    @Transaction
    suspend fun addTrackDelta(
        trackKey: String,
        trackId: String,
        title: String,
        artist: String,
        listenedMs: Long,
        countedPlays: Int,
        completedCount: Int,
        eventCount: Int,
        playedAt: Long
    ) {
        val updated = updateTrackDelta(
            trackKey, trackId, title, artist, listenedMs,
            countedPlays, completedCount, eventCount, playedAt
        )
        if (updated > 0) return
        val inserted = insertTrack(
            ListenLifetimeTrackEntity(
                trackKey = trackKey,
                trackId = trackId,
                title = title,
                artist = artist,
                listenedMs = listenedMs,
                countedPlays = countedPlays,
                completedCount = completedCount,
                eventCount = eventCount,
                firstPlayedAt = playedAt,
                lastPlayedAt = playedAt
            )
        )
        if (inserted == -1L) {
            updateTrackDelta(
                trackKey, trackId, title, artist, listenedMs,
                countedPlays, completedCount, eventCount, playedAt
            )
        }
    }

    @Query(
        """
        UPDATE listen_lifetime_artists SET
            listenedMs = listenedMs + :listenedMs,
            countedPlays = countedPlays + :countedPlays,
            completedCount = completedCount + :completedCount,
            eventCount = eventCount + :eventCount,
            name = CASE WHEN :name != '' THEN :name ELSE name END,
            firstPlayedAt = CASE
                WHEN :playedAt > 0 AND (firstPlayedAt = 0 OR :playedAt < firstPlayedAt)
                THEN :playedAt ELSE firstPlayedAt END,
            lastPlayedAt = CASE WHEN :playedAt > lastPlayedAt THEN :playedAt ELSE lastPlayedAt END
        WHERE artistKey = :artistKey
        """
    )
    suspend fun updateArtistDelta(
        artistKey: String,
        name: String,
        listenedMs: Long,
        countedPlays: Int,
        completedCount: Int,
        eventCount: Int,
        playedAt: Long
    ): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArtist(entity: ListenLifetimeArtistEntity): Long

    @Transaction
    suspend fun addArtistDelta(
        artistKey: String,
        name: String,
        listenedMs: Long,
        countedPlays: Int,
        completedCount: Int,
        eventCount: Int,
        playedAt: Long
    ) {
        val updated = updateArtistDelta(
            artistKey, name, listenedMs, countedPlays, completedCount, eventCount, playedAt
        )
        if (updated > 0) return
        val inserted = insertArtist(
            ListenLifetimeArtistEntity(
                artistKey = artistKey,
                name = name,
                listenedMs = listenedMs,
                countedPlays = countedPlays,
                completedCount = completedCount,
                eventCount = eventCount,
                firstPlayedAt = playedAt,
                lastPlayedAt = playedAt
            )
        )
        if (inserted == -1L) {
            updateArtistDelta(
                artistKey, name, listenedMs, countedPlays, completedCount, eventCount, playedAt
            )
        }
    }

    @Query(
        """
        SELECT
            COALESCE(SUM(listenedMs), 0) AS listenedMs,
            COALESCE(SUM(countedPlays), 0) AS countedPlays,
            COALESCE(SUM(completedCount), 0) AS completedCount,
            COALESCE(SUM(eventCount), 0) AS eventCount,
            COUNT(*) AS distinctTracks,
            COALESCE(MIN(NULLIF(firstPlayedAt, 0)), 0) AS firstPlayedAt,
            COALESCE(MAX(lastPlayedAt), 0) AS lastPlayedAt
        FROM listen_lifetime_tracks
        """
    )
    suspend fun trackTotals(): ListenLifetimeTotals

    @Query("SELECT COUNT(*) FROM listen_lifetime_artists")
    suspend fun artistCount(): Int

    @Query(
        "SELECT * FROM listen_lifetime_tracks ORDER BY listenedMs DESC, countedPlays DESC LIMIT :limit"
    )
    suspend fun topTracks(limit: Int): List<ListenLifetimeTrackEntity>

    @Query(
        "SELECT * FROM listen_lifetime_artists ORDER BY listenedMs DESC, countedPlays DESC LIMIT :limit"
    )
    suspend fun topArtists(limit: Int): List<ListenLifetimeArtistEntity>

    @Query("DELETE FROM listen_lifetime_tracks")
    suspend fun clearTracks()

    @Query("DELETE FROM listen_lifetime_artists")
    suspend fun clearArtists()
}
