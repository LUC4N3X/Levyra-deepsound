package com.luc4n3x.levyra.domain

import java.util.concurrent.TimeUnit

enum class FreshDiscoveryWindow(val id: String, val days: Int?) {
    DAYS_7("7d", 7),
    DAYS_30("30d", 30),
    DAYS_90("90d", 90),
    ALL_TIME("all", null);

    fun cutoffMs(nowMs: Long): Long = days?.let { nowMs - TimeUnit.DAYS.toMillis(it.toLong()) } ?: 1L

    companion object {
        val DEFAULT = DAYS_30

        fun from(id: String?): FreshDiscoveryWindow = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

data class FreshDiscoverySettings(
    val enabled: Boolean = false,
    val window: FreshDiscoveryWindow = FreshDiscoveryWindow.DEFAULT,
    val allowFavorites: Boolean = true
)

object FreshDiscovery {

    fun lookupKeys(candidates: List<Track>): List<String> {
        val keys = LinkedHashSet<String>(candidates.size * 2)
        candidates.forEach { track ->
            ListenIdentity.trackKey(track.id, track.title, track.artist).takeIf(::isUsableKey)?.let(keys::add)
            ListenIdentity.trackKey("", track.title, track.artist).takeIf(::isUsableKey)?.let(keys::add)
        }
        return keys.toList()
    }

    fun select(
        candidates: List<Track>,
        lastPlayedByKey: Map<String, Long>,
        settings: FreshDiscoverySettings,
        isFavorite: (Track) -> Boolean,
        minimumFresh: Int,
        nowMs: Long = System.currentTimeMillis()
    ): List<Track> {
        if (!settings.enabled || candidates.isEmpty() || lastPlayedByKey.isEmpty()) return candidates
        val cutoff = settings.window.cutoffMs(nowMs)
        val fresh = ArrayList<Track>(candidates.size)
        val heard = ArrayList<HeardCandidate>()
        val protectedFavorites = HashSet<Track>()
        candidates.forEach { track ->
            val lastPlayedAt = lastPlayedAt(track, lastPlayedByKey)
            val protectedFavorite = settings.allowFavorites && isFavorite(track)
            if (protectedFavorite) protectedFavorites += track
            if (lastPlayedAt < cutoff || protectedFavorite) fresh += track else heard += HeardCandidate(track, lastPlayedAt)
        }
        if (heard.isEmpty()) return candidates
        val distinctFresh = withoutHeardRecordings(fresh, heard, protectedFavorites)
        if (distinctFresh.size >= minimumFresh) return distinctFresh
        val fallback = heard.sortedBy(HeardCandidate::lastPlayedAt)
            .take(minimumFresh - distinctFresh.size)
            .map(HeardCandidate::track)
        return distinctFresh + fallback
    }

    private fun lastPlayedAt(track: Track, lastPlayedByKey: Map<String, Long>): Long {
        val byId = lastPlayedByKey[ListenIdentity.trackKey(track.id, track.title, track.artist)] ?: 0L
        val byText = lastPlayedByKey[ListenIdentity.trackKey("", track.title, track.artist)] ?: 0L
        return maxOf(byId, byText)
    }

    private fun withoutHeardRecordings(
        fresh: List<Track>,
        heard: List<HeardCandidate>,
        protectedFavorites: Set<Track>
    ): List<Track> {
        val heardArtistsByTitle = HashMap<String, MutableSet<String>>(heard.size)
        heard.forEach { candidate ->
            val title = LevyraPersonalOrbit.musicTitleKey(candidate.track)
            val artists = LevyraPersonalOrbit.artistKeys(candidate.track)
            if (title.isNotBlank() && artists.isNotEmpty()) {
                heardArtistsByTitle.getOrPut(title) { HashSet() } += artists
            }
        }
        if (heardArtistsByTitle.isEmpty()) return fresh
        return fresh.filterNot { track ->
            if (track in protectedFavorites) return@filterNot false
            val artists = heardArtistsByTitle[LevyraPersonalOrbit.musicTitleKey(track)] ?: return@filterNot false
            LevyraPersonalOrbit.artistKeys(track).any(artists::contains)
        }
    }

    private fun isUsableKey(key: String): Boolean = key.isNotEmpty() && key != "|"

    private data class HeardCandidate(val track: Track, val lastPlayedAt: Long)
}
