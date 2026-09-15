package com.luc4n3x.levyra.feature.search

internal class SearchMemoCache<V : Any>(
    private val maxEntries: Int,
    private val ttlMs: Long,
    private val clock: () -> Long = { System.nanoTime() / 1_000_000L }
) {
    private class Entry<V>(val value: V, val storedAtMs: Long)

    private val lock = Any()
    private val entries = object : LinkedHashMap<String, Entry<V>>(maxEntries.coerceAtLeast(1) + 1, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry<V>>?): Boolean =
            size > maxEntries.coerceAtLeast(1)
    }

    val size: Int
        get() = synchronized(lock) { entries.size }

    fun get(key: String): V? {
        synchronized(lock) {
            val entry = entries[key] ?: return null
            if (isExpired(entry)) {
                entries.remove(key)
                return null
            }
            return entry.value
        }
    }

    fun put(key: String, value: V) {
        if (key.isEmpty()) return
        synchronized(lock) { entries[key] = Entry(value, clock()) }
    }

    fun longestPrefix(key: String): V? = synchronized(lock) {
        var best: Entry<V>? = null
        var bestLength = -1
        for ((candidateKey, entry) in entries) {
            if (candidateKey.length >= key.length || candidateKey.length <= bestLength) continue
            if (!key.startsWith(candidateKey) || isExpired(entry)) continue
            best = entry
            bestLength = candidateKey.length
        }
        best?.value
    }

    private fun isExpired(entry: Entry<V>): Boolean = clock() - entry.storedAtMs >= ttlMs
}
