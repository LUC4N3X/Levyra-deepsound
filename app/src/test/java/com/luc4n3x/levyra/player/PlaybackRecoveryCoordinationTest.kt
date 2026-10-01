package com.luc4n3x.levyra.player

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackRecoveryCoordinationTest {

    @Test
    fun activeRecoveryRestoresWhilePlaybackIsStillIntended() {
        assertFalse(shouldAbortActiveRecoveryRestore(activeRecovery = true, playWhenReady = true))
    }

    @Test
    fun pauseDuringActiveRecoveryResolutionAbortsTheRestore() {
        var playWhenReady = true
        val startedWithIntent = !shouldAbortActiveRecoveryRestore(activeRecovery = true, playWhenReady = playWhenReady)
        playWhenReady = false
        assertTrue(startedWithIntent)
        assertTrue(shouldAbortActiveRecoveryRestore(activeRecovery = true, playWhenReady = playWhenReady))
    }

    @Test
    fun stickyRestoreIsNotGatedOnPlayWhenReady() {
        assertFalse(shouldAbortActiveRecoveryRestore(activeRecovery = false, playWhenReady = false))
        assertFalse(shouldAbortActiveRecoveryRestore(activeRecovery = false, playWhenReady = true))
    }

    @Test
    fun watchdogRestoresWithoutProgressAreBounded() {
        val allowance = WatchdogRecoveryAllowance()
        var restores = 0
        repeat(20) {
            if (allowance.isAvailable()) {
                restores++
                allowance.consume()
            }
        }
        assertEquals(WATCHDOG_RECOVERIES_WITHOUT_PROGRESS, restores)
    }

    @Test
    fun genuinePlaybackProgressRestoresTheWatchdogAllowance() {
        val allowance = WatchdogRecoveryAllowance()
        allowance.consume()
        assertFalse(allowance.isAvailable())
        if (isGenuineWatchdogProgress(previousPositionMs = 42_000L, positionMs = 47_000L)) allowance.reset()
        assertTrue(allowance.isAvailable())
    }

    @Test
    fun isolatedStallStillGetsAWatchdogRecovery() {
        assertTrue(WatchdogRecoveryAllowance().isAvailable())
    }

    @Test
    fun stalledOrResetPositionsAreNotGenuineProgress() {
        assertFalse(isGenuineWatchdogProgress(previousPositionMs = C.TIME_UNSET, positionMs = 42_000L))
        assertFalse(isGenuineWatchdogProgress(previousPositionMs = 42_000L, positionMs = 42_000L))
        assertFalse(isGenuineWatchdogProgress(previousPositionMs = 42_000L, positionMs = 42_200L))
        assertFalse(isGenuineWatchdogProgress(previousPositionMs = 42_000L, positionMs = 10_000L))
    }
}
