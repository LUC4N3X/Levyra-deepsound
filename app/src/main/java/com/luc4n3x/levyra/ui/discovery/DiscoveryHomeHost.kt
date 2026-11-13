package com.luc4n3x.levyra.ui.discovery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luc4n3x.levyra.domain.HomeStyle
import com.luc4n3x.levyra.ui.CuratedPlaylistsHost
import com.luc4n3x.levyra.ui.theme.LevyraSkinManagesLayout
import com.luc4n3x.levyra.viewmodel.DiscoveryViewModel

@Composable
internal fun DiscoveryHomeHost(
    factory: ViewModelProvider.Factory,
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    backEnabled: Boolean,
    style: HomeStyle,
    animationsEnabled: Boolean,
    skinComposesDiscovery: Boolean,
    bottomInset: Dp,
    content: @Composable () -> Unit
) {
    val skinHidesDiscovery = LevyraSkinManagesLayout && !skinComposesDiscovery && !com.luc4n3x.levyra.ui.theme.LevyraIsMono && !com.luc4n3x.levyra.ui.theme.LevyraIsAtelier && !com.luc4n3x.levyra.ui.theme.LevyraIsTorque && !com.luc4n3x.levyra.ui.theme.LevyraIsIvory && com.luc4n3x.levyra.ui.theme.LevyraActivePalette.id != com.luc4n3x.levyra.ui.theme.PrismPaletteId &&
        !com.luc4n3x.levyra.ui.theme.LevyraIsYouTubeMusic && !com.luc4n3x.levyra.ui.theme.LevyraIsSpotify
    val homeContent: @Composable () -> Unit = { CuratedPlaylistsHost(factory, bottomInset, backEnabled, content) }
    if (skinHidesDiscovery && !skinShowsDailyDiscovery()) {
        homeContent()
        return
    }
    val discovery: DiscoveryViewModel = viewModel(key = "levyra-discovery", factory = factory)
    val openChange by rememberUpdatedState(onOpenChange)
    val handle = remember(discovery) {
        DiscoveryHomeHandle(
            state = discovery.state,
            onWarm = discovery::warm,
            onOpen = { openChange(true) },
            onRetry = discovery::retry,
            onOpenBubble = { level ->
                discovery.setLevel(level)
                openChange(true)
            },
            homeTrackKeys = discovery::homeVisibleTrackKeys,
            familiarArtistKeys = discovery::familiarArtistKeys,
            onPlayDaily = discovery::playDaily
        )
    }
    if (skinShowsDailyDiscovery()) LaunchedEffect(discovery) { discovery.warmDaily() }
    if (skinHidesDiscovery) {
        CompositionLocalProvider(LocalDailyDiscovery provides handle, content = homeContent)
        return
    }
    CompositionLocalProvider(LocalHomeDiscovery provides handle, LocalDailyDiscovery provides handle, content = homeContent)
    if (open) {
        DiscoveryScreen(
            viewModel = discovery,
            style = style,
            animationsEnabled = animationsEnabled,
            bottomInset = bottomInset,
            backEnabled = backEnabled,
            onDismiss = { openChange(false) },
            onOpenSaved = {
                openChange(false)
                discovery.openSavedPlaylist()
            }
        )
    }
}
