@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.luc4n3x.levyra.feature.cast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.media3.cast.MediaRouteButton as Media3RouteButton

@Composable
fun CastRouteChip(
    label: String,
    contentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    if (!CastRuntimeInitializer.available) return

    Box(
        modifier = modifier
            .height(40.dp)
            .clip(CircleShape)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clearAndSetSemantics { },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Cast, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = label, color = contentColor, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
        CompositionLocalProvider(LocalContentColor provides Color.Transparent) {
            Media3RouteButton(modifier = Modifier.matchParentSize())
        }
    }
}
