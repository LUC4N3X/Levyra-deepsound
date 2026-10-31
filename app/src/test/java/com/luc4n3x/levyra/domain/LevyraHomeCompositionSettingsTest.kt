package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraHomeCompositionSettingsTest {
    private val studio = LevyraHomeStudioSettings()
    private val speedDial = HomeBlock.Section(HomeSectionId.SpeedDial)
    private val spotlight = HomeBlock.Section(HomeSectionId.Spotlight)
    private val genreChips = HomeBlock.Section(HomeSectionId.GenreChips)

    @Test
    fun everySkinDefaultsToOriginalWithoutComposedBlocks() {
        val settings = LevyraHomeCompositionSettings()

        LevyraSkin.entries.forEach { skin ->
            assertEquals(HomeCompositionMode.Original, settings.modeFor(skin))
            assertNull(settings.resolvedBlocks(skin, studio))
        }
    }

    @Test
    fun blankOrMalformedStorageDecodesToDefaults() {
        listOf(null, "", "   ", "not json", "[]", "{", """{"skins":5}""", """{"skins":{"Default":3}}""").forEach { raw ->
            assertEquals(LevyraHomeCompositionSettings(), LevyraHomeCompositionSettings.decode(raw))
        }
    }

    @Test
    fun encodeDecodeRoundTripsModesAndBlocks() {
        val settings = LevyraHomeCompositionSettings()
            .withCustomBlocks(LevyraSkin.Default, listOf(spotlight, speedDial))
            .withMode(LevyraSkin.Prism, HomeCompositionMode.Levyra)
            .withCustomBlocks(LevyraSkin.AppleMusicStyle, listOf(HomeBlock.SkinFeed, speedDial))

        assertEquals(settings, LevyraHomeCompositionSettings.decode(settings.encode()))
    }

    @Test
    fun unknownSkinsAndBlockIdsAreIgnored() {
        val decoded = LevyraHomeCompositionSettings.decode(
            """{"skins":{"Gone":{"mode":"custom","blocks":["speed_dial"]},"Default":{"mode":"custom","blocks":["missing","speed_dial"]}}}"""
        )

        assertEquals(setOf(LevyraSkin.Default), decoded.skins.keys)
        assertEquals(listOf<HomeBlock>(speedDial), decoded.forSkin(LevyraSkin.Default).customBlocks)
    }

    @Test
    fun unknownModeFallsBackToOriginal() {
        val decoded = LevyraHomeCompositionSettings.decode("""{"skins":{"Default":{"mode":"future"}}}""")

        assertEquals(HomeCompositionMode.Original, decoded.modeFor(LevyraSkin.Default))
        assertTrue(decoded.skins.isEmpty())
    }

    @Test
    fun duplicateBlocksAreRemovedKeepingFirstPosition() {
        val settings = LevyraHomeCompositionSettings()
            .withCustomBlocks(LevyraSkin.Default, listOf(spotlight, speedDial, spotlight))
        val decoded = LevyraHomeCompositionSettings.decode(
            """{"skins":{"Default":{"mode":"custom","blocks":["spotlight","speed_dial","spotlight"]}}}"""
        )

        assertEquals(listOf<HomeBlock>(spotlight, speedDial), settings.forSkin(LevyraSkin.Default).customBlocks)
        assertEquals(listOf<HomeBlock>(spotlight, speedDial), decoded.forSkin(LevyraSkin.Default).customBlocks)
    }

    @Test
    fun skinsAreIndependent() {
        val settings = LevyraHomeCompositionSettings()
            .withMode(LevyraSkin.Default, HomeCompositionMode.Levyra)
            .withCustomBlocks(LevyraSkin.Prism, listOf(speedDial))

        assertEquals(HomeCompositionMode.Levyra, settings.modeFor(LevyraSkin.Default))
        assertEquals(HomeCompositionMode.Custom, settings.modeFor(LevyraSkin.Prism))
        assertEquals(HomeCompositionMode.Original, settings.modeFor(LevyraSkin.Torque))
    }

    @Test
    fun resetAffectsOnlyTheRequestedSkin() {
        val settings = LevyraHomeCompositionSettings()
            .withCustomBlocks(LevyraSkin.Default, listOf(speedDial))
            .withCustomBlocks(LevyraSkin.Prism, listOf(spotlight))
            .reset(LevyraSkin.Default)

        assertEquals(HomeCompositionMode.Original, settings.modeFor(LevyraSkin.Default))
        assertNull(settings.forSkin(LevyraSkin.Default).customBlocks)
        assertEquals(listOf<HomeBlock>(spotlight), settings.forSkin(LevyraSkin.Prism).customBlocks)
    }

    @Test
    fun existingUsersStayOnOriginalAndCustomStartsFromTheirCurrentLayout() {
        val customizedStudio = LevyraHomeStudioSettings()
            .updateLayout(HomeStyle.Original) { it.withVisibility(HomeSectionId.PersonalOrbit, false) }
        val existingUser = LevyraHomeCompositionSettings.decode(null)

        assertNull(existingUser.resolvedBlocks(LevyraSkin.Default, customizedStudio))
        val firstCustom = existingUser
            .withMode(LevyraSkin.Default, HomeCompositionMode.Custom)
            .resolvedBlocks(LevyraSkin.Default, customizedStudio)
            .orEmpty()
        assertEquals(LevyraHomeCatalog.originalBlocks(LevyraSkin.Default, customizedStudio), firstCustom)
        assertFalse(HomeBlock.Section(HomeSectionId.PersonalOrbit) in firstCustom)
    }

    @Test
    fun levyraBlocksExcludeGenreChipsOnlyForSpotifyStyle() {
        LevyraSkin.entries.forEach { skin ->
            assertEquals(skin != LevyraSkin.SpotifyStyle, genreChips in LevyraHomeCatalog.levyraBlocks(skin))
        }
    }

    @Test
    fun customBlocksSurviveSwitchingToOtherModesAndBack() {
        val blocks = listOf<HomeBlock>(spotlight, speedDial)
        val settings = LevyraHomeCompositionSettings()
            .withCustomBlocks(LevyraSkin.Default, blocks)
            .withMode(LevyraSkin.Default, HomeCompositionMode.Levyra)

        assertEquals(LevyraHomeCatalog.levyraBlocks(LevyraSkin.Default), settings.resolvedBlocks(LevyraSkin.Default, studio))
        val original = settings.withMode(LevyraSkin.Default, HomeCompositionMode.Original)
        assertNull(original.resolvedBlocks(LevyraSkin.Default, studio))
        val restored = LevyraHomeCompositionSettings.decode(original.encode())
            .withMode(LevyraSkin.Default, HomeCompositionMode.Custom)
        assertEquals(blocks, restored.resolvedBlocks(LevyraSkin.Default, studio))
    }

    @Test
    fun skinFeedIsRejectedForSkinsWithoutTheirOwnFeed() {
        LevyraSkin.entries.forEach { skin ->
            val blocks = LevyraHomeCompositionSettings()
                .withCustomBlocks(skin, listOf(HomeBlock.SkinFeed, speedDial))
                .forSkin(skin)
                .customBlocks
                .orEmpty()
            val expected = if (LevyraHomeCatalog.hasOwnFeed(skin)) listOf(HomeBlock.SkinFeed, speedDial) else listOf(speedDial)

            assertEquals(expected, blocks)
        }
    }

    @Test
    fun storedSkinFeedIsDroppedOnDecodeForSkinsWithoutTheirOwnFeed() {
        val decoded = LevyraHomeCompositionSettings.decode(
            """{"skins":{"Default":{"mode":"custom","blocks":["skin_feed","speed_dial"]},"Atelier":{"mode":"custom","blocks":["skin_feed"]}}}"""
        )

        assertEquals(listOf<HomeBlock>(speedDial), decoded.forSkin(LevyraSkin.Default).customBlocks)
        assertEquals(listOf<HomeBlock>(HomeBlock.SkinFeed), decoded.forSkin(LevyraSkin.Atelier).customBlocks)
    }

    @Test
    fun genreChipsCannotBeAddedToSpotifyStyleCustomLayouts() {
        val blocks = LevyraHomeCompositionSettings()
            .withCustomBlocks(LevyraSkin.SpotifyStyle, listOf(genreChips, speedDial))
            .forSkin(LevyraSkin.SpotifyStyle)
            .customBlocks

        assertEquals(listOf<HomeBlock>(speedDial), blocks)
    }

    @Test
    fun originalBlocksOfSkinsWithOwnFeedAreTheFeedAlone() {
        assertEquals(
            listOf<HomeBlock>(HomeBlock.SkinFeed),
            LevyraHomeCatalog.originalBlocks(LevyraSkin.AppleMusicStyle, studio)
        )
    }
}
