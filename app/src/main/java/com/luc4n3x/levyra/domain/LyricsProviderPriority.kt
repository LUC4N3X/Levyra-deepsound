package com.luc4n3x.levyra.domain

import org.json.JSONArray
import org.json.JSONObject

enum class LyricsProviderId(val storageValue: String) {
    YOUTUBE_MUSIC("youtube_music"),
    LRCLIB_EXACT("lrclib_exact"),
    LRCLIB_SEARCH("lrclib_search"),
    LYRICS_PLUS("lyrics_plus"),
    BINIMUM("binimum"),
    YOUTUBE_TRANSCRIPT("youtube_transcript"),
    LYRICS_OVH("lyrics_ovh");

    fun matches(providerString: String): Boolean {
        val clean = providerString.trim()
        return when (this) {
            YOUTUBE_MUSIC -> clean.startsWith("YouTube Music", ignoreCase = true)
            LRCLIB_EXACT -> clean.startsWith("LRCLIB Exact", ignoreCase = true)
            LRCLIB_SEARCH -> clean.startsWith("LRCLIB Search", ignoreCase = true)
            LYRICS_PLUS -> clean.startsWith("LyricsPlus", ignoreCase = true)
            BINIMUM -> clean.startsWith("Binimum", ignoreCase = true)
            YOUTUBE_TRANSCRIPT -> clean.startsWith("YouTube Transcript", ignoreCase = true)
            LYRICS_OVH -> clean.startsWith("Lyrics.ovh", ignoreCase = true)
        }
    }

    companion object {
        val DEFAULT_ORDER: List<LyricsProviderId> = listOf(
            YOUTUBE_MUSIC,
            LRCLIB_EXACT,
            LRCLIB_SEARCH,
            LYRICS_PLUS,
            BINIMUM,
            YOUTUBE_TRANSCRIPT,
            LYRICS_OVH
        )

        fun fromStorage(value: String?): LyricsProviderId? =
            entries.firstOrNull { it.storageValue == value }

        fun of(providerString: String): LyricsProviderId? =
            entries.firstOrNull { it.matches(providerString) }
    }
}

data class LyricsProviderEntry(
    val id: LyricsProviderId,
    val enabled: Boolean = true
)

data class LyricsProviderOrdering(
    val entries: List<LyricsProviderEntry> = LyricsProviderId.DEFAULT_ORDER.map { LyricsProviderEntry(it) }
) {
    val enabledIds: List<LyricsProviderId>
        get() = entries.filter { it.enabled }.map { it.id }

    val isDefault: Boolean
        get() = entries == LyricsProviderId.DEFAULT_ORDER.map { LyricsProviderEntry(it) }

    fun isEnabled(id: LyricsProviderId): Boolean =
        entries.firstOrNull { it.id == id }?.enabled != false

    fun providerScoreFor(providerString: String): Int {
        val id = LyricsProviderId.of(providerString) ?: return 0
        if (!isEnabled(id)) return DISABLED_PROVIDER_SCORE
        val rank = enabledIds.indexOfFirst { it == id }
        if (rank < 0) return 0
        return (PROVIDER_PRIORITY_BASE_SCORE - rank).coerceAtLeast(0)
    }

    fun moved(from: Int, to: Int): LyricsProviderOrdering {
        if (from !in entries.indices || to !in entries.indices || from == to) return this
        val reordered = entries.toMutableList()
        reordered.add(to, reordered.removeAt(from))
        return LyricsProviderOrdering(reordered)
    }

    fun withEnabled(id: LyricsProviderId, enabled: Boolean): LyricsProviderOrdering =
        LyricsProviderOrdering(entries.map { entry -> if (entry.id == id) entry.copy(enabled = enabled) else entry })

    fun encode(): String {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(JSONObject().put("id", entry.id.storageValue).put("enabled", entry.enabled))
        }
        return array.toString()
    }

    companion object {
        private const val PROVIDER_PRIORITY_BASE_SCORE = 8
        private const val DISABLED_PROVIDER_SCORE = -20

        fun decode(raw: String?): LyricsProviderOrdering {
            if (raw.isNullOrBlank()) return LyricsProviderOrdering()
            return runCatching {
                val array = JSONArray(raw)
                val parsed = ArrayList<LyricsProviderEntry>(array.length())
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = LyricsProviderId.fromStorage(item.optString("id")) ?: continue
                    val enabled = item.optBoolean("enabled", true)
                    parsed += LyricsProviderEntry(id, enabled)
                }
                val known = parsed.map { it.id }
                LyricsProviderId.DEFAULT_ORDER
                    .filterNot { it in known }
                    .forEach { parsed += LyricsProviderEntry(it) }
                LyricsProviderOrdering(parsed.distinctBy { it.id })
            }.getOrDefault(LyricsProviderOrdering())
        }
    }
}

fun isLastResortLyricsProvider(id: LyricsProviderId): Boolean = when (id) {
    LyricsProviderId.LYRICS_OVH,
    LyricsProviderId.YOUTUBE_TRANSCRIPT -> true
    else -> false
}

fun isOptimalLyricsResult(synced: Boolean, confidence: Int, threshold: Int = 80): Boolean =
    synced && confidence >= threshold

data class LyricsFetchPlan(
    val primary: LyricsProviderId?,
    val trusted: List<LyricsProviderId>,
    val lastResort: List<LyricsProviderId>
) {
    companion object {
        fun build(ordering: LyricsProviderOrdering): LyricsFetchPlan {
            val enabled = ordering.enabledIds
            val primary = enabled.firstOrNull()
            val remainder = enabled.drop(1)
            return LyricsFetchPlan(
                primary = primary,
                trusted = remainder.filterNot(::isLastResortLyricsProvider),
                lastResort = remainder.filter(::isLastResortLyricsProvider)
            )
        }
    }
}
