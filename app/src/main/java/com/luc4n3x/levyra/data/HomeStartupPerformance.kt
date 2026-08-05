package com.luc4n3x.levyra.data

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.concurrent.atomic.AtomicReference

/** Keeps nonessential startup work away from first render and active audio playback. */
internal data class HomeStartupWorkPlan(
    val idleWindowMs: Long,
    val homeFeedStartDelayMs: Long,
    val secondaryStartDelayMs: Long,
    val albumStartDelayMs: Long,
    val artistStartDelayMs: Long,
    val chartRefreshStartDelayMs: Long,
    val chartPrefetchStartDelayMs: Long,
    val chartMemoryWarmStartDelayMs: Long,
    val maintenanceStartDelayMs: Long,
    val activePlaybackProtectionMs: Long,
    val albumSeedCount: Int,
    val albumCandidateCount: Int,
    val albumConcurrency: Int,
    val priorityArtworkCount: Int,
    val refreshedArtworkCount: Int,
    val chartArtworkCount: Int,
    val persistentArtworkCount: Int,
    val chartEnrichmentCount: Int,
    val chartEnrichmentConcurrency: Int,
    val chartWarmCount: Int,
    val releaseRadarArtistCount: Int,
    val releasesPerArtist: Int
)

internal object HomeStartupWorkPolicy {
    fun create(lowRam: Boolean, powerConstrained: Boolean): HomeStartupWorkPlan {
        return when {
            lowRam -> HomeStartupWorkPlan(
                idleWindowMs = 900L,
                homeFeedStartDelayMs = 1_400L,
                secondaryStartDelayMs = 6_000L,
                albumStartDelayMs = 9_000L,
                artistStartDelayMs = 12_000L,
                chartRefreshStartDelayMs = 4_500L,
                chartPrefetchStartDelayMs = 9_000L,
                chartMemoryWarmStartDelayMs = 13_000L,
                maintenanceStartDelayMs = 20_000L,
                activePlaybackProtectionMs = 14_000L,
                albumSeedCount = 4,
                albumCandidateCount = 9,
                albumConcurrency = 1,
                priorityArtworkCount = 2,
                refreshedArtworkCount = 4,
                chartArtworkCount = 3,
                persistentArtworkCount = 1,
                chartEnrichmentCount = 1,
                chartEnrichmentConcurrency = 1,
                chartWarmCount = 0,
                releaseRadarArtistCount = 2,
                releasesPerArtist = 4
            )
            powerConstrained -> HomeStartupWorkPlan(
                idleWindowMs = 800L,
                homeFeedStartDelayMs = 1_100L,
                secondaryStartDelayMs = 5_000L,
                albumStartDelayMs = 7_500L,
                artistStartDelayMs = 10_000L,
                chartRefreshStartDelayMs = 3_800L,
                chartPrefetchStartDelayMs = 7_500L,
                chartMemoryWarmStartDelayMs = 11_000L,
                maintenanceStartDelayMs = 18_000L,
                activePlaybackProtectionMs = 12_000L,
                albumSeedCount = 5,
                albumCandidateCount = 11,
                albumConcurrency = 1,
                priorityArtworkCount = 3,
                refreshedArtworkCount = 5,
                chartArtworkCount = 4,
                persistentArtworkCount = 1,
                chartEnrichmentCount = 2,
                chartEnrichmentConcurrency = 1,
                chartWarmCount = 1,
                releaseRadarArtistCount = 3,
                releasesPerArtist = 5
            )
            else -> HomeStartupWorkPlan(
                idleWindowMs = 700L,
                homeFeedStartDelayMs = 850L,
                secondaryStartDelayMs = 4_200L,
                albumStartDelayMs = 6_500L,
                artistStartDelayMs = 8_500L,
                chartRefreshStartDelayMs = 3_000L,
                chartPrefetchStartDelayMs = 6_500L,
                chartMemoryWarmStartDelayMs = 9_500L,
                maintenanceStartDelayMs = 15_000L,
                activePlaybackProtectionMs = 10_000L,
                albumSeedCount = 6,
                albumCandidateCount = 12,
                albumConcurrency = 1,
                priorityArtworkCount = 3,
                refreshedArtworkCount = 5,
                chartArtworkCount = 4,
                persistentArtworkCount = 1,
                chartEnrichmentCount = 2,
                chartEnrichmentConcurrency = 1,
                chartWarmCount = 1,
                releaseRadarArtistCount = 4,
                releasesPerArtist = 5
            )
        }
    }
}

internal data class StartupPlaybackWarmPlan(
    val delayMs: Long,
    val trackCount: Int,
    val concurrency: Int
)

internal object StartupPlaybackWarmPolicy {
    fun create(lowRam: Boolean, powerConstrained: Boolean, preferredConcurrency: Int): StartupPlaybackWarmPlan {
        return when {
            lowRam -> StartupPlaybackWarmPlan(delayMs = 18_000L, trackCount = 1, concurrency = 1)
            powerConstrained -> StartupPlaybackWarmPlan(delayMs = 15_000L, trackCount = 1, concurrency = 1)
            else -> StartupPlaybackWarmPlan(
                delayMs = 12_000L,
                trackCount = 1,
                concurrency = preferredConcurrency.coerceIn(1, 1)
            )
        }
    }
}

internal class HomeInteractionGate(
    private val nowMs: () -> Long = { System.nanoTime() / 1_000_000L }
) {
    private data class InteractionState(
        val scrolling: Boolean,
        val lastInteractionMs: Long
    )

    private data class IdleStatus(
        val scrolling: Boolean,
        val remainingMs: Long
    )

    private val state = AtomicReference(
        InteractionState(
            scrolling = false,
            lastInteractionMs = nowMs()
        )
    )

    fun update(isScrolling: Boolean) {
        while (true) {
            val current = state.get()
            if (current.scrolling == isScrolling) return
            val updated = InteractionState(
                scrolling = isScrolling,
                lastInteractionMs = nowMs()
            )
            if (state.compareAndSet(current, updated)) return
        }
    }

    fun remainingIdleMs(idleWindowMs: Long): Long {
        return idleStatus(idleWindowMs).remainingMs
    }

    suspend fun awaitIdle(idleWindowMs: Long) {
        val safeWindow = idleWindowMs.coerceAtLeast(0L)
        while (currentCoroutineContext().isActive) {
            val status = idleStatus(safeWindow)
            if (!status.scrolling && status.remainingMs == 0L) return
            delay(if (status.scrolling) 80L else status.remainingMs.coerceIn(40L, 120L))
        }
    }

    private fun idleStatus(idleWindowMs: Long): IdleStatus {
        val safeWindow = idleWindowMs.coerceAtLeast(0L)
        val snapshot = state.get()
        if (snapshot.scrolling) {
            return IdleStatus(
                scrolling = true,
                remainingMs = safeWindow.coerceAtLeast(1L)
            )
        }
        return IdleStatus(
            scrolling = false,
            remainingMs = (safeWindow - (nowMs() - snapshot.lastInteractionMs)).coerceAtLeast(0L)
        )
    }
}
