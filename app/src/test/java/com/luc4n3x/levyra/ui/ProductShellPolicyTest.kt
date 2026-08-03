package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.LevyraTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductShellPolicyTest {
    @Test
    fun primaryNavigationKeepsPlayerContextual() {
        assertEquals(
            listOf(LevyraTab.Home, LevyraTab.Search, LevyraTab.Explore, LevyraTab.Library),
            productPrimaryTabs()
        )
        assertFalse(LevyraTab.Player in productPrimaryTabs())
    }

    @Test
    fun navigationIsHiddenForPipPlayerAndBlockingOverlays() {
        assertFalse(
            shouldShowProductNavigation(
                isInPictureInPicture = true,
                selectedTab = LevyraTab.Home,
                hasBlockingOverlay = false
            )
        )
        assertFalse(
            shouldShowProductNavigation(
                isInPictureInPicture = false,
                selectedTab = LevyraTab.Player,
                hasBlockingOverlay = false
            )
        )
        assertFalse(
            shouldShowProductNavigation(
                isInPictureInPicture = false,
                selectedTab = LevyraTab.Home,
                hasBlockingOverlay = true
            )
        )
        assertTrue(
            shouldShowProductNavigation(
                isInPictureInPicture = false,
                selectedTab = LevyraTab.Search,
                hasBlockingOverlay = false
            )
        )
    }

    @Test
    fun contentClearanceAccountsForMiniPlayer() {
        assertEquals(PRODUCT_NAVIGATION_HEIGHT_DP, productOverlayBottomPadding(hasCurrentTrack = false))
        assertEquals(PRODUCT_MINI_PLAYER_CLEARANCE_DP, productOverlayBottomPadding(hasCurrentTrack = true))
    }

    @Test
    fun homeHeaderClearanceOnlyAppliesWhenHeaderIsVisible() {
        assertEquals(
            PRODUCT_HOME_HEADER_CLEARANCE_DP,
            productHomeTopPadding(
                isInPictureInPicture = false,
                selectedTab = LevyraTab.Home,
                hasBlockingOverlay = false
            )
        )
        assertEquals(
            0,
            productHomeTopPadding(
                isInPictureInPicture = true,
                selectedTab = LevyraTab.Home,
                hasBlockingOverlay = false
            )
        )
        assertEquals(
            0,
            productHomeTopPadding(
                isInPictureInPicture = false,
                selectedTab = LevyraTab.Search,
                hasBlockingOverlay = false
            )
        )
        assertEquals(
            0,
            productHomeTopPadding(
                isInPictureInPicture = false,
                selectedTab = LevyraTab.Home,
                hasBlockingOverlay = true
            )
        )
    }
}
