package com.luc4n3x.levyra.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.luc4n3x.levyra.domain.Track

@Entity(tableName = "favorite_tracks")
data class FavoriteTrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val streamUrl: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val largeThumbnailUrl: String,
    val source: String,
    val moodTags: String,
    val energy: Int,
    val vocal: Int,
    val replayScore: Int,
    val cacheScore: Int,
    val accentStart: Int,
    val accentEnd: Int,
    val createdAt: Long
)

fun FavoriteTrackEntity.toTrack(): Track = Track(
    id = id,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    streamUrl = streamUrl,
    videoUrl = videoUrl,
    thumbnailUrl = thumbnailUrl,
    largeThumbnailUrl = largeThumbnailUrl,
    source = source,
    moodTags = moodTags.split(TAG_SEPARATOR).filter { it.isNotBlank() }.toSet().ifEmpty { setOf("music") },
    energy = energy,
    vocal = vocal,
    replayScore = replayScore,
    cacheScore = cacheScore,
    accentStart = accentStart,
    accentEnd = accentEnd
)

fun Track.toFavoriteTrackEntity(createdAt: Long): FavoriteTrackEntity = FavoriteTrackEntity(
    id = id,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    streamUrl = streamUrl,
    videoUrl = videoUrl,
    thumbnailUrl = thumbnailUrl,
    largeThumbnailUrl = largeThumbnailUrl,
    source = source,
    moodTags = moodTags.joinToString(TAG_SEPARATOR),
    energy = energy,
    vocal = vocal,
    replayScore = replayScore,
    cacheScore = cacheScore,
    accentStart = accentStart,
    accentEnd = accentEnd,
    createdAt = createdAt
)

private const val TAG_SEPARATOR = "\u001F"
