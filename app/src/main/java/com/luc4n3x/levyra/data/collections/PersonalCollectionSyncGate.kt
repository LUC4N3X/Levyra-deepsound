package com.luc4n3x.levyra.data.collections

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class PersonalCollectionSyncGate {
    private val locks = ConcurrentHashMap<String, Mutex>()

    suspend fun <T> tryRun(key: String, block: suspend () -> T): T? {
        val lock = locks.computeIfAbsent(key) { Mutex() }
        if (!lock.tryLock()) return null
        return try {
            block()
        } finally {
            lock.unlock()
        }
    }

    suspend fun <T> run(key: String, block: suspend () -> T): T =
        locks.computeIfAbsent(key) { Mutex() }.withLock { block() }
}
