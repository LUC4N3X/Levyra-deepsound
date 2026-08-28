@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.luc4n3x.levyra.feature.cast

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.cast.MediaRouteButton as Media3RouteButton
import com.luc4n3x.levyra.ui.components.playerGlass
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign

@Composable
fun CastRouteButton(modifier: Modifier = Modifier) {
    if (!CastRuntimeInitializer.available) return

    Box(
        modifier = modifier.playerGlass(
            shape = CircleShape,
            fill = LevyraPlayerDesign.GlassFill,
            borderTop = LevyraPlayerDesign.GlassBorderTop,
            borderBottom = LevyraPlayerDesign.GlassBorderBottom
        ),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(
            LocalContentColor provides Color.White.copy(alpha = 0.86f)
        ) {
            Media3RouteButton(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            )
        }
    }
}
