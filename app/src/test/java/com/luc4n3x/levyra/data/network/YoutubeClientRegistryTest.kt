package com.luc4n3x.levyra.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeClientRegistryTest {
    @Test
    fun playbackProfilesExposeStableIdentities() {
        val profiles = YoutubeClientRegistry.playbackProfiles

        assertEquals(profiles.size, profiles.map { it.id }.toSet().size)
        assertEquals(profiles.size, profiles.map { it.clientName }.toSet().size)
        assertEquals(profiles.indices.toList(), profiles.map { it.tier })
        assertTrue(profiles.all { it.clientHeaderName.toIntOrNull() != null })
        assertTrue(profiles.all { it.clientVersion.isNotBlank() && it.userAgent.isNotBlank() })
    }

    @Test
    fun onlyDecipheringClientsRequestASignatureTimestamp() {
        val profiles = YoutubeClientRegistry.playbackProfiles

        assertEquals(
            profiles.filter { it.deciphersStreamUrls }.map { it.clientName },
            profiles.filter { it.useSignatureTimestamp }.map { it.clientName }
        )
    }

    @Test
    fun poTokenIsOnlyRequiredByWebSurfaces() {
        val required = YoutubeClientRegistry.playbackProfiles
            .filter { it.requiresPoToken }
            .map { it.clientName }

        assertEquals(listOf("WEB_REMIX", "WEB"), required)
    }

    @Test
    fun androidVrIsTheOnlyClientThatCannotServeLiveStreams() {
        val restricted = YoutubeClientRegistry.playbackProfiles
            .filterNot { it.supportsLive }
            .map { it.clientName }

        assertEquals(listOf("ANDROID_VR"), restricted)
    }

    @Test
    fun musicOnlySurfacesDeclineUserUploads() {
        val restricted = YoutubeClientRegistry.playbackProfiles
            .filterNot { it.supportsUserUploads }
            .map { it.clientName }

        assertEquals(listOf("ANDROID_VR", "ANDROID_MUSIC"), restricted)
    }

    @Test
    fun embeddedSurfaceIsTheOnlyAgeRestrictionCandidate() {
        val embedded = YoutubeClientRegistry.playbackProfiles.filter { it.isEmbedded }

        assertEquals(listOf("WEB_EMBEDDED_PLAYER"), embedded.map { it.clientName })
        assertEquals(YoutubeClientRegistry.playbackProfiles.last(), embedded.single())
        assertEquals(
            embedded,
            YoutubeClientRegistry.playbackProfiles.filter { it.supportsAgeRestricted }
        )
    }

    @Test
    fun browseProfilesReuseTheRegistryAndHonourTheSyncedRemixVersion() {
        val profiles = YoutubeClientRegistry.browseProfiles("1.99999999.99.99")

        assertEquals(
            listOf("web-remix", "android-music", "android", "ios", "web"),
            profiles.map { it.id }
        )
        assertEquals("1.99999999.99.99", profiles.first().clientVersion)
        assertEquals(profiles.indices.toList(), profiles.map { it.tier })
        assertFalse(profiles.any { it.isEmbedded })
    }

    @Test
    fun blankRemixVersionFallsBackToTheRegistryDefault() {
        val profiles = YoutubeClientRegistry.browseProfiles("")

        assertEquals(YoutubeClientRegistry.WEB_REMIX_VERSION, profiles.first().clientVersion)
    }
}
