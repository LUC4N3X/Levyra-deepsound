package com.luc4n3x.levyra.player

import com.luc4n3x.levyra.data.PlaybackFailureKind
import com.luc4n3x.levyra.data.classifyPlaybackFailureReason
import com.luc4n3x.levyra.data.playbackRecoveryPlanFor
import java.io.EOFException
import java.io.FileNotFoundException
import java.io.IOException
import java.net.ConnectException
import java.net.ProtocolException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackCacheRecoveryTest {
    private fun invalidatesCache(error: Throwable): Boolean =
        playbackRecoveryPlanFor(classifyPlaybackFailureReason(playbackFailureReasonOf(error))).invalidateCache

    @Test
    fun completeCacheRequiresAKnownLengthFullyPresent() {
        assertTrue(playbackCacheIsComplete(contentLength = 1_000L, cachedLength = 1_000L))
        assertTrue(playbackCacheIsComplete(contentLength = 1_000L, cachedLength = 1_200L))
        assertFalse(playbackCacheIsComplete(contentLength = 1_000L, cachedLength = 999L))
    }

    @Test
    fun partialCacheIsNeverReportedAsComplete() {
        assertFalse(playbackCacheIsComplete(contentLength = 5_000_000L, cachedLength = 384L * 1024L))
        assertFalse(playbackCacheIsComplete(contentLength = 0L, cachedLength = 384L * 1024L))
        assertFalse(playbackCacheIsComplete(contentLength = -1L, cachedLength = 384L * 1024L))
    }

    @Test
    fun discardedKeysMatchTheKeysThePlaybackModeActuallyUses() {
        assertEquals(
            setOf("stream"),
            playbackCacheKeysToDiscard("stream", "video", videoMode = false, hasSeparateVideoStream = false)
        )
        assertEquals(
            setOf("stream"),
            playbackCacheKeysToDiscard("stream", "video", videoMode = false, hasSeparateVideoStream = true)
        )
        assertEquals(
            setOf("video"),
            playbackCacheKeysToDiscard("stream", "video", videoMode = true, hasSeparateVideoStream = false)
        )
        assertEquals(
            setOf("stream", "video"),
            playbackCacheKeysToDiscard("stream", "video", videoMode = true, hasSeparateVideoStream = true)
        )
    }

    @Test
    fun prematureStreamEndIsResumable() {
        assertTrue(isRecoverableStreamEnd(ProtocolException("unexpected end of stream")))
        assertTrue(isRecoverableStreamEnd(EOFException()))
        assertTrue(
            isRecoverableStreamEnd(
                IOException("read failed", ProtocolException("unexpected end of stream"))
            )
        )
    }

    @Test
    fun ordinaryTransportFailuresAreNotTreatedAsResumableStreamEnds() {
        assertFalse(isRecoverableStreamEnd(SocketTimeoutException("timeout")))
        assertFalse(isRecoverableStreamEnd(UnknownHostException("dns")))
        assertFalse(isRecoverableStreamEnd(IOException("connection reset")))
    }

    @Test
    fun temporaryFailuresPreserveCachedBytes() {
        assertFalse(invalidatesCache(SocketTimeoutException("timed out")))
        assertFalse(invalidatesCache(UnknownHostException("dns failure")))
        assertFalse(invalidatesCache(IOException("connection reset")))
        assertFalse(invalidatesCache(ProtocolException("unexpected end of stream")))
        assertFalse(invalidatesCache(IOException("stream expired, http 403 forbidden")))
        assertFalse(invalidatesCache(IOException("http 503 service unavailable")))
    }

    @Test
    fun rangeNotSatisfiableDiscardsTheIncompatibleEntry() {
        assertTrue(invalidatesCache(IOException("http 416 range not satisfiable")))
        assertEquals(
            PlaybackFailureKind.RangeNotSatisfiable,
            classifyPlaybackFailureReason("http 416 range not satisfiable")
        )
    }

    @Test
    fun missingCacheFileDiscardsTheIncompatibleEntry() {
        assertTrue(invalidatesCache(FileNotFoundException("/data/levyra_media_cache/x.exo")))
        assertEquals(
            PlaybackFailureKind.ResourceMissing,
            classifyPlaybackFailureReason("open failed: ENOENT (No such file or directory)")
        )
        assertTrue(invalidatesCache(IOException("open failed: ENOENT (No such file or directory)")))
    }

    @Test
    fun httpNotFoundIsNotMistakenForAMissingCacheFile() {
        assertEquals(PlaybackFailureKind.NotFound, classifyPlaybackFailureReason("http 404 not found"))
        assertFalse(invalidatesCache(IOException("http 404 not found")))
    }

    @Test
    fun failureReasonCarriesTheDecisiveCauseChainEvidence() {
        val reason = playbackFailureReasonOf(
            IOException("load failed", FileNotFoundException("cache span missing"))
        )
        assertTrue(reason.contains("enoent", ignoreCase = true))
        assertTrue(reason.contains("cache span missing"))
    }

    @Test
    fun midStreamTransportResetsAreResumableButCancellationAndConnectFailuresAreNot() {
        assertTrue(
            isTransientStreamTransportFailure(
                IOException("read failed", SocketException("Connection reset"))
            )
        )
        assertTrue(isTransientStreamTransportFailure(SocketTimeoutException("timeout")))
        assertTrue(isTransientStreamTransportFailure(IOException("stream was reset: INTERNAL_ERROR")))
        assertFalse(isTransientStreamTransportFailure(SocketException("Socket closed")))
        assertFalse(isTransientStreamTransportFailure(IOException("Canceled", SocketException("Connection reset"))))
        assertFalse(isTransientStreamTransportFailure(ConnectException("Connection refused")))
        assertFalse(isTransientStreamTransportFailure(UnknownHostException("rr1.googlevideo.com")))
    }

    @Test
    fun resolverConnectivityFailuresKeepPlaybackIntentButContentFailuresDoNot() {
        assertTrue(isTransientNetworkFailure(ConnectException("Failed to connect to youtubei.googleapis.com")))
        assertTrue(isTransientNetworkFailure(UnknownHostException("youtubei.googleapis.com")))
        assertTrue(
            isTransientNetworkFailure(
                IllegalStateException("LevyraExtractor: java.net.ConnectException: Failed to connect to www.youtube.com")
            )
        )
        assertTrue(isTransientNetworkFailure(IOException("isConnected failed: ECONNREFUSED (Connection refused)")))
        assertFalse(isTransientNetworkFailure(IllegalStateException("Sign in to confirm you're not a bot")))
        assertFalse(isTransientNetworkFailure(IOException("stream expired, http 403 forbidden")))
        assertFalse(isTransientNetworkFailure(IllegalStateException("Video unavailable")))
    }

    @Test
    fun loadRetriesAreBoundedAndOnlyForTransientNetworkFailures() {
        val refused = IOException("load failed", ConnectException("ECONNREFUSED"))
        val delays = (1..TRANSIENT_NETWORK_LOAD_RETRIES).map { transientNetworkRetryDelayMs(refused, it) }
        assertEquals(listOf(1_000L, 2_000L, 4_000L, 5_000L, 5_000L, 5_000L, 5_000L, 5_000L), delays)
        assertEquals(
            androidx.media3.common.C.TIME_UNSET,
            transientNetworkRetryDelayMs(refused, TRANSIENT_NETWORK_LOAD_RETRIES + 1)
        )
        assertEquals(
            androidx.media3.common.C.TIME_UNSET,
            transientNetworkRetryDelayMs(IOException("stream expired, http 403 forbidden"), 1)
        )
    }
}
