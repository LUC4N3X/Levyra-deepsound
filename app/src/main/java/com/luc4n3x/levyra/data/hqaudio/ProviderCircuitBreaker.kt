package com.luc4n3x.levyra.data.hqaudio

internal class ProviderCircuitBreaker(
    private val providerId: String,
    private val clock: () -> Long,
    private val failureThreshold: Int = DEFAULT_FAILURE_THRESHOLD,
    private val baseOpenMs: Long = DEFAULT_OPEN_MS,
    private val maxOpenMs: Long = MAX_OPEN_MS
) {
    enum class State { CLOSED, OPEN, HALF_OPEN }

    enum class Permit { NORMAL, PROBE, REJECTED }

    private val lock = Any()
    private var state = State.CLOSED
    private var consecutiveFailures = 0
    private var openUntilMs = 0L
    private var openDurationMs = baseOpenMs
    private var probeInFlight = false
    private var lastSuccessAtMs = 0L
    private var lastFailureAtMs = 0L
    private var lastFailure = ""
    private var lastLatencyMs = -1L

    val currentState: State
        get() = synchronized(lock) { state }

    fun snapshot(ownerProviderId: String): ProviderBackendHealth = synchronized(lock) {
        val now = clock()
        ProviderBackendHealth(
            providerId = ownerProviderId,
            backend = providerId,
            state = if (state == State.OPEN && now >= openUntilMs) State.HALF_OPEN.name else state.name,
            cooldownRemainingMs = if (state == State.OPEN) (openUntilMs - now).coerceAtLeast(0L) else 0L,
            consecutiveFailures = consecutiveFailures,
            lastSuccessAtMs = lastSuccessAtMs,
            lastFailureAtMs = lastFailureAtMs,
            lastFailure = lastFailure,
            lastLatencyMs = lastLatencyMs
        )
    }

    fun acquire(): Permit {
        var probing = false
        val permit = synchronized(lock) {
            when (state) {
                State.CLOSED -> Permit.NORMAL
                State.OPEN -> if (clock() < openUntilMs) {
                    Permit.REJECTED
                } else {
                    state = State.HALF_OPEN
                    probeInFlight = true
                    probing = true
                    Permit.PROBE
                }
                State.HALF_OPEN -> if (probeInFlight) {
                    Permit.REJECTED
                } else {
                    probeInFlight = true
                    probing = true
                    Permit.PROBE
                }
            }
        }
        if (probing) HighQualityAudioDiagnostics.circuit(providerId, State.HALF_OPEN.name, "probe")
        return permit
    }

    fun onSuccess(permit: Permit, latencyMs: Long = -1L) {
        val recovered = synchronized(lock) {
            lastSuccessAtMs = clock()
            lastLatencyMs = latencyMs
            when {
                permit == Permit.PROBE && state == State.HALF_OPEN -> {
                    state = State.CLOSED
                    consecutiveFailures = 0
                    openDurationMs = baseOpenMs
                    probeInFlight = false
                    true
                }
                state == State.CLOSED -> {
                    consecutiveFailures = 0
                    false
                }
                else -> false
            }
        }
        if (recovered) HighQualityAudioDiagnostics.circuit(providerId, State.CLOSED.name, "recovered")
    }

    fun onFailure(permit: Permit, cause: String = "") {
        val openedForMs = synchronized(lock) {
            lastFailureAtMs = clock()
            lastFailure = cause
            when {
                permit == Permit.PROBE && state == State.HALF_OPEN -> {
                    openDurationMs = (openDurationMs * 2).coerceAtMost(maxOpenMs)
                    open()
                }
                state == State.CLOSED -> {
                    consecutiveFailures += 1
                    if (consecutiveFailures >= failureThreshold) open() else null
                }
                else -> null
            }
        }
        openedForMs?.let {
            HighQualityAudioDiagnostics.circuit(providerId, State.OPEN.name, "openMs=$it cause=${cause.ifBlank { "-" }}")
        }
    }

    fun release(permit: Permit) {
        synchronized(lock) {
            if (permit == Permit.PROBE && state == State.HALF_OPEN) probeInFlight = false
        }
    }

    private fun open(): Long {
        state = State.OPEN
        openUntilMs = clock() + openDurationMs
        consecutiveFailures = 0
        probeInFlight = false
        return openDurationMs
    }

    companion object {
        const val DEFAULT_FAILURE_THRESHOLD = 3
        const val DEFAULT_OPEN_MS = 90_000L
        const val MAX_OPEN_MS = 10L * 60L * 1_000L
    }
}
