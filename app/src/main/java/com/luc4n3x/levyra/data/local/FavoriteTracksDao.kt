package com.luc4n3x.levyra.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class FavoriteTracksDao {
    @Query("SELECT * FROM favorite_tracks ORDER BY createdAt DESC")
    abstract suspend fun all(): List<FavoriteTrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAllInternal(tracks: List<FavoriteTrackEntity>)

    @Query("DELETE FROM favorite_tracks")
    abstract suspend fun clearInternal()

    @Transaction
    open suspend fun replaceAll(tracks: List<FavoriteTrackEntity>) {
        clearInternal()
        if (tracks.isNotEmpty()) insertAllInternal(tracks)
    }
}
