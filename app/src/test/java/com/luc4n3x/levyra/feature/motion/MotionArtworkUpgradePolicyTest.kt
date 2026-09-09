package com.luc4n3x.levyra.feature.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionArtworkUpgradePolicyTest {
    @Test
    fun firstAcceptedCandidateAlwaysPublishes() {
        assertTrue(
            shouldPublishMotionUpgrade(
                publishedProviderRank = null,
                candidateProviderRank = 2,
                forcedSource = false,
                upgradesUsed = 0
            )
        )
        assertTrue(
            shouldPublishMotionUpgrade(
                publishedProviderRank = null,
                candidateProviderRank = 0,
                forcedSource = true,
                upgradesUsed = 0
            )
        )
    }

    @Test
    fun strictlyBetterProviderRankUpgradesOnce() {
        assertTrue(
            shouldPublishMotionUpgrade(
                publishedProviderRank = 1,
                candidateProviderRank = 0,
                forcedSource = false,
                upgradesUsed = 0
            )
        )
    }

    @Test
    fun equalOrWorseProviderRankNeverDowngrades() {
        assertFalse(
            shouldPublishMotionUpgrade(
                publishedProviderRank = 1,
                candidateProviderRank = 1,
                forcedSource = false,
                upgradesUsed = 0
            )
        )
        assertFalse(
            shouldPublishMotionUpgrade(
                publishedProviderRank = 0,
                candidateProviderRank = 2,
                forcedSource = false,
                upgradesUsed = 0
            )
        )
    }

    @Test
    fun secondUpgradeIsRejected() {
        assertFalse(
            shouldPublishMotionUpgrade(
                publishedProviderRank = 1,
                candidateProviderRank = 0,
                forcedSource = false,
                upgradesUsed = 1
            )
        )
    }

    @Test
    fun forcedSourceNeverUpgradesAcrossProviders() {
        assertFalse(
            shouldPublishMotionUpgrade(
                publishedProviderRank = 2,
                candidateProviderRank = 0,
                forcedSource = true,
                upgradesUsed = 0
            )
        )
    }

    @Test
    fun tidalThenAppleThenCommunityArrivalKeepsAtMostOneUpgrade() {
        val published = simulatePublications(listOf(2, 1, 0), forcedSource = false)

        assertEquals(listOf(2, 1), published)
    }

    @Test
    fun communityFirstArrivalPublishesOnceAndStaysStable() {
        val published = simulatePublications(listOf(0, 1, 2), forcedSource = false)

        assertEquals(listOf(0), published)
    }

    @Test
    fun appleFirstArrivalUpgradesToCommunityAndIgnoresTidal() {
        val published = simulatePublications(listOf(1, 2, 0), forcedSource = false)

        assertEquals(listOf(1, 0), published)
    }

    @Test
    fun forcedSourceArrivalPublishesExactlyOnce() {
        val published = simulatePublications(listOf(1, 0), forcedSource = true)

        assertEquals(listOf(1), published)
    }

    private fun simulatePublications(arrivalRanks: List<Int>, forcedSource: Boolean): List<Int> {
        val published = mutableListOf<Int>()
        var publishedRank: Int? = null
        var upgradesUsed = 0
        arrivalRanks.forEach { rank ->
            val accepted = shouldPublishMotionUpgrade(
                publishedProviderRank = publishedRank,
                candidateProviderRank = rank,
                forcedSource = forcedSource,
                upgradesUsed = upgradesUsed
            )
            if (accepted) {
                if (publishedRank != null) upgradesUsed++
                publishedRank = rank
                published += rank
            }
        }
        assertTrue(published.size <= 1 + MAX_MOTION_ARTWORK_UPGRADES)
        assertEquals(published.sortedDescending(), published)
        return published
    }
}
