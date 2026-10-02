package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.data.network.byedpi.ByeDpiFailureKind
import com.luc4n3x.levyra.data.network.byedpi.ByeDpiHealthPolicy
import com.luc4n3x.levyra.data.network.byedpi.ByeDpiHealthState
import com.luc4n3x.levyra.data.network.byedpi.ByeDpiSupervisor
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
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
    private val recoveryProbeBudgetMs: Long = ByeDpiHealthPolicy.RECOVERY_PROBE_BUDGET_MS,
    private val directScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private data class DirectAttempt<T>(val value: T?)

    suspend fun <T> resolve(
        byeDpiEnabled: Boolean,
        direct: suspend () -> T?,
        piped: suspend () -> T?
    ): T? {
        if (!byeDpiEnabled) return direct() ?: piped()

        piped()?.let { return it }

        val state = health.state()
        val directBudgetMs = if (
            state == ByeDpiHealthState.DEGRADED ||
            state == ByeDpiHealthState.PROBE ||
            health.isProbeDue()
        ) {
            recoveryProbeBudgetMs
        } else {
            resolutionBudgetMs
        }
        return directWithinBudget(directBudgetMs, direct).value
    }

    private suspend fun <T> directWithinBudget(
        budgetMs: Long,
        direct: suspend () -> T?
    ): DirectAttempt<T> {
        val startedAt = System.nanoTime()
        val pending = directScope.async { direct() }
        val attempt = try {
            withTimeoutOrNull(budgetMs) { DirectAttempt(pending.await()) }
        } catch (cancellation: CancellationException) {
            pending.cancel()
            throw cancellation
        }
        if (attempt == null) {
            pending.cancel()
            health.recordFailure(ByeDpiFailureKind.TIMEOUT)
            return DirectAttempt(null)
        }
        if (attempt.value == null) {
            health.recordFailure(ByeDpiFailureKind.RESOLUTION)
        } else {
            health.recordSuccess(elapsedMs(startedAt))
        }
        return attempt
    }

    private fun elapsedMs(startedAtNanos: Long): Long =
        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos).coerceAtLeast(1L)
}
