package com.luc4n3x.levyra.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal class SharedLoads<K : Any, V> {
    private val lock = Any()
    private val pending = HashMap<K, CompletableDeferred<V>>()

    val pendingCount: Int
        get() = synchronized(lock) { pending.size }

    suspend fun load(key: K, produce: suspend () -> V): V {
        while (true) {
            var owner = false
            val deferred = synchronized(lock) {
                pending.getOrPut(key) {
                    owner = true
                    CompletableDeferred()
                }
            }
            if (!owner) {
                try {
                    return deferred.await()
                } catch (cancelled: CancellationException) {
                    currentCoroutineContext().ensureActive()
                    continue
                }
            }
            try {
                return produce().also { deferred.complete(it) }
            } catch (error: Throwable) {
                deferred.cancel()
                throw error
            } finally {
                synchronized(lock) {
                    if (pending[key] === deferred) pending.remove(key)
                }
            }
        }
    }
}
