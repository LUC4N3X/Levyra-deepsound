package com.luc4n3x.levyra.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.theme.*

private val TorqueMediaBand = GenericShape { size, _ ->
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height * 0.62f)
    lineTo(0f, size.height)
    close()
}

internal fun skinMediaColors(cover: Color = Color.Unspecified): ImmersiveMediaColors? {
    if (!LevyraIsPrism) return null
    val p = LevyraActivePalette
    if (LevyraIsAppleMusic) return appleMusicMediaColors()
    if (com.luc4n3x.levyra.ui.theme.LevyraIsYouTubeMusic) return youTubeMusicMediaColors()
    if (com.luc4n3x.levyra.ui.theme.LevyraIsQobuz) return qobuzMediaColors(cover)
    if (LevyraIsSpotify) {
        val field = if (cover.isSpecified) SpotifyDesign.heroSurface(cover) else SpotifyDesign.highlight
        return ImmersiveMediaColors(
            fieldTop = field, fieldMid = SpotifyDesign.wash(field, 0.45f), base = SpotifyDesign.base,
            content = SpotifyDesign.text, contentMuted = SpotifyDesign.subdued, accent = SpotifyDesign.green,
            actionStart = SpotifyDesign.green, actionEnd = SpotifyDesign.green, actionContent = Color.Black,
            secondaryFill = SpotifyDesign.elevated, hairline = SpotifyDesign.decorative
        )
    }
    return ImmersiveMediaColors(
        fieldTop = p.black, fieldMid = p.black, base = p.black,
        content = p.text, contentMuted = p.muted, accent = p.cyan,
        actionStart = p.blue, actionEnd = if (LevyraIsTorque || LevyraIsIvory) p.blue else p.violet, actionContent = if (LevyraIsIvory) IvoryDesign.onInk else Color.White,
        secondaryFill = p.panel, hairline = p.outline
    )
}

@Composable
private fun MediaSubtitle(text: String, color: Color, onClick: (() -> Unit)?, align: TextAlign = TextAlign.Start, uppercase: Boolean = false) {
    if (text.isBlank()) return
    Text(
        if (uppercase) text.uppercase() else text, color = color, style = MaterialTheme.typography.titleMedium, textAlign = align,
        letterSpacing = if (uppercase) 1.2.sp else MaterialTheme.typography.titleMedium.letterSpacing,
        maxLines = 2, overflow = TextOverflow.Ellipsis,
        modifier = if (onClick != null) Modifier.heightIn(min = LevyraPlayerDesign.MinimumTouchTarget).clickable(role = Role.Button, onClick = onClick).wrapContentHeight().padding(vertical = 4.dp) else Modifier
    )
}

@Composable
internal fun PrismMediaHero(
    title: String, overline: String, subtitle: String, metadata: String, colors: ImmersiveMediaColors,
    viewportWidth: Dp, topBarHeight: Dp, onSubtitleClick: (() -> Unit)?, actions: @Composable () -> Unit,
    artwork: @Composable BoxScope.() -> Unit, modifier: Modifier
) {
    Column(modifier.fillMaxWidth().padding(top = topBarHeight, start = 24.dp, end = 24.dp, bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val art = min(viewportWidth * 0.72f, 300.dp)
        Box(Modifier.size(art).clip(RoundedCornerShape(PrismDesign.artworkCorner))) { artwork() }
        Box(Modifier.padding(top = 6.dp).width(art * 0.6f).height(2.dp).background(PrismAccent.gradient(), RoundedCornerShape(1.dp)))
        Spacer(Modifier.height(20.dp))
        if (overline.isNotBlank()) Text(overline.uppercase(), color = colors.accent, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.6.sp, maxLines = 1)
        Text(title, color = colors.content, fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.8).sp, textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() })
        MediaSubtitle(subtitle, colors.content.copy(alpha = 0.86f), onSubtitleClick, align = TextAlign.Center)
        if (metadata.isNotBlank()) Text(metadata, color = colors.contentMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(18.dp))
        actions()
    }
}

@Composable
internal fun SpotifyMediaHero(
    title: String, overline: String, subtitle: String, metadata: String, colors: ImmersiveMediaColors,
    viewportWidth: Dp, topBarHeight: Dp, onSubtitleClick: (() -> Unit)?, actions: @Composable () -> Unit,
    artwork: @Composable BoxScope.() -> Unit, modifier: Modifier
) {
    val field = Brush.verticalGradient(0f to colors.fieldTop, 0.55f to colors.fieldMid, 1f to colors.base)
    val art = min(viewportWidth * 0.66f, SpotifyDesign.heroArtwork)
    Column(modifier.fillMaxWidth().background(field).padding(top = topBarHeight + 8.dp)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(art)
                    .shadow(24.dp, RoundedCornerShape(SpotifyDesign.cornerSmall), clip = false, ambientColor = Color.Black.copy(alpha = 0.5f), spotColor = Color.Black.copy(alpha = 0.7f))
                    .clip(RoundedCornerShape(SpotifyDesign.cornerSmall))
            ) { artwork() }
        }
        Column(Modifier.fillMaxWidth().padding(start = SpotifyDesign.gutter, end = SpotifyDesign.gutter, top = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                title, color = colors.content, style = MaterialTheme.typography.headlineMedium,
                maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() }
            )
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle, color = colors.content, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = if (onSubtitleClick != null) Modifier.heightIn(min = LevyraPlayerDesign.MinimumTouchTarget).clickable(role = Role.Button, onClick = onSubtitleClick).wrapContentHeight() else Modifier
                )
            }
            val detail = listOf(overline, metadata).filter { it.isNotBlank() }.joinToString(" • ")
            if (detail.isNotBlank()) Text(detail, color = colors.contentMuted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.padding(horizontal = SpotifyDesign.gutter)) { actions() }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
internal fun SpotifyMediaActionRow(
    primary: ImmersiveMediaPrimaryAction, shuffleLabel: String, downloadLabel: String, colors: ImmersiveMediaColors,
    shuffleEnabled: Boolean, downloadEnabled: Boolean, onShuffle: () -> Unit, onDownload: () -> Unit,
    modifier: Modifier, secondary: @Composable RowScope.() -> Unit
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        secondary()
        ImmersiveMediaQuietAction(Icons.Rounded.Download, downloadLabel, downloadEnabled, colors, onDownload)
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.size(LevyraPlayerDesign.MinimumTouchTarget).clip(CircleShape)
                .clickable(enabled = shuffleEnabled, role = Role.Button, onClickLabel = shuffleLabel, onClick = onShuffle)
                .semantics { contentDescription = shuffleLabel },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Shuffle, null, tint = if (shuffleEnabled) colors.contentMuted else colors.contentMuted.copy(alpha = 0.4f), modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier.size(SpotifyDesign.playButton).clip(CircleShape).background(if (primary.enabled) SpotifyDesign.green else SpotifyDesign.faint)
                .clickable(enabled = primary.enabled && !primary.loading, role = Role.Button, onClickLabel = primary.label, onClick = primary.onClick)
                .semantics { contentDescription = primary.contentDescription },
            contentAlignment = Alignment.Center
        ) {
            if (primary.loading) com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
            else Icon(primary.icon, null, tint = Color.Black, modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
internal fun IvoryMediaHero(
    title: String, overline: String, subtitle: String, metadata: String, colors: ImmersiveMediaColors,
    viewportWidth: Dp, topBarHeight: Dp, onSubtitleClick: (() -> Unit)?, actions: @Composable () -> Unit,
    artwork: @Composable BoxScope.() -> Unit, modifier: Modifier
) {
    val ink = IvoryDesign.ink
    val onInk = IvoryDesign.onInk
    val art = min(viewportWidth * 0.7f, 296.dp)
    Column(modifier.fillMaxWidth().padding(top = topBarHeight + 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.matchParentSize().padding(top = art * 0.62f, start = 14.dp, end = 14.dp).clip(RoundedCornerShape(36.dp)).background(ink))
            Column(Modifier.padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(art).clip(IvoryDesign.coverShape)) { artwork() }
                Spacer(Modifier.height(18.dp))
                Column(Modifier.fillMaxWidth().padding(horizontal = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (overline.isNotBlank()) Text(overline, color = IvoryDesign.rose, style = MaterialTheme.typography.labelLarge, letterSpacing = 1.4.sp, maxLines = 1)
                    Text(title, color = onInk, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() })
                    MediaSubtitle(subtitle, onInk.copy(alpha = 0.78f), onSubtitleClick, align = TextAlign.Center)
                    if (metadata.isNotBlank()) Text(metadata, color = onInk.copy(alpha = 0.55f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Box(Modifier.padding(horizontal = 24.dp)) { actions() }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
internal fun TorqueMediaHero(
    title: String, overline: String, subtitle: String, metadata: String, colors: ImmersiveMediaColors,
    viewportWidth: Dp, onSubtitleClick: (() -> Unit)?, actions: @Composable () -> Unit,
    artwork: @Composable BoxScope.() -> Unit, modifier: Modifier
) {
    Column(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(min(viewportWidth, 420.dp))) { artwork() }
        Box(Modifier.fillMaxWidth().height(150.dp)) {
            Box(Modifier.matchParentSize().clip(TorqueMediaBand).background(LevyraActivePalette.blue))
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (overline.isNotBlank()) Text(overline.uppercase(), color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp, maxLines = 1)
                Text(title.uppercase(), color = Color.White, fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() })
                MediaSubtitle(subtitle, Color.White.copy(alpha = 0.92f), onSubtitleClick, align = TextAlign.Center)
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (metadata.isNotBlank()) Text(metadata.uppercase(), color = colors.contentMuted, fontSize = 12.sp, letterSpacing = 1.4.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            actions()
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
internal fun NocturneMediaHero(
    title: String, overline: String, subtitle: String, metadata: String, colors: ImmersiveMediaColors,
    topBarHeight: Dp, onSubtitleClick: (() -> Unit)?, actions: @Composable () -> Unit,
    artwork: @Composable BoxScope.() -> Unit, modifier: Modifier
) {
    val amber = colors.accent
    val frame = RoundedCornerShape(Nocturne78Design.artworkCorner + 4.dp)
    val readout = RoundedCornerShape(Nocturne78Design.controlCorner)
    Column(modifier.fillMaxWidth().padding(top = topBarHeight, start = 20.dp, end = 20.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).border(1.dp, amber.copy(alpha = 0.45f), frame)
                .padding(10.dp).clip(RoundedCornerShape(Nocturne78Design.artworkCorner))
        ) { artwork() }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (overline.isNotBlank()) Text(overline.uppercase(), color = amber, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.4.sp, maxLines = 1)
            Text(title, color = colors.content, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Medium, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() })
            MediaSubtitle(subtitle, colors.contentMuted, onSubtitleClick, uppercase = true)
        }
        if (metadata.isNotBlank()) {
            Box(
                Modifier.fillMaxWidth().clip(readout).background(Brush.verticalGradient(listOf(LevyraPanelSoft, LevyraPanel)))
                    .border(1.dp, colors.hairline, readout).padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(metadata.uppercase(), color = amber, fontFamily = FontFamily.Monospace, fontSize = 12.sp, letterSpacing = 1.2.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        actions()
    }
}
