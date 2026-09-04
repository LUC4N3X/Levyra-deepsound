package com.luc4n3x.levyra.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction

@Entity(
    tableName = "playlist_tags",
    indices = [Index(value = ["normalizedName"], unique = true)]
)
data class PlaylistTagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val normalizedName: String,
    val createdAt: Long
)

@Entity(
    tableName = "playlist_tag_links",
    primaryKeys = ["playlistId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PlaylistTagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId"), Index("tagId")]
)
data class PlaylistTagLinkEntity(
    val playlistId: String,
    val tagId: String,
    val assignedAt: Long
)

@Dao
abstract class PlaylistTagsDao {

    @Query("SELECT * FROM playlist_tags ORDER BY name COLLATE NOCASE ASC")
    abstract suspend fun allTags(): List<PlaylistTagEntity>

    @Query("SELECT * FROM playlist_tags WHERE normalizedName = :normalizedName LIMIT 1")
    abstract suspend fun tagByNormalizedName(normalizedName: String): PlaylistTagEntity?

    @Query("SELECT * FROM playlist_tag_links")
    abstract suspend fun allLinks(): List<PlaylistTagLinkEntity>

    @Query("SELECT tagId FROM playlist_tag_links WHERE playlistId = :playlistId")
    abstract suspend fun tagIdsOf(playlistId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertTag(tag: PlaylistTagEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertTags(tags: List<PlaylistTagEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertLinks(links: List<PlaylistTagLinkEntity>)

    @Query("DELETE FROM playlist_tags WHERE id = :tagId")
    abstract suspend fun deleteTag(tagId: String)

    @Query("DELETE FROM playlist_tag_links WHERE playlistId = :playlistId")
    abstract suspend fun clearLinksOf(playlistId: String)

    @Query("DELETE FROM playlist_tags")
    abstract suspend fun clearTags()

    @Query("UPDATE playlist_tags SET name = :name, normalizedName = :normalizedName WHERE id = :tagId")
    abstract suspend fun renameTag(tagId: String, name: String, normalizedName: String)

    @Transaction
    open suspend fun replaceLinksOf(playlistId: String, links: List<PlaylistTagLinkEntity>) {
        clearLinksOf(playlistId)
        if (links.isNotEmpty()) upsertLinks(links)
    }

    @Transaction
    open suspend fun replaceAll(tags: List<PlaylistTagEntity>, links: List<PlaylistTagLinkEntity>) {
        clearTags()
        if (tags.isNotEmpty()) upsertTags(tags)
        if (links.isNotEmpty()) upsertLinks(links)
    }
}
