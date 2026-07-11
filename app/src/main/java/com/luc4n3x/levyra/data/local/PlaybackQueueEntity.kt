package com.luc4n3x.levyra.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_queue_items")
data class PlaybackQueueItemEntity(
    @PrimaryKey val position: Int,
    val payload: String,
    val identity: String
)

@Entity(tableName = "playback_queue_state")
data class PlaybackQueueStateEntity(
    @PrimaryKey val singletonId: Int = 1,
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
