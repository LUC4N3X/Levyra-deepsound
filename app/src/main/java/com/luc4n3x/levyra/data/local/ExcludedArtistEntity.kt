package com.luc4n3x.levyra.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "excluded_artists")
data class ExcludedArtistEntity(
    @PrimaryKey val artistKey: String,
    val browseId: String,
    val name: String,
    val excludedAt: Long
)

@Dao
interface ExcludedArtistsDao {
    @Query("SELECT * FROM excluded_artists ORDER BY excludedAt DESC")
    suspend fun all(): List<ExcludedArtistEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: ExcludedArtistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<ExcludedArtistEntity>)

    @Query("DELETE FROM excluded_artists WHERE artistKey = :artistKey")
    suspend fun delete(artistKey: String)

    @Query("DELETE FROM excluded_artists")
    suspend fun clear()
}
