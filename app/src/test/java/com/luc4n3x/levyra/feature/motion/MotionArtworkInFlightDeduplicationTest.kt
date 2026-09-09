package com.luc4n3x.levyra.feature.motion

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test

class MotionArtworkInFlightDeduplicationTest {

    private fun testArtwork(identity: String, provider: String) = MotionArtwork(
        identityKey = identity,
        provider = provider,
        url = "https://example.com/$identity.mp4",
        mimeType = "video/mp4",
        width = 1080,
        height = 1080,
        confidence = 100,
        expiresAtMs = 1_000_000L,
        lastVerifiedAtMs = 500_000L,
        configEpoch = 1L
    )

    @Test
    fun multipleSubscribersReceiveAllEmittedArtworks() = runBlocking {
        val session = MotionProgressiveSession()
        val art1 = testArtwork("track-1", "tidal")
        val art2 = testArtwork("track-1", "apple")

        val sub1Events = mutableListOf<MotionArtwork>()
        val sub2Events = mutableListOf<MotionArtwork>()

        coroutineScope {
            val job1 = launch {
                session.openSubscription().collect { sub1Events.add(it) }
            }
            val job2 = launch {
                session.openSubscription().collect { sub2Events.add(it) }
            }

            yield()

            session.emit(art1)
            yield()

            session.emit(art2)
            yield()

            session.complete()
            yield()

            job1.join()
            job2.join()
        }

        assertEquals(listOf(art1, art2), sub1Events)
        assertEquals(listOf(art1, art2), sub2Events)
    }

    @Test
    fun lateSubscriberReceivesCurrentArtworkAndSubsequentUpgrades() = runBlocking {
        val session = MotionProgressiveSession()
        val art1 = testArtwork("track-1", "tidal")
        val art2 = testArtwork("track-1", "apple")

        session.emit(art1)

        val lateEvents = mutableListOf<MotionArtwork>()
        coroutineScope {
            val jobLate = launch {
                session.openSubscription().collect { lateEvents.add(it) }
            }
            yield()

            assertEquals(listOf(art1), lateEvents)

            session.emit(art2)
            yield()

            session.complete()
            yield()

            jobLate.join()
        }

        assertEquals(listOf(art1, art2), lateEvents)
    }

    @Test
    fun subscriberCancellationDoesNotDisruptOtherActiveSubscribers() = runBlocking {
        val session = MotionProgressiveSession()
        val art1 = testArtwork("track-1", "tidal")
        val art2 = testArtwork("track-1", "apple")

        val sub1Events = mutableListOf<MotionArtwork>()
        val sub2Events = mutableListOf<MotionArtwork>()

        coroutineScope {
            val job1 = launch {
                session.openSubscription().collect { sub1Events.add(it) }
            }
            val job2 = launch {
                session.openSubscription().collect { sub2Events.add(it) }
            }
            yield()

            session.emit(art1)
            yield()

            job1.cancelAndJoin()

            session.emit(art2)
            yield()

            session.complete()
            yield()

            job2.join()
        }

        assertEquals(listOf(art1), sub1Events)
        assertEquals(listOf(art1, art2), sub2Events)
    }

    @Test
    fun subscriberAfterCompletionReceivesFinalArtworkImmediately() = runBlocking {
        val session = MotionProgressiveSession()
        val art1 = testArtwork("track-1", "apple")

        session.emit(art1)
        session.complete()

        val events = session.openSubscription().toList()
        assertEquals(listOf(art1), events)
    }
}
