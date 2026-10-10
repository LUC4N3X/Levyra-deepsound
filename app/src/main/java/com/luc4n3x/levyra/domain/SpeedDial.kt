package com.luc4n3x.levyra.domain

import java.util.Locale

enum class SpeedDialKind(val storageCode: String) {
    SONG("song"),
    ALBUM("album"),
    ARTIST("artist"),
    PLAYLIST("playlist");

    companion object {
        fun fromStorageCode(value: String): SpeedDialKind? =
            entries.firstOrNull { it.storageCode == value.trim().lowercase(Locale.ROOT) }
    }
}

data class SpeedDialPin(
    val kind: SpeedDialKind,
    val targetId: String,
    val title: String,
    val subtitle: String,
    val artworkUrl: String,
    val pinnedAt: Long,
    val track: Track? = null,
    val album: AlbumHit? = null
) {
    val key: String
        get() = speedDialKey(kind, targetId)
}

fun speedDialKey(kind: SpeedDialKind, targetId: String): String = "${kind.storageCode}:$targetId"

object SpeedDial {
    const val MAX_PINS = 24
    const val MAX_TEXT_CHARS = 160
    const val MAX_TARGET_ID_CHARS = 256

    fun songKey(track: Track): String? =
        track.id.trim().takeIf { it.isNotEmpty() }?.let { speedDialKey(SpeedDialKind.SONG, it) }

    fun albumKey(album: AlbumHit): String? = albumTargetId(album)?.let { speedDialKey(SpeedDialKind.ALBUM, it) }

    fun artistKey(name: String, browseId: String): String? =
        artistTargetId(name, browseId)?.let { speedDialKey(SpeedDialKind.ARTIST, it) }

    fun playlistKey(playlistId: String): String? =
        playlistId.trim().takeIf { it.isNotEmpty() }?.let { speedDialKey(SpeedDialKind.PLAYLIST, it) }

    fun song(track: Track, now: Long): SpeedDialPin? {
        val id = track.id.trim().takeIf { it.isNotEmpty() } ?: return null
        return SpeedDialPin(
            kind = SpeedDialKind.SONG,
            targetId = id,
            title = track.title,
            subtitle = track.artist,
            artworkUrl = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl },
            pinnedAt = now,
            track = track.forSpeedDial()
        ).sanitized()
    }

    fun album(album: AlbumHit, now: Long): SpeedDialPin? {
        val id = albumTargetId(album) ?: return null
        return SpeedDialPin(
            kind = SpeedDialKind.ALBUM,
            targetId = id,
            title = album.title,
            subtitle = album.artist,
            artworkUrl = album.thumbnailUrl,
            pinnedAt = now,
            album = album
        ).sanitized()
    }

    fun artist(name: String, browseId: String, artworkUrl: String, now: Long): SpeedDialPin? {
        val id = artistTargetId(name, browseId) ?: return null
        return SpeedDialPin(
            kind = SpeedDialKind.ARTIST,
            targetId = id,
            title = name,
            subtitle = "",
            artworkUrl = artworkUrl,
            pinnedAt = now
        ).sanitized()
    }

    fun playlist(playlist: Playlist, now: Long): SpeedDialPin? {
        val id = playlist.id.trim().takeIf { it.isNotEmpty() } ?: return null
        return SpeedDialPin(
            kind = SpeedDialKind.PLAYLIST,
            targetId = id,
            title = playlist.name,
            subtitle = "",
            artworkUrl = playlistArtwork(playlist),
            pinnedAt = now
        ).sanitized()
    }

    fun artistBrowseId(pin: SpeedDialPin): String =
        if (pin.kind != SpeedDialKind.ARTIST || pin.targetId.startsWith(ARTIST_NAME_PREFIX)) "" else pin.targetId

    fun toggle(pins: List<SpeedDialPin>, pin: SpeedDialPin): List<SpeedDialPin> {
        val current = sanitize(pins)
        if (current.any { it.key == pin.key }) return current.filterNot { it.key == pin.key }
        if (current.size >= MAX_PINS) return current
        return listOf(pin) + current
    }

    fun remove(pins: List<SpeedDialPin>, key: String): List<SpeedDialPin> = pins.filterNot { it.key == key }

    fun reorder(pins: List<SpeedDialPin>, orderedKeys: List<String>): List<SpeedDialPin> {
        val byKey = pins.associateBy { it.key }
        val ordered = orderedKeys.distinct().mapNotNull(byKey::get)
        val orderedSet = ordered.mapTo(HashSet()) { it.key }
        return ordered + pins.filterNot { it.key in orderedSet }
    }

    fun sanitize(pins: List<SpeedDialPin>): List<SpeedDialPin> =
        pins.mapNotNull { it.sanitized() }.distinctBy { it.key }.take(MAX_PINS)

    fun visible(
        pins: List<SpeedDialPin>,
        playlists: List<Playlist>,
        localTrackIds: Set<String>?
    ): List<SpeedDialPin> {
        if (pins.isEmpty()) return pins
        val playlistsById = playlists.associateBy { it.id }
        return pins.mapNotNull { pin ->
            when (pin.kind) {
                SpeedDialKind.PLAYLIST -> playlistsById[pin.targetId]?.let { live ->
                    val artwork = playlistArtwork(live)
                    if (live.name == pin.title && artwork == pin.artworkUrl) {
                        pin
                    } else {
                        pin.copy(title = live.name.ifBlank { pin.title }, artworkUrl = artwork)
                    }
                }
                SpeedDialKind.SONG -> pin.takeUnless {
                    localTrackIds != null && pin.targetId.startsWith(LOCAL_TRACK_PREFIX) && pin.targetId !in localTrackIds
                }
                SpeedDialKind.ALBUM, SpeedDialKind.ARTIST -> pin
            }
        }
    }

    fun withoutMissingLocalTracks(pins: List<SpeedDialPin>, localTrackIds: Set<String>): List<SpeedDialPin> =
        pins.filterNot { pin ->
            pin.kind == SpeedDialKind.SONG && pin.targetId.startsWith(LOCAL_TRACK_PREFIX) && pin.targetId !in localTrackIds
        }

    private fun SpeedDialPin.sanitized(): SpeedDialPin? {
        val cleanId = targetId.trim()
        val cleanTitle = title.trim().take(MAX_TEXT_CHARS)
        if (cleanId.isEmpty() || cleanId.length > MAX_TARGET_ID_CHARS || cleanTitle.isEmpty()) return null
        if (kind == SpeedDialKind.SONG && track == null) return null
        if (kind == SpeedDialKind.ALBUM && album == null) return null
        return copy(
            targetId = cleanId,
            title = cleanTitle,
            subtitle = subtitle.trim().take(MAX_TEXT_CHARS),
            artworkUrl = artworkUrl.trim()
        )
    }

    private fun albumTargetId(album: AlbumHit): String? {
        val browseId = album.browseId.trim()
        if (browseId.isNotEmpty()) return browseId
        val title = album.title.trim().lowercase(Locale.ROOT)
        if (title.isEmpty()) return null
        return "q:$title|${album.artist.trim().lowercase(Locale.ROOT)}"
    }

    private fun artistTargetId(name: String, browseId: String): String? {
        val cleanBrowseId = browseId.trim()
        if (cleanBrowseId.isNotEmpty()) return cleanBrowseId
        val cleanName = name.trim().lowercase(Locale.ROOT)
        return if (cleanName.length < 2) null else "$ARTIST_NAME_PREFIX$cleanName"
    }

    private fun playlistArtwork(playlist: Playlist): String =
        playlist.coverUrl.ifBlank {
            playlist.tracks.firstNotNullOfOrNull { track ->
                track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }.takeIf { it.isNotBlank() }
            }.orEmpty()
        }

    private fun Track.forSpeedDial(): Track {
        val durableStream = streamUrl.takeIf {
            it.startsWith("content://", ignoreCase = true) || it.startsWith("file://", ignoreCase = true)
        }.orEmpty()
        return copy(
            streamUrl = durableStream,
            videoStreamUrl = "",
            sponsorSegments = emptyList(),
            playbackManifest = null,
            videoSubtitleTracks = emptyList()
        )
    }

    private const val LOCAL_TRACK_PREFIX = "local:"
    private const val ARTIST_NAME_PREFIX = "n:"
}
