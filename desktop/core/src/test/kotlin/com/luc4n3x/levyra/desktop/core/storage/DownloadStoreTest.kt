package com.luc4n3x.levyra.desktop.core.storage

import com.luc4n3x.levyra.desktop.core.model.Track
import java.nio.file.Files
import java.nio.file.Path
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DownloadStoreTest {
    private lateinit var directory: Path
    private lateinit var downloadsDirectory: Path

    private val track = Track(
        id = "video-id",
        title = "Titolo",
        artist = "Artista",
        videoUrl = "https://www.youtube.com/watch?v=video-id"
    )

    @Before
    fun setUp() {
        directory = Files.createTempDirectory("levyra-download-store")
        downloadsDirectory = directory.resolve("offline")
        Files.createDirectories(downloadsDirectory)
    }

    @After
    fun tearDown() {
        directory.toFile().deleteRecursively()
    }

    private fun newStore(clock: () -> Long = { 1_000L }): DownloadStore = DownloadStore(
        store = JsonFileStore(
            file = directory.resolve("downloads.json"),
            serializer = DownloadData.serializer(),
            defaultValue = { DownloadData() }
        ),
        nowMillis = clock
    )

    @Test
    fun `records persist and completed files remain playable`() {
        val file = downloadsDirectory.resolve("song.m4a")
        Files.write(file, byteArrayOf(1, 2, 3, 4))
        val store = newStore()
        store.upsert(
            DownloadRecord(
                id = track.id,
                track = track,
                status = DownloadStatus.COMPLETED,
                filePath = file.toString(),
                bytesDownloaded = 4L,
                totalBytes = 4L,
                createdAt = 500L
            )
        )

        val reloaded = newStore()
        reloaded.reconcile(downloadsDirectory)
        val record = reloaded.current.records.single()

        assertEquals(DownloadStatus.COMPLETED, record.status)
        assertTrue(record.isPlayable)
        assertEquals(file.toString(), record.playableTrack().offlinePath)
    }

    @Test
    fun `interrupted downloads resume as queued`() {
        val partial = downloadsDirectory.resolve("song.m4a.part")
        Files.write(partial, ByteArray(32))
        val store = newStore()
        store.upsert(
            DownloadRecord(
                id = track.id,
                track = track,
                status = DownloadStatus.DOWNLOADING,
                temporaryPath = partial.toString(),
                bytesDownloaded = 8L,
                totalBytes = 64L,
                createdAt = 500L
            )
        )

        val reloaded = newStore()
        reloaded.reconcile(downloadsDirectory)
        val record = reloaded.current.records.single()

        assertEquals(DownloadStatus.QUEUED, record.status)
        assertEquals(32L, record.bytesDownloaded)
    }

    @Test
    fun `missing completed file becomes failed`() {
        val store = newStore()
        store.upsert(
            DownloadRecord(
                id = track.id,
                track = track,
                status = DownloadStatus.COMPLETED,
                filePath = downloadsDirectory.resolve("missing.m4a").toString(),
                createdAt = 500L
            )
        )

        store.reconcile(downloadsDirectory)

        assertEquals(DownloadStatus.FAILED, store.current.records.single().status)
    }
}
