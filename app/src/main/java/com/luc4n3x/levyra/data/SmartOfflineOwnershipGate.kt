package com.luc4n3x.levyra.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object SmartOfflineOwnershipGate {
    private val mutex = Mutex()

    suspend fun <T> withLock(block: suspend () -> T): T = mutex.withLock { block() }
}
