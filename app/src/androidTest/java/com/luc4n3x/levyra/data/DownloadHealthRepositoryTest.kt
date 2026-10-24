package com.luc4n3x.levyra.data

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.luc4n3x.levyra.data.local.DownloadEntity
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.domain.DownloadHealth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DownloadHealthRepositoryTest {
    @Test fun scanRecognizesRealAudioMissingEmptyAndInvalidFilesWithoutMutatingThem() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, LevyraDatabase::class.java).build()
        val directory = File(context.cacheDir, "health-test-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            val healthy = File(directory, "healthy.wav").apply { writeBytes(wave()) }
            val empty = File(directory, "empty.m4a").apply { writeBytes(byteArrayOf()) }
            val invalid = File(directory, "invalid.m4a").apply { writeText("not audio") }
            val missing = File(directory, "missing.m4a")
            listOf(healthy, empty, invalid, missing).forEach { database.downloadedTracksDao().insert(download(it)) }
            val originalBytes = healthy.readBytes()
            val progress = mutableListOf<Pair<Int, Int>>()
            val repository = DownloadHealthRepository(context, database)
            val results = repository.scan { count, total -> progress += count to total }.associateBy { it.download.title }
            assertEquals(DownloadHealth.Healthy, results.getValue("healthy.wav").health)
            assertEquals(DownloadHealth.Empty, results.getValue("empty.m4a").health)
            assertEquals(DownloadHealth.Unreadable, results.getValue("invalid.m4a").health)
            assertEquals(DownloadHealth.Missing, results.getValue("missing.m4a").health)
            assertEquals(0 to 4, progress.first())
            assertEquals(4 to 4, progress.last())
            assertEquals(4, database.downloadedTracksDao().all().size)
            assertArrayEquals(originalBytes, healthy.readBytes())
            assertFalse(missing.exists())
            assertFalse(repository.repair(results.getValue("healthy.wav")))
            try {
                repository.scan { count, _ -> if (count == 1) throw CancellationException("cancelled by test") }
                fail("Cancellation was swallowed")
            } catch (_: CancellationException) { }
            assertEquals(4, database.downloadedTracksDao().all().size)
            val dao = database.downloadedTracksDao()
            val original = results.getValue("missing.m4a").download
            assertFalse(dao.completeRepair(original, "content://missing-replacement"))
            assertEquals(original, dao.byId(original.id))
            assertTrue(dao.completeRepair(original, results.getValue("healthy.wav").download.uri))
            assertEquals(null, dao.byId(original.id))
            assertArrayEquals(originalBytes, healthy.readBytes())
        } finally {
            database.close()
            directory.listFiles()?.forEach { it.delete() }
            directory.delete()
        }
    }

    private fun download(file: File) = DownloadEntity(
        trackId = "health-test", title = file.name, artist = "Test", album = "", durationMs = 1_000L,
        fileName = file.name, uri = Uri.fromFile(file).toString(), mimeType = "audio/wav", embeddedMetadata = false,
        downloadPreset = "", downloadQuality = "", savedAt = 1L
    )

    private fun wave(): ByteArray {
        val bytes = 16_000
        return ByteBuffer.allocate(44 + bytes).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + bytes); put("WAVEfmt ".toByteArray()); putInt(16)
            putShort(1); putShort(1); putInt(8_000); putInt(16_000); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(bytes); repeat(bytes / 2) { putShort(0) }
        }.array()
    }
}
