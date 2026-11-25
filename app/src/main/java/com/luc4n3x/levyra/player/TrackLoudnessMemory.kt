package com.luc4n3x.levyra.player

import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.ConcurrentHashMap

internal class TrackLoudnessMemory(private val preferences: SharedPreferences) {
    private val entries = ConcurrentHashMap<String, Entry>()

    @Volatile
    private var loaded = false

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    )

    fun load() {
        if (loaded) return
        preferences.all.forEach { (key, value) ->
            val entry = (value as? String)?.let(::decode)
            if (entry != null) entries.putIfAbsent(key, entry)
        }
        loaded = true
    }

    fun loudnessLufs(key: String): Float? = entries[key]?.lufs

    @Synchronized
    fun remember(
        key: String,
        integratedLufs: Float,
        measuredBlocks: Int,
        nowMs: Long = System.currentTimeMillis()
    ) {
        if (!integratedLufs.isFinite() || integratedLufs !in MIN_LUFS..MAX_LUFS || measuredBlocks <= 0) return
        val existing = entries[key]
        if (existing != null && existing.measuredBlocks > measuredBlocks) return
        val entry = Entry(integratedLufs, measuredBlocks, nowMs)
        entries[key] = entry
        val editor = preferences.edit().putString(key, encode(entry))
        if (entries.size > CAPACITY) {
            entries.entries
                .sortedBy { it.value.updatedAtMs }
                .take(entries.size - TRIMMED_SIZE)
                .forEach { (staleKey, _) ->
                    entries.remove(staleKey)
                    editor.remove(staleKey)
                }
        }
        editor.apply()
    }

    private data class Entry(val lufs: Float, val measuredBlocks: Int, val updatedAtMs: Long)

    companion object {
        private const val PREFERENCES_NAME = "levyra_track_loudness_v1"
        private const val SEPARATOR = '|'
        internal const val CAPACITY = 2_000
        internal const val TRIMMED_SIZE = 1_800
        private const val MIN_LUFS = -70f
        private const val MAX_LUFS = 5f

        fun key(mediaId: String, source: String?): String? =
            mediaId.takeIf { it.isNotBlank() }?.let { "$it$SEPARATOR${source.orEmpty()}" }

        private fun encode(entry: Entry): String =
            listOf(entry.lufs, entry.measuredBlocks, entry.updatedAtMs).joinToString(SEPARATOR.toString())

        private fun decode(value: String): Entry? {
            val parts = value.split(SEPARATOR)
            val lufs = parts.getOrNull(0)?.toFloatOrNull()?.takeIf { it.isFinite() } ?: return null
            val measuredBlocks = parts.getOrNull(1)?.toIntOrNull() ?: return null
            val updatedAtMs = parts.getOrNull(2)?.toLongOrNull() ?: 0L
            return Entry(lufs, measuredBlocks, updatedAtMs)
        }
    }
}
