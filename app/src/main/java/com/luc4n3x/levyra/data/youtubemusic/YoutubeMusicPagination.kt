package com.luc4n3x.levyra.data.youtubemusic

import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

internal data class YoutubeMusicPage<T>(val items: List<T>, val continuation: String)

internal suspend fun <T> collectYoutubeMusicPages(
    maxItems: Int = 10000,
    maxPages: Int = 200,
    key: (T) -> String,
    load: suspend (String) -> YoutubeMusicPage<T>
): List<T> {
    val items = LinkedHashMap<String, T>()
    val seen = hashSetOf<String>()
    var continuation = ""
    var count = 0
    do {
        coroutineContext.ensureActive()
        val page = load(continuation)
        page.items.forEach { items.putIfAbsent(key(it), it) }
        continuation = page.continuation
        count++
        if (items.size > maxItems || (continuation.isNotBlank() && count >= maxPages)) throw YoutubeMusicLibraryLimit()
        if (continuation.isNotBlank() && !seen.add(continuation)) throw YoutubeMusicProtocolException()
    } while (continuation.isNotBlank())
    return items.values.toList()
}
