package com.luc4n3x.levyra.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyChartBootstrapProviderTest {
    @Test
    fun installsBundledCatalogWhenNoCacheExists() {
        assertTrue(
            shouldInstallBundledCatalog(
                cachePresent = false,
                cachedGeneratedAtMs = null,
                bundledGeneratedAtMs = null
            )
        )
    }

    @Test
    fun replacesOlderCachedCatalogWithNewerBundledCatalog() {
        assertTrue(
            shouldInstallBundledCatalog(
                cachePresent = true,
                cachedGeneratedAtMs = 100L,
                bundledGeneratedAtMs = 200L
            )
        )
    }

    @Test
    fun installsExploreCapableBundleOverNewerLegacyCache() {
        assertTrue(
            shouldInstallBundledCatalog(
                cachePresent = true,
                cachedGeneratedAtMs = 300L,
                bundledGeneratedAtMs = 200L,
                cachedHasExplore = false,
                bundledHasExplore = true
            )
        )
    }

    @Test
    fun keepsNewerCachedCatalogInsteadOfDowngradingToBundledData() {
        assertFalse(
            shouldInstallBundledCatalog(
                cachePresent = true,
                cachedGeneratedAtMs = 300L,
                bundledGeneratedAtMs = 200L
            )
        )
    }

    @Test
    fun doesNotReplaceCacheWhenBundledFreshnessIsUnknown() {
        assertFalse(
            shouldInstallBundledCatalog(
                cachePresent = true,
                cachedGeneratedAtMs = 300L,
                bundledGeneratedAtMs = null
            )
        )
    }
}
