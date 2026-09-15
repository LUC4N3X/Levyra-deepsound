package com.luc4n3x.levyra.feature.search

import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.Track

internal fun searchTestTrack(
    id: String,
    title: String,
    artist: String,
    durationMs: Long = 180_000L,
    thumbnailUrl: String = ""
): Track = Track(
    id = id,
    title = title,
    artist = artist,
    album = "",
    durationMs = durationMs,
    streamUrl = "",
    videoUrl = "https://music.youtube.com/watch?v=$id",
    thumbnailUrl = thumbnailUrl,
    largeThumbnailUrl = "",
    source = "YouTube Music",
    moodTags = emptySet(),
    energy = 50,
    vocal = 50,
    replayScore = 50,
    cacheScore = 0,
    accentStart = 0,
    accentEnd = 0
)

internal fun searchTestArtist(
    name: String,
    browseId: String,
    officialArtwork: Boolean = false
): ArtistHit = ArtistHit(
    name = name,
    subscribers = "",
    thumbnailUrl = "https://example.com/$browseId.jpg",
    accentStart = 0,
    accentEnd = 0,
    browseId = browseId,
    officialArtwork = officialArtwork
)

internal fun searchTestAlbum(title: String, artist: String, browseId: String): AlbumHit = AlbumHit(
    title = title,
    artist = artist,
    year = "",
    thumbnailUrl = "https://example.com/$browseId.jpg",
    query = "$title $artist",
    browseId = browseId
)
