package com.luc4n3x.levyra.data

import android.content.Context
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class ExploreMoodArtwork(val url: String, val rotation: Long)

internal object ExploreMoodArtworkCache {
    private val memory = LruCache<String, ExploreMoodArtwork>(128)

    fun peek(key: String): ExploreMoodArtwork? = memory.get(key)

    suspend fun load(context: Context, key: String): ExploreMoodArtwork? = withContext(Dispatchers.IO) {
        memory.get(key)?.let { return@withContext it }
        val preferences = context.getSharedPreferences("explore_mood_artwork", Context.MODE_PRIVATE)
        val url = preferences.getString("$key.url", "").orEmpty()
        if (url.isBlank()) return@withContext null
        val entry = ExploreMoodArtwork(url, preferences.getLong("$key.rotation", Long.MIN_VALUE))
        memory.put(key, entry)
        entry
    }

    suspend fun store(context: Context, key: String, artwork: ExploreMoodArtwork) = withContext(Dispatchers.IO) {
        if (artwork.url.isBlank()) return@withContext
        memory.put(key, artwork)
        context.getSharedPreferences("explore_mood_artwork", Context.MODE_PRIVATE).edit()
            .putString("$key.url", artwork.url).putLong("$key.rotation", artwork.rotation).apply()
    }
}
