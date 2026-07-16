package com.luc4n3x.levyra.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.luc4n3x.levyra.domain.Track

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val coverUrl: String,
    val createdAt: Long,
    val updatedAt: Long
)

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
    val youtubeLoudnessDb: Float?,
    val youtubePerceptualLoudnessDb: Float?,
    val isrc: String,
    val upc: String,
    val releaseDate: String,
    val year: String,
    val trackNumber: Int,
    val discNumber: Int,
    val explicit: Boolean,
    val albumBrowseId: String,
    val artistBrowseIds: String,
    val counterpartVideoId: String,
    val videoType: String,
    val metadataProvider: String,
    val metadataConfidence: Int,
    val canonicalAlbumUrl: String,
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
    accentEnd = accentEnd,
    youtubeLoudnessDb = youtubeLoudnessDb,
    youtubePerceptualLoudnessDb = youtubePerceptualLoudnessDb,
    isrc = isrc,
    upc = upc,
    releaseDate = releaseDate,
    year = year,
    trackNumber = trackNumber,
    discNumber = discNumber,
    explicit = explicit,
    albumBrowseId = albumBrowseId,
    artistBrowseIds = artistBrowseIds.split(TAG_SEPARATOR).filter(String::isNotBlank),
    counterpartVideoId = counterpartVideoId,
    videoType = videoType,
    metadataProvider = metadataProvider,
    metadataConfidence = metadataConfidence.coerceIn(0, 100),
    canonicalAlbumUrl = canonicalAlbumUrl
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
        youtubeLoudnessDb = youtubeLoudnessDb,
        youtubePerceptualLoudnessDb = youtubePerceptualLoudnessDb,
        isrc = isrc,
        upc = upc,
        releaseDate = releaseDate,
        year = year,
        trackNumber = trackNumber,
        discNumber = discNumber,
        explicit = explicit,
        albumBrowseId = albumBrowseId,
        artistBrowseIds = artistBrowseIds.joinToString(TAG_SEPARATOR),
        counterpartVideoId = counterpartVideoId,
        videoType = videoType,
        metadataProvider = metadataProvider,
        metadataConfidence = metadataConfidence.coerceIn(0, 100),
        canonicalAlbumUrl = canonicalAlbumUrl,
        addedAt = addedAt
    )

private const val TAG_SEPARATOR = "\u001F"
