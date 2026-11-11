package com.luc4n3x.levyra.ui.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luc4n3x.levyra.data.ArtworkPalette
import com.luc4n3x.levyra.domain.HomeStyle
import com.luc4n3x.levyra.feature.discovery.DiscoveryCard
import com.luc4n3x.levyra.feature.discovery.dailyDiscoveryCards
import com.luc4n3x.levyra.ui.AtelierPaper
import com.luc4n3x.levyra.ui.AtelierSectionLabel
import com.luc4n3x.levyra.ui.AtelierSerif
import com.luc4n3x.levyra.ui.CoverImage
import com.luc4n3x.levyra.ui.HomeStyleSpec
import com.luc4n3x.levyra.ui.IvoryCornerBadge
import com.luc4n3x.levyra.ui.MonoSectionHeader
import com.luc4n3x.levyra.ui.PrismShelfHeader
import com.luc4n3x.levyra.ui.PrismSplitArtwork
import com.luc4n3x.levyra.ui.SkinSectionTitle
import com.luc4n3x.levyra.ui.StudioSectionHeader
import com.luc4n3x.levyra.ui.artwork.rememberArtworkPalette
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.defaultRailItemMotion
import com.luc4n3x.levyra.ui.defaultRailMotionEnabled
import com.luc4n3x.levyra.ui.defaultRailParallax
import com.luc4n3x.levyra.ui.glacierRailItemMotion
import com.luc4n3x.levyra.ui.glacierRailMotionEnabled
import com.luc4n3x.levyra.ui.i18n.DiscoveryCopy
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.discoveryCopy
import com.luc4n3x.levyra.ui.ivoryRailItemMotion
import com.luc4n3x.levyra.ui.ivoryRailMotionEnabled
import com.luc4n3x.levyra.ui.theme.AtelierDesign
import com.luc4n3x.levyra.ui.theme.IvoryDesign
import com.luc4n3x.levyra.ui.theme.LevyraActivePalette
import com.luc4n3x.levyra.ui.theme.LevyraIsAppleMusic
import com.luc4n3x.levyra.ui.theme.LevyraIsAtelier
import com.luc4n3x.levyra.ui.theme.LevyraIsIvory
import com.luc4n3x.levyra.ui.theme.LevyraIsMono
import com.luc4n3x.levyra.ui.theme.LevyraIsNocturne
import com.luc4n3x.levyra.ui.theme.LevyraIsPrism
import com.luc4n3x.levyra.ui.theme.LevyraIsQobuz
import com.luc4n3x.levyra.ui.theme.LevyraIsSpotify
import com.luc4n3x.levyra.ui.theme.LevyraIsTorque
import com.luc4n3x.levyra.ui.theme.LevyraIsYouTubeMusic
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.MonoDesign
import com.luc4n3x.levyra.ui.theme.Nocturne78Design
import com.luc4n3x.levyra.ui.theme.PrismAccent
import com.luc4n3x.levyra.ui.theme.PrismDesign
import com.luc4n3x.levyra.ui.theme.TorqueStripShape

private enum class DailyLook { Default, Nocturne, Prism, Torque, Ivory, Mono, Atelier }

private fun dailyLook(): DailyLook = when {
    LevyraIsTorque -> DailyLook.Torque
    LevyraIsIvory -> DailyLook.Ivory
    LevyraIsMono -> DailyLook.Mono
    LevyraIsAtelier -> DailyLook.Atelier
    LevyraIsPrism -> DailyLook.Prism
    LevyraIsNocturne -> DailyLook.Nocturne
    else -> DailyLook.Default
}

internal fun skinShowsDailyDiscovery(): Boolean =
    !LevyraIsQobuz && !LevyraIsAppleMusic && !LevyraIsYouTubeMusic && !LevyraIsSpotify

@Composable
internal fun DailyDiscoveryShelf(inset: Dp, style: HomeStyle? = null, spec: HomeStyleSpec? = null) {
    val handle = LocalDailyDiscovery.current ?: return
    if (!skinShowsDailyDiscovery()) return
    val ui by handle.state.collectAsStateWithLifecycle()
    LaunchedEffect(handle) { handle.onWarm() }
    val cards = remember(ui.daily) { dailyDiscoveryCards(ui.daily, handle.homeTrackKeys(), handle.familiarArtistKeys()) }
    if (cards.isEmpty()) return
    val code = LocalLevyraStrings.current.code
    val copy = remember(code) { discoveryCopy(code) }
    val look = dailyLook()
    val rowState = rememberLazyListState()
    val defaultMotion = defaultRailMotionEnabled()
    val ivoryMotion = ivoryRailMotionEnabled()
    val monoMotion = glacierRailMotionEnabled()
    val playAll = { handle.onPlayDaily(cards, cards.first()) }
    Column(Modifier.fillMaxWidth().padding(top = dailyTopGap(look, spec))) {
        if (style != null && spec != null) {
            Box(Modifier.padding(bottom = 12.dp)) { StudioSectionHeader(style, spec, copy.dailyTitle, false, onPlayAll = playAll) }
        } else when (look) {
            DailyLook.Prism -> PrismShelfHeader(copy.dailyTitle, inset)
            DailyLook.Mono -> MonoSectionHeader(copy.dailyTitle, Modifier.padding(bottom = MonoDesign.headerToContent))
            DailyLook.Atelier -> AtelierSectionLabel(copy.dailyTitle)
            else -> SkinSectionTitle(copy.dailyTitle, inset)
        }
        LazyRow(
            state = rowState,
            contentPadding = PaddingValues(horizontal = inset),
            horizontalArrangement = Arrangement.spacedBy(dailyCardGap(look))
        ) {
            itemsIndexed(cards, key = { _, card -> "daily-${card.key}" }, contentType = { _, _ -> "daily-discovery" }) { index, card ->
                val key = "daily-${card.key}"
                val reason = dailyDiscoveryReason(card, copy, index)
                val onPlay = { handle.onPlayDaily(cards, card) }
                val motion = Modifier
                    .defaultRailItemMotion(defaultMotion, rowState, key)
                    .ivoryRailItemMotion(ivoryMotion, rowState, key)
                    .glacierRailItemMotion(monoMotion, rowState, key)
                Box(motion) {
                    when (look) {
                        DailyLook.Default -> DefaultDailyCard(card, reason, Modifier.defaultRailParallax(defaultMotion, rowState, key), onPlay)
                        DailyLook.Nocturne -> NocturneDailyCard(card, reason, onPlay)
                        DailyLook.Prism -> PrismDailyCard(card, reason, onPlay)
                        DailyLook.Torque -> TorqueDailyCard(card, reason, onPlay)
                        DailyLook.Ivory -> IvoryDailyCard(card, reason, onPlay)
                        DailyLook.Mono -> MonoDailyCard(card, reason, onPlay)
                        DailyLook.Atelier -> AtelierDailyCard(card, reason, onPlay)
                    }
                }
            }
        }
    }
}

private fun dailyTopGap(look: DailyLook, spec: HomeStyleSpec?): Dp = when (look) {
    DailyLook.Prism -> 0.dp
    DailyLook.Torque, DailyLook.Ivory -> 8.dp
    DailyLook.Mono -> MonoDesign.sectionGap
    DailyLook.Atelier -> 0.dp
    DailyLook.Default, DailyLook.Nocturne -> spec?.sectionGap ?: 24.dp
}

private fun dailyCardGap(look: DailyLook): Dp = when (look) {
    DailyLook.Mono -> MonoDesign.itemGap
    DailyLook.Atelier -> AtelierDesign.itemGap
    DailyLook.Prism, DailyLook.Ivory -> 14.dp
    else -> 12.dp
}

internal fun dailyDiscoveryReason(card: DiscoveryCard, copy: DiscoveryCopy, index: Int): String {
    val artist = primaryArtistName(card.reason.seedArtist)
    return if (index % 3 == 2) copy.reasonForFansOf(artist) else copy.reasonSimilarTo(card.reason.seedTitle, artist)
}

private fun primaryArtistName(artist: String): String =
    artist.split(PrimaryArtistSeparators).first().trim().ifBlank { artist.trim() }

@Composable
private fun Modifier.dailyCard(card: DiscoveryCard, reason: String, onPlay: () -> Unit): Modifier =
    levyraPressable(onClick = onPlay, pressedScale = LevyraPressScale.Tile, role = Role.Button)
        .semantics(mergeDescendants = true) { contentDescription = "${card.track.title}, ${card.track.artist}. $reason" }

private fun artworkUrl(card: DiscoveryCard): String = card.track.largeThumbnailUrl.ifBlank { card.track.thumbnailUrl }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DefaultDailyCard(card: DiscoveryCard, reason: String, artworkMotion: Modifier, onPlay: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val fallback = remember(scheme.primary, scheme.tertiary) { ArtworkPalette(scheme.primary.toArgb(), scheme.tertiary.toArgb()) }
    val palette by rememberArtworkPalette(paletteKey = "daily-${card.key}", artworkUrl = card.track.thumbnailUrl, fallback = fallback)
    val tint = Color(palette.start)
    val accent = lerp(tint, scheme.onSurface, 0.35f)
    Column(
        Modifier
            .width(204.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(lerp(tint, scheme.surfaceContainerHigh, 0.84f))
            .dailyCard(card, reason, onPlay)
            .padding(8.dp)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)).background(scheme.surfaceContainerHighest)) {
                CoverImage(track = card.track, modifier = Modifier.fillMaxSize().then(artworkMotion), highRes = true)
            }
            Box(
                Modifier.align(Alignment.BottomEnd).padding(10.dp).size(48.dp)
                    .clip(MaterialShapes.Cookie9Sided.toShape())
                    .background(scheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.PlayArrow, null, tint = scheme.onPrimaryContainer, modifier = Modifier.size(26.dp))
            }
        }
        Column(Modifier.padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 8.dp).clearAndSetSemantics { }) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = accent, modifier = Modifier.padding(top = 1.dp).size(14.dp))
                Text(
                    reason, color = accent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                    minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                card.track.title, color = scheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                card.track.artist, color = scheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NocturneDailyCard(card: DiscoveryCard, reason: String, onPlay: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Column(Modifier.width(172.dp).dailyCard(card, reason, onPlay)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(Nocturne78Design.artworkCorner)).background(Nocturne78Design.variant)) {
            CoverImage(track = card.track, modifier = Modifier.fillMaxSize(), highRes = true)
        }
        Column(Modifier.clearAndSetSemantics { }) {
            Box(Modifier.padding(top = 12.dp).width(24.dp).height(1.dp).background(accent))
            Text(
                reason.uppercase(), color = accent, style = MaterialTheme.typography.labelSmall,
                minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                card.track.title, color = LevyraText, style = MaterialTheme.typography.titleLarge.copy(fontSize = 17.sp, lineHeight = 22.sp),
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp)
            )
            Text(card.track.artist, color = LevyraMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun PrismDailyCard(card: DiscoveryCard, reason: String, onPlay: () -> Unit) {
    Box(
        Modifier.size(196.dp, 252.dp).clip(PrismDesign.cut(22.dp)).background(Color.Black).dailyCard(card, reason, onPlay)
    ) {
        PrismSplitArtwork(artworkUrl(card), Modifier.fillMaxSize().clearAndSetSemantics { })
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.38f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.94f))))
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp).clearAndSetSemantics { }) {
            Text(
                reason, color = Color.White, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clip(CircleShape).background(PrismAccent.gradient(false)).padding(horizontal = 10.dp, vertical = 4.dp)
            )
            Text(
                card.track.title, color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp)
            )
            Text(card.track.artist, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TorqueDailyCard(card: DiscoveryCard, reason: String, onPlay: () -> Unit) {
    Box(Modifier.size(212.dp, 276.dp).background(LevyraPanel).dailyCard(card, reason, onPlay)) {
        CoverImage(track = card.track, modifier = Modifier.fillMaxWidth().aspectRatio(1f).clearAndSetSemantics { }, highRes = true)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.35f to Color.Transparent, 0.7f to Color.Black.copy(alpha = 0.55f))))
        Box(Modifier.fillMaxSize().clip(TorqueStripShape).background(LevyraActivePalette.blue))
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp).clearAndSetSemantics { }) {
            Text(
                reason.uppercase(), color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                card.track.title.uppercase(), color = Color.White, fontSize = 15.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp)
            )
            Text(card.track.artist, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun IvoryDailyCard(card: DiscoveryCard, reason: String, onPlay: () -> Unit) {
    Column(Modifier.width(188.dp).dailyCard(card, reason, onPlay)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(IvoryDesign.coverShape).background(LevyraPanel)) {
            CoverImage(track = card.track, modifier = Modifier.fillMaxSize().clearAndSetSemantics { }, highRes = true)
            IvoryCornerBadge(52.dp, Modifier.align(Alignment.BottomEnd)) {
                Icon(Icons.Rounded.PlayArrow, null, tint = IvoryDesign.onInk, modifier = Modifier.size(20.dp))
            }
        }
        Column(Modifier.padding(top = 10.dp).clearAndSetSemantics { }) {
            Text(reason, color = IvoryDesign.ink, style = MaterialTheme.typography.labelMedium, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                card.track.title, color = LevyraText, style = MaterialTheme.typography.titleMedium, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp)
            )
            Text(card.track.artist, color = LevyraMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MonoDailyCard(card: DiscoveryCard, reason: String, onPlay: () -> Unit) {
    val shape = RoundedCornerShape(MonoDesign.artCorner)
    Column(Modifier.width(MonoDesign.cardWide).dailyCard(card, reason, onPlay)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(shape).background(MonoDesign.surface).border(1.dp, MonoDesign.hairline, shape)) {
            CoverImage(track = card.track, modifier = Modifier.fillMaxSize(), highRes = true)
        }
        Column(Modifier.padding(top = 10.dp).clearAndSetSemantics { }) {
            Text(
                reason.uppercase(), color = MonoDesign.accent, style = MaterialTheme.typography.labelMedium,
                minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Text(
                card.track.title, color = MonoDesign.text, style = MaterialTheme.typography.titleSmall, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp)
            )
            Text(card.track.artist, color = MonoDesign.textSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AtelierDailyCard(card: DiscoveryCard, reason: String, onPlay: () -> Unit) {
    Column(Modifier.width(168.dp).dailyCard(card, reason, onPlay)) {
        Box(
            Modifier.size(168.dp)
                .graphicsLayer {
                    shadowElevation = 12.dp.toPx()
                    shape = AtelierDesign.sleeveShape
                    clip = true
                }
                .background(AtelierPaper)
                .padding(5.dp)
        ) {
            CoverImage(track = card.track, modifier = Modifier.fillMaxSize().clearAndSetSemantics { }, highRes = true)
        }
        Column(Modifier.clearAndSetSemantics { }) {
            Text(
                reason, color = AtelierDesign.accent, style = MaterialTheme.typography.labelSmall, minLines = 2, maxLines = 2,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp)
            )
            Text(
                card.track.title, fontFamily = AtelierSerif, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 19.sp,
                color = AtelierDesign.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp)
            )
            Text(card.track.artist, style = MaterialTheme.typography.bodySmall, color = AtelierDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private val PrimaryArtistSeparators = Regex("""\s*(?:,|&|\s+x\s+|\s+feat\.?\s+|\s+ft\.?\s+)\s*""", RegexOption.IGNORE_CASE)
