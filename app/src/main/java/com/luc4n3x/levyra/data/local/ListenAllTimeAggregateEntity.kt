package com.luc4n3x.levyra.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "listen_all_time_aggregates",
    indices = [Index("artist")]
)
data class ListenAllTimeAggregateEntity(
    @PrimaryKey val trackKey: String,
    val trackId: String,
    val title: String,
    val artist: String,
    val countedPlays: Int,
    val listenedMs: Long,
    val completedCount: Int,
    val firstStartedAt: Long,
    val lastStartedAt: Long,
    @ColumnInfo(defaultValue = "''") val artistBrowseIds: String = ""
)
