@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.luc4n3x.levyra.feature.cast

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.media3.cast.Cast
import androidx.media3.cast.MediaRouteButton as Media3RouteButton

@Composable
fun CastRouteButton(modifier: Modifier = Modifier) {
    val appContext = LocalContext.current.applicationContext
    val castReady = remember(appContext) {
        runCatching {
            Cast.getSingletonInstance(appContext).apply {
                if (needsInitialization()) initialize()
            }
            true
        }.getOrElse { false }
    }
    if (castReady) {
        Media3RouteButton(modifier = modifier)
    }
}
