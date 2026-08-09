package com.luc4n3x.levyra.data

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal suspend inline fun <T> runCatchingPreservingCancellation(
    crossinline block: suspend () -> T
): Result<T> {
    return try {
        val value = block()
        currentCoroutineContext().ensureActive()
        Result.success(value)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        currentCoroutineContext().ensureActive()
        Result.failure(error)
    }
}

internal class PlaybackSingleFlight<K : Any, V>(
    private val scope: CoroutineScope
) {
    private val requests = ConcurrentHashMap<K, Deferred<V>>()

    suspend fun run(key: K, block: suspend () -> V): V {
        val candidate = scope.async(start = CoroutineStart.LAZY) { block() }
        val existing = requests.putIfAbsent(key, candidate)
        if (existing != null) {
            candidate.cancel()
            return existing.await()
        }

        candidate.invokeOnCompletion {
            requests.remove(key, candidate)
        }
        candidate.start()
        return candidate.await()
    }

    internal fun activeCount(): Int = requests.size
}
