package com.luc4n3x.levyra.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "personal_collections")
data class PersonalCollectionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sourceKind: String,
    val sourceUrl: String,
    val sourceLabel: String,
    val format: String,
    val artworkUrl: String,
    val artworkFile: String,
    val allowCleartext: Boolean,
    val allowLocalNetwork: Boolean,
    val autoRefresh: Boolean,
    val hosts: String,
    val etag: String,
    val lastModified: String,
    val lastSyncedAt: Long,
    val lastSyncAttemptAt: Long,
    val lastSyncError: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "personal_collection_tracks",
    primaryKeys = ["collectionId", "entryKey"],
    foreignKeys = [
        ForeignKey(
            entity = PersonalCollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["trackId"], unique = true),
        Index(value = ["collectionId", "position"])
    ]
)
data class PersonalCollectionTrackEntity(
    val collectionId: String,
    val entryKey: String,
    val trackId: String,
    val position: Int,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val durationMs: Long,
    val audioUrl: String,
    val videoUrl: String,
    val artworkUrl: String,
    val artworkFile: String,
    val trackNumber: Int,
    val discNumber: Int,
    val year: Int,
    val addedAt: Long
)

data class PersonalCollectionTrackRow(
    @Embedded val track: PersonalCollectionTrackEntity,
    val collectionArtworkFile: String
)

data class PersonalCollectionSummaryRow(
    val id: String,
    val name: String,
    val sourceKind: String,
    val sourceLabel: String,
    val format: String,
    val artworkFile: String,
    val autoRefresh: Boolean,
    val allowCleartext: Boolean,
    val lastSyncedAt: Long,
    val lastSyncAttemptAt: Long,
    val lastSyncError: String,
    val createdAt: Long,
    val hosts: String,
    val trackCount: Int,
    val durationMs: Long
)

@Dao
interface PersonalCollectionDao {
    @Query(
        """
        SELECT c.id, c.name, c.sourceKind, c.sourceLabel, c.format, c.artworkFile, c.autoRefresh,
            c.allowCleartext, c.lastSyncedAt, c.lastSyncAttemptAt, c.lastSyncError, c.createdAt, c.hosts,
            COUNT(t.entryKey) AS trackCount, COALESCE(SUM(t.durationMs), 0) AS durationMs
        FROM personal_collections c
        LEFT JOIN personal_collection_tracks t ON t.collectionId = c.id
        GROUP BY c.id
        ORDER BY c.createdAt ASC
        """
    )
    fun observeSummaries(): Flow<List<PersonalCollectionSummaryRow>>

    @Query("SELECT * FROM personal_collections ORDER BY createdAt ASC")
    suspend fun all(): List<PersonalCollectionEntity>

    @Query("SELECT * FROM personal_collections WHERE id = :id LIMIT 1")
    suspend fun byId(id: String): PersonalCollectionEntity?

    @Query("SELECT * FROM personal_collections WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<PersonalCollectionEntity?>

    @Query("SELECT COUNT(*) FROM personal_collections WHERE autoRefresh = 1")
    suspend fun autoRefreshCount(): Int

    @Query(
        """
        SELECT t.*, c.artworkFile AS collectionArtworkFile
        FROM personal_collection_tracks t
        INNER JOIN personal_collections c ON c.id = t.collectionId
        WHERE t.collectionId = :collectionId
        ORDER BY t.position ASC
        """
    )
    fun observeTracks(collectionId: String): Flow<List<PersonalCollectionTrackRow>>

    @Query("SELECT * FROM personal_collection_tracks WHERE collectionId = :collectionId ORDER BY position ASC")
    suspend fun tracks(collectionId: String): List<PersonalCollectionTrackEntity>

    @Query("SELECT * FROM personal_collection_tracks WHERE trackId = :trackId LIMIT 1")
    suspend fun trackById(trackId: String): PersonalCollectionTrackEntity?

    @Query("SELECT artworkFile FROM personal_collection_tracks WHERE artworkFile != ''")
    suspend fun referencedTrackArtworkFiles(): List<String>

    @Query("SELECT artworkFile FROM personal_collections WHERE artworkFile != ''")
    suspend fun referencedCollectionArtworkFiles(): List<String>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCollection(collection: PersonalCollectionEntity)

    @Upsert
    suspend fun upsertCollection(collection: PersonalCollectionEntity)

    @Upsert
    suspend fun upsertTracks(tracks: List<PersonalCollectionTrackEntity>)

    @Query("DELETE FROM personal_collection_tracks WHERE collectionId = :collectionId AND entryKey IN (:entryKeys)")
    suspend fun deleteTracks(collectionId: String, entryKeys: List<String>)

    @Query("DELETE FROM personal_collections WHERE id = :id")
    suspend fun deleteCollection(id: String)

    @Query(
        """
        UPDATE personal_collections SET lastSyncAttemptAt = :attemptAt, lastSyncError = :error, updatedAt = :attemptAt
        WHERE id = :id
        """
    )
    suspend fun recordSyncFailure(id: String, attemptAt: Long, error: String)

    @Query(
        """
        UPDATE personal_collections SET lastSyncAttemptAt = :now, lastSyncedAt = :now, lastSyncError = '', updatedAt = :now
        WHERE id = :id
        """
    )
    suspend fun recordNotModified(id: String, now: Long)

    @Query("UPDATE personal_collections SET name = :name, autoRefresh = :autoRefresh, allowCleartext = :allowCleartext, updatedAt = :now WHERE id = :id")
    suspend fun updateSettings(id: String, name: String, autoRefresh: Boolean, allowCleartext: Boolean, now: Long)

    @Transaction
    suspend fun replaceContents(
        collection: PersonalCollectionEntity,
        tracks: List<PersonalCollectionTrackEntity>,
        removedEntryKeys: List<String>
    ) {
        upsertCollection(collection)
        removedEntryKeys.chunked(SQL_CHUNK).forEach { deleteTracks(collection.id, it) }
        tracks.chunked(SQL_CHUNK).forEach { upsertTracks(it) }
    }

    companion object {
        const val SQL_CHUNK = 400
    }
}
