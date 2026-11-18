package com.luc4n3x.levyra.player.offline.work

import com.luc4n3x.levyra.domain.DownloadOwnership
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.delay

internal enum class OfflineTransferLane {
    Preload,
    Manual,
    SmartOffline;

    companion object {
        fun of(ownership: DownloadOwnership): OfflineTransferLane = when (ownership) {
            DownloadOwnership.MANUAL -> Manual
            DownloadOwnership.SMART_OFFLINE -> SmartOffline
        }
    }
}

internal fun offlineDownloadSlotAvailable(
    lane: OfflineTransferLane,
    active: Int,
    limit: Int,
    waitingByLane: Map<OfflineTransferLane, Int>
): Boolean {
    if (active >= limit.coerceIn(1, 4)) return false
    return OfflineTransferLane.entries
        .filter { it.ordinal < lane.ordinal }
        .none { (waitingByLane[it] ?: 0) > 0 }
}

internal const val PRELOAD_PRIORITY_TRACKS = 3

internal object OfflineTransferPriority {
    private const val POLL_MS = 120L
    private val lock = Any()
    private var active = 0
    private val waiting = mutableMapOf<OfflineTransferLane, Int>()
    private val preloading = AtomicInteger(0)

    suspend fun <T> withDownloadSlot(ownership: DownloadOwnership, limit: Int, block: suspend () -> T): T {
        val lane = OfflineTransferLane.of(ownership)
        synchronized(lock) { waiting[lane] = (waiting[lane] ?: 0) + 1 }
        var acquired = false
        try {
            while (!acquired) {
                acquired = synchronized(lock) {
                    val available = offlineDownloadSlotAvailable(lane, active, limit, waiting + (OfflineTransferLane.Preload to preloading.get()))
                    if (available) {
                        active += 1
                        waiting[lane] = ((waiting[lane] ?: 1) - 1).coerceAtLeast(0)
                    }
                    available
                }
                if (!acquired) delay(POLL_MS)
            }
        } finally {
            if (!acquired) synchronized(lock) { waiting[lane] = ((waiting[lane] ?: 1) - 1).coerceAtLeast(0) }
        }
        return try {
            block()
        } finally {
            synchronized(lock) { active = (active - 1).coerceAtLeast(0) }
        }
    }

    fun beginPreload() {
        preloading.incrementAndGet()
    }

    fun endPreload() {
        preloading.updateAndGet { (it - 1).coerceAtLeast(0) }
    }
}
