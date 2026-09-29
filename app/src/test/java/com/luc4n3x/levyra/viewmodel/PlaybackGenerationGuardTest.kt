package com.luc4n3x.levyra.viewmodel

import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackGenerationGuardTest {
    @Test
    fun resultFromAnOlderGenerationIsRejectedAfterNewerTracksStart() {
        val guard = PlaybackGenerationGuard()
        val songA = guard.begin("song-a")
        val songB = guard.begin("song-b")
        val songC = guard.begin("song-c")

        assertFalse(guard.isCurrent(songA))
        assertFalse(guard.accepts(songA, "song-a"))
        assertFalse(guard.isCurrent(songB))
        assertTrue(guard.isCurrent(songC))
        assertTrue(guard.accepts(songC, "song-c"))
        assertTrue(songA.generation < songB.generation && songB.generation < songC.generation)
    }

    @Test
    fun repeatedOccurrencesOfTheSameSongAreDistinguishable() {
        val guard = PlaybackGenerationGuard()
        val firstOccurrence = guard.begin("song-a")
        val secondOccurrence = guard.begin("song-a")

        assertNotEquals(firstOccurrence, secondOccurrence)
        assertFalse(guard.accepts(firstOccurrence, "song-a"))
        assertTrue(guard.accepts(secondOccurrence, "song-a"))
    }

    @Test
    fun resultForAnotherTrackIsRejectedEvenInTheCurrentGeneration() {
        val guard = PlaybackGenerationGuard()
        val ticket = guard.begin("song-d")

        assertFalse(guard.accepts(ticket, "song-c"))
        assertTrue(guard.accepts(ticket, "song-d"))
    }

    @Test
    fun ticketsCapturedBeforeAnyTransitionStayCurrentUntilTheNextOne() {
        val guard = PlaybackGenerationGuard()
        val restored = guard.current()

        assertTrue(guard.isCurrent(restored))
        assertTrue(guard.accepts(restored, "restored-song"))
        guard.begin("next-song")
        assertFalse(guard.isCurrent(restored))
    }

    @Test
    fun delayedResultsOnlyReachTheTrackThatIsActiveWhenTheyArrive() = runBlocking {
        val guard = PlaybackGenerationGuard()
        val published = Collections.synchronizedList(mutableListOf<String>())
        val tracks = listOf("song-a", "song-b", "song-c", "song-d")
        val gates = tracks.associateWith { CompletableDeferred<Unit>() }

        val requests = tracks.map { identity ->
            val ticket = guard.begin(identity)
            async {
                gates.getValue(identity).await()
                if (guard.accepts(ticket, identity)) published += identity
            }
        }
        listOf("song-a", "song-c", "song-b", "song-d").forEach { identity ->
            gates.getValue(identity).complete(Unit)
        }
        requests.awaitAll()

        assertEquals(listOf("song-d"), published)
    }

    @Test
    fun concurrentTransitionsNeverMoveTheActiveGenerationBackwards() {
        val guard = PlaybackGenerationGuard()
        val threads = 8
        val perThread = 500
        val executor = Executors.newFixedThreadPool(threads)
        val start = CountDownLatch(1)
        val issued = Collections.synchronizedList(mutableListOf<Long>())
        repeat(threads) { worker ->
            executor.execute {
                start.await()
                repeat(perThread) { index -> issued += guard.begin("w$worker-$index").generation }
            }
        }
        start.countDown()
        executor.shutdown()
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS))

        assertEquals(threads * perThread, issued.toSet().size)
        assertEquals(issued.maxOrNull(), guard.currentGeneration)
    }
}
