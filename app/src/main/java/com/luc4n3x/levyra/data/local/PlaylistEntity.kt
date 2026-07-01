package com.luc4n3x.levyra.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.luc4n3x.levyra.domain.Track

/** Una playlist creata dall'utente. */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val coverUrl: String,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Una traccia dentro una playlist. Contiene una copia completa dei dati del brano così la
 * playlist resta consultabile offline e indipendente dalla cache di risoluzione.
 * position mantiene l'ordine scelto dall'utente.
 */
@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId")]
)
data class PlaylistTrackEntity(
    val playlistId: String,
    val trackId: String,
    val position: Int,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val videoUrl: String,
    val thumbnailUrl: String,
    val largeThumbnailUrl: String,
    val source: String,
    val accentStart: Int,
    val accentEnd: Int,
    val addedAt: Long
)

fun PlaylistTrackEntity.toTrack(): Track = Track(
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
    energy = 60,
    vocal = 50,
    replayScore = 84,
    cacheScore = 78,
    accentStart = accentStart,
    accentEnd = accentEnd
)

fun Track.toPlaylistTrackEntity(playlistId: String, position: Int, addedAt: Long): PlaylistTrackEntity =
    PlaylistTrackEntity(
        playlistId = playlistId,
        trackId = id,
        position = position,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        videoUrl = videoUrl,
        thumbnailUrl = thumbnailUrl,
        largeThumbnailUrl = largeThumbnailUrl,
        source = source,
        accentStart = accentStart,
        accentEnd = accentEnd,
        addedAt = addedAt
    )
