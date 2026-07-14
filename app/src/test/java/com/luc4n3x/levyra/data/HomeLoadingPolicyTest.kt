package com.luc4n3x.levyra.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLoadingPolicyTest {
    @Test
    fun albumShimmerAppearsOnlyOnEmptyColdStart() {
        val empty = HomeContentAvailability()

        assertTrue(HomeLoadingPolicy.showAlbumShimmer(empty, loading = true))
        assertFalse(HomeLoadingPolicy.showAlbumShimmer(empty, loading = false))
    }

    @Test
    fun albumShimmerStaysHiddenWhenAnyRealHomeContentExists() {
        val cachedHome = HomeContentAvailability(trackCount = 12, homeSectionCount = 2, homeSectionTrackCount = 12)

        assertFalse(HomeLoadingPolicy.showAlbumShimmer(cachedHome, loading = true))
    }

    @Test
    fun chartShimmerStaysHiddenWhenCachedHomeContentExists() {
        val cachedHome = HomeContentAvailability(personalOrbitCount = 12)

        assertFalse(HomeLoadingPolicy.showChartShimmer(cachedHome, loading = true))
    }

    @Test
    fun chartShimmerAppearsOnEmptyColdStart() {
        assertTrue(HomeLoadingPolicy.showChartShimmer(HomeContentAvailability(), loading = true))
    }
}
