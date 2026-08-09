package com.luc4n3x.levyra.data

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private val playbackCancellationJob = ThreadLocal<Job?>()

internal fun currentPlaybackCancellationJob(): Job? = playbackCancellationJob.get()

internal suspend inline fun <T> runCatchingPreservingCancellation(
    crossinline block: suspend () -> T
): Result<T> {
    val context = currentCoroutineContext()
    return try {
        val value = withContext(playbackCancellationJob.asContextElement(context[Job])) {
            block()
        }
        context.ensureActive()
        Result.success(value)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        context.ensureActive()
        Result.failure(error)
    }
}

internal class PlaybackSingleFlight<K : Any, V>(
    private val scope: CoroutineScope
) {
    private data class Entry<V>(
        val deferred: Deferred<V>,
        var waiters: Int
    )

    private val requests = ConcurrentHashMap<K, Entry<V>>()
    private val mutex = Mutex()

    suspend fun run(key: K, block: suspend () -> V): V {
        currentCoroutineContext().ensureActive()

        val entry = mutex.withLock {
            requests[key]?.also { existing ->
                existing.waiters += 1
            } ?: Entry(
                deferred = scope.async(start = CoroutineStart.LAZY) { block() },
                waiters = 1
            ).also { created ->
                requests[key] = created
                created.deferred.start()
            }
        }

        try {
            return entry.deferred.await()
        } finally {
            val cancelShared = withContext(NonCancellable) {
                mutex.withLock {
                    entry.waiters -= 1
                    if (entry.waiters == 0 && requests[key] === entry) {
                        requests.remove(key, entry)
                        !entry.deferred.isCompleted
                    } else {
                        false
                    }
                }
            }
            if (cancelShared) entry.deferred.cancel()
        }
    }

    internal fun activeCount(): Int = requests.size
}
