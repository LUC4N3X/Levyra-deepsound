package com.luc4n3x.levyra.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicInteger

internal object LevyraResolverLatency {
    const val INNER_TUBE_HEDGE_BUDGET_MS = 55L
    private const val AUDIO_EXTRACTOR_HEDGE_MS = 35L
    private const val VIDEO_EXTRACTOR_HEDGE_MS = 0L

    fun extractorHedgeDelayMs(isVideoMode: Boolean, preferMp4Audio: Boolean): Long {
        if (preferMp4Audio) return 0L
        return if (isVideoMode) VIDEO_EXTRACTOR_HEDGE_MS else AUDIO_EXTRACTOR_HEDGE_MS
    }
}

internal suspend fun <T> hedgedFirst(
    tiers: List<List<suspend () -> T?>>,
    hedgeBudgetMs: Long,
    onAttemptError: (Throwable) -> Unit = {}
): T? {
    val ladder = tiers.filter { it.isNotEmpty() }
    if (ladder.isEmpty()) return null
    if (ladder.size == 1 && ladder[0].size == 1) {
        return try {
            ladder[0][0].invoke()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            onAttemptError(error)
            null
        }
    }
    return coroutineScope {
        val winner = CompletableDeferred<T?>()
        val remaining = AtomicInteger(ladder.sumOf { it.size })
        fun dispatch(attempt: suspend () -> T?) {
            launch {
                val outcome = try {
                    attempt()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (error: Throwable) {
                    onAttemptError(error)
                    null
                }
                if (outcome != null) {
                    winner.complete(outcome)
                } else if (remaining.decrementAndGet() == 0) {
                    winner.complete(null)
                }
            }
        }
        launch {
            for ((index, tier) in ladder.withIndex()) {
                if (winner.isCompleted) break
                tier.forEach { dispatch(it) }
                if (index != ladder.lastIndex) {
                    val settled = withTimeoutOrNull(hedgeBudgetMs) { winner.await() } != null || winner.isCompleted
                    if (settled) break
                }
            }
        }
        val result = winner.await()
        coroutineContext.cancelChildren()
        result
    }
}
