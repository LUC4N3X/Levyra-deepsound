package com.luc4n3x.levyra.player.offline

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineMetadataTest {
    @Test
    fun requestedMetadataWinsWhileResolvedStreamIsPreserved() {
        val requested = track(
            id = "requested-id",
            title = "Original title",
            artist = "Original artist",
            album = "Original album",
            streamUrl = "",
            isrc = "ITABC2600001",
            releaseDate = "2026-07-20",
            trackNumber = 7,
            discNumber = 2,
            explicit = true,
            metadataProvider = "Qobuz",
            metadataConfidence = 97
        )
        val resolved = track(
            id = "resolved-id",
            title = "Resolver title",
            artist = "Resolver artist",
            album = "Resolver album",
            streamUrl = "https://example.com/final.m4a",
            isrc = "",
            releaseDate = "",
            trackNumber = 0,
            discNumber = 0,
            explicit = false,
            metadataProvider = "YouTube Music",
            metadataConfidence = 82
        )

        val merged = mergeOfflineMetadataTrack(requested, resolved)

        assertEquals("https://example.com/final.m4a", merged.streamUrl)
        assertEquals("Original title", merged.title)
        assertEquals("Original artist", merged.artist)
        assertEquals("Original album", merged.album)
        assertEquals("ITABC2600001", merged.isrc)
        assertEquals("2026-07-20", merged.releaseDate)
        assertEquals(7, merged.trackNumber)
        assertEquals(2, merged.discNumber)
        assertTrue(merged.explicit)
        assertEquals("Qobuz", merged.metadataProvider)
        assertEquals(97, merged.metadataConfidence)
    }

    @Test
    fun richMetadataFiltersInternalTagsAndCarriesProvenance() {
        val track = track(
            moodTags = setOf("music", "shared", "Pop", "Dance Pop", "pop"),
            isrc = "ITABC2600001",
            upc = "123456789012",
            releaseDate = "2026-07-20",
            trackNumber = 3,
            discNumber = 1,
            explicit = true,
            metadataProvider = "Deezer",
            metadataConfidence = 94
        )

        val metadata = track.toRichM4aMetadata(null, "First line\nSecond line")

        assertEquals(listOf("Pop", "Dance Pop"), metadata.genres)
        assertEquals("ITABC2600001", metadata.isrc)
        assertEquals("123456789012", metadata.upc)
        assertEquals("2026-07-20", metadata.releaseDate)
        assertEquals(3, metadata.trackNumber)
        assertEquals(1, metadata.discNumber)
        assertTrue(metadata.explicit)
        assertEquals("Deezer", metadata.metadataProvider)
        assertEquals(94, metadata.metadataConfidence)
        assertTrue(metadata.sourceUrl.contains("video-123"))
    }

    @Test
    fun cachedLyricsExcludeMetadataAndInstrumentalRows() {
        val payload = """
            {
              "lines": [
                {"text":"[Verse 1]","metadata":true,"instrumental":false},
                {"text":"First line","metadata":false,"instrumental":false},
                {"text":"Instrumental","metadata":false,"instrumental":true},
                {"text":"Second line","metadata":false,"instrumental":false}
              ]
            }
        """.trimIndent()

        val lyrics = cachedLyricsText(payload)

        assertEquals("First line\nSecond line", lyrics)
        assertFalse(lyrics.contains("Verse"))
        assertFalse(lyrics.contains("Instrumental"))
    }

    private fun track(
        id: String = "video-123",
        title: String = "Song",
        artist: String = "Artist",
        album: String = "Album",
        streamUrl: String = "https://example.com/audio.m4a",
        moodTags: Set<String> = setOf("music"),
        isrc: String = "",
        upc: String = "",
        releaseDate: String = "",
        trackNumber: Int = 0,
        discNumber: Int = 0,
        explicit: Boolean = false,
        metadataProvider: String = "",
        metadataConfidence: Int = 0
    ): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = 210_000L,
        streamUrl = streamUrl,
        videoUrl = "https://www.youtube.com/watch?v=$id",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "YouTube Music",
        moodTags = moodTags,
        energy = 60,
        vocal = 50,
        replayScore = 80,
        cacheScore = 80,
        accentStart = 1,
        accentEnd = 2,
        isrc = isrc,
        upc = upc,
        releaseDate = releaseDate,
        year = releaseDate.take(4),
        trackNumber = trackNumber,
        discNumber = discNumber,
        explicit = explicit,
        metadataProvider = metadataProvider,
        metadataConfidence = metadataConfidence
    )
}
