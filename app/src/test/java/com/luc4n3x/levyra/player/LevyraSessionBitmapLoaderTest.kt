package com.luc4n3x.levyra.player

import android.graphics.Bitmap
import android.net.Uri
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import com.luc4n3x.levyra.data.locallibrary.localArtworkModel
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito

@UnstableApi
class LevyraSessionBitmapLoaderTest {
    private val maxres = "https://i.ytimg.com/vi/abc/maxresdefault.jpg"
    private val thumbnail = "https://lh3.googleusercontent.com/a=w120-h120"
    private val hq = "https://i.ytimg.com/vi/abc/hqdefault.jpg"
    private val local = localArtworkModel("content://media/external/audio/media/42", 7L)
    private val bitmap: Bitmap = Mockito.mock(Bitmap::class.java)

    private fun scope() = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private fun embedded(vararg bytes: Byte) = MediaMetadata.Builder()
        .setArtworkData(bytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
        .build()

    @Test
    fun missingHeroFrameFallsBackToTheUrlsTheInAppPlayerAlsoTries() = runBlocking {
        val tried = mutableListOf<Any>()
        val models = sessionArtworkModels(null, maxres, artworkFallbacks(maxres, thumbnail, hq), liveRadio = false)

        val loaded = firstLoadedArtwork(listOf(0L), candidates = { models }) { model ->
            tried += model
            if (model == maxres) throw IllegalStateException("HTTP 404")
            model
        }

        assertEquals(thumbnail, loaded)
        assertEquals(listOf<Any>(maxres, thumbnail), tried)
    }

    @Test
    fun localModelWinsSoEmbeddedArtworkLoadsThroughTheContentResolver() {
        val albumArt = "content://media/external/audio/albumart/7"

        val models = sessionArtworkModels(null, albumArt, artworkFallbacks(local, "", null), liveRadio = false)

        assertEquals(listOf<Any>(local, albumArt), models)
    }

    @Test
    fun embeddedBytesComeFirstAndUrlsRemainTheirFallback() {
        val bytes = byteArrayOf(1, 2, 3)

        val models = sessionArtworkModels(bytes, maxres, emptyList(), liveRadio = false)

        assertArrayEquals(bytes, models.first() as ByteArray)
        assertEquals(maxres, models.last())
    }

    @Test
    fun freshRadioArtworkBeatsStaleFallbacksAndUnsafeRadioUrlsAreDropped() {
        val fresh = "https://cdn.radio.example/now.png"
        val logo = "https://cdn.radio.example/logo.png"

        assertEquals(
            listOf<Any>(fresh, logo),
            sessionArtworkModels(null, fresh, listOf(logo, "http://192.168.1.1/a.png", "file:///a.png"), liveRadio = true)
        )
    }

    @Test
    fun transientFailureRetriesOnABoundedSchedule() {
        var attempts = 0

        assertThrows(IOException::class.java) {
            runBlocking {
                firstLoadedArtwork(listOf(0L, 0L, 0L), candidates = { listOf(maxres) }) {
                    attempts++
                    throw IOException("offline")
                }
            }
        }

        assertEquals(4, attempts)
    }

    @Test
    fun conclusiveFailuresAreNotRetried() {
        var attempts = 0

        assertThrows(FileNotFoundException::class.java) {
            runBlocking {
                firstLoadedArtwork(listOf(0L, 0L), candidates = { listOf(local) }) {
                    attempts++
                    throw FileNotFoundException("no artwork")
                }
            }
        }

        assertEquals(1, attempts)
    }

    @Test
    fun cancellationStopsFallingBack() {
        var attempts = 0

        assertThrows(CancellationException::class.java) {
            runBlocking {
                firstLoadedArtwork(listOf(0L), candidates = { listOf(maxres, thumbnail) }) {
                    attempts++
                    throw CancellationException("skipped")
                }
            }
        }

        assertEquals(1, attempts)
    }

    @Test
    fun embeddedArtworkArrivingAfterAFailedUriStillLoadsWithoutRetryingTheDeadUri() = runBlocking {
        val embedded = byteArrayOf(9, 9)
        var pass = 0
        val tried = mutableListOf<Any>()

        val loaded = firstLoadedArtwork(
            listOf(0L, 0L),
            awaitLateCandidates = true,
            candidates = { if (pass++ == 0) listOf(maxres) else listOf(embedded, maxres) }
        ) { model ->
            tried += model
            if (model == maxres) throw IllegalStateException("HTTP 404")
            model
        }

        assertSame(embedded, loaded)
        assertEquals(listOf<Any>(maxres, embedded), tried)
    }

    @Test
    fun sessionMetadataLoadPicksUpEmbeddedArtworkTheStreamParsedLater() {
        val artworkUri = Mockito.mock(Uri::class.java)
        Mockito.`when`(artworkUri.toString()).thenReturn(maxres)
        val lookups = AtomicInteger()
        val loader = LevyraSessionBitmapLoader(
            scope(),
            retryDelaysMs = listOf(0L),
            currentArtworkData = { uri -> if (lookups.getAndIncrement() == 0 || uri != maxres) null else byteArrayOf(7) }
        ) { model, _ ->
            if (model is ByteArray) bitmap else throw IllegalStateException("HTTP 404")
        }

        val future = loader.loadBitmapFromMetadata(MediaMetadata.Builder().setArtworkUri(artworkUri).build())

        assertSame(bitmap, future.get(5, TimeUnit.SECONDS))
    }

    @Test
    fun failedLoadIsNotReusedSoTheNextRefreshTriesAgain() {
        val loads = AtomicInteger()
        val loader = LevyraSessionBitmapLoader(scope(), retryDelaysMs = emptyList()) { _, _ ->
            if (loads.getAndIncrement() == 0) throw IllegalStateException("outage") else bitmap
        }

        val failed = loader.loadBitmapFromMetadata(embedded(1))
        assertThrows(ExecutionException::class.java) { failed.get(5, TimeUnit.SECONDS) }
        val retried = loader.loadBitmapFromMetadata(embedded(1))

        assertNotSame(failed, retried)
        assertSame(bitmap, retried.get(5, TimeUnit.SECONDS))
    }

    @Test
    fun pendingAndSuccessfulLoadsAreSharedAcrossRefreshes() {
        val loads = AtomicInteger()
        val gate = CompletableDeferred<Unit>()
        val loader = LevyraSessionBitmapLoader(scope()) { _, _ ->
            loads.incrementAndGet()
            gate.await()
            bitmap
        }

        val first = loader.loadBitmapFromMetadata(embedded(1))
        val whilePending = loader.loadBitmapFromMetadata(embedded(1))
        gate.complete(Unit)
        first.get(5, TimeUnit.SECONDS)
        val afterSuccess = loader.loadBitmapFromMetadata(embedded(1))

        assertSame(first, whilePending)
        assertSame(first, afterSuccess)
        assertEquals(1, loads.get())
    }

    @Test
    fun rapidSkipStopsTheSupersededLoadButResumesItsFutureWhenTheTrackReturns() = runBlocking {
        val firstStarted = CompletableDeferred<Unit>()
        val firstStopped = CompletableDeferred<Unit>()
        val trackALoads = AtomicInteger()
        val loader = LevyraSessionBitmapLoader(scope()) { model, _ ->
            if ((model as ByteArray)[0] == 1.toByte() && trackALoads.getAndIncrement() == 0) {
                firstStarted.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    firstStopped.complete(Unit)
                }
            }
            bitmap
        }

        val trackA = loader.loadBitmapFromMetadata(embedded(1))
        withTimeout(5_000L) { firstStarted.await() }
        val trackB = loader.loadBitmapFromMetadata(embedded(2))
        withTimeout(5_000L) { firstStopped.await() }

        assertFalse(trackA.isDone)
        assertSame(bitmap, trackB.get(5, TimeUnit.SECONDS))
        assertSame(trackA, loader.loadBitmapFromMetadata(embedded(1)))
        assertSame(bitmap, trackA.get(5, TimeUnit.SECONDS))
    }

    @Test
    fun serviceTeardownCancelsPendingLoads() {
        val loaderScope = scope()
        val started = CompletableDeferred<Unit>()
        val loader = LevyraSessionBitmapLoader(loaderScope) { _, _ ->
            started.complete(Unit)
            awaitCancellation()
        }

        val future = loader.loadBitmapFromMetadata(embedded(1))
        runBlocking { withTimeout(5_000L) { started.await() } }
        loaderScope.cancel()

        assertThrows(java.util.concurrent.CancellationException::class.java) { future.get(5, TimeUnit.SECONDS) }
    }

    @Test
    fun noArtworkCompletesImmediatelySoMediaThreeDropsThePreviousTracksPendingLoad() {
        val loader = LevyraSessionBitmapLoader(scope()) { _, _ -> error("no model should be loaded") }

        val future = loader.loadBitmapFromMetadata(MediaMetadata.Builder().build())

        assertTrue(future.isDone)
        assertThrows(ExecutionException::class.java) { future.get() }
    }

    @Test
    fun failedDecodeDegradesToAFailedFutureInsteadOfCrashing() {
        val loader = LevyraSessionBitmapLoader(scope()) { _, _ -> throw IllegalArgumentException("corrupt image") }

        val future = loader.decodeBitmap(byteArrayOf(1))

        val failure = assertThrows(ExecutionException::class.java) { future.get(5, TimeUnit.SECONDS) }
        assertTrue(failure.cause is IllegalArgumentException)
    }
}
