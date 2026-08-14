package com.luc4n3x.levyra.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeContentAwareClientSelectorTest {
    private val profiles = YoutubeClientRegistry.playbackProfiles

    @Test
    fun liveContentExcludesClientsThatCannotServeIt() {
        val ordered = order(YoutubeContentHints(isLive = true))

        assertFalse(ordered.any { it.clientName == "ANDROID_VR" })
        assertTrue(ordered.any { it.clientName == "WEB_REMIX" })
    }

    @Test
    fun userUploadExcludesTheMusicOnlyClients() {
        val ordered = order(YoutubeContentHints(isUserUpload = true))

        assertEquals(
            listOf("ANDROID", "IOS", "WEB_REMIX", "WEB", "WEB_EMBEDDED_PLAYER"),
            ordered.map { it.clientName }
        )
    }

    @Test
    fun ageRestrictionPromotesTheEmbeddedSurfaceWithoutDroppingCandidates() {
        val ordered = order(YoutubeContentHints(isAgeRestricted = true))

        assertEquals("WEB_EMBEDDED_PLAYER", ordered.first().clientName)
        assertEquals(profiles.size, ordered.size)
    }

    @Test
    fun parentalAdvisoryAloneNeverChangesTheClientLadder() {
        val ordered = order(YoutubeContentHints.fromMetadata("MUSIC_VIDEO_TYPE_ATV"))

        assertEquals(order(YoutubeContentHints.NONE).map { it.clientName }, ordered.map { it.clientName })
        assertEquals("ANDROID_VR", ordered.first().clientName)
    }

    @Test
    fun emptyHintsKeepEveryClientAndPromoteAndroidVr() {
        val ordered = order(YoutubeContentHints.NONE)

        assertEquals(profiles.size, ordered.size)
        assertEquals("ANDROID_VR", ordered.first().clientName)
    }

    @Test
    fun healthyClientIsPreferredOverPenalizedOne() {
        val ordered = order(
            hints = YoutubeContentHints(isLive = true),
            ranking = mapOf(
                "WEB_REMIX" to ranking(score = 10.0, consecutiveFailures = 3),
                "IOS" to ranking(score = 95.0)
            )
        )

        assertEquals("IOS", ordered.first().clientName)
    }

    @Test
    fun temporarilyBlockedClientIsSkipped() {
        val ordered = order(
            hints = YoutubeContentHints.NONE,
            nowMs = 1_000L,
            ranking = mapOf("ANDROID_VR" to ranking(score = 99.0, blockedUntilMs = 5_000L))
        )

        assertFalse(ordered.any { it.clientName == "ANDROID_VR" })
        assertEquals(profiles.size - 1, ordered.size)
    }

    @Test
    fun everyBlockedClientStillProducesCandidates() {
        val blocked = profiles.associate { it.clientName to ranking(blockedUntilMs = 5_000L) }

        val ordered = order(YoutubeContentHints.NONE, nowMs = 1_000L, ranking = blocked)

        assertEquals(profiles.size, ordered.size)
    }

    @Test
    fun incompatibleHintsNeverStrandPlayback() {
        val ordered = YoutubeContentAwareClientSelector.order(
            profiles = listOf(YoutubeClientRegistry.ANDROID_VR),
            hints = YoutubeContentHints(isLive = true),
            nowMs = 0L
        ) { null }

        assertEquals(listOf("ANDROID_VR"), ordered.map { it.clientName })
    }

    @Test
    fun androidVrLosesPrimarySlotAfterConsecutiveFailures() {
        val ordered = order(
            hints = YoutubeContentHints.NONE,
            ranking = mapOf(
                "ANDROID_VR" to ranking(score = 60.0, consecutiveFailures = 1),
                "ANDROID_MUSIC" to ranking(score = 90.0)
            )
        )

        assertEquals("ANDROID_MUSIC", ordered.first().clientName)
    }

    @Test
    fun metadataHintsDetectUserUploadsOnly() {
        val upload = YoutubeContentHints.fromMetadata("MUSIC_VIDEO_TYPE_UGC")
        assertEquals(true, upload.isUserUpload)
        assertNull(upload.isAgeRestricted)

        val artTrack = YoutubeContentHints.fromMetadata("MUSIC_VIDEO_TYPE_ATV")
        assertNull(artTrack.isUserUpload)
    }

    @Test
    fun playabilityReasonsClassifyAgeRestriction() {
        assertEquals(
            true,
            YoutubeContentHints.fromPlayabilityReason("Sign in to confirm your age").isAgeRestricted
        )
        assertEquals(
            true,
            YoutubeContentHints.fromPlayabilityReason(
                "This video may be inappropriate for some users."
            ).isAgeRestricted
        )
        assertNull(
            YoutubeContentHints.fromPlayabilityReason("Video unavailable").isAgeRestricted
        )
    }

    @Test
    fun observedHintsDoNotClearKnownMetadata() {
        val merged = YoutubeContentHints(isUserUpload = true)
            .mergedWith(YoutubeContentHints(isLive = true))

        assertEquals(YoutubeContentHints(isLive = true, isUserUpload = true), merged)
    }

    private fun order(
        hints: YoutubeContentHints,
        nowMs: Long = 0L,
        ranking: Map<String, YoutubeClientRanking> = emptyMap()
    ): List<YoutubeClientProfile> = YoutubeContentAwareClientSelector.order(
        profiles = profiles,
        hints = hints,
        nowMs = nowMs
    ) { ranking[it.clientName] }

    private fun ranking(
        score: Double = YoutubeContentAwareClientSelector.DEFAULT_SCORE,
        averageLatencyMs: Long = Long.MAX_VALUE,
        consecutiveFailures: Int = 0,
        blockedUntilMs: Long = 0L
    ) = YoutubeClientRanking(score, averageLatencyMs, consecutiveFailures, blockedUntilMs)
}
