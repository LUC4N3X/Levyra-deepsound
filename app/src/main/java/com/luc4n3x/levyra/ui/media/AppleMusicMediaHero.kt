package com.luc4n3x.levyra.ui.media

import androidx.compose.ui.text.TextStyle
import com.luc4n3x.levyra.ui.components.LevyraTabLabel
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
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
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

internal fun appleMusicMediaColors(cover: Color = Color.Unspecified): ImmersiveMediaColors {
    if (cover.isSpecified) {
        val field = appleAlbumField(cover)
        return ImmersiveMediaColors(
            fieldTop = field, fieldMid = field, base = field,
            content = Color.White, contentMuted = Color.White.copy(alpha = 0.62f), accent = Color.White,
            actionStart = Color.White, actionEnd = Color.White, actionContent = Color(0xFF1C1C1E),
            secondaryFill = Color.White.copy(alpha = 0.14f), hairline = Color.White.copy(alpha = 0.22f)
        )
    }
    return appleMusicPlainMediaColors()
}

private fun appleAlbumField(cover: Color): Color {
    var tone = cover.copy(alpha = 1f)
    var steps = 0
    while (tone.luminance() > AppleAlbumFieldMaxLuminance && steps < 24) {
        tone = androidx.compose.ui.graphics.lerp(tone, Color.Black, 0.1f)
        steps++
    }
    return tone
}

private const val AppleAlbumFieldMaxLuminance = 0.075f

private fun appleMusicPlainMediaColors(): ImmersiveMediaColors = ImmersiveMediaColors(
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
            title, color = colors.content, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
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
        val caption = listOf(overline, metadata).filter(String::isNotBlank).joinToString(" • ").replace(" · ", " • ")
        if (caption.isNotBlank()) {
            Text(
                caption, color = colors.contentMuted, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
            )
        }
        Box(Modifier.fillMaxWidth().padding(16.dp)) { actions() }
        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = colors.hairline)
    }
}

@Composable
internal fun AppleMusicMediaActionRow(
    primary: ImmersiveMediaPrimaryAction, shuffleLabel: String, downloadLabel: String, colors: ImmersiveMediaColors,
    shuffleEnabled: Boolean, downloadEnabled: Boolean, onShuffle: () -> Unit, onDownload: () -> Unit,
    modifier: Modifier, secondary: @Composable RowScope.() -> Unit
) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppleMediaActionGap)) {
            AppleMediaCircle(shuffleLabel, shuffleEnabled, colors, onShuffle) {
                Icon(Icons.Rounded.Shuffle, null, tint = colors.content, modifier = Modifier.size(24.dp))
            }
            AppleMediaPill(primary.label, primary.icon, primary.enabled, primary.loading, primary.contentDescription, colors, primary.onClick, Modifier.width(AppleMediaPlayWidth))
            Row(
                Modifier.size(AppleMediaActionHeight).clip(AppleMusicDesign.pillShape).background(colors.secondaryFill),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
            ) { secondary() }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(AppleMusicDesign.pillShape)
                    .clickable(enabled = downloadEnabled, role = Role.Button, onClick = onDownload)
                    .semantics { contentDescription = downloadLabel },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Download, null, tint = if (downloadEnabled) colors.content else colors.contentMuted, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun AppleMediaCircle(label: String, enabled: Boolean, colors: ImmersiveMediaColors, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Box(
        Modifier.size(AppleMediaActionHeight).clip(AppleMusicDesign.pillShape).background(colors.secondaryFill)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) { icon() }
}

private val AppleMediaActionHeight = 54.dp
private val AppleMediaPlayWidth = 160.dp
private val AppleMediaActionGap = 16.dp

@Composable
private fun AppleMediaPill(
    label: String, icon: ImageVector, enabled: Boolean, loading: Boolean, description: String, colors: ImmersiveMediaColors, onClick: () -> Unit, modifier: Modifier
) {
    Row(
        modifier.height(AppleMediaActionHeight).clip(AppleMusicDesign.pillShape).background(colors.actionStart)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) LevyraLoadingIndicator(color = colors.actionContent, modifier = Modifier.size(20.dp))
        else Icon(icon, null, tint = if (enabled) colors.actionContent else AppleMusicDesign.tertiaryLabel, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(5.dp))
        LevyraTabLabel(
            label,
            TextStyle(color = if (enabled) colors.actionContent else AppleMusicDesign.tertiaryLabel, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
            Modifier.weight(1f, fill = false)
        )
    }
}
