package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackPayloadCodecTest {
    @Test
    fun offlineMetadataSurvivesWorkManagerPayloadRoundTrip() {
        val track = Track(
            id = "video-123",
            title = "Song",
            artist = "Artist",
            album = "Album",
            durationMs = 212_000L,
            streamUrl = "https://example.com/audio.m4a",
            videoUrl = "https://www.youtube.com/watch?v=video-123",
            thumbnailUrl = "https://example.com/cover.jpg",
            largeThumbnailUrl = "https://example.com/cover-large.jpg",
            source = "YouTube Music",
            moodTags = setOf("Pop", "Dance"),
            energy = 72,
            vocal = 61,
            replayScore = 90,
            cacheScore = 88,
            accentStart = 1,
            accentEnd = 2,
            isrc = "ITABC2600001",
            upc = "123456789012",
            releaseDate = "2026-07-20",
            year = "2026",
            trackNumber = 4,
            discNumber = 2,
            explicit = true,
            albumBrowseId = "MPRE-ALBUM",
            artistBrowseIds = listOf("UC-ARTIST"),
            counterpartVideoId = "counterpart-1",
            videoType = "MUSIC_VIDEO_TYPE_ATV",
            metadataProvider = "OfficialArtworkRepository",
            metadataConfidence = 96,
            canonicalAlbumUrl = "https://music.youtube.com/browse/MPRE-ALBUM",
            youtubeLikeCount = 200L,
            youtubeViewCount = 4_000L
        )

        val restored = TrackPayloadCodec.decode(TrackPayloadCodec.encode(track))

        requireNotNull(restored)
        assertEquals(track.isrc, restored.isrc)
        assertEquals(track.upc, restored.upc)
        assertEquals(track.releaseDate, restored.releaseDate)
        assertEquals(track.year, restored.year)
        assertEquals(track.trackNumber, restored.trackNumber)
        assertEquals(track.discNumber, restored.discNumber)
        assertTrue(restored.explicit)
        assertEquals(track.albumBrowseId, restored.albumBrowseId)
        assertEquals(track.artistBrowseIds, restored.artistBrowseIds)
        assertEquals(track.counterpartVideoId, restored.counterpartVideoId)
        assertEquals(track.videoType, restored.videoType)
        assertEquals(track.metadataProvider, restored.metadataProvider)
        assertEquals(track.metadataConfidence, restored.metadataConfidence)
        assertEquals(track.canonicalAlbumUrl, restored.canonicalAlbumUrl)
    }
}
