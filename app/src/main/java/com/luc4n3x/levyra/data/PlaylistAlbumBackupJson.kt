package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ReleaseType
import com.luc4n3x.levyra.domain.Track
import org.json.JSONArray
import org.json.JSONObject

internal data class PlaylistAlbumBackup(
    val entryId: String,
    val slot: Int,
    val album: AlbumHit,
    val tracks: List<Track>,
    val addedAt: Long
)

internal object PlaylistAlbumBackupJson {
    fun toJson(entry: PlaylistAlbumBackup): JSONObject = JSONObject()
        .put("entryId", entry.entryId)
        .put("slot", entry.slot)
        .put("addedAt", entry.addedAt)
        .put("title", entry.album.title)
        .put("artist", entry.album.artist)
        .put("year", entry.album.year)
        .put("thumbnailUrl", entry.album.thumbnailUrl)
        .put("query", entry.album.query)
        .put("browseId", entry.album.browseId)
        .put("artistBrowseId", entry.album.artistBrowseId)
        .put("audioPlaylistId", entry.album.audioPlaylistId)
        .put("explicit", entry.album.explicit)
        .put("releaseDate", entry.album.releaseDate)
        .put("upc", entry.album.upc)
        .put("canonicalUrl", entry.album.canonicalUrl)
        .put("metadataProvider", entry.album.metadataProvider)
        .put("metadataConfidence", entry.album.metadataConfidence)
        .put("releaseType", entry.album.releaseType.name)
        .put("tracks", JSONArray().apply { entry.tracks.forEach { put(TrackJson.toJson(it)) } })

    fun fromJson(json: JSONObject, fallbackAddedAt: Long): PlaylistAlbumBackup? {
        val entryId = json.optString("entryId").trim()
        val title = json.optString("title").trim()
        if (entryId.isBlank() || title.isBlank()) return null
        val tracks = json.optJSONArray("tracks")?.let { array ->
            buildList {
                for (index in 0 until array.length()) {
                    array.optJSONObject(index)?.let(TrackJson::fromJson)?.let(::add)
                }
            }
        }.orEmpty()
        if (tracks.isEmpty()) return null
        val releaseTypeName = json.optString("releaseType")
        return PlaylistAlbumBackup(
            entryId = entryId,
            slot = json.optInt("slot", Int.MAX_VALUE).coerceAtLeast(0),
            album = AlbumHit(
                title = title,
                artist = json.optString("artist"),
                year = json.optString("year"),
                thumbnailUrl = json.optString("thumbnailUrl"),
                query = json.optString("query"),
                browseId = json.optString("browseId"),
                artistBrowseId = json.optString("artistBrowseId"),
                audioPlaylistId = json.optString("audioPlaylistId"),
                explicit = json.optBoolean("explicit"),
                releaseDate = json.optString("releaseDate"),
                upc = json.optString("upc"),
                canonicalUrl = json.optString("canonicalUrl"),
                metadataProvider = json.optString("metadataProvider"),
                metadataConfidence = json.optInt("metadataConfidence").coerceIn(0, 100),
                releaseType = ReleaseType.entries.firstOrNull { it.name == releaseTypeName } ?: ReleaseType.Unknown
            ),
            tracks = tracks,
            addedAt = json.optLong("addedAt", fallbackAddedAt)
        )
    }

    fun parseArray(array: JSONArray?, fallbackAddedAt: Long): List<PlaylistAlbumBackup> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                array.optJSONObject(index)?.let { fromJson(it, fallbackAddedAt) }?.let(::add)
            }
        }.distinctBy { it.entryId }
    }
}

internal data class RestoredPlaylistLayout(
    val trackPositions: List<Int>,
    val albumPositions: List<Int>
)

internal fun restoredPlaylistLayout(trackCount: Int, albumSlots: List<Int>): RestoredPlaylistLayout {
    val total = trackCount + albumSlots.size
    val order = albumSlots.indices.sortedWith(compareBy({ albumSlots[it] }, { it }))
    val albumPositions = IntArray(albumSlots.size)
    val taken = BooleanArray(total)
    order.forEachIndexed { rank, albumIndex ->
        val minimum = order.take(rank).maxOfOrNull { albumPositions[it] + 1 } ?: 0
        val upper = total - (order.size - rank)
        val position = albumSlots[albumIndex].coerceIn(minimum, upper)
        albumPositions[albumIndex] = position
        taken[position] = true
    }
    val trackPositions = (0 until total).filterNot { taken[it] }
    return RestoredPlaylistLayout(trackPositions.take(trackCount), albumPositions.toList())
}
