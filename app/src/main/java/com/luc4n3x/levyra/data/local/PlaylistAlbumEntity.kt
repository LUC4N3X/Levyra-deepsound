package com.luc4n3x.levyra.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.luc4n3x.levyra.data.TrackJson
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.PlaylistAlbumEntry
import com.luc4n3x.levyra.domain.ReleaseType
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.playlistAlbumIdentity
import org.json.JSONArray
import org.json.JSONException
import timber.log.Timber

@Entity(
    tableName = "playlist_albums",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId"), Index(value = ["playlistId", "albumKey"])]
)
data class PlaylistAlbumEntity(
    @PrimaryKey val entryId: String,
    val playlistId: String,
    val albumKey: String,
    val position: Int,
    val title: String,
    val artist: String,
    val year: String,
    val thumbnailUrl: String,
    val query: String,
    val browseId: String,
    val artistBrowseId: String,
    val audioPlaylistId: String,
    val explicit: Boolean,
    val releaseDate: String,
    val upc: String,
    val canonicalUrl: String,
    val metadataProvider: String,
    val metadataConfidence: Int,
    val releaseType: String,
    val tracksJson: String,
    val addedAt: Long
)

fun PlaylistAlbumEntity.toAlbumHit(): AlbumHit = AlbumHit(
    title = title,
    artist = artist,
    year = year,
    thumbnailUrl = thumbnailUrl,
    query = query,
    browseId = browseId,
    artistBrowseId = artistBrowseId,
    audioPlaylistId = audioPlaylistId,
    explicit = explicit,
    releaseDate = releaseDate,
    upc = upc,
    canonicalUrl = canonicalUrl,
    metadataProvider = metadataProvider,
    metadataConfidence = metadataConfidence.coerceIn(0, 100),
    releaseType = ReleaseType.entries.firstOrNull { it.name == releaseType } ?: ReleaseType.Unknown
)

fun PlaylistAlbumEntity.toPlaylistAlbumEntry(trackOffset: Int): PlaylistAlbumEntry = PlaylistAlbumEntry(
    entryId = entryId,
    album = toAlbumHit(),
    tracks = decodePlaylistAlbumTracks(tracksJson),
    trackOffset = trackOffset,
    addedAt = addedAt
)

fun AlbumHit.toPlaylistAlbumEntity(
    playlistId: String,
    entryId: String,
    position: Int,
    tracks: List<Track>,
    addedAt: Long
): PlaylistAlbumEntity = PlaylistAlbumEntity(
    entryId = entryId,
    playlistId = playlistId,
    albumKey = playlistAlbumIdentity(this),
    position = position,
    title = title,
    artist = artist,
    year = year,
    thumbnailUrl = thumbnailUrl,
    query = query,
    browseId = browseId,
    artistBrowseId = artistBrowseId,
    audioPlaylistId = audioPlaylistId,
    explicit = explicit,
    releaseDate = releaseDate,
    upc = upc,
    canonicalUrl = canonicalUrl,
    metadataProvider = metadataProvider,
    metadataConfidence = metadataConfidence.coerceIn(0, 100),
    releaseType = releaseType.name,
    tracksJson = encodePlaylistAlbumTracks(tracks),
    addedAt = addedAt
)

fun encodePlaylistAlbumTracks(tracks: List<Track>): String =
    JSONArray().apply {
        tracks.forEach { track -> put(TrackJson.toJson(track.copy(streamUrl = "", playlistEntryId = "", playlistAddedAt = 0L))) }
    }.toString()

fun decodePlaylistAlbumTracks(json: String): List<Track> {
    if (json.isBlank()) return emptyList()
    return try {
        val array = JSONArray(json)
        buildList {
            for (index in 0 until array.length()) {
                array.optJSONObject(index)?.let(TrackJson::fromJson)?.let(::add)
            }
        }
    } catch (error: JSONException) {
        Timber.w(error, "Playlist album track snapshot is unreadable")
        emptyList()
    }
}
