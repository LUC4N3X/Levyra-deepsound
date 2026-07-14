package com.luc4n3x.levyra.data

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

internal data class HomeStartupWorkPlan(
    val idleWindowMs: Long,
    val secondaryStartDelayMs: Long,
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
                idleWindowMs = 700L,
                secondaryStartDelayMs = 900L,
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
                idleWindowMs = 600L,
                secondaryStartDelayMs = 800L,
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
                idleWindowMs = 420L,
                secondaryStartDelayMs = 550L,
                priorityArtworkCount = 6,
                refreshedArtworkCount = 10,
                chartArtworkCount = 8,
                persistentArtworkCount = 4,
                chartEnrichmentCount = 6,
                chartEnrichmentConcurrency = 2,
                chartWarmCount = 2,
                releaseRadarArtistCount = 6,
                releasesPerArtist = 6
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
            lowRam -> StartupPlaybackWarmPlan(delayMs = 180L, trackCount = 1, concurrency = 1)
            powerConstrained -> StartupPlaybackWarmPlan(delayMs = 140L, trackCount = 1, concurrency = 1)
            else -> StartupPlaybackWarmPlan(
                delayMs = 100L,
                trackCount = 3,
                concurrency = preferredConcurrency.coerceIn(1, 2)
            )
        }
    }
}

internal class HomeInteractionGate(
    private val nowMs: () -> Long = { System.nanoTime() / 1_000_000L }
) {
    @Volatile
    private var scrolling = false

    @Volatile
    private var lastInteractionMs = nowMs()

    fun update(isScrolling: Boolean) {
        if (scrolling == isScrolling) return
        scrolling = isScrolling
        lastInteractionMs = nowMs()
    }

    fun remainingIdleMs(idleWindowMs: Long): Long {
        if (scrolling) return idleWindowMs.coerceAtLeast(1L)
        return (idleWindowMs - (nowMs() - lastInteractionMs)).coerceAtLeast(0L)
    }

    suspend fun awaitIdle(idleWindowMs: Long) {
        val safeWindow = idleWindowMs.coerceAtLeast(0L)
        while (currentCoroutineContext().isActive) {
            val remaining = remainingIdleMs(safeWindow)
            if (!scrolling && remaining == 0L) return
            delay(if (scrolling) 80L else remaining.coerceIn(40L, 120L))
        }
    }
}
