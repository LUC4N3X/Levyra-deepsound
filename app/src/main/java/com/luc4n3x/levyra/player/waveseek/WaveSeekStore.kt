package com.luc4n3x.levyra.player.waveseek

import android.content.Context
import java.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class WaveSeekIndexUpdate(
    val kept: List<String>,
    val evicted: List<String>
)

internal fun waveSeekStorageKey(mediaId: String, durationMs: Long): String? {
    val cleanId = mediaId.trim()
    if (cleanId.isBlank() || durationMs <= 0L) return null
    val durationSeconds = durationMs / 1_000L
    if (durationSeconds <= 0L) return null
    val encodedId = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(cleanId.toByteArray(Charsets.UTF_8))
    return "wave-v1:$durationSeconds:$encodedId"
}

internal fun waveSeekUpdatedIndex(
    existing: List<String>,
    touched: String,
    limit: Int
): WaveSeekIndexUpdate {
    val maxEntries = limit.coerceAtLeast(1)
    val ordered = existing
        .filter { it.isNotBlank() && it != touched }
        .distinct()
        .toMutableList()
        .apply { add(touched) }
    val dropCount = (ordered.size - maxEntries).coerceAtLeast(0)
    return WaveSeekIndexUpdate(
        kept = ordered.drop(dropCount),
        evicted = ordered.take(dropCount)
    )
}

internal object WaveSeekStore {
    private const val PREFS_NAME = "levyra.waveseek"
    private const val INDEX_KEY = "recent"
    private const val MAX_ENTRIES = 64

    private val _revision = MutableStateFlow(0L)
    val revision: StateFlow<Long> = _revision.asStateFlow()

    fun load(context: Context, mediaId: String, durationMs: Long): WaveSeekEnvelope? {
        val key = waveSeekStorageKey(mediaId, durationMs) ?: return null
        val encoded = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(key, null)
            ?: return null
        return WaveSeekEnvelope.decodeFromString(encoded)
    }

    fun save(context: Context, mediaId: String, durationMs: Long, envelope: WaveSeekEnvelope): Boolean {
        val key = waveSeekStorageKey(mediaId, durationMs) ?: return false
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(INDEX_KEY, null)
            .orEmpty()
            .lineSequence()
            .filter(String::isNotBlank)
            .toList()
        val update = waveSeekUpdatedIndex(existing, key, MAX_ENTRIES)
        val editor = prefs.edit()
            .putString(key, envelope.encodeToString())
            .putString(INDEX_KEY, update.kept.joinToString("\n"))
        update.evicted.forEach(editor::remove)
        editor.apply()
        _revision.value = _revision.value + 1L
        return true
    }
}
