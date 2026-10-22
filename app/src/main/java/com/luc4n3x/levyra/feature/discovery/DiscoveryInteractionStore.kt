package com.luc4n3x.levyra.feature.discovery

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal class DiscoveryInteractionStore(private val preferences: SharedPreferences) {
    constructor(context: Context) : this(context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE))

    private val mutex = Mutex()

    suspend fun load(): DiscoveryInteractions = withContext(Dispatchers.IO) {
        DiscoveryInteractionCodec.decode(preferences.getString(KEY_INTERACTIONS, null))
    }

    suspend fun record(card: DiscoveryCard, decision: DiscoveryDecision, nowMs: Long = System.currentTimeMillis()): DiscoveryInteractions =
        update { it.recorded(card, decision, nowMs) }

    suspend fun revert(step: DiscoveryStep): DiscoveryInteractions = update { it.reverted(step) }

    private suspend fun update(transform: (DiscoveryInteractions) -> DiscoveryInteractions): DiscoveryInteractions = withContext(Dispatchers.IO) {
        mutex.withLock {
            val next = transform(DiscoveryInteractionCodec.decode(preferences.getString(KEY_INTERACTIONS, null)))
            preferences.edit().putString(KEY_INTERACTIONS, DiscoveryInteractionCodec.encode(next)).apply()
            next
        }
    }

    private companion object {
        const val PREFERENCES = "levyra_discovery"
        const val KEY_INTERACTIONS = "interactions"
    }
}

internal fun DiscoveryInteractions.recorded(card: DiscoveryCard, decision: DiscoveryDecision, nowMs: Long): DiscoveryInteractions {
    val artist = discoveryPrimaryArtistKey(card.track.artist)
    return when (decision) {
        DiscoveryDecision.Saved -> copy(saved = saved + (card.key to nowMs), dismissed = dismissed - card.key)
        DiscoveryDecision.Dismissed -> copy(
            dismissed = dismissed + (card.key to nowMs),
            artistDismissals = if (artist.isBlank()) artistDismissals else artistDismissals + (artist to (artistDismissals[artist] ?: 0) + 1)
        )
    }.bounded()
}

internal fun DiscoveryInteractions.reverted(step: DiscoveryStep): DiscoveryInteractions {
    val artist = discoveryPrimaryArtistKey(step.card.track.artist)
    return when (step.decision) {
        DiscoveryDecision.Saved -> copy(saved = saved - step.card.key)
        DiscoveryDecision.Dismissed -> {
            val count = (artistDismissals[artist] ?: 0) - 1
            copy(
                dismissed = dismissed - step.card.key,
                artistDismissals = if (count > 0) artistDismissals + (artist to count) else artistDismissals - artist
            )
        }
    }
}

internal fun DiscoveryInteractions.bounded(): DiscoveryInteractions = copy(
    dismissed = dismissed.newest(DiscoveryInteractionCodec.MAX_DISMISSED),
    saved = saved.newest(DiscoveryInteractionCodec.MAX_SAVED),
    artistDismissals = if (artistDismissals.size <= DiscoveryInteractionCodec.MAX_ARTISTS) artistDismissals
    else artistDismissals.entries.sortedByDescending { it.value }.take(DiscoveryInteractionCodec.MAX_ARTISTS).associate { it.key to it.value }
)

private fun Map<String, Long>.newest(limit: Int): Map<String, Long> =
    if (size <= limit) this else entries.sortedByDescending { it.value }.take(limit).associate { it.key to it.value }

internal object DiscoveryInteractionCodec {
    const val MAX_DISMISSED = 400
    const val MAX_SAVED = 400
    const val MAX_ARTISTS = 200

    fun encode(value: DiscoveryInteractions): String = JSONObject()
        .put("dismissed", JSONObject().apply { value.dismissed.forEach { (key, time) -> put(key, time) } })
        .put("saved", JSONObject().apply { value.saved.forEach { (key, time) -> put(key, time) } })
        .put("artists", JSONObject().apply { value.artistDismissals.forEach { (key, count) -> put(key, count) } })
        .toString()

    fun decode(raw: String?): DiscoveryInteractions {
        if (raw.isNullOrBlank()) return DiscoveryInteractions()
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return DiscoveryInteractions()
        return DiscoveryInteractions(
            dismissed = root.optJSONObject("dismissed").longs(),
            saved = root.optJSONObject("saved").longs(),
            artistDismissals = root.optJSONObject("artists").longs().mapValues { it.value.toInt() }.filterValues { it > 0 }
        ).bounded()
    }

    private fun JSONObject?.longs(): Map<String, Long> {
        if (this == null) return emptyMap()
        val result = HashMap<String, Long>()
        keys().forEach { key -> optLong(key, -1L).takeIf { it >= 0L && key.isNotBlank() }?.let { result[key] = it } }
        return result
    }
}
