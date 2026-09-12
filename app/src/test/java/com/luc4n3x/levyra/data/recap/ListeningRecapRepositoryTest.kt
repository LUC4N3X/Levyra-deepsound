package com.luc4n3x.levyra.data.recap

import com.luc4n3x.levyra.domain.LifetimeListening
import com.luc4n3x.levyra.domain.ListenEvent
import com.luc4n3x.levyra.domain.recap.ListeningRecapPeriod
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneOffset

class ListeningRecapRepositoryTest {
    private val zone = ZoneOffset.UTC

    @Test
    fun inFlightComputationDoesNotPublishAfterInvalidateCache() = runBlocking {
        val startedSignal = CompletableDeferred<Unit>()
        val releaseComputationSignal = CompletableDeferred<Unit>()

        val fakeSource = object : ListeningPulseDataSource {
            override suspend fun eventsWindow(days: Int): List<ListenEvent> {
                startedSignal.complete(Unit)
                releaseComputationSignal.await()
                return listOf(
                    ListenEvent(
                        trackId = "track-1",
                        title = "Song",
                        artist = "Artist",
                        album = "Album",
                        thumbnailUrl = "",
                        listenedMs = 120_000L,
                        trackDurationMs = 180_000L,
                        completed = true,
                        startedAt = System.currentTimeMillis() - 60_000L
                    )
                )
            }

            override suspend fun lifetime(): LifetimeListening = LifetimeListening()
        }

        val repository = ListeningRecapRepository(fakeSource) { zone }

        val computeJob = launch {
            repository.getRecap(ListeningRecapPeriod.Days7)
        }

        startedSignal.await()

        repository.invalidateCache()

        releaseComputationSignal.complete(Unit)
        computeJob.join()

        assertNull(repository.peekCached(ListeningRecapPeriod.Days7))
    }

    @Test
    fun subsequentComputationCachesNormallyAfterInvalidation() = runBlocking {
        val fakeSource = object : ListeningPulseDataSource {
            override suspend fun eventsWindow(days: Int): List<ListenEvent> = listOf(
                ListenEvent(
                    trackId = "track-1",
                    title = "Song",
                    artist = "Artist",
                    album = "Album",
                    thumbnailUrl = "",
                    listenedMs = 120_000L,
                    trackDurationMs = 180_000L,
                    completed = true,
                    startedAt = System.currentTimeMillis() - 60_000L
                )
            )

            override suspend fun lifetime(): LifetimeListening = LifetimeListening()
        }

        val repository = ListeningRecapRepository(fakeSource) { zone }

        repository.invalidateCache()
        val result = repository.getRecap(ListeningRecapPeriod.Days7)

        assertNotNull(result)
        assertEquals(result, repository.peekCached(ListeningRecapPeriod.Days7))
    }
}
