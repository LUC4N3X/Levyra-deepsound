package com.luc4n3x.levyra.extraction.audio

import java.util.concurrent.atomic.AtomicBoolean
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
        onAttemptError: (Throwable) -> Unit = {},
        quality: StreamQuality<T>? = null
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
        val stream = hedgedFirst(guarded, hedgeBudgetMs, quality) { error ->
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
            throw cancellation
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
            throw cancellation
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
        selection: StreamSelection<T>,
        onAttemptError: (Throwable) -> Unit
    ) {
        val outcome = try {
            attemptOnce(attempt, onAttemptError)
        } catch (cancellation: CancellationException) {
            selection.fail(cancellation)
            throw cancellation
        }
        selection.offer(outcome)
    }

    private suspend fun <T> hedgedFirst(
        tiers: List<List<suspend () -> T?>>,
        hedgeBudgetMs: Long,
        quality: StreamQuality<T>?,
        onAttemptError: (Throwable) -> Unit
    ): T? {
        val ladder = tiers.filter { it.isNotEmpty() }
        if (ladder.isEmpty()) return null
        if (ladder.size == 1 && ladder[0].size == 1) {
            return attemptOnce(ladder[0][0], onAttemptError)
        }
        return coroutineScope {
            val selection = StreamSelection(quality)
            fun dispatch(attempt: suspend () -> T?) {
                selection.started()
                launch {
                    settleAttempt(attempt, selection, onAttemptError)
                }
            }
            launch {
                for ((index, tier) in ladder.withIndex()) {
                    if (selection.first.isCompleted) break
                    tier.forEach { dispatch(it) }
                    if (index != ladder.lastIndex) {
                        val settled = withTimeoutOrNull(hedgeBudgetMs) { selection.first.await() } != null ||
                            selection.first.isCompleted
                        if (settled) break
                    }
                }
                selection.closeDispatch()
            }
            val first = selection.first.await()
            val result = if (first == null || quality == null || quality.graceMs == 0L) {
                first
            } else {
                withTimeoutOrNull(quality.graceMs) { selection.settled.await() }
                selection.best()
            }
            coroutineContext.cancelChildren()
            result
        }
    }

    private class StreamSelection<T>(private val quality: StreamQuality<T>?) {
        val first = CompletableDeferred<T?>()
        val settled = CompletableDeferred<Unit>()
        private var started = 0
        private var finished = 0
        private var dispatchClosed = false
        private var best: T? = null
        private var bestRank = Int.MIN_VALUE

        fun started() {
            synchronized(this) { started++ }
        }

        fun closeDispatch() {
            val drained = synchronized(this) {
                dispatchClosed = true
                finished == started
            }
            if (drained) completeDrained()
        }

        fun offer(candidate: T?) {
            if (candidate != null) {
                val rank = quality?.rank?.invoke(candidate) ?: 0
                val sufficient = synchronized(this) {
                    if (best == null || rank > bestRank) {
                        best = candidate
                        bestRank = rank
                    }
                    quality == null || bestRank >= quality.sufficientRank
                }
                first.complete(candidate)
                if (sufficient) settled.complete(Unit)
            }
            val drained = synchronized(this) {
                finished++
                dispatchClosed && finished == started
            }
            if (drained) completeDrained()
        }

        private fun completeDrained() {
            first.complete(null)
            settled.complete(Unit)
        }

        fun fail(cancellation: CancellationException) {
            first.completeExceptionally(cancellation)
            settled.completeExceptionally(cancellation)
        }

        fun best(): T? = synchronized(this) { best }
    }
}

class StreamQuality<in T>(
    val rank: (T) -> Int,
    val sufficientRank: Int,
    val graceMs: Long
) {
    init {
        require(graceMs >= 0L)
    }
}
