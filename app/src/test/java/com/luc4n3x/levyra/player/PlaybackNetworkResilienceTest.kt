package com.luc4n3x.levyra.player

import androidx.media3.common.Player
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackNetworkResilienceTest {
    private val automatic = PlaybackBufferProfile(12_000, 24_000, 100, 300, 4_000)

    @Test
    fun customBufferProfileKeepsPrecedenceOverStabilityEscalation() {
        val custom = PlaybackBufferProfile(30_000, 50_000, 1_000, 2_000, 4_000)
        assertSame(custom, stabilityBufferProfile(custom, customProfile = true))
    }

    @Test
    fun automaticProfileGainsBoundedStabilityHeadroom() {
        val stable = stabilityBufferProfile(automatic, customProfile = false)
        assertTrue(stable.minBufferMs > automatic.minBufferMs)
        assertTrue(stable.maxBufferMs > automatic.maxBufferMs)
        assertTrue(stable.maxBufferMs <= 60_000)
        assertEquals(automatic.playbackBufferMs, stable.playbackBufferMs)
    }

    @Test
    fun watchdogWaitsForNetworkInsteadOfRebuildingOnlinePlayback() {
        assertTrue(shouldDeferWatchdogForConnectivity(localPlayback = false, hasInternet = false))
        assertFalse(shouldDeferWatchdogForConnectivity(localPlayback = false, hasInternet = true))
        assertFalse(shouldDeferWatchdogForConnectivity(localPlayback = true, hasInternet = false))
    }

    @Test
    fun crossfadeHandoffResyncsOnlyAgainstReadySecondary() {
        assertTrue(crossfadeHandoffCanResync(Player.STATE_READY))
        assertFalse(crossfadeHandoffCanResync(Player.STATE_BUFFERING))
        assertFalse(crossfadeHandoffCanResync(Player.STATE_IDLE))
        assertFalse(crossfadeHandoffCanResync(Player.STATE_ENDED))
    }

    @Test
    fun missingCapabilitiesAreNotUsableInternet() {
        assertFalse(hasUsableInternet(null))
    }

    @Test
    fun transientLoadRetriesStayBoundedWithBackoff() {
        val delays = (1..TRANSIENT_NETWORK_LOAD_RETRIES).map {
            transientNetworkRetryDelayMs(SocketTimeoutException("timeout"), it)
        }
        assertEquals(listOf(1_000L, 2_000L, 4_000L, 5_000L, 5_000L, 5_000L, 5_000L, 5_000L), delays)
        assertEquals(
            androidx.media3.common.C.TIME_UNSET,
            transientNetworkRetryDelayMs(UnknownHostException("offline"), TRANSIENT_NETWORK_LOAD_RETRIES + 1)
        )
    }
}
