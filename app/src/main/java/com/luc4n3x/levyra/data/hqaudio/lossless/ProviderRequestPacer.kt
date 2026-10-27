package com.luc4n3x.levyra.data.hqaudio.lossless

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

internal class ProviderRequestPacer(
    private val spacingMs: Long,
    maxConcurrent: Int,
    private val clock: () -> Long
) {
    private val gate = Semaphore(maxConcurrent.coerceAtLeast(1))
    private val slots = Mutex()
    private var nextSlotAtMs = 0L

    suspend fun <T> paced(block: suspend () -> T): T = gate.withPermit {
        slots.withLock {
            val waitMs = nextSlotAtMs - clock()
            if (waitMs > 0L) delay(waitMs)
            nextSlotAtMs = clock() + spacingMs
        }
        block()
    }
}
