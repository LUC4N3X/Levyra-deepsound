package com.luc4n3x.levyra.desktop.core.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogArtistDetailTest {
    @Test
    fun trackLikeResolvedChannelNameDoesNotReplaceArtistName() {
        assertEquals(
            "HUGEL",
            chooseDesktopArtistName(
                requestedName = "HUGEL",
                resolvedName = "HUGEL - Isihloko"
            )
        )
    }

    @Test
    fun officialArtistNameIsKeptWhenItIsNotAFalseTrackTitle() {
        assertEquals(
            "The Weeknd",
            chooseDesktopArtistName(
                requestedName = "Weeknd",
                resolvedName = "The Weeknd"
            )
        )
    }

    @Test
    fun youtubePortraitIsPromotedToHighResolution() {
        val upgraded = desktopArtistArtworkUrl(
            "https://yt3.googleusercontent.com/example=s88-c-k-c0x00ffffff-no-rj",
            size = 720
        )

        assertTrue(upgraded.endsWith("=s720-c-k-c0x00ffffff-no-rj"))
    }

    @Test
    fun channelTabsAreRecognizedWithLocalizedLabels() {
        assertEquals(
            DesktopArtistTabKind.TRACKS,
            desktopArtistTabKind(listOf("Brani"))
        )
        assertEquals(
            DesktopArtistTabKind.ALBUMS,
            desktopArtistTabKind(listOf("Discografia album"))
        )
        assertEquals(
            DesktopArtistTabKind.VIDEOS,
            desktopArtistTabKind(listOf("Music videos"))
        )
    }
}
