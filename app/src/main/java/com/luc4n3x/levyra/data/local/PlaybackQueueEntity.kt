package com.luc4n3x.levyra.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

const val DEFAULT_QUEUE_SPACE_ID = "default"

@Entity(tableName = "queue_spaces")
data class QueueSpaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val lastActiveAt: Long,
    val isActive: Boolean,
    val trackCount: Int,
    val durationMs: Long,
    val artworkUrls: String
)

@Entity(
    tableName = "playback_queue_items",
    primaryKeys = ["spaceId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = QueueSpaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["spaceId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlaybackQueueItemEntity(
    val spaceId: String,
    val position: Int,
    val payload: String,
    val identity: String
)

@Entity(
    tableName = "playback_queue_state",
    foreignKeys = [
        ForeignKey(
            entity = QueueSpaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["spaceId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlaybackQueueStateEntity(
    @PrimaryKey val spaceId: String,
    val currentIndex: Int,
    val positionMs: Long,
    val shuffleEnabled: Boolean,
    val shuffleOrder: String,
    val shuffleCursor: Int,
    val history: String,
    val repeatMode: String,
    val radioEnabled: Boolean,
    val generation: Long,
    val updatedAt: Long
)
