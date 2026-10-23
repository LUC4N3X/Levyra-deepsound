package com.luc4n3x.levyra.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeout

internal class AndroidAutoSearchRequests<Owner, Item>(
    private val scope: CoroutineScope,
    private val timeoutMs: Long = 12_000L,
    private val search: suspend (String) -> List<Item>
) {
    private data class Request<Item>(val query: String, val result: Deferred<List<Item>>)

    private val requests = LinkedHashMap<Owner, Request<Item>>()

    fun start(owner: Owner, query: String): Deferred<List<Item>> {
        requests[owner]?.let {
            if (it.query == query && it.result.isActive) return it.result
            it.result.cancel()
        }
        if (owner !in requests && requests.size >= 32) {
            val oldest = requests.entries.first()
            oldest.value.result.cancel()
            requests.remove(oldest.key)
        }
        val result = scope.async(start = CoroutineStart.LAZY) {
            withTimeout(timeoutMs) { search(query) }
        }
        requests[owner] = Request(query, result)
        result.start()
        return result
    }

    fun isCurrent(owner: Owner, query: String, result: Deferred<List<Item>>): Boolean =
        requests[owner]?.let { it.query == query && it.result === result } == true

    suspend fun result(owner: Owner, query: String): List<Item> {
        val request = requests[owner]?.takeIf { it.query == query }
            ?: throw IllegalArgumentException("Search is no longer active")
        return request.result.await()
    }

    fun disconnect(owner: Owner) {
        requests.remove(owner)?.result?.cancel()
    }
}
