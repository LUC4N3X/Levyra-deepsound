package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.data.local.DownloadEntity
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupDownloadedTracksTest {
    @Test
    fun downloadMetadataRoundTripPreservesOfflineIdentity() {
        val download = download(
            trackId = "track-1",
            uri = "content://media/external/audio/media/41",
            fileName = "Artist - Track.m4a"
        ).copy(
            embeddedMetadata = true,
            downloadPreset = "high",
            downloadQuality = "256k",
            savedAt = 123_456L
        )

        val restored = parseDownloads(JSONArray().put(downloadToJson(download)))

        assertEquals(listOf(download), restored)
    }

    @Test
    fun reconciliationRecoversFilesWhenLegacyBackupHasNoDownloadSection() {
        val discovered = download(
            trackId = "",
            uri = "content://media/external/audio/media/42",
            fileName = "Artist - Track.m4a"
        )

        val restored = reconcileDownloadedTracks(emptyList(), listOf(discovered)) { true }

        assertEquals(listOf(discovered), restored)
    }

    @Test
    fun reconciliationRemapsStaleUriAndKeepsBackedUpMetadata() {
        val backup = download(
            trackId = "track-1",
            uri = "content://media/external/audio/media/7",
            fileName = "Artist - Track.m4a"
        ).copy(embeddedMetadata = true, downloadPreset = "automatic", downloadQuality = "best")
        val discovered = download(
            trackId = "",
            uri = "content://media/external/audio/media/42",
            fileName = backup.fileName
        )

        val restored = reconcileDownloadedTracks(listOf(backup), listOf(discovered)) {
            it == discovered.uri
        }.single()

        assertEquals(discovered.uri, restored.uri)
        assertEquals(backup.trackId, restored.trackId)
        assertEquals(backup.downloadPreset, restored.downloadPreset)
        assertEquals(backup.downloadQuality, restored.downloadQuality)
        assertTrue(restored.embeddedMetadata)
    }

    @Test
    fun reconciliationDropsMissingFilesAndDeduplicatesReadableUris() {
        val available = download(
            trackId = "track-1",
            uri = "content://media/external/audio/media/42",
            fileName = "Artist - Track.m4a"
        )
        val missing = download(
            trackId = "missing",
            uri = "content://media/external/audio/media/404",
            fileName = "Missing.m4a"
        )

        val restored = reconcileDownloadedTracks(listOf(available, missing), listOf(available.copy(trackId = ""))) {
            it == available.uri
        }

        assertEquals(listOf(available.copy(id = 0L)), restored)
    }

    private fun download(trackId: String, uri: String, fileName: String) = DownloadEntity(
        trackId = trackId,
        title = "Track",
        artist = "Artist",
        album = "Album",
        durationMs = 180_000L,
        fileName = fileName,
        uri = uri,
        mimeType = "audio/mp4",
        embeddedMetadata = false,
        downloadPreset = "",
        downloadQuality = "",
        savedAt = 100L
    )
}
