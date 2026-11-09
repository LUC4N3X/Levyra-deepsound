package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.ui.components.LevyraMediaTitle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ReleaseType
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.searchDiscographyCopy
import com.luc4n3x.levyra.ui.theme.IvoryDesign
import com.luc4n3x.levyra.ui.theme.LevyraActivePalette
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraIsDefaultSkin
import com.luc4n3x.levyra.ui.theme.LevyraIsIvory
import com.luc4n3x.levyra.ui.theme.LevyraIsMono
import com.luc4n3x.levyra.ui.theme.LevyraIsNocturne
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.MonoDesign
import com.luc4n3x.levyra.ui.theme.Nocturne78Design
import com.luc4n3x.levyra.ui.theme.PrismAccent
import com.luc4n3x.levyra.ui.theme.PrismDesign

internal class ReleaseLook(
    val background: Color,
    val panel: Color,
    val text: Color,
    val muted: Color,
    val accent: Brush,
    val onAccent: Color,
    val heroShape: Shape,
    val cardShape: Shape,
    val badgeShape: Shape,
    val actionShape: Shape,
    val actionColor: Color,
    val onAction: Color,
    val outline: Color?,
    val uppercase: Boolean,
    val titleSize: TextUnit,
    val titleWeight: FontWeight,
    val spacing: TextUnit,
    val gap: Dp,
    val art: @Composable (String, Modifier) -> Unit
)

private val PlainArt: @Composable (String, Modifier) -> Unit = { url, modifier ->
    AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = modifier)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun rememberReleaseLook(): ReleaseLook? {
    val scheme = MaterialTheme.colorScheme
    return when {
        LevyraIsMono -> ReleaseLook(
            MonoDesign.base, MonoDesign.surface, MonoDesign.text, MonoDesign.textSecondary, Brush.linearGradient(listOf(MonoDesign.accent, MonoDesign.accent)),
            MonoDesign.onAccent, MonoDesign.panelShape, MonoDesign.cardShape, MonoDesign.pillShape, CircleShape, MonoDesign.accent, MonoDesign.onAccent,
            MonoDesign.hairline, false, 26.sp, FontWeight.SemiBold, (-0.4).sp, 12.dp, PlainArt
        )
        LevyraIsPrismOnly -> {
            val light = LevyraActivePalette.isLight
            ReleaseLook(
                if (light) Color(0xFFF7F2F5) else Color(0xFF060507), if (light) Color(0xFFFFFFFF) else Color(0xFF0E0C10), LevyraText, LevyraMuted,
                PrismAccent.gradient(false), Color.White, PrismDesign.cut(26.dp), PrismDesign.cut(18.dp), CircleShape, CircleShape, Color.Transparent, Color.White,
                null, false, 30.sp, FontWeight.Normal, (-0.8).sp, 14.dp
            ) { url, modifier -> PrismSplitArtwork(url, modifier) }
        }
        LevyraIsNocturne -> ReleaseLook(
            LevyraActivePalette.black, LevyraPanel, LevyraText, LevyraMuted, Brush.linearGradient(listOf(LevyraCyan, LevyraCyan)), Color.Black,
            RoundedCornerShape(Nocturne78Design.panelCorner), RoundedCornerShape(Nocturne78Design.cardCorner), RoundedCornerShape(Nocturne78Design.controlCorner),
            RoundedCornerShape(Nocturne78Design.controlCorner), LevyraCyan, Color.Black, LevyraActivePalette.outline, true, 22.sp, FontWeight.Bold, 1.6.sp, 12.dp, PlainArt
        )
        LevyraIsIvory -> ReleaseLook(
            IvoryDesign.onInk, IvoryDesign.well, IvoryDesign.ink, IvoryDesign.ink.copy(alpha = 0.62f), Brush.linearGradient(listOf(IvoryDesign.ink, IvoryDesign.ink)),
            IvoryDesign.onInk, RoundedCornerShape(IvoryDesign.surfaceCorner), IvoryDesign.coverShape, CircleShape, CircleShape, IvoryDesign.ink, IvoryDesign.onInk,
            null, false, 28.sp, FontWeight.Medium, (-0.6).sp, 14.dp, PlainArt
        )
        LevyraIsDefaultSkin -> ReleaseLook(
            scheme.surface, scheme.surfaceContainer, scheme.onSurface, scheme.onSurfaceVariant, Brush.linearGradient(listOf(scheme.tertiaryContainer, scheme.tertiaryContainer)),
            scheme.onTertiaryContainer, RoundedCornerShape(32.dp), RoundedCornerShape(24.dp), MaterialShapes.Pill.toShape(), MaterialShapes.Cookie9Sided.toShape(),
            scheme.primary, scheme.onPrimary, null, false, 28.sp, FontWeight.Bold, (-0.4).sp, 12.dp, PlainArt
        )
        else -> null
    }
}

@Composable
internal fun SkinNewReleasesDestination(
    look: ReleaseLook,
    releases: List<AlbumHit>,
    isLoading: Boolean,
    strings: LevyraStrings,
    bottomInset: Dp,
    onBack: () -> Unit,
    onOpenRelease: (AlbumHit) -> Unit
) {
    val labels = remember(strings.code) { searchDiscographyCopy(strings.code) }
    val badge: (AlbumHit) -> String = { album ->
        when (album.releaseType) {
            ReleaseType.Album, ReleaseType.Compilation -> labels.album
            ReleaseType.Single -> labels.single
            ReleaseType.Ep -> "EP"
            ReleaseType.Unknown -> ""
        }
    }
    val cased: (String) -> String = { if (look.uppercase) it.uppercase() else it }
    val rest = remember(releases) { releases.drop(1).chunked(2) }
    val gutter = 20.dp
    LazyColumn(Modifier.fillMaxSize().background(look.background), contentPadding = PaddingValues(bottom = bottomInset + 24.dp)) {
        item(key = "skin-releases-head") {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 8.dp, end = gutter, top = 8.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp).clip(look.actionShape).background(look.panel)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, strings.back, tint = look.text)
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    cased(strings.exploreNewReleases), color = look.text, fontSize = look.titleSize, lineHeight = look.titleSize * 1.15f,
                    fontWeight = look.titleWeight, letterSpacing = look.spacing, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).semantics { heading() }
                )
                if (releases.isNotEmpty()) Text(
                    releases.size.toString(), color = look.onAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(look.badgeShape).background(look.accent).padding(horizontal = 10.dp, vertical = 4.dp).clearAndSetSemantics { }
                )
            }
        }
        when {
            isLoading && releases.isEmpty() -> item(key = "skin-releases-loading") {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { LevyraLoadingIndicator(color = look.text, modifier = Modifier.size(36.dp)) }
            }
            releases.isEmpty() -> item(key = "skin-releases-empty") {
                Text(strings.exploreEmpty, color = look.muted, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(gutter))
            }
            else -> {
                item(key = "skin-releases-lead") {
                    val lead = releases.first()
                    SkinReleaseLead(look, lead, badge(lead), cased, strings, Modifier.padding(horizontal = gutter).padding(bottom = look.gap)) { onOpenRelease(lead) }
                }
                itemsIndexed(rest, key = { index, pair -> "skin-releases-$index-${pair.first().browseId.ifBlank { pair.first().title }}" }) { _, pair ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = gutter).padding(bottom = look.gap), horizontalArrangement = Arrangement.spacedBy(look.gap)) {
                        pair.forEach { album -> SkinReleaseCard(look, album, badge(album), cased, Modifier.weight(1f)) { onOpenRelease(album) } }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun Modifier.lookOutline(look: ReleaseLook, shape: Shape): Modifier = look.outline?.let { border(1.dp, it, shape) } ?: this

@Composable
private fun SkinReleaseLead(look: ReleaseLook, album: AlbumHit, badge: String, cased: (String) -> String, strings: LevyraStrings, modifier: Modifier, onOpen: () -> Unit) {
    Box(
        modifier.fillMaxWidth().aspectRatio(1f).clip(look.heroShape).background(look.panel).lookOutline(look, look.heroShape)
            .levyraPressable(onClick = onOpen, onClickLabel = strings.openAlbum, role = Role.Button, pressedScale = LevyraPressScale.Tile)
            .semantics(mergeDescendants = true) { contentDescription = listOf(album.title, badge, album.artist).filter(String::isNotBlank).joinToString(", ") }
    ) {
        look.art(album.thumbnailUrl, Modifier.fillMaxSize().clearAndSetSemantics { })
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 0.85f to Color.Black.copy(alpha = 0.72f), 1f to Color.Black.copy(alpha = 0.88f))))
        if (badge.isNotBlank()) Text(
            cased(badge), color = look.onAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp,
            modifier = Modifier.align(Alignment.TopStart).padding(14.dp).clip(look.badgeShape).background(look.accent).padding(horizontal = 12.dp, vertical = 5.dp).clearAndSetSemantics { }
        )
        Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f).clearAndSetSemantics { }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LevyraMediaTitle(cased(album.title), explicit = album.explicit, color = Color.White, fontSize = 26.sp, lineHeight = 30.sp, fontWeight = look.titleWeight, letterSpacing = look.spacing)
                Text(listOf(album.artist, album.year).filter(String::isNotBlank).joinToString(" · "), color = Color.White.copy(alpha = 0.82f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(
                Modifier.padding(start = 12.dp).size(56.dp).clip(look.actionShape).background(if (look.actionColor == Color.Transparent) look.accent else Brush.linearGradient(listOf(look.actionColor, look.actionColor)))
                    .clearAndSetSemantics { },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = look.onAction, modifier = Modifier.size(24.dp)) }
        }
    }
}

@Composable
private fun SkinReleaseCard(look: ReleaseLook, album: AlbumHit, badge: String, cased: (String) -> String, modifier: Modifier, onOpen: () -> Unit) {
    Column(
        modifier.levyraPressable(onClick = onOpen, role = Role.Button, pressedScale = LevyraPressScale.Tile)
            .semantics(mergeDescendants = true) { contentDescription = listOf(album.title, badge, album.artist).filter(String::isNotBlank).joinToString(", ") }
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(look.cardShape).background(look.panel).lookOutline(look, look.cardShape)) {
            look.art(album.thumbnailUrl, Modifier.fillMaxSize().clearAndSetSemantics { })
            if (badge.isNotBlank()) Text(
                cased(badge), color = look.onAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp,
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp).clip(look.badgeShape).background(look.accent).padding(horizontal = 9.dp, vertical = 3.dp).clearAndSetSemantics { }
            )
        }
        Column(Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp).clearAndSetSemantics { }, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            LevyraMediaTitle(cased(album.title), explicit = album.explicit, color = look.text, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = if (look.uppercase) 0.8.sp else 0.sp)
            Text(album.artist, color = look.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
