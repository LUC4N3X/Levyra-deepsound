package com.luc4n3x.levyra.ui.library

import com.luc4n3x.levyra.data.local.LocalMediaEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalLibraryQualityFilterTest {
    @Test
    fun exposesOnlyFiltersSupportedByIndexedMetadata() {
        val now = 2_000_000_000_000L
        val rows = listOf(
            media(
                displayName = "master.flac",
                mimeType = "audio/flac",
                bitrate = 1_050_000,
                dateAddedMs = now - 1_000L
            ),
            media(
                displayName = "old.mp3",
                mimeType = "audio/mpeg",
                bitrate = 192_000,
                dateAddedMs = now - 90L * DAY_MS
            )
        )

        assertEquals(
            listOf(
                LocalLibraryQualityFilter.All,
                LocalLibraryQualityFilter.Lossless,
                LocalLibraryQualityFilter.HighBitrate,
                LocalLibraryQualityFilter.Recent
            ),
            localLibraryQualityFilters(rows, now)
        )
    }

    @Test
    fun m4aIsNotCalledLosslessWithoutLosslessMetadata() {
        val row = media(
            displayName = "track.m4a",
            mimeType = "audio/mp4",
            bitrate = 900_000,
            dateAddedMs = 0L
        )

        assertFalse(row.matchesLocalQualityFilter(LocalLibraryQualityFilter.Lossless, nowMs = DAY_MS))
        assertTrue(row.matchesLocalQualityFilter(LocalLibraryQualityFilter.HighBitrate, nowMs = DAY_MS))
    }

    @Test
    fun recentUsesTheThirtyDayBoundary() {
        val now = 2_000_000_000_000L
        val inside = media(dateAddedMs = now - 30L * DAY_MS)
        val outside = media(dateAddedMs = now - 30L * DAY_MS - 1L)

        assertTrue(inside.matchesLocalQualityFilter(LocalLibraryQualityFilter.Recent, now))
        assertFalse(outside.matchesLocalQualityFilter(LocalLibraryQualityFilter.Recent, now))
    }

    private fun media(
        displayName: String = "track.mp3",
        mimeType: String = "audio/mpeg",
        bitrate: Int = 192_000,
        dateAddedMs: Long = 0L
    ) = LocalMediaEntity(
        identityKey = displayName,
        contentUri = "content://media/$displayName",
        volumeName = "external_primary",
        mediaStoreId = 1L,
        filePath = "/Music/$displayName",
        relativePath = "Music/",
        displayName = displayName,
        folderKey = "external_primary:music",
        folderName = "Music",
        title = "Track",
        artist = "Artist",
        album = "Album",
        albumArtist = "Artist",
        genre = "",
        composer = "",
        lyricist = "",
        comment = "",
        copyright = "",
        customTags = "",
        fullTagSearchText = "track artist album",
        year = 2026,
        trackNumber = 1,
        discNumber = 1,
        durationMs = 180_000L,
        mimeType = mimeType,
        bitrate = bitrate,
        sizeBytes = 10_000_000L,
        dateAddedMs = dateAddedMs,
        dateModifiedMs = dateAddedMs,
        albumId = 1L,
        albumKey = "album",
        artistKey = "artist",
        contentFingerprint = "fingerprint",
        levyraTrackId = "",
        isLevyraDownload = false,
        available = true,
        missingSince = 0L,
        lastSeenAt = dateAddedMs
    )

    private companion object {
        const val DAY_MS = 24L * 60L * 60L * 1_000L
    }
}
