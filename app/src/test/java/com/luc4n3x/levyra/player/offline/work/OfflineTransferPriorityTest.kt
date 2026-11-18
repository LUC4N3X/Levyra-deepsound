package com.luc4n3x.levyra.player.offline.work

import com.luc4n3x.levyra.domain.DownloadOwnership
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineTransferPriorityTest {
    private fun waiting(vararg pairs: Pair<OfflineTransferLane, Int>) = mapOf(*pairs)

    @Test
    fun freeSlotGoesToAnyLaneWhenNobodyHigherWaits() {
        assertTrue(offlineDownloadSlotAvailable(OfflineTransferLane.Manual, active = 0, limit = 2, waitingByLane = waiting()))
        assertTrue(offlineDownloadSlotAvailable(OfflineTransferLane.SmartOffline, active = 1, limit = 2, waitingByLane = waiting(OfflineTransferLane.SmartOffline to 3)))
    }

    @Test
    fun preloadHoldsBackManualAndSmartOffline() {
        val preloading = waiting(OfflineTransferLane.Preload to 1)
        assertFalse(offlineDownloadSlotAvailable(OfflineTransferLane.Manual, active = 0, limit = 4, waitingByLane = preloading))
        assertFalse(offlineDownloadSlotAvailable(OfflineTransferLane.SmartOffline, active = 0, limit = 4, waitingByLane = preloading))
    }

    @Test
    fun manualQueueGoesBeforeSmartOffline() {
        val manualWaiting = waiting(OfflineTransferLane.Manual to 2, OfflineTransferLane.SmartOffline to 5)
        assertTrue(offlineDownloadSlotAvailable(OfflineTransferLane.Manual, active = 0, limit = 1, waitingByLane = manualWaiting))
        assertFalse(offlineDownloadSlotAvailable(OfflineTransferLane.SmartOffline, active = 0, limit = 1, waitingByLane = manualWaiting))
    }

    @Test
    fun concurrencyLimitStaysClampedBetweenOneAndFour() {
        assertFalse(offlineDownloadSlotAvailable(OfflineTransferLane.Manual, active = 1, limit = 0, waitingByLane = waiting()))
        assertFalse(offlineDownloadSlotAvailable(OfflineTransferLane.Manual, active = 4, limit = 9, waitingByLane = waiting()))
        assertTrue(offlineDownloadSlotAvailable(OfflineTransferLane.Manual, active = 3, limit = 9, waitingByLane = waiting()))
    }

    @Test
    fun ownershipMapsToItsLane() {
        assertEquals(OfflineTransferLane.Manual, OfflineTransferLane.of(DownloadOwnership.MANUAL))
        assertEquals(OfflineTransferLane.SmartOffline, OfflineTransferLane.of(DownloadOwnership.SMART_OFFLINE))
    }
}
