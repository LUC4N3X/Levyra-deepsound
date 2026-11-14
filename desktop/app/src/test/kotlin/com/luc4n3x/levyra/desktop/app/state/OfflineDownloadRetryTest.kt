package com.luc4n3x.levyra.desktop.app.state

import com.luc4n3x.levyra.desktop.core.catalog.CatalogRepository
import com.luc4n3x.levyra.desktop.core.model.*
import com.luc4n3x.levyra.desktop.core.storage.*
import com.luc4n3x.levyra.desktop.core.stream.*
import java.nio.file.Files
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineDownloadRetryTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun cancelledDownloadRefreshesWithoutReportingFailureOrDeletingItsPartial() = verifyRetry(false)
    @Test fun expiredSourceRefreshesOnceWithoutReportingTheMatchAsFailed() = verifyRetry(true)

    private fun verifyRetry(expired: Boolean) = runTest {
        val paths = AppPaths(temporary.newFolder().toPath()).prepare()
        val partial = paths.downloadsDirectory.resolve("matched.m4a.part")
        val bytes = byteArrayOf(1, 2, 3, 4)
        Files.write(partial, bytes)
        val track = Track("one", "Song", "Artist", videoUrl = Track.watchUrlOf("one"))
        val store = DownloadStore.create(paths)
        store.upsert(DownloadRecord(track.id, track, DownloadStatus.CANCELLED,
            temporaryPath = partial.toString(), bytesDownloaded = bytes.size.toLong(), streamIdentity = "jiosaavn:one"))
        val resolver = PendingResolver(expired)
        val controller = OfflineDownloadController(backgroundScope, paths, resolver, CatalogRepository(),
            SettingsStore.create(paths), store, { _, _ -> error("Unexpected transcoding") })
        try {
            controller.retry(track.id)
            runCurrent()
            assertEquals(if (expired) 2 else 1, resolver.refreshes)
            assertEquals(0, resolver.failures)
            assertEquals(if (expired) 2 else 1, resolver.attempts)
            assertEquals(DownloadStatus.RESOLVING, store.record(track.id)?.status)
            assertEquals(4L, store.record(track.id)?.bytesDownloaded)
            assertArrayEquals(bytes, Files.readAllBytes(partial))
        } finally { controller.shutdown() }
    }

    private class PendingResolver(private val expireFirst: Boolean) : StreamResolver {
        var refreshes = 0
        var failures = 0
        var attempts = 0
        override suspend fun resolve(track: Track, quality: AudioQuality, codec: PreferredCodec): ResolvedAudio {
            if (++attempts == 1 && expireFirst) throw StreamSourceExpiredException()
            return CompletableDeferred<ResolvedAudio>().await()
        }
        override fun refresh(track: Track) { refreshes++ }
        override fun invalidate(track: Track) { failures++ }
    }
}
