package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.data.network.byedpi.ByeDpiFailureKind
import com.luc4n3x.levyra.data.network.byedpi.ByeDpiHealthPolicy
import com.luc4n3x.levyra.data.network.byedpi.ByeDpiHealthState
import com.luc4n3x.levyra.data.network.byedpi.ByeDpiSupervisor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

internal interface ByeDpiHealthGateway {
    fun state(): ByeDpiHealthState
    fun isProbeDue(): Boolean
    fun recordSuccess(latencyMs: Long)
    fun recordFailure(kind: ByeDpiFailureKind)
}

internal object SupervisorByeDpiHealthGateway : ByeDpiHealthGateway {
    override fun state(): ByeDpiHealthState = ByeDpiSupervisor.healthState()
    override fun isProbeDue(): Boolean = ByeDpiSupervisor.isRecoveryProbeDue()
    override fun recordSuccess(latencyMs: Long) = ByeDpiSupervisor.recordResolutionSuccess(latencyMs)
    override fun recordFailure(kind: ByeDpiFailureKind) = ByeDpiSupervisor.recordResolutionFailure(kind)
}

internal class AdaptiveYoutubeRescue(
    private val health: ByeDpiHealthGateway = SupervisorByeDpiHealthGateway,
    private val resolutionBudgetMs: Long = ByeDpiHealthPolicy.RESOLUTION_BUDGET_MS,
    private val recoveryProbeBudgetMs: Long = ByeDpiHealthPolicy.RECOVERY_PROBE_BUDGET_MS
) {
    private data class DirectAttempt<T>(
        val value: T?,
        val timedOut: Boolean
    )

    suspend fun <T> resolve(
        byeDpiEnabled: Boolean,
        direct: suspend () -> T?,
        piped: suspend () -> T?
    ): T? {
        if (!byeDpiEnabled) return direct() ?: piped()
        val state = health.state()
        if (state == ByeDpiHealthState.DEGRADED || state == ByeDpiHealthState.PROBE) {
            if (state == ByeDpiHealthState.DEGRADED && health.isProbeDue()) {
                return raceRecoveryProbe(direct, piped)
            }
            piped()?.let { return it }
            return directWithinBudget(recoveryProbeBudgetMs, direct).value
        }

        val primary = directWithinBudget(resolutionBudgetMs, direct)
        primary.value?.let { return it }
        piped()?.let { return it }
        return null
    }

    private suspend fun <T> directWithinBudget(
        budgetMs: Long,
        direct: suspend () -> T?
    ): DirectAttempt<T> {
        val startedAt = System.nanoTime()
        val attempt = withTimeoutOrNull(budgetMs) {
            val value = direct()
            val latencyMs = elapsedMs(startedAt)
            if (value == null) {
                health.recordFailure(ByeDpiFailureKind.RESOLUTION)
            } else {
                health.recordSuccess(latencyMs)
            }
            DirectAttempt(value, timedOut = false)
        }
        return if (attempt == null) {
            health.recordFailure(ByeDpiFailureKind.TIMEOUT)
            DirectAttempt(null, timedOut = true)
        } else attempt
    }

    private suspend fun <T> raceRecoveryProbe(
        direct: suspend () -> T?,
        piped: suspend () -> T?
    ): T? = coroutineScope {
        val winner = CompletableDeferred<T?>()
        val remaining = AtomicInteger(2)
        fun settle(value: T?) {
            if (value != null) {
                winner.complete(value)
            } else if (remaining.decrementAndGet() == 0) {
                winner.complete(null)
            }
        }
        launch {
            val value = try {
                piped()
            } catch (cancellation: CancellationException) {
                throw cancellation
            }
            settle(value)
        }
        launch {
            settle(directWithinBudget(recoveryProbeBudgetMs, direct).value)
        }
        val result = winner.await()
        coroutineContext.cancelChildren()
        result
    }

    private fun elapsedMs(startedAtNanos: Long): Long =
        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos).coerceAtLeast(1L)
}
