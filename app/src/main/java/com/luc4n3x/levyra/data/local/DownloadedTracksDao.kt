package com.luc4n3x.levyra.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedTracksDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: DownloadEntity): Long

    @Query("SELECT * FROM downloaded_tracks ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloaded_tracks ORDER BY savedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 80): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloaded_tracks ORDER BY savedAt DESC LIMIT :limit")
    suspend fun recent(limit: Int = 80): List<DownloadEntity>

    @Query(
        """
        SELECT * FROM downloaded_tracks
        WHERE id IN (SELECT MAX(id) FROM downloaded_tracks GROUP BY uri)
        ORDER BY savedAt DESC, id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun page(limit: Int, offset: Int): List<DownloadEntity>

    @Query("SELECT * FROM downloaded_tracks WHERE trackId = :trackId ORDER BY savedAt DESC LIMIT 1")
    suspend fun byTrackId(trackId: String): DownloadEntity?

    @Query(
        """
        SELECT * FROM downloaded_tracks
        WHERE trackId = :trackId
          AND downloadPreset = :downloadPreset
          AND downloadQuality = :downloadQuality
        ORDER BY savedAt DESC
        LIMIT 1
        """
    )
    suspend fun byTrackIdAndProfile(
        trackId: String,
        downloadPreset: String,
        downloadQuality: String
    ): DownloadEntity?

    @Query("DELETE FROM downloaded_tracks WHERE id = :id")
    suspend fun deleteById(id: Long)
}
