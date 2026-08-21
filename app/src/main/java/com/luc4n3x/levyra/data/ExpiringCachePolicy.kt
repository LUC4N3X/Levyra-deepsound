package com.luc4n3x.levyra.data

import org.json.JSONObject

internal fun parsePersistedCacheEntry(value: Any?): JSONObject? {
    val raw = value as? String ?: return null
    return runCatching { JSONObject(raw) }.getOrNull()
}

internal fun expiringCacheKeysToRemove(
    entries: Map<String, Long>,
    nowMs: Long,
    maxEntries: Int,
    incomingKey: String? = null
): Set<String> {
    require(maxEntries > 0)
    val removals = entries
        .filterValues { it <= nowMs }
        .keys
        .toMutableSet()
    val retained = entries.filterKeys { it !in removals }
    var projectedSize = retained.size + if (incomingKey != null && incomingKey !in retained) 1 else 0
    if (projectedSize <= maxEntries) return removals

    retained.entries
        .asSequence()
        .filter { it.key != incomingKey }
        .sortedWith(compareBy<Map.Entry<String, Long>> { it.value }.thenBy { it.key })
        .forEach { entry ->
            if (projectedSize <= maxEntries) return@forEach
            removals += entry.key
            projectedSize -= 1
        }
    return removals
}
