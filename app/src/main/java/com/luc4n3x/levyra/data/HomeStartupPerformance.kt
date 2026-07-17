package com.luc4n3x.levyra.data

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.concurrent.atomic.AtomicReference

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
                idleWindowMs = 520L,
                secondaryStartDelayMs = 800L,
                priorityArtworkCount = 4,
                refreshedArtworkCount = 6,
                chartArtworkCount = 5,
                persistentArtworkCount = 2,
                chartEnrichmentCount = 3,
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
            lowRam -> StartupPlaybackWarmPlan(delayMs = 1_000L, trackCount = 1, concurrency = 1)
            powerConstrained -> StartupPlaybackWarmPlan(delayMs = 900L, trackCount = 1, concurrency = 1)
            else -> StartupPlaybackWarmPlan(
                delayMs = 700L,
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
