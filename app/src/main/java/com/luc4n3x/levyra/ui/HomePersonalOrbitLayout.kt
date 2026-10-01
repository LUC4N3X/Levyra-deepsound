package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.LevyraPersonalOrbit
import com.luc4n3x.levyra.domain.Track
import java.util.Locale

internal const val HOME_PERSONAL_ORBIT_GRID_COLUMNS = 3
private const val HOME_PERSONAL_ORBIT_PAGE_SIZE = HOME_PERSONAL_ORBIT_GRID_COLUMNS * HOME_PERSONAL_ORBIT_GRID_COLUMNS

internal fun homePersonalOrbitPages(
    tracks: List<Track>,
    limit: Int = LevyraPersonalOrbit.DISPLAY_LIMIT
): List<List<List<Track>>> {
    val distinct = LevyraPersonalOrbit.distinctWorks(
        LevyraPersonalOrbit.distinctRecordings(tracks)
    ).take(limit.coerceAtLeast(0))
    val visibleCount = if (distinct.size > HOME_PERSONAL_ORBIT_PAGE_SIZE) {
        distinct.size / HOME_PERSONAL_ORBIT_PAGE_SIZE * HOME_PERSONAL_ORBIT_PAGE_SIZE
    } else {
        distinct.size
    }
    return distinct
        .take(visibleCount)
        .chunked(HOME_PERSONAL_ORBIT_PAGE_SIZE)
        .map { page -> page.chunked(HOME_PERSONAL_ORBIT_GRID_COLUMNS) }
}

internal fun homePersonalOrbitInitial(userName: String): String? {
    val trimmed = userName.trim()
    if (trimmed.isEmpty()) return null
    return trimmed.substring(0, trimmed.offsetByCodePoints(0, 1)).uppercase(Locale.getDefault())
}
