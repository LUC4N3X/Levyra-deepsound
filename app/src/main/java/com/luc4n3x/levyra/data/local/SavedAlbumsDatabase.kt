package com.luc4n3x.levyra.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "saved_albums")
data class SavedAlbumEntity(
    @PrimaryKey val albumKey: String,
    val browseId: String,
    val title: String,
    val artist: String,
    val year: String,
    val thumbnailUrl: String,
    val query: String,
    val artistBrowseId: String,
    val audioPlaylistId: String,
    val explicit: Boolean,
    val releaseDate: String,
    val upc: String,
    val canonicalUrl: String,
    val metadataProvider: String,
    val metadataConfidence: Int,
    val releaseType: String,
    val savedAt: Long
)

@Dao
interface SavedAlbumsDao {
    @Query("SELECT * FROM saved_albums ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<SavedAlbumEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_albums WHERE albumKey = :albumKey)")
    fun observeContains(albumKey: String): Flow<Boolean>

    @Query("SELECT * FROM saved_albums ORDER BY savedAt DESC")
    suspend fun all(): List<SavedAlbumEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_albums WHERE albumKey = :albumKey)")
    suspend fun contains(albumKey: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: SavedAlbumEntity)

    @Query("DELETE FROM saved_albums WHERE albumKey = :albumKey")
    suspend fun delete(albumKey: String)
}

@Database(
    entities = [SavedAlbumEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SavedAlbumsDatabase : RoomDatabase() {
    abstract fun savedAlbumsDao(): SavedAlbumsDao

    companion object {
        @Volatile private var instance: SavedAlbumsDatabase? = null

        fun get(context: Context): SavedAlbumsDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                SavedAlbumsDatabase::class.java,
                "levyra_saved_albums.db"
            ).build().also { instance = it }
        }
    }
}
