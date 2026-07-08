package com.luc4n3x.levyra.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.luc4n3x.levyra.domain.ListenEvent
import com.luc4n3x.levyra.domain.Track

@Entity(
    tableName = "listen_events",
    indices = [Index("startedAt"), Index("trackId")]
)
data class ListenEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val videoUrl: String,
    val thumbnailUrl: String,
    val largeThumbnailUrl: String,
    val source: String,
    val listenedMs: Long,
    val completed: Boolean,
    val startedAt: Long
)

fun ListenEventEntity.toListenEvent(): ListenEvent = ListenEvent(
    trackId = trackId,
    title = title,
    artist = artist,
    listenedMs = listenedMs,
    trackDurationMs = durationMs,
    completed = completed,
    startedAt = startedAt
)

fun ListenEventEntity.toTrack(): Track = Track(
    id = trackId,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    streamUrl = "",
    videoUrl = videoUrl,
    thumbnailUrl = thumbnailUrl,
    largeThumbnailUrl = largeThumbnailUrl,
    source = source,
    moodTags = setOf("music"),
    energy = 50,
    vocal = 50,
    replayScore = 60,
    cacheScore = 50,
    accentStart = 0,
    accentEnd = 0
)

fun Track.toListenEventEntity(listenedMs: Long, completed: Boolean, startedAt: Long): ListenEventEntity = ListenEventEntity(
    trackId = id,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    videoUrl = videoUrl,
    thumbnailUrl = thumbnailUrl,
    largeThumbnailUrl = largeThumbnailUrl,
    source = source,
    listenedMs = listenedMs,
    completed = completed,
    startedAt = startedAt
)
