package com.luc4n3x.levyra.data

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkManager
import com.luc4n3x.levyra.data.local.DownloadEntity
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.domain.DownloadHealth
import com.luc4n3x.levyra.domain.downloadRepairTaskKey
import com.luc4n3x.levyra.player.offline.work.OfflineExportWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DownloadHealthRepairTest {
    @Test fun repairCanBePausedResumedAndCancelledWithoutLosingTheOriginal() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = LevyraDatabase.get(context)
        val key = "health-test-${UUID.randomUUID()}"
        val file = File(context.cacheDir, "$key.m4a").apply { writeText("damaged audio fixture") }
        val original = DownloadEntity(trackId = key, title = "Health test fixture", artist = "Test", album = "",
            durationMs = 180_000L, fileName = file.name, uri = Uri.fromFile(file).toString(), mimeType = "audio/mp4",
            embeddedMetadata = false, downloadPreset = "", downloadQuality = "", savedAt = System.currentTimeMillis())
        val downloadId = database.downloadedTracksDao().insert(original)
        val repairKey = downloadRepairTaskKey(downloadId)
        val saved = requireNotNull(database.downloadedTracksDao().byId(downloadId))
        val bytes = file.readBytes()
        val repository = DownloadHealthRepository(context)
        try {
            assertTrue(repository.repair(DownloadHealthResult(saved, DownloadHealth.Unreadable)))
            val task = requireNotNull(database.offlineDownloadTasksDao().byKey(repairKey))
            val work = withTimeout(5_000) { WorkManager.getInstance(context).getWorkInfoByIdFlow(UUID.fromString(task.workId)).first { it != null } }
            assertTrue(requireNotNull(work).tags.contains("levyra_health_repair_$downloadId"))
            OfflineExportWorker.pause(context, repairKey)
            val resumed = requireNotNull(OfflineExportWorker.resume(context, repairKey))
            val resumedWork = withTimeout(5_000) { WorkManager.getInstance(context).getWorkInfoByIdFlow(resumed).first { it != null } }
            assertTrue(requireNotNull(resumedWork).tags.contains("levyra_health_repair_$downloadId"))
            repository.cancel(repairKey)
            assertEquals("CANCELLED", database.offlineDownloadTasksDao().byKey(repairKey)?.state)
            assertEquals(saved, database.downloadedTracksDao().byId(downloadId))
            assertArrayEquals(bytes, file.readBytes())
        } finally {
            OfflineExportWorker.cancel(context, repairKey)
            database.offlineDownloadTasksDao().delete(repairKey)
            database.downloadedTracksDao().deleteById(downloadId)
            file.delete()
        }
    }
}
