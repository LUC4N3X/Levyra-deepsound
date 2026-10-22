package com.luc4n3x.levyra.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.theme.YouTubeMusicDesign

internal fun youTubeMusicMediaColors(): ImmersiveMediaColors = ImmersiveMediaColors(
    fieldTop = Color.Transparent, fieldMid = Color.Transparent, base = YouTubeMusicDesign.background,
    content = YouTubeMusicDesign.text, contentMuted = YouTubeMusicDesign.textSecondary, accent = Color.White,
    actionStart = Color.White, actionEnd = Color.White, actionContent = Color.Black,
    secondaryFill = YouTubeMusicDesign.buttonFill, hairline = YouTubeMusicDesign.divider
)

@Composable
internal fun YouTubeMusicMediaHero(
    title: String, overline: String, subtitle: String, metadata: String, colors: ImmersiveMediaColors,
    viewportWidth: Dp, topBarHeight: Dp, onSubtitleClick: (() -> Unit)?, actions: @Composable () -> Unit,
    artwork: @Composable BoxScope.() -> Unit, modifier: Modifier
) {
    Column(modifier.fillMaxWidth().padding(top = topBarHeight), horizontalAlignment = Alignment.CenterHorizontally) {
        val art = min(viewportWidth - 96.dp, YouTubeMusicDesign.collectionArtwork + 40.dp)
        Box(Modifier.padding(top = 12.dp).size(art).clip(YouTubeMusicDesign.cardShape).background(YouTubeMusicDesign.placeholder)) { artwork() }
        Text(
            title, color = colors.content, style = MaterialTheme.typography.displayMedium,
            textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp).semantics { heading() }
        )
        if (subtitle.isNotBlank()) {
            Text(
                subtitle, color = colors.content, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp)
                    .then(if (onSubtitleClick != null) Modifier.levyraPressable(onClick = onSubtitleClick, role = Role.Button) else Modifier)
            )
        }
        val caption = listOf(overline, metadata).filter(String::isNotBlank).joinToString(" • ")
        if (caption.isNotBlank()) {
            Text(
                caption, color = colors.contentMuted, style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 6.dp)
            )
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp)) { actions() }
    }
}

@Composable
internal fun YouTubeMusicMediaActionRow(
    primary: ImmersiveMediaPrimaryAction, shuffleLabel: String, downloadLabel: String,
    shuffleEnabled: Boolean, downloadEnabled: Boolean, onShuffle: () -> Unit, onDownload: () -> Unit,
    modifier: Modifier, secondary: @Composable RowScope.() -> Unit
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        YouTubeMusicMediaCircle(Icons.Rounded.Download, downloadLabel, downloadEnabled, onDownload)
        YouTubeMusicMediaCircle(Icons.Rounded.Shuffle, shuffleLabel, shuffleEnabled, onShuffle)
        Box(
            Modifier.size(YouTubeMusicDesign.collectionPlay).clip(CircleShape).background(Color.White)
                .levyraPressable(onClick = primary.onClick, enabled = primary.enabled, role = Role.Button, pressedScale = LevyraPressScale.Control)
                .semantics { contentDescription = primary.contentDescription },
            contentAlignment = Alignment.Center
        ) {
            if (primary.loading) LevyraLoadingIndicator(color = Color.Black, modifier = Modifier.size(28.dp))
            else Icon(primary.icon, null, tint = Color.Black, modifier = Modifier.size(36.dp))
        }
        secondary()
    }
}

@Composable
private fun YouTubeMusicMediaCircle(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(YouTubeMusicDesign.collectionAction).clip(CircleShape).background(YouTubeMusicDesign.buttonFill)
            .levyraPressable(onClick = onClick, enabled = enabled, role = Role.Button, pressedScale = LevyraPressScale.Control)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = if (enabled) Color.White else YouTubeMusicDesign.textTertiary, modifier = Modifier.size(24.dp))
    }
}
