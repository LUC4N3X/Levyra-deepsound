package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.theme.LevyraHomeDesign
import com.luc4n3x.levyra.ui.theme.LevyraType

@Composable
internal fun HomeCollectionFeedCard(
    title: String,
    subtitle: String,
    artwork: Track?,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.levyraPressable(onClick = onOpen, role = Role.Button, pressedScale = LevyraPressScale.Tile),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(LevyraHomeDesign.FeedArtworkShape).background(colors.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            if (artwork != null) {
                CoverImage(track = artwork, modifier = Modifier.fillMaxSize(), highRes = false)
            } else {
                Icon(Icons.Rounded.MusicNote, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(32.dp))
            }
        }
        Text(title, color = colors.onSurface, style = LevyraType.cardTitle.copy(fontSize = LevyraHomeDesign.FeedTitleSize), softWrap = true)
        if (subtitle.isNotBlank()) {
            Text(subtitle, color = colors.onSurfaceVariant, style = LevyraType.caption, softWrap = true)
        }
    }
}
