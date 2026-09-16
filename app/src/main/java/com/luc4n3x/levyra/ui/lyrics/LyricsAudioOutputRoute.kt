package com.luc4n3x.levyra.ui.lyrics

import androidx.compose.runtime.Composable
import com.luc4n3x.levyra.feature.audio.LevyraAudioOutputRoute
import com.luc4n3x.levyra.feature.audio.rememberLevyraAudioOutputState
import com.luc4n3x.levyra.feature.audio.resolveLevyraPreTiramisuAudioOutputRoute
import com.luc4n3x.levyra.feature.audio.selectLevyraAudioOutputRoute
import com.luc4n3x.levyra.feature.audio.stableLevyraAudioRouteKey

typealias LyricsAudioOutputRoute = LevyraAudioOutputRoute

internal fun selectLyricsAudioOutputRoute(
    routes: List<LyricsAudioOutputRoute>,
    systemOrdered: Boolean
): LyricsAudioOutputRoute? = selectLevyraAudioOutputRoute(routes, systemOrdered)

internal fun stableLyricsAudioRouteKey(type: Int, address: String?): String? =
    stableLevyraAudioRouteKey(type, address)

@Composable
fun rememberLyricsAudioOutputRoute(): LyricsAudioOutputRoute? =
    rememberLevyraAudioOutputState().active

internal fun resolvePreTiramisuAudioOutputRoute(
    routes: List<LyricsAudioOutputRoute>,
    selectedRouteName: String?,
    selectedDeviceType: Int
): LyricsAudioOutputRoute? =
    resolveLevyraPreTiramisuAudioOutputRoute(routes, selectedRouteName, selectedDeviceType)
