package com.luc4n3x.levyra.data.locallibrary

import com.luc4n3x.levyra.data.local.LocalMediaEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalLibraryReconcilerTest {

    @Test
    fun newFilesAreInsertedAndUnchangedFilesAreNotRewrittenByQuickScan() {
        val existing = listOf(row(id = 1, mediaId = 10, title = "Kept"))
        val scanned = listOf(row(mediaId = 10, title = "Kept"), row(mediaId = 11, title = "Fresh"))

        val plan = planLocalLibraryReconcile(existing, scanned, setOf("external_primary"), fullScan = false, now = 50L)

        assertEquals(listOf(11L), plan.inserts.map { it.mediaStoreId })
        assertTrue(plan.updates.isEmpty())
        assertTrue(plan.missingIds.isEmpty())
        assertTrue(plan.deleteIds.isEmpty())
    }

    @Test
    fun changedMetadataUpdatesTheExistingRowInPlace() {
        val existing = listOf(row(id = 7, mediaId = 10, title = "Old", modified = 1L))
        val scanned = listOf(row(mediaId = 10, title = "New", modified = 2L))

        val plan = planLocalLibraryReconcile(existing, scanned, setOf("external_primary"), fullScan = false, now = 50L)

        assertEquals(1, plan.updates.size)
        assertEquals(7L, plan.updates.single().id)
        assertEquals("New", plan.updates.single().title)
        assertEquals(50L, plan.updates.single().lastSeenAt)
    }

    @Test
    fun quickScanMarksVanishedFilesMissingWhileFullScanDeletesThem() {
        val existing = listOf(row(id = 1, mediaId = 10), row(id = 2, mediaId = 11))
        val scanned = listOf(row(mediaId = 10))

        val quick = planLocalLibraryReconcile(existing, scanned, setOf("external_primary"), fullScan = false, now = 9L)
        val full = planLocalLibraryReconcile(existing, scanned, setOf("external_primary"), fullScan = true, now = 9L)

        assertEquals(listOf(2L), quick.missingIds)
        assertTrue(quick.deleteIds.isEmpty())
        assertEquals(listOf(2L), full.deleteIds)
        assertTrue(full.missingIds.isEmpty())
    }

    @Test
    fun filesOnAnUnmountedVolumeAreKeptAsUnavailableEvenOnFullScan() {
        val existing = listOf(row(id = 1, mediaId = 10), row(id = 2, mediaId = 20, volume = "1a2b-3c4d"))
        val scanned = listOf(row(mediaId = 10))

        val plan = planLocalLibraryReconcile(existing, scanned, setOf("external_primary"), fullScan = true, now = 9L)

        assertEquals(listOf(2L), plan.missingIds)
        assertTrue(plan.deleteIds.isEmpty())
    }

    @Test
    fun reinsertedCardRestoresTheSameRows() {
        val existing = listOf(row(id = 2, mediaId = 20, volume = "1a2b-3c4d", available = false, missingSince = 5L))
        val scanned = listOf(row(mediaId = 20, volume = "1a2b-3c4d"))

        val plan = planLocalLibraryReconcile(
            existing,
            scanned,
            setOf("external_primary", "1a2b-3c4d"),
            fullScan = false,
            now = 9L
        )

        val restored = plan.updates.single()
        assertEquals(2L, restored.id)
        assertTrue(restored.available)
        assertEquals(0L, restored.missingSince)
        assertTrue(plan.inserts.isEmpty())
    }

    @Test
    fun movedFileKeepsItsRowWhenTheFingerprintIsUnique() {
        val existing = listOf(row(id = 3, mediaId = 30, title = "Song", size = 4_000_000L, path = "Music/Old/"))
        val scanned = listOf(row(mediaId = 31, title = "Song", size = 4_000_000L, path = "Music/New/"))

        val plan = planLocalLibraryReconcile(existing, scanned, setOf("external_primary"), fullScan = true, now = 9L)

        assertEquals(1, plan.movedCount)
        assertTrue(plan.inserts.isEmpty())
        assertTrue(plan.deleteIds.isEmpty())
        val moved = plan.updates.single()
        assertEquals(3L, moved.id)
        assertEquals(31L, moved.mediaStoreId)
        assertEquals("Music/New/", moved.relativePath)
    }

    @Test
    fun ambiguousCopiesAreNotMergedIntoAMove() {
        val existing = listOf(row(id = 3, mediaId = 30, title = "Song", size = 4_000_000L))
        val scanned = listOf(
            row(mediaId = 31, title = "Song", size = 4_000_000L, path = "Music/A/"),
            row(mediaId = 32, title = "Song", size = 4_000_000L, path = "Music/B/")
        )

        val plan = planLocalLibraryReconcile(existing, scanned, setOf("external_primary"), fullScan = true, now = 9L)

        assertEquals(0, plan.movedCount)
        assertEquals(2, plan.inserts.size)
        assertEquals(listOf(3L), plan.deleteIds)
    }

    @Test
    fun differentSongsWithTheSameSizeAreNeverTreatedAsAMove() {
        val existing = listOf(row(id = 3, mediaId = 30, title = "Intro", size = 4_000_000L))
        val scanned = listOf(row(mediaId = 31, title = "Outro", size = 4_000_000L))

        val plan = planLocalLibraryReconcile(existing, scanned, setOf("external_primary"), fullScan = false, now = 9L)

        assertEquals(0, plan.movedCount)
        assertEquals(listOf(31L), plan.inserts.map { it.mediaStoreId })
        assertEquals(listOf(3L), plan.missingIds)
    }

    @Test
    fun levyraIdentityLearnedEarlierSurvivesARescan() {
        val existing = listOf(row(id = 4, mediaId = 40).copy(levyraTrackId = "yt-abc", isLevyraDownload = true))
        val scanned = listOf(row(mediaId = 40))

        val plan = planLocalLibraryReconcile(existing, scanned, setOf("external_primary"), fullScan = true, now = 9L)

        assertEquals("yt-abc", plan.updates.single().levyraTrackId)
        assertTrue(plan.updates.single().isLevyraDownload)
    }

    @Test
    fun duplicateCopiesPreferTheLevyraDownload() {
        val plain = row(id = 1, mediaId = 10, title = "Same", size = 5_000_000L, path = "Download/", modified = 99L)
        val levyra = row(id = 2, mediaId = 11, title = "Same", size = 5_000_000L, path = "Music/Levyra/", modified = 1L)
        val other = row(id = 3, mediaId = 12, title = "Other", size = 5_000_000L)

        val visible = preferredLocalCopies(listOf(plain, levyra, other))

        assertEquals(listOf(2L, 3L), visible.map { it.id })
    }

    @Test
    fun rowsWithoutFingerprintAreAlwaysVisible() {
        val unknown = row(id = 1, mediaId = 10).copy(contentFingerprint = "")
        val same = row(id = 2, mediaId = 11).copy(contentFingerprint = "")

        assertEquals(2, preferredLocalCopies(listOf(unknown, same)).size)
    }

    @Test
    fun excludedFoldersAlsoHideSubfolders() {
        val excluded = setOf("external_primary:recordings/")

        assertFalse(localFolderAllowed("external_primary:recordings/", excluded))
        assertFalse(localFolderAllowed("external_primary:recordings/2024/", excluded))
        assertTrue(localFolderAllowed("external_primary:music/", excluded))
    }

    @Test
    fun strongerScanRequestsWinWhileAScanIsRunning() {
        assertEquals(LocalScanMode.Full, strongerLocalScanMode(LocalScanMode.Quick, LocalScanMode.Full))
        assertEquals(
            LocalScanMode.RebuildLevyra,
            strongerLocalScanMode(LocalScanMode.RebuildLevyra, LocalScanMode.Quick)
        )
        assertEquals(LocalScanMode.Quick, strongerLocalScanMode(null, LocalScanMode.Quick))
    }

    private fun row(
        id: Long = 0L,
        mediaId: Long,
        title: String = "Track $mediaId",
        size: Long = 3_000_000L + mediaId,
        path: String = "Music/",
        volume: String = "external_primary",
        modified: Long = 1L,
        available: Boolean = true,
        missingSince: Long = 0L
    ): LocalMediaEntity = ScannedLocalAudio(
        volumeName = volume,
        mediaStoreId = mediaId,
        contentUri = "content://media/$volume/audio/media/$mediaId",
        filePath = "",
        relativePath = path,
        displayName = "$title.mp3",
        title = title,
        artist = "Artist",
        album = "Album",
        albumArtist = "",
        genre = "",
        year = 0,
        trackField = 0,
        trackText = "",
        discText = "",
        durationMs = 200_000L,
        mimeType = "audio/mpeg",
        bitrate = 0,
        sizeBytes = size,
        dateAddedMs = 1L,
        dateModifiedMs = modified,
        albumId = 5L
    ).toLocalMediaEntity(levyraTrackId = "", now = 1L).copy(id = id, available = available, missingSince = missingSince)
}
