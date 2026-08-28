package com.luc4n3x.levyra.feature.cast

import android.view.ContextThemeWrapper
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import androidx.mediarouter.R as MediaRouterR

@Composable
fun CastRouteButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier,
        factory = {
            val themed = ContextThemeWrapper(context, MediaRouterR.style.Theme_MediaRouter)
            MediaRouteButton(themed).apply {
                CastButtonFactory.setUpMediaRouteButton(context.applicationContext, this)
                setPadding(0, 0, 0, 0)
            }
        }
    )
}
