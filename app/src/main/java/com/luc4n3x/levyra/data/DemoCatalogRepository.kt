package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.CacheReport
import com.luc4n3x.levyra.domain.Track

class DemoCatalogRepository {
    fun featured(): List<Track> = emptyList()

    fun search(query: String): List<Track> = emptyList()

    fun cachedTracks(): List<Track> = emptyList()

    fun cacheReport(): CacheReport = CacheReport(
        offlineReady = 0,
        smartCached = 0,
        nextPreload = 0,
        totalTracks = 0
    )
}
