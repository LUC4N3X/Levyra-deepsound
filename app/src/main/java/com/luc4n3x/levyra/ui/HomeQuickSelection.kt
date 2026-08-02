package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.LevyraPersonalOrbit
import com.luc4n3x.levyra.domain.Track

internal fun buildHomeQuickSelectionTracks(
    personalTracks: List<Track>,
    quickPickTracks: List<Track>,
    favoriteTracks: List<Track>,
    newReleaseTracks: List<Track>,
    resonanceTracks: List<Track>,
    showPersonalOrbit: Boolean,
    showNewReleases: Boolean,
    showResonance: Boolean,
    limit: Int = 9
): List<Track> {
    if (limit <= 0) return emptyList()

    val sources = listOf(
        personalTracks.takeIf { showPersonalOrbit }.orEmpty(),
        quickPickTracks,
        favoriteTracks,
        newReleaseTracks.takeIf { showNewReleases }.orEmpty(),
        resonanceTracks.takeIf { showResonance }.orEmpty()
    ).filter { it.isNotEmpty() }

    if (sources.isEmpty()) return emptyList()

    val positions = IntArray(sources.size)
    val selected = ArrayList<Track>(limit)

    while (selected.size < limit) {
        var addedInRound = false

        sources.indices.forEach { sourceIndex ->
            if (selected.size >= limit) return@forEach
            val source = sources[sourceIndex]

            while (positions[sourceIndex] < source.size) {
                val candidate = source[positions[sourceIndex]++]
                if (selected.none { existing -> LevyraPersonalOrbit.sameRecording(existing, candidate) }) {
                    selected += candidate
                    addedInRound = true
                    break
                }
            }
        }

        if (!addedInRound) break
    }

    return selected
}
