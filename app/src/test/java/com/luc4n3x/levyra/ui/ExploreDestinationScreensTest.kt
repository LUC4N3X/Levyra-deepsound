package com.luc4n3x.levyra.ui

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
    }

    @Test
    fun topLevelExploreDestinationsRemainDistinct() {
        assertNotEquals(ExploreNewReleasesDestination, ExploreMoodsDestination)
        assertNotEquals(ExploreNewReleasesDestination, exploreMoodDestination("nuove-uscite"))
    }
}
