package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Test
import org.schabi.newpipe.extractor.localization.DateWrapper
import org.schabi.newpipe.extractor.stream.SongMetadata
import java.time.OffsetDateTime

class YoutubeSongMetadataEnrichmentTest {
    @Test
    fun fillsOnlyMissingCatalogFields() {
        val metadata = SongMetadata.Builder("Extracted title", "Extracted artist")
            .setAlbum("Extracted album")
            .setTrack(7)
            .setReleaseDate(DateWrapper(OffsetDateTime.parse("2026-08-14T00:00:00Z")))
            .build()

        val enriched = track().withYoutubeSongMetadata(metadata)

        assertEquals("Original title", enriched.title)
        assertEquals("Original artist", enriched.artist)
        assertEquals("Extracted album", enriched.album)
        assertEquals("2026-08-14", enriched.releaseDate)
        assertEquals("2026", enriched.year)
        assertEquals(7, enriched.trackNumber)
    }

    @Test
    fun neverOverwritesStrongerExistingMetadata() {
        val metadata = SongMetadata.Builder("Other title", "Other artist")
            .setAlbum("Other album")
            .setTrack(9)
            .setReleaseDate(DateWrapper(OffsetDateTime.parse("2026-08-14T00:00:00Z")))
            .build()

        val enriched = track(
            album = "Catalog album",
            releaseDate = "2024-03-02",
            year = "2024",
            trackNumber = 3
        ).withYoutubeSongMetadata(metadata)

        assertEquals("Catalog album", enriched.album)
        assertEquals("2024-03-02", enriched.releaseDate)
        assertEquals("2024", enriched.year)
        assertEquals(3, enriched.trackNumber)
    }

    private fun track(
        album: String = "",
        releaseDate: String = "",
        year: String = "",
        trackNumber: Int = 0
    ) = Track(
        id = "video-id",
        title = "Original title",
        artist = "Original artist",
        album = album,
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "https://www.youtube.com/watch?v=abcdefghijk",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "test",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 50,
        cacheScore = 50,
        accentStart = 0,
        accentEnd = 0,
        releaseDate = releaseDate,
        year = year,
        trackNumber = trackNumber
    )
}
