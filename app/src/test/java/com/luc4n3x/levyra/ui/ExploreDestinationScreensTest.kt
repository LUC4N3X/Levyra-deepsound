package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.ExploreCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExploreDestinationScreensTest {
    @Test
    fun moodDestinationRoundTripsItsZoneId() {
        val zoneId = "rap-drill"

        assertEquals(zoneId, exploreMoodDestinationId(exploreMoodDestination(zoneId)))
    }

    @Test
    fun unrelatedDestinationsDoNotResolveAsMoodRoutes() {
        assertNull(exploreMoodDestinationId(null))
        assertNull(exploreMoodDestinationId(ExploreNewReleasesDestination))
        assertNull(exploreMoodDestinationId(ExploreMoodsDestination))
        assertNull(exploreMoodDestinationId(ExploreGenresDestination))
    }

    @Test
    fun topLevelExploreDestinationsRemainDistinct() {
        assertNotEquals(ExploreNewReleasesDestination, ExploreMoodsDestination)
        assertNotEquals(ExploreNewReleasesDestination, ExploreGenresDestination)
        assertNotEquals(ExploreMoodsDestination, ExploreGenresDestination)
        assertNotEquals(ExploreNewReleasesDestination, exploreMoodDestination("nuove-uscite"))
    }

    @Test
    fun providerCategoryRoutePreservesOpaqueIdentityAndLocalizedMetadata() {
        val category = ExploreCategory(
            title = "集中・リラックス",
            params = "ggMPOg1uX2ozUHlwbWM3ajNq%3D%3D",
            section = "ムードとシーン",
            sectionIndex = 0
        )

        assertEquals(category, exploreCategoryDestinationValue(exploreCategoryDestination(category)))
    }

    @Test
    fun malformedProviderCategoryRoutesAreRejected() {
        assertNull(exploreCategoryDestinationValue("explore-destination-provider-category:not-valid"))
        assertNull(exploreCategoryDestinationValue(ExploreMoodsDestination))
    }
}
