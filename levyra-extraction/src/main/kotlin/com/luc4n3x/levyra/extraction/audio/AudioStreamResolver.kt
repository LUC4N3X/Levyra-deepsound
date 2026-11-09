package com.luc4n3x.levyra.extraction.audio

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

object AudioStreamResolver {
    const val MAX_NORMAL_ATTEMPTS: Int = 32

    suspend fun <T> resolve(
        tiers: List<List<suspend () -> T?>>,
        hedgeBudgetMs: Long,
        recovery: (suspend () -> T?)? = null,
        isNoPlayableStream: (Throwable) -> Boolean = { false },
        onAttemptError: (Throwable) -> Unit = {}
    ): AudioResolution<T> {
        require(hedgeBudgetMs >= 0L)
        require(tiers.size <= MAX_NORMAL_ATTEMPTS)
        require(tiers.sumOf { it.size.toLong() } <= MAX_NORMAL_ATTEMPTS)
        currentCoroutineContext().ensureActive()
        val inconclusive = AtomicBoolean(false)
        val guarded = tiers.map { tier ->
            tier.map { attempt ->
                suspend { attempt().also { if (it == null) inconclusive.set(true) } }
            }
        }
        val stream = hedgedFirst(guarded, hedgeBudgetMs) { error ->
            if (!isNoPlayableStream(error)) inconclusive.set(true)
            onAttemptError(error)
        }
        currentCoroutineContext().ensureActive()
        if (stream != null) return AudioResolution.Resolved(stream)
        if (inconclusive.get() || tiers.none { it.isNotEmpty() }) return AudioResolution.Inconclusive
        if (recovery == null) return AudioResolution.NoPlayableStream
        return recoverOnce(recovery, isNoPlayableStream, onAttemptError)
    }

    private suspend fun <T> recoverOnce(
        recovery: suspend () -> T?,
        isNoPlayableStream: (Throwable) -> Boolean,
        onAttemptError: (Throwable) -> Unit
    ): AudioResolution<T> {
        currentCoroutineContext().ensureActive()
        return try {
            val recovered = recovery()
            currentCoroutineContext().ensureActive()
            recovered?.let { AudioResolution.Resolved(it) } ?: AudioResolution.Inconclusive
        } catch (cancellation: CancellationException) {
            currentCoroutineContext().ensureActive()
            onAttemptError(cancellation)
            AudioResolution.Inconclusive
        } catch (error: Exception) {
            currentCoroutineContext().ensureActive()
            onAttemptError(error)
            val conclusive = isNoPlayableStream(error)
            currentCoroutineContext().ensureActive()
            if (conclusive) AudioResolution.NoPlayableStream else AudioResolution.Inconclusive
        }
    }

    private suspend fun <T> attemptOnce(
        attempt: suspend () -> T?,
        onAttemptError: (Throwable) -> Unit
    ): T? {
        currentCoroutineContext().ensureActive()
        val candidate = try {
            attempt()
        } catch (cancellation: CancellationException) {
            currentCoroutineContext().ensureActive()
            onAttemptError(cancellation)
            null
        } catch (error: Throwable) {
            currentCoroutineContext().ensureActive()
            onAttemptError(error)
            null
        }
        currentCoroutineContext().ensureActive()
        return candidate
    }

    private suspend fun <T> settleAttempt(
        attempt: suspend () -> T?,
        winner: CompletableDeferred<T?>,
        remaining: AtomicInteger,
        onAttemptError: (Throwable) -> Unit
    ) {
        val outcome = try {
            attemptOnce(attempt, onAttemptError)
        } catch (cancellation: CancellationException) {
            winner.completeExceptionally(cancellation)
            throw cancellation
        }
        if (outcome != null) {
            winner.complete(outcome)
        } else if (remaining.decrementAndGet() == 0) {
            winner.complete(null)
        }
    }

    private suspend fun <T> hedgedFirst(
        tiers: List<List<suspend () -> T?>>,
        hedgeBudgetMs: Long,
        onAttemptError: (Throwable) -> Unit
    ): T? {
        val ladder = tiers.filter { it.isNotEmpty() }
        if (ladder.isEmpty()) return null
        if (ladder.size == 1 && ladder[0].size == 1) {
            return attemptOnce(ladder[0][0], onAttemptError)
        }
        return coroutineScope {
            val winner = CompletableDeferred<T?>()
            val remaining = AtomicInteger(ladder.sumOf { it.size })
            fun dispatch(attempt: suspend () -> T?) {
                launch {
                    settleAttempt(attempt, winner, remaining, onAttemptError)
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
}
