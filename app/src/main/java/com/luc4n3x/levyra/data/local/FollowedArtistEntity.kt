package com.luc4n3x.levyra.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "followed_artists")
data class FollowedArtistEntity(
    @PrimaryKey val artistKey: String,
    val browseId: String,
    val name: String,
    val thumbnailUrl: String,
    val followedAt: Long
)

@Dao
interface FollowedArtistsDao {
    @Query("SELECT * FROM followed_artists ORDER BY followedAt DESC")
    suspend fun all(): List<FollowedArtistEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<FollowedArtistEntity>)

    @Query("DELETE FROM followed_artists")
    suspend fun clear()
}
