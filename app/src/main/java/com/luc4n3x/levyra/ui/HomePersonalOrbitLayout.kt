package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.LevyraPersonalOrbit
import com.luc4n3x.levyra.domain.Track

private const val HOME_PERSONAL_ORBIT_ROWS = 4

internal fun homePersonalOrbitColumns(
    tracks: List<Track>,
    limit: Int = LevyraPersonalOrbit.DISPLAY_LIMIT
): List<List<Track>> {
    val cappedLimit = limit.coerceAtLeast(0)
    return LevyraPersonalOrbit.distinctWorks(
        LevyraPersonalOrbit.distinctRecordings(tracks)
    )
        .take(cappedLimit)
        .chunked(HOME_PERSONAL_ORBIT_ROWS)
}
