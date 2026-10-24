package com.luc4n3x.levyra.data

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkManager
import com.luc4n3x.levyra.data.local.DownloadEntity
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.domain.DownloadHealth
import com.luc4n3x.levyra.player.offline.work.OfflineExportWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.Assume.assumeTrue
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkInfo
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DownloadHealthRepairTest {
    @Test fun liveRepairPublishesAFreshFileBeforeRemovingTheBrokenRecord() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("levyraLiveRepair") == "true")
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = LevyraDatabase.get(context)
        val downloads = database.downloadedTracksDao()
        val tasks = database.offlineDownloadTasksDao()
        val before = downloads.all()
        val original = before.firstOrNull { it.trackId.length == 11 }
        assumeTrue("Requires a saved remote download", original != null)
        val source = requireNotNull(original)
        assumeTrue(tasks.active().none { it.trackId == source.trackId })
        val previousTask = tasks.byKey(source.trackId)
        val missing = File(context.cacheDir, "health-live-${UUID.randomUUID()}.m4a")
        val fixtureId = downloads.insert(source.copy(id = 0L, uri = Uri.fromFile(missing).toString()))
        val fixture = requireNotNull(downloads.byId(fixtureId))
        try {
            assertTrue(DownloadHealthRepository(context).repair(DownloadHealthResult(fixture, DownloadHealth.Missing)))
            val task = requireNotNull(tasks.byKey(source.trackId))
            val result = withTimeout(120_000) {
                WorkManager.getInstance(context).getWorkInfoByIdFlow(UUID.fromString(task.workId)).first { it?.state?.isFinished == true }
            }
            assertEquals(WorkInfo.State.SUCCEEDED, result?.state)
            assertNull(downloads.byId(fixtureId))
            val fresh = requireNotNull(downloads.byTrackId(source.trackId))
            assertFalse(before.any { it.id == fresh.id })
            context.contentResolver.openAssetFileDescriptor(Uri.parse(fresh.uri), "r").use { assertTrue(requireNotNull(it).length != 0L) }
            assertEquals(source, downloads.byId(source.id))
        } finally {
            OfflineExportWorker.cancel(context, source.trackId)
            downloads.all().filter { it.trackId == source.trackId && before.none { old -> old.id == it.id } }.forEach { added ->
                if (before.none { it.uri == added.uri } && added.id != fixtureId) context.contentResolver.delete(Uri.parse(added.uri), null, null)
                downloads.deleteById(added.id)
            }
            if (previousTask == null) tasks.delete(source.trackId) else tasks.upsert(previousTask)
        }
    }

    @Test fun repairCanBePausedResumedAndCancelledWithoutLosingTheOriginal() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = LevyraDatabase.get(context)
        val key = "health-test-${UUID.randomUUID()}"
        val file = File(context.cacheDir, "$key.m4a").apply { writeText("damaged audio fixture") }
        val original = DownloadEntity(trackId = key, title = "Health test fixture", artist = "Test", album = "",
            durationMs = 180_000L, fileName = file.name, uri = Uri.fromFile(file).toString(), mimeType = "audio/mp4",
            embeddedMetadata = false, downloadPreset = "", downloadQuality = "", savedAt = System.currentTimeMillis())
        val downloadId = database.downloadedTracksDao().insert(original)
        val saved = requireNotNull(database.downloadedTracksDao().byId(downloadId))
        val bytes = file.readBytes()
        val repository = DownloadHealthRepository(context)
        try {
            assertTrue(repository.repair(DownloadHealthResult(saved, DownloadHealth.Unreadable)))
            val task = requireNotNull(database.offlineDownloadTasksDao().byKey(key))
            val work = withTimeout(5_000) { WorkManager.getInstance(context).getWorkInfoByIdFlow(UUID.fromString(task.workId)).first { it != null } }
            assertTrue(requireNotNull(work).tags.contains("levyra_health_repair_$downloadId"))
            OfflineExportWorker.pause(context, key)
            val resumed = requireNotNull(OfflineExportWorker.resume(context, key))
            val resumedWork = withTimeout(5_000) { WorkManager.getInstance(context).getWorkInfoByIdFlow(resumed).first { it != null } }
            assertTrue(requireNotNull(resumedWork).tags.contains("levyra_health_repair_$downloadId"))
            repository.cancel(key)
            assertEquals("CANCELLED", database.offlineDownloadTasksDao().byKey(key)?.state)
            assertEquals(saved, database.downloadedTracksDao().byId(downloadId))
            assertArrayEquals(bytes, file.readBytes())
        } finally {
            OfflineExportWorker.cancel(context, key)
            database.offlineDownloadTasksDao().delete(key)
            database.downloadedTracksDao().deleteById(downloadId)
            file.delete()
        }
    }
}
