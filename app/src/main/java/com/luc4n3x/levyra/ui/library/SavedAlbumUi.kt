package com.luc4n3x.levyra.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraGlass
import com.luc4n3x.levyra.ui.theme.LevyraGlassBorder
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText

@Composable
fun BoxScope.SavedAlbumBookmarkOverlay(
    saved: Boolean,
    enabled: Boolean,
    languageCode: String,
    onToggle: () -> Unit
) {
    val density = LocalDensity.current
    val strings = LevyraStrings.forCode(languageCode)
    val fixedStatusBarInset = with(density) {
        WindowInsets.statusBars.getTop(density).toDp()
    }

    Surface(
        onClick = onToggle,
        enabled = enabled,
        shape = CircleShape,
        color = LevyraGlass,
        border = BorderStroke(1.dp, LevyraGlassBorder),
        modifier = Modifier
            .align(Alignment.TopEnd)
            .zIndex(100f)
            .padding(top = fixedStatusBarInset + 8.dp, end = 76.dp)
            .size(48.dp)
    ) {
        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (saved) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = if (saved) strings.removeFromFavorites else strings.addToFavorites,
                tint = when {
                    !enabled -> LevyraMuted
                    saved -> LevyraPink
                    else -> LevyraText
                },
                modifier = Modifier.size(23.dp)
            )
        }
    }
}
