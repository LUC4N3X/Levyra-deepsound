package com.luc4n3x.levyra.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.theme.AppleMusicDesign

internal fun appleMusicMediaColors(): ImmersiveMediaColors = ImmersiveMediaColors(
    fieldTop = AppleMusicDesign.background, fieldMid = AppleMusicDesign.background, base = AppleMusicDesign.background,
    content = AppleMusicDesign.label, contentMuted = AppleMusicDesign.secondaryLabel, accent = AppleMusicDesign.accent,
    actionStart = AppleMusicDesign.fill, actionEnd = AppleMusicDesign.fill, actionContent = AppleMusicDesign.accent,
    secondaryFill = AppleMusicDesign.fill, hairline = AppleMusicDesign.separator
)

@Composable
internal fun AppleMusicMediaHero(
    title: String, overline: String, subtitle: String, metadata: String, colors: ImmersiveMediaColors,
    viewportWidth: Dp, topBarHeight: Dp, onSubtitleClick: (() -> Unit)?, actions: @Composable () -> Unit,
    artwork: @Composable BoxScope.() -> Unit, modifier: Modifier
) {
    Column(modifier.fillMaxWidth().background(colors.base).padding(top = topBarHeight), horizontalAlignment = Alignment.CenterHorizontally) {
        val art = min(viewportWidth - AppleMusicDesign.collectionArtworkInset * 2, 420.dp)
        Box(
            Modifier.padding(top = 10.dp).size(art)
                .shadow(18.dp, AppleMusicDesign.artworkShape, clip = false, ambientColor = Color.Black.copy(alpha = 0.18f), spotColor = Color.Black.copy(alpha = 0.26f))
                .clip(AppleMusicDesign.artworkShape).background(AppleMusicDesign.placeholder)
                .border(0.5.dp, AppleMusicDesign.artworkStroke, AppleMusicDesign.artworkShape)
        ) { artwork() }
        Text(
            title, color = colors.content, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
            textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 15.dp).semantics { heading() }
        )
        if (subtitle.isNotBlank()) {
            Text(
                subtitle, color = colors.accent, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
                textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp)
                    .then(if (onSubtitleClick != null) Modifier.clickable(role = Role.Button, onClick = onSubtitleClick) else Modifier)
            )
        }
        val caption = listOf(overline, metadata).filter(String::isNotBlank).joinToString(" · ")
        if (caption.isNotBlank()) {
            Text(
                caption.uppercase(), color = colors.contentMuted, style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
            )
        }
        Box(Modifier.fillMaxWidth().padding(16.dp)) { actions() }
        HorizontalDivider(Modifier.padding(start = 16.dp), thickness = 0.5.dp, color = AppleMusicDesign.separator)
    }
}

@Composable
internal fun AppleMusicMediaActionRow(
    primary: ImmersiveMediaPrimaryAction, shuffleLabel: String, downloadLabel: String,
    shuffleEnabled: Boolean, downloadEnabled: Boolean, onShuffle: () -> Unit, onDownload: () -> Unit,
    modifier: Modifier, secondary: @Composable RowScope.() -> Unit
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AppleMediaPill(primary.label, primary.icon, primary.enabled, primary.loading, primary.contentDescription, primary.onClick, Modifier.weight(1f))
            AppleMediaPill(shuffleLabel, Icons.Rounded.Shuffle, shuffleEnabled, false, shuffleLabel, onShuffle, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            secondary()
            Box(
                Modifier.size(44.dp).clip(AppleMusicDesign.pillShape)
                    .clickable(enabled = downloadEnabled, role = Role.Button, onClick = onDownload)
                    .semantics { contentDescription = downloadLabel },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Download, null, tint = if (downloadEnabled) AppleMusicDesign.accent else AppleMusicDesign.tertiaryLabel, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun AppleMediaPill(
    label: String, icon: ImageVector, enabled: Boolean, loading: Boolean, description: String, onClick: () -> Unit, modifier: Modifier
) {
    Row(
        modifier.height(AppleMusicDesign.pillHeight).clip(AppleMusicDesign.pillShape).background(AppleMusicDesign.fill)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) LevyraLoadingIndicator(color = AppleMusicDesign.accent, modifier = Modifier.size(20.dp))
        else Icon(icon, null, tint = if (enabled) AppleMusicDesign.accent else AppleMusicDesign.tertiaryLabel, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            label, color = if (enabled) AppleMusicDesign.accent else AppleMusicDesign.tertiaryLabel,
            fontSize = 18.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}
