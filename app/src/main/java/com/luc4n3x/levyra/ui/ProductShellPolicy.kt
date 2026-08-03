package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.LevyraTab

internal const val PRODUCT_NAVIGATION_HEIGHT_DP = 76
internal const val PRODUCT_MINI_PLAYER_CLEARANCE_DP = 154

internal fun productPrimaryTabs(): List<LevyraTab> = listOf(
    LevyraTab.Home,
    LevyraTab.Search,
    LevyraTab.Explore,
    LevyraTab.Library
)

internal fun productOverlayBottomPadding(hasCurrentTrack: Boolean): Int =
    if (hasCurrentTrack) PRODUCT_MINI_PLAYER_CLEARANCE_DP else PRODUCT_NAVIGATION_HEIGHT_DP

internal fun shouldShowProductNavigation(
    isInPictureInPicture: Boolean,
    selectedTab: LevyraTab,
    hasBlockingOverlay: Boolean
): Boolean = !isInPictureInPicture && !hasBlockingOverlay && selectedTab in productPrimaryTabs()
