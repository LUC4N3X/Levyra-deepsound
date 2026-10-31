package com.luc4n3x.levyra.data

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.luc4n3x.levyra.data.local.DownloadEntity
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.domain.DownloadHealth
import com.luc4n3x.levyra.domain.DownloadOwnership
import com.luc4n3x.levyra.player.offline.OfflineAudioMaintenance
import com.luc4n3x.levyra.player.offline.OfflineAudioStore
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineAudioRegistrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val database = LevyraDatabase.get(context)
    private val downloads = database.downloadedTracksDao()
    private val store = OfflineAudioStore(context.filesDir)

    private fun row(uri: String, trackId: String, ownership: DownloadOwnership) = DownloadEntity(
        trackId = trackId,
        title = "Offline fixture",
        artist = "Test",
        album = "",
        durationMs = 1_000L,
        fileName = "fixture.m4a",
        uri = uri,
        mimeType = "audio/mp4",
        embeddedMetadata = false,
        downloadPreset = "",
        downloadQuality = "",
        savedAt = System.currentTimeMillis(),
        ownership = ownership.name
    )

    private fun committedFile(id: String): File {
        val staged = File(store.stagingWorkspace(), "$id.part").apply { writeBytes(ByteArray(8_192)) }
        return store.commit(staged, id, "m4a")
    }

    @Test
    fun registeringTheSamePrivateFileReplacesItsRowAndKeepsManualOwnership() = runBlocking {
        val id = "registration-${UUID.randomUUID()}"
        val file = committedFile(id)
        val uri = Uri.fromFile(file).toString()
        try {
            downloads.registerDownload(row(uri, id, DownloadOwnership.MANUAL))
            downloads.registerDownload(row(uri, id, DownloadOwnership.SMART_OFFLINE))

            assertEquals(1, downloads.all().count { it.uri == uri })
            assertEquals(DownloadOwnership.MANUAL.name, downloads.byUri(uri)?.ownership)
        } finally {
            downloads.deleteByUri(uri)
            file.delete()
        }
    }

    @Test
    fun maintenanceDropsAbsentPrivateRowsAndNeverTouchesLegacyRows() = runBlocking {
        val id = "maintenance-${UUID.randomUUID()}"
        val absentUri = Uri.fromFile(File(store.audioDirectory, "$id.m4a")).toString()
        val legacyUri = "content://media/external_primary/audio/media/$id"
        val kept = committedFile("$id-kept")
        val keptUri = Uri.fromFile(kept).toString()
        try {
            downloads.insert(row(absentUri, id, DownloadOwnership.MANUAL))
            downloads.insert(row(legacyUri, id, DownloadOwnership.MANUAL))
            downloads.insert(row(keptUri, id, DownloadOwnership.MANUAL))

            OfflineAudioMaintenance.run(context)

            assertNull(downloads.byUri(absentUri))
            assertNotNull(downloads.byUri(legacyUri))
            assertNotNull(downloads.byUri(keptUri))
            assertTrue(kept.isFile)
        } finally {
            downloads.deleteByUri(absentUri)
            downloads.deleteByUri(legacyUri)
            downloads.deleteByUri(keptUri)
            kept.delete()
        }
    }

    @Test
    fun healthScanReportsAMissingPrivateFile() = runBlocking {
        val id = "health-${UUID.randomUUID()}"
        val file = committedFile(id)
        val uri = Uri.fromFile(file).toString()
        try {
            downloads.insert(row(uri, id, DownloadOwnership.MANUAL))
            assertTrue(file.delete())

            val result = DownloadHealthRepository(context).scan { _, _ -> }.first { it.download.uri == uri }

            assertEquals(DownloadHealth.Missing, result.health)
            assertFalse(file.exists())
        } finally {
            downloads.deleteByUri(uri)
        }
    }
}
